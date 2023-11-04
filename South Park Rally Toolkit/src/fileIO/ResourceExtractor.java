package fileIO;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import fileIO.exceptions.GameResourceException;

public class ResourceExtractor {

	public static String extractGameResource(String filename) {
		File file = new File(filename);
		
		filename = file.getName();
		String absolutePath = file.getAbsolutePath();
		String filePath = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
		String outputDir = filePath + "\\" + filename.substring(0, filename.lastIndexOf('.')) + ".output\\";
		
		if (file.exists()) {
			File folder = new File(outputDir);
			if (!folder.exists()) {
				folder.mkdir();
			}
		}
		
		try {
			DataInputStream stream = new DataInputStream(new FileInputStream(file));
			GameResourceReader reader = new GameResourceReader(stream);
			try {
				reader.extractGameResource(outputDir);
			} catch (GameResourceException e) {
				return "failed: " + e.getMessage();
			}
			stream.close();
			
			return "done!";
			
		} catch (FileNotFoundException e) {
			return "failed: File not found!";
		} catch (IOException e) {
			return "failed: " + e.getMessage();
		}
	}

	public static String createGameResource(String foldername) {
		GameResourceWriter writer = new GameResourceWriter();
		try {
			writer.createGameResource(foldername);
		} catch (GameResourceException e) {
			return "failed: " + e.getMessage();
		}
		return "done!";
	}
}
