import java.util.*;

class example1 {

static int n_swaps;
static Random r;

// bubble sorts int array a, increments n_swaps by the # of swaps 
private static void bubble_sort (int a[])
{
    for (int i = 0; i < a.length; i++)
	for (int j = i+1; j < a.length; j++)
	    if (a[i] < a[j])
	    {
		int tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;

		n_swaps++;
	    }
}

private static int[] get_random_array (int n)
{
    int[] a = new int[n];

    for (int i = 0 ; i < a.length; i++)
	a[i] = r.nextInt();

    return a;
}

public static void main (String args[])
{
    r = new Random (0L);
    n_swaps = 0;

    int times = 20;

    for (int i = 0; i < times; i++)
    {
        int a[] = get_random_array (10);
        bubble_sort (a);
    }

    System.out.println ("There were " + n_swaps + " swaps");
}

}

