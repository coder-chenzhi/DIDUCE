
package diduce;

import java.io.*;
import java.util.jar.*;
import java.util.*;
import java.util.zip.*;

public class ClassCache implements Serializable {


private static final long serialVersionUID = 3593965593530074631L;

private static final String classcache_entry_name = "diduce.ClassCacheInfo";

// the next 3 fields are not serializable
// and are in any case reinitialized when
// de-serialized. hence marked transient.
transient JarOutputStream _out_jar_stream;
transient JarInputStream _in_jar_stream;
transient JarFile _input_jarfile;

int _class_cache_version;
SPPNumAssigner _spp_num_assigner;

ArrayList _input_class_cache_entries;
ArrayList _output_class_cache_entries;

public ClassCache (String input_jar_name, String output_jar_name) throws IOException
{
    if (input_jar_name != null)
    {
        try {
	    try {
            _input_jarfile = new JarFile (input_jar_name);
	    } catch (IOException ioe) 
	    {
		System.err.println ("ERROR: Unable to open cache file \"" + input_jar_name + "\", please check the file path or permissions");
		util.fatal (ioe);
		System.exit (1);
	    }

            JarEntry je = _input_jarfile.getJarEntry (classcache_entry_name);
            if (je != null)
            {
                ObjectInputStream ois = new ObjectInputStream (_input_jarfile.getInputStream (je));
                Integer existing_version = (Integer) ois.readObject();
                _class_cache_version = existing_version.intValue() + 1;
                _spp_num_assigner = (SPPNumAssigner) ois.readObject();
                _input_class_cache_entries = (ArrayList) ois.readObject();
                System.out.println ("Class cache " + input_jar_name +
				" has " + _input_class_cache_entries.size() 
                                  + " entries, with version " + existing_version 
                                  + ", new version will be " + _class_cache_version);
                _spp_num_assigner.print_instrumentation_constraints();
            }
            else
            {
                util.fatal ("Jar file \"" + input_jar_name + "\" is not a valid class cache file; it does not contain an entry called " + classcache_entry_name);
            }
        } catch (Exception e)
        {
            util.fatal (e);
            System.exit (41);
        }
    }
    else
    {
        _class_cache_version = 1;
	try {
        _spp_num_assigner = new SPPNumAssigner(System.getProperty ("diduce.control.file"));
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    _output_class_cache_entries = new ArrayList();
    FileOutputStream fos = new FileOutputStream (output_jar_name);
    _out_jar_stream = new JarOutputStream (fos);
}

public int get_spp_num (StaticProgramPoint spp)
{
    return _spp_num_assigner.get_spp_num (spp);
}

// copies the contents of file 'filename' into the cache under
// the entry 'entry_name'
public void addFileToCache (String entry_name, String filename, ClassCacheEntry cce) throws IOException
{
    _output_class_cache_entries.add (cce);
    JarEntry je = new JarEntry (entry_name.toString());
    je.setMethod (ZipEntry.DEFLATED);
    _out_jar_stream.putNextEntry (je);
    FileInputStream src = new FileInputStream (filename);
    copyStreamToJarStream (_out_jar_stream, src);
    src.close();
}

private static void copyStreamToJarStream (JarOutputStream jout, InputStream src) throws IOException
{
    byte[] buf = new byte[512];
    do
    {
        int bread = src.read (buf);
        if (bread <= 0)
             break;
        jout.write(buf, 0, bread);
    } while (true);

    jout.closeEntry();
}

public InputStream lookup_and_copy_if_hit (ClassCacheEntry cce) 
{
    InputStream foundClass = null;
    if ((_input_class_cache_entries != null) && 
        (_input_class_cache_entries.contains (cce)))
    {
	System.out.println("$$$ found the entry for:" + cce);
        _output_class_cache_entries.add (cce);
        JarEntry je = _input_jarfile.getJarEntry (cce.get_entry_name());
        if (je != null)
        {
            try { 
                InputStream is = _input_jarfile.getInputStream (je);
                _out_jar_stream.putNextEntry (je);
                copyStreamToJarStream (_out_jar_stream, is);
                foundClass = _input_jarfile.getInputStream (je);
            } catch (IOException ioe) { util.fatal ("Error reading file \"" + cce.get_entry_name() + "\": " + ioe); }
        }
        else
            util.fatal ("Cache does not contain expected file \"" + cce.get_entry_name() + "\"");
    }
    else
    {
        String class_filename = cce.get_entry_name();
        String classname = class_filename.substring (0, class_filename.length() - ".class".length());
        classname = classname.replace (File.separatorChar, '.');
        _spp_num_assigner.release_pip_nums_for_class (classname);
    }
    return foundClass;
}

public void close () throws IOException
{
    JarEntry je = new JarEntry (classcache_entry_name);
    je.setMethod (ZipEntry.DEFLATED);
    _out_jar_stream.putNextEntry (je);
    ObjectOutputStream oos = new ObjectOutputStream (_out_jar_stream);
    oos.writeObject (new Integer (_class_cache_version));
    oos.writeObject (_spp_num_assigner);
    oos.writeObject (_output_class_cache_entries);
    oos.flush ();
    _out_jar_stream.closeEntry();
    _out_jar_stream.close();
}
}

