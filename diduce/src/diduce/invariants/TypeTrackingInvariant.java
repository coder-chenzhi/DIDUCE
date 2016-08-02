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

public class TypeTrackingInvariant extends Invariant implements java.io.Serializable {

String _sample_class_name;
int _value, _value_mask, _lattice_jumps;
boolean _is_initialized;

public void newSample (Object o, String spp_descriptor, String invariant_name)
{
    _nSamples ++;
    String cname = (o == null) ? null : o.getClass().getName();
    int x = (cname == null) ? 0 : cname.hashCode();

    if (!_is_initialized)
    {
        _is_initialized = true;
        _sample_class_name = cname;
        if (_sample_class_name == null)
            _sample_class_name = "null";
        _value = x;
        _value_mask = 0xffffffff;
    }
    else
    {
        if ((_value & _value_mask) != (x & _value_mask))
        {
            // option here would be to make _sample the superclass of
            // _sample and o

            if (diduce.Runtime._check_violations)
            {
                float orig_conf = this.invariantStrength();
                int orig_value_mask = _value_mask;  // reqd for debug mesg below
                int common_bits = ~ (_value ^ x);
                _value_mask &= common_bits;
                _lattice_jumps ++;
                float conf_change = this.invariantStrength() - orig_conf;

                String old = "";
                if (_lattice_jumps == 1)
                    old = "Always " + util.delete_to_last_dot (_sample_class_name);
                else //if (_lattice_jumps >= 1)
                {
                    old = "started as " + util.delete_to_last_dot (_sample_class_name)
                        + " hashcode matched 0x" + Integer.toHexString (_value)
                        + " in bits 0x" + Integer.toHexString (orig_value_mask);
                }

                if (cname == null)
                    cname = "null";

                Relaxation r = new Relaxation (spp_descriptor, conf_change, invariant_name, old, util.delete_to_last_dot (cname), _nSamples);
            }
        }
    }
}

public String toString ()
{
    String old = "";
    if (_lattice_jumps == 0) // if it's constant, lattice jumps shd be 0
        old = "Always " + util.delete_to_last_dot (_sample_class_name);
    else 
    {
        old = "started as type " + util.delete_to_last_dot (_sample_class_name)
            + ", hashcode matched 0x" + Integer.toHexString (_value)
            + " in bits 0x" + Integer.toHexString (_value_mask);
    }

    if (_nSamples != 1)
        return old + " (" + _nSamples + " times)";
    else
        return old + " (once)";
}

public float invariantStrength ()
{
    /* we want to deliberately give it a high invariant rating
for a different type. lattice_jump+1 vaguely approximates
the number of different types seen at that site.
will this be a problem when displaying invariants
(artificially high invariant ratings) ? */

    return ((float) _nSamples)/(_lattice_jumps+1);

}
}
