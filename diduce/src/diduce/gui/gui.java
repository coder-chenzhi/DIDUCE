/*
DIDUCE: A tool for detecting and root-causing bugs in Java programs.
Copyright (C) 2002 Sudheendra Hangal

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

For more details, please see:
http://www.gnu.org/copyleft/gpl.html

*/

package diduce.gui;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.lang.*;
import java.util.*;
import java.util.Vector;
import java.io.*;

import diduce.jedit.*;
import diduce.*;
import diduce.invariants.*;

public class gui extends JFrame
{

public static gui _g;

/* key data containers: relaxations holds relaxations
   _inv_vector is a vefctor of sppdata's holding actual
   invariants (not violations)
   _row_vector is a vector of vectors representing the table's
   elements */
private ArrayList _relaxations;
private Vector _inv_vector;
private final Vector _relax_row_vector = new Vector(), _inv_row_vector = new Vector();
private JTable _relax_table, _inv_table;
private static final String[] _relax_col_names = {"Time", "Confidence", "What", "Inv. name", "Current Invariant", "New value", "Where", "Line#"};
private static final String[] _inv_col_names = {"Confidence", "What", "Inv. name", "Current Invariant", "Where", "Line#"};
private static double[] _relax_col_pct_widths = {0.05, 0.06, 0.20, 0.14, 0.20, 0.10, 0.22, 0.03}; // must add up to 1
private static double[] _inv_col_pct_widths = {0.05, 0.30, 0.09, 0.30, 0.22, 0.04}; // must add up to 1

private final ArrayList _disabled_invariant_names = new ArrayList();

/* these represent the offsets into the current relaxation vector
   which are to be displayed. both current_min/max_index are inclusive, i.e.
   they *will* be displayed. numbering starts from 0 */
private int _current_min_index, _current_max_index = 0;
private String _selected_class;

/* these represent the max and min line numbers into the currently
   selected file for which invariants/relaxations are to be displayed. 
   both current_min/max_lineno are inclusive, i.e.
   they *will* be displayed. numbering starts from 1 */
private int _min_lineno, _max_lineno = 0;

private int _current_selected_row = 0;
private double _min_conf_change = 0.0;

private JOptionPane _dialog = new JOptionPane ();
private JEditTextArea _ta; /* displays source */
private JTextArea _out;
private String[] _source_paths;
private JTabbedPane _tabs;

private static final String title = "DIDUCE Invariant Violations";

private String _currently_displayed_filename; /* this is /path/to/a/b/classname.java */
private String _currently_displayed_class;  /* this is a.b.classname */
private String _current_file_contents; /* this contains contents of /path/to/a/b/c/classname.java */
private String _data_filename; /* this is the file from which invariants/violations are being read */

private JMenuBar _menubar; 
private JMenu _file_menu, _filter_menu, _show_menu;
private JMenuItem _open_file, _quit, _filter_before, _filter_after, _conf_item, _disable_inv_item, _selected_fname, _show_all, _reread_item;
private MenuSelectionAction _ms_action;

// sets up the menu bar, needs access to the 2 tables (for firing data changes, etc)
private void setup_menus ()
{
    _menubar = new JMenuBar ();

    _file_menu = new JMenu ();

    _reread_item = new JMenuItem ();
    _open_file = new JMenuItem();
    _quit = new JMenuItem();

    _file_menu.setText ("File");
    _file_menu.setActionCommand ("File");
    _file_menu.setMnemonic ((int)'F');
    _menubar.add (_file_menu);

    _reread_item.setText ("Reread invariants");
    _reread_item.setActionCommand ("Reread invariants");
    _reread_item.setAccelerator (KeyStroke.getKeyStroke('R',java.awt.Event.CTRL_MASK,false));
    _file_menu.add (_reread_item);

    _open_file.setText ("Open Class");
    _open_file.setActionCommand ("Open Class");
    _open_file.setAccelerator (KeyStroke.getKeyStroke('F',java.awt.Event.CTRL_MASK,false));
    _file_menu.add (_open_file);

    _quit.setText ("Quit");
    _quit.setActionCommand ("Quit");
    _quit.setAccelerator (KeyStroke.getKeyStroke('Q',java.awt.Event.CTRL_MASK,false));
    _file_menu.add (_quit);

    _filter_menu = new JMenu ();

    _filter_before = new JMenuItem ();
    _filter_after = new JMenuItem ();
    _conf_item = new JMenuItem ();
    _disable_inv_item = new JMenuItem ();
    _selected_fname = new JMenuItem ();
    _show_all = new JMenuItem ();

    _show_menu = new JMenu ();

    _filter_menu.setText ("Filter");
    _filter_menu.setActionCommand ("Filter");
    _filter_menu.setMnemonic ((int)'L');
    _menubar.add (_filter_menu);

    _filter_before.setText ("Hide violations before selected");
    _filter_before.setActionCommand ("Hide violations before selected");
    _filter_before.setAccelerator (KeyStroke.getKeyStroke('B',java.awt.Event.CTRL_MASK,false));
    _filter_menu.add (_filter_before);

    _filter_after.setText ("Hide violations after selected");
    _filter_after.setActionCommand ("Hide violations after selected");
    _filter_after.setAccelerator (KeyStroke.getKeyStroke('A',java.awt.Event.CTRL_MASK,false));
    _filter_menu.add (_filter_after);

    _conf_item.setText ("Set minimum confidence");
    _conf_item.setActionCommand ("Set minimum confidence");
    _conf_item.setAccelerator (KeyStroke.getKeyStroke('C',java.awt.Event.CTRL_MASK,false));
    _filter_menu.add (_conf_item);

    _disable_inv_item.setText ("Disable invariant category");
    _disable_inv_item.setActionCommand ("Disable Invariant category");
    _disable_inv_item.setAccelerator (KeyStroke.getKeyStroke('D',java.awt.Event.CTRL_MASK,false));
    _filter_menu.add (_disable_inv_item);

    _selected_fname.setText ("Show invariants in selected code");
    _selected_fname.setActionCommand ("Show invariants in selected code");

    _selected_fname.setAccelerator (KeyStroke.getKeyStroke('E',java.awt.Event.CTRL_MASK,false));
    _filter_menu.add (_selected_fname);

    _show_all.setText ("Show all");
    _show_all.setActionCommand ("Show all");
    _show_all.setAccelerator (KeyStroke.getKeyStroke('S',java.awt.Event.CTRL_MASK,false));
    _filter_menu.add (_show_all);

    _ms_action = new MenuSelectionAction();
    _open_file.addActionListener (_ms_action);
    _quit.addActionListener (_ms_action);
    _reread_item.addActionListener (_ms_action);
    _filter_before.addActionListener (_ms_action);
    _filter_after.addActionListener (_ms_action);
    _show_all.addActionListener (_ms_action);
    _conf_item.addActionListener (_ms_action);
    _disable_inv_item.addActionListener (_ms_action);
    _selected_fname.addActionListener (_ms_action);
}

private void display_all ()
{ 
    _g = this;

    Container contentPane = getContentPane();
    contentPane.setLayout (new GridLayout(0,1));
    setSize (600,673);

    _ta = new JEditTextArea();
    _ta.setTokenMarker(new JavaTokenMarker());

    _out = new JTextArea();

    if (diduce.Runtime._gui_running)
    {
        // read online
        _relaxations = diduce.Runtime._relaxations;
        _inv_vector = diduce.Runtime._inv_vector;
        System.out.println ("GUI reading " + diduce.Runtime._relaxations.size()
                          + " relaxations from: " + _data_filename);
    }
    else
    {
        // read from a file

        ObjectInputStream ois;
        try { 
			ois= new ObjectInputStream (new FileInputStream (_data_filename)); 
            _inv_vector = (Vector) ois.readObject();
            _relaxations = (ArrayList) ois.readObject();
		}
        catch (Exception e)
        {
            System.err.println ("Warning: error opening file: " + _data_filename);
            return;
        }
    }

    parse_outfile_for_relaxations ();

    _relax_table = new MyJTable (_relax_row_vector, new RelaxMessageSelectionAction(), _relax_col_names, _relax_col_pct_widths);
    _inv_table = new MyJTable (_inv_row_vector, new InvMessageSelectionAction(), _inv_col_names, _inv_col_pct_widths);
    JScrollPane scroll1 = new JScrollPane(_relax_table);
    JScrollPane scroll2 = new JScrollPane(_inv_table);
    _tabs = new JTabbedPane();
    _tabs.add ("Anomalies", scroll1);
    _tabs.add ("Invariants", scroll2);

    JSplitPane splitPane = new JSplitPane (JSplitPane.VERTICAL_SPLIT,
                                           _ta, _tabs);
    setup_menus ();
    setJMenuBar (_menubar);

    splitPane.setOneTouchExpandable(true);
    splitPane.setDividerLocation (0.5);
    splitPane.setDividerSize (7);
    contentPane.add (splitPane);
}

public gui(String[] sp, String data_filename)
{
    super (title + " from \"" + data_filename + '"');
    _source_paths = sp;
    _data_filename = data_filename;
    display_all ();
}

/* looks for a line of the form:
   field_name: <value>
   and returns the value
   needs to be robustified.
*/
private String get_field_value (LineNumberReader r, String field_name) throws IOException
{
    String line;

    do {
	    line = r.readLine();

	    if (line == null)
	        return null;

    } while (!line.startsWith (field_name));

    // parse rest of the line for the conf. change value
    return line.substring (field_name.length());
}

private void parse_outfile_for_relaxations ()
{
    if (_current_min_index == -1)
        _current_min_index = 0;

    // this is really wrong.
	// if a previous max was set, we are losing it.
	// we shd really track if max was set by
	// prev relaxations.size(). if so, when we
	// reread relaxations, we should be ok to
	// change it to the new relaxations.size().
	// otherwise stick to the current max_index
	// not a big deal either way.
    _current_max_index = _relaxations.size()-1;

    initialize_relax_row_vector ();
    initialize_inv_row_vector ();
}

private void parse_outfile_for_invariants ()
{
    initialize_inv_row_vector ();
}

private void initialize_relax_row_vector ()
{
    ArrayList ra = _relaxations;
    _relax_row_vector.clear ();

    int num = 0;
    int n_elements = ra.size();
    for (int i = 0 ; i < n_elements ; i++)
    {
	num++;
        if (num-1 < _current_min_index)
            continue;
        if (num-1 > _current_max_index)
            break; // could be continue also, just efficient to break out
        Relaxation r = (Relaxation) ra.get (i);

        float f = r.get_conf_change();
        f = Math.abs (f);

        String inv_name = r.get_inv_type ();

        // disabled_invariant_names can't ever be null
        if (_disabled_invariant_names.contains (inv_name))
            continue;

        if (f < _min_conf_change)
            continue;

        String filename = r.get_where();
        int line_no = r.get_lineno();

        if (_selected_class != null)
        {
            String classname = util.delete_after_last_dot (filename);

            if (!_selected_class.equals (classname))
                continue;
            if ((_min_lineno > line_no) || (_max_lineno < line_no))
                continue;
        }

        Vector row_data = new Vector(); // row values
        row_data.addElement (new Integer(num));
        row_data.addElement (new Float(f));
        row_data.addElement (r.get_what ());
        row_data.addElement (inv_name);
	String x = r.get_old_value();
	if (x.equals(""))
            row_data.addElement ("None");
	else
	{
	    int n = r.get_n_samples();
	    String s;
	    if (n == 1) s = "once";
	    else if (n == 2) s = "twice";
	    else if (n == 3) s = "thrice";
	    else s = Integer.toString (n) + " times";

            row_data.addElement (r.get_old_value() + " (" + s + ")");
	}

        row_data.addElement (r.get_new_value());
/*        row_data.addElement (new Integer (r.get_n_samples())); */
        WhereObject wo = new WhereObject (filename);
        row_data.addElement (wo);
        row_data.addElement (new Integer (line_no));
        _relax_row_vector.addElement (row_data);
    }
}

private void initialize_inv_row_vector ()
{
    _inv_row_vector.clear ();

    int num = 0;

    if (_inv_vector != null)
    {
        // don't use an iterator here because the vector might be concurrently
        // modified
        int n_elements = _inv_vector.size();
        for (int i = 0 ; i < n_elements ; i++)
        {
            SPPData d = (SPPData) _inv_vector.elementAt (i);
            if (d == null)
                continue;

            String spp_desc = d.getSPPDesc();

            String filename = StaticProgramPoint.get_attribute (spp_desc, "method");
            int line_no = 0;
            try {
            line_no = Integer.parseInt (StaticProgramPoint.get_attribute (spp_desc, "line"));
            } catch (NumberFormatException nfe) { } // shd never happen, even if it does, we leave lineno at 0

            if (_selected_class != null)
            {
                String classname = util.delete_after_last_dot (filename);

                if (!_selected_class.equals (classname))
                    continue;
                if ((_min_lineno > line_no) || (_max_lineno < line_no))
                    continue;
            }

            String op_descr = StaticProgramPoint.get_op_description (spp_desc);

            if (d._invariants != null)
            {
                for (int k = 0; k < d._invariants.length; k++)
                {
                    Invariant inv = d._invariants[k];
                    float f = inv.invariantStrength();
                    f = Math.abs (f);

                    String inv_name = inv.get_description();

                    // disabled_invariant_names can't ever be null
                    if (_disabled_invariant_names.contains (inv_name))
                        continue;
                    if (f < _min_conf_change)
			continue;
		    Vector row_data = new Vector(); // row values
		    row_data.addElement (new Float(f));
		    row_data.addElement (op_descr);
		    row_data.addElement (inv_name);
		    row_data.addElement (inv.toString());
		    WhereObject wo = new WhereObject (filename);
		    row_data.addElement (wo);
		    row_data.addElement (new Integer (line_no));

		    _inv_row_vector.addElement (row_data);

                }
            }
        }
    }
}

public void display (String classname, int lineno)
{
    /* this is still with dots, not slashes */
    String s = classname.replace('.', File.separatorChar);
    int x = s.indexOf ('$'); // if inner class drop it
    if (x >= 0)
        s = s.substring (0, x);
    s += ".java";

    readFile (s);
    _currently_displayed_class = classname;
    setTitle (_currently_displayed_filename);
    _ta.setText(_current_file_contents);

    if (lineno > _ta.getLineCount() || lineno < 0)
         lineno = 0;
    _ta.setCaretPosition (_ta.getLineStartOffset (lineno));
    _ta.updateScrollBars ();
}

/* Lookup a file name along all the source paths
   sets _current_file_contents to the contents of the file
   (or an error message if the file is not found)

   if the file is found, the full path to it is returned.
   if not found, an error string is returned.
*/
private void readFile (String filename)
{
    String full_path = null;
    boolean found = false;

    // handle a common case
    String suffix = File.separatorChar + filename;
    if (_currently_displayed_filename != null)
        if (_currently_displayed_filename.endsWith (suffix))
            return;

    for (int i = 0 ; i < _source_paths.length ; i++)
    {
        full_path = _source_paths[i] + File.separatorChar + filename;
        File f = new File (full_path);

        if (f.exists())
        {
            found = true;
            break;
        }
    }

    if (!found)
    {
        String s = "Sorry. unable to find file \"" + filename + '"'
                 + " on source path, which is currently set to:\n";
        for (int i = 0 ; i < _source_paths.length ; i++)
            s += '"' + _source_paths[i] + '"' + "\n" +
                "Define the property diduce.sp to view sources";
        _current_file_contents = s;
        _currently_displayed_filename = "File not found";
        return;
    }

    _currently_displayed_filename = full_path;

    StringBuffer sb = new StringBuffer();

    try {

    LineNumberReader r = new LineNumberReader
                         (new InputStreamReader
                          (new FileInputStream (full_path)));

    while (true)
    {
        String s = r.readLine();
        if (s == null)
            break;

        sb.append (r.getLineNumber() + ": ");
        sb.append (s);
        sb.append ("\n");
    }
    _current_file_contents = sb.toString();
	r.close();

    return;
    } catch (IOException e) {
        _current_file_contents = "Sorry: Exception trying to read file: " + filename + "\n" + e;
        _currently_displayed_filename = "File not found";
        return;
    }
}

class RelaxMessageSelectionAction implements ListSelectionListener
{

public void valueChanged(ListSelectionEvent e)
{
    //Ignore extra messages.
    if (e.getValueIsAdjusting())
        return;

    ListSelectionModel lsm = (ListSelectionModel)e.getSource();

    // if no row is selected, we need to bail out
    if (lsm.isSelectionEmpty ())
        return;

    // get the curr selected row by the serial number entry in col. 0.
    // can't directly use selected row because table may be sorted.
    int selectedRow = lsm.getMinSelectionIndex();

    TableSorter ts = (TableSorter) _relax_table.getModel();
    _current_selected_row = ts.indexes[selectedRow];

    WhereObject wo = (WhereObject) ts.getValueAt (selectedRow, 6);
    int line = ((Integer) ts.getValueAt (selectedRow, 7)).intValue()-1;
    String s = wo.getClassName();
    display (s, line);
//    Relaxation r = (Relaxation) _relaxations.get (_current_selected_row);
//    String context = r.get_context ();
}
}

class InvMessageSelectionAction implements ListSelectionListener
{

public void valueChanged(ListSelectionEvent e)
{
    //Ignore extra messages.
    if (e.getValueIsAdjusting())
        return;

    ListSelectionModel lsm = (ListSelectionModel)e.getSource();

    // if no row is selected, we need to bail out
    if (lsm.isSelectionEmpty ())
        return;

    int selectedRow = lsm.getMinSelectionIndex();

    TableSorter ts = (TableSorter) _inv_table.getModel();
    _current_selected_row = ts.indexes[selectedRow];

    WhereObject wo = (WhereObject) ts.getValueAt (selectedRow, 4);
    int line = ((Integer) ts.getValueAt (selectedRow, 5)).intValue()-1;
    String s = wo.getClassName();
    display (s, line);
}
}

class WhereObject
{
String _str;
public WhereObject (String s) { _str = s; }
public String toString () { return util.strip_package_name (_str); }
public String getClassName() { return util.delete_after_last_dot (_str); }
public String getFullString() { return _str; }
}

class MenuSelectionAction implements java.awt.event.ActionListener {

private AbstractTableModel _relax_tm, _inv_tm;

public MenuSelectionAction ()
{
    TableSorter tm = (TableSorter) _relax_table.getModel();
    _relax_tm = (AbstractTableModel) tm.getModel();
    tm = (TableSorter) _inv_table.getModel();
    _inv_tm = (AbstractTableModel) tm.getModel();
}

void setup_base(java.awt.event.ActionEvent event)
{
    try
    {
//       _tableModel.fireTableChanged (new  TableModelEvent (_tableModel));
    } catch(Exception e)
    {
        System.out.println ("exception thrown in uimanager: " + e);
        e.printStackTrace();
    }
}

public void actionPerformed(java.awt.event.ActionEvent event)
{
    Object object = event.getSource();
    if (object == _open_file)
       do_open_file (event); 
    else if (object == _quit)
       System.exit (0);
    else if (object == _filter_before)
       do_filter_before (event); 
    else if (object == _filter_after)
       do_filter_after (event); 
    else if (object == _show_all)
        do_show_all (event);
    else if (object == _conf_item)
        do_set_conf (event);
    else if (object == _disable_inv_item)
        do_disable_inv_class (event);
    else if (object == _selected_fname)
        do_select_fname (event);
    else if (object == _reread_item)
        do_reread (event);
}

void do_open_file (ActionEvent event)
{
    Object o = (String) _dialog.showInputDialog (null, "Open source file for class:", "Input", JOptionPane.QUESTION_MESSAGE, null, null, "");

    if (!(o instanceof String))
        return;
    else
        display ((String) o, 0);
}

private void do_filter_before (ActionEvent event)
{
    _current_min_index = _current_selected_row;
    initialize_relax_row_vector ();
    _relax_tm.fireTableChanged(new TableModelEvent (_relax_tm));
}

private void do_filter_after (ActionEvent event)
{
    _current_max_index = _current_selected_row;
    initialize_relax_row_vector();
    _relax_tm.fireTableChanged(new TableModelEvent (_relax_tm));
}

private void do_show_all (ActionEvent event)
{
    _current_min_index = 0;
    _current_max_index = _relaxations.size()-1;
    _min_conf_change = 0.0;
    _disabled_invariant_names.clear ();
    _selected_class = null;

    initialize_relax_row_vector();
    initialize_inv_row_vector();
    _relax_tm.fireTableChanged(new TableModelEvent (_relax_tm));
    _inv_tm.fireTableChanged(new TableModelEvent (_inv_tm));
}

private void do_set_conf (ActionEvent event)
{
    Object o = (String) _dialog.showInputDialog (null, "Only display confidence changes above magnitude:", "Input", JOptionPane.QUESTION_MESSAGE, null, null, new Float (_min_conf_change)); 
    if (!(o instanceof String))
        return;
    if (o == null) 
        return;
        
    String s = (String) o;
    try { _min_conf_change = Double.parseDouble (s); } 
    catch (NumberFormatException nfe) { return; }

    _min_conf_change = Math.abs (_min_conf_change);
    util.ASSERT (_min_conf_change >= 0.0);

    initialize_relax_row_vector();
    initialize_inv_row_vector();
    _relax_tm.fireTableChanged(new TableModelEvent (_relax_tm));
    _inv_tm.fireTableChanged(new TableModelEvent (_inv_tm));
}

private void do_disable_inv_class (ActionEvent event)
{
    String inv_class;

    if (_tabs.getSelectedIndex() == 1)
        inv_class = (String) ((Vector) _inv_row_vector.elementAt (_current_selected_row)).elementAt (2);
    else
        inv_class = (String) ((Vector) _relax_row_vector.elementAt (_current_selected_row)).elementAt (3);

    Object o = (String) _dialog.showInputDialog (null, "Hide invariants in the category:", "Input", JOptionPane.QUESTION_MESSAGE, null, null, inv_class); 

    if (!(o instanceof String))
        return;

    _disabled_invariant_names.add ((String) o);

    initialize_relax_row_vector();
    initialize_inv_row_vector();
    _relax_tm.fireTableChanged(new TableModelEvent (_relax_tm));
    _inv_tm.fireTableChanged(new TableModelEvent (_inv_tm));
}

private void do_select_fname (ActionEvent event)
{
    _selected_class = _currently_displayed_class;
    _min_lineno = _ta.getSelectionStartLine()+1;
    _max_lineno = _ta.getSelectionEndLine()+1;
    
    initialize_relax_row_vector();
    initialize_inv_row_vector();
    _relax_tm.fireTableChanged(new TableModelEvent (_relax_tm));
    _inv_tm.fireTableChanged(new TableModelEvent (_inv_tm));
}

private void do_reread (ActionEvent event)
{
    parse_outfile_for_relaxations (); 
    try
    {
        initialize_relax_row_vector();
        initialize_inv_row_vector();
       _relax_tm.fireTableChanged(new TableModelEvent (_relax_tm));
       _inv_tm.fireTableChanged(new TableModelEvent (_inv_tm));
    } catch(Exception e)
    {
        System.out.println ("Exception in UI " + e);
        e.printStackTrace();
    }
}

/*
void stuff () {
System.out.println ("creating progress bar");
ProgressMonitor m = new ProgressMonitor (gui._g, "Some Progress", "OK...", 0, 10);
m.setProgress (0);
m.setMillisToDecideToPopup(1000);

for (int i = 1 ; i < 11 ; i++)
{
try {
m.setProgress (i);
System.out.println ("this = " + m + " i = " + i);
Thread.sleep (3000);
} catch (InterruptedException ie) { }
}
}
*/

/*
    public void stuff0 () {
            ProgressMonitor progressMonitor = new ProgressMonitor (null,
                                      "Running a Long Task",
                     		"", 0, 4);

            progressMonitor.setProgress(0);
    progressMonitor.setMillisToDecideToPopup(1000);

    for (int i = 0 ; i < 5 ; i++)
    { try
      {
        progressMonitor.setProgress (i); 
        Thread.sleep (2000);
      } catch (InterruptedException e) { } 
    }
    }
*/

}
}


