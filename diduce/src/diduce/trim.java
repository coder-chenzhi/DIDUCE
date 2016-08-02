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
import java.io.*;

public abstract class trim {
    private static Vector _inv_vector;

    public static void main(String[] args) {
	try {
	    ObjectInputStream inp = new ObjectInputStream (new FileInputStream(args[0]));
	    String fileBaseName = args[1];
	    double threshold = Double.parseDouble(args[2]);

	    System.out.println ("Reading DIDUCE data from file: " + args[0]);
	    _inv_vector = (Vector) inp.readObject();
	    System.out.println ("Outputting configuration information to file: "+args[1]);

	    File confFile = new File(fileBaseName);
	    if (confFile.exists()) {
		int i = 1;
		File f;
		while (true) {
		    f = new File(fileBaseName + ".backup-" + String.valueOf(i));
		    i++;
		    if (!f.exists()) break;
		}
		System.out.println("Renaming old configuration file to: "+f.getName());
		confFile.renameTo(f);
		/* TODO: Check if this line is actually necessary -- is
		   renameTo state-preserving? */
		confFile = new File(fileBaseName);
	    }
	    
	    PrintStream ps = new PrintStream(new FileOutputStream(confFile));
	    printConfigFile(ps, threshold);
	} catch (IOException ioe) {
	    System.err.println ("Could not read invariant file!");
	    ioe.printStackTrace();
	} catch (ClassNotFoundException cnfe) {
	    System.err.println("Invariants file is corrupted!");
	} catch (ArrayIndexOutOfBoundsException aioobe) {
	    System.err.println("Usage:");
	    System.err.println("    java diduce.trim {invariants-file} {config-file} {threshold}");
	} catch (NumberFormatException nfe) {
	    System.err.println("Could not parse threshold value '"+args[2]+"'");
	}
    }

    public static void printConfigFile (PrintStream ps, double threshold)
    {
	Vector real_invariants = new Vector ();
	int invariant_count = 0;

	Enumeration all_spps;
	all_spps = _inv_vector.elements();

	while (all_spps.hasMoreElements ()) {
	    SPPData d = (SPPData) all_spps.nextElement ();
	    if (d == null)
		continue;

	    String spp_desc = d.getSPPDesc();

	    if (d._invariants != null) {
		boolean killSPP;
		killSPP = false;
		for (int i = 0; i < d._invariants.length; i++) {
		    if (d._invariants[i].invariantStrength() <= threshold) {
			if (!killSPP) {
			    System.out.println("Killing SPP "+spp_desc+": overly weak invariant.  "+d._invariants[i].toString());
			    killSPP=true;
			}
		    }
		}
		if(killSPP) {
		    String config_line = "- "+spp_desc;
		    ps.println(config_line);
		}
	    }		
	}
    }
}
