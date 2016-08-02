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

import java.io.Serializable;

public class Counter implements Serializable {

long _max, _min;
long _total;
long _nSamples;

public Counter() {
   _min = Integer.MAX_VALUE;
   _max = Integer.MIN_VALUE;
}

/* this function bias'es all samples recd so far with an offset value 
   adjust totals and min/max */

void bias (long bias)
{
    _total += bias * _nSamples;
    _min += bias;
    _max += bias;
}

void newSample (int sample)
{
    if (sample < _min)
        _min = sample;
    if (sample > _max)
        _max = sample;
    _nSamples ++;
    _total += sample;
}

public void merge (Counter c)
{

    if (c._max > _max)
        _max = c._max;
    if (c._max < _min)
        _max = c._min;
    _nSamples += c._nSamples;
    _total += c._total;
}

public long getTotal ()
{
    return _total;
}

public long getNSamples ()
{
    return _nSamples;
}

public String toString ()
{
    return ("total: " + _total + " (" + _nSamples + " samples, avg " 
          + ((double) _total)/_nSamples + ", min " + _min + ", max " + _max + ")");

}
}

