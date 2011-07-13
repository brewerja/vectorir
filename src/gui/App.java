package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import javax.swing.JMenuBar;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.JTextPane;

public class App {

	// GUI Components
	private JFrame frame;
	private JTextField textField;
	private JButton buttonSearch;
	private JPanel searchPanel;
	private JTable table;
	private CustomTableModel tableModel;
	private MyTableCellRenderer cellRenderer = new MyTableCellRenderer();
	private JScrollPane tableScrollPane;
	private JTextPane bodyTextPane;
	private JScrollPane bodyTextScrollPane;
	private JSplitPane splitPane;
	private JMenu menu;
	private JMenuItem item1;
	private JMenuBar menuBar;
	private final JFileChooser fc = new JFileChooser();

	// Search Components
	private static Corpus corpus;
	private static Query q;
	private String queryText = "";
	private HashSet<Integer> formerRelevantDocs = new HashSet<Integer>();
	private HashSet<Integer> formerNonRelevantDocs = new HashSet<Integer>();

	// Launch the application.
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

		// Load the corpus into memory and instantiate a Query object.
		deserializeCorpus("corpus.dat");
		q = new Query(corpus);
	}

	// Create the application.
	public App() {
		initialize();
	}

	// Initialize the contents of the frame.
	private void initialize() {

		try {
			UIManager
					.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Frame (Application Window)
		frame = new JFrame("Reuters-21578 Search");
		frame.setBounds(200, 100, 1050, 550);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Menu Bar
		menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		menu = new JMenu("Corpus");
		menu.getAccessibleContext().setAccessibleDescription("Corpus Menu");
		menuBar.add(menu);

		item1 = new JMenuItem("Select Corpus", KeyEvent.VK_S);
		item1.setToolTipText("Load a new corpus file into memory.");
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
						e1.printStackTrace();
					} catch (ClassNotFoundException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		menu.add(item1);

		// Search Panel
		searchPanel = new JPanel();
		frame.getContentPane().add(searchPanel, BorderLayout.NORTH);

		// Query Entry Field
		textField = new JTextField();
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				search();
			}
		});
		textField.setColumns(10);
		textField.setToolTipText("Enter query here.");
		searchPanel.add(textField);

		// Search button
		buttonSearch = new JButton("Search");
		buttonSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				search();
			}
		});
		buttonSearch.setToolTipText("Search or Refine Search.");
		searchPanel.add(buttonSearch);

		// JTable
		Object headers[] = { "Doc ID", "Title", "Score" };
		tableModel = new CustomTableModel(null, headers);
		table = new JTable(tableModel);

		// Set the custom cell renderer for coloring of relevance feedback.
		for (int i = 0; i < 3; i++) {
			table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
		}

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getColumnModel().getColumn(1).setPreferredWidth(550);
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(false);

		// Display a document selected with the mouse.
		table.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				displayDocument((Integer) table.getModel().getValueAt(row, 0));
			}
		});

		// React to keystrokes interacting with the result set.
		table.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				int row = table.getSelectedRow();
				if (row == -1)
					return;
				int docId = (Integer) table.getModel().getValueAt(row, 0);
				char c = e.getKeyChar();
				// Note that once a document marker R or NR has been used in a
				// Rocchio expanded query, it's R/NR selection cannot be
				// modified.
				if (c == 'r') {
					// Mark as relevant.
					if (!formerRelevantDocs.contains(docId)
							&& !formerNonRelevantDocs.contains(docId)) {
						q.addRelevantDocs(docId);
						q.removeNonRelevantDocs(docId);
						tableModel.fireTableRowsUpdated(row, row);
					}
				} else if (c == 'n') {
					// Mark as non-relevant.
					if (!formerRelevantDocs.contains(docId)
							&& !formerNonRelevantDocs.contains(docId)) {
						q.addNonRelevantDocs(docId);
						q.removeRelevantDocs(docId);
						tableModel.fireTableRowsUpdated(row, row);
					}
				} else if (c == 'u') {
					// Undo relevance feedback provided.
					if (!formerRelevantDocs.contains(docId)
							&& !formerNonRelevantDocs.contains(docId)) {
						q.removeRelevantDocs(docId);
						q.removeNonRelevantDocs(docId);
						tableModel.fireTableRowsUpdated(row, row);
					}
				} else if (c == 'j' && row != table.getRowCount() - 1) {
					// Move down.
					table.setRowSelectionInterval(row + 1, row + 1);
					docId = (Integer) table.getModel().getValueAt(row + 1, 0);
				} else if (c == 'k' && row != 0) {
					// Move up.
					table.setRowSelectionInterval(row - 1, row - 1);
					docId = (Integer) table.getModel().getValueAt(row - 1, 0);
				}
				displayDocument(docId);
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}
		});

		tableScrollPane = new JScrollPane(table);

		// Document Display
		bodyTextPane = new JTextPane();
		bodyTextScrollPane = new JScrollPane(bodyTextPane);

		// Split Pane
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				tableScrollPane, bodyTextScrollPane);
		splitPane.setDividerLocation(600);
		frame.getContentPane().add(splitPane, BorderLayout.CENTER);

	}

	private static void deserializeCorpus(String file) throws IOException,
			ClassNotFoundException {
		// Deserialize the stored Corpus object, loading into memory.
		System.out.println("Deserializing corpus...");
		FileInputStream fis = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fis);
		corpus = (Corpus) ois.readObject();
		System.out.println(corpus.getNumDocuments()
				+ " documents loaded from the corpus.");
	}

	private void displayDocument(int docId) {
		Document doc = corpus.getDocument(docId);
		String dateline = doc.getDateline();
		String body = doc.getBody().replaceAll("\\\\\"", "\\\"");
		bodyTextPane.setText(dateline + " " + body);
		bodyTextPane.setCaretPosition(0);
	}

	@SuppressWarnings("unchecked")
	private void search() {

		// Check to see if losing new feedback information.
		if (!queryText.equals(textField.getText())
				&& (!formerRelevantDocs.equals(q.getRelevantDocs()) || !formerNonRelevantDocs
						.equals(q.getNonRelevantDocs()))) {
			int n = JOptionPane
					.showConfirmDialog(
							frame,
							"You have given relevance feedback on the current result set. Do you wish to run a new query and discard the feedback?",
							"New Query?", JOptionPane.YES_NO_OPTION);
			if (n != 0)
				return; // NO
		}

		// Check for an empty query.
		if (textField.getText().equals(""))
			return;
		// Check for an unchanged query, with no new feedback.
		else if (formerRelevantDocs.equals(q.getRelevantDocs())
				&& formerNonRelevantDocs.equals(q.getNonRelevantDocs())
				&& queryText.equals(textField.getText()))
			return;
		// Either a new query entirely or the same query with new feedback.
		else {
			long startTime = System.currentTimeMillis();
			Map<Integer, Double> docScores = new HashMap<Integer, Double>();

			// If the query is unchanged and has made it this far, it's
			// feedback, otherwise it's a new query.
			if (queryText.equals(textField.getText())) {
				// If there is no positive feedback (only negative), can't run
				// Rocchio.
				if (q.getRelevantDocs().isEmpty())
					return;
				q.rocchio();
			} else {
				queryText = textField.getText();
				q = new Query(corpus);
				if (q.prepareQuery(queryText))
					q.executeQuery();
				else
					return;
			}
			docScores = q.getDocScores();

			// Output the documents in order of similarity to the query.
			long stopTime = System.currentTimeMillis();
			System.out.println(docScores.size() + " results ("
					+ (stopTime - startTime) / 1000.0 + " seconds)");

			// Populate the table.
			tableModel.getDataVector().removeAllElements();
			for (Map.Entry<Integer, Double> item : docScores.entrySet()) {
				Document doc = corpus.getDocument(item.getKey());
				Object[] rowData = { item.getKey(), doc.getTitle(),
						item.getValue() };
				tableModel.addRow(rowData);
			}
			tableModel.fireTableChanged(new TableModelEvent(tableModel));
			displayDocument((Integer) table.getModel().getValueAt(0, 0));

			// Store the feedback to test for changes later.
			formerRelevantDocs = (HashSet<Integer>) q.getRelevantDocs().clone();
			formerNonRelevantDocs = (HashSet<Integer>) q.getNonRelevantDocs()
					.clone();
		}
	}

	// Makes all cells not editable and identifies the types of each column.
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
			return false;
		}

	}

	// Make relevant docs green and non-relevant docs red.
	static class MyTableCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 5464571011029151373L;

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			Component c = super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);

			int docId = (Integer) table.getModel().getValueAt(row, 0);

			if (q.getNonRelevantDocs().contains(docId))
				c.setForeground(Color.RED);
			else if (q.getRelevantDocs().contains(docId))
				c.setForeground(Color.GREEN);
			else {
				c.setForeground(null);
			}
			return c;
		}
	}
}
