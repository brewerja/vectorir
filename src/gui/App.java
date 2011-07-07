package gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import java.awt.BorderLayout;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JList;

import vectorir.Query;
import vectorir.Corpus;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

public class App {

	private JFrame frame;
	private JTextField textField;
	private JButton btnSearch;
	private JPanel resultsPanel;
	private JList list;
	private JScrollPane scrollPane;

	private static Corpus corpus;
	private static Query q;

	/**
	 * Launch the application.
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					App window = new App();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
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

	/**
	 * Create the application.
	 */
	public App() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {

		try {
			UIManager
					.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}

		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel searchPanel = new JPanel();
		frame.getContentPane().add(searchPanel, BorderLayout.NORTH);

		textField = new JTextField();

		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				search();
			}
		});
		searchPanel.add(textField);
		textField.setColumns(10);

		btnSearch = new JButton("Search");
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				search();
			}
		});
		searchPanel.add(btnSearch);

		resultsPanel = new JPanel();
		frame.getContentPane().add(resultsPanel, BorderLayout.CENTER);

		list = new JList();

		scrollPane = new JScrollPane(list);
		resultsPanel.add(scrollPane);
	}

	private void search() {
		// Instantiate a Query on the chosen Corpus.
		q = new Query(corpus);
		String query = textField.getText();

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
