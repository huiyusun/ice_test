package edu.nyu.jet.ice.models;

import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * PathMatcher is a Edit-Distance-based matcher that produces an alignment and an alignment score between two
 * MatcherPaths using the generalized Levenshtein algorithm. It can optionally use word embeddings to compute the
 * substitution cost, if embeddings is set.
 */
public class PathMatcher {

	private TObjectDoubleHashMap weights = new TObjectDoubleHashMap();
	private TObjectDoubleHashMap labelWeights = new TObjectDoubleHashMap();
	private TObjectDoubleHashMap wordWeights = new TObjectDoubleHashMap();

	private Map<String, double[]> embeddings = null;

	public static double labelMismatchCost = 2.5;

	public PathMatcher() {
		setWeights();
		setLabelWeights();
		setWordWeights();
	}

	public void setWeights() {
		weights.put("replace", 0.5);
		weights.put("insert", 0.25);
		weights.put("delete", 1.0);
	}

	public void setLabelWeights() {
		// dependency count >= 1000
		labelWeights.put("pobj", 0.2);
		labelWeights.put("prep", 0.2);
		labelWeights.put("nsubj-1", 0.2);
		labelWeights.put("appos", 0.2);
		labelWeights.put("nn-1", 0.2);
		labelWeights.put("dobj", 0.2);
		labelWeights.put("poss-1", 0.2);
		labelWeights.put("nn", 0.2);
		labelWeights.put("nsubj", 0.2);
		labelWeights.put("pobj-1", 0.2);
		labelWeights.put("prep-1", 0.2);

		// 500 <= dependency count < 1000 (not good?)
		// labelWeights.put("poss", 0.4);
		// labelWeights.put("amod-1", 0.4);
		// labelWeights.put("dobj-1", 0.4);
		// labelWeights.put("amod", 0.4);
		// labelWeights.put("cop", 0.4);

		// 100 <= dependency count < 500 (not good?)
		// labelWeights.put("ccomp", 0.8);
		// labelWeights.put("ccomp-1", 0.8);
		// labelWeights.put("postloc", 0.8);
		// labelWeights.put("dep", 0.8);
		// labelWeights.put("dep-1", 0.8);
		// labelWeights.put("xcomp", 0.8);
		// labelWeights.put("parataxis", 0.8);
		// labelWeights.put("advmod", 0.8);
		// labelWeights.put("tmod", 0.8);
		// labelWeights.put("advmod-1", 0.8);
		// labelWeights.put("iobj", 0.8);
		// labelWeights.put("infmod", 0.8);

		// 50 <= dependency count < 100
		// labelWeights.put("appos-1", 1.5);
		// labelWeights.put("agent", 1.5);
		// labelWeights.put("objcomp", 1.5);
		// labelWeights.put("cop-1", 1.5);
		// labelWeights.put("pcomp", 1.5);
		// labelWeights.put("vch", 1.5);
		// labelWeights.put("tmod-1", 1.5);
		// labelWeights.put("purpcl", 1.5);

		// 10 <= depedency count < 50
		// labelWeights.put("iobj-1", 1.8);
		// labelWeights.put("agent-1", 1.8);
		// labelWeights.put("csubj-1", 1.8);
		// labelWeights.put("whadvmod-1", 1.8);
		// labelWeights.put("punct-1", 1.8);
		// labelWeights.put("pcomp-1", 1.8);
		// labelWeights.put("det-1", 1.8);
		// labelWeights.put("num", 1.8);
		// labelWeights.put("acomp", 1.8);
		// labelWeights.put("num-1", 1.8);

		// dependency < 10
		// labelWeights.put("xcomp-1", 2.0);
		// labelWeights.put("acomp-1", 2.0);
		// labelWeights.put("parataxis-1", 2.0);
		// labelWeights.put("cc", 2.0);
		// labelWeights.put("objcomp-1", 2.0);
		// labelWeights.put("purpcl-1", 2.0);
		// labelWeights.put("combo", 2.0);
		// labelWeights.put("number-1", 2.0);
		// labelWeights.put("acomp-1", 2.0);
		// labelWeights.put("det", 2.0);
		// labelWeights.put("infmod-1", 2.0);
		// labelWeights.put("preconj-1", 2.0);
		// labelWeights.put("postloc-1", 2.0);
		// labelWeights.put("cleft", 2.0);
		// labelWeights.put("vch-1", 2.0);
		// labelWeights.put("sccomp", 2.0);
		// labelWeights.put("possessive", 2.0);
		// labelWeights.put("quantmod-1", 2.0);
		// labelWeights.put("mark-1", 2.0);
		// labelWeights.put("quantmod", 2.0);
		// labelWeights.put("csubj", 2.0);
	}

	public void setWordWeights() {
		wordWeights.put("work", 1.5);
		wordWeights.put("chairman", 1.5);
		wordWeights.put("executive", 1.5);
		wordWeights.put("employee", 1.5);
	}

	public void setEmbeddings(Map<String, double[]> embeddings) {
		this.embeddings = embeddings;
	}

	public void updateLabelMismatchCost(double cost) {
		labelMismatchCost = cost;
	}

	public void updateWeights(double replace, double insert, double delete) {
		weights.put("replace", replace);
		weights.put("insert", insert);
		weights.put("delete", delete);
	}

	public double matchPaths(String path1, String path2) {
		MatcherPath matcherPath1 = new MatcherPath(path1);
		MatcherPath matcherPath2 = new MatcherPath(path2);

		return matchPaths(matcherPath1, matcherPath2);
	}

	public double matchPaths(MatcherPath matcherPath1, MatcherPath matcherPath2) {
		int len1 = matcherPath1.nodes.size();
		int len2 = matcherPath2.nodes.size();

		if (len1 == 1 && len2 == 1) {
			return matcherPath1.nodes.get(0).label.equals(matcherPath2.nodes.get(0).label)
					&& matcherPath1.arg1Type.equals(matcherPath2.arg1Type) && matcherPath1.arg2Type.equals(matcherPath2.arg2Type)
							? 0 : 1;
		}

		double[][] dp = new double[len1 + 1][len2 + 1];

		for (int i = 0; i <= len1; i++) {
			dp[i][0] = i;
		}

		for (int j = 0; j <= len2; j++) {
			dp[0][j] = j;
		}

		// iterate though, and check last char
		for (int i = 0; i < len1; i++) {
			MatcherNode c1 = matcherPath1.nodes.get(i);
			for (int j = 0; j < len2; j++) {
				MatcherNode c2 = matcherPath2.nodes.get(j);

				// if last two chars equal
				if (c1.equals(c2)) {
					// update dp value for +1 length
					dp[i + 1][j + 1] = dp[i][j];
				} else {
					double labelWeight = labelWeights.containsKey(c2.label) ? labelWeights.get(c2.label) : 1;
					double wordWeight = wordWeights.containsKey(c2.token) ? wordWeights.get(c2.token) : 1;
					double insertLabelWeight = labelWeights.containsKey(c1.label) ? labelWeights.get(c1.label) : 1;
					double insertWordWeight = wordWeights.containsKey(c1.token) ? wordWeights.get(c1.token) : 1;

					double replaceLabelCost = c1.label.equals(c2.label) ? 1 : labelMismatchCost;
					double replaceWordCost = 1 - WordEmbedding.similarity(c1.token, c2.token);
					// if (c1.token.equals("distribute") || c2.token.equals("distribute")) {
					// System.err.println("[LOG] " + c1.token + " " + c2.token + " " + replaceCost);
					// }
					double replace = dp[i][j] + weights.get("replace") * replaceLabelCost * replaceWordCost * labelWeight;
					double insert = dp[i][j + 1] + weights.get("insert") * insertLabelWeight * insertWordWeight;
					double delete = dp[i + 1][j] + weights.get("delete") * labelWeight * wordWeight;

					double min = replace > insert ? insert : replace;
					min = delete > min ? min : delete;
					dp[i + 1][j + 1] = min; // smallest of replace, insert and delete

					// System.out.println(c2.label + labelWeight);
					// System.out.println(c1.label + insertLabelWeight);
					// System.out.println(replacePenalty);
					// System.out.println(c1.token + c2.token + replaceCost);
					// System.out.println("replace " + replace);
					// System.out.println("insert " + insert);
					// System.out.println("delete " + delete);
				}
			}
		}

		return matcherPath1.arg1Type.equals(matcherPath2.arg1Type) && matcherPath1.arg2Type.equals(matcherPath2.arg2Type)
				? dp[len1][len2] : Math.max(matcherPath1.length(), matcherPath2.length());
	}

}
