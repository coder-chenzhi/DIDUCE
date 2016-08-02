package diduce;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.lang.reflect.*;

/**
 * A system class loader to instrument the loaded classes
 * "on-the-fly"
 * Note: This is a "fragile" code. Be sure about making changes
 * to it. 
 */
public class DiduceClassLoader extends ClassLoader {

private ClassLoader parent;
public static final boolean debug = false;
private boolean isInitWatch = false;

/**
 * diduce.watch is explicitly loaded using DiduceClassLoader
 * to make accessible to diduce.run which is loaded by this
 * class loader. If we don't enforce this, diduce.watch
 * gets loaded by sun.misc.launch$Application class loader,
 * there by in a different namespace than diduce.run.
 */
private Class watchClass = null;
private Method watchInitMethod = null;
private Method watchInstrMethod = null;

public DiduceClassLoader(ClassLoader parent) {
    super(parent);
    this.parent = parent;
}

public synchronized Class loadClass(String name)
    throws ClassNotFoundException {

    if (debug) {
	System.out.println("loadClass called on class:" + name);
    }

    // First, check if the class has already been loaded
    Class c = findLoadedClass(name);
    if (c == null) {
        try {
    	     if (debug) {
		System.out.println("Class not loaded, Loading class:" + name);
	     }

	    /**
	     * By-pass the JDK classes
	     * load diduce and BCEL classes through this loader
	     */ 
	    if (name.startsWith("diduce") ||
	            name.startsWith("de.fub")) {
		return loadDiduceClass(name);
	    } else if (!name.startsWith("java") &&
		   !name.startsWith("javax") &&
		   !name.startsWith("sun") &&
		   !name.startsWith("sunw") &&
		   !name.startsWith("com.sun") &&
		   !name.startsWith("org.omg") &&
		   !name.startsWith("org.apache") &&
		   !name.startsWith("org.xml") &&
		   !name.startsWith("org.w3c") &&
		   !name.startsWith("org.ietf")) { 
		return loadInstrumentedClass(name);
	    }
	    if (parent != null) {
		c = parent.loadClass(name);
	    } else {
		c = findSystemClass(name);
            }
	} catch (ClassNotFoundException e) {

	    // If still not found, then invoke findClass in order
	    // to find the class.
	    c = findClass(name);
	}
    }
    return c;
}

private Class loadInstrumentedClass(String name)
	throws ClassNotFoundException {
    if (debug) {
        System.out.println("Instrumented class loader: about to load " + name);
    }
    byte[] b = null;
    try {
        //InputStream in = getResourceAsStream(name.replace('.', '/') + ".class");
	if (!isInitWatch) {
	    loadWatchClass();
	    watchInitMethod.invoke(null, new Object[]{ });
	    isInitWatch = true;
	}
        URL resource = getResource(name.replace('.', '/') + ".class");
	b = (byte[]) watchInstrMethod.invoke(null, new Object[] { name + ".class",
			resource.openStream(), resource.openStream()});
    } catch (Exception exp) {
	exp.printStackTrace();
  	ClassNotFoundException clExp = new ClassNotFoundException(exp.getMessage());	
	// clExp.initCause(exp); over-writing cause is not allowed
	throw clExp;
    }
    // get the instrumented bytes
    return defineClass(name, b, 0, b.length);
}


private void loadWatchClass() throws Exception {
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    watchClass = cl.loadClass("diduce.watch");
    watchInitMethod = watchClass.getDeclaredMethod("init", new Class[] {}); 
    watchInstrMethod = watchClass.getDeclaredMethod("instrument_class", new Class[] {
			String.class, InputStream.class, InputStream.class});
}

private Class loadDiduceClass(String name)
		throws ClassNotFoundException {
    if (debug) {
        System.out.println("Diduce class loader: about to load " + name);
    }
    byte[] b = null;
    try {
        InputStream in = getResourceAsStream(name.replace('.', '/') + ".class");
	b = getBytes(in);
    } catch (Exception exp) {
        throw (ClassNotFoundException) new ClassNotFoundException().initCause(exp);
    }
    // get the instrumented bytes
    return defineClass(name, b, 0, b.length);
}

   
static byte[] getBytes(InputStream in) throws IOException { 
    // Read until end of stream is reached
    byte[] b = new byte[1024];
    int total = 0;
    int len = 0;
    while ((len = in.read(b, total, b.length - total)) != -1) {
        total += len;
        if (total >= b.length) {
            byte[] tmp = new byte[total * 2];
            System.arraycopy(b, 0, tmp, 0, total);
            b = tmp;
        }
    }

    // Trim array to correct size, if necessary
    if (total != b.length) {
        byte[] tmp = new byte[total];
        System.arraycopy(b, 0, tmp, 0, total);
        b = tmp;
    }
    return b;
}
}
