package edu.nyu.jet.aceJet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class combineTriples {

	public static void combine() {
		try {
			BufferedReader input = null;
			BufferedReader input1 = new BufferedReader(new FileReader("/Users/nuist/jetx/data/ACE/2005/allSgmList.txt"));
			BufferedWriter output = null;

			String inputLine;

			File folder1 = new File("/Users/nuist/documents/NlpResearch/ice-eval/aceResponseApf/bc");
			File folder2 = new File("/Users/nuist/documents/NlpResearch/ice-eval/aceResponseApf/bn");
			File folder3 = new File("/Users/nuist/documents/NlpResearch/ice-eval/aceResponseApf/cts");
			File folder4 = new File("/Users/nuist/documents/NlpResearch/ice-eval/aceResponseApf/nw");
			File folder5 = new File("/Users/nuist/documents/NlpResearch/ice-eval/aceResponseApf/un");
			File folder6 = new File("/Users/nuist/documents/NlpResearch/ice-eval/aceResponseApf/wl");

			File[] listOfFiles1 = folder1.listFiles();
			File[] listOfFiles2 = folder2.listFiles();
			File[] listOfFiles3 = folder3.listFiles();
			File[] listOfFiles4 = folder4.listFiles();
			File[] listOfFiles5 = folder5.listFiles();
			File[] listOfFiles6 = folder6.listFiles();

			output = new BufferedWriter(new FileWriter("/Users/nuist/documents/NlpResearch/ice-eval/aceResponseTriples"));

			Set<String> docList = new HashSet<String>();

			while ((inputLine = input1.readLine()) != null) {
				docList.add(inputLine.split("/")[1].replaceAll(".sgm", ""));
			}

			HashSet<String> relationsSet = new HashSet<String>(); // prevents repeating triples

			// read files in each folder
			for (int i = 0; i < listOfFiles1.length; i++) {
				File file = listOfFiles1[i];
				input = new BufferedReader(new FileReader(file));

				if (file.isFile() && file.getName().endsWith(".triples")
						&& docList.contains(file.getName().replace(".triples", ""))) {
					while ((inputLine = input.readLine()) != null) {
						if (!inputLine.isEmpty()) {
							if (!relationsSet.contains(inputLine)) {
								output.write(inputLine + "\n");
							}

							relationsSet.add(inputLine);
						}
					}
				}
			}

			for (int i = 0; i < listOfFiles2.length; i++) {
				File file = listOfFiles2[i];
				input = new BufferedReader(new FileReader(file));

				if (file.isFile() && file.getName().endsWith(".triples")
						&& docList.contains(file.getName().replace(".triples", ""))) {
					while ((inputLine = input.readLine()) != null) {
						if (!inputLine.isEmpty()) {
							if (!relationsSet.contains(inputLine)) {
								output.write(inputLine + "\n");
							}

							relationsSet.add(inputLine);
						}
					}
				}
			}

			for (int i = 0; i < listOfFiles3.length; i++) {
				File file = listOfFiles3[i];
				input = new BufferedReader(new FileReader(file));

				if (file.isFile() && file.getName().endsWith(".triples")
						&& docList.contains(file.getName().replace(".triples", ""))) {
					while ((inputLine = input.readLine()) != null) {
						if (!inputLine.isEmpty()) {
							if (!relationsSet.contains(inputLine)) {
								output.write(inputLine + "\n");
							}

							relationsSet.add(inputLine);
						}
					}
				}
			}

			for (int i = 0; i < listOfFiles4.length; i++) {
				File file = listOfFiles4[i];
				input = new BufferedReader(new FileReader(file));

				if (file.isFile() && file.getName().endsWith(".triples")
						&& docList.contains(file.getName().replace(".triples", ""))) {
					while ((inputLine = input.readLine()) != null) {
						if (!inputLine.isEmpty()) {
							if (!relationsSet.contains(inputLine)) {
								output.write(inputLine + "\n");
							}

							relationsSet.add(inputLine);
						}
					}
				}
			}

			for (int i = 0; i < listOfFiles5.length; i++) {
				File file = listOfFiles5[i];
				input = new BufferedReader(new FileReader(file));

				if (file.isFile() && file.getName().endsWith(".triples")
						&& docList.contains(file.getName().replace(".triples", ""))) {
					while ((inputLine = input.readLine()) != null) {
						if (!inputLine.isEmpty()) {
							if (!relationsSet.contains(inputLine)) {
								output.write(inputLine + "\n");
							}

							relationsSet.add(inputLine);
						}
					}
				}
			}

			for (int i = 0; i < listOfFiles6.length; i++) {
				File file = listOfFiles6[i];
				input = new BufferedReader(new FileReader(file));

				if (file.isFile() && file.getName().endsWith(".triples")
						&& docList.contains(file.getName().replace(".triples", ""))) {
					while ((inputLine = input.readLine()) != null) {
						if (!inputLine.isEmpty()) {
							if (!relationsSet.contains(inputLine)) {
								output.write(inputLine + "\n");
							}

							relationsSet.add(inputLine);
						}
					}
				}
			}

			input.close();
			input1.close();
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		combine();
	}
}