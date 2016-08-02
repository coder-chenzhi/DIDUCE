/**
 * This test is for making sure that the method constraints
 * specified in XML work as expected. 
 * Usage:
 *     step 1: Compile this class.
 *     step 2: Instrument this class with different constraints specified in
 *             method*.xml files.
 * 	       java -Ddiduce.watch.props=method1.xml diduce.watch Methods.class
 *     verification:
 *	   Make sure that the specified program points are not being
 *	   instrumented by checking the file "diduce.watch.log" 
 */
class Methods {

    int someInt;
    Object someObj;

    Methods (int i) {
	someInt = i;
	System.out.println(someInt);
	
    }

    void bar(Object arg[]) {
	System.out.println(arg);
    }

    Object bar() {
	return someObj;
    }

    void baz(int i, int j, int k) {
	someInt = i + j + k;
    }

    void baz(int i, float j) {
	someInt = i + (int) j;
    } 

    Object baz(Object obj, int i, Object obj2) {
	someObj = obj;
	return someObj;
    } 

    public static void main(String args[]) {
	Methods m = new Methods(1);
	
    }	
}
