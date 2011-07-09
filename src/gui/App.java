package gui;

import java.awt.Dimension;
import java.awt.EventQueue;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

import java.awt.BorderLayout;
import javax.swing.JTextField;
import javax.swing.JButton;
import vectorir.Document;
import vectorir.Query;
import vectorir.Corpus;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.JMenuBar;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.JTextPane;

public class App {

	private JFrame frame;
	private JTextField textField;
	private JButton btnSearch;
	private JPanel resultsPanel;
	private JPanel searchPanel;
	private JScrollPane tableScrollPane;
	private JMenu menu;
	private JMenuItem item1;
	private final JFileChooser fc = new JFileChooser();

	private static Corpus corpus;
	private static Query q;
	private JMenuBar menuBar;
	private JTable table;
	private CustomTableModel tableModel;
	private JTextPane bodyTextPane;
	private JScrollPane bodyTextScrollPane;

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
		frame.setBounds(200, 100, 1050, 550);
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

		Object headers[] = { "Doc ID", "Title", "Score" };
		tableModel = new CustomTableModel(null, headers);
		table = new JTable(tableModel);

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getColumnModel().getColumn(1).setPreferredWidth(550);
		// table.setAutoCreateRowSorter(false);
		table.setRowSelectionAllowed(true);

		table.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				displayDocument((Integer) table.getModel().getValueAt(row, 0));
			}
		});
		table.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void keyReleased(KeyEvent e) {
				int row = table.getSelectedRow();
				displayDocument((Integer) table.getModel().getValueAt(row, 0));
			}

			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub
			}
		});

		tableScrollPane = new JScrollPane(table);
		tableScrollPane.setPreferredSize(new Dimension(600, 400));
		resultsPanel.add(tableScrollPane);

		bodyTextPane = new JTextPane();
		bodyTextPane.setPreferredSize(new Dimension(400, 400));

		bodyTextScrollPane = new JScrollPane(bodyTextPane);
		resultsPanel.add(bodyTextScrollPane);

	}

	private void displayDocument(int docId) {
		Document doc = corpus.getDocument(docId);
		String dateline = doc.getDateline();
		String body = doc.getBody();
		bodyTextPane.setText(dateline + " " + body);
		// TODO: This isn't working, not starting at the top.
		JScrollBar verticalScrollBar = bodyTextScrollPane
				.getVerticalScrollBar();
		verticalScrollBar.setValue(verticalScrollBar.getMinimum());
	}

	private void search() {

		// Check for an empty query.
		String query = textField.getText();
		if (query.equals(""))
			return;

		// Instantiate a Query on the chosen Corpus.
		q = new Query(corpus);

		long startTime = System.currentTimeMillis();

		Map<Integer, Double> docScores = new HashMap<Integer, Double>();
		if (q.prepareQuery(query)) {
			docScores = q.executeQuery();
			// Output the documents in order of similarity to the query.
			// System.out.println("Results: " + docScores);
			long stopTime = System.currentTimeMillis();
			System.out.println(docScores.size() + " results ("
					+ (stopTime - startTime) / 1000.0 + " seconds)");
		}

		// Populate the table.
		tableModel.getDataVector().removeAllElements();
		for (Map.Entry<Integer, Double> item : docScores.entrySet()) {
			Document doc = corpus.getDocument(item.getKey());
			Object[] rowData = { item.getKey(), doc.getTitle(), item.getValue() };
			tableModel.addRow(rowData);
		}
		tableModel.fireTableChanged(new TableModelEvent(tableModel));
	}

	class CustomTableModel extends DefaultTableModel {

		private static final long serialVersionUID = -4979601734379067486L;

		public CustomTableModel(Object rowData[][], Object columnNames[]) {
			super(rowData, columnNames);
		}

		public Class<? extends Object> getColumnClass(int col) {
			Vector<?> v = (Vector<?>) dataVector.elementAt(0);
			return v.elementAt(col).getClass();
		}
		
	    @Override
	    public boolean isCellEditable(int row, int column) {
	       //all cells false
	       return false;
	    }

	}
}
