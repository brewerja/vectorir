package gui;

/*
 * App.java requires no other files. 
 */
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

import javax.swing.*;

import vectorir.Corpus;
import vectorir.Query;

public class App implements ActionListener {

	static Corpus corpus;
	static Query q;
	JTextField queryTextField;

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void createAndShowGUI() {
		try {
			UIManager
					.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Create and set up the window.
		JFrame frame = new JFrame("Reuters-21578 Search");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setMinimumSize(new Dimension(500, 600));

		Container pane = frame.getContentPane();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

		// Top Panel
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		pane.add(topPanel);

		// Query Area
		queryTextField = new JTextField(10);
		queryTextField.setMaximumSize(new Dimension(100, 50));
		JLabel queryTextFieldLabel = new JLabel("Query: ");
		queryTextFieldLabel.setLabelFor(queryTextField);
		topPanel.add(queryTextFieldLabel);
		topPanel.add(queryTextField);

		JButton queryButton = new JButton("Search");
		topPanel.add(queryButton);
		queryButton.addActionListener(this);

		// Bottom Panel
		JPanel bottomPanel = new JPanel(new BorderLayout());
		pane.add(bottomPanel);

		// Results Table
		JTable table = new ResultsTable();
		// JScrollPane scrollPane = new JScrollPane(table);
		// bottomPanel.add(scrollPane);
		bottomPanel.add(table);

		// Display the window.
		frame.pack();
		frame.setVisible(true);

	}

	public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		final App app = new App();

		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				app.createAndShowGUI();
			}
		});

		// Deserialize the stored Corpus object.
		System.out.println("Deserializing corpus...");
		FileInputStream fis = new FileInputStream("corpus.dat");
		ObjectInputStream ois = new ObjectInputStream(fis);
		corpus = (Corpus) ois.readObject();
		System.out.println(corpus.getNumDocuments()
				+ " documents loaded from the corpus.");

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// Instantiate a Query on the chosen Corpus.
		q = new Query(corpus);
		String query = queryTextField.getText();

		long startTime = System.currentTimeMillis();

		if (q.prepareQuery(query)) {
			Map<Integer, Double> docScores = q.executeQuery();
			// Output the documents in order of similarity to the query.
			System.out.println("Results: " + docScores);
			long stopTime = System.currentTimeMillis();
			System.out.println(docScores.size() + " results ("
					+ (stopTime - startTime) / 1000.0 + " seconds)");
		}
	}
}