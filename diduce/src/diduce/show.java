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
    
package diduce;

import java.util.*;
import diduce.gui.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

public class show extends java.lang.Thread
{

public static String[] _source_paths;

show (ThreadGroup tg)
{
    super (tg, "GUI-thread");
}

public static void parse_source_path ()
{
    String path = System.getProperty ("diduce.sp");
    if (path == null)
    {
        _source_paths = new String[1];
        _source_paths[0] = ".";
        return;
    }

    StringTokenizer st = new StringTokenizer (path, ":");
    _source_paths = new String[st.countTokens()];
    for (int i = 0 ; i < _source_paths.length ; i++)
        _source_paths[i] = st.nextToken();
}

public static void main(String[] args)
{
    parse_source_path ();
    gui g = new gui (_source_paths, args[0]);
    g.pack ();
    g.setVisible (true);
	/*
    gui._g.stuff0();
    System.out.println ( g == gui._g);
    gui._g.stuff0 ();
	*/
}

public void run ()
{
    String a[] = new String[1];
    a[0] = "Live Run";
    main(a);
}

}
