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

abstract public class Invariant implements java.io.Serializable {

public int _nSamples;

abstract public float invariantStrength ();

public void newSample (Object o, int new_val, int old_val, String spp_descr, int spp_num) 
{
    System.out.println ("object o: " + o); 
    System.out.println ("new val = " + new_val + ", old val = " + old_val);
    System.out.println ("spp descriptor " + spp_descr);
    System.out.println ("this invariant's class = " + this.getClass() + "\nthis invariant = " + this);
    util.ASSERT (false); 
    util.ASSERT (false); 
    util.fatal ("Invariant not implemented!"); 
}

public void newSample (Object o, long new_val, long old_val, String spp_descr, int spp_num) 
{ 
    System.out.println ("object o: " + o); 
    System.out.println ("new val = " + new_val + ", old val = " + old_val);
    System.out.println ("spp descriptor " + spp_descr);
    System.out.println ("this invariant's class = " + this.getClass() + "\nthis invariant = " + this);
    util.ASSERT (false); 
    util.fatal ("Invariant not implemented!");
}

public void newSample (Object o, Object new_val, Object old_val, String spp_descr, int spp_num) { util.ASSERT (false); util.fatal ("Invariant not implemented!"); }
public void newSample (Object o, float new_val, float old_val, String spp_descr, int spp_num) { util.ASSERT (false); util.fatal ("Invariant not implemented!"); }
public void newSample (Object o, double new_val, double old_val, String spp_descr, int spp_num) { util.ASSERT (false); util.fatal ("Invariant not implemented!"); }

// default inv. description is simply its name 
public String get_description () { return this.getClass().getName(); };
}
