package fileIO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import entities.model.Face;
import entities.model.Model;
import entities.model.Normal;
import entities.model.TexCoord;
import entities.model.Vertex;
import entities.rbh.BodyNode;
import entities.rbh.RBH;
import entities.rbh.RBHChunkHeader;
import fileIO.obj.ObjWriter;
import logger.Logger;
import logger.Logger.Level;

public class ModelReader {

	private LittleEndianInputStream stream;
	private Logger logger;
	private RBH rbh;

	public ModelReader() {
		logger = new Logger();
		logger.setLevel(Level.DEBUG);
	}

	public String extractModel(File file) {
		
		String filename = file.getName();
		String modelName = filename.substring(0, filename.lastIndexOf('.'));
		String outputDir = file.getParent() + "\\" + modelName + ".output\\";
		
		System.out.println(filename);
		System.out.println(modelName);
		System.out.println(outputDir);
		
		File folder = new File(outputDir);
		if (!folder.exists()) {
			folder.mkdir();
		}
		
		rbh = new RBH();
		rbh.setModelName(modelName);
		rbh.setOutputDir(outputDir);
		
		try {
			stream = new LittleEndianInputStream(file);
			
			int[] bodyChunkSize = readRbhh();
			
			if (bodyChunkSize[0] == 0) {
				
				rbh.setIsMap(true);
				
				for (int i = 0; i < 5; i++) {
		            this.stream.skip(8 + bodyChunkSize[i]);
		        }
				
				stream.skip(8); // BODY, size
				int endOffset = stream.getPosition() + bodyChunkSize[5];
				
				int index = 0;
				while (stream.getPosition() != endOffset) {
					System.out.println("model index: " + index++);
					Model model = readModel(stream.getPosition());
					rbh.addModel(model);
				}
				
				// BODY3 contains WAVE files in sewer.rbh
				// in city2.rbh there are no additional WAVE files
				
				readNormals();
				readVertices();
				
				if (bodyChunkSize.length == 12) {
					readWaterTextureNames();
				}
				
				readTextureOffsets();
				readMaterialIds();
				
			} else {
				readBodyNodes();
				readBodyVertices();
				readBodyNormals();
				readExtraInfoHeader();
				readExtraInfo();
				readSubModels();
				readNormals();
				readVertices();
				readTextureOffsets();
				readMaterialIds();
			}
			
			stream.close();
			
			ObjWriter.outputMaterialsFile(rbh);
			ObjWriter.outputSingleModels(rbh);
			ObjWriter.outputModels(rbh);
		
		} catch (IOException e) {
			e.printStackTrace();
			return "failed: " + e.getMessage();
		}
		
		logger.info("Done!");
		
		return "done!";
	}

	private RBHChunkHeader readChunkHeader() throws IOException {
		return new RBHChunkHeader(stream.getPosition(), stream.readString(4), stream.readInt());
	}

	private int[] readRbhh() throws IOException {
		stream.skip(0xC); // PIFF, size, RBHF
		
		RBHChunkHeader header = readChunkHeader(); // RBH
		
		int numBodyChunks = header.getSize() / 12;
		
		int[] bodyChunkSize = new int[numBodyChunks];
		logger.debug("----------\r\nRBHH:");
		
		for (int i = 0; i < numBodyChunks; i++) {
			int unknown1 = stream.readInt();
			bodyChunkSize[i] = stream.readInt();
			short unknown2 = stream.readShort();
			short unknown3 = stream.readShort();
			
			logger.debug("\t" + unknown1 + "\t" + bodyChunkSize[i] + "\t" + unknown2 + "\t" + unknown3);
		}
		
		rbh.setBodyChunkSize(bodyChunkSize);
		
		return bodyChunkSize;
	}

	private void readBodyNodes() throws IOException { // BODY 1
		RBHChunkHeader header = readChunkHeader();
		
		int numNodes = stream.readInt();
		int numLeafs = stream.readInt();
		int offsetRoot = stream.readInt();
		int offsetBody2 = stream.readInt();
		int offsetBody3 = stream.readInt();
		
		if (offsetBody2 != 0 || offsetBody3 != 0) {
			logger.error("BODY1: offsetBody2 or offsetBody3 not NULL!");
		}
		
		BodyNode root = readNode(header.getPosition(), offsetRoot);
		
		logger.debug("----------\r\nBODY 1:");
		logger.debug("\t" + root.toString());
		
		rbh.setRootNode(root);
	}

	private BodyNode readNode(int position, int offset) throws IOException {
		stream.seek(position + offset);
		
		int offsetSibling = stream.readInt();
		int offsetChild = stream.readInt();
		float[] values = { stream.readFloat(), stream.readFloat(), stream.readFloat(), stream.readFloat() };
		
		if (offsetSibling == 0) {
			// is leaf
			int numVertices = stream.readInt();
			int unknown = stream.readInt();
			int index3 = stream.readInt();
			int[] indices2 = new int[numVertices];
			for (int i = 0; i < numVertices; i++) {
				indices2[i] = stream.readShort();
			}
			return new BodyNode(values, index3, indices2);
		} else {
			// is node
			return new BodyNode(readNode(position, offsetSibling), readNode(position, offsetChild), values);
		}
	}

	private Vertex[] readBodyVertices() throws IOException { // BODY 2
		return readBodyVert(2);
	}

	private Vertex[] readBodyNormals() throws IOException { // BODY 3
		return readBodyVert(3);
	}

	private Vertex[] readBodyVert(int bodyIndex) throws IOException {
		RBHChunkHeader header = readChunkHeader();
		
		int numVerts = stream.readInt();
		
		Vertex[] verts = new Vertex[numVerts];
		
		logger.debug("----------\r\nBODY 2/3:");
		
		for (int i = 0; i < numVerts; i++) {
			verts[i] = new Vertex(stream.readFloat(), stream.readFloat(), stream.readFloat());
			logger.debug("\t" + verts[i].toString());
		}
		
		if (bodyIndex == 2) {
			rbh.setVerts(verts);
		} else {
			rbh.setNormals(verts);
		}
		
		return verts;
	}

	private void readExtraInfoHeader() throws IOException { // BODY 4
		RBHChunkHeader header = readChunkHeader();
		
		logger.debug("----------\r\nBODY 4:");
		
		int offsetName   = stream.readInt();
		int numUnknown   = stream.readInt();
		int numLods      = stream.readInt();
		int numSubmodels = stream.readInt();
		int numWheels    = stream.readInt();
		
		logger.debug(String.format("\t%d, %d, %d, %d", numUnknown, numLods, numSubmodels, numWheels));
		
		int unknown1 = stream.readInt();
		int unknown2 = stream.readInt();
		
		int offsetBody1 = stream.readInt();
		int[] offsetBody5 = new int[] { stream.readInt(), stream.readInt(), stream.readInt() };
		float[] values = new float[] {
				stream.readFloat(), stream.readFloat(), stream.readFloat(),
				stream.readFloat(), stream.readFloat(), stream.readFloat() };
		
		if ((stream.getPosition() - header.getPosition()) != offsetName) {
			logger.error("BODY4: wrong position!");
		}
		
		String name = stream.readString();
		
		logger.debug("\t" + name);
		logger.debug("\t" + values[0] + "\t" + values[1] + "\t" + values[2]);
		logger.debug("\t" + values[3] + "\t" + values[4] + "\t" + values[5]);
		
		rbh.setNumLods(numLods);
		rbh.setNumSubmodels(numSubmodels);
		rbh.setNumWheels(numWheels);
		rbh.setName(name);
	}

	private void readExtraInfo() throws IOException { // BODY 5
		RBHChunkHeader header = readChunkHeader();
		
		int[] lodOffset = new int[rbh.getNumLods()];
		for (int lodIndex = 0; lodIndex < rbh.getNumLods(); lodIndex++) {
			lodOffset[lodIndex] = stream.readInt();
		}
		
		logger.debug("----------\r\nBODY 5:");
		
		int[][] modelOffset = new int[rbh.getNumLods()][rbh.getNumSubmodels()];
		int[][] wheelOffset = new int[rbh.getNumLods()][rbh.getNumWheels()];
		Vertex[][] wheelPosition = new Vertex[rbh.getNumLods()][rbh.getNumWheels()];
		
		for (int lodIndex = 0; lodIndex < lodOffset.length; lodIndex++) {
			stream.seek(header.getPosition() + lodOffset[lodIndex]);
			stream.readInt(); // unknown
			int wheels = stream.readInt();
			
			int[] subModelOffset = new int[rbh.getNumSubmodels()];
			for (int subModelIndex = 0; subModelIndex < subModelOffset.length; subModelIndex++) {
				subModelOffset[subModelIndex] = stream.readInt();
			}
			
			for (int subModelIndex = 0; subModelIndex < subModelOffset.length; subModelIndex++) {
				stream.seek(header.getPosition() + subModelOffset[subModelIndex]);
				
				stream.readInt();
				stream.readInt();
				stream.readInt();
				
				float[] values = new float[] { stream.readFloat(), stream.readFloat(), stream.readFloat(), stream.readFloat() };
				logger.debug(String.format(Locale.US, "\t%f, %f, %f, %f", values[0], values[1], values[2], values[3]));
				
				modelOffset[lodIndex][subModelIndex] = stream.readInt();
			}
			logger.debug("");
			
			stream.seek(header.getPosition() + wheels);
			
			for (int wheelIndex = 0; wheelIndex < rbh.getNumWheels(); wheelIndex++) {
				stream.readByte(); // wheel index?
				stream.readByte(); // 0
				stream.readByte(); // 0
				stream.readByte(); // 0x60 || 0x20
				
				wheelPosition[lodIndex][wheelIndex] = new Vertex(stream.readFloat(), stream.readFloat() * -1, stream.readFloat() * -1);
				stream.readFloat();
				stream.readFloat();
				stream.readFloat();
				stream.readFloat();
				
				wheelOffset[lodIndex][wheelIndex] = stream.readInt();
			}
		}
		
		rbh.setModelOffset(modelOffset);
		rbh.setWheelOffset(wheelOffset);
		rbh.setWheelPosition(wheelPosition);
		
		stream.seek(header.getEndPosition());
	}

	private void readSubModels() throws IOException { // BODY 6
		RBHChunkHeader header = readChunkHeader();
		logger.debug("header pos: " + header.getPosition());
		
		logger.debug("----------\r\nBODY 6:");
		logger.debug(String.format("\t#LODs: %d, #Submodels: %d", rbh.getNumLods(), rbh.getNumSubmodels()));
		
		for (int lodIndex = 0; lodIndex < rbh.getNumLods(); lodIndex++) {
			for (int wheelIndex = 0; wheelIndex < rbh.getNumWheels(); wheelIndex++) {
				int offset = rbh.getWheelOffset(lodIndex, wheelIndex);
				Vertex wheelPosition = rbh.getWheelPosition(lodIndex, wheelIndex);
				
				if (wheelPosition.x != 0 || offset != 0) {
					Model model = readModel(header.getPosition() + offset);
					
					model.setLodIndex(lodIndex);
					model.setWheelIndex(wheelIndex);
					model.setWheelPosition(wheelPosition);
					rbh.addModel(model);
				}
			}
			
			for (int subModelIndex = 0; subModelIndex < rbh.getNumSubmodels(); subModelIndex++) {
				int offset = rbh.getModelOffset(lodIndex, subModelIndex);
				
				//if (offset != 0) {
					Model model = readModel(header.getPosition() + offset);
					
					model.setLodIndex(lodIndex);
					model.setSubModelIndex(subModelIndex);
					rbh.addModel(model);
				//}
			}
		}
		
		stream.seek(header.getEndPosition());
	}

	private Model readModel(int position) throws IOException {
		stream.seek(position);
		
		int identifier = stream.readInt(); // 2 = standard map face, 1 = special map face, 1000 = model face
		
		stream.readInt(); // subChunkSize
		int numFaces = stream.readInt();
		int sumNumVerts = stream.readInt(); // sumNumVerts
		int vertexOffset = stream.readInt(); // offset in BODY 8
		int normalOffset = stream.readInt(); // offset in BODY 7
		
		if (identifier != 1000) {
			stream.readInt(); // another offset, only for map faces
		}
		
		ArrayList<Face> faces = new ArrayList<Face>(numFaces);
		ArrayList<TexCoord> texCoords = new ArrayList<TexCoord>();
		
		int texCoordOffset = 0;
		
		for (int f = 0; f < numFaces; f++) {
			ArrayList<Integer> vertIds = new ArrayList<Integer>();
			
			stream.readInt(); // 41 00 00 00 | 51 00 00 00 | 61 00 00 00 | 43 10 00 00
			stream.readByte(); // numBytes
			int numVerts = stream.readByte();
			int normalId = stream.readShort();
			int texId = stream.readShort();
			stream.readShort(); // AD DE
			
			for (int v = 0; v < numVerts; v++) {
				int vertId = stream.readShort();
				short flags = stream.readShort(); // unknown effect
				
				float vertU = stream.readFloat();
				float vertV = stream.readFloat() * -1;
				
				if (flags != 4) {
					// TODO: figure out what the flags do
					// see the hood of stan.rbh -> the flags do something to the texture mapping
				}
				vertIds.add(vertId);
				texCoords.add(new TexCoord(vertU, vertV));
			}
			
			faces.add(new Face(vertIds, normalId, texCoordOffset, texId));
			
			texCoordOffset += numVerts;
		}
		
		stream.readInt(); // 00 00 00 00
		stream.readInt(); // 00 00 00 00
		
		Model model = new Model();
		model.setNumVerts(sumNumVerts);
		model.setVertexOffset(vertexOffset);
		model.setNormalOffset(normalOffset);
		model.setFaces(faces);
		model.setTexCoords(texCoords);
		
		return model;
	}

	private void readNormals() throws IOException { // BODY 7
		RBHChunkHeader header = readChunkHeader();
		
		logger.debug("----------\r\nBODY 7:");
		
		for (Model m : rbh.getModels()) {
			stream.seek(header.getPosition() + m.getNormalOffset());
			
			ArrayList<Normal> normals = new ArrayList<Normal>();
			for (int i = 0; i < m.getNumFaces(); i++) {
				Normal normal = new Normal(stream.readFloat(), stream.readFloat(), stream.readFloat());
				logger.debug(normal.toString());
				normals.add(normal);
			}
			logger.debug("");
			
			m.setNormals(normals);
		}
		
		stream.seek(header.getEndPosition());
	}

	private void readVertices() throws IOException { // BODY 8
		RBHChunkHeader header = readChunkHeader();
		
		logger.debug("----------\r\nBODY 8:");
		
		for (Model m : rbh.getModels()) {
			stream.seek(header.getPosition() + m.getVertexOffset());
			
			int numVerts = stream.readInt();
			ArrayList<Vertex> verts = new ArrayList<Vertex>(numVerts);
			
			for (int v = 0; v < numVerts; v++) {
				float x = stream.readFloat();
				float y = stream.readFloat() * -1;
				float z = stream.readFloat() * -1;
				stream.readFloat();
				stream.readFloat();
				stream.readFloat();
				stream.readInt();
				
				Vertex vert = new Vertex(x, y, z);
				logger.debug(vert.toString());
				verts.add(vert);
			}
			logger.debug("");
			
			m.setVerts(verts);
		}
		
		stream.seek(header.getEndPosition());
	}

	private void readWaterTextureNames() throws IOException { // BODY extra
		RBHChunkHeader header = readChunkHeader();
		
		int endPosition = stream.getPosition() + header.getSize();
		
		while (stream.getPosition() != endPosition) { 
			String textureName = stream.readString();
			logger.debug(textureName);
		}
	}

	private void readTextureOffsets() throws IOException { // BODY 9
		RBHChunkHeader header = readChunkHeader();
		
		int numTextures = stream.readShort();
		int numSpecialTextures = stream.readShort();
		int offset = stream.readInt();
		int offsetSpecial = stream.readInt();
		
		int[] textureOffset = new int[numTextures];
		for (int i = 0; i < numTextures; i++) {
			textureOffset[i] = stream.readInt();
		}
		
		int[] textureOffsetSpecial = new int[numSpecialTextures];
		for (int i = 0; i < numSpecialTextures; i++) {
			textureOffsetSpecial[i] = stream.readInt();
		}
		
		rbh.setTextureOffset(textureOffset);
		rbh.setTextureOffsetSpecial(textureOffsetSpecial);
		
		logger.debug("Texture: " + numTextures + ", " + numSpecialTextures + ", " + offset + ", " + offsetSpecial);
	}

	private void readMaterialIds() throws IOException { // BODY 10
		RBHChunkHeader header = readChunkHeader();
		
		int[] materialIds = new int[rbh.getNumTextures()];
		
		for (int i = 0; i < materialIds.length; i++) {
			stream.seek(header.getPosition() +  rbh.getTextureOffset(i));
			materialIds[i] = stream.readShort();
		}
		
		rbh.setMaterialIds(materialIds);
	}

}
