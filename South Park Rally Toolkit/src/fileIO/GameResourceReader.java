package fileIO;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import entities.Bitmap;
import entities.Sound;
import fileIO.exceptions.GameResourceException;

public class GameResourceReader {
	private InputStream stream;
	private String outputDir;
	private ByteBuffer buffer;
	
	private int soundUnknown;

	public GameResourceReader(InputStream stream) {
		this.stream = stream;
	}

	private ByteBuffer getByteBuffer(int size) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(size);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		if (stream.read(buf.array(), 0, buf.capacity()) != buf.capacity()) throw new EOFException();
		return buf;
	}

	private boolean equalsString(ByteBuffer buffer, String string) {
		byte[] array = new byte[string.length()];
		buffer.get(array);
		if (string.equals(new String(array))) {
			return true;
		}
		return false;
	}

	public void extractGameResource(String outputDir) throws GameResourceException {
		this.outputDir = outputDir;
		
		try {
			buffer = getByteBuffer(12);
			if (!equalsString(buffer, "PIFF")) {
				throw new GameResourceException("Not a valid resource file!");
			}
			
			int fileSize = buffer.getInt();
			
			if (equalsString(buffer, "VVTP")) {
				extractBitmapResource(fileSize - 4);
			} else { // RBHF
				extractGenericResource();
			}
			
		} catch (IOException e) {
			throw new GameResourceException(e.getMessage());
		}
	}

	private void extractBitmapResource(int fileSize) throws IOException {
		ArrayList<Bitmap> bitmaps = readBitmaps(fileSize);
		writeBitmapInfoFile(bitmaps);
		writeBitmaps(bitmaps);
	}

	private void extractSoundResource() throws IOException {
		ArrayList<Sound> sounds = readSounds();
		writeSoundInfoFile(sounds);
		writeSounds(sounds);
	}

	private ArrayList<Bitmap> readBitmaps(int fileSize) throws IOException {
		ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();
		
		while (fileSize > 0) {
			buffer = getByteBuffer(12);
			buffer.getInt(); // BODY
			int chunkSize = buffer.getInt();
			int headerSize = buffer.getInt();
			
			buffer = getByteBuffer(headerSize - 4);
			int index = buffer.getInt();
			int width = buffer.getShort();
			int height = buffer.getShort();
			int flags = buffer.getShort();
			int colorDepth = buffer.getShort();
			byte[] bitFields = new byte[0x10]; // ARGB
			buffer.get(bitFields);
			buffer.getInt(); // imageDataOffset
			buffer.getInt(); // chunkSize2
			int numMipmaps = buffer.getInt();
			// skip 20 bytes
			
			/*
			 * flags:
			 * bit 1	hasNoColortable (16,24,32)
			 * bit 2	hasTransparency (16,32)
			 * bit 3	hasColorDepth4
			 * bit 4	hasColorDepth8
			 * bit 5	hasNoTransparency (16,32)
			 * bit 8	hasMipmaps
			 */
			
			fileSize -= (chunkSize + 8);
			
			buffer = getByteBuffer(chunkSize - headerSize);
			
			byte[] colortable = null;
			if (colorDepth <= 8) {
				colortable = new byte[(int) (Math.pow(2,colorDepth) * 4)];
				buffer.get(colortable);
			}
			
			byte[] pixels = new byte[width * height * colorDepth / 8];
			buffer.get(pixels);
			
			ArrayList<byte[]> mipmaps = new ArrayList<byte[]>(numMipmaps);
			
			for (int i = 0; i < numMipmaps; i++) {
				int div = (int) Math.pow(2, (i + 1));
				byte[] mipmap = new byte[width/div * height/div * colorDepth / 8];
				buffer.get(mipmap);
				mipmaps.add(mipmap);
			}
			
			bitmaps.add(new Bitmap(Bitmap.Type.VRAM_BITMAP, index, width, height, flags, colorDepth, bitFields, colortable, pixels, mipmaps));
		}
		
		return bitmaps;
	}

	private void writeBitmapInfoFile(ArrayList<Bitmap> bitmaps) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputDir + "VRAM.txt"));
		writer.write("VRAM");
		writer.newLine();
		writer.write("" + bitmaps.size());
		writer.newLine();
		
		for (Bitmap bitmap : bitmaps) {
			writer.write(bitmap.getIndex() + "\t" + bitmap.getNumMipmaps() + "\t" + bitmap.getPaddedIndex() + ".bmp");
			writer.newLine();
		}
		writer.close();
	}

	private void writeBitmaps(ArrayList<Bitmap> bitmaps) throws IOException {
		for (Bitmap bitmap : bitmaps) {
			
			bitmap.writeToFile(outputDir);
		}
	}

	private void extractGenericResource() throws IOException {
		buffer = getByteBuffer(8);
		buffer.getInt(); // RBHH
		int chunkSize = buffer.getInt();
		if (chunkSize == 0x0C) {
			// ai
			throw new IOException("Unsupported resource type!");
		} else if (chunkSize == 0x18) {
			// sound
			extractSoundResource();
		} else {
			// art
			throw new IOException("Unsupported resource type!");
		}
	}

	private ArrayList<Sound> readSounds() throws IOException {
		stream.skip(0x18); // skip header
		
		buffer = getByteBuffer(8);
		buffer.getInt(); // BODY
		int chunkSize = buffer.getInt();
		int numSounds = chunkSize / 40;
		
		ArrayList<Sound> sounds = new ArrayList<>(numSounds);
		
		// DEBUG:
		//int actualHeaderSize = 0x34 + numSounds * 0x28 + 0x10;
		//int dataBlockSize = 0;
		// DEBUG END
		
		buffer = getByteBuffer(chunkSize);
		for (int i = 0; i < numSounds; i++) {
			short bitsPerSample = buffer.getShort(); // 08 00 or 10 00
			buffer.getShort(); // FF 00 or E0 00
			int size = buffer.getInt() * bitsPerSample / 8;
			int sampleRate = buffer.getInt();
			buffer.getInt(); // FF FF FF FF
			buffer.getInt(); // 01 00 00 00
			buffer.getInt(); // 01 00 00 00
			int offset = buffer.getInt();
			buffer.getInt(); // 00 00 00 00
			buffer.getInt(); // 00 00 00 00
			buffer.getShort(); // 00 00
			buffer.get(); // C8 or FF
			buffer.get(); // 00 or 01
			
			sounds.add(new Sound((i + 1), offset, size, sampleRate, bitsPerSample));
		}
		
		buffer = getByteBuffer(16);
		buffer.getInt(); // BODY
		chunkSize = buffer.getInt();
		buffer.getInt(); // 01 00 00 00 or 00 00 00 00
		buffer.getShort(); // 01 00
		buffer.getShort(); // 01 00
		
		for (Sound sound : sounds) {
			int size = sound.getSize();
			byte[] data = new byte[size];
			stream.read(data);
			sound.setData(data);
			
			// DEBUG:
			//dataBlockSize += size;
			//int padding = (4 - ((actualHeaderSize + dataBlockSize) % 4)) % 4;
			//stream.skip(padding);
			//dataBlockSize += padding;
			// DEBUG END
			
			int padding = (16 - (size % 16)) % 16;
			stream.skip(padding);
		}
		
		stream.skip(12); // GLOB, chunkSize, numSounds
		
		buffer = getByteBuffer(4 + numSounds * 8);
		
		soundUnknown = buffer.getInt();
		
		for (Sound sound : sounds) {
			int value1 = buffer.getInt();
			int value2 = buffer.getInt();
			
			sound.setValue1(value1);
			sound.setValue2(value2);
		}
		
		return sounds;
	}

	private void writeSoundInfoFile(ArrayList<Sound> sounds) throws IOException {
		String filename = outputDir + "Info.txt";
		FileWriter fw = new FileWriter(filename);
		BufferedWriter bw = new BufferedWriter(fw);
		
		bw.write("WAVE");
		bw.newLine();
		
		bw.write(sounds.size() + "\t" + soundUnknown);
		bw.newLine();
		
		for (Sound sound : sounds) {
			bw.write(sound.getValue1() + "\t" + sound.getValue2() + "\t" + sound.getPaddedIndex() + ".wav");
			bw.newLine();
		}
		bw.close();
	}

	private void writeSounds(ArrayList<Sound> sounds) throws IOException {
		for (Sound sound : sounds) {
			sound.writeToFile(outputDir);
		}
	}
}
