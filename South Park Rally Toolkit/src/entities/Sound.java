package entities;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Sound {

	private int index;

	private String chunkId = "RIFF";
	private int chunkSize;
	private String format = "WAVE";
	
	private String subchunk1Id = "fmt ";
	private int subchunk1Size = 0x10;
	private short audioFormat = 1; // PCM
	private short numChannels = 1; // mono
 	private int sampleRate;
	private int byteRate;
	private short blockAlign;
 	private short bitsPerSample;
 	
	private String subchunk2Id = "data";
	private int subchunk2Size;
	private byte[] data;
	
	private int value1;
	private int value2;
	private String filename;

	public Sound(int index, int offset, int size, int sampleRate, short bitsPerSample) {
		this.index = index;
		this.bitsPerSample = bitsPerSample;
		this.chunkSize = size + 0x24;
		this.sampleRate = sampleRate;
		this.subchunk2Size = size;
		
		this.byteRate = sampleRate * numChannels * bitsPerSample / 8;
		this.blockAlign = (short) (numChannels * bitsPerSample / 8);
	}

	public Sound(int value1, int value2, String filename) {
		this.value1 = value1;
		this.value2 = value2;
		this.filename = filename;
	}

	public int getSize() {
		return subchunk2Size;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public byte[] getData() {
		return data;
	}

	public byte[] getRbhHeader(int size, int offset) {
		ByteBuffer buffer = ByteBuffer.allocate(0x28);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		buffer.putShort(bitsPerSample);
		buffer.putShort((short) 0xFF); // or 0xE0
		buffer.putInt(size);
		buffer.putInt(sampleRate);
		buffer.putInt(-1);
		buffer.putInt(1);
		buffer.putInt(1);
		buffer.putInt(offset);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putShort((short) 0);
		buffer.put((byte) 0xFF); // or 0xC8
		buffer.put((byte) 0); // or 1
		
		return buffer.array();
	}

	private byte[] getWaveHeader() {
		ByteBuffer buffer = ByteBuffer.allocate(0x2C);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		buffer.put(chunkId.getBytes());
		buffer.putInt(chunkSize);
		buffer.put(format.getBytes());
		buffer.put(subchunk1Id.getBytes());
		buffer.putInt(subchunk1Size);
		buffer.putShort(audioFormat);
		buffer.putShort(numChannels);
		buffer.putInt(sampleRate);
		buffer.putInt(byteRate);
		buffer.putShort(blockAlign);
		buffer.putShort(bitsPerSample);
		buffer.put(subchunk2Id.getBytes());
		buffer.putInt(subchunk2Size);
		
		return buffer.array();
	}

	public String getPaddedIndex() {
		return String.format("%04d", index);
	}

	public void writeToFile(String outputDir) throws IOException {
		String filename = outputDir + getPaddedIndex() + ".wav";
		FileOutputStream fos = new FileOutputStream(new File(filename));
		BufferedOutputStream outputStream = new BufferedOutputStream(fos);
		outputStream.write(getWaveHeader());
		outputStream.write(getData());
		outputStream.close();
	}

	public void setValue1(int value1) {
		this.value1 = value1;
	}

	public void setValue2(int value2) {
		this.value2 = value2;
	}

	public int getValue1() {
		return value1;
	}

	public int getValue2() {
		return value2;
	}

	public String getFilename() {
		return filename;
	}
}
