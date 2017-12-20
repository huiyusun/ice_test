
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

public class RelationOracle_append {

	static String status = "";
	static String jetHome = System.getProperty("jetHome");

	static Set<String> knownRepr = new HashSet<String>();
	static Set<String> knownLDP = new HashSet<String>();

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

	public static boolean existsLDP() {
		try {
			if (new File(jetHome + "/data/relationOracleLDP").exists()) {
				BufferedReader br = new BufferedReader(new FileReader(new File(jetHome + "/data/relationOracleLDP")));
				String line;

				while ((line = br.readLine()) != null) {
					knownLDP.add(line);
				}

				br.close();
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			System.err.println("IOException in RelationOracleLDP");
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
		System.out.println("label");
	}

	// write a relation representation to oracle file
	public static void addRepr(List<IcePath> approvedPaths, String[] seedsArr, List<IcePath> rejectedPaths,
			String relationName) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(jetHome + "/data/relationOracle"), true)); // append

			for (IcePath path : approvedPaths) { // add approved paths
				String outputPath = path.getRepr() + " = " + relationName + " : YES";
				if (!knownRepr.contains(outputPath)) {
					knownRepr.add(outputPath);
					bw.write(outputPath + "\n");
				}
			}

			for (int i = 0; i < seedsArr.length; i++) { // add user supplied seed paths
				String outputPath = seedsArr[i].trim() + " = " + relationName + " : YES";
				if (!knownRepr.contains(outputPath)) {
					knownRepr.add(outputPath);
					bw.write(outputPath + "\n");
				}
			}

			for (IcePath path : rejectedPaths) { // add negative paths
				String outputPath = path.getRepr() + " = " + relationName + " : NO";
				if (!knownRepr.contains(outputPath)) {
					knownRepr.add(outputPath);
					bw.write(outputPath + "\n");
				}
			}

			System.out.println("addRepr");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// convert repr to LDP format
	public static void addLDP(DepPathMap depPathMap, String relationName, String arg1, String arg2) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(jetHome + "/data/relationOracleLDP"), true)); // append

			for (String line : knownRepr) {
				if (line.split(":")[1].trim().equals("YES")) { // only create LDPs from positive paths
					String repr = line.split("=")[0].trim();
					String relation = line.split("=")[1].split(":")[0].trim();

					if (relation.equals(relationName)) { // check if relation name match
						List<String> LDP = depPathMap.findPath(repr);

						if (LDP != null) { // check if corpus has LDP
							String[] args = LDP.get(0).split("--");

							if (args[0].trim().equals(arg1) && args[2].trim().equals(arg2)) { // check arguments
								for (String ldp : LDP) {
									String ldp1 = ldp.replaceAll("\\s", "") + " = " + relation;

									if (!knownLDP.contains(ldp1)) { // avoid duplicates
										bw.write(ldp1 + "\n");
									}
								}
							}
						}
					}
				}
			}

			System.out.println("addLDP");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}