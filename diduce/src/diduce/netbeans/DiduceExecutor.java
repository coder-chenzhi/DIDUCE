/**
 *
 * @author  hangal
 */

package diduce.netbeans;

import org.openide.execution.*;
import org.openide.*;
import org.openide.filesystems.*;
import java.io.*;
import java.util.*;
import java.beans.*;

public class DiduceExecutor extends Executor {

public static String PROP_NAME = "FOO EXEC";
public static String X_FO = "X_FO";

static ProcessExecutor _my_prexec;

/** Creates a new instance of Main_1 */
public DiduceExecutor () 
{
	super();
	System.out.println ("new diduceexecutor");
	_my_prexec = new ProcessExecutor();
}

public ExecutorTask execute (ExecInfo ei) throws IOException
{
	if (_my_prexec == null)
		_my_prexec = new ProcessExecutor();

	System.out.println ("Execinfo: " + ei);
    Repository r = Repository.getDefault();

	String orig_args[] = ei.getArguments();
	String new_args[] = new String[orig_args.length+4];
	new_args[0] = "-g";
	new_args[1] = "-r";
	new_args[2] = "-v";
	new_args[3] = ei.getClassName();
	for (int i = 0 ; i < orig_args.length ; i++)
		new_args[4+i] = orig_args[i];

	ExecInfo new_ei = new ExecInfo ("diduce.run", new_args);

	try {
	    JarFileSystem jfs = new JarFileSystem();
	    jfs.setJarFile (new File("diduce-classes.jar"));
	    r.addFileSystem (jfs);

		jfs = new JarFileSystem();
	    jfs.setJarFile (new File("/home/hangal/netbeans/modules/diduce.jar"));
	    r.addFileSystem (jfs);

	} catch (PropertyVetoException pve)
	{
        System.out.println ("Uncaught exception while adding diduce filesystems!");
	}


	/*
    System.out.println ("about to execute: ");
	System.out.println (new_ei.getClassName());
	String x[] = new_ei.getArguments();
	for (int i = 0 ; i < x.length ; i++)
	    System.out.println (x[i]);
	*/

	return _my_prexec.execute (new_ei);
}
    
/**
 * @param args the command line arguments
 */
public static void install_executor () 
{
	System.out.println ("in install executor");

	ServiceType.Registry registry = TopManager.getDefault().getServices();
	Enumeration en = registry.services();
	while (en.hasMoreElements())
		System.out.println ("next : " + en.nextElement());

	System.out.println ("afterinstall executor");
	List l = registry.getServiceTypes();
	for (Iterator it = l.iterator(); it.hasNext(); )
		System.out.println ("next service type: " + it.next());
	
	l.add (new DiduceExecutor());
	registry.setServiceTypes(l);
	
	System.out.println ("in install executor");

	l = registry.getServiceTypes();
	for (Iterator it = l.iterator(); it.hasNext(); )
		System.out.println ("next service type: " + it.next());
}
}
