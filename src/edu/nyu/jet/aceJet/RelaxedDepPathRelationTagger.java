package edu.nyu.jet.aceJet;

import edu.nyu.jet.ice.models.DepPathRegularizer;
import edu.nyu.jet.ice.utils.IceUtils;
import edu.nyu.jet.JetTest;
import edu.nyu.jet.parser.SyntacticRelationSet;
import edu.nyu.jet.refres.Resolve;
import edu.nyu.jet.ice.relation.PathRelationExtractor;
import edu.nyu.jet.tipster.Document;
import edu.nyu.jet.tipster.ExternalDocument;
import edu.nyu.jet.zoner.SentenceSet;
import edu.nyu.jet.aceJet.AnchoredPath;
import opennlp.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * a relaxed relation tagger based on dependency paths and argument types, as produced by Jet ICE.
 */

public class RelaxedDepPathRelationTagger {

	final static Logger logger = LoggerFactory.getLogger(RelaxedDepPathRelationTagger.class);

	static Document doc;
	static AceDocument aceDoc;
	static String currentDoc;
	static PathRelationExtractor pathRelationExtractor;
	static boolean searchMode = true;
	private static DepPathRegularizer pathRegularizer = new DepPathRegularizer();

	// model: a map from AnchoredPath strings to relation types
	static Map<String, String> model = null;

	/**
	 * relation 'decoder': identifies the relations in document 'doc' (from file name 'currentDoc') and adds them as
	 * AceRelations to AceDocument 'aceDoc'.
	 */

	public static void findRelations(String currentDoc, Document d, AceDocument ad) {
		doc = d;
		RelationTagger.doc = d;
		doc.relations.addInverses();
		aceDoc = ad;
		RelationTagger.docName = currentDoc;
		SentenceSet sentences = new SentenceSet(doc);
		RelationTagger.relationList = new ArrayList<AceRelation>();
		// RelationTagger.findEntityMentions (aceDoc);
		AceMention[] ray = aceDoc.allMentionsList.toArray(new AceMention[0]);
		Arrays.sort(ray);
		for (int i = 0; i < ray.length - 1; i++) {
			for (int j = 1; j < 5 && i + j < ray.length; j++) {
				AceMention m1 = ray[i];
				AceMention m2 = ray[i + j];
				// if two mentions co-refer, they can't be in a relation
				// if (!canBeRelated(m1, m2)) continue;
				// if two mentions are not in the same sentence, they can't be in a relation
				if (!sentences.inSameSentence(m1.getJetHead().start(), m2.getJetHead().start()))
					continue;

				// System.out.println(doc.relations);

				predictRelation(m1, m2, doc.relations);
			}
		}
		// combine relation mentions into relations
		RelationTagger.relationCoref(aceDoc);
		RelationTagger.removeRedundantMentions(aceDoc);
	}

	/**
	 * load the model used by the relation tagger. Each line consists of an AnchoredPath [a lexicalized dependency path
	 * with information on the endpoint types], a tab, and a relation type.
	 */

	static void loadPosAndNegModel(String posModelFile, String negModelFile, String embeddingFile) throws IOException {
		pathRelationExtractor = new PathRelationExtractor();
		pathRelationExtractor.loadRules(posModelFile);
		pathRelationExtractor.loadNeg(negModelFile);
		pathRelationExtractor.loadEmbeddings(embeddingFile);
	}

	/**
	 * use dependency paths to determine whether the pair of mentions bears some ACE relation; if so, add the relation to
	 * relationList.
	 */

	private static void predictRelation(AceMention m1, AceMention m2, SyntacticRelationSet relations) {
		// compute path
		int h1 = m1.getJetHead().start();
		int h2 = m2.getJetHead().start();
		String path = EventSyntacticPattern.buildSyntacticPath(h1, h2, relations);

		// logger.info(path);

		if (path == null)
			return;
		path = AnchoredPath.reduceConjunction(path);

		if (path == null)
			return;
		path = AnchoredPath.lemmatizePath(path); // telling -> tell, does -> do, watched -> watch, etc.

		// look up path in model
		Event event = new Event("UNK", new String[] { pathRegularizer.regularize(path), m1.getType(), m2.getType() });
		String outcome = pathRelationExtractor.predict(event);
		if (outcome == null)
			return;
		// if (!RelationTagger.blockingTest(m1, m2)) return;
		// if (!RelationTagger.blockingTest(m2, m1)) return;
		boolean inv = outcome.endsWith("-1");
		outcome = outcome.replace("-1", "");
		String[] typeSubtype = outcome.split(":", 2);
		String type = typeSubtype[0];
		String subtype;
		if (typeSubtype.length == 1) {
			subtype = "";
		} else {
			subtype = typeSubtype[1];
		}

		if (inv) {
			AceRelationMention mention = new AceRelationMention("", m2, m1, doc);
			System.out.println("Inverse Found " + outcome + " relation " + mention.text); // <<<
			AceRelation relation = new AceRelation("", type, subtype, "", m2.getParent(), m1.getParent());
			relation.addMention(mention);
			RelationTagger.relationList.add(relation);
		} else {
			AceRelationMention mention = new AceRelationMention("", m1, m2, doc);
			System.out.println("Found " + outcome + " relation " + mention.text); // <<<
			AceRelation relation = new AceRelation("", type, subtype, "", m1.getParent(), m2.getParent());
			relation.addMention(mention);
			RelationTagger.relationList.add(relation);
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 5) {
			System.err.println(RelaxedDepPathRelationTagger.class.getName()
					+ "propsFile rulesFile embeddingsFile txtFileList outputFileList");
			System.exit(-1);
		}

		String propsFile = args[0];
		String rulesFile = args[1];
		String embeddingsFile = args[2];
		String txtFileList = args[3];
		String outputFileList = args[4];

		String[] txtFiles = IceUtils.readLines(txtFileList);
		String[] outputFiles = IceUtils.readLines(outputFileList);

		if (txtFiles.length != outputFiles.length) {
			System.err.println("Length of txt, output, and answer files should equal");
			System.exit(-1);
		}

		JetTest.initializeFromConfig(propsFile);
		loadPosAndNegModel(rulesFile + ".pos", rulesFile + ".neg", embeddingsFile);

		if (searchMode) {
			for (int replace = 1; replace < 2; replace++) {
				for (int insert = 1; insert < 2; insert++) {
					for (int delete = 1; delete < 2; delete++) {
						pathRelationExtractor.updateWeights(replace / 5.0, insert / 5.0, delete / 5.0);
						String parameterString = String.format("replace:%.2f\tinsert:%.2f\tdelete:%.2f", replace / 5.0,
								insert / 5.0, delete / 5.0);

						for (int i = 0; i < txtFiles.length; i++) {
							Resolve.ACE = true;
							String fileName = txtFiles[i];
							ExternalDocument doc = new ExternalDocument("sgml", fileName);
							doc.setAllTags(true);
							doc.open();
							AceDocument aceDoc = Ace.processDocument(doc, "doc", fileName, ".");

							findRelations(fileName, doc, aceDoc);

							PrintWriter pw = new PrintWriter(new FileWriter(outputFiles[i]));
							aceDoc.write(pw, doc);
							pw.close();

							String triplesFileName = outputFiles[i].replace("sgm.apf", "triples");
							PrintWriter triplesWriter = new PrintWriter(triplesFileName, JetTest.encoding);
							List<String> triples = APFtoTriples.makeTriples(doc, aceDoc);
							for (String s : triples) {
								triplesWriter.println(s);
								System.out.println("Wrote triple " + s);
							}
							triplesWriter.close();
						}

						System.err.println("[TUNING]\t" + parameterString + "\t");

						combineTriples.main(args);
						ScoreAceTriples.main(args);
					}
				}
			}
		}
	}
}
