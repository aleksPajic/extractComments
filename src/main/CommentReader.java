package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class CommentReader {

	static String repositoriumURL;
	static String rootFolderPath;
	static String pathToExportFile = "extractedComments.txt";
	static String pathToDetailsFile = "detailsFile.txt";
	static List<String> lines = new ArrayList<String>();
	static List<String> allComments = new ArrayList<String>();

	public static void main(String[] args) {
		String repositoriums[] = { 
				"https://github.com/yemount/pose-animator", 
				"https://github.com/compact/space",
				"https://github.com/jdrews/spaceplannr",
				"https://github.com/ibmcnxdev/customizer-sample-apps",
				"https://github.com/Ctrl-Alt-Tec/Website"
			};
		String rootFolderPaths[] = { 
				"pose-animator",
				"space",
				"spaceplannr",
				"customizer-sample-apps",
				"Website"
			};
		for (int i = 0; i < repositoriums.length; i++) {
			repositoriumURL = repositoriums[i];
			rootFolderPath = rootFolderPaths[i];
			proccessProject();
		}
	}

	public static void proccessProject() {
		goThroughFilesAndExtract(rootFolderPath);
		printToFile(pathToExportFile, lines);
		lines.clear();
	}

	public static boolean extractComments(String filePath) {
		String comment = "";
		try {
			FileInputStream in = new FileInputStream(filePath);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			StringBuilder stringBuilder = new StringBuilder();
			Pattern pattern = Pattern.compile("(//.*)|(/\\*[\\s\\S]*?\\*/)|(/\\*\\*[\\s\\S]*?\\*/)"); 
			
			ArrayList<Integer> lineNumbers = new ArrayList<>();
			int counter = 1;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line + "\n");
				if (line.contains("//") || line.contains("/*")) {
					line = line.trim();
					if(!(line.startsWith("*") && line.contains("://")))
						lineNumbers.add(counter);
				}
				counter++;
			}

			String text = stringBuilder.toString();
			Matcher matcher = pattern.matcher(text);
			counter = 0;

			while (matcher.find()) {
				comment = matcher.group();
				// comment = comment.replace('\n', ' ');
				comment = comment.replaceAll("\n", "\\\\n");
				if (comment.length() > 2 && comment.charAt(1) == '/') { // case //
					comment = comment.substring(2);
				} else { // case /* */
					if (comment.length() > 5 && comment.charAt(2) == '*')
						comment = comment.substring(3, comment.length() - 2);
					else if (comment.length() > 4)
						comment = comment.substring(2, comment.length() - 2);
					else
						continue;
				}
				comment = makeSubstring(comment); // change order of these lines in case we want to keep first space
													// sign in comment
				comment = comment.trim(); // change order of these lines in case we want to keep first space sign in
											// comment
				if(isCommentNotValid(comment)) 
				{
					counter++;
					continue;
				}	
				
				lines.add(makeFileLine(comment, filePath.replace('\\', '/'), lineNumbers.get(counter)));
				allComments.add(new String(comment));
				counter++;
			}

			
			// checking if we extraced any comment for file
			return counter > 0;

		} catch (Exception x) {
			System.err.println("Comments content: " + comment);
			System.err.println("File: " + filePath);
			System.err.println(x);
		}
		return false;
	}
	
	public static boolean isCommentNotValid(String comment) {		
		return allComments.contains(comment) || comment.contains("@license") || comment.contains("Copyright (c)");
	}

	public static String makeSubstring(String str) {
		if (str.startsWith("\\n")) {
			str = str.substring(2);
		}

		if (str.endsWith("\\n")) {
			str = str.substring(0, str.length() - 2);
		}

		return str;
	}

	public static String makeFileLine(String comment, String fileId, int lineNumber) {
		String relativePathOfFile = fileId.substring(fileId.indexOf('/'));
		String repoId = repositoriumURL;
		String sourceId = repositoriumURL + relativePathOfFile;
		String commentId = repoId + "/" + sourceId + "/" + lineNumber;
		return "EN\t" + "JavaScript\t" + repoId + "\t" + sourceId + "\t" + commentId + "\t" + comment;
	}

	public static void goThroughFilesAndExtract(String sDir) {
		File[] faFiles = new File(sDir).listFiles();
		for (File file : faFiles) {
			if (file.getName().matches("^.*\\.(js)$")) {
				if (extractComments(file.getPath())) {
					writeDetailsIntoFile(file.getPath());
				}
			}
			if (file.isDirectory()) {
				goThroughFilesAndExtract(file.getPath());
			}
		}		
	}

	public static void printToFile(String pathToExportFile, List<String> lines) {
		try {
			FileWriter myWriter = new FileWriter(pathToExportFile, true);
			for (int i = 0; i < lines.size(); i++) {
				myWriter.write(lines.get(i) + "\n");
			}
			myWriter.close();
			System.out.println("Successfully wrote to the file.");
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

	public static void writeDetailsIntoFile(String pathToFile) {
		try {
			FileWriter myWriter = new FileWriter(pathToDetailsFile, true);
			String nameOfUser = "yemount";
			String relativePathOfFile = pathToFile.substring(pathToFile.indexOf('\\'));
			relativePathOfFile = relativePathOfFile.replace('\\', '/');
			String URLToFile = repositoriumURL + "/blob/master" + relativePathOfFile;
			Path path = Paths.get(pathToFile);
			long lineCount = Files.lines(path).count();
			String sourceIdFile = repositoriumURL + relativePathOfFile;
			myWriter.write(repositoriumURL + "\t" + sourceIdFile + "\t" + lineCount + "\t" + URLToFile + "\n");

			myWriter.close();
			System.out.println("Successfully wrote to the file.");
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

}
