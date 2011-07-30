package vectorir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Document implements java.io.Serializable {

	private static final long serialVersionUID = 8345154028331003306L;
	private int id;
	private String title;
	private String dateline;
	private String body;
	private ArrayList<String> tokens = new ArrayList<String>();
	private Map<String, Double> weights = new HashMap<String, Double>();
	private double euclideanDistance;
	private int maxTermFrequency = 0;
	private ArrayList<String> topics = new ArrayList<String>();

	public Document(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDateline() {
		return dateline;
	}

	public void setDateline(String dateline) {
		this.dateline = dateline;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public ArrayList<String> getTokens() {
		return tokens;
	}

	public void setTokens(ArrayList<String> tokens) {
		this.tokens = tokens;
	}

	public int getNumTokens() {
		return tokens.size();
	}

	public int getId() {
		return id;
	}

	public int getMaxTermFreq() {
		return maxTermFrequency;
	}

	public void registerTermFreq(int freq) {
		if (this.maxTermFrequency < freq)
			this.maxTermFrequency = freq;
	}

	public double getWeight(String term) {
		return weights.get(term);
	}

	public void addWeight(String term, Double weight) {
		this.weights.put(term, weight);
	}

	public double getEuclideanDistance() {
		return euclideanDistance;
	}

	public void setEuclideanDistance() {
		double distance = 0.0;
		for (Double weight : this.weights.values())
			distance += weight * weight;
		this.euclideanDistance = Math.sqrt(distance);
	}

	public void clearTokens() {
		this.tokens.clear();
	}

	public Map<String, Double> getWeights() {
		return weights;
	}

	public ArrayList<String> getTopics() {
		return topics;
	}

	public void addTopic(String topic) {
		this.topics.add(topic);
	}

}
