package gui;

import java.awt.EventQueue;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
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
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JMenuBar;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class App {

	private JFrame frame;
	private JTextField textField;
	private JButton btnSearch;
	private JPanel resultsPanel;
	private JPanel searchPanel;
	private JList list;
	private JScrollPane scrollPane;
	private JMenu menu;
	private JMenuItem item1;
	private final JFileChooser fc = new JFileChooser();

	private static Corpus corpus;
	private static Query q;
	private JMenuBar menuBar;

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

		deserializeCorpus("corpus.dat");
	}

	/**
	 * Create the application.
	 */
	public App() {
		initialize();
	}

	private static void deserializeCorpus(String file) throws IOException,
			ClassNotFoundException {
		// Deserialize the stored Corpus object.
		System.out.println("Deserializing corpus...");
		FileInputStream fis = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fis);
		corpus = (Corpus) ois.readObject();
		System.out.println(corpus.getNumDocuments()
				+ " documents loaded from the corpus.");
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

		frame = new JFrame("Reuters-21578 Search");
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		// Build the first menu.
		menu = new JMenu("Corpus");
		menu.setMnemonic(KeyEvent.VK_A);
		menu.getAccessibleContext().setAccessibleDescription("Corpus Menu");
		menuBar.add(menu);

		// a group of JMenuItems
		item1 = new JMenuItem("Select Corpus", KeyEvent.VK_T);
		item1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FileFilter filter = new FileNameExtensionFilter(
						"Corpus Files (*.dat)", "dat");
				fc.setFileFilter(filter);

				int returnVal = fc.showOpenDialog(fc);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					try {
						deserializeCorpus(file.getPath());
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (ClassNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		item1.getAccessibleContext().setAccessibleDescription(
				"Load a new corpus file into memory.");
		menu.add(item1);

		searchPanel = new JPanel();
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
		String query = textField.getText();
		if (query.equals(""))
			return;

		// Instantiate a Query on the chosen Corpus.
		q = new Query(corpus);

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
