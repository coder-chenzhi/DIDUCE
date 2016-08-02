
package diduce.gui;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.*;

public class MyJTable extends JTable {

public MyJTable (final Vector data, final ListSelectionListener lsl,final String[] col_names, final double[] col_pct_widths)
{
    AbstractTableModel atm = get_tablemodel (data, col_names);
    setup_table (atm, lsl, col_pct_widths);
}

private AbstractTableModel get_tablemodel (final Vector row_vector, final String col_names[])
{
    AbstractTableModel tm = new AbstractTableModel()
    {
//        private String[] _col_names = col_names;
        public String getColumnName(int column) { return col_names[column]; }
        public int getRowCount() { return row_vector.size(); }
        public int getColumnCount() { return col_names.length; }
        public Class getColumnClass(int c) { return getValueAt(0, c).getClass(); }
        public Object getValueAt(int row, int column)
        {
            Object o = ((Vector)row_vector.elementAt(row)).elementAt(column);
            return o;
        }
    }; //end of AbstractTableModel
  
    return tm;
}

private void setup_table(AbstractTableModel tm, ListSelectionListener lsl, double[] col_pct_widths)
{
    TableSorter ts = new TableSorter (tm);
    setModel (ts);

    /* enable this for text area based cells
    setDefaultRenderer (Object.class, new TextAreaTableCellRenderer());
    */
    /* enable this for tool tips */
    setDefaultRenderer (Object.class, new ToolTipTableCellRenderer());
    ToolTipManager.sharedInstance().registerComponent(this);

    ts.addMouseListenerToHeaderInTable(this);

    getTableHeader().setReorderingAllowed(true);
    getTableHeader().setResizingAllowed(true);
    setBounds(0,0,875,0);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    setRowHeight(20);

    for (int i = 0 ; i < col_pct_widths.length ; i++)
    {
        TableColumn colm = getColumnModel().getColumn(i);
        colm.setPreferredWidth ((int) (getWidth() * col_pct_widths[i]));
    }

    ListSelectionModel rowSM = getSelectionModel();
    rowSM.addListSelectionListener (lsl);
}

}

