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

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

public class util
{

public static void fatal (String s)
{
    System.err.println ("\n-----------------------------------------------------------------------");
    System.err.println ("FATAL ERROR: " + s);
    System.exit (32);
}

public static void fatal (Exception e)
{
    System.err.println ("\n-----------------------------------------------------------");
    System.err.println ("FATAL ERROR, Exception thrown: " + e);
    e.fillInStackTrace();
    e.printStackTrace();
    System.exit (32);
}


public static void warn (String s)
{
    System.err.println ("WARNING: " + s);
    System.exit (32);
}

public static void ASSERT (boolean b)
{
    if (!b)
    {
        System.out.println ("Assertion failed!\n");
        throw new RuntimeException();
    }
}

// does a limited RE match (support for * at the beginning and/or end)
// between s1 and s2. Only one of s1 and s2 can have *
public static boolean limited_re_match (String s1, String s2)
{
    if (s2.indexOf('*') >= 0) { String tmp = s1; s1 = s2; s2 = tmp; }

    // s1 is now the string which has '*'s if any

    if (s1.equals ("*"))
        return true;
    else if (s1.endsWith ("*") && s1.startsWith ("*"))
    {
        if (s2.indexOf (s1.substring (1, s1.length()-1)) >= 0)
            return true;
    }
    else if (s1.endsWith ("*"))
    {
        if (s2.startsWith (s1.substring (0, s1.length()-1)))
            return true;
    }
    else if (s1.startsWith ("*"))
    {
        if (s2.endsWith (s1.substring (1, s1.length())))
            return true;
    }
    else if (s1.equals (s2))
        return true;

    return false;
}

// maps method sigs to classes they belong to
// e.g. a.b.m to a.b
// a.b.m(somesig)V to m(somesig)V
public static String delete_after_last_dot (String s)
{
    int x = s.lastIndexOf ('.');
    if (x >= 0)
        s = s.substring (0,x);

    return s;
}

// maps method sigs to classes they belong to
// e.g. a.b.m to m
// a.b.m(somesig)V to m(somesig)V
public static String delete_to_last_dot (String s)
{
    int x = s.lastIndexOf ('.');
    if (x >= 0)
        s = s.substring (x+1);

    return s;
}

// converts fq names to simple names
// e.g. a.b.c.d to c.d
// a.b.c.d(somesig)V to c.d(somesig)V

public static String strip_package_name (String s)
{
    // System.out.print ("input is " + s);

    String sig = "";
    String full_name = s;

    // first split based on whether there is a sig component in s
    int x = s.indexOf ('(');
    if (x >= 0)
    {
        full_name = s.substring (0, x);
        sig = s.substring (x);
    }

    int y = full_name.lastIndexOf ('.'); // y shd be > 0
    String class_name = full_name.substring(0, y);

    int z = class_name.lastIndexOf ('.');
    if (z >= 0)
        full_name = full_name.substring (z+1);
    else
    {
        z = class_name.lastIndexOf ('/');
        if (z >= 0)
            full_name = full_name.substring (z+1);
    }

    //        System.out.print ("output is " + full_name + sig);
    return full_name + sig;
}


}
