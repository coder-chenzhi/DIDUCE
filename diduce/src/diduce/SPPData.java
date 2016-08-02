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
import diduce.invariants.*;
import diduce.xml.*;

// this class is related to runtime, not instrumentation
// it carries all the invariants associated with a particular
// program point.

public class SPPData implements Serializable {

// ideally nsamples shd be factored out of all the invariants and kept here
// int _nSamples;
public Invariant _invariants[];
private String _spp_descriptor;
private static int total_invariants_created = 0;

private static List _spp_to_inv_list;

static {
     // initialize the list of invariants 
    _spp_to_inv_list = new ArrayList ();
    
    if (!XmlInputParser.isParsed) {
	String control_file = System.getProperty ("diduce.control.file");
	if (control_file != null) {
	    try {
	        XmlInputParser parser = new XmlInputParser(control_file);
                parser.parse();
	    } catch (Exception e) {
		e.printStackTrace();
		util.fatal ("Unable to initialise class SPPData: " + e);
	    }
	}
     }
    _spp_to_inv_list =  XmlInputParser.invariantSpecs;
    if (_spp_to_inv_list.size() > 0) {
	System.out.println ("Read " + _spp_to_inv_list.size() +
		" runtime directives from \"" + XmlInputParser.userSpecFile +
		"\"");
    }
    else { 

	/* hardcoding the invariants here sucks, but better than
	 * looking around for a default file in some location
	 */
        System.out.println ("Using default invariant set");
	String ctypes = "int|byte|short|char|boolean";
        ControlInfo ci = new ControlInfo();	
	ci.type = ctypes; ci.write = "false";
	ci.invariants.add("diduce.invariants.IntValInvariant");
        _spp_to_inv_list.add(ci);

	ci = new ControlInfo();
	ci.type = ctypes; ci.write = "true";
	ci.invariants.add("diduce.invariants.IntValInvariant");
	ci.invariants.add("diduce.invariants.IntDiffInvariant");
        _spp_to_inv_list.add(ci);

	ci = new ControlInfo();
	ci.type = ctypes; ci.staticPoint = "false";
	ci.op = "field"; ci.write = "false";
	ci.invariants.add("diduce.invariants.IntValInvariant");
	ci.invariants.add("diduce.invariants.BaseTypeInvariant");
        _spp_to_inv_list.add(ci);

	ci = new ControlInfo();
	ci.type = ctypes; ci.staticPoint = "false";
	ci.op = "field"; ci.write = "true";
	ci.invariants.add("diduce.invariants.IntValInvariant");
	ci.invariants.add("diduce.invariants.IntDiffInvariant");
	ci.invariants.add("diduce.invariants.BaseTypeInvariant");
        _spp_to_inv_list.add(ci);

	ci = new ControlInfo();
	ci.type = "long"; ci.write = "false";
	ci.invariants.add("diduce.invariants.LongValInvariant");
        _spp_to_inv_list.add(ci);

	ci = new ControlInfo();
	ci.type = "long"; ci.op = "field";
	ci.staticPoint = "false"; ci.write = "false";
	ci.invariants.add("diduce.invariants.LongValInvariant");
	ci.invariants.add("diduce.invariants.BaseTypeInvariant");
        _spp_to_inv_list.add(ci);
	
	ci = new ControlInfo();
	ci.type = "long"; ci.op = "field";
	ci.staticPoint = "false"; ci.write = "true";
	ci.invariants.add("diduce.invariants.LongValInvariant");
	ci.invariants.add("diduce.invariants.LongDiffInvariant");
	ci.invariants.add("diduce.invariants.BaseTypeInvariant");
        _spp_to_inv_list.add(ci);

	ci = new ControlInfo();
	ci.type = "object|array"; ci.write = "false";
	ci.invariants.add("diduce.invariants.TypeInvariant");
        _spp_to_inv_list.add(ci);

	ci = new ControlInfo();
	ci.type = "object|array"; ci.write = "true";
	ci.invariants.add("diduce.invariants.TypeInvariant");
	ci.invariants.add("diduce.invariants.TypeChangeInvariant");
        _spp_to_inv_list.add(ci);

	ci = new ControlInfo();
	ci.type = "object|array"; ci.write = "false";
	ci.op = "field"; ci.staticPoint = "false";
	ci.invariants.add("diduce.invariants.TypeInvariant");
	ci.invariants.add("diduce.invariants.BaseTypeInvariant");
        _spp_to_inv_list.add(ci);

	ci = new ControlInfo();
	ci.type = "object|array"; ci.write = "true";
	ci.op = "field"; ci.staticPoint = "false";
	ci.invariants.add("diduce.invariants.TypeInvariant");
	ci.invariants.add("diduce.invariants.TypeChangeInvariant");
	ci.invariants.add("diduce.invariants.BaseTypeInvariant");
        _spp_to_inv_list.add(ci);
    }

    if (Runtime._verbose)
    {
        System.out.println ("The following invariants are being tracked:");
        for (ListIterator it = _spp_to_inv_list.listIterator() ; it.hasNext() ; )
            System.out.println (it.next());
        System.out.println ("**************************************************");
        System.out.println ();
    }
}

public Invariant[] get_invariants () { return _invariants; }

/* given an SPP descr, returns a vector containing names of
   invariants which shd be tracked for that SPP */
private Collection get_inv_names_for_SPP (String spp_descriptor)
{
    Collection c = new ArrayList();
    for (ListIterator it = _spp_to_inv_list.listIterator() ; it.hasNext() ; )
    {
	/*
	    String spec_string = (String) it.next();
        StringTokenizer st = new StringTokenizer (spec_string);
	*/

        // need the foll. check for allowing blank lines in the 
        // control file. can't be done when reading these lines in
        // from the control file, because we don't have a tokenizer there.
	/*
        if (!st.hasMoreTokens())
            continue; 
        String spp_spec = st.nextToken ();
	*/	
	
	ControlInfo spp_spec = (ControlInfo) it.next();	

        if (SPPNumAssigner.matches_spp_descriptor (spp_descriptor, spp_spec))
        {

	    return spp_spec.invariants;

            // init the c for every line that matches
            // earlier bug was that successive lines which matched were 
            // getting added on to the collection.
	    /*
            c = new ArrayList();
            while (st.hasMoreTokens ())
                c.add (st.nextToken());
	    */
        }
    }

    return c;
}

public SPPData (String spp_descriptor)
{
    _spp_descriptor = spp_descriptor;

    Collection c = get_inv_names_for_SPP (spp_descriptor);
// System.out.println ("------");
// System.out.println (" c = " + c + ", " + c.size() + " elements");
// for (Iterator it = c.iterator () ; it.hasNext() ; )
// System.out.println (" elem = " + it.next());

   if (c == null) System.out.println ("going to crash: spp " + spp_descriptor);

    _invariants = new Invariant[c.size ()];

    int next_inv_index = 0;
    for (Iterator it = c.iterator() ; it.hasNext() ; )
    {
        String classname = (String) it.next();
        try {
            _invariants[next_inv_index] = (Invariant) Class.forName (classname).newInstance();
			next_inv_index++;
        } catch (Exception cnfe) { 
            // catch class not found and instantiation exceptions
            util.fatal ("Could not instantiate invariant of type: " + classname);
        }
        total_invariants_created++;
    }

// System.out.println (" spp descriptor: " + spp_descriptor);
// System.out.println (" number of invariants is " + _invariants.length);
// for (int i = 0 ; i < _invariants.length ; i++) { System.out.println (" _invariants[i] = " + _invariants[i]); }
// System.out.println ("------");

}

public String getSPPDesc ()
{
    return _spp_descriptor;
}

public String toString ()
{
    StringBuffer sb = new StringBuffer ();

    // todo: need description for all the inv from somewhere!
    sb.append ("Descriptor: " + _spp_descriptor + "\n");
    if (_invariants != null)
    {
        for (int i = 0 ; i < _invariants.length ; i++)
            sb.append (i + " : " + _invariants[i] + "\n");
    }
    return sb.toString();
}

}
