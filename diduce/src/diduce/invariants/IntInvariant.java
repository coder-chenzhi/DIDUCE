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

public class IntInvariant extends diduce.invariants.Invariant implements java.io.Serializable {

boolean _is_initialized;
int _value, _value_mask, _lattice_jumps;

public void newSample (int x, String spp_descriptor, String invariant_name)
{
    _nSamples ++;
    if (!_is_initialized)
    {
        _is_initialized = true;
        _value = x;
        _value_mask = 0xffffffff;
    }
    else
    {
        if ((_value & _value_mask) != (x & _value_mask))
        {
            if (diduce.Runtime._check_violations)
            {
                float orig_conf = this.invariantStrength();
                int orig_value_mask = _value_mask;
                int common_bits = ~ (_value ^ x);
                _value_mask &= common_bits;
                _lattice_jumps ++;
                // strength after
                float conf_change = this.invariantStrength() - orig_conf;

                String old;

                if (_lattice_jumps == 1)
                    old = "Always 0x" + Integer.toHexString (_value);
                else 
                {
                    old = "matched 0x" + Integer.toHexString (_value)
                        + " in bits 0x" + Integer.toHexString (orig_value_mask);
                }

                String newstr = "0x" + Integer.toHexString (x);
                Relaxation r = new Relaxation (spp_descriptor, conf_change, invariant_name, old, newstr, _nSamples-1);
            }
        }
    }
}

public String toString ()
{
    String old;
    if (_lattice_jumps == 0)
        old = "Always 0x" + Integer.toHexString (_value);
    else
    {
        old = "matched 0x" + Integer.toHexString (_value)
            + " in bits 0x" + Integer.toHexString (_value_mask);
    }

    if (_nSamples != 1)
        return old + " (" + _nSamples + " times)";
    else
        return old + " (once)";
}

public float invariantStrength ()
{
    int x = _value_mask;
    int bits_not_invariant = 0;

    for (int i = 0; i < 32 ; i++)
    {
        if ((x & 1) == 0)
           bits_not_invariant ++;
        x >>= 1;
    }

    // avoid problems with 1 << 31 or 1 << 32 going to a -ve value/overflow
    if (bits_not_invariant <= 30)
        return ((float) _nSamples)/(1 << bits_not_invariant);
    else if (bits_not_invariant == 31)
        return _nSamples/(((float) 2.0) * (1 << 30));
    else
        return _nSamples/(((float) 4.0) * (1 << 30));
}
}
