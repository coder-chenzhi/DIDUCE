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

/* this class is purely related to instrumentation, i.e.
will be used only by diduce.watch. */
public class StaticProgramPoint implements Serializable {

// SPP descriptor is a string of one of the forms described in docs:

// 4 basic types: field, array, param, retval
// maybe later, lvars, array length, array index, param instrumentation within a procedure

String _op, _method, _type, _static, _write, _target, _num, _lineno;

public StaticProgramPoint (String op, String method, String lineno)
{
    _op = op;
    _method = method;
    _lineno = lineno;
    _type = _static = _write = _target = _num = "*";
}

public void set_target (String s) { _target = s; }
public void set_type (String s) { _type = s; }
public void set_write (String s) { _write = s; }
public void set_static (String s) { _static = s; }
public void set_num (String s) { _num = s; }

public static String mapTypeCharToString (char x)
{
    x = Character.toUpperCase (x);

    switch (x)
    {
      case 'B': return ("byte");
      case 'C': return ("char");
      case 'Z': return ("boolean");
      case 'S': return ("short");
      case 'I': return ("int");
      case 'J': return ("long");
      case 'F': return ("float");
      case 'D': return ("double");
      case 'L': return ("object");
      case '[': return ("array");
    }
    return "Unknown type " + x + "!!!";
}

// returns value of an attribute of the form x=b from the
// passed spp_descriptor string
// if attrib is not found, returns null
public static String get_attribute (String s, String attrib_name)
{
// none of these string indices shd go out of bounds
    int x = s.indexOf (attrib_name + "=");
    // x is index of where $attrib-name= starts
	if (x < 0)
	    return null;

    x = x + attrib_name.length() + 1;
    // x is index just beyond $attrib-name=
	// therefore x is at starting of value of attrib
    s = s.substring (x);
    int y = s.indexOf ("-");

    // return uptil next '-' or end of string
    return (y >= 0) ? s.substring (0, y) : s;
}

private static String get_static_or_instance_string (String s)
{
    return s.indexOf("static=true") >= 0 ? "static" : "instance";
}

private static String get_read_or_write_string (String s)
{
    return s.indexOf("write=true") >= 0 ? "write" : "read";
}

/* IMPORTANT:
   spp.tostring and spp.printsppdescription have to be in sync
   because printsppdescr parses the format output by tostring
   Also output of printsppdescr is parsed by diduce.show (in gui)
   so those also have to be compatible
*/

public String toString ()
{
    StringBuffer sb = new StringBuffer();
    if (!_op.equals ("*")) sb.append ("op=" + _op);
    if (!_num.equals ("*")) sb.append ("-num=" + _num);
    if (!_method.equals ("*")) sb.append ("-method=" + _method);
    if (!_target.equals ("*")) sb.append ("-target=" + _target);
    if (!_type.equals ("*")) sb.append ("-type=" + _type);
    if (!_write.equals ("*")) sb.append ("-write=" + _write);
    if (!_static.equals ("*")) sb.append ("-static=" + _static);
    if (!_lineno.equals ("*")) sb.append ("-line=" + _lineno);

    return sb.toString();
}

// returns a user-friendly description of the op
public static String get_op_description (String spp_descriptor)
{
    String op = get_attribute (spp_descriptor, "op");

    if (op.equals ("retval"))
    {
        String target = StaticProgramPoint.get_attribute (spp_descriptor, "target");
        util.ASSERT (target != null);
        target = util.strip_package_name (target);

        return "Return value of " + target;
    }
    else if (op.equals ("param"))
    {
        String target = StaticProgramPoint.get_attribute (spp_descriptor, "target");
        util.ASSERT (target != null);
        target = util.strip_package_name (target);
        String num = StaticProgramPoint.get_attribute (spp_descriptor, "num");
        util.ASSERT (num != null);

        return "Param #" + num + " of " + target;
    }
    else if (op.equals ("field"))
    {
        String s = StaticProgramPoint.get_attribute (spp_descriptor, "static");
        boolean is_static = false;
        if (s != null)
            is_static = s.equals ("true");

        String w = StaticProgramPoint.get_attribute (spp_descriptor, "write");
        boolean is_write = false;
        if (w != null)
            is_write = w.equals ("true");

        String target = StaticProgramPoint.get_attribute (spp_descriptor, "target");
        util.ASSERT (target != null);
        target = util.strip_package_name (target);

        return (is_write ? "write" : "read") + " to " + target + " (" +
               (is_static ? "static" : "instance") + " field)";
    }
    else if (op.equals ("array"))
    {
        String w = StaticProgramPoint.get_attribute (spp_descriptor, "write");
        boolean is_write = false;
        if (w != null)
            is_write = w.equals ("true");

        return "Array " + (is_write ? "write" : "read");
    }
    else 
        util.fatal ("unknown op in SPP descriptor: " + spp_descriptor);

    // should not reach here
    return "";
}

}
