package entities.model;

import java.util.ArrayList;

public class Model {
	private ArrayList<Vertex> verts;
	private ArrayList<Normal> normals;
	private ArrayList<TexCoord> texCoords;
	private ArrayList<Face> faces;
	
	private int sumNumVerts;
	private int vertexOffset;
	private int normalOffset;
	
	private int lodIndex;
	private int subModelIndex;
	private int wheelIndex;
	private boolean isWheel;
	private Vertex wheelPosition;

	public String toString() {
		return String.format("[Model] #verts: %d, #normals: %d, #texCoords: %d, numFaces: %d", verts.size(), normals.size(), texCoords.size(), faces.size());
	}

	public int getNumVerts() {
		return verts.size();
	}

	public int getNumTexCoords() {
		return texCoords.size();
	}

	public int getNumFaces() {
		return faces.size();
	}

	public ArrayList<Vertex> getVerts() {
		return verts;
	}

	public ArrayList<TexCoord> getTexCoords() {
		return texCoords;
	}

	public ArrayList<Face> getFaces() {
		return faces;
	}

	public void setVerts(ArrayList<Vertex> verts) {
		this.verts = verts;
	}

	public void setTexCoords(ArrayList<TexCoord> texCoords) {
		this.texCoords = texCoords;
	}

	public void setFaces(ArrayList<Face> faces) {
		this.faces = faces;
	}

	public void setNumVerts(int sumNumVerts) {
		this.sumNumVerts = sumNumVerts;
	}

	public void setVertexOffset(int vertexOffset) {
		this.vertexOffset = vertexOffset;
	}

	public void setNormalOffset(int normalOffset) {
		this.normalOffset = normalOffset;
	}

	public int getNormalOffset() {
		return normalOffset;
	}

	public void setNormals(ArrayList<Normal> normals) {
		this.normals = normals;
	}

	public int getVertexOffset() {
		return vertexOffset;
	}

	public ArrayList<Normal> getNormals() {
		return normals;
	}

	public int getNumNormals() {
		return normals.size();
	}

	public void setLodIndex(int lodIndex) {
		this.lodIndex = lodIndex;
	}

	public void setSubModelIndex(int subModelIndex) {
		this.subModelIndex = subModelIndex;
		this.isWheel = false;
	}

	public void setWheelIndex(int wheelIndex) {
		this.wheelIndex = wheelIndex;
		this.isWheel = true;
	}

	public boolean isWheel() {
		return isWheel;
	}

	public int getLodIndex() {
		return lodIndex;
	}

	public int getSubModelIndex() {
		return subModelIndex;
	}

	public int getWheelIndex() {
		return wheelIndex;
	}

	public void setWheelPosition(Vertex wheelPosition) {
		this.wheelPosition = wheelPosition;
	}

	public Vertex getWheelPosition() {
		return wheelPosition;
	}
}
