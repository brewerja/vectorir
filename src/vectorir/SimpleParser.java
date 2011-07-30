package vectorir;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

// Modeled after the SAX Quickstart found at:
// http://sax.sourceforge.net/quickstart.html

public class SimpleParser extends DefaultHandler {

	public static void main(String args[]) throws Exception {
		SimpleParser handler = new SimpleParser();
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

		// For each <document, term> pair, calculate weights (tf-idf).
		handler.corpus.calculateTermWeights();

		System.out.println(handler.corpus.getNumTerms());

		// Serialize the Corpus
		System.out.print("Serializing Corpus...");
		try {
			FileOutputStream fout = new FileOutputStream("corpus.dat");
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(handler.corpus);
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.print("Done.\n");
		
		handler.corpus.featureSelection("earn", 10);

	}

	private Corpus corpus = new Corpus();
	private String currentTag;
	private Document currentDocument;
	private StringBuilder sb;
	private String category = null;

	public SimpleParser() {
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
			if (train.equals("TRAIN") && topics.equals("YES"))
				corpus.addTrainingDoc(id);
			else if (train.equals("TEST") && topics.equals("YES"))
				corpus.addTestDoc(id);
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
			// Remove excess white space and chop anything after the final
			// period.
			String bodyText = sb.toString().replaceAll("\\s+", " ");
			this.currentDocument.setBody(bodyText.substring(0, bodyText.lastIndexOf(".") + 1));

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
				// if (!corpus.stopWord(s))
				tokenList.add(s);
			}

			this.currentDocument.setTokens(tokenList);

		} else if (qName.equals("REUTERS")) {
			this.corpus.addDocument(this.currentDocument);
			System.out.println("DOCID:" + this.currentDocument.getId());
			System.out.println("TOPICS:" + this.currentDocument.getTopics());
			System.out.println("TITLE:" + this.currentDocument.getTitle());
			System.out.println("DATELINE:" + this.currentDocument.getDateline());
			System.out.println("BODY:" + this.currentDocument.getBody());
			System.out.print("TOKENS:");
			for (String s : this.currentDocument.getTokens())
				System.out.print(s + ",");
			System.out.println("\n");
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