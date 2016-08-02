class Sanity {

boolean z;
byte b;
short s;
char c;
int i;
long l;
float f;
double d;
Object o;
Object oa[];

static boolean sz;
static byte sb;
static short ss;
static char sc;
static int si;
static long sl;
static float sf;
static double sd;
static Object so;
static Object[] soa;

public void foo(int x)

{
    
boolean z1;
byte b1;
short s1;
char c1;
int i1;
long l1;
float f1;
double d1;
Object o1;
Object[] oa1;

// field read/writes
    z = true;
    z1 = z;

    b = (byte) x;
    b1 = b;
   
    s = (short) x;
    s1 = s;

    c = (char) x;
    c1 = c;

    i = x;
    i1 = i;

    l = x;
    l1 = l;

    f = x;
    f1 = f;

    d = x;
    d1 = d;

    o = new Object();
    o1 = o;

    oa = new Object[2];
    oa1 = oa;


// static read/writes
    sz = true;
    z1 = sz;

    sb = (byte) x;
    b1 = sb;
   
    ss = (short) x;
    s1 = ss;

    sc = (char) x;
    c1 = sc;

    si = x;
    i1 = si;

    sl = x;
    l1 = sl;

    sf = x;
    f1 = sf;

    sd = x;
    d1 = sd;

    so = new Object();
    o1 = so;

    soa = new Object[2];
    oa1 = soa;

   
// array read/writes
    boolean az[] = new boolean [2];
    byte ab[] = new byte [2];
    char ac[] = new char [2];
    short as[] = new short [2];
    int ai[] = new int [2];
    long al[] = new long [2];
    float af[] = new float [2];
    double ad[] = new double [2];
    Object ao[] = new Object [2];
    Object aoa[][] = new Object [2][2];

    az[0] = true;
    z1 = az[0];

    ab[0] = (byte) x;
    b1 = ab[0];
   
    as[0] = (short) x;
    s1 = as[0];

    ac[0] = (char) x;
    c1 = ac[0];

    ai[0] = x;
    i1 = ai[0];

    al[0] = x;
    l1 = al[0];

    af[0] = x;
    f1 = af[0];

    ad[0] = x;
    d1 = ad[0];

    ao[0] = new Object();
    o1 = ao[0];

    aoa[0] = new Object[2];
    oa1 = aoa[0];

}

public static void main (String args[])

{
    Sanity h = new Sanity();
    h.foo (1);

}

}


