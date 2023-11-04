package entities.rbh;

import java.util.ArrayList;

import entities.model.Model;
import entities.model.Vertex;

public class RBH {

	private ArrayList<Model> models = new ArrayList<Model>();
	
	private int[] bodyChunkSize;
	private BodyNode root;
	private Vertex[] verts;
	private Vertex[] normals;
	private int numLods;
	private int numSubmodels;
	private int numWheels;
	private String name;
	
	private int[][] modelOffset;
	private int[][] wheelOffset;
	private Vertex[][] wheelPosition;
	
	private int[] textureOffset;
	private int[] textureOffsetSpecial;
	private int[] materialIds;
	
	private boolean isMap;
	private String modelName;
	private String outputDir;

	// private RbhLod lod;

	public void setBodyChunkSize(int[] bodyChunkSize) {
		this.bodyChunkSize = bodyChunkSize;
	}

	public void setRootNode(BodyNode root) {
		this.root = root;
	}

	public void setVerts(Vertex[] verts) {
		this.verts = verts;
	}

	public void setNormals(Vertex[] normals) {
		this.normals = normals;
	}

	public void setNumLods(int numLods) {
		this.numLods = numLods;
	}

	public void setNumSubmodels(int numSubmodels) {
		this.numSubmodels = numSubmodels;
	}

	public void setNumWheels(int numWheels) {
		this.numWheels = numWheels;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getNumLods() {
		return numLods;
	}

	public int getNumSubmodels() {
		return numSubmodels;
	}

	public int getNumWheels() {
		return numWheels;
	}

	public void setModelOffset(int[][] modelOffset) {
		this.modelOffset = modelOffset;
	}

	public void setWheelOffset(int[][] wheelOffset) {
		this.wheelOffset = wheelOffset;
	}

	public void setWheelPosition(Vertex[][] wheelPosition) {
		this.wheelPosition = wheelPosition;
	}

	public int getModelOffset(int lodIndex, int subModelIndex) {
		return modelOffset[lodIndex][subModelIndex];
	}

	public int getWheelOffset(int lodIndex, int wheelIndex) {
		return wheelOffset[lodIndex][wheelIndex];
	}

	public Vertex getWheelPosition(int lodIndex, int wheelIndex) {
		return wheelPosition[lodIndex][wheelIndex];
	}

	public void addModel(Model m) {
		models.add(m);
	}

	public ArrayList<Model> getModels() {
		return models;
	}

	public void setTextureOffset(int[] textureOffset) {
		this.textureOffset = textureOffset;
	}

	public void setTextureOffsetSpecial(int[] textureOffsetSpecial) {
		this.textureOffsetSpecial = textureOffsetSpecial;
	}

	public int getNumTextures() {
		return textureOffset.length;
	}

	public int getTextureOffset(int index) {
		return textureOffset[index];
	}

	public void setMaterialIds(int[] materialIds) {
		this.materialIds = materialIds;
	}

	public int getNumMaterials() {
		return materialIds.length;
	}

	public int getMaterialId(int index) {
		return materialIds[index];
	}

	public void setIsMap(boolean isMap) {
		this.isMap = isMap;
	}

	public boolean isMap() {
		return isMap;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public void setOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

	public String getModelName() {
		return modelName;
	}

	public String getOutputDir() {
		return outputDir;
	}
}
