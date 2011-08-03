package vectorir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
				if (terms.containsKey(stem))
					terms.get(stem).foundAgain(doc, i);
				else
					terms.put(stem, new Term(stem, doc, i));
			}
		}
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

	public String[] featureSelection(String topic, int k) {
		int n = trainingSet.size();
		HashMap<String, Double> features = new HashMap<String, Double>();
		TreeMap<String, Double> sorted_features = new TreeMap<String, Double>(new ValueComparator(features));

		// Iterate through all the terms in the vocabulary.
		for (Term term : this.terms.values()) {
			int n11 = 1, n10 = 1, n00 = 1, n01 = 1;
			for (Integer id : trainingSet) {
				if (term.getPostings().containsKey(id)) { // term in doc
					if (this.getDocument(id).getTopics().contains(topic))
						n11++;
					else
						n10++;
				} else { // term not in doc
					if (this.getDocument(id).getTopics().contains(topic))
						n01++;
					else
						n00++;
				}
			}

			int n1dot = n10 + n11;
			int ndot1 = n01 + n11;
			int n0dot = n00 + n01;
			int ndot0 = n00 + n10;

			double score = 0;
			score += n11 / (double) n * Math.log(n * n11 / (double) (n1dot * ndot1));
			score += n01 / (double) n * Math.log(n * n01 / (double) (n0dot * ndot1));
			score += n10 / (double) n * Math.log(n * n10 / (double) (n1dot * ndot0));
			score += n00 / (double) n * Math.log(n * n00 / (double) (n0dot * ndot0));

			features.put(term.getString(), score);
		}

		sorted_features.putAll(features);

		// for (Map.Entry<String, Double> e : sorted_features.entrySet())
		// System.out.println(e.getValue() + ":" + e.getKey());

		String[] returnFeatures = new String[k];
		int i = 0;
		for (String t : sorted_features.keySet()) {
			returnFeatures[i] = t;
			++i;
			if (i == k)
				break;
		}
		return returnFeatures;
	}

	public Map<Integer, Boolean> testCategorization(String topic, String[] features) {
		// System.out.println("---TERMS---");
		// for (String term : features)
		// System.out.println(term);

		int n = trainingSet.size(); // Count docs.

		// Count docs in class.
		int nc = 0;
		for (Integer id : trainingSet) {
			boolean inClass = this.getDocument(id).getTopics().contains(topic);
			if (inClass)
				nc++;
		}

		double prior_c = nc / (double) n;
		double prior_cbar = (n - nc) / (double) n;

		Map<String, Double> condProb_c = new HashMap<String, Double>();
		Map<String, Double> condProb_cbar = new HashMap<String, Double>();

		// Count docs in class containing term.
		for (String term : features) {
			Set<Integer> postings = terms.get(term).getPostings().keySet();
			int nct_c = 0, nct_cbar = 0;
			for (Integer id : trainingSet) {
				boolean inClass = this.getDocument(id).getTopics().contains(topic);
				if (postings.contains(id)) { // document contains the term
					if (inClass)
						nct_c++;
					else
						nct_cbar++;
				}
			}
			// And calculate conditional probabilities.
			condProb_c.put(term, (nct_c + 1) / (double) (nc + 2));
			condProb_cbar.put(term, (nct_cbar + 1) / (double) ((n - nc) + 2));
		}

		// ---APPLY BERNOULLI----

		Map<Integer, Boolean> marked = new HashMap<Integer, Boolean>();
		for (Integer id : testSet) {
			double score_c = Math.log(prior_c);
			double score_cbar = Math.log(prior_cbar);
			for (String term : features) {
				Set<Integer> postings = terms.get(term).getPostings().keySet();
				if (postings.contains(id)) {
					score_c += Math.log(condProb_c.get(term));
					score_cbar += Math.log(condProb_cbar.get(term));
				} else {
					score_c += Math.log(1 - condProb_c.get(term));
					score_cbar += Math.log(1 - condProb_cbar.get(term));
				}
			}
			if (score_c > score_cbar)
				marked.put(id, true);
			else
				marked.put(id, false);
		}
		return marked;
	}

	public int[] getStats(String topic, Map<Integer, Boolean> marked) {
		// System.out.println("Test Set Size: " + marked.size());
		int tp = 0, tn = 0, fp = 0, fn = 0;
		for (Map.Entry<Integer, Boolean> e : marked.entrySet()) {
			Integer id = e.getKey();
			boolean actuallyInClass = this.getDocument(id).getTopics().contains(topic);
			boolean meThinksInClass = e.getValue();
			if (actuallyInClass) {
				if (meThinksInClass)
					tp++;
				else
					fn++;
			} else {
				if (meThinksInClass)
					fp++;
				else
					tn++;
			}
		}
		// System.out.println("TP:" + tp);
		// System.out.println("TN:" + tn);
		// System.out.println("FP:" + fp);
		// System.out.println("FN:" + fn);

		double precision = tp / (double) (tp + fp);
		double recall = tp / (double) (tp + fn);
		double f1 = 2 * precision * recall / (precision + recall);
		System.out.println("F1:" + f1 * 100);

		int[] stats = { tp, tn, fp, fn };
		return stats;
	}

	class ValueComparator implements Comparator<Object> {

		Map<String, Double> base;

		public ValueComparator(Map<String, Double> base) {
			this.base = base;
		}

		public int compare(Object a, Object b) {

			if (base.get(a) < base.get(b)) {
				return 1;
			} else if (base.get(a) == base.get(b)) {
				return 0;
			} else {
				return -1;
			}
		}
	}
}
