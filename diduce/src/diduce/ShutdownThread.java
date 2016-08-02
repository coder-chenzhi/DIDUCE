package diduce;

public class ShutdownThread extends java.lang.Thread {

public void run()
{
	System.out.println ("Shutdown thread running");
    diduce.Runtime.printStats();
}

}
