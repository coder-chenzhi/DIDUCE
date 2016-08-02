class params {

public params()
{

}

public void q (int x) { }

public static int p_static (int i, double d, Object o, int[] x)
{
    return i;
}

public int p_virtual (int i, double d, Object o, int[] x)
{
    return i;
}

public void p1 (int x) { }

public static void main (String argv[])
{
	int j = 2;
	double d = 3.0;
	params o = new params();
	int arr[] = null;

    for (int i = 0 ; i < 10000 ; i++)
	{
	    if (i == 9999)
	    {
	        j = 3; 
	  	    d = 13.0;
		    o = new params_sub ();
			arr = new int[2];
        }

        p_static (j, d, o, arr);
        o.p_virtual (j, d, o, arr);
	}
}
}

class params_sub extends params { }

