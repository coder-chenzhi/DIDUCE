/*
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
import diduce.invariants.*;

/* we ordinarily don't use the register_float/double functions 
   in this class */

public class Runtime {

public static ArrayList _relaxations;
public static boolean _gui_running;

// contains inv. in a vector instead of a hashtable for faster random access
public static Vector _inv_vector;
private static String _inv_filename;
public static ObjectOutputStream _inv_stream;

/* controlling parameters for the behaviour of the runtime */
public static boolean _check_violations;
public static boolean _verbose;
public static boolean _print_timestamp;
public static boolean _print_inv_at_exit;
public static float _min_conf_change;
public static float _new_code_conf_change;
public static PrintStream _outstream;

static {
    String s1 = System.getProperty ("diduce.read");
    String s2 = System.getProperty ("diduce.print");
    _check_violations = (System.getProperty ("diduce.report") != null);
    _verbose = (System.getProperty ("diduce.verbose") != null);

    boolean append_report_file =
             System.getProperty ("diduce.report.append") != null;
    _print_inv_at_exit =
             System.getProperty ("diduce.print.inv") != null;

    String s3 = System.getProperty ("diduce.report.file");
    try {
    _outstream = (s3 != null) ?
                 new PrintStream (new FileOutputStream (s3, append_report_file))
               : System.out;
    } catch (FileNotFoundException f) {
        util.fatal ("DIDUCE error: Unable to open file " + s3);
    }

    _outstream.println ("DIDUCE Runtime loaded by classloader "
	               + diduce.Runtime.class.getClassLoader());

    if (s1 != null)
    {
    try
    {
        _outstream.println ("Reading DIDUCE data from file: " + s1);
        ObjectInputStream inp =
           new ObjectInputStream (new FileInputStream(s1));

        Object o = inp.readObject ();
        _inv_vector = (Vector) o;
        o = inp.readObject ();
        Runtime._relaxations = (ArrayList) o;

        if (s2 != null)
        {
	    // we just want to view what's in the inv file, not run anything
	    // so we'll just print out the stats and exit.
            _outstream.println (s2);
            printAnalyzerStats ();
            System.exit (32);
        }
    } catch (Exception e)
    {
        e.printStackTrace();
        util.fatal ("Unable to read invariants and relaxations from file: " + s1 + "\n" + e);
    }
    }
    else
    {
        _inv_vector = new Vector (200);
        _relaxations = new ArrayList();
    }

    _print_timestamp = false;
    if (System.getProperty ("diduce.timestamp") != null)
        _print_timestamp = true;
    _min_conf_change = (float) 0.0;
    String s = System.getProperty ("diduce.min.confidence.change");
    if (s != null)
        _min_conf_change = new Float(s).floatValue();

    _new_code_conf_change = (float) -100.0;
    String sconf = System.getProperty ("diduce.new.code.confidence.change");
    if (sconf != null)
        _new_code_conf_change = new Float(sconf).floatValue();

    _inv_filename = System.getProperty ("diduce.write");
    if (_inv_filename != null)
    {
        try 
        {
            _inv_stream =
                   new ObjectOutputStream (new FileOutputStream(_inv_filename));
			java.lang.Runtime.getRuntime().addShutdownHook (new ShutdownThread());
//            System.err.println ("added shutdown hook");
        } catch (Exception e)
        {
             System.err.println ("Unable to open output file: " + _inv_filename + ":\n" + e);
        }
    }

    if (System.getProperty ("diduce.gui") != null)
        _gui_running = true;

    System.err.println ("DIDUCE Runtime initialization completed");
}

// put in just so we can run the static initializer
public static void main (String args[]) { }

public static void printStats ()
{
    if (_print_inv_at_exit)
        printAnalyzerStats ();
    saveAnalyzerStats ();
}

public static void saveAnalyzerStats ()
{
    if (_inv_stream != null)
    {
        try
        {
            _outstream.println ("Saving upto " + _inv_vector.size() + " invariants and " +
		                _relaxations.size() + " relaxations to file \"" + _inv_filename + "\"... ");
            _inv_stream.writeObject (_inv_vector);
            _inv_stream.writeObject (Runtime._relaxations);
            _inv_stream.close (); // close here, we don't want to write anything more
			                      // might change in future.
            _outstream.println ("Done");
            _outstream.flush ();

        } catch (Exception e)
        {
             System.err.println ("Unable to write out invariants to file: " + _inv_filename);
        }
    }
}

public static void register_object_int_load (Object ref, int val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (ref, val, val, spp_desc, spp_num);
}

public static void register_object_int_store (Object ref, int new_val, int old_val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (ref, new_val, old_val, spp_desc, spp_num);
}

public static void register_object_long_load (Object ref, long val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (ref, val, val, spp_desc, spp_num);
}

public static void register_object_long_store (Object ref, long new_val, long old_val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (ref, new_val, old_val, spp_desc, spp_num);
}

public static void register_object_float_load (Object ref, float val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (ref, val, val, spp_desc, spp_num);
}

public static void register_object_float_store (Object ref, float new_val, float old_val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (ref, new_val, old_val, spp_desc, spp_num);
}

public static void register_object_double_load (Object ref, double val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (ref, val, val, spp_desc, spp_num);
}

public static void register_object_double_store (Object ref, double new_val, double old_val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (ref, new_val, old_val, spp_desc, spp_num);
}

public static void register_object_ref_load (Object ref, Object val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (ref, val, val, spp_desc, spp_num);
}

public static void register_object_ref_store (Object ref, Object new_val, Object old_val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (ref, new_val, old_val, spp_desc, spp_num);
}

public static void register_static_int_load (int val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (null, val, val, spp_desc, spp_num);
}

public static void register_static_int_store (int new_val, int old_val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (null, new_val, old_val, spp_desc, spp_num);
}

public static void register_static_long_load (long val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (null, val, val, spp_desc, spp_num);
}

public static void register_static_long_store (long new_val, long old_val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (null, new_val, old_val, spp_desc, spp_num);
}

public static void register_static_float_load (float val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (null, val, val, spp_desc, spp_num);
}

public static void register_static_float_store (float new_val, float old_val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (null, new_val, old_val, spp_desc, spp_num);
}

public static void register_static_double_load (double val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (null, val, val, spp_desc, spp_num);
}

public static void register_static_double_store (double new_val, double old_val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (null, new_val, old_val, spp_desc, spp_num);
}

public static void register_static_ref_load (Object val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (null, val, val, spp_desc, spp_num);
}

public static void register_static_ref_store (Object new_val, Object old_val, String spp_desc, int spp_num)
{
    SPPData ad = mapSPPToDataObj (spp_desc, spp_num);
    for (int i = 0 ; i < ad._invariants.length ; i++)
        ad._invariants[i].newSample (null, new_val, old_val, spp_desc, spp_num);
}

/* spp_num shd be > 0
   if it's <  we fall back to the old hashtable implementation of storing
   SPPData objects instead of using the vector representation
*/
public static SPPData mapSPPToDataObj (String spp_descriptor, int spp_num)
{
    util.ASSERT (spp_num >= 0);
    SPPData ad;

    try {
       ad = (SPPData) _inv_vector.elementAt (spp_num);
    } catch (ArrayIndexOutOfBoundsException aioobe) {
       _inv_vector.ensureCapacity (spp_num+1);
       _inv_vector.setSize (spp_num+1);
       ad = (SPPData) _inv_vector.elementAt (spp_num);
    }

    if (ad == null)
    {
        ad = new SPPData(spp_descriptor);
        if (_check_violations)
        {
            String inv_names = "";

	    Invariant[] inv = ad.get_invariants();

            if (inv.length == 0)
                inv_names = "WARNING: no invariants";
            else
                for (int i = 0 ; i < inv.length ; i++)
                     inv_names += (util.delete_to_last_dot (inv[i].getClass().getName()) + " ");

            String inv_names_message = "(Attached: " + inv_names + ")";
            Relaxation r = new Relaxation (spp_descriptor, _new_code_conf_change, "New code", "", inv_names_message, 0);
        }

        _inv_vector.setElementAt (ad, spp_num);
    }
    return ad;
}

static class SortObject implements Comparable {
String _spp_descriptor;
Invariant _inv;
int _inv_number;

public SortObject (String spp_desc, Invariant inv, int inv_no)
{
    _spp_descriptor = spp_desc;
    _inv = inv;
    _inv_number = inv_no;
}

public float invariantStrength ()
{
    return _inv.invariantStrength ();
}

public int compareTo (Object o)
{
    float f1 = this.invariantStrength();
    float f2 = ((SortObject) o).invariantStrength();

    if (f1 < f2)
        return -1;
    else if (f1 == f2)
        return 0;
    else
        return 1;
}

public String toString ()
{
    StringBuffer sb =  new StringBuffer();

    sb.append ("--------------------------------------------\n");
    sb.append ("DIDUCE Invariant:\n");
    sb.append ("Confidence: " + _inv.invariantStrength() + "\n");
    sb.append ("In: " + 
               StaticProgramPoint.get_attribute (_spp_descriptor, "method") +
               ", line" +
               StaticProgramPoint.get_attribute (_spp_descriptor, "line") + "\n");
    sb.append ("Invariant name: " + util.delete_to_last_dot (_inv.getClass().getName()) + "\n");
    sb.append ("Old value: " + _inv + "\n");
    sb.append ("New value: -\n");
    sb.append ("Descriptor: " + _spp_descriptor + "\n");

    return sb.toString();
}

}

public static void printAnalyzerStats ()
{
    Vector real_invariants = new Vector ();
    int invariant_count = 0;

    Enumeration all_spps;
    all_spps = _inv_vector.elements();

    while (all_spps.hasMoreElements ())
    {
        SPPData d = (SPPData) all_spps.nextElement ();
        if (d == null)
            continue;

        String spp_desc = d.getSPPDesc();

        // todo: but what abt the names of the inv!
        if (d._invariants != null)
            for (int i = 0; i < d._invariants.length; i++)
                real_invariants.add (new SortObject (spp_desc, d._invariants[i], 0));
    }

    _outstream.println ("__________________________________");
    _outstream.println ("Invariants detected: " + real_invariants.size());
    _outstream.println ("List of invariants, sorted by confidence: ");

    Collections.sort (real_invariants);

    float conf = 0;
    for (int i = 0 ; i < real_invariants.size() ; i++)
    {
       _outstream.println (real_invariants.elementAt(i));
       conf += ((SortObject) real_invariants.elementAt(i)).invariantStrength ();
    }

    _outstream.println ("Average invariant confidence : " + conf/real_invariants.size());
}
public static void printAnalyzerStats_old ()
{
    _outstream.println ("__________________________________");
    _outstream.println ("Invariants for all program points: ");

    Vector real_invariants = new Vector ();

    int invariant_count = 0;

    Enumeration all_spps;
    all_spps = _inv_vector.elements();

    while (all_spps.hasMoreElements ())
    {
        SPPData d = (SPPData) all_spps.nextElement ();
        if (d == null)
            continue;

        String spp_desc = d.getSPPDesc();

        // todo: but what abt the names of the inv!
        if (d._invariants != null)
            for (int i = 0; i < d._invariants.length; i++)
                real_invariants.add (new SortObject (spp_desc, d._invariants[i], 0));
    }

    _outstream.println ("__________________________________");
    _outstream.println ("Invariants detected: " + real_invariants.size());
    _outstream.println ("List of invariants, sorted by confidence: ");

    Collections.sort (real_invariants);

    float conf = 0;
    for (int i = 0 ; i < real_invariants.size() ; i++)
    {
       _outstream.println (real_invariants.elementAt(i));
       conf += ((SortObject) real_invariants.elementAt(i)).invariantStrength ();
    }

    _outstream.println ("Average invariant confidence : " + conf/real_invariants.size());
}

}

