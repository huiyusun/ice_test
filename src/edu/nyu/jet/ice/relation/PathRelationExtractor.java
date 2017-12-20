package edu.nyu.jet.ice.relation;

import opennlp.model.Event;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.nyu.jet.ice.models.MatcherPath;
import edu.nyu.jet.ice.models.PathMatcher;
import edu.nyu.jet.ice.models.WordEmbedding;

/**
 * Tag relations in a document by using positive and negative dependency path rules created with ICE
 *
 * @author yhe
 */
public class PathRelationExtractor {

	public void setNegDiscount(double nDiscount) {
		negDiscount = nDiscount;
	}

	public void setMinThreshold(double minThresh) {
		minThreshold = minThresh;
	}

	public static double minThreshold = 0.5;

	public static double negDiscount = 0.8;

	private PathMatcher pathMatcher = new PathMatcher();

	private List<MatcherPath> ruleTable = new ArrayList<MatcherPath>();

	private List<MatcherPath> negTable = new ArrayList<MatcherPath>();

	public void updateWeights(double replace, double insert, double delete) {
		pathMatcher.updateWeights(replace, insert, delete);
	}

	public void updateLabelMismatchCost(double cost) {
		pathMatcher.updateLabelMismatchCost(cost);
	}

	public void loadRules(String rulesFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(rulesFile));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] parts = line.split("=");
			MatcherPath path = new MatcherPath(parts[0].trim());
			if (parts[0].contains("EMPTY")) {
				continue;
			}
			if (!path.isEmpty()) {
				path.setRelationType(parts[1].trim());
			}
			ruleTable.add(path);
		}

		br.close();
	}

	public void loadNeg(String negRulesFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(negRulesFile));
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] parts = line.split("=");
			MatcherPath path = new MatcherPath(parts[0].trim());
			if (parts[0].contains("EMPTY")) {
				continue;
			}
			if (!path.isEmpty()) {
				path.setRelationType(parts[1].trim());
			}
			negTable.add(path);
		}

		br.close();
	}

	public void loadEmbeddings(String embeddingFile) throws IOException {
		WordEmbedding.loadWordEmbedding(embeddingFile);
	}

	/**
	 * Predict the relation type of an Event. The context[] array of the Event should have the format [dependency path,
	 * arg1 type, arg2 type]
	 * 
	 * @param e
	 *          An OpenNLP context[]:label pair
	 * @return
	 */
	public String predict(Event e) {
		String[] context = e.getContext();
		String depPath = context[0];
		String arg1Type = context[1];
		String arg2Type = context[2];
		String fullDepPath = arg1Type + "--" + depPath + "--" + arg2Type;
		MatcherPath matcherPath = new MatcherPath(fullDepPath);
		double minScore = 1;
		double minNegScore = 1;
		MatcherPath minRule = null;

		// System.out.println("candidate path: " + fullDepPath);

		for (MatcherPath rule : ruleTable) { // positive paths
			// System.out.println("positive path: " + rule);

			double score = pathMatcher.matchPaths(matcherPath, rule) / rule.length(); // normalized by the length of the path
			if (score < minScore) {
				minScore = score;
				minRule = rule;
			}
		}

		MatcherPath minNegRule = null;

		if (minScore < minThreshold) {
			for (MatcherPath rule : negTable) { // negative paths
				if (!rule.getRelationType().equals(minRule.getRelationType())) {
					// continue; // e.g. ORG-AFF vs ORG-AFF-1 should be allowed, but not ORG-AFF vs GEN-AFF
				}

				// System.out.println("negative path: " + rule);

				double score = pathMatcher.matchPaths(matcherPath, rule) / rule.length();
				if (score < minNegScore) {
					minNegScore = score;
					minNegRule = rule;
				}
			}
		} else {
			return null;
		}

		if (minScore < minThreshold && minScore < minNegScore * negDiscount) {
			System.err.println("[ACCEPT] Pos Score:" + minScore);
			System.err.println("[ACCEPT] Neg Score:" + minNegScore * negDiscount);
			System.err.println("[ACCEPT] Pos Rule:" + minRule);
			System.err.println("[ACCEPT] Neg Rule:" + minNegRule);
			System.err.println("[ACCEPT] Current:" + matcherPath);
			System.err.println("[ACCEPT] Actual:" + e.getOutcome() + "\tPredicted:" + minRule.getRelationType());

			return minRule.getRelationType();
		}
		if (minScore > minNegScore * negDiscount) {
			System.err.println("[REJECT] Pos Score:" + minScore);
			System.err.println("[REJECT] Neg Score:" + minNegScore * negDiscount);
			System.err.println("[REJECT] Pos Rule:" + minRule);
			System.err.println("[REJECT] Neg Rule:" + minNegRule);
			System.err.println("[REJECT] Current:" + matcherPath);
			System.err.println("[REJECT] Actual:" + e.getOutcome() + "\tPredicted:" + minRule.getRelationType());
		}
		return null;
	}
}
