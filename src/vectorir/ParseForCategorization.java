package vectorir;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Map;

import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

// Modeled after the SAX Quickstart found at:
// http://sax.sourceforge.net/quickstart.html

public class ParseForCategorization extends DefaultHandler {

	public static void main(String args[]) throws Exception {
		ParseForCategorization handler = new ParseForCategorization();
		XMLReader xr = XMLReaderFactory.createXMLReader();
		xr.setContentHandler(handler);
		xr.setErrorHandler(handler);

		// Parse each file provided on the command line.
		for (int i = 0; i < args.length; i++) {
			FileReader r = new FileReader(args[i]);
			xr.parse(new InputSource(r));
		}

		// Using Porter's Stemming Algorithm, transform tokens to terms.
		handler.corpus.stemTokens();

		// Clear out token stores to save space.
		handler.corpus.clearTokens();

		// Select topic to categorize on.
		// String topic = "interest";

		// Feature selection.
		// String[] features = handler.corpus.featureSelection(topic, 50);

		// Apply Bernoulli Model.
		// Map<Integer, Boolean> marked =
		// handler.corpus.testCategorization(topic, features);

		// Print results.
		// handler.corpus.getStats(topic, marked);
		
		// -----------------BATCH RESULTS--------------------

		String[] topics = { "earn", "acq", "money-fx", "grain", "crude", "trade", "interest", "ship", "wheat", "corn" };
		ArrayList<int[]> statsList = new ArrayList<int[]>();
		for (String topic : topics) {
			System.out.println("Topic: " + topic);
			String[] features = handler.corpus.featureSelection(topic, 50);
			Map<Integer, Boolean> marked = handler.corpus.testCategorization(topic, features);
			int[] stats = handler.corpus.getStats(topic, marked);
			statsList.add(stats);
		}

		double recTotal = 0, precTotal = 0, f1Total = 0;
		double[] totals = { 0, 0, 0, 0 };
		for (int[] s : statsList) { // s = {tp, tn, fp, fn};
			double prec = s[0] / (double) (s[0] + s[2]); // tp / (tp + fp)
			precTotal += prec;
			double rec = s[0] / (double) (s[0] + s[3]); // tp / (tp + fn)
			recTotal += rec;
			double f1 = 2 * prec * rec / (prec + rec);
			f1Total += f1;
			for (int i = 0; i < 4; ++i)
				totals[i] += s[i];
			System.out.println("prec:" + Math.round(100 * prec) + ", rec:" + Math.round(100 * rec) + ", f1:"
					+ Math.round(100 * f1));
		}

		int macroPrec = (int) Math.round(100 * precTotal / topics.length);
		int macroRec = (int) Math.round(100 * recTotal / topics.length);
		int macroF1 = (int) Math.round(100 * f1Total / topics.length);
		System.out.println("Macroaveraged Precision: " + macroPrec);
		System.out.println("Macroaveraged Recall: " + macroRec);
		System.out.println("Macroaveraged F1: " + macroF1);

		int microPrec = (int) Math.round(100 * totals[0] / (totals[0] + totals[2]));
		int microRec = (int) Math.round(100 * totals[0] / (totals[0] + totals[3]));
		int microF1 = (int) Math.round(2 * microPrec * microRec / (microPrec + microRec));
		System.out.println("Microaveraged Precision: " + microPrec);
		System.out.println("Microaveraged Recall: " + microRec);
		System.out.println("Microaveraged F1: " + microF1);

		// topic = "corn";
		// int[] no_f = {1, 10, 50, 100, 150, 200, 300};
		// for (int x : no_f) {
		// System.out.println("FEATURES: " + x);
		// String[] features = handler.corpus.featureSelection(topic, x);
		// Map<Integer, Boolean> marked =
		// handler.corpus.testCategorization(topic, features);
		// handler.corpus.getStats(topic, marked);
		// }

	}

	private Corpus corpus = new Corpus();
	private String currentTag;
	private Document currentDocument;
	private StringBuilder sb;
	private String category = null;
	private boolean storeDoc = false;

	public ParseForCategorization() {
		super();
	}

	// //////////////////////////////////////////////////////////////////
	// Event handlers.
	// //////////////////////////////////////////////////////////////////

	public void startDocument() {
		System.out.println("START DOCUMENT");
	}

	public void endDocument() {
		System.out.println("END DOCUMENT");
		System.out.println(corpus.getNumDocuments() + " Document objects");
	}

	public void startElement(String uri, String name, String qName, Attributes atts) {

		this.currentTag = qName;

		if (qName.equals("REUTERS")) {
			Integer id = new Integer(atts.getValue("NEWID"));
			this.currentDocument = new Document(id);
			String train = atts.getValue("LEWISSPLIT");
			String topics = atts.getValue("TOPICS");
			if (train.equals("TRAIN") && topics.equals("YES")) {
				corpus.addTrainingDoc(id);
				storeDoc = true;
			} else if (train.equals("TEST") && topics.equals("YES")) {
				corpus.addTestDoc(id);
				storeDoc = true;
			} else
				storeDoc = false;
		} else if ((qName.equals("TITLE") || qName.equals("DATELINE") || qName.equals("BODY")))
			this.sb = new StringBuilder();
		else if (qName.equals("TOPICS"))
			this.category = qName;
		else if (qName.equals("D") && this.category != null)
			this.sb = new StringBuilder();
	}

	public void endElement(String uri, String name, String qName) {
		if (qName.equals("TOPICS"))
			this.category = null;
		else if (qName.equals("D") && this.category != null)
			this.currentDocument.addTopic(sb.toString().trim());
		else if (qName.equals("TITLE"))
			this.currentDocument.setTitle(sb.toString().trim());
		else if (qName.equals("DATELINE"))
			this.currentDocument.setDateline(sb.toString().trim());
		else if (qName.equals("BODY")) {
			// Remove excess white space.
			String title = this.currentDocument.getTitle();
			sb.append(" " + title);
			String bodyText = sb.toString().replaceAll("\\s+", " ");
			this.currentDocument.setBody(bodyText);

			String[] tokens = sb.toString()
			// Replace all dashes and slashes with white space.
					.replaceAll("[\\-\\/]", " ")
					// Remove all non letters, keeping white space.
					.replaceAll("[^a-zA-Z\\s]", "")
					// Convert text to lower case.
					.toLowerCase()
					// Split on white space.
					.split("\\s+");

			// Eliminate stop words.
			ArrayList<String> tokenList = new ArrayList<String>();
			for (String s : tokens) {
				if (!corpus.stopWord(s))
					tokenList.add(s);
			}

			this.currentDocument.setTokens(tokenList);

		} else if (qName.equals("REUTERS")) {
			if (storeDoc)
				this.corpus.addDocument(this.currentDocument);
			// System.out.println("DOCID:" + this.currentDocument.getId());
			// System.out.println("TOPICS:" + this.currentDocument.getTopics());
			// System.out.println("TITLE:" + this.currentDocument.getTitle());
			// System.out.println("DATELINE:" +
			// this.currentDocument.getDateline());
			// System.out.println("BODY:" + this.currentDocument.getBody());
			// System.out.print("TOKENS:");
			// for (String s : this.currentDocument.getTokens())
			// System.out.print(s + ",");
			// System.out.println("\n");
		}
	}

	public void characters(char ch[], int start, int length) {
		String t = this.currentTag;
		// Only store text from these tags:
		if (!(t.equals("TITLE") || t.equals("DATELINE") || t.equals("BODY") || (t.equals("D") && (this.category != null))))
			return;

		for (int i = start; i < start + length; i++) {
			switch (ch[i]) {
			case '\\':
				sb.append("\\\\");
				break;
			case '"':
				sb.append("\\\"");
				break;
			case '\n':
				sb.append(" ");
			case '\r':
				break;
			case '\t':
				break;
			default:
				sb.append(ch[i]);
				break;
			}
		}
	}

}