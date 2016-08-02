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

public class IntMinMaxInvariant extends diduce.invariants.Invariant implements java.io.Serializable {

boolean _is_initialized;
int _min, _max;

public void newSample (int x, String spp_descriptor, String invariant_name)
{
    _nSamples ++;

    if (!_is_initialized)
    {
        _is_initialized = true;
        _min = x;
        _max = x;
    }
    else
    {
        if (x < _min)
        {
            if (diduce.Runtime._check_violations)
            {
                float conf_prev = invariantStrength();
                 _min = x;
                float conf_now = invariantStrength();
                float conf_change = conf_now - conf_prev;

                Relaxation r = new Relaxation (spp_descriptor, conf_change, "Min " + invariant_name, Integer.toString(_min), Integer.toString(x), _nSamples-1);
             }
             _min = x; // must do this anyway - diduce.Runtime._check_violations may not be on
        } 
        else if (x > _max)
        {
            if (diduce.Runtime._check_violations)
            {
                float conf_prev = invariantStrength();
                _max = x;
                float conf_now = invariantStrength();
                float conf_change = conf_now - conf_prev;

                Relaxation r = new Relaxation (spp_descriptor, conf_change, "Max " + invariant_name, Integer.toString (_max), Integer.toString (x), _nSamples-1);
             }
             _max = x; // must do this anyway - diduce.Runtime._check_violations may not be on
        } 
    }
}

public String toString ()
{
    return (_nSamples + " samples, Min: " + _min + ", Max: " + _max);
}

public float invariantStrength ()
{
    float diff = _max - _min;

    // nSamples shd never be 0 when invariant confidence is called
    return diff/_nSamples; 
}
}
