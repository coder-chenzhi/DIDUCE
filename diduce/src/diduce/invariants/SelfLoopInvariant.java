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
    
package diduce.invariants;

import java.util.*;
import java.io.*;
import diduce.*;

public class SelfLoopInvariant extends diduce.invariants.Invariant implements java.io.Serializable {

boolean _self_loop, both; // self_loop tracks which state was seen first
                          // if both is true, both possible states were seen
boolean _is_initialized;

public void newSample (Object o, Object new_val, Object old_val, String spp_descriptor, int spp_num) 
{
    boolean this_state = (o == new_val);

// System.out.println ((o == null) ? "o is null" : " o is nonnull");
// System.out.println ((new_val == null) ? "new_val is null" : " new_val is nonnull");
// System.out.println ((old_val == null) ? "old_val is null" : " old_val is nonnull");

// System.out.println ("this state = " + this_state + " self_loop = " + _self_loop + " both = " + both);

    if (!_is_initialized)
    {
        _is_initialized = true;
        _self_loop = this_state;
		both = false;
    }
    else
    {
        String invariant_name = "object field points to itself";
        if ((!both) && (this_state != _self_loop))
        {
            if (diduce.Runtime._check_violations)
            {
                float conf_change = invariantStrength(); // conf after is 0, so current conf is same as conf. change
                String old_str = _self_loop ? "Always" : "Never";
                String new_str = this_state ? "Now it does" : "Now it does not";
    
                Relaxation r = new Relaxation (spp_descriptor, conf_change, invariant_name, old_str, new_str, _nSamples);
            }

    	    both = true;
        }
    }

    _nSamples ++;
}
    
public String get_description () { return "self loop"; }

public String toString ()
{
    return (_nSamples + " samples, " + (_self_loop ? "Always" : "Never"));
}

public float invariantStrength ()
{
     return (both) ? (float) 0.0 : (float) _nSamples;
}

}
