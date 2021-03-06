package edu.nyu.jet.ice.relation;

import java.io.*;
import java.util.*;
import edu.nyu.jet.aceJet.*;
import edu.nyu.jet.tipster.*;

/**
 * converts a document with entities and relations annotated by MAE into standard
 * APF format.
 */

public class MaeToApf {

	static int entityCount = 0;
	static int relationCount = 0;
	static String docId = "doc";
	static ExternalDocument doc;
	static boolean trace = false;
	// if tagQuants is false, quant tags (quantities) are ignored
	// if tagQuants is true, then quant tags take precedence over
	//                       arg tags in designating relation arguments
	static boolean tagQuants = false;

	/**
	 *  converts a single document to APF.  Takes three arguments:
	 *  maeFile:  XML file produced by MAE                   <br>
	 *  textFile:  original source                           <br>
	 *  apfFile:  APF file (output)                          <br>
	 */

	public static void main (String[] args) throws IOException {

		String maeFile = args[0];
		String txtFile = args[1];
		doc = new ExternalDocument("sgml", txtFile);
		doc.open();
		String apfFile = args[2];

		BufferedReader maeReader = new BufferedReader (new FileReader (maeFile));
		PrintWriter apf = new PrintWriter(apfFile);

		// decode tags produced by MAE
		List<Map<String, String>> tags = new ArrayList<Map<String, String>>();
		String line;
		boolean readingTags = false;
		while ((line = maeReader.readLine()) != null) {
			if (line.equals("</TAGS>"))
				break;
			if (readingTags)
			tags.add(readTag(line));
			if (line.equals("<TAGS>"))
				readingTags = true;
		}

		// create ACE document
		AceDocument aceDoc = new AceDocument (txtFile, "MAE", docId, doc.text());

		// convert tags from MAE into entities and relations
		for (Map<String, String> tag : tags)
			if (tag.get("tagType").startsWith("arg") ||
			    tag.get("tagType").startsWith("quant")) 
				addEntity(aceDoc, tag);
		for (Map<String, String> tag : tags)
			if (tag.get("tagType").equals("relation"))
				addRelation(aceDoc, tag);
				
		// write APF
		aceDoc.write(apf, doc);
	}

	/**
	 *  decode a single XML tag as generated by MAE into a map from features
	 *  to values.  The type of the tag is included as the value of the
	 *  tagType feature.  This is not a general XML tag reader;  it is
	 *  only works for the spacing used by MAE.
	 */

	static Map<String, String> readTag (String line) {
		if (!line.startsWith("<")) {
			System.out.println("cannot find <");
			return null;
		}
		line = line.substring(1);
		int blank = line.indexOf(' ');
		if (blank < 0) {
			System.out.println("cannot find blank");
			return null;
		}
		Map<String, String> map = new HashMap< String, String>();
		String type = line.substring(0, blank);
		map.put("tagType", type);
		if (trace) System.out.println("type = " + type);
		line = line.substring(blank + 1);
		while (!line.startsWith("/>")) {
			int equals = line.indexOf('=');
			if (equals < 0) {
				System.out.println("cannot find =");
				return null;
			}
			String feature = line.substring(0, equals);
			if (trace) System.out.println("feature = " + feature);
			line = line.substring(equals + 1);
			if (!line.startsWith("\"")) {
				System.out.println("cannot find \"");
				return null;
			}
			line = line.substring(1);
			int quote = line.indexOf('"');
			String value = line.substring(0, quote);
			if (trace) System.out.println("value = " + value);
			map.put(feature, value);
			line = line.substring(quote + 2);
		}
		return map;
	}

	/**
	 *  convert a MAE relation tag into an ACE relation
	 */

	static void addRelation (AceDocument aceDoc, Map<String, String> relationTag) {
		relationCount++;
		String relationId = docId + "-R1";
		String relMentionId = relationId + "-1";
		String relType = relationTag.get("type");
		if (relType == null)
			System.out.println("No type specified in relation tag.");
		else
			relType = relType.toUpperCase();
		int start = Integer.valueOf(relationTag.get("start")) - 1;
		int end = Integer.valueOf(relationTag.get("end")) - 1;
		Span relSpan = new Span(start, end);
		List<AceEntity> arg1list = new LinkedList<AceEntity>();
		List<AceEntity> arg2list = new LinkedList<AceEntity>();
		// does this relation have a quant2 argument?  if so, set 'arg2isQuant'
		boolean arg2isQuant = false;
		for (AceEntity e : aceDoc.entities) {
			Span span = e.mentions.get(0).head;
			if (span.within(relSpan)) {
			    if (e.type.equals("quant2")) {
				arg2isQuant = true;
			    }
			}
		}
		for (AceEntity e : aceDoc.entities) {
			Span span = e.mentions.get(0).head;
			if (span.within(relSpan)) {
				if (e.type.equals("arg1")) {
					arg1list.add(e);
				} else if (e.type.equals("arg2") && (!tagQuants || !arg2isQuant)) {
					arg2list.add(e);
				} else if (e.type.equals("quant2") && tagQuants) {
					arg2list.add(e);
				}
			}
		}
		if (arg1list.isEmpty())
		    System.out.println ("Missing arg1 in " + relType + " relation.");
		if (arg2list.isEmpty())
		    System.out.println ("Missing arg2 in " + relType + " relation.");
		for (AceEntity arg1 : arg1list) {
		    AceEntityMention arg1mention = arg1.mentions.get(0);
		    for (AceEntity arg2 : arg2list) {
			AceEntityMention arg2mention = arg2.mentions.get(0);
			AceRelation aceRel = new AceRelation(relationId, relType, "", "relClass", arg1, arg2);
			AceRelationMention aceRelMen = new AceRelationMention (relMentionId, arg1mention, arg2mention, doc);
			aceRel.addMention(aceRelMen);
			aceDoc.addRelation(aceRel);
		    }
		}
	}

	/**
	 *  convert a MAE arg tag into an ACE entity
	 */

	static void addEntity (AceDocument aceDoc, Map<String, String> argTag) {
		entityCount++;
		String entityId = docId + "-E" + entityCount;
		String mentionId = entityId + "-1";
		int start = Integer.valueOf(argTag.get("start")) - 1;
		int end = Integer.valueOf(argTag.get("end")) - 1;
		Span span = new Span(start, end);
		String entityType = argTag.get("tagType");

		AceEntity entity = new AceEntity (entityId, entityType, "entitySubtype", false);
		AceEntityMention mention = new AceEntityMention(mentionId, "mentionType", /*extent*/ span, span, doc.text());
		entity.addMention(mention);
		aceDoc.addEntity(entity);
	}

}
