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

import java.lang.reflect.*;
import java.util.*;
import java.io.*;

class run {

private static String _version_str = "v1.0Beta";

private static String launch_prog_name = "java diduce.run";

private static void print_usage_and_die ()
{
    System.out.println (
    "Usage:\n" +
    launch_prog_name + " <options> <target program> <target program args>\n" +
    "Options can be:\n" +
    (launch_prog_name.equals("java diduce.run") ? "-r : Report violations\n" : "") +
    (launch_prog_name.equals("java diduce.run") ? "-g : Start GUI\n" : "") +
    "-v : Verbose mode\n" +
    "-t : Print timestamps\n" +
    "-i <file>   : Read invariants file\n" +
    "-o <file>   : Write invariants file\n" +
    "-p <file>   : Read diduce.run properties file\n" +
    "-nc <value> : Confidence change for execution of new code\n" +
    "-sp <path>  : Source path (list of directories separated with ':')\n" +
    "-h : this message\n");

    System.exit (0);
}

public static void main(String args[])
{
    // different thread group for gui threads so we can tell
    // when program threads have completed but gui threads are
    // remaining
    Thread gui_thread = null;
    ThreadGroup gui_thread_group = null;

//	diduce.Runtime.initRuntime();
    if (args.length < 1)
        print_usage_and_die();

    int argno = 0;
    for (argno = 0; argno < args.length ; argno++)
    {
        if (args[argno].startsWith ("-"))
        {
            if (args[argno].equals ("-h"))
                print_usage_and_die ();
            else if (args[argno].equals ("-g"))
                System.setProperty ("diduce.gui", "true");
            else if (args[argno].equals ("-v"))
                System.setProperty ("diduce.verbose", "true");
            else if (args[argno].equals ("-r"))
                System.setProperty ("diduce.report", "true");
            else if (args[argno].equals ("-t"))
                System.setProperty ("diduce.timestamp", "true");
            else if (args[argno].equals ("-i"))
                System.setProperty ("diduce.read", args[++argno]);
            else if (args[argno].equals ("-o"))
                System.setProperty ("diduce.write", args[++argno]);
            else if (args[argno].equals ("-p"))
                System.setProperty ("diduce.run.props", args[++argno]);
            else if (args[argno].equals ("-nc"))
                System.setProperty ("diduce.new.code.confidence.change", args[++argno]);
            else if (args[argno].equals ("-sp"))
                System.setProperty ("diduce.sp", args[++argno]);
	    else if (args[argno].equals("-djava"))
		launch_prog_name = "djava";
            else
            {
                System.out.println ("Error: unrecognized option to diduce.run: " + args[argno]);
                print_usage_and_die();
                System.exit (2);
            }
        }
        else
            break;
    }

    if (args.length < argno+1)
        print_usage_and_die();

    System.out.println ("-----------------------------------------------------------------");
    System.out.print ("DIDUCE Runtime System, " + _version_str);
//    System.out.println (", run starting at " + new Date());
    System.out.println ();

    String prog_name = args[argno];
    argno++;
    // just shift the original args by 1
    String[] new_args = new String[args.length-argno];
    for (int i = argno ; i < args.length ; i++)
         new_args[i-argno] = args[i];

    System.out.print ("running " + prog_name + ".main with arguments: ");
    for (int i = 0 ; i < new_args.length ; i++)
        System.out.print (new_args[i] + " ");
    System.out.println ();
    System.out.println ("-----------------------------------------------------------------");

    try {
        Class c = Class.forName (prog_name);
        java.lang.reflect.Method m = c.getMethod("main", new Class[] { args.getClass() });
        m.setAccessible (true);
        int mods = m.getModifiers();
        if (m.getReturnType() != void.class || !Modifier.isStatic(mods) ||
            !Modifier.isPublic(mods)) 
        {
            throw new NoSuchMethodException("main");
        }

        if (System.getProperty ("diduce.gui") != null)
        {
            gui_thread_group = new ThreadGroup ("GUI-threadgroup");
            gui_thread = new show(gui_thread_group);
            gui_thread.start();
        }

        m.invoke(null, new Object[] { new_args });

    } catch (Exception e)
    {
        System.out.println (prog_name + ".main ended with exception: " + e);
        e.printStackTrace();
    }

    // If this is the only thread running, then dump stats
    // otherwise silently exit, the other thread which is running
    // is responsible for dumping stats. this requires that the
    // other threads be instrumented, and exit by calling System.exit.
    // Most GUI apps would exit by instrumenting System.exit.
          
    // get the root thread group in the system.
    ThreadGroup rootgrp = Thread.currentThread().getThreadGroup();
    while (rootgrp.getParent() != null)
        rootgrp = rootgrp.getParent ();
  
    boolean some_target_thread_running = true;
    int gui_threads_running = 0;

//	while (some_target_thread_running)
	{
		gui_threads_running = 0;
    // l will be an array containing all the threads
    // activeCount is not guaranteed to return the actual no.
    // of running threads. so we just sandbag l's size a little.
    Thread l[] = new Thread[Thread.activeCount() + 100];

    // sometimes the threads need a little time to die down
    try  {
        Thread.sleep (2000);
    } catch (Exception e) { System.out.println (e); } 

    // call enumerate with recursive = true, so it will get us 
    // all the threads in the system.
    int count = rootgrp.enumerate (l, true);

    // l now has a list of all threads

	some_target_thread_running = true;
    for (int p = 0 ; p < count ; p++)
    {
        if (l[p] == Thread.currentThread())
            continue;
        else if (!l[p].isAlive())
            continue;
        else if (l[p].isDaemon())
            continue;
        else 
        {
            if (l[p].getThreadGroup() == gui_thread_group)
                gui_threads_running++;
            else 
            {
                some_target_thread_running = true;
                System.out.println ("A target program thread is running: " + l[p]);
                break;
            }
        }
    }
	}

    if (gui_threads_running > 0)
        System.out.println ("Info: Target program (" + prog_name + ".main) completed, " + gui_threads_running + " GUI thread(s) are still running");
//    if (!some_target_thread_running)
//        Runtime.printStats();

	if ((System.getProperty("java.system.class.loader").equals
		("diduce.DiduceClassLoader")) &&
	        System.getProperty("java.version").startsWith("1.4")) {
	    try {
	    	watch.finish();
	    } catch (IOException ioe) {
	    	util.fatal(ioe);
	    	System.exit(1);
	    }
	}
	System.out.println ("diduce.run exiting");
	System.out.flush();
}

}
