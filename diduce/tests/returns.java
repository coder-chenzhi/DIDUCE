// A very basic test for invariants on return values
class returns {

int _m, _o, _j; 
static int _sj, _sm, _so;

char m  ()  { _m++; if (_m > 100000) return 0; else return 1;}
long j  ()  { _j++; if (_j > 100000) return 0; else return 1;}
Object o  ()  { _o++; if (_o > 100000) return null; else return new Object();}

static long sj ()  { _sj++; if (_sj > 100000) return 0; else return 1;}
static int sm ()  { _sm++; if (_sm > 100000) return 0; else return 1;}
static Object so ()  { _so++; if (_so > 100000) return null; else return new Object();}

public static void main (String argv[])
{
    returns a = new returns(); // inv special
    for (int i = 0 ; i < 100001 ; i++)
    {
        a.m();         // inv virtual, int
        a.j();         // inv virtual, long
        a.o();         // inv virtual, obj

        a.sm();         // inv static, int
        a.sj();         // inv static, long
        a.so();         // inv static, obj
    }
}
}

