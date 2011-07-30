package vectorir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Term implements java.io.Serializable {

	private static final long serialVersionUID = -9215956026571055086L;
	private int docFreq = 1;
	private Map<Integer, TermDoc> postings = new HashMap<Integer, TermDoc>();
	private String string;

	public Term(String term, Document doc, int pos) {
		postings.put(doc.getId(), new TermDoc(this, doc, pos));
		this.string = term;
	}

	public Term(int zero) {
		this.docFreq = 0;
		this.string = "";
	}

	public void foundAgain(Document doc, int pos) {
		// If this is the first time the term has been found in a particular
		// document, add the id to the postings, and increment the document
		// frequency.
		int docId = doc.getId();
		if (!postings.containsKey(docId)) {
			postings.put(docId, new TermDoc(this, doc, pos));
			docFreq++;
		} else
			postings.get(docId).recordPosition(doc, pos);
	}

	public int getDocFreq() {
		return docFreq;
	}

	public Map<Integer, TermDoc> getPostings() {
		return postings;
	}

	public String getString() {
		return string;
	}

	public class TermDoc implements java.io.Serializable {

		private static final long serialVersionUID = 4956168170750177461L;
		private int freq = 0;
		private ArrayList<Integer> positions = new ArrayList<Integer>();

		public TermDoc(Term t, Document doc, int pos) {
			recordPosition(doc, pos);
		}

		public void recordPosition(Document doc, int pos) {
			this.freq++;
			this.positions.add(pos);
			doc.registerTermFreq(this.freq);
		}

		public ArrayList<Integer> getPositions() {
			return positions;
		}

		public int getFreq() {
			return freq;
		}
	}
}
