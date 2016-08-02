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
import java.awt.*;

class ToolTipTableCellRenderer extends 
javax.swing.table.DefaultTableCellRenderer {

public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) 
{
    Component c = super.getTableCellRendererComponent (table, value, isSelected, hasFocus, row, column);
    ((JComponent)c).setToolTipText(value.toString());
    return c;
}

}


