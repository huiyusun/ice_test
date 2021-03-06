package edu.nyu.jet.ice.models;

import edu.nyu.jet.aceJet.AnchoredPath;
import edu.nyu.jet.ice.uicomps.Ice;
import edu.nyu.jet.ice.utils.FileNameSchema;

import java.io.*;
import java.util.*;

/**
 * DepPathMap maintains maps between depPaths, linearizations, and example sentences. 
 * It originally computed linearizations and saved them for each path. Linearization 
 * is currently handled by the DepPath class, so DepPathMap only
 * handles serialization/deserialization of the map.
 */

public class DepPathMap {
    private static DepPathMap instance = null;

    // ldp -> linearized ldp
    private HashMap<String, String> pathReprMap = new HashMap<String, String>();
    // linearized ldp -> ldp
    private HashMap<String, List<String>> reprPathMap = new HashMap<String, List<String>>();
    // ldp -> sentence
    private HashMap<String, String> pathExampleMap = new HashMap<String, String>();
    private DepPathMap() { }
    private String previousFileName = null;

    private Set<String> leftRelations = new HashSet<String>();
    {
        leftRelations.add("nsubj");
        leftRelations.add("amod");
        leftRelations.add("advmod");
        leftRelations.add("auxpass");
        leftRelations.add("cop");
        leftRelations.add("det");
        leftRelations.add("expl");
        leftRelations.add("neg");
        leftRelations.add("nn");
        leftRelations.add("num");
        leftRelations.add("number");
        leftRelations.add("poss");
        leftRelations.add("preconj");
        leftRelations.add("quantmod");
    }

    /**
    DepPathMap is a singleton which is shared across the program.
     */
    public static DepPathMap getInstance() {
        if (instance == null) {
            instance = new DepPathMap();
        }
        return instance;
    }

    public Set<String> getPathSet () {
        return pathReprMap.keySet();
    }

    /**
     *  Given a dependency path, returns its linearization as
     *  an English phrase, or <CODE>null</CODE> if this path does
     *  not occur in the current corpus.
     */

    public String findRepr(String path) {
        return pathReprMap.get(path);
    }
    public String findRepr(AnchoredPath  path) {
        return pathReprMap.get(path.toString());
    }

    /**
     *  Given a linearized English phrase derived from a
     *  dependency path in the selected corpus, returns the original path.
     */

    public List<String> findPath(String repr) {
        String norm = normalizeRepr(repr);
        List<String> paths = reprPathMap.get(norm);
        if (paths != null)
            return paths;
        String norm2 = swap12(norm);
        paths = reprPathMap.get(norm2);
        if (paths == null)
            return null;
        List<String> q = new ArrayList<String>();
        for (String p : paths)
            q.add(swap12(p));
        return q;
    }

    /**
     *  Interchanges the subscripts (1) and (2) in a path.
     */

    public String swap12 (String s) {
        String temp1 = s.replaceAll("\\(2\\)", "#");
        String temp2 = temp1.replaceAll("\\(1\\)", "(2)");
        String temp3 = temp2.replaceAll("#", "(1)");
        return temp3;
    }

    /**
     *  Given a dependency path in the current corpus, returns an
     *  example sentence containing tha path.
     */

    public String findExample(String path) {
        return pathExampleMap.get(path);
    }

    public void clear() {
        pathReprMap.clear();
        reprPathMap.clear();
        pathExampleMap.clear();
    }

    public void unpersist() {
        String fileName = FileNameSchema.getRelationReprFileName(Ice.selectedCorpusName);
        try {
            File f = new File(fileName);
            f.delete();
            previousFileName = null;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  Saves the mapping between paths, linearizations, and examples for the
     *  currently selected corpus to a file.
     */

    public void persist() {
        String fileName = FileNameSchema.getRelationReprFileName(Ice.selectedCorpusName);
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(fileName));
            for (String path : pathReprMap.keySet()) {
                if (pathExampleMap.containsKey(path)) {
                    pw.println(String.format("%s:::%s:::%s", path,
                            pathReprMap.get(path),
                            pathExampleMap.get(path)));
                }
            }
            pw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Loads the mappings between paths, linearizations, and examples for the
     *  currently selected corpus.
     */

    public boolean forceLoad() {
        String fileName = FileNameSchema.getRelationReprFileName(Ice.selectedCorpusName);
        File f = new File(fileName);
        if (!f.exists() || f.isDirectory()) return false;
        // if (previousFileName != null && previousFileName.equals(fileName)) return true; // use old data
        pathExampleMap.clear();
        pathReprMap.clear();
        reprPathMap.clear();
        try {
            BufferedReader r = new BufferedReader(new FileReader(f));
            String line = null;
            while ((line = r.readLine()) != null) {
                String[] parts = line.split(":::");
                if (parts.length != 3) continue;
                String path = parts[0];
                String repr = parts[1];
                String example = parts[2];
                pathReprMap.put(path, repr);
                String normalizedRepr = normalizeRepr(repr);
                if (!reprPathMap.containsKey(normalizedRepr)) {
                    reprPathMap.put(normalizedRepr, new ArrayList());
                }
                reprPathMap.get(normalizedRepr).add(path);
                pathExampleMap.put(path, example);
            }
            r.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        previousFileName = fileName;
        return true;
    }

    /**
     *  Loads the mappings between paths, linearizations, and examples for the
     *  currently selected corpus from a file.  Skips the load if the mappings
     *  have been previously loaded from the same file.
     */

    public boolean load() {
        String fileName = FileNameSchema.getRelationReprFileName(Ice.selectedCorpusName);
        File f = new File(fileName);
        if (!f.exists() || f.isDirectory()) return false;
        if (previousFileName != null && previousFileName.equals(fileName) && pathExampleMap.size() > 0) return true; // use old data
        pathExampleMap.clear();
        pathReprMap.clear();
        reprPathMap.clear();
        try {
            BufferedReader r = new BufferedReader(new FileReader(f));
            String line = null;
            while ((line = r.readLine()) != null) {
                String[] parts = line.split(":::");
                if (parts.length != 3) continue;
                String path = parts[0];
                String repr = parts[1];
                String example = parts[2];
                pathReprMap.put(path, repr);
                String normalizedRepr = normalizeRepr(repr);
                if (!reprPathMap.containsKey(normalizedRepr)) {
                    reprPathMap.put(normalizedRepr, new ArrayList());
                }
                reprPathMap.get(normalizedRepr).add(path);
                pathExampleMap.put(path, example);
            }
            r.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        previousFileName = fileName;
        return true;
    }

    /**
     Normalize the internal copy of DepPath representation: all lowercase, single space, trimmed.
     Used by find and add.
     */

    public static String normalizeRepr(String repr) {
        return repr.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    /**
     *  Find the English phrase most similar to 'repr' (as measured by edit
     *  distance) which is the representation of a dependency path.
     */

    public String findClosest (String repr) {
        String norm = normalizeRepr(repr);
        int distance = 1000;
        String closest = null;
        for (String p : reprPathMap.keySet()) {
            int d = minDistance (norm, p);
            if (d < distance) {
                distance = d;
                closest = p;
            }
        }
        return closest;
    }

    /**
     *  maximum length of arguments to minDistance
     */
    static final int MAX_MINDISTANCE = 100;

    private static int[][] dp = new int[MAX_MINDISTANCE][MAX_MINDISTANCE];

    /**
     * Computes the minimum edit distance between strings 'word1' and 'word2'.
     */

    public static int minDistance(String word1, String word2) {
        int len1 = word1.length();
        int len2 = word2.length();
        if (len1 >= MAX_MINDISTANCE || len2 >= MAX_MINDISTANCE) 
            return 1001;

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        //iterate through, and check last char
	for (int i = 0; i < len1; i++) {
            char c1 = word1.charAt(i);
            for (int j = 0; j < len2; j++) {
                char c2 = word2.charAt(j);

                //if last two chars equal
                if (c1 == c2) {
                    //update dp value for +1 length
                    dp[i + 1][j + 1] = dp[i][j];
                } else {
                    int replace = dp[i][j] + 1;
                    int insert = dp[i][j + 1] + 1;
                    int delete = dp[i + 1][j] + 1;

                    int min = replace > insert ? insert : replace;
                    min = delete > min ? min : delete;
                    dp[i + 1][j + 1] = min;
                }
            }
        }
        return dp[len1][len2];
    }
}
