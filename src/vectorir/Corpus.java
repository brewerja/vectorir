package vectorir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import vectorir.Term.TermDoc;

public class Corpus implements java.io.Serializable {

	private static final long serialVersionUID = 5726961619157084200L;
	private Map<Integer, Document> documents = new HashMap<Integer, Document>();
	private Map<String, Term> terms = new HashMap<String, Term>();
	private ArrayList<Integer> trainingSet = new ArrayList<Integer>();
	private ArrayList<Integer> testSet = new ArrayList<Integer>();

	// Documents Methods
	public Document getDocument(Integer key) {
		return documents.get(key);
	}

	public void addDocument(Document doc) {
		documents.put(doc.getId(), doc);
	}

	public int getNumDocuments() {
		return documents.size();
	}

	// Terms Methods
	public Term getTerm(String key) {
		if (terms.containsKey(key))
			return terms.get(key);
		else
			return new Term(0);
	}

	public void addTerm(String term, Document doc, int pos) {
		terms.put(term, new Term(term, doc, pos));
	}

	public int getNumTerms() {
		return terms.size();
	}

	public Map<String, Term> getTerms() {
		return terms;
	}

	// Corpus Methods
	public void stemTokens() {
		Stemmer stemmer = new Stemmer();
		for (Document doc : documents.values()) {
			ArrayList<String> tokens = doc.getTokens();
			for (int i = 0; i < tokens.size(); i++) {
				String t = tokens.get(i);
				stemmer.add(t.toCharArray(), t.length());
				stemmer.stem();
				String stem = stemmer.toString();
				// The stem is added to the overall corpus set of terms if it
				// has not already been added.
				if (terms.containsKey(stem)) {
					terms.get(stem).foundAgain(doc, i);
				} else {
					terms.put(stem, new Term(stem, doc, i));
				}
			}
		}

		for (String stem : terms.keySet())
			System.out.print(stem + ",");
		System.out.println("\nTerms:" + terms.size());
	}

	public void calculateTermWeights() {

		// Iterate through the vocabulary.
		for (String termString : terms.keySet()) {
			Term t = terms.get(termString);
			Map<Integer, TermDoc> posting = t.getPostings();

			// Iterate through the docs in which the selected term is found.
			for (Integer docId : posting.keySet()) {

				TermDoc td = posting.get(docId);
				Document doc = this.getDocument(docId);

				final double a = 0.4;
				double ntf = a + (1 - a) * (double) td.getFreq() / (double) doc.getMaxTermFreq();
				double idf = Math.log((double) documents.size() / (1 + (double) t.getDocFreq()));
				doc.addWeight(termString, ntf * idf);
			}
		}

		for (Document doc : documents.values())
			doc.setEuclideanDistance();
	}

	public void clearTokens() {
		for (Document doc : this.documents.values())
			doc.clearTokens();

	}

	// Stop word list from:
	// http://www.textfixer.com/resources/common-english-words-with-contractions.txt
	private String stopStrings[] = { "tis", "twas", "a", "able", "about", "across", "after", "aint", "all", "almost",
			"also", "am", "among", "an", "and", "any", "are", "arent", "as", "at", "be", "because", "been", "but",
			"by", "can", "cant", "cannot", "could", "couldve", "couldnt", "dear", "did", "didnt", "do", "does",
			"doesnt", "dont", "either", "else", "ever", "every", "for", "from", "get", "got", "had", "has", "hasnt",
			"have", "he", "hed", "hell", "hes", "her", "hers", "him", "his", "how", "howd", "howll", "hows", "however",
			"i", "id", "ill", "im", "ive", "if", "in", "into", "is", "isnt", "it", "its", "its", "just", "least",
			"let", "like", "likely", "may", "me", "might", "mightve", "mightnt", "most", "must", "mustve", "mustnt",
			"my", "neither", "no", "nor", "not", "of", "off", "often", "on", "only", "or", "other", "our", "own",
			"rather", "said", "say", "says", "shant", "she", "shed", "shell", "shes", "should", "shouldve", "shouldnt",
			"since", "so", "some", "than", "that", "thatll", "thats", "the", "their", "them", "then", "there",
			"theres", "these", "they", "theyd", "theyll", "theyre", "theyve", "this", "tis", "to", "too", "twas", "us",
			"wants", "was", "wasnt", "we", "wed", "well", "were", "were", "werent", "what", "whatd", "whats", "when",
			"when", "whend", "whenll", "whens", "where", "whered", "wherell", "wheres", "which", "while", "who",
			"whod", "wholl", "whos", "whom", "why", "whyd", "whyll", "whys", "will", "with", "wont", "would",
			"wouldve", "wouldnt", "yet", "you", "youd", "youll", "youre", "youve", "your", "reuter" };

	private Set<String> stopWordSet = new HashSet<String>(Arrays.asList(stopStrings));

	public boolean stopWord(String s) {
		return this.stopWordSet.contains(s);
	}

	public ArrayList<Integer> getTrainingSet() {
		return trainingSet;
	}

	public void addTrainingDoc(Integer doc) {
		this.trainingSet.add(doc);
	}

	public ArrayList<Integer> getTestSet() {
		return testSet;
	}

	public void addTestDoc(Integer doc) {
		this.testSet.add(doc);
	}
}
