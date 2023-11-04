package main;

import java.io.File;
import java.util.Scanner;

import fileIO.ModelReader;
import fileIO.ResourceExtractor;

public class RallyToolkit {

public static void main(String[] args) {
		
		if (args.length == 0) {
			System.out.println("South Park Rally - Resource Toolkit");
			System.out.println("Usage: RallyToolkit <filename> [<filename> ...]\n");
			
			System.out.print("Please enter the filename (or type exit to close the program).\n\n>");
			Scanner scanner = new Scanner(System.in);
			String line = scanner.next();
			if (line.toLowerCase().equals("exit")) {
				System.exit(0);
			}
			scanner.close();
			args = new String[] { line };
		}
		System.out.println();
		
		for (String filename : args) {
			File file = new File(filename);
			
			System.out.print("Processing \"" + filename + "\"...");
			if (file.exists()) {
				if (file.isDirectory()) {
					System.out.println(ResourceExtractor.createGameResource(filename));
				} if (filename.endsWith(".rbh")) {
					ModelReader modelReader = new ModelReader();
					System.out.println(modelReader.extractModel(file.getAbsoluteFile()));
				} else {
					System.out.println(ResourceExtractor.extractGameResource(filename));
				}
			} else {
				System.out.println("failed: File not found!");
			}
		}
	}
}
