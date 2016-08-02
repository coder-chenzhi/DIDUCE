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

import diduce.xml.*;
import java.util.regex.Pattern;

/* 
this class assigns spp numbers to spp's. 
provides services to optionally suppress
some program points, or have multiple program
points share the same spp num.
*/

class SPPNumAssigner implements Serializable {

static final long serialVersionUID = -7639672330788283589L;

private List _instrumentation_constraints, _equiv_spp_sets;
private String _constraints_filename;

private static final boolean debug = false;

// the first few pip nums are assigned to the equiv sets 
// (all equiv sets have a pip num assigned whether they
// match any real program point or not - since they are expected
// to be few in number, this is not a real problem)
// _next_pip_num is set to _next_equiv_set_pip_num after we've
// parsed all the equiv sets, so that they can be assigned
// to future instrumentation points.

private int _next_equiv_set_pip_num; // this tracks the equiv set pip num
private int _next_pip_num; // this tracks the pip num
                           // we'll assign to the next new SPP

// _spps contains a list of all assigned spp's.
// the first <n> entries are always assigned for the _equiv_spp_sets
// and are never deallocated.
// the spp number for an equiv set is always just the position of the
// equiv spp set in the list _equiv_spp_sets. The strings entered
// for these positions in the spp array are just dummy strings containing
// the original "=" line in the instrumentation constraints file.
// spp numbers can get released (when classes get updated and release their
// old numbers) - in this case the corresponding entry in _spps is set to null.
// a null entry in _spps means that number can be reused.

// _next_pip_num ALWAYS points to the lowest numbered spp which MAY be free.
// i.e. definitely no lower numbered entry is free, but _next_pip_num and higher entries
// may be free.

private ArrayList _spps;

/** initializes a spp num assigner project from the given prop_filename.
prop_filename should have lines like:
+ <descriptor> to indicate descriptors which should be instrumented
- <descriptor> to indicate descriptors which should NOT be instrumented
= <descriptor1> <descriptor2> to indicate descriptors which should be treated
as the same, - a single set of invariants maintained for ALL descriptors which
match any of descriptor1, descriptor2, etc.
  */

public SPPNumAssigner (String prop_filename)
{
    _next_pip_num = 0;
    _next_equiv_set_pip_num = 0;
    _spps = new ArrayList();
    _equiv_spp_sets = new ArrayList ();
    _instrumentation_constraints = new ArrayList();
    _constraints_filename = prop_filename;

    try {

	if (_constraints_filename != null)
    	{
            System.out.println ("Instrumentation constraints filename is " + prop_filename);
	    XmlInputParser parser = new XmlInputParser(_constraints_filename);
	    parser.parse();
	    _instrumentation_constraints = XmlInputParser.constraints;
	    Collection equivSets = XmlInputParser.equivSets;

            if (equivSets != null)
            {
		List nextSet;
		for (Iterator iter = equivSets.iterator(); iter.hasNext(); ) { 
		    nextSet = (List) iter.next();
                    EquivSPPSet ess = new EquivSPPSet(nextSet);
		    ess.set_pip_num (_next_equiv_set_pip_num);
                    _next_equiv_set_pip_num++;
                    _equiv_spp_sets.add (ess);
                    _spps.add ("Equivalent points: " + nextSet);
		}
            } 
        }
    } catch (Exception e) {
       e.printStackTrace();
       util.fatal ("Unable to initialise class StaticProgramPoint: " + e);
    }
	    // disable instrumentation on float's and double's by default.
    ControlInfo disableFloat = new ControlInfo();
    disableFloat.isWatch = false;
    disableFloat.type = "float";	
    _instrumentation_constraints.add(disableFloat);

    ControlInfo disableDouble = new ControlInfo();
    disableDouble.isWatch = false;
    disableDouble.type = "double";	
    _instrumentation_constraints.add(disableDouble);

    if (_constraints_filename != null)
    {
        System.out.println (_instrumentation_constraints.size() - 2 // -2 for float/double which we introduced
                           + " constraint(s) read from file \"" 
                           + _constraints_filename + "\"");
        System.out.println (_equiv_spp_sets.size() 
                           + " set(s) of equivalent program points");
    }

    // 0 to equiv_spp_sets-1 are reserved for the equiv sets
    // so start allocating pips from the next available number
    _next_pip_num = _equiv_spp_sets.size();
}

/**
  * a null object in the _spps arraylist indicates a free spp number.
 */
private int allocate_a_free_pip(String desc)
{
    util.ASSERT (_next_pip_num <= _spps.size());
    while (true)
    {
        if (_next_pip_num == _spps.size())
        {   // there is no free number in the existing _spps
            _spps.add (desc);
            _next_pip_num++;
            return _next_pip_num-1;
        }
        else if (_spps.get (_next_pip_num) == null)
        {
            _spps.set (_next_pip_num, desc);
            _next_pip_num++;
            return _next_pip_num-1;
        }
        else
            _next_pip_num++;
    }
}

/** releases all the spp numbers associated with the given class.
    can be expensive, has to go down the list of all spps to
    determine if it belongs to the given class. this determination
    is made based on the spp descriptor stores in the _spps arraylist
 */
public void release_pip_nums_for_class (String classname)
{
    int s = _spps.size();
    for (int i = _equiv_spp_sets.size(); i < s ; i++)
    {
        String str = (String) _spps.get (i);
        // str: method=a.b.c.d(Lx.y)V,op=field,type=int,target=....
        str = StaticProgramPoint.get_attribute (str, "method");
        // str: a.b.c.d(Lx.y)V
        str = str.substring (0, str.indexOf("("));
        // str: a.b.c.d
        str = str.substring (0, str.lastIndexOf("."));
        // str: a.b.c (d is the method name)
        if (str.equals(classname))
        {
            _spps.set (i, null);
            if (i < _next_pip_num)
               _next_pip_num = i;
        }
    }
}

public void print_instrumentation_constraints ()
{
    System.out.println ("# of Instrumentation constraints: " + _instrumentation_constraints.size());
    for (Iterator it = _instrumentation_constraints.iterator() ; it.hasNext(); )
        System.out.println (it.next());
}

/** returns the spp num for spp.
    takes care of instrumentation constraints (+ or - lines in constraint file)
    also takes care of spp numbers shared between multiple spp's
    (= lines in constraint file)
    returns < 0 if spp shd not be instrumented
 */
public int get_spp_num (StaticProgramPoint spp)
{
    util.ASSERT (_instrumentation_constraints != null);
    boolean isWatch = true; // by default we will instrument this point
    String desc = spp.toString (); // my descriptor

    // first get the directive for watching this spp
    for (Iterator it = _instrumentation_constraints.iterator() ; it.hasNext(); )
    {
        ControlInfo cInfo = (ControlInfo) it.next();

        // don't break out if it matches, we want to 
        // match the last descriptor in the constraints
        if (matches_spp_descriptor (spp, cInfo)) {
	    isWatch = cInfo.isWatch;
	}
    }

    // if the directive is set to not-watch, just return
    if (!isWatch)
    {
        diduce.watch.Log.println ("Not Instrumenting program point: " + spp);
        return -1;
    }

    // the directive is set to watch, we need to figure what spp num
    // to give this spp. first, check if this spp maps to any in the
    // list of equivalent spps. if so, the pip number is just the
    // pip number of the equiv set.

    for (ListIterator it = _equiv_spp_sets.listIterator() ; it.hasNext() ; )
    {
        EquivSPPSet ess = (EquivSPPSet) it.next();
        if (ess.match_spp (spp)) {
	    if (debug) {
		System.out.println("getSPPNum(): equiv pip: " +  ess.get_pip_num());
	    }
            return ess.get_pip_num();
	}
    }

    // it did not match any equivSPPSet, so get an unassigned spp number 
    int pip_num = allocate_a_free_pip (desc);
    if (debug) {
	System.out.println("getSPPNum(): Newly assigned pip: " +  pip_num);
    }
    return pip_num;
}

/* return true if every field in spec_value has a matching field in desc_value
   '*' is allowed for RE match at the beginning and end of spec_value 
   | are allowed for OR options in spec_value
   (neither of these is allowed in desc value)
*/
private static boolean RE_match (String desc_value, String spec_value)
{
    // spec_value not specified, default it matches.
    if (spec_value == null) {
	return true;
    }

    /** Not sure if this is right, need a revisit. Fixed as some desc fields
     * of default invariants are null
     */
    if (desc_value == null) {
	return true;
    }

    /**
     * The chars below are special chars in java.util.regex
     * They need to be treated as normal chars for our purpose,
     * hence escape them.
     */	
    char[] escapees = {'.', '[', '(', ')'};
    String escaped = escapeChars(spec_value, escapees);

    /**
     * The meaning of '*' (matches anything) is represented
     * as '.*' in java.util.regex  
     */
    escaped = replace(escaped, '*', ".*");
    return Pattern.matches(escaped, desc_value);
}

static String escapeChars(String pattern, char[] targets) {
    for (int i = 0; i < targets.length; i++) {
        pattern = replace(pattern, targets[i], "\\" + targets[i]);
    }
    return pattern;
}

private static String replace(String pattern,
		char target, String replacement) {
    StringBuffer sb = new StringBuffer();
    char ch;
    if (pattern.indexOf(target) == -1) {
        return pattern;
    }
    for (int i = 0; i < pattern.length(); i++) {
        ch = pattern.charAt(i);
        if (ch == target) {
            sb.append(replacement);
        } else {
            sb.append(ch);
        }
    }
    return sb.toString();
}


/** 
    returns true if the given SPP descriptor matches the given spec.
    (i.e. all fields in spec shd match)
    warning: order of the 2 params to this function is important.
*/
public static boolean matches_spp_descriptor (StaticProgramPoint descr,
						ControlInfo spec)
{
    if (!RE_match(descr._op, spec.op) ||
    	    !RE_match(descr._type, spec.type) ||
    	    !RE_match(descr._static, spec.staticPoint) ||
   	    !RE_match(descr._method, spec.method) ||
   	    !RE_match(descr._target, spec.target) ||
    	    !RE_match(descr._write, spec.write) ||
    	    !RE_match(descr._num, spec.num) ||
    	    !RE_match(descr._lineno, spec.lineno)) {
	return false;
    }
    return true;
}


/* 
returns true if the given SPP descriptor matches the given spec.
(i.e. all fields in spec shd match)
warning: order of the 2 params to this function is important.

 descriptor shd be in the form <attrib1>=<value1>-<attrib2=value2>- ...
star's can be present at the begin or end of values in the spec (but
not in the descriptor)

*/
public static boolean matches_spp_descriptor (String descriptor, ControlInfo spec)
{
    // spec not specified, default it matches.
    if (spec == null) {
	return true;
    }
    //System.out.println("descr:" + descriptor);
    if (!RE_match(StaticProgramPoint.get_attribute(descriptor, "op"),
		spec.op) ||
    	    !RE_match(StaticProgramPoint.get_attribute(descriptor, "type"),
		spec.type) ||
    	    !RE_match(StaticProgramPoint.get_attribute(descriptor, "static"),
		spec.staticPoint) ||
   	    !RE_match(StaticProgramPoint.get_attribute(descriptor, "method"),
		spec.method) ||
   	    !RE_match(StaticProgramPoint.get_attribute(descriptor, "target"),
		spec.target) ||
    	    !RE_match(StaticProgramPoint.get_attribute(descriptor, "write"),
		spec.write) ||
    	    !RE_match(StaticProgramPoint.get_attribute(descriptor, "num"),
		spec.num) ||
    	    !RE_match(StaticProgramPoint.get_attribute(descriptor, "lineno"),
		spec.lineno)) {
	return false;
    }
    return true;
}
}

