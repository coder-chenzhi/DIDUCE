package diduce;

import java.io.*;
import java.util.zip.*;

public class ClassCacheEntry implements Serializable {

private static final long serialVersionUID = 2778912025439981393L;

String _entry_name;
long _checksum;

public ClassCacheEntry (String entry_name, InputStream is) 
{
    _entry_name = entry_name;
    _checksum = compute_checksum (is);
//    System.out.println ("Checksum for entry " + entry_name + " = " + _checksum);
}

public String get_entry_name () { return _entry_name; }

private long compute_checksum (InputStream is)
{
    CheckedInputStream cis = null;

    try { 
        cis = new CheckedInputStream (is, new CRC32());

        byte byte_buf[] = new byte[1024];
        int bytes_read;
        do { bytes_read = cis.read(byte_buf); } while (bytes_read >= 0);
		cis.close();
    } catch (IOException ioe) { 
        System.out.println ("Fatal error while reading file \"" + "\":\n" + ioe); 
        System.exit (22);
    }

    return cis.getChecksum().getValue();
}

public int hashCode ()
{
	return (_entry_name.hashCode() + (int) _checksum);
}

public boolean equals (Object o)
{
    if ((o == null) || (!(o instanceof ClassCacheEntry)))
        return false;

    ClassCacheEntry other = (ClassCacheEntry) o;
    return (_entry_name.equals (other._entry_name) && (_checksum == other._checksum));
}

public String toString() {
    return _entry_name;
}

}

