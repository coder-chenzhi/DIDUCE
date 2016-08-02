class fields {

private int i;
private static int si;
private long j;
private static long sj;
private Object o;
private static Object so;

public static void main (String args[])
{
    fields f = new fields ();

    for (int x = 10000 ; x >= 0 ; x--)
    {
        f.i = (x == 0) ? 0 : 100;
        si = (x == 0) ? 0 : 100;
        f.j = (x == 0) ? 0 : 100;
        sj = (x == 0) ? 0 : 100;
        f.o = (x == 0) ? null : f;
        so = (x == 0) ? null : f;
    }
}

}
