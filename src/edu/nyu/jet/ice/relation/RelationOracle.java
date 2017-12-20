package edu.nyu.jet.ice.relation;// -*- tab-width: 4 -*-

import edu.nyu.jet.aceJet.AnchoredPath;
import edu.nyu.jet.ice.models.DepPathMap;
import edu.nyu.jet.ice.models.IcePath;
import edu.nyu.jet.ice.models.IceRelation;
import edu.nyu.jet.ice.models.IcePath.IcePathChoice;
import edu.nyu.jet.ice.uicomps.Ice;

import java.util.*;
import java.io.*;

/**
 * Provides a mechanism for simulated active learning of relations, avoiding the need to label the same dependency paths
 * repeatedly by hand.
 * <p>
 * If there is a local file <CODE>relationOracle</CODE>, use that file to label candidate paths. If there is no entry
 * for a particular candidate, ask the user to label it and record that label for future use in file
 * <CODE>newRelationOracle</CODE>.
 */

public class RelationOracle {

	static String jetHome = System.getProperty("jetHome");
	static Set<String> knownRepr = new HashSet<String>();

	/**
	 * If a relation oracle table has been loaded, use that table to label the candidate paths on
	 * <CODE>foundPatterns</CODE>. If a candidate path is not in the table, ask the user for a label, apply that label and
	 * record that label for future use.
	 * <p>
	 * At the end, write a file <CODE>newRelationOracle</CODE> with an updated table.
	 */

	public static boolean existsRepr() {
		try {
			if (new File(jetHome + "/data/relationOracle").exists()) {
				BufferedReader br = new BufferedReader(new FileReader(new File(jetHome + "/data/relationOracle")));
				String line;

				while ((line = br.readLine()) != null) {
					knownRepr.add(line);
				}

				br.close();
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			System.err.println("IOException in RelationOracle");
			return false;
		}
	}

	public static void label(List<IcePath> foundPatterns, String relationName) {
		Set<String> knownReprRelation = new HashSet<String>();

		for (String reprLine : knownRepr) {
			String repr = reprLine.split("=")[0].trim();
			String relation = reprLine.split("=")[1].replace("-1", "").trim();
			knownReprRelation.add(repr + " = " + relation);
		}

		// label patterns
		for (IcePath fp : foundPatterns) {
			String repr = fp.getRepr();

			if (knownReprRelation.contains(repr + " = " + relationName + " : YES")) {
				fp.setChoice(IcePathChoice.YES);
			} else if (knownReprRelation.contains(repr + " = " + relationName + " : NO")) {
				fp.setChoice(IcePathChoice.NO);
			}
		}

		knownRepr.clear();
	}

	// write a relation representation to oracle file
	public static void addRepr(List<IcePath> approvedPaths, String[] seedsArr, List<IcePath> rejectedPaths,
			String relationName) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(jetHome + "/data/relationOracle"), true)); // append

			for (IcePath path : approvedPaths) { // add approved paths
				String outputPath = path.getRepr() + " = " + relationName + " : YES";
				if (!knownRepr.contains(outputPath)) {
					bw.write(outputPath + "\n");
				}
			}

			for (int i = 0; i < seedsArr.length; i++) { // add user supplied seed paths
				String outputPath = seedsArr[i].trim() + " = " + relationName + " : YES";
				if (!knownRepr.contains(outputPath)) {
					bw.write(outputPath + "\n");
				}
			}

			for (IcePath path : rejectedPaths) { // add negative paths
				String outputPath = path.getRepr() + " = " + relationName + " : NO";
				if (!knownRepr.contains(outputPath)) {
					bw.write(outputPath + "\n");
				}
			}

			knownRepr.clear();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// In case previous choices have been altered
	public static void alterChoice() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(jetHome + "/data/relationOracle")));

			// use linked hash map to keep original order
			LinkedHashMap<String, String> alteredRepr = new LinkedHashMap<String, String>();

			String line;

			while ((line = br.readLine()) != null) {
				String repr = line.split(":")[0].trim();
				String choice = line.split(":")[1].trim();

				if (alteredRepr.containsKey(repr)) {
					alteredRepr.remove(repr); // remove previous choice if there exists conflict
				}

				alteredRepr.put(repr, choice);
			}

			br.close();

			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(jetHome + "/data/relationOracle"))); // rewrite

			for (String key : alteredRepr.keySet()) {
				bw.write(key + " : " + alteredRepr.get(key) + "\n");
			}

			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// don't include previously labeled negative patterns on the list
	public static Set<String> getPreviousNegPatterns() {
		Set<String> negPatterns = new HashSet<String>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(jetHome + "/data/relationOracle")));
			String line;

			while ((line = br.readLine()) != null) {
				String repr = line.split("=")[0].trim(); // ORGANIZATION secretary PERSON
				String relation = line.split("=")[1].replace("-1", "").trim(); // ORG-AFF : YES
				String relationName = relation.split(":")[0].trim(); // ORG-AFF
				String choice = relation.split(":")[1].trim(); // YES

				if (choice.equals("NO")) {
					negPatterns.add(repr + " = " + relationName);
				}
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return negPatterns;
	}

	// don't include previously labeled positive and negative patterns on the list
	public static Set<String> getPreviousPatterns() {
		Set<String> negPatterns = new HashSet<String>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(jetHome + "/data/relationOracle")));
			String line;

			while ((line = br.readLine()) != null) {
				String repr = line.split("=")[0].trim(); // ORGANIZATION secretary PERSON
				String relation = line.split("=")[1].replace("-1", "").trim(); // ORG-AFF : YES
				String relationName = relation.split(":")[0].trim(); // ORG-AFF

				negPatterns.add(repr + " = " + relationName);
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return negPatterns;
	}

	// convert repr to LDP format
	public static void addLDP(DepPathMap depPathMap) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(jetHome + "/data/relationOracle")));
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(jetHome + "/data/relationOracleLDP"))); // rewrite
			BufferedWriter bwNeg = new BufferedWriter(new FileWriter(new File(jetHome + "/data/relationOracleLDP.neg"))); // rewrite
			String inputLine;

			while ((inputLine = br.readLine()) != null) {
				String repr = inputLine.split("=")[0].trim();
				String relation = inputLine.split("=")[1].split(":")[0].trim();

				List<String> LDP = depPathMap.findPath(repr);

				if (LDP != null) { // check if corpus has LDP
					for (String ldp : LDP) {
						String ldp1 = ldp.replaceAll("\\s", "") + " = " + relation;

						if (inputLine.split(":")[1].trim().equals("YES")) { // create LDPs from positive paths
							bw.write(ldp1 + "\n");
						} else if (inputLine.split(":")[1].trim().equals("NO")) { // create LDPs from negative paths
							bwNeg.write(ldp1 + "\n");
						}
					}

				}
			}

			br.close();
			bw.close();
			bwNeg.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}