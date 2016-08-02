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
import java.util.jar.*;
import java.util.zip.*;
import de.fub.bytecode.classfile.*;
import de.fub.bytecode.generic.*;
import de.fub.bytecode.*;

//public class watch implements Constants {
public class watch implements Constants {

static final String _version_str = "1.0Beta";

// stats

static int getf_instrumented_count, total_getf_instrumented_count;
static int putf_instrumented_count, total_putf_instrumented_count;
static int gets_instrumented_count, total_gets_instrumented_count;
static int puts_instrumented_count, total_puts_instrumented_count;
static int array_load_instrumented_count, total_array_load_instrumented_count;
static int array_store_instrumented_count, total_array_store_instrumented_count;
static int invoke_instrumented_count, total_invoke_instrumented_count;
static int classes_processed = 0, jars_processed = 0;

// these are the same for all classes
static Instruction dup_insn = new DUP();
static Instruction dup2_insn = new DUP2();
static Instruction dup2_x1_insn = new DUP2_X1();
static Instruction dup_x1_insn = new DUP_X1();
static Instruction dup_x2_insn = new DUP_X2();
static Instruction pop_insn = new POP();
static Instruction pop2_insn = new POP2();
static Instruction i2l_insn = new I2L();
static Instruction swap_insn = new SWAP();
static Instruction isub_insn = new ISUB();
static Instruction aconst_null_insn = new ACONST_NULL();

static Instruction invoke_dump_stats_insn;

static int _current_method_orig_locals, _current_method_extra_locals;

private static String _delimiter_line_1 =
        "*************************************************************";
private static String _delimiter_line_2 =
        "-------------------------------------------------------------";

private static String log_file_name = "diduce.watch.log";
private static String output_jar_name = "diduce-cache.jar";

// Create a tmp output jar and rename it to output_jar_name
// when done.
private static String tmp_output_jar_name = "tmp-diduce-cache.jar";
private static String diduce_tmpdir_name = "instrumented";
private static PrintStream Out = System.out;
public static PrintStream Log;
private static PrintStream Err = System.err;
private static ClassCache _cache;

private static int ensureScratchLocalVars (int n_extra_locals_needed)
{
    if (n_extra_locals_needed > _current_method_extra_locals)
        _current_method_extra_locals = n_extra_locals_needed;

    return _current_method_orig_locals;
}

private static void Tee (String s) {
    Out.println (s);
    Log.println (s);
}

private static void print_usage_and_die ()
{
    Out.print (
	    "Usage: java diduce.watch <options> <list of .class or .jar files>\n" +
	    "Options can be:\n" +
	    "-o <jar file> : Sent instrumented class files to <jar file>.\n" +
	    "                This file can be used as an input class cache\n" +
	    "                to perform incremental instrumentation\n" +
	    "-c <jar file> : Use <jar file> as a repository of instrumented classes, \n" +
	    "                and re-instrument only files which have changed\n" +
	    "-p <file> : read XML instrumentation constraints from <file> (see manual)\n" +
	    "-l <file> : send detailed log messages to <file>\n" +
	    "-h : this message\n");
    				System.exit (2);
}

private static String[] parse_args (String args[])
{
    int argno = 0;
    for (argno = 0; argno < args.length ; argno++)
    {
        if (args[argno].startsWith ("-"))
        {
            if (args[argno].equals ("-h"))
                print_usage_and_die ();
            else if (args[argno].equals ("-c"))
                System.setProperty ("diduce.cache", args[++argno]);
            else if (args[argno].equals ("-o"))
                System.setProperty ("diduce.output", args[++argno]);
            else if (args[argno].equals ("-p"))
                System.setProperty ("diduce.control.file", args[++argno]);
            else if (args[argno].equals ("-l"))
                System.setProperty ("diduce.watch.log", args[++argno]);
            else
            {
                Out.println ("Error: unrecognized option to diduce.watch: " + args[argno]);
                print_usage_and_die();
                System.exit (2);
            }
        }
        else
            break;
    }

    // shift the remaining args and pass it back.
    String[] new_args = new String[args.length-argno];
    for (int i = argno ; i < args.length ; i++)
         new_args[i-argno] = args[i];
    return new_args;
}

public static void main(String[] args) throws IOException
{
    // set the options and get the remaining args back
    args = parse_args (args);
    init();

    for (int argno = 0 ; argno < args.length ; argno++)
    {
        if (args[argno].endsWith (".jar"))
            instrument_jar_file (args[argno]);
        else
        {
            String class_name = args[argno];
            if (class_name.endsWith (".class")) {
		class_name = class_name.substring(0,
				class_name.indexOf(".class"));
	    }
            class_name = class_name.replace('.', File.separatorChar);
            class_name = class_name + ".class";
	    FileInputStream instream = new FileInputStream(class_name);
	    FileInputStream instreamCopy = new FileInputStream(class_name);
            instrument_class (class_name, instream, instreamCopy);
        }
    }
    Log.println (_delimiter_line_2);

    Tee ("\nInstrumentation complete. Statistics:\n");

    Tee ("instance variable reads  : " + total_getf_instrumented_count);
    Tee ("instance variable writes : " + total_putf_instrumented_count);
    Tee ("static variable reads    : " + total_gets_instrumented_count);
    Tee ("static variable writes   : " + total_puts_instrumented_count);
    Tee ("array reads              : " + total_array_load_instrumented_count);
    Tee ("array writes             : " + total_array_store_instrumented_count);
    Tee ("Method invocations       : " + total_invoke_instrumented_count);
    Tee ("Processed " + jars_processed + " jar(s) and " + classes_processed + " class(es)\n");
    Log.close();
    _cache.close ();

    // Copy from tmp jar file to output jar
    File outFile = new File(tmp_output_jar_name);
    if (outFile.renameTo( new File(output_jar_name))) {
        Out.println ("All instrumented classes are in file " + output_jar_name);
    } else {
        Out.println ("All instrumented classes are in file " + tmp_output_jar_name);
    }
    Out.println ("Please add this file at the beginning of your classpath\nto run the DIDUCE'd version of your program.");
}


public static void init() throws IOException {

    String s1 = System.getProperty ("diduce.watch.log");
    if (s1 != null)
        log_file_name = s1;
    try {
        Log = new PrintStream (new FileOutputStream (log_file_name));
    } catch (FileNotFoundException e) {
        Tee ("Unable to open file \"" + log_file_name + "\" for logging");
        System.exit (2);
    }
    Tee ("Detailed log messages are being sent to file \"" + log_file_name +
	 "\"");

    Tee ("DIDUCE Instrumentation System, " + _version_str + "\n");

    // if update mode, then
    String diduce_cache = null;
    s1 = System.getProperty ("diduce.cache");
    if (s1 != null)
        diduce_cache = s1;
    s1 = System.getProperty ("diduce.output");
    if (s1 != null)
        output_jar_name = s1;

    // diduce_cache can be null, but not output_jar_name
    // so must call equals on output_jar_name

    /*
    if (output_jar_name.equals (diduce_cache))
    {
	Tee ("Warning: Same filename for input and output instrumented class cache");
	output_jar_name = "1." + output_jar_name;
	Tee ("Instrumented classes are being redirected to go to file: \"" + output_jar_name + "\"\n");
    }
    else
        Tee ("Instrumented classes will be written to file \"" + output_jar_name + "\"\n");

    */
    _cache = new ClassCache (diduce_cache, tmp_output_jar_name);

    s1 = System.getProperty ("diduce.dir");
    if (s1 != null)
        diduce_tmpdir_name = s1;
}

static void finish() throws IOException {
    Log.println (_delimiter_line_2);
    Log.println ("\nInstrumentation complete. Statistics:\n");
    Log.println ("instance variable reads  : " + total_getf_instrumented_count);
    Log.println ("instance variable writes : " + total_putf_instrumented_count);
    Log.println ("static variable reads    : " + total_gets_instrumented_count);
    Log.println ("static variable writes   : " + total_puts_instrumented_count);
    Log.println ("array reads              : " + total_array_load_instrumented_count);
    Log.println ("array writes             : " + total_array_store_instrumented_count);
    Log.println ("Method invocations       : " + total_invoke_instrumented_count);
    Log.println ("Processed " + jars_processed + " jar(s) and " + classes_processed + " class(es)\n");
    Log.close();

    _cache.close ();

    Out.println();
    Out.println();
    Out.println ("**************************************************");
    // Copy from tmp jar file to output jar
    File outFile = new File(tmp_output_jar_name);
    if (outFile.renameTo( new File(output_jar_name))) {
        Out.println ("All instrumented classes are in file " + output_jar_name);
    } else {
        Out.println ("All instrumented classes are in file " + tmp_output_jar_name);
    }
}

// instruments the jar file named 'name' and writes the instrumented
// file to jout
private static void instrument_jar_file (String name) throws IOException
{
    Log.println (_delimiter_line_2);
    Tee ("Instrumenting all classes in jar file \"" + name + '"');
    Log.println (_delimiter_line_2);

    JarInputStream jin = new JarInputStream (new FileInputStream(name));
    JarFile jf = new JarFile (name);
    Enumeration e = jf.entries();

    while (e.hasMoreElements())
    {
        JarEntry je = (JarEntry) e.nextElement();
        if (je == null)
            break;

        // if it's a directory, we don't copy to the output jar file
        // because it causes problems if there are multiple jar files we
        // are trying to instrument - the entry for the directory could
        // already exist in the output jar file, and if so jout.putNextEntry
        // craps out
        if (je.isDirectory())
            continue;

        // we can't use the same jar entry in the output file because the
        // size parameters etc will be different. therefore create a new
        // one which only shares the name
        JarEntry new_je = new JarEntry (je.getName());

        Log.println ("Jar entry " + je.getName() +
                    ": "  + ((je.getMethod() == java.util.zip.ZipEntry.DEFLATED) ?
                             "compressed, " : ", uncompressed, ") +
                    " (size = " + je.getSize() +
                    ", compressed size = " + je.getCompressedSize() + ")");

        if (je.getName().endsWith (".class"))
        {
            instrument_class (je.getName(), jf.getInputStream(je), jf.getInputStream(je));
        }
    }

    jin.close();
    jars_processed++;
}

// instruments the class whose name is passed
// filename MUST BE in format a/b/c.class
public static byte[] instrument_class (String name, InputStream is1, InputStream is2)
		throws IOException
{
    Tee ("Instrumenting class \"" + name + '"');
    ClassCacheEntry cce = new ClassCacheEntry (name, is1);
    InputStream cachedStream;
    if ((cachedStream = _cache.lookup_and_copy_if_hit (cce)) != null)
    {
        System.out.println ("Cache hit!");
	return DiduceClassLoader.getBytes(cachedStream);
    }
    name = name.substring (0, name.length()-".class".length());
    byte[] instrumentedBytes = instrument_class(name, is2);
    String instrumented_class_filename = null;
    try
    {
	//check if name and clazz.getClassName() are same
        // String class_name_with_slash = clazz.getClassName().replace ('.', File.separatorChar);
        String class_name_with_slash = name.replace ('.', File.separatorChar);
	/**
	 * to write an instrumented file, the tmp directory should contain
	 * class's package path directory structure beneath it.
	 */ 
	String package_pathname = "";
	int package_path_index;	
	if ((package_path_index = class_name_with_slash.lastIndexOf(
				File.separatorChar)) != -1) {
	     package_pathname = class_name_with_slash.substring(0,
					package_path_index);
	}
	String instrumented_dirname = diduce_tmpdir_name + File.separatorChar +
					package_pathname;
        instrumented_class_filename = (diduce_tmpdir_name + File.separatorChar +
					class_name_with_slash) + ".class";

       // if (!name.endsWith (class_name_with_slash + ".class"))
       // {
       //     util.warn ("Warning: File name \"" + name + "\" does not match class name \"" +
	//		clazz.getClassName() + "\"\n");
        //}

        // no .class needed at the end of instrumented_class_filename for clazz.dump
        //clazz.dump (instrumented_class_filename);
	File tmpDir = new File(instrumented_dirname);
	tmpDir.mkdirs();
	File instFile = new File(instrumented_class_filename);
	FileOutputStream out = new FileOutputStream(instFile);
	out.write(instrumentedBytes);
	out.close();
        _cache.addFileToCache (name + ".class", instrumented_class_filename, cce);

        // delete the temp. instrumented file we created
        if (!((new File(instrumented_class_filename)).delete()))
            util.warn ("Warning: unable to delete file: " + instrumented_class_filename);
    } catch (Exception e) { e.printStackTrace(); System.exit (32); }
    return instrumentedBytes;
}

static byte[] instrument_class (String name, InputStream is)
		throws IOException
{
    ClassParser cp = new ClassParser (is, name);
    JavaClass clazz = null;

    try {
       clazz  = cp.parse();
    }  catch (ClassFormatError cfe) {
        Out.println ("Exception while parsing file " + name + "\n" + cfe);
        System.exit(2);
    } catch (IOException ioe) {
        Out.println ("Error while reading file " + name + "\n" + ioe);
        System.exit(2);
    }

    if (clazz == null)
    {
        Tee ("\nUnable to find class " + cp
                    + " ignoring this class");
        System.exit (2);
    }

    Method[]        methods = clazz.getMethods();
    Log.println (methods.length + " methods");
    Log.println (_delimiter_line_1);

    ConstantPoolGen cpgen = new ConstantPoolGen (clazz.getConstantPool());

    // better not already have a ref to diduce.Runtime
    if (cpgen.lookupClass ("diduce.Runtime") != -1)
    {
        Out.println ("Error: Class \"" + name + "\" is already instrumented!?!");
        Out.println ("Please try to instrument only previously uninstrumented classes");
        System.exit (22);
    }

    for (int i=0; i < methods.length; i++)
    {
        LineNumberTable line_num_table = null;

        Log.println (_delimiter_line_2);

        if (methods[i].getCode() != null)
        {
            Attribute[] attribs = methods[i].getCode().getAttributes();
            if (attribs != null)
                for (int j = 0 ; j < attribs.length ; j ++)
                    if (attribs[j] instanceof LineNumberTable)
                       line_num_table = (LineNumberTable) attribs[j];
        }

        MethodGen mg = new MethodGen(methods[i], clazz.getClassName(), cpgen);
        Log.println ("Instrumenting method "
                    + clazz.getClassName() + "."
                    + mg.getName() + mg.getSignature());

        // things break if we instrument system class constructors
        // at some point, ok to break the sun.*
        // not a big deal since it's constructors only
        boolean init_method = (mg.getClassName().startsWith ("java.") ||
                               mg.getClassName().startsWith ("sun."))
                             &&
                              (mg.getName().equals ("<init>") ||
                               mg.getName().equals ("<clinit>"));
        if (init_method)
        {
            Log.println ("\nWarning: not instrumenting system class constructor "
                        + "(because this is known to cause problems): "
                        + mg.getClassName() + "." + mg.getName()
                        + mg.getSignature());
        }
        else if (!methods[i].isNative() && !methods[i].isAbstract())
            methods[i] = instrumentMethod (mg, line_num_table);
        else
            Log.println ("(abstract or native)");
    }
    clazz.setConstantPool(cpgen.getFinalConstantPool());
    return clazz.getBytes();
}

private static Method instrumentMethod (MethodGen mg, LineNumberTable lnt)
{
    if (lnt == null)
        Log.println ("no line number information for method!");

    InstructionList il = mg.getInstructionList();

    getf_instrumented_count = 0;
    putf_instrumented_count = 0;
    gets_instrumented_count = 0;
    puts_instrumented_count = 0;
    array_load_instrumented_count = 0;
    array_store_instrumented_count = 0;
    invoke_instrumented_count = 0;

    _current_method_orig_locals = mg.getMaxLocals();
    _current_method_extra_locals = 0;

    ConstantPoolGen cpgen = mg.getConstantPool ();

    for (InstructionHandle ih = il.getStart();
          ih != null;
         ih = ih.getNext())
    {
        Instruction insn = ih.getInstruction();
        if ((insn instanceof FieldInstruction) ||
            (insn instanceof ArrayInstruction) ||
            (insn instanceof InvokeInstruction))
        {
            int lineno = 0;
            /* call to getsource line sometimes throws a
               ArrayIndexOutOfBoundsException, because the line number
               info is sometimes not inserted correctly by some compilers.
               so we just let it be 0 in those cases. */
            try {
                if (lnt != null)
                    lineno = lnt.getSourceLine(ih.getPosition());
            } catch (ArrayIndexOutOfBoundsException a) {
              Tee ("Warning: class file has bogus line number information "
                  + "for method " + mg.getName() + mg.getSignature() + "\n"
                  + "Some line numbers in this method may be printed as 0");
            }

            if (insn instanceof FieldInstruction)
                ih = instrument_field_instruction (mg, il, ih, lineno);
            else if (insn instanceof ArrayInstruction)
                ih = instrument_array_instruction (mg, il, ih, lineno);
            else if (ih.getInstruction() instanceof InvokeInstruction)
                ih = instrument_invoke_insn (mg, il, ih, lineno);

//            else if (ih.getInstruction() instanceof ReturnInstruction &&
//                     (! (ih.getInstruction() instanceof RETURN)))
//            {
//                ih = instrument_return_insn (mg, il, ih, lineno);
//            }
        }
    }
    Log.println ();

    mg.setMaxStack (mg.getMaxStack()+10);
    mg.setMaxLocals (_current_method_orig_locals + _current_method_extra_locals);

    //    mg.setConstantPool (cpgen);
    mg.setInstructionList (il);

    total_getf_instrumented_count += getf_instrumented_count;
    total_putf_instrumented_count += putf_instrumented_count;
    total_gets_instrumented_count += gets_instrumented_count;
    total_puts_instrumented_count += puts_instrumented_count;
    total_array_load_instrumented_count += array_load_instrumented_count;
    total_array_store_instrumented_count += array_store_instrumented_count;
    total_invoke_instrumented_count += invoke_instrumented_count;

    Log.println ("instance variable reads instrumented:"
                 + getf_instrumented_count);
    Log.println ("instance variable writes instrumented:"
                 + putf_instrumented_count);
    Log.println ("static variable reads instrumented:"
                 + gets_instrumented_count);
    Log.println ("static variable writes instrumented:"
                 + puts_instrumented_count);
    Log.println ("array reads instrumented:"
                 + array_load_instrumented_count);
    Log.println ("array writes instrumented:"
                 + array_store_instrumented_count);
    Log.println ("Method invocations instrumented:"
                 + invoke_instrumented_count);


/*
 il.setPositions();

    for (InstructionHandle ih = il.getStart();
          ih != null;
         ih = ih.getNext())
    {
        Instruction insn = ih.getInstruction();
        Out.println (insn + " : " + ih.getPosition ());
    }
*/

    return mg.getMethod();
}

private static String map_insn_name_to_description (String iname)
{
    if (iname.equals ("GETSTATIC"))
        return "svar-read";
    else if (iname.equals ("PUTSTATIC"))
        return "svar-write";
    else if (iname.equals ("GETFIELD"))
        return "ivar-read";
    else if (iname.equals ("PUTFIELD"))
        return "ivar-write";
    else if (iname.endsWith ("ALOAD"))
        return "array-read";
    else if (iname.endsWith ("ASTORE"))
        return "array-write";
    else
        return iname;
}

private static String mapTypeToString (Type t)
{
    if (t == Type.BOOLEAN) return "boolean";
    if (t == Type.BYTE) return "byte";
    if (t == Type.CHAR) return "char";
    if (t == Type.SHORT) return "short";
    if (t == Type.INT) return "int";
    if (t == Type.LONG) return "long";
    if (t == Type.FLOAT) return "float";
    if (t == Type.DOUBLE) return "double";

    // some day shd make this return the actual object or array type string
    if (t instanceof ArrayType) return "array";
    if (t instanceof ObjectType) return "object";

    return "Unknown type " + t + "!!!";
}

// returns value of an attribute of the form x=b from the
private static String mapTypeCharToString (char x)
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
      case '[': return ("object"); // this needs to be object
//      case '[': return ("array");
    }
    return "Unknown type " + x + "!!!";
}

// returns a new invoke instruction for calling the right backend
// function for a value of this type. the significance of is_write is
// that there are 2 values for that type in the signature of the
// Runtime function we are going to invoke
private static Instruction new_invoke_helper_insn (ConstantPoolGen cpgen,
                                                   Type type, boolean is_write,
                                                   boolean is_object)
{
    String helper = "diduce.Runtime";

    String invoke_helper_sig = "(";
    String invoke_helper_name = "register";

    invoke_helper_name += is_object ? "_object_" : "_static_";
    invoke_helper_sig += is_object ? "Ljava/lang/Object;" : "";

    String sig_str = null;

    // choose the appropriate instrumentation backend function we will invoke
    if (type == Type.DOUBLE)
    {
        sig_str = "D";
        invoke_helper_name += "double";
    }
    else if (type == Type.FLOAT)
    {
        sig_str = "F";
        invoke_helper_name += "float";
    }
    else if (type == Type.LONG)
    {
        sig_str = "J";
        invoke_helper_name += "long";
    }
    else if ((type == Type.INT) || (type == Type.BOOLEAN) ||
             (type == Type.CHAR) || (type == Type.SHORT) ||
             (type == Type.BYTE))
    {
        sig_str = "I";
        invoke_helper_name += "int";
    }
    else if ((type instanceof ObjectType) || (type instanceof ArrayType))
    {
         sig_str = "Ljava/lang/Object;";
        invoke_helper_name += "ref";
    }
    else { util.fatal ("FATAL ERROR: trying to call Runtime function with unknown type: " + type); }

    invoke_helper_sig += sig_str;
    if (is_write)
        invoke_helper_sig += sig_str;

    // add sig. for args for spp descriptor and spp num
    invoke_helper_sig = invoke_helper_sig + "Ljava/lang/String;I)V";

    invoke_helper_name += is_write ? "_store" : "_load";

    int instrument_cp = cpgen.addMethodref ("diduce.Runtime", invoke_helper_name,
                                            invoke_helper_sig);
    return (new INVOKESTATIC (instrument_cp));
}

private static InstructionHandle instrument_invoke_params (MethodGen mg,
                    InstructionList il, InstructionHandle ih, int lineno)
{
    InstructionHandle orig_first_insn = ih;

    InstructionHandle new_first_insn = orig_first_insn;

    ConstantPoolGen cpgen = mg.getConstantPool ();
    InvokeInstruction inv = ((InvokeInstruction) ih.getInstruction());
    boolean is_static = (inv instanceof INVOKESTATIC);
    boolean is_init_call = inv.getMethodName(cpgen).equals ("<init>");
    Type[] t = inv.getArgumentTypes (cpgen);

    /* for non-static calls, add a dummy parameter for 'this'.
       we modify the t[] array to add a reference Type
    (arbitrarily java.lang.Object) at the beginning.

    we need to avoid instrumenting 'this' for calls
    (invokespecial's) to <init>
    otherwise program fails bytecode verification.
    I'm not sure why this is.
    possibly because the object is not initialized ??
    the JLS has a requirement that a constructor's
    first executable stmt shd be a call to another
    constructor, either of itself or its superclass.
    our instrumentation would violate this rule,
    and that's probably what the verifier is checking.

    other parameters to constructors seem to work fine even
    if they are initialized. I think other types of
    invokespecials are ok, but not really tested.


     */
    if (!is_static && !is_init_call)
    {
        Type[] t_new = new Type[t.length+1];
        // it's java dot lang dot Object not java/lang/Object
        t_new[0] = new ObjectType ("java.lang.Object");
        for (int i = 0 ; i < t.length ; i++)
            t_new[i+1] = t[i];

        t = t_new;
    }

    /* t[0] : java/lang/Object */

    String target_method_str = inv.getClassName(cpgen) + "." + inv.getMethodName(cpgen)
                             + inv.getSignature(cpgen);

    // compute how many extra locals we will need
    int n_extra_locals_needed = 0;
    for (int i = t.length-1 ; i >= 0 ; i--)
        n_extra_locals_needed += t[i].getSize();
    int scratch_var_idx = ensureScratchLocalVars (n_extra_locals_needed);

    // insert_point is the handle at which the next set of param instrumentation
    // insns will be instrumented.
    InstructionHandle insert_point = ih;

    // go backwards in the param order - instrument and pop the params to save them,
    // then push them back. for the param currently at top of stack, we:
    // 1. dup (or dup2) it
    // 2. call appropriate version of helper
    // 3. store the param
    // 4. load the param
    // since this has to operate as a stack, instrumentation for successive params
    // is inserted between steps 3 and 4.

    for (int i = t.length-1 ; i >= 0 ; i--)
    {
        // instrument each parameter
        // scratch_var_idx contains local var index we will use for this param
        // insert_point contains the instr handle where the instrumentation
        // for this param will go.

        Instruction store_insn = null, load_insn = null;

        if (t[i].equals (Type.BOOLEAN) || t[i].equals (Type.BYTE) ||
            t[i].equals (Type.CHAR) || t[i].equals (Type.INT) || t[i].equals (Type.SHORT))
        {
            store_insn = new ISTORE (scratch_var_idx);
            load_insn = new ILOAD (scratch_var_idx);
        }
        else if (t[i].equals (Type.FLOAT))
        {
            store_insn = new FSTORE (scratch_var_idx);
            load_insn = new FLOAD (scratch_var_idx);
        }
        else if (t[i].equals (Type.DOUBLE))
        {
            store_insn = new DSTORE (scratch_var_idx);
            load_insn = new DLOAD (scratch_var_idx);
        }
        else if (t[i].equals (Type.LONG))
        {
            store_insn = new LSTORE (scratch_var_idx);
            load_insn = new LLOAD (scratch_var_idx);
        }
        else if (t[i] instanceof ReferenceType)
        {
            store_insn = new ASTORE (scratch_var_idx);
            load_insn = new ALOAD (scratch_var_idx);
        }
        int n_stack_slots = t[i].getSize();
        scratch_var_idx += n_stack_slots;

        Instruction this_dup_insn = null;
        switch (n_stack_slots)
        {
          case 2: this_dup_insn = dup2_insn; break;
          case 1: this_dup_insn = dup_insn; break;
          case 0: util.fatal ("Void in invoke parameter!");
                  break;
        }

        // set up an SPP object for this program point
        String where = mg.getClassName() + "." + mg.getName() + mg.getSignature();
        StaticProgramPoint spp = new StaticProgramPoint ("param", where, String.valueOf(lineno));
        spp.set_num (String.valueOf(i));
        spp.set_target (target_method_str);
        spp.set_type (mapTypeToString(t[i]));
        spp.set_write ("false");
        spp.set_static (is_static ? "true" : "false");

        // remember that even if we don't want to instrument this parameter
        // we must add a load and store for it! used to be a bug
        util.ASSERT (_cache != null);
        int pip_num = _cache.get_spp_num (spp);

        boolean instrument = (pip_num >= 0);
        if (instrument)
        {
            int spp_cp = cpgen.addString (spp.toString());
            Log.println ("Instrumentation point " + pip_num +  ": " + spp);

            Instruction ldc_spp_desc_insn1 = new LDC (cpgen.addString(spp.toString()));
            Instruction ldc_spp_desc_insn2 = new LDC (cpgen.addInteger(pip_num));
            Instruction invoke_helper_insn = new_invoke_helper_insn (cpgen, t[i], false, false);

            // now insert the new instrumentation instructions
            InstructionHandle first_ih = il.insert (insert_point, this_dup_insn);

            // set new_first_insn for the very first param instrumented
            if (i == t.length-1)
                new_first_insn = first_ih;

            il.insert (insert_point, ldc_spp_desc_insn1);
            il.insert (insert_point, ldc_spp_desc_insn2);
            il.insert (insert_point, invoke_helper_insn);
        }

        InstructionHandle store_insn_handle = il.insert (insert_point, store_insn);
        il.insert (insert_point, load_insn);

        // if we did not instrument the last param then we shd make new_first_insn
        // the store which saves it
        if ((i == (t.length-1)) && (!instrument))
            new_first_insn = store_insn_handle;

        // now move insert_point to the prev instruction so it is pointing
        // correctly to the insert point for the next param
        insert_point = insert_point.getPrev ();
    }

    // you thot i'd forget to retarget branches again, didn't you ?
    // so did i.
    il.redirectBranches (orig_first_insn, new_first_insn);

    // always return the orig. first invoke insn because we've inserted
    // our stuff before it
    return (orig_first_insn);
}

private static InstructionHandle instrument_return_value (MethodGen mg,
               InstructionList il, InstructionHandle ih, int lineno)
{
    ConstantPoolGen cpgen = mg.getConstantPool ();
    InvokeInstruction inv = ((InvokeInstruction) ih.getInstruction());
    boolean is_static = (inv instanceof INVOKESTATIC);

    String target_method_str = inv.getClassName(cpgen) + "." + inv.getMethodName(cpgen)
                             + inv.getSignature(cpgen);

    String full_sig = inv.getSignature (cpgen);

    Type return_type = inv.getReturnType (cpgen);

    // if a void method, we don't need to do anything
    if (return_type != Type.VOID)
    {
        // set up an SPP object for this program point
        String where = mg.getClassName() + "." + mg.getName() + mg.getSignature();
        StaticProgramPoint spp = new StaticProgramPoint ("retval", where, String.valueOf(lineno));

        // no need to set num
        spp.set_target (target_method_str);
        spp.set_type (mapTypeToString (return_type));
        spp.set_write ("false");
        spp.set_static (is_static ? "true" : "false");

        int pip_num = _cache.get_spp_num (spp);
        if (pip_num < 0)
            return ih;

        int spp_cp = cpgen.addString (spp.toString());
        Log.println ("Instrumentation point " + pip_num +  ": " + spp);

        Instruction ldc_spp_desc_insn1 = new LDC (spp_cp);
        Instruction ldc_spp_desc_insn2 = new LDC (cpgen.addInteger(pip_num));
        Instruction invoke_helper_insn = new_invoke_helper_insn (cpgen, return_type, false, false);

        Instruction dup;
        int size = return_type.getSize();
        util.ASSERT ((size == 1) || (size == 2));

        dup = (size == 2) ? dup2_insn : dup_insn;

        ih = il.append (ih, dup);
        ih = il.append (ih, ldc_spp_desc_insn1);
        ih = il.append (ih, ldc_spp_desc_insn2);
        ih = il.append (ih, invoke_helper_insn);
        // no need for redirect branches here, since we only appended
        // insns after the original invoke
    }
    return ih;
}

private static InstructionHandle instrument_invoke_insn (MethodGen mg,
                                InstructionList il, InstructionHandle ih, int lineno)
{
    // we add a call to diduce.Runtime.printStats() before a call to java.lang.System.exit

    invoke_instrumented_count++;
    InvokeInstruction inv = ((InvokeInstruction) ih.getInstruction());
    ConstantPoolGen cpgen = mg.getConstantPool ();
    String sig = inv.getClassName(cpgen) + "." + inv.getMethodName(cpgen)
                 + inv.getSignature(cpgen);

    if ((inv instanceof INVOKESTATIC) && sig.equals ("java.lang.System.exit(I)V"))
    {
        // set up an SPP object for this program point
        String where = mg.getClassName() + "." + mg.getName() + mg.getSignature();
        StaticProgramPoint spp = new StaticProgramPoint ("exit", where, String.valueOf(lineno));

        spp.set_target (sig);

        int pip_num = _cache.get_spp_num (spp);
        if (pip_num < 0)
            return ih;

        int spp_cp = cpgen.addString (spp.toString());
        Log.println ("Instrumentation point " + pip_num +  ": " + spp);

        Instruction ldc_spp_desc_insn1 = new LDC (spp_cp);
        Instruction ldc_spp_desc_insn2 = new LDC (cpgen.addInteger(pip_num));
        int instrument_cp = cpgen.addMethodref ("diduce.Runtime", "printStats", "()V");

        Instruction invoke_instrument_insn = new INVOKESTATIC (instrument_cp);
        InstructionHandle orig_first_insn = ih;
        InstructionHandle new_first_insn = il.insert (ih, invoke_instrument_insn);
        il.redirectBranches (orig_first_insn, new_first_insn);
    }
    else
    {
        ih = instrument_invoke_params (mg, il, ih, lineno);
        ih = instrument_return_value (mg, il, ih, lineno);
    }
    return ih;
}

private static InstructionHandle instrument_field_instruction (MethodGen mg,
                                InstructionList il, InstructionHandle ih, int lineno)
{
    ConstantPoolGen cpgen = mg.getConstantPool ();

    FieldInstruction field_insn = (FieldInstruction) ih.getInstruction ();
    int cp_index = field_insn.getIndex ();

    String field_classname = field_insn.getClassName(cpgen);
    String field_name = field_insn.getFieldName (cpgen);
    String field_sig = field_insn.getSignature (cpgen);
    Type field_type = field_insn.getType (cpgen);

    boolean is_insn_getf = field_insn instanceof GETFIELD;
    boolean is_insn_putf = field_insn instanceof PUTFIELD;
    boolean is_insn_gets = field_insn instanceof GETSTATIC;
    boolean is_insn_puts = field_insn instanceof PUTSTATIC;

    // set up an SPP object for this program point
    String where = mg.getClassName() + "." + mg.getName() + mg.getSignature();
    StaticProgramPoint spp = new StaticProgramPoint ("field", where, String.valueOf(lineno));

    // no need to set num, need to set all other fields
    spp.set_target (field_classname + "." + field_name);
    spp.set_type (mapTypeToString (field_type));
    spp.set_write ((is_insn_putf || is_insn_puts) ? "true" : "false");
    spp.set_static ((is_insn_gets || is_insn_puts) ? "true" : "false");

    int pip_num = _cache.get_spp_num (spp);
    if (pip_num < 0)
        return ih;

    int spp_cp = cpgen.addString (spp.toString());
    Log.println ("Instrumentation point " + pip_num +  ": " + spp);

    Instruction ldc_spp_desc_insn1 = new LDC (spp_cp);
    Instruction ldc_spp_desc_insn2 = new LDC (cpgen.addInteger(pip_num));
    Instruction invoke_instrument_insn = new_invoke_helper_insn (cpgen, field_type, (is_insn_putf || is_insn_puts), (is_insn_getf || is_insn_putf));

    // orig_first_insn is the first instruction in the sequence
    // we are looking at, before instrumentation
    // new_first_insn is the first instruction after the instrumentation
    // we need this to make sure that at the end we redirect branches
    // pointing to the orig_first_insn to new_first_insn instead

    // careful with changing the instrumentation sequence here - the
    // new_first_insn MUST point to the right insn
    // an option might be to introduce (and assign new_first_insn) a dummy nop
    // at the head of the sequence right here, so we don't have to assign
    // new_first_insn all over the place

    // for the getf/gets insns, we now perform the gets/getf twice.
    // shd be improved to getting it just once and then using it twice,
    // once by the instrumenation and once by the program ??
    // (maybe instr. functions shd just return the "new value", so we
    // can just push it on the stack.)
    InstructionHandle new_first_insn = null, orig_first_insn = ih;

    if (is_insn_getf)
    {
        new_first_insn = il.insert (ih, dup_insn);
        il.insert (ih, dup_insn);
        il.insert (ih, new GETFIELD (cp_index));
        // stack now looks like: ref ref <value>
        il.insert (ih, ldc_spp_desc_insn1);
        il.insert (ih, ldc_spp_desc_insn2);
        il.insert (ih, invoke_instrument_insn);
        getf_instrumented_count++;
    }
    else if (is_insn_putf)
    {
        if (field_type == Type.LONG)
        {
            int scratch = ensureScratchLocalVars (3);

            new_first_insn = il.insert (ih, new LSTORE (scratch+1));
            il.insert (ih, new ASTORE (scratch));

            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new LLOAD (scratch+1));

            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new LLOAD (scratch+1));
            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new GETFIELD (cp_index));
            // stack is now: ref v1 v2 ref v1 v2 v1-old v2-old
            // so it is ready for putfield after instrumentation fn. returns
        }
        else if (field_type == Type.DOUBLE)
        {
            int scratch = ensureScratchLocalVars (3);
            new_first_insn = il.insert (ih, new DSTORE (scratch+1));
            il.insert (ih, new ASTORE (scratch));

            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new DLOAD (scratch+1));

            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new DLOAD (scratch+1));

            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new GETFIELD (cp_index));
            // stack is now: ref v1 v2 ref v1 v2 v1-old v2-old
            // so it is ready for putfield after _invoke_instrument returns
        }
        else if ((field_type == Type.INT) || (field_type == Type.BOOLEAN) ||
                 (field_type == Type.CHAR) || (field_type == Type.SHORT) ||
                 (field_type == Type.BYTE) ||
                 (field_type == Type.FLOAT) || (field_type instanceof ReferenceType))
        {
            // ref, new_val
            new_first_insn = il.insert (ih, dup2_insn);
            // ref, new_val, ref, new_val
            il.insert (ih, swap_insn);
            // ref, new_val, new_val, ref
            il.insert (ih, dup_x1_insn);
            // ref, new_val, ref, new_val, ref
            il.insert (ih, new GETFIELD (cp_index));
            // ref, new_val, ref, new_val, old_val

        }
        else
            util.fatal ("Unknown type for field " + field_classname + "." + field_name + ": " + field_type);

        il.insert (ih, ldc_spp_desc_insn1);
        il.insert (ih, ldc_spp_desc_insn2);
        il.insert (ih, invoke_instrument_insn);
        putf_instrumented_count++;
    }
    else if (is_insn_gets)
    {
        // ref
        new_first_insn = il.insert (ih, new GETSTATIC (cp_index));
        // ref val
        il.insert (ih, ldc_spp_desc_insn1);
        il.insert (ih, ldc_spp_desc_insn2);
        il.insert (ih, invoke_instrument_insn);
        // ref
        gets_instrumented_count++;
    }
    else if (is_insn_puts)
    {
        int size = field_type.getSize ();
        util.ASSERT ((size == 1) || (size == 2));

        if (size == 2)
        {
            new_first_insn = il.insert (ih, dup2_insn);
            il.insert (ih, new GETSTATIC (cp_index));
        }
        else
        {
            new_first_insn = il.insert (ih, dup_insn);
            il.insert (ih, new GETSTATIC (cp_index));
        }

        il.insert (ih, ldc_spp_desc_insn1);
        il.insert (ih, ldc_spp_desc_insn2);
        il.insert (ih, invoke_instrument_insn);
        puts_instrumented_count++;
    }

    il.redirectBranches (orig_first_insn, new_first_insn);
    return ih;
}

private static InstructionHandle instrument_array_instruction (MethodGen mg,
                     InstructionList il, InstructionHandle ih, int lineno)
{
    ConstantPoolGen cpgen = mg.getConstantPool ();

    ArrayInstruction array_insn = (ArrayInstruction) ih.getInstruction();
    String cname = array_insn.getClass().getName();
    String iname = cname.substring (cname.lastIndexOf (".")+1);

    Type array_element_type = null;

    if (iname.startsWith ("B")) array_element_type = Type.BYTE;
    else if (iname.startsWith ("C")) array_element_type = Type.CHAR;
    else if (iname.startsWith ("S")) array_element_type = Type.SHORT;
    else if (iname.startsWith ("I")) array_element_type = Type.INT;
    else if (iname.startsWith ("L")) array_element_type = Type.LONG;
    else if (iname.startsWith ("D")) array_element_type = Type.DOUBLE;
    else if (iname.startsWith ("F")) array_element_type = Type.FLOAT;
    else if (iname.startsWith ("A")) array_element_type = new ObjectType ("dummy");
    else util.ASSERT (false);

// above is a hack becos foll. line doesn't work (BCEL throws exception)
// Type array_element_type = array_insn.getType (cpgen);

    String full_class_name = array_insn.getClass().getName();

    boolean is_load = (array_insn.produceStack(cpgen) > 0);

    // set up an SPP object for this program point
    String where = mg.getClassName() + "." + mg.getName() + mg.getSignature();
    StaticProgramPoint spp = new StaticProgramPoint ("array", where, String.valueOf(lineno));

    // no need to set num, target, need to set other fields
    // static must be set to false since we're accessing an array element
    // (it still needs to be explicitly set)
    spp.set_type (mapTypeToString (array_element_type));
    spp.set_write (is_load ? "false" : "true");
    spp.set_static ("false");

    int pip_num = _cache.get_spp_num (spp);
    if (pip_num < 0)
        return ih;

    int spp_cp = cpgen.addString (spp.toString());
    Log.println ("Instrumentation point " + pip_num +  ": " + spp);

    Instruction ldc_spp_desc_insn1 = new LDC (spp_cp);
    Instruction ldc_spp_desc_insn2 = new LDC (cpgen.addInteger(pip_num));
    Instruction invoke_instrument_insn = new_invoke_helper_insn (cpgen, array_element_type, !is_load, false);

    // orig_first_insn is the first instruction in the sequence
    // we are looking at, before instrumentation
    // new_first_insn is the first instruction after the instrumentation
    // we need this to make sure that at the end we redirect branches
    // pointing to the orig_first_insn to new_first_insn instead

    // careful with changing the instrumentation sequence here - the
    // new_first_insn MUST point to the right insn
    // an option might be to introduce (and assign new_first_insn) a dummy nop
    // at the head of the sequence right here, so we don't have to assign
    // new_first_insn all over the place

    InstructionHandle new_first_insn = null, orig_first_insn = ih;

    if (is_load)
    {
    /*  this was the rather complex seq. when we were passing the array ref also
        to the instrumentation function. we've now decided to get rid of it - we
        just pass the value, and the sequence reqd to implement that is also simple
        // ai
        new_first_insn = il.insert (ih, dup2_insn);
        // aiai
        il.insert (ih, swap_insn);
        // aiia
        il.insert (ih, dup_x1_insn);
        // aiaia
        il.insert (ih, swap_insn);
        // aiaai
        il.insert (ih, insn);
        // aia<val>
    */
        // ai
        new_first_insn = il.insert (ih, dup2_insn);
        // aiai
        il.insert (ih, array_insn);
        // ai<val>

        il.insert (ih, ldc_spp_desc_insn1);
        il.insert (ih, ldc_spp_desc_insn2);
        il.insert (ih, invoke_instrument_insn);
        array_load_instrumented_count++;
    }
    else
    {
        // array store insn
        int scratch = ensureScratchLocalVars (4);
        if (array_element_type == Type.LONG)
        {
            new_first_insn = il.insert (ih, new LSTORE (scratch+2));
            il.insert (ih, new ISTORE (scratch+1));
            il.insert (ih, new ASTORE (scratch));

            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new ILOAD (scratch+1));
            il.insert (ih, new LLOAD (scratch+2));

            il.insert (ih, new LLOAD (scratch+2));
            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new ILOAD (scratch+1));
        }
        else if (array_element_type == Type.DOUBLE)
        {
            new_first_insn = il.insert (ih, new DSTORE (scratch+2));
            il.insert (ih, new ISTORE (scratch+1));
            il.insert (ih, new ASTORE (scratch));

            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new ILOAD (scratch+1));
            il.insert (ih, new DLOAD (scratch+2));

            il.insert (ih, new DLOAD (scratch+2));
            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new ILOAD (scratch+1));
        }
        else if (array_element_type == Type.FLOAT)
        {
            new_first_insn = il.insert (ih, new FSTORE (scratch+2));
            il.insert (ih, new ISTORE (scratch+1));
            il.insert (ih, new ASTORE (scratch));

            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new ILOAD (scratch+1));
            il.insert (ih, new FLOAD (scratch+2));

            il.insert (ih, new FLOAD (scratch+2));
            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new ILOAD (scratch+1));
        }
        else if ((array_element_type == Type.INT) || (array_element_type == Type.CHAR) ||
                 (array_element_type == Type.SHORT) || (array_element_type == Type.BYTE))
        {
            new_first_insn = il.insert (ih, new ISTORE (scratch+2));
            il.insert (ih, new ISTORE (scratch+1));
            il.insert (ih, new ASTORE (scratch));

            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new ILOAD (scratch+1));
            il.insert (ih, new ILOAD (scratch+2));

            il.insert (ih, new ILOAD (scratch+2));
            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new ILOAD (scratch+1));
        }
        else if (array_element_type instanceof ReferenceType)
        {
            new_first_insn = il.insert (ih, new ASTORE (scratch+2));
            il.insert (ih, new ISTORE (scratch+1));
            il.insert (ih, new ASTORE (scratch));

            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new ILOAD (scratch+1));
            il.insert (ih, new ALOAD (scratch+2));

            il.insert (ih, new ALOAD (scratch+2));
            il.insert (ih, new ALOAD (scratch));
            il.insert (ih, new ILOAD (scratch+1));
        }
        else
        {
            util.fatal ("Unknown type in array store: " + array_element_type);
            System.exit (32);
        }

        // get rid of the "STORE" and add the corresponding "LOAD"
        String load_class_name =
            full_class_name.substring (0, full_class_name.length()-"STORE".length()) + "LOAD";

        // whatever this array store insn was, we insert the corresponding
        // array load
        try {
        il.insert (ih, (Instruction) Class.forName (load_class_name).newInstance());
        } catch (Exception e)
        {
           util.fatal ("Error instantiating new instance of class "
                       + load_class_name + ", exiting...");
           System.exit (22);
        }

        il.insert (ih, ldc_spp_desc_insn1);
        il.insert (ih, ldc_spp_desc_insn2);
        il.insert (ih, invoke_instrument_insn);
        array_store_instrumented_count++;
    }

    il.redirectBranches (orig_first_insn, new_first_insn);
    return ih;
}

/*
private static InstructionHandle instrument_return_instruction (MethodGen mg,
                     InstructionList il, InstructionHandle ih, int lineno)
{


    ConstantPoolGen cpgen = mg.getConstantPool ();

    Instruction insn = ih.getInstruction();
    Class insn_class = insn.getClass();
    String full_class_name = insn_class.getName();
s
    // drop the de.fub.... part
    String class_name = full_class_name.substring (full_class_name.lastIndexOf ('.')+1);

    char sig_char = class_name.charAt (0);

    boolean is_load = class_name.endsWith ("LOAD");

    Instruction getstatic_counter_array_insn;

    String spp_str =iname + "|"
                   + sig_char + "|"
                   + mg.getClassName() + "." + mg.getMethodName()
                   + "|" + lineno;

    int spp_cp = cpgen.addString (spp_str);
    _spp_map_file.println ("Instrumentation point " + _global_pip_num +  ": "
                           + StaticProgramPoint.//Descriptor (spp_str));

    Instruction ldc_spp_desc_insn1 = new LDC (spp_cp);
    Instruction ldc_spp_desc_insn2 = new LDC (cpgen.addInteger(_global_pip_num));
    _global_pip_num++;

    // generate the signature of the backend instrument function we will call
    String invoke_insn_sig = "(Ljava/lang/Object;";

    String val_sig = "";

    // choose the appropriate instrumentation backend function we will invoke
    switch (sig_char)
    {
      case 'J':
        val_sig = "J";
        break;

      case 'D':
        val_sig = "D";
        break;

      case 'F':
        val_sig = "F";
        break;

      case 'I':
      case 'C':
      case 'S':
      case 'B':
      case 'Z':
        val_sig = "I";
        break;

      case 'L':
      case '[':
        val_sig = "Ljava/lang/Object;";
        break;

      default:
        fatal ("Unknown sig for field (" + sig_char + ")");
        break;
    }

    invoke_insn_sig = invoke_insn_sig.concat (val_sig);

    // for a put, there are 2 values, so append val sig again
    if (!is_load)
        invoke_insn_sig = invoke_insn_sig.concat (val_sig);

    //    invoke_insn_sig = invoke_insn_sig.concat ("Ljava/lang/String;)V");
    invoke_insn_sig = invoke_insn_sig.concat ("Ljava/lang/String;I)V");

    int instrument_cp = cpgen.addMethodref ("diduce.Runtime", "Instrument",
                        invoke_insn_sig);
    Instruction invoke_instrument_insn = new_invoke_helper_insn (cpgen, sig_char, !is_load);

    // orig_first_insn is the first instruction in the sequence
    // we are looking at, before instrumentation
    // new_first_insn is the first instruction after the instrumentation
    // we need this to make sure that at the end we redirect branches
    // pointing to the orig_first_insn to new_first_insn instead

    // careful with changing the instrumentation sequence here - the
    // new_first_insn MUST point to the right insn
    // an option might be to introduce (and assign new_first_insn) a dummy nop
    // at the head of the sequence right here, so we don't have to assign
    // new_first_insn all over the place

    InstructionHandle new_first_insn = null, orig_first_insn = ih;

    if (is_load)
    {   // ai
        new_first_insn = il.insert (ih, dup2_insn);
        // aiai
        il.insert (ih, swap_insn);
        // aiia
        il.insert (ih, dup_x1_insn);
        // aiaia
        il.insert (ih, swap_insn);
        // aiaai
        il.insert (ih, insn);
        // aia<val>
        il.insert (ih, ldc_spp_desc_insn1);
        il.insert (ih, ldc_spp_desc_insn2);
        il.insert (ih, invoke_instrument_insn);
        array_load_instrumented_count++;
    }

    il.redirectBranches (orig_first_insn, new_first_insn);
    return ih;
}
*/

}
