package rene.util.sort;

import java.io.*;

import java.util.*;

/**
Quick sort implementation. Sorts an array or a vector of SortObject.
*/

public class Sorter
{	static public void sort (SortObject v[])
	{	QuickSort(v,0,v.length-1);
	}
	
	static public void sort (SortObject v[], int n)
	{	QuickSort(v,0,n-1);
	}
	
	static public void sort (Vector v)
	{	SortObject o[]=new SortObject[v.size()];
		v.copyInto(o);
		sort(o);
		for (int i=0; i<o.length; i++)
			v.setElementAt(o[i],i);
	}
	
	static public void QuickSort (SortObject a[], int lo0, int hi0)
	{	int lo = lo0;
		int hi = hi0;
		SortObject mid;

		if ( hi0 > lo0)
		{	mid = a[ ( lo0 + hi0 ) / 2 ];
			while( lo <= hi )
			{	while( ( lo < hi0 ) && ( a[lo].compare(mid)<0 ) )
				   ++lo;
				while( ( hi > lo0 ) && ( a[hi].compare(mid)>0 ) )
				   --hi;
				if( lo <= hi ) 
				{	swap(a, lo, hi);
					++lo;
					--hi;
				}
			}
			if( lo0 < hi ) QuickSort( a, lo0, hi );
			if( lo < hi0 ) QuickSort( a, lo, hi0 );
		}	
	}

	static private void swap (SortObject a[], int i, int j)
	{	SortObject T;
		T = a[i]; 
		a[i] = a[j];
		a[j] = T;
	}

	static public void QuickSort (Object a[], int lo0, int hi0)
	{	int lo = lo0;
		int hi = hi0;
		SortObject mid;

		if ( hi0 > lo0)
		{	mid = (SortObject) a[ ( lo0 + hi0 ) / 2 ];
			while( lo <= hi )
			{	while( ( lo < hi0 ) && ( ((SortObject)a[lo]).compare(mid)<0 ) )
				   ++lo;
				while( ( hi > lo0 ) && ( ((SortObject)a[hi]).compare(mid)>0 ) )
				   --hi;
				if( lo <= hi ) 
				{	swap(a, lo, hi);
					++lo;
					--hi;
				}
			}
			if( lo0 < hi ) QuickSort( a, lo0, hi );
			if( lo < hi0 ) QuickSort( a, lo, hi0 );
		}
	}

	static private void swap (Object a[], int i, int j)
	{	Object T;
		T = a[i]; 
		a[i] = a[j];
		a[j] = T;
	}

	public static void main (String args[])
		throws IOException
	// Sort the incoming lines and remove doublicates
	{	BufferedReader in=new BufferedReader(
			new InputStreamReader(System.in));
		Vector v=new Vector();
		while (true)
		{	String line=in.readLine();
			if (line==null) break;
			v.addElement(new SortString(line));
		}
		in.close();
		sort(v);
		Enumeration e=v.elements();
		String last=null;
		while (e.hasMoreElements())
		{	String s=((SortString)e.nextElement()).toString();
			if (last==null || !s.equals(last))
			{	System.out.println(s);
				last=s;
			}
		}
	}
}

