package vectorir;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import vectorir.Term.TermDoc;

public class Query {

	public static void main(String args[]) throws FileNotFoundException,
			IOException, ClassNotFoundException {

		// Deserialize the stored Corpus object.
		System.out.println("Deserializing corpus...");
		FileInputStream fis = new FileInputStream(args[0]);
		ObjectInputStream ois = new ObjectInputStream(fis);
		Corpus corpus = (Corpus) ois.readObject();
		System.out.println(corpus.getNumDocuments()
				+ " documents loaded from the corpus.");

		// Instantiate a Query on the chosen Corpus.
		Query q = new Query(corpus);
		BufferedReader reader;

		// Take in user input until user exits by entering an empty query.
		while (true) {
			reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.print("\nEnter Query: ");
			String query = reader.readLine();
			if (query.isEmpty())
				break;

			long startTime = System.currentTimeMillis();

			if (q.prepareQuery(query)) {
				q.executeQuery();
				// Output the documents in order of similarity to the query.
				System.out.println("Results: " + q.docScores);
				long stopTime = System.currentTimeMillis();
				System.out.println(q.docScores.size() + " results ("
						+ (stopTime - startTime) / 1000.0 + " seconds)");
			}

			reader = new BufferedReader(new InputStreamReader(System.in));
			try {
				System.out.print("Enter Document ID to View: ");
				Integer docId = Integer.parseInt(reader.readLine());
				Document retrieved = corpus.getDocument(docId);
				System.out.println(retrieved.getTitle());
				System.out.print(retrieved.getDateline());
				System.out.println(" " + retrieved.getBody());
			} catch (NumberFormatException e) {
				System.err.println("Not a valid document ID.");
			} catch (NullPointerException np) {
				System.err.println("Document not found.");
			}
		}
		System.out.println("Goodbye.");

	}

	private Corpus corpus;
	private int numEmptyTokens;
	private Map<Integer, Integer> phrasePositions;
	private String[] queryTokens = null;
	private Map<String, Double> queryVector = new HashMap<String, Double>();
	private HashSet<Integer> relevantDocs = new HashSet<Integer>();
	private HashSet<Integer> nonRelevantDocs = new HashSet<Integer>();
	private String[] queryTerms;
	private Map<Integer, Double> docScores = new HashMap<Integer, Double>();
	private double queryDistance = 0.0;

	public Query(Corpus c) {
		this.corpus = c;
	}

	public boolean prepareQuery(String query) {
		// Replace all dashes and slashes with white space.
		this.queryTokens = query.replaceAll("[\\-\\/]", " ")
		// Remove all non letters except double quotes and white space.
				.replaceAll("[^a-zA-Z\\s\\\"]", "")
				// Convert text to lower case.
				.toLowerCase()
				// Split on white space.
				.split("\\s+");

		boolean inPhrase = false; // flag to distinguish L/R quote mark.
		this.numEmptyTokens = 0;
		int start = 0, finish = 0;
		this.phrasePositions = new HashMap<Integer, Integer>();

		// Find any phrases by looking for quotes (") in the terms.
		for (int i = 0; i < queryTokens.length; i++) {
			if (queryTokens[i].contains("\"")) {
				if (!inPhrase) { // L quotation found.
					inPhrase = true;
					start = i;
				} else { // R quotation found.
					inPhrase = false;
					finish = i;
					// Store the positions of the term starting a phrase in a
					// Map with the term ending that same phrase.
					phrasePositions.put(start - numEmptyTokens, finish
							- numEmptyTokens);
				}
				queryTokens[i] = queryTokens[i].replace("\"", "");
			} else if (queryTokens[i].isEmpty()) {
				// If the token is empty it should be the first token.
				// That token should be the start of a phrase.
				numEmptyTokens++;
			}
		}

		// After iterating though all the tokens, if still in a phrase, there is
		// a missing quote.
		if (inPhrase) {
			System.err.println("SYNTAX ERROR: Unmatched quotation mark in"
					+ " query OR single word eclosed in quotes.");
			return false;
		} else
			return true;
	}

	public void executeQuery() {
		// Convert query tokens to terms using the stemming algorithm.
		// This is done independent of phrasing.
		queryTerms = new String[queryTokens.length - numEmptyTokens];
		Stemmer stemmer = new Stemmer();
		for (int i = 0; i < queryTerms.length; i++) {
			String q = queryTokens[i + numEmptyTokens];
			stemmer.add(q.toCharArray(), q.length());
			stemmer.stem();
			queryTerms[i] = stemmer.toString();
		}

		final double a = 0.4;
		double ntf_query = a + (1 - a);
		

		// For each "true" term, initialize docScores entries for entries in the
		// term's postings and calculate query tf-idf values.
		for (int i = 0; i < queryTerms.length; i++) {
			String termString = queryTerms[i];
			double weight = 0.0, idf = 0.0;

			if (phrasePositions.containsKey(i)) {

				// Mash phrase together into one String and find documents that
				// contain all of the terms.
				StringBuilder sb = new StringBuilder();
				Set<Integer> docsWithAllTerms = corpus.getTerm(queryTerms[i])
						.getPostings().keySet();
				for (int t = i; t < phrasePositions.get(i); t++) {
					sb.append(queryTerms[t]);
					docsWithAllTerms.retainAll(corpus
							.getTerm(queryTerms[t + 1]).getPostings().keySet());
				}
				sb.append(queryTerms[phrasePositions.get(i)]);
				termString = sb.toString();
				Integer[] docs = docsWithAllTerms
						.toArray(new Integer[docsWithAllTerms.size()]);

				// 3. Examine resulting set of documents one-by-one to determine
				// if any contain the phrase.
				for (Integer docId : docs) {

					// Retrieve the position lists for each term in the phrase.
					Map<String, ArrayList<Integer>> positions = new HashMap<String, ArrayList<Integer>>();
					for (int t = i; t <= phrasePositions.get(i); t++)
						positions.put(queryTerms[t],
								corpus.getTerm(queryTerms[t]).getPostings()
										.get(docId).getPositions());

					boolean phraseFoundInDocument = false;
					for (Integer k : positions.get(queryTerms[i])) {
						boolean phraseFoundAtThisPosition = true;
						for (int j = i + 1; j <= phrasePositions.get(i); j++) {
							if (!positions.get(queryTerms[j]).contains(
									k + j - i)) {
								phraseFoundAtThisPosition = false;
								break;
							}
						}
						if (phraseFoundAtThisPosition) {
							phraseFoundInDocument = true;
							if (!corpus.getTerms().containsKey(termString))
								corpus.addTerm(termString,
										corpus.getDocument(docId), k);
							else
								corpus.getTerm(termString).foundAgain(
										corpus.getDocument(docId), k);
						}
					}

					// If phrase was found in the document, calculate its tf-idf
					// weight, otherwise set that weight to zero.
					Document doc = corpus.getDocument(docId);
					if (phraseFoundInDocument) {
						docScores.put(docId, 0.0);
						Term t = corpus.getTerm(termString);
						TermDoc td = t.getPostings().get(docId);

						double ntf = a + (1 - a) * (double) td.getFreq()
								/ (double) doc.getMaxTermFreq();
						idf = Math.log((double) corpus.getNumDocuments()
								/ (1 + (double) t.getDocFreq()));
						doc.addWeight(termString, ntf * idf);
					} else
						doc.addWeight(termString, 0.0);
				} // END: for (Integer docId : docs)

				// Move out of phrase.
				i = phrasePositions.get(i);

			} else {
				// If it's not a phrase, things are much easier since the df_t
				// number has already been calculated.
				Term t = corpus.getTerm(queryTerms[i]);
				for (Integer docId : t.getPostings().keySet())
					docScores.put(docId, 0.0);
				idf = Math.log(((double) corpus.getNumDocuments())
						/ (1 + corpus.getTerm(termString).getDocFreq()));
				// System.out.println(termString + " " + idf);
			}

			System.out.println("'" + termString + "'" + " found in "
					+ corpus.getTerm(termString).getDocFreq() + " documents: "
					+ corpus.getTerm(termString).getPostings().keySet());
			weight = ntf_query * idf;

			queryVector.put(termString, weight);
			queryDistance += weight * weight;
		} // END for (int i = 0; i < queryTerms.length; i++)

		queryDistance = Math.sqrt(queryDistance);
		cosineScore();
	}
	
	public void cosineScore() {
		// For each of the documents where a query term is found, calculate it's
		// cosine similarity to the query vector.
		for (Integer docId : docScores.keySet()) {
			double dotproduct = 0.0;
			for (String term : queryVector.keySet()) {
				if (corpus.getTerm(term).getPostings().containsKey(docId))
					dotproduct += queryVector.get(term)
							* corpus.getDocument(docId).getWeight(term);
			}
			double cosineSim = dotproduct
					/ (corpus.getDocument(docId).getEuclideanDistance() * queryDistance);
			docScores.put(docId, cosineSim);
		}

		docScores = sortByValue(docScores);
		System.out.println(docScores);
	}

	private static Map<Integer, Double> sortByValue(Map<Integer, Double> map) {
		List<Map.Entry<Integer, Double>> list = new LinkedList<Map.Entry<Integer, Double>>(
				map.entrySet());

		Collections.sort(list, Collections
				.reverseOrder(new Comparator<Map.Entry<Integer, Double>>() {
					public int compare(Map.Entry<Integer, Double> o1,
							Map.Entry<Integer, Double> o2) {
						return ((Comparable<Double>) ((Map.Entry<Integer, Double>) (o1))
								.getValue())
								.compareTo(((Map.Entry<Integer, Double>) (o2))
										.getValue());
					}
				}));

		Map<Integer, Double> result = new LinkedHashMap<Integer, Double>();
		for (Iterator<Map.Entry<Integer, Double>> it = list.iterator(); it
				.hasNext();) {
			Map.Entry<Integer, Double> entry = (Map.Entry<Integer, Double>) it
					.next();
			DecimalFormat twoDForm = new DecimalFormat("#.##");
			result.put(entry.getKey(),
					Double.valueOf(twoDForm.format(entry.getValue())));
		}

		return result;
	}

	public HashSet<Integer> getRelevantDocs() {
		return relevantDocs;
	}

	public void addRelevantDocs(int docId) {
		relevantDocs.add(docId);
	}

	public HashSet<Integer> getNonRelevantDocs() {
		return nonRelevantDocs;
	}

	public void addNonRelevantDocs(int docId) {
		nonRelevantDocs.add(docId);
	}

	public void removeRelevantDocs(int docId) {
		relevantDocs.remove(docId);
	}

	public void removeNonRelevantDocs(int docId) {
		nonRelevantDocs.remove(docId);
	}

	public void rocchio() {
		HashMap<String, Double> relevantDocsVector = new HashMap<String, Double>();
		HashMap<String, Double> nonRelevantDocsVector = new HashMap<String, Double>();
		for (Integer docId : relevantDocs) {
			HashMap<String, Double> weights = (HashMap<String, Double>) corpus
					.getDocument(docId).getWeights();
			// Expand the query vector by adding (term, 0.0) pairs for all
			// distinct terms found in relevant documents that are not already a
			// part of the query vector.
			// TODO: phrases already in the query vector?
			for (String termString : weights.keySet()) {
				if (!queryVector.containsKey(termString)) {
					queryVector.put(termString, 0.0);
				}
				Double wt = weights.get(termString);
				if (!relevantDocsVector.containsKey(termString)) {
					relevantDocsVector.put(termString, wt);
					nonRelevantDocsVector.put(termString, 0.0);
				} else
					relevantDocsVector.put(termString,
							relevantDocsVector.get(termString) + wt);
			}
		} // end for (Integer docId : relevantDocs)
		for (Integer docId : nonRelevantDocs) {
			HashMap<String, Double> weights = (HashMap<String, Double>) corpus
					.getDocument(docId).getWeights();
			for (String termString : weights.keySet()) {
				// Only get weights and sum weights for terms that appear in the
				// relevant docs vector, which should have the same terms in it
				// as the non-relevant docs vector.
				if (nonRelevantDocsVector.containsKey(termString)) {
					Double wt = weights.get(termString);
					nonRelevantDocsVector.put(termString,
							nonRelevantDocsVector.get(termString) + wt);
				}
			}
		} // end for (Integer docId : nonRelevantDocs)
		
		// Set Rocchio parameters.
		double alpha = 1.0;
		double beta = 0.75;
		double lambda = 0.25;
		
		for (String termString : queryVector.keySet())
			queryVector.put(termString, alpha * queryVector.get(termString));
		for (String termString : relevantDocsVector.keySet())
			relevantDocsVector.put(termString, beta / relevantDocs.size()
					* relevantDocsVector.get(termString));
		for (String termString : nonRelevantDocsVector.keySet())
			nonRelevantDocsVector.put(
					termString,
					lambda / nonRelevantDocs.size()
							* nonRelevantDocsVector.get(termString));
		
		// Final summation to form the modified queryVector.
		queryDistance = 0.0;
		for (String termString : queryVector.keySet()) {
			double value = queryVector.get(termString)
					+ relevantDocsVector.get(termString)
					- nonRelevantDocsVector.get(termString);
			if (value < 0.0)
				value = 0.0;
			queryVector.put(termString, value);
			queryDistance += value*value;
		}
		queryDistance = Math.sqrt(queryDistance);
		phrasePositions.clear();
		
		docScores.clear();
		for (String termString : queryVector.keySet()) {
			for (Integer docId : corpus.getTerm(termString).getPostings().keySet()) {
				docScores.put(docId, 0.0);
			}
		}
		
		cosineScore();
	}

	public Map<Integer, Double> getDocScores() {
		return docScores;
	}

}
