package entities.model;

import java.util.ArrayList;

public class Face {
	private ArrayList<Integer> vertIds;
	private int texCoordOffset;
	private int texId;
	private int normalId;

	public Face(ArrayList<Integer> vertIds, int normalId, int texCoordOffset, int texId) {
		this.vertIds = vertIds;
		this.normalId = normalId;
		this.texId = texId;
		this.texCoordOffset = texCoordOffset;
	}

	public int getTexId() {
		return texId;
	}

	public int getNumVerts() {
		return vertIds.size();
	}

	public String toString() {
		String s = "f";
		for (int i = vertIds.size() - 1; i >= 0; i--) {
			int vertId = vertIds.get(i) + 1;
			s += " " + vertId;
		}
		return s;
	}

	public String toString(int globalVertOffset) {
		String s = "f";
		for (int i = vertIds.size() - 1; i >= 0; i--) {
			int vertId = vertIds.get(i) + 1;
			int texCoordId = globalVertOffset + i + 1;
			s += " " + vertId + "/" + texCoordId + "/" + (normalId + 1);
		}
		return s;
	}

	public String toString(int globalVertOffset, int globalTexCoordOffset, int globalNormalOffset) {
		String s = "f";
		for (int i = vertIds.size() - 1; i >= 0; i--) {
			int vertId = vertIds.get(i) + globalVertOffset + 1;
			int texCoordId = globalTexCoordOffset + texCoordOffset + i + 1;
			int normalId = globalNormalOffset + this.normalId + 1;
			s += " " + vertId + "/" +texCoordId + "/" + normalId;
		}
		return s;
	}

	public String toString(int globalVertOffset, boolean writeTexCoordIdx) {
		String s = "f";
		for (int i = vertIds.size() - 1; i >= 0; i--) {
			int vertId = vertIds.get(i) + globalVertOffset + 1;
			s += " " + vertId;
		}
		return s;
	}
}
