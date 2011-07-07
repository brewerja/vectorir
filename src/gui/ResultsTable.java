package gui;

import javax.swing.JTable;

public class ResultsTable extends JTable {

	private static final long serialVersionUID = 3559025877048334112L;

	public ResultsTable() {
		super(10, 3);
	}

	public boolean isCellEditable(int row, int column) {
		return false;
	}
}
