package entities;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class Bitmap {
	
	public enum Type { MS_BITMAP, VRAM_BITMAP }; 
	
	private int index;
	private int width;
	private int height;
	private int flags;
	private int colorDepth;
	private byte[] bitFields;
	private byte[] colortable;
	private byte[] pixels;
	private List<byte[]> mipmaps;
	private int numMipmaps;
	private String filename;
	
	private boolean hasTransparency = true;

	public Bitmap(Type type, int index, int width, int height, int flags, int colorDepth, byte[] bitFields, byte[] colortable, byte[] pixels, List<byte[]> mipmaps) {
		this.index = index;
		this.colorDepth = colorDepth;
		if (colorDepth == 4) {
			swapPixels(pixels);
		} else {
			this.pixels = pixels;
		}
		this.width = width;
		this.height = height;
		if (colortable != null) {
			convertColortable(colortable);
		}
		swapBitFields(bitFields, type);
		if (type == Type.MS_BITMAP) {
			this.flags = calculateFlags();
		} else {
			this.flags = flags;
		}
		this.mipmaps = mipmaps;
		this.numMipmaps = mipmaps.size();
	}

	public Bitmap(int index, int numMipmaps, String filename) {
		this.index = index;
		this.numMipmaps = numMipmaps;
		this.filename = filename;
	}

	/*
	 * flags:
	 * bit 1 (1)	hasNoColortable (16,24,32)
	 * bit 2 (2) 	hasTransparency (16,32)
	 * bit 3 (4) 	hasColorDepth4
	 * bit 4 (8) 	hasColorDepth8
	 * bit 5 (16) 	hasNoTransparency (16,32)
	 * bit 8 (128)	hasMipmaps
	 */
	private int calculateFlags() {
		int flags = 1;
		
		if (colorDepth == 4) {
			flags = 4;
		} else if (colorDepth == 8) {
			flags = 8;
		}
		
		if (colorDepth == 16 || colorDepth == 32) {
			if (hasTransparency) {
				flags |= 2;
			} else {
				flags |= 16;
			}
		}
		
		return flags;
	}

	// ARGB <> RGBA
	private void swapBitFields(byte[] bf, Type type) {
		bitFields = new byte[0x10];
		
		if (type == Type.VRAM_BITMAP) {
			for (int i = 0; i < 12; i++) {
				bitFields[i] = bf[i + 4];
			}
			for (int i = 0; i < 4; i++) {
				bitFields[i + 12] = bf[i];
			}
		} else { // MS_BITMAP
			for (int i = 0; i < 12; i++) {
				bitFields[i + 4] = bf[i];
			}
			for (int i = 0; i < 4; i++) {
				bitFields[i] = bf[i + 12];
			}
		}
		
		if ((bitFields[0] == 0 && bitFields[1] == 0 && bitFields[2] == 0 && bitFields[3] == 0) ||
			(bitFields[12] == 0 && bitFields[13] == 0 && bitFields[14] == 0 && bitFields[15] == 0)) {
			hasTransparency = false;
		}
	}

	private void swapPixels(byte[] p) {
		pixels = new byte[p.length];
		for (int i = 0; i < p.length; i++) {
			pixels[i]  = (byte) ((p[i] & 0xF) << 4);
			pixels[i] |= (byte) ((p[i] & 0xF0) >> 4);
		}
	}

	// swap byte order (RGB <> BGR)
	private void convertColortable(byte[] ct) {
		colortable = new byte[ct.length];
		
		for (int i = 0; i < ct.length >> 2; i++) {
			colortable[i * 4]     = ct[i * 4 + 2];
			colortable[i * 4 + 1] = ct[i * 4 + 1];
			colortable[i * 4 + 2] = ct[i * 4];
			colortable[i * 4 + 3] = ct[i * 4 + 3];
		}
	}

	public byte[] getVramHeader() {
		int headerSize = 0x40;
		int chunkSize = headerSize + pixels.length;
		int imageDataOffset = headerSize;
		if (hasColortable()) {
			chunkSize += colortable.length;
			imageDataOffset += colortable.length;
		}
		
		ByteBuffer buffer = ByteBuffer.allocate(headerSize + 8);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		buffer.putInt(0x59444F42); // BODY
		buffer.putInt(chunkSize);
		buffer.putInt(headerSize);
		buffer.putInt(index);
		buffer.putShort((short) width);
		buffer.putShort((short) height);
		buffer.putShort((short) flags);
		buffer.putShort((short) colorDepth);
		buffer.put(bitFields);
		buffer.putInt(imageDataOffset);
		if (hasColortable()) {
			buffer.putInt(headerSize);
		}
		
		return buffer.array();
	}

	public byte[] getBitmapHeader() {
		return getBitmapHeader(this.width, this.height);
	}

	public byte[] getBitmapHeader(int width, int height) {
		int headerSize = 0x36;
		if (colorDepth == 16 || colorDepth == 32) {
			headerSize = 0x46;
		}
		int imageDataOffset = headerSize;
		if (colortable != null) {
			imageDataOffset += colortable.length;
		}
		int size = imageDataOffset + width * height * colorDepth / 8;
		
		ByteBuffer buffer = ByteBuffer.allocate(headerSize);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		buffer.putShort((short) 0x4D42); // BM
		buffer.putInt(size);
		buffer.putInt(0); // reserved
		buffer.putInt(imageDataOffset);
		buffer.putInt(headerSize - 14);
		buffer.putInt(width);
		buffer.putInt(height);
		buffer.putShort((short) 1);
		buffer.putShort((short) colorDepth);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putInt(0);
		if (headerSize == 0x46) {
			buffer.put(bitFields);
		}
		
		return buffer.array();
	}

	public byte[] getColortable() {
		return colortable;
	}

	public byte[] getPixels() {
		int lineLength = pixels.length / height;
		byte[] p = new byte[pixels.length];
		
		// flip line order from "top-to-bottom" to MS Bitmap's "upside-down" (or vice-versa)
		for (int h = 0; h < height; h++) {
			System.arraycopy(pixels, h * lineLength, p, (height - h - 1) * lineLength, lineLength);
		}
		return p;
	}

	public byte[] getMipmap(int index) {
		int div = (int) Math.pow(2, (index + 1));
		int height = this.height / div;
		byte[] pixels = mipmaps.get(index);
		int lineLength = pixels.length / height;
		byte[] p = new byte[pixels.length];
		
		// flip line order from "top-to-bottom" to MS Bitmap's "upside-down" (or vice-versa)
		for (int h = 0; h < height; h++) {
			System.arraycopy(pixels, h * lineLength, p, (height - h - 1) * lineLength, lineLength);
		}
		return p;
	}

	public boolean hasColortable() {
		if (colorDepth <= 8) {
			return true;
		}
		return false;
	}

	public int getIndex() {
		return index;
	}

	public int getColorDepth() {
		return colorDepth;
	}

	public int getFlags() {
		return flags;
	}

	public int getNumMipmaps() {
		return numMipmaps;
	}

	public String getPaddedIndex() {
		return String.format("%04d", index);
	}

	public void writeToFile(String outputDir) throws IOException {
		String filename = outputDir + getPaddedIndex() + ".bmp";
		FileOutputStream fos = new FileOutputStream(new File(filename));
		BufferedOutputStream outputStream = new BufferedOutputStream(fos);
		outputStream.write(getBitmapHeader());
		if (hasColortable()) {
			outputStream.write(getColortable());
		}
		outputStream.write(getPixels());
		outputStream.close();
		
		for (int i = 0; i < mipmaps.size(); i++) {
			filename = outputDir + getPaddedIndex() + ".mipmap" + (i+1) + ".bmp";
			fos = new FileOutputStream(new File(filename));
			outputStream = new BufferedOutputStream(fos);
			int div = (int) Math.pow(2, (i + 1));
			outputStream.write(getBitmapHeader(width / div, height / div));
			if (hasColortable()) {
				outputStream.write(getColortable());
			}
			outputStream.write(getMipmap(i));
			outputStream.close();
		}
	}

	public String getFilename() {
		return filename;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public void setNumMipmaps(int numMipmaps) {
		this.numMipmaps = numMipmaps;
	}

	public List<byte[]> getMipmaps() {
		return mipmaps;
	}
}
