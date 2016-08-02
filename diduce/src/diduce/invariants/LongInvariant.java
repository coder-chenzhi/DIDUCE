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

public class LongInvariant extends diduce.invariants.Invariant implements java.io.Serializable {

boolean _is_initialized;
long _value, _value_mask, _lattice_jumps;

public void newSample (long x, String spp_descriptor, String invariant_name)
{
    _nSamples ++;
    if (!_is_initialized)
    {
        _is_initialized = true;
        _value = x;
        _value_mask = 0xffffffffffffffffL;
    }
    else
    {
        if ((_value & _value_mask) != (x & _value_mask))
        {
            if (diduce.Runtime._check_violations)
            {
                float orig_conf = this.invariantStrength();
                long orig_value_mask = _value_mask;
                long common_bits = ~ (_value ^ x);
                _value_mask &= common_bits;
                _lattice_jumps ++;
                // strength after
                float conf_change = this.invariantStrength() - orig_conf;

                String old;

                if (_lattice_jumps == 1)
                    old = "Always 0x" + Long.toHexString (_value);
                else 
                {
                    old = "matched 0x" + Long.toHexString (_value)
                        + " in bits 0x" + Long.toHexString (orig_value_mask);
                }

                String newstr = "0x" + Long.toHexString (x);
                Relaxation r = new Relaxation (spp_descriptor, conf_change, invariant_name, old, newstr, _nSamples-1);
            }
        }
    }
}

public String toString ()
{
    StringBuffer sb = new StringBuffer ();

    String old;
    if (_lattice_jumps == 0)
        old = "Always 0x" + Long.toHexString (_value);
    else
    {
        old = "matched 0x" + Long.toHexString (_value)
            + " in bits 0x" + Long.toHexString (_value_mask);
    }

    sb.append (old);

    if (_nSamples != 1)
        return old + " (" + _nSamples + " times)";
    else
        return old + " (once)";
}

public float invariantStrength ()
{
    long x = _value_mask;
    int bits_not_invariant = 0;

    for (int i = 0; i < 64 ; i++)
    {
        if ((x & 1) == 0)
           bits_not_invariant ++;
        x >>= 1;
    }

    if (bits_not_invariant <= 62)
        return ((float) _nSamples)/(((long)1) << bits_not_invariant);
    else if (bits_not_invariant == 63)
        return _nSamples/(((float) 2.0) * (((long)1) << 62));
    else
        return _nSamples/(((float) 4.0) * (((long)1) << 62));
}
}
