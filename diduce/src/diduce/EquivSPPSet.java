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

class EquivSPPSet implements Serializable {

Collection _spp_specs; // the vector of spp specs which are all equivalent

// the assigned spp num for this SPP. if pip num is uninit'ed, it has value 1
int _pip_num; 

private static final boolean debug = false;

public EquivSPPSet (List set)
{
    _pip_num = -1;
    _spp_specs = set;
}

/** returns true if spp_descriptor matches any of the terms
  in this equivSPPSet 
  */
public boolean match_spp (StaticProgramPoint spp)
{
	for (Iterator it = _spp_specs.iterator(); it.hasNext() ; )
	{
	    ControlInfo next_spp_spec = (ControlInfo) it.next();
            if (SPPNumAssigner.matches_spp_descriptor (spp, next_spp_spec)) {
		if (debug) {
	    	    System.out.println("match_spp(): spp=" + spp +
				"    Matches equiv set point="
				+ next_spp_spec); 
		}
		return true;
	    }
        }
    if (debug) {
        System.out.println (
	    "match_spp(): any spp in set returns false for given point:" +
	    "spp=" + spp);
    	System.out.println(" and set =" + _spp_specs);
    }
    return false;
}

/* if equiv spp set is not init'ed with a pip num, -1 is returned */
public int get_pip_num () { return _pip_num; }

/* pip num's shd be set for an equiv spp set only once. therefore
if we are setting it, it shd have been -1 (uninit'ed)
*/
public void set_pip_num (int n) { util.ASSERT (_pip_num == -1); _pip_num = n; }

public String toString ()
{
    StringBuffer sb = new StringBuffer("EquivSet: ");

	for (Iterator it = _spp_specs.iterator(); it.hasNext() ; )
	    sb.append (it.next());

    return sb.toString();
}

}

