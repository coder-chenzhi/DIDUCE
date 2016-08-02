class SelfLoop {

Object _f;

public static void main (String s[])
{
    SelfLoop o = new SelfLoop ();
    SelfLoop o1 = new SelfLoop ();

    for (int i = 5000 ; i >= 0 ; i--)
    {
         o._f = (i == 0) ? o : o1;
         o._f = (i == 0) ? o1 : o;
    }
}

}
