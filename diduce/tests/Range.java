class Range {

// tests the range invariant in various situations

public int iif; 
public static int isf; 
public long lif; 
public static long lsf; // int/long instance/static fields

static byte b[] = new byte[1];
static char c[] = new char[1];
static short s[] = new short[1];
static int[] ia = new int[1];
static long [] l = new long[1];

public static int foo (int x)
{
    return x;
}

public int foo_i (int x)
{
    return x;
}

public static void main (String args[])
{
    int i;
    Range o = new Range();

    for (i = 5000 ; i >= 0; i--)
    {
        o.iif = (i == 0) ? 100 : (i % 10);
        o.iif = (i == 0) ? -50 : (i % 10);
        isf = (i == 0) ? 100 : (i % 10);
        isf = (i == 0) ? -50 : (i % 10);

        o.lif = (i == 0) ? 100 : (i % 10);
        o.lif = (i == 0) ? -50 : (i % 10);
        lsf = (i == 0) ? 100 : (i % 10);
        lsf = (i == 0) ? -50 : (i % 10);

        b[0] = (byte) ((i == 0) ? 100 : (i % 10));
        b[0] = (byte) ((i == 0) ? -50 : (i % 10));

        s[0] = (short) ((i == 0) ? 100 : (i % 10));
        s[0] = (short) ((i == 0) ? -50 : (i % 10));

        c[0] = (char) ((i == 0) ? 100 : (i % 10));
        c[0] = (char) ((i == 0) ? -50 : (i % 10));

        ia[0] = (i == 0) ? 100 : (i % 10);
        ia[0] = (i == 0) ? -50 : (i % 10);

        l[0] = (i == 0) ? 100 : (i % 10);
        l[0] = (i == 0) ? -50 : (i % 10);

        foo ((i == 0) ? 100 : (i % 10));
        foo ((i == 0) ? -50 : (i % 10));
        o.foo_i ((i == 0) ? 100 : (i % 10));
        o.foo_i ((i == 0) ? -50 : (i % 10));
    }
}

}

