package entities.rbh;

public class RBHChunkHeader {

	private int position; // position in stream
	private String identifier; // e.g. "BODY"
	private int size;

	public RBHChunkHeader(int position, String identifier, int size) {
		this.position = position + 8; // include identifier and size
		this.identifier = identifier;
		this.size = size;
	}

	public int getPosition() {
		return position;
	}

	public String getIdentifier() {
		return identifier;
	}

	public int getSize() {
		return size;
	}

	public int getEndPosition() {
		return position + size;
	}
}
