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
    
package diduce.netbeans;

import java.io.*;
import java.util.*;

import org.openide.util.*;
import org.openide.filesystems.*;
import org.openide.*;
import org.openide.util.actions.CallableSystemAction;

/** Action that can always be invoked and work procedurally.
 *
 * @author hangal
     */
public class watch extends CallableSystemAction {

public watch() 
{ 
	super();
    System.out.println ("Diduce initiated in netbeans successfully");
	DiduceExecutor.install_executor();
}

public void performAction() 
{
    Enumeration e = Repository.getDefault().fileSystems();

	List list = new ArrayList();
    while (e.hasMoreElements())
    {
        FileSystem fs = (FileSystem) e.nextElement();
        System.out.println (" class : " + fs.getClass().getName());

		if (fs.isHidden())
			continue; // don't instrument hidden file systems

		if (fs instanceof JarFileSystem)
		{
			JarFileSystem jfs = (JarFileSystem) fs;

		    File f = jfs.getJarFile();
			try {
				System.out.println (f.getCanonicalPath());
				list.add (f.getCanonicalPath());
			} catch (IOException ioe) {
				System.out.println ("Exception trying to read canonical path for: " + jfs);
				System.exit(3);
			}
		}
    }

	System.out.println ("these are the jars:");
    String a[] = new String[list.size()];
	for (int i = 0 ; i < a.length ; i++)
	{
		a[i] = (String) list.get(i);
	    System.out.println (a[i]);
	}

    try {
            diduce.watch.main (a);
    } catch (IOException x) { System.out.println (x); }
}
    
public String getName()
{
    return NbBundle.getMessage(diduce.netbeans.watch.class, "LBL_Action");
}
    
protected String iconResource() 
{
    return "diduce/MyActionIcon.gif";
}
    
public HelpCtx getHelpCtx() 
{
    return HelpCtx.DEFAULT_HELP;
    // If you will provide context help then use:
        // return new HelpCtx(MyAction.class);
}
    
}
