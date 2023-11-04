package fileIO.obj;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import entities.model.Face;
import entities.model.Model;
import entities.model.Normal;
import entities.model.TexCoord;
import entities.model.Vertex;
import entities.rbh.RBH;

public class ObjWriter {

	public static void outputMaterialsFile(RBH rbh) throws IOException {
		FileWriter writer = new FileWriter(new File(rbh.getOutputDir() + rbh.getModelName() + ".mtl"));
		
		for (int i = 0; i < rbh.getNumMaterials(); i++) {
			writer.write("newmtl " + (i + 1));
			writer.write(System.lineSeparator());
			
			writer.write("Ka 1.0 1.0 1.0");
			writer.write(System.lineSeparator());
			
			writer.write("Kd 1.0 1.0 1.0");
			writer.write(System.lineSeparator());
			
			writer.write("Ks 0.0 0.0 0.0");
			writer.write(System.lineSeparator());
			
			writer.write("d 1.0");
			writer.write(System.lineSeparator());
			
			writer.write("illum 2");
			writer.write(System.lineSeparator());
			
			if (rbh.getMaterialId(i) != 0) {
				writer.write("map_Kd " + String.format("%04d", rbh.getMaterialId(i)) + ".bmp");
				writer.write(System.lineSeparator());
			}
			
			writer.write(System.lineSeparator());
		}
		
		writer.close();
	}

	public static void outputSingleModels(RBH rbh) throws IOException {
		
		int modelIndex = 0;
		
		for (Model m : rbh.getModels()) {
			
			String name = rbh.getModelName() + "_";
			
			if (rbh.isMap()) {
				name += modelIndex + ".obj";
				modelIndex++;
			} else {
				name = rbh.getModelName() + "_" + m.getLodIndex() + "_";
				if (m.isWheel()) {
					name += "wheel_" + m.getWheelIndex() + ".obj";
				} else {
					name += m.getSubModelIndex() + ".obj";
				}
			}
			
			FileWriter writer = new FileWriter(new File(rbh.getOutputDir() + name));
			writer.write("mtllib " + rbh.getModelName()  + ".mtl");
			writer.write(System.lineSeparator());
			writer.write(System.lineSeparator());
			
			// Write Vertices
			for (Vertex v : m.getVerts()) {
				writer.write(v.toString());
				writer.write(System.lineSeparator());
			}
			writer.write(System.lineSeparator());
			
			// Write Texture Coordinates
			for (TexCoord vt : m.getTexCoords()) {
				writer.write(vt.toString());
				writer.write(System.lineSeparator());
			}
			writer.write(System.lineSeparator());
			
			// Write Face Normals
			for (Normal vn : m.getNormals()) {
				writer.write(vn.toString());
				writer.write(System.lineSeparator());
			}
			writer.write(System.lineSeparator());
			
			// Write Faces
			writer.write("g " + rbh.getModelName());
			writer.write(System.lineSeparator());
			
			int lastTexId = -1;
			int globalVertOffset = 0;
			for (Face f : m.getFaces()) {
				int texId = f.getTexId();
				if (texId != lastTexId) {
					lastTexId = texId;
					writer.write("usemtl " + (texId + 1));
					writer.write(System.lineSeparator());
				}
				writer.write(f.toString(globalVertOffset));
				writer.write(System.lineSeparator());
				
				globalVertOffset += f.getNumVerts();
			}
			writer.write(System.lineSeparator());
			
			writer.close();
		}
	}

	public static void outputModels(RBH rbh) throws IOException {
		FileWriter writer = new FileWriter(new File(rbh.getOutputDir() + rbh.getModelName() + ".obj"));
		writer.write("mtllib " + rbh.getModelName() + ".mtl");
		writer.write(System.lineSeparator());
		writer.write(System.lineSeparator());
		
		// Write Vertices
		for (Model m : rbh.getModels()) {
			if (m.isWheel()) {
				Vertex wheelPosition = m.getWheelPosition();
				for (Vertex v : m.getVerts()) {
					v.add(wheelPosition);
					writer.write(v.toString());
					writer.write(System.lineSeparator());
				}
			} else {
				for (Vertex v : m.getVerts()) {
					writer.write(v.toString());
					writer.write(System.lineSeparator());
				}
			}
		}
		writer.write(System.lineSeparator());
		
		// Write Texture Coordinates
		for (Model m : rbh.getModels()) {
			for (TexCoord tc : m.getTexCoords()) {
				writer.write(tc.toString());
				writer.write(System.lineSeparator());
			}
		}
		writer.write(System.lineSeparator());
		
		// Write Face Normals
		for (Model m : rbh.getModels()) {
			for (Normal vn : m.getNormals()) {
				writer.write(vn.toString());
				writer.write(System.lineSeparator());
			}
		}
		writer.write(System.lineSeparator());
		
		// Write Faces
		int modelIndex = 1;
		int globalVertOffset = 0;
		int globalNormalOffset = 0;
		int globalTexCoordOffset = 0;
		for (Model m : rbh.getModels()) {
			writer.write("g " + rbh.getModelName() + "_" + modelIndex);
			writer.write(System.lineSeparator());
			
			int lastTexId = -1;
			int vertOffset = globalVertOffset;
			for (Face f : m.getFaces()) {
				int texId = f.getTexId();
				
				if (texId != lastTexId) {
					lastTexId = texId;
					writer.write("usemtl " + (texId + 1));
					writer.write(System.lineSeparator());
				}
				
				writer.write(f.toString(vertOffset, globalTexCoordOffset, globalNormalOffset));
				writer.write(System.lineSeparator());
				
			}
			
			modelIndex++;
			globalVertOffset += m.getNumVerts();
			globalNormalOffset += m.getNumNormals();
			globalTexCoordOffset += m.getNumTexCoords();
		}
		writer.close();
	}
}
