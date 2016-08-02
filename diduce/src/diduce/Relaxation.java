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

import java.io.*;
import java.util.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

public class Relaxation implements java.io.Serializable
{

static int _next_id = 0;
int _id;
float _conf_change;
int _lineno, _n_samples;
String _spp_descriptor, _what, _inv_type, _new_value, _old_value, _where, _context;

public Relaxation (String spp_descriptor, float conf_change, String inv_type, String old_value, String new_value, int n_samples)
{
    this (spp_descriptor, conf_change, inv_type, old_value, new_value, n_samples, null);
}

public Relaxation (String spp_descriptor, float conf_change, String inv_type, String old_value, String new_value, int n_samples, String context)
{
    _spp_descriptor = spp_descriptor;
    _context = context;
    String op = StaticProgramPoint.get_attribute (spp_descriptor, "op");

    // context = null => we're called from diduce.run
    // context != null => we're called from diduce.show
    if (context == null)
    {
        Exception e = new Exception();
        e.fillInStackTrace();
        StringWriter sw = new StringWriter();
        e.printStackTrace (new PrintWriter (sw, true));
        _context = sw.toString();
    }
    else
        _context = context;

    _where = StaticProgramPoint.get_attribute (spp_descriptor, "method");
       
    _conf_change = conf_change;
    _inv_type = inv_type;
    _old_value = old_value;
    _new_value = new_value;
    _n_samples = n_samples;
    _what = StaticProgramPoint.get_op_description (spp_descriptor);
    String line = StaticProgramPoint.get_attribute (spp_descriptor, "line");
    try {
       _lineno = Integer.parseInt (line);
    } catch (NumberFormatException nfe) { _lineno = 0; }


    // let's always gather relaxations, regardless of gui, so we
    // can save them to invariants file.
    // we should probably use a quiet option.
    diduce.Runtime._relaxations.add (this);
    _id = diduce.Runtime._relaxations.size();

    if (Runtime._verbose)
        Runtime._outstream.print (this);
}

public float get_conf_change() { return _conf_change; }
public String get_what() { return _what; }
public String get_inv_type() { return _inv_type; }
public String get_old_value() { return _old_value; }
public String get_new_value() { return _new_value; }
public int get_n_samples() { return _n_samples; }
public String get_where() { return _where; }
public int get_lineno () { return _lineno; }
public String get_context () 
{
    String context = _context;
    int x = context.lastIndexOf ("diduce");
    // example of context at this point:
    // java.lang.Exception     at diduce.Relaxation.<init>(Relaxation.java:27) at diduce.Runtime.mapSPPToDataObj(Runtime.java:301)     at diduce.Runtime.register_static_ref_load(Runtime.java:251)    at iodine.Env.<init>(Env.java:60)       at iodine.main.main(main.java:91)

    if (x >= 0)
        context = context.substring (x);
    // example of context at this point:
    // diduce.Runtime.register_static_ref_load(Runtime.java:251)    at iodine.Env.<init>(Env.java:60)       at iodine.main.main(main.java:91)
    x = context.indexOf (" ");
    if (x >= 0)
        context = context.substring (x);
    // example of context at this point:
    //     at iodine.Env.<init>(Env.java:60)       at iodine.main.main(main.java:91)
    while (context.charAt (0) == 1)
        context = context.substring (1);
    // example of context at this point:
    // at iodine.Env.<init>(Env.java:60)       at iodine.main.main(main.java:91)

//    System.out.println (context);
    return context;
}

public String toString ()
{
    return ("[DIDUCE Invariant Violation #" + _id + "] Context is: \n" + get_context() + "\n");
}
}
