package fileIO;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

import entities.Bitmap;
import entities.Sound;
import fileIO.exceptions.BmpReadException;
import fileIO.exceptions.GameResourceException;
import fileIO.exceptions.WavReadException;

public class GameResourceWriter {
	
	private int index = 1;
	private String inputFolderName;
	private int soundUnknown;
	private List<Bitmap> bitmapList = new ArrayList<Bitmap>();
	private List<Sound> soundList = new ArrayList<Sound>();

	public void createGameResource(String inputFolderName) throws GameResourceException {
		this.inputFolderName = inputFolderName;
		
		File inputFolder = new File(inputFolderName);
		
		if (!inputFolder.exists()) {
			throw new GameResourceException("Folder not found!");
		}
		if (!inputFolder.isDirectory()) {
			throw new GameResourceException("Not a folder!");
		}
		
		File infoFile = new File(inputFolderName + "\\" + "Info.txt");
		if (infoFile.exists()) {
			try {
				readInfoFile(infoFile);
			} catch (IOException e) {
				throw new GameResourceException(e.getMessage());
			}
		} else {
			File[] files = inputFolder.listFiles();
			
			if (files == null) {
				throw new GameResourceException("Could not read files from folder!");
			}
			
			for (File f : files) {
				String name = f.getName().toUpperCase();
				if ((name.length() > 4) && (name.endsWith(".BMP"))) {
					Bitmap b;
					try {
						b = readBitmap(f);
						bitmapList.add(b);
					} catch (BmpReadException e) {
						System.err.println(e.getMessage());
						System.exit(1);
					}
				}
			}
		}
		
		if (bitmapList.size() != 0) {
			try {
				writeVram();
			} catch (IOException e) {
				throw new GameResourceException(e.getMessage());
			}
		} else if (soundList.size() != 0) {
			try {
				writeSoundRbh();
			} catch (IOException e) {
				throw new GameResourceException(e.getMessage());
			}
		}
	}

	private void readInfoFile(File infoFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(infoFile));
		String identifier = br.readLine();
		if (identifier.equals("WAVE")) {
			readSoundInfoFile(br);
		} else if (identifier.equals("VRAM")) {
			readBitmapInfoFile(br);
		} else {
			System.out.println("Error: Unknown identifier: " + identifier);
		}
		
		br.close();
	}

	private void readSoundInfoFile(BufferedReader br) throws IOException {
		List<Sound> sounds = new ArrayList<Sound>();
		
		String line = br.readLine();
		StringTokenizer st = new StringTokenizer(line, "\t");
		int numSounds = Integer.parseInt(st.nextToken());
		soundUnknown = Integer.parseInt(st.nextToken());
		
		for (int i = 0; i < numSounds; i++) {
			line = br.readLine();
			st = new StringTokenizer(line, "\t");
			int value1 = Integer.parseInt(st.nextToken());
			int value2 = Integer.parseInt(st.nextToken());
			String filename = st.nextToken();
			
			sounds.add(new Sound(value1, value2, filename));
		}
		
		readSounds(sounds);
	}

	private void readSounds(List<Sound> sounds) {
		for (Sound sound : sounds) {
			File file = new File(inputFolderName + "\\" + sound.getFilename());
			try {
				Sound s = readSound(file);
				s.setValue1(sound.getValue1());
				s.setValue2(sound.getValue2());
				soundList.add(s);
			} catch (WavReadException e) {
				System.err.println(e.getMessage());
			}
		}	
	}

	private void readBitmapInfoFile(BufferedReader br) throws IOException {
		List<Bitmap> bitmaps = new ArrayList<Bitmap>();
		
		int numBitmaps = Integer.parseInt(br.readLine());
		
		for (int i = 0; i < numBitmaps; i++) {
			String line = br.readLine();
			StringTokenizer st = new StringTokenizer(line, "\t");
			int index = Integer.parseInt(st.nextToken());
			int numMipmaps = Integer.parseInt(st.nextToken());
			String filename = st.nextToken();
			
			bitmaps.add(new Bitmap(index, numMipmaps, filename));
		}
		
		readBitmaps(bitmaps);
	}

	private void readBitmaps(List<Bitmap> bitmaps) {
		for (Bitmap bitmap : bitmaps) {
			File file = new File(inputFolderName + "\\" + bitmap.getFilename());
			try {
				Bitmap b = readBitmap(file);
				b.setIndex(bitmap.getIndex());
				b.setNumMipmaps(bitmap.getNumMipmaps());
				bitmapList.add(b);
			} catch (BmpReadException e) {
				System.err.println(e.getMessage());
			}
		}	
	}

	private void writeSoundRbh() throws IOException {
		String rbhFilename = inputFolderName;
		String extension = ".RBH.OUTPUT";
		if (rbhFilename.toUpperCase().endsWith(extension)) {
			rbhFilename = rbhFilename.substring(0, rbhFilename.length() - extension.length());
		}
		rbhFilename += ".rbh";
		
		File f = new File(rbhFilename);
		if (f.exists()) {
			System.out.print("File aready exists!\nDo you want to overwrite it? (Y)ES / (N)O: ");
			Scanner sc = new Scanner(System.in);
			String answer = sc.next().toUpperCase();
			sc.close();
			if (answer.charAt(0) != 'Y') {
				return;
			}
		}
		
		//int actualHeaderSize = 0x34 + soundList.size() * 0x28 + 0x10;
		
		ByteBuffer headerBlock = ByteBuffer.allocate(soundList.size() * 0x28 + 8);
		headerBlock.order(ByteOrder.LITTLE_ENDIAN);
		headerBlock.put("BODY".getBytes());
		headerBlock.putInt(soundList.size() * 0x28);
		
		ByteArrayOutputStream dataBlock = new ByteArrayOutputStream();
		
		for (Sound sound : soundList) {
			byte[] data = sound.getData();
			
			headerBlock.put(sound.getRbhHeader(data.length, dataBlock.size()));
			dataBlock.write(data);
			
			int remainder = data.length % 16;
			if (remainder != 0) {
				byte[] b = new byte[16 - remainder];
				dataBlock.write(b);
			}
			
			/* WRONG:
			// add padding if not fileSize mod 4
			int remainder  = (actualHeaderSize + dataBlock.size()) % 4;
			if (remainder != 0) {
				byte[] b = new byte[4 - remainder];
				dataBlock.write(b);
			}
			*/
		}
		
		ByteBuffer dataBlockHeader = ByteBuffer.allocate(16);
		dataBlockHeader.order(ByteOrder.LITTLE_ENDIAN);
		dataBlockHeader.put("BODY".getBytes());
		dataBlockHeader.putInt(dataBlock.size() + 8);
		dataBlockHeader.putInt(0);
		dataBlockHeader.putShort((short) 1);
		dataBlockHeader.putShort((short) 1);
		
		ByteBuffer globBlock = ByteBuffer.allocate(soundList.size() * 8 + 16);
		globBlock.order(ByteOrder.LITTLE_ENDIAN);
		globBlock.put("GLOB".getBytes());
		globBlock.putInt(soundList.size() * 8 + 8);
		globBlock.putInt(soundList.size());
		globBlock.putInt(soundUnknown);
		for (Sound sound : soundList) {
			globBlock.putInt(sound.getValue1());
			globBlock.putInt(sound.getValue2());
		}
		
		int chunkSize = 0x24 + headerBlock.capacity() + dataBlockHeader.capacity()
			+ dataBlock.size() + globBlock.capacity();
		ByteBuffer rbhHeader = ByteBuffer.allocate(0x2C);
		rbhHeader.order(ByteOrder.LITTLE_ENDIAN);
		rbhHeader.put("PIFF".getBytes());
		rbhHeader.putInt(chunkSize);
		rbhHeader.put("RBHF".getBytes());
		rbhHeader.put("RBHH".getBytes());
		rbhHeader.putInt(0x18);
		rbhHeader.putInt(0);
		rbhHeader.putInt(soundList.size() * 0x28);
		rbhHeader.putShort((short) 2);
		rbhHeader.putShort((short) 4);
		rbhHeader.putInt(0);
		rbhHeader.putInt(dataBlock.size() + 8);
		rbhHeader.putShort((short) 0);
		rbhHeader.putShort((short) 4);
		
		FileOutputStream stream = new FileOutputStream(rbhFilename);
		stream.write(rbhHeader.array());
		stream.write(headerBlock.array());
		stream.write(dataBlockHeader.array());
		stream.write(dataBlock.toByteArray());
		stream.write(globBlock.array());
		stream.close();
	}

	private void writeVram() throws IOException {
		String vramFilename = inputFolderName;
		
		String extension = ".VRAM.OUTPUT";
		if (vramFilename.toUpperCase().endsWith(extension)) {
			vramFilename = vramFilename.substring(0, vramFilename.length() - extension.length());
		}
		vramFilename += ".vram";
		
		File f = new File(vramFilename);
		if (f.exists()) {
			System.out.print("File aready exists!\nDo you want to overwrite it? (Y)ES / (N)O: ");
			Scanner sc = new Scanner(System.in);
			String answer = sc.next().toUpperCase();
			sc.close();
			if (answer.charAt(0) != 'Y') {
				return;
			}
		}
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		for (Bitmap bitmap : bitmapList) {
			outputStream.write(bitmap.getVramHeader());
			if (bitmap.hasColortable()) {
				outputStream.write(bitmap.getColortable());
			}
			outputStream.write(bitmap.getPixels());
		}
		
		int fileSize = outputStream.size() + 4;
		byte[] size = new byte[4];
		size[0] = (byte) ((fileSize & 0x000000FF));
		size[1] = (byte) ((fileSize & 0x0000FF00) >> 8);
		size[2] = (byte) ((fileSize & 0x00FF0000) >> 16);
		size[3] = (byte) ((fileSize & 0xFF000000) >> 24);
		
		FileOutputStream stream = new FileOutputStream(vramFilename);
		stream.write("PIFF".getBytes());
		stream.write(size);
		stream.write("VVTP".getBytes());
		stream.write(outputStream.toByteArray());
		stream.close();
	}

	private ByteBuffer getByteBuffer(InputStream stream, int size) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(size);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		if (stream.read(buf.array(), 0, buf.capacity()) != buf.capacity()) throw new EOFException();
		return buf;
	}

	private Sound readSound(File file) throws WavReadException {
		int sampleRate;
		short bitsPerSample;
		byte[] data;
		
		try {
			DataInputStream stream = new DataInputStream(new FileInputStream(file));
			ByteBuffer buffer = getByteBuffer(stream, 0x2C);
			
			int chunkId = buffer.getInt();
			if (chunkId != 0x46464952) { // RIFF
				throw new WavReadException("Not a valid WAVE file!");
			}
			
			buffer.getInt(); // chunkSize
			buffer.getInt(); // format
			
			buffer.getInt(); // subchunk1Id
			buffer.getInt(); // subchunk1Size
			buffer.getShort(); // audioFormat
			short numChannels = buffer.getShort();
			if (numChannels != 1) {
				throw new WavReadException("Stereo WAVE files are not allowed!");
			}
			
			sampleRate = buffer.getInt();
			buffer.getInt(); // byteRate
			buffer.getShort(); // blockAlign
			bitsPerSample = buffer.getShort();
			
			buffer.getInt(); // subchunk2Id
			int subchunk2Size = buffer.getInt();
			
			data = new byte[subchunk2Size];
			stream.read(data);
			stream.close();
			
		} catch (IOException e) {
			throw new WavReadException(e.getMessage());
		} catch (BufferUnderflowException e) {
			throw new WavReadException("Could not read from wave file: " + file.getName());
		}
		
		int index = 0, offset = 0;
		Sound s = new Sound(index, offset, data.length, sampleRate, bitsPerSample);
		s.setData(data);
		
		return s;
	}

	private Bitmap readBitmap(File file) throws BmpReadException {
		int width;
		int height;
		int flags = -1;
		int colorDepth;
		byte[] bitFields;
		byte[] colortable;
		byte[] pixels;
		
		try {
			DataInputStream stream = new DataInputStream(new FileInputStream(file));
			ByteBuffer buffer = getByteBuffer(stream, 18);
			
			int magicWord = buffer.getShort();
			if (magicWord != 0x4D42) { // BM
				throw new BmpReadException("Not a valid BMP file!");
			}
			buffer.getInt(); // fileSize
			buffer.getInt(); // reserved
			buffer.getInt(); // imageDataOffset
			int headerSize = buffer.getInt();
			
			buffer = getByteBuffer(stream, headerSize - 4);
			
			width = buffer.getInt();
			height = buffer.getInt();
			buffer.getShort(); // numColorPlanes, always 1
			colorDepth = buffer.getShort();
			
			int lineWidth = width * colorDepth / 8;
			if (lineWidth % 4 != 0) {
				throw new BmpReadException("Invalid Bitmap width!");
			}
			int compression = buffer.getInt();
			if (compression != 0 && compression != 3) {
				throw new BmpReadException("Compressed Bitmaps are not supported!");
			}
			buffer.getInt(); // imageSize
			buffer.getInt(); // horizontalResolution
			buffer.getInt(); // verticalResolution
			
			int numColors = buffer.getInt();
			buffer.getInt(); // numImportantColors
			
			bitFields = new byte[0x10];
			if (headerSize == 0x38) { // colorDepth == 16 || colorDepth == 32
				buffer.get(bitFields);
			}
			
			colortable = null;
			if (colorDepth <= 8) {
				colortable = new byte[(int) (Math.pow(2,colorDepth) * 4)];
				
				if (numColors == 0) {
					stream.read(colortable);
				} else {
					byte[] ct = new byte[numColors * 4];
					stream.read(ct);
					System.arraycopy(ct, 0, colortable, 0, ct.length);
				}
			}
			
			pixels = new byte[height * lineWidth];
			stream.read(pixels);
			
			stream.close();
			
		} catch (IOException e) {
			throw new BmpReadException(e.getMessage());
		} catch (BufferUnderflowException e) {
			throw new BmpReadException("Could not read from bitmap: " + file.getName());
		}
		
		return(new Bitmap(Bitmap.Type.MS_BITMAP, index++, width, height, flags, colorDepth, bitFields, colortable, pixels, new ArrayList<byte[]>()));
	}
}
