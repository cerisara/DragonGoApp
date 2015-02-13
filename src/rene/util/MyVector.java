package rene.util;

import java.util.Enumeration;
import java.util.Iterator;

/**
This is a more effective replacement of the Vector class. It is based
on a growing array. If an object is removed, it is replaced by null.
The class knows about the first null object (the gap). Searching for
elements or other operations automatically compress the array by
copying the elements upwards, but only as far as they need to go
anyway.

Accessing an element is very effective, at least the second time. If
you want to make sure, it is always effective, compress first. The
most effective way is to get the object array itself.

The objects can be enumerated. The object returned by nextElement()
is found very rapidly. E.g. it can be deleted at once.

Enumeration is not reentrant. Do it only once each time.

Nothing in this class is synchronized!
**/

public class MyVector <klasse>
	implements Enumeration<klasse>, Iterator<klasse>, Iterable<klasse>
{	klasse O[];
	int OSize,ON,OLast,Gap;
	int EN=0;
	
	public MyVector (int initsize)
	{	O=(klasse[])(new Object[initsize]);
		OSize=initsize;
		OLast=ON=0;
		Gap=-1;
	}
	public MyVector ()
	{	this(8);
	}

	/**
	Add an element. Extend the array, if necessary.
	*/	
	public void addElement (klasse o)
	{	if (OLast>=OSize) extend();
		O[OLast++]=o;
		ON++;
	}
	
	/**
	Extend the array, or get space by compressing it.
	*/
	public void extend ()
	{	if (ON<OLast/2)
		{	compress(); return;
		}
		klasse o[]=(klasse[])(new Object[2*OSize]);
		System.arraycopy(O,0,o,0,OLast);
		OSize*=2;
		O=o;
	}
	
	/**
	Compress the array.
	*/
	public void compress ()
	{	if (Gap<0) return;
		int k=Gap;
		for (int i=Gap; i<OLast; i++)
		{	if (O[i]==null) continue;
			O[k++]=O[i];
		}
		ON=k;
		for (int i=k; i<OLast; i++) O[i]=null;
		Gap=-1; OLast=ON;
	}
	
	/**
	Get an enumeration of this array.
	*/
	public Enumeration<klasse> elements ()
	{	compress(); EN=0;
		return this;
	}

	/**
	Method for Enumeration.
	*/	
	public boolean hasMoreElements ()
	{	while (EN<OLast && O[EN]==null) EN++;
		return EN<OLast;
	}
	
	/**
	Method for Enumeration.
	*/	
	public klasse nextElement ()
	{	if (!hasMoreElements())
			throw new ArrayIndexOutOfBoundsException(OLast);
		return O[EN++];
	}

	/**
	 * Method for Iterator
	 */
	public boolean hasNext()
	{	while (EN<OLast && O[EN]==null) EN++;
		return EN<OLast;
	}
	
	/**
	 * Method for Iterator
	 */
	public klasse next()
	{	if (!hasMoreElements())
		throw new ArrayIndexOutOfBoundsException(OLast);
		return O[EN++];
	}
	
	/**
	 * Method for iterator
	 */
	public void remove()
	{	int i=EN-1;
		if (EN<0) return;
		O[i]=null; ON--;
		if (Gap<0 || Gap>i) Gap=i;
		if (i==OLast-1) OLast--;
		while (OLast>0 && O[OLast-1]==null) OLast--;
		if (Gap>=OLast) Gap=-1;		
	}
	
	/**
	 * Method for iteration
	 */
	public Iterator<klasse> iterator()
	{	return this;
	}

	/**
	Clear this array, but keep its memory!
	*/	
	public void removeAllElements ()
	{	for (int i=0; i<OLast; i++) O[i]=null;
		ON=OLast=0; Gap=-1;
	}
	
	/**
	Remove a single element. This will also compress the part below
	the element, or all, if it is not found.
	*/
	public void removeElement (klasse o)
	{	int i=indexOf(o);
		if (i<0) return;
		O[i]=null; ON--;
		if (Gap<0 || Gap>i) Gap=i;
		if (i==OLast-1) OLast--;
		while (OLast>0 && O[OLast-1]==null) OLast--;
		if (Gap>=OLast) Gap=-1;
	}
	
	/**
	Find an element. Compress on the way. Check for the last element,
	returned by nextElement() first. Equality is checked with the
	equal() function.
	@return -1, if not found.
	*/
	public int indexOf (klasse o)
	{	if (EN>0 && EN<=OLast && O[EN-1].equals(o)) return EN-1;
		if (Gap<0)
		{	for (int i=0; i<OLast; i++)
			{	if (O[i].equals(o)) return i;
			}
			return -1;
		}
		for (int i=0; i<Gap; i++)
		{	if (O[i].equals(o)) return i;
		}
		int k=Gap;
		for (int i=Gap; i<OLast; i++)
		{	if (O[i]==null) continue;
			if (O[i].equals(o))
			{	Gap=k;
				return i;
			}
			O[k++]=O[i]; O[i]=null;
		}
		ON=k;
		for (int i=k; i<OLast; i++) O[i]=null;
		Gap=-1; OLast=ON;
		return -1;
	}
	
	/**
	@return the number of objects in the vector.
	*/
	public int size ()
	{	return ON;
	}
	
	/**
	Get the element at a given position. Second access will always be
	effective. First access compresses. Throws an exception, if the
	index is invalid.
	*/
	public klasse elementAt (int n)
	{	if (n<0 || n>=ON)
			throw new ArrayIndexOutOfBoundsException(n);
		if (Gap<0 || n<Gap) return O[n];
		int k=Gap;
		for (int i=Gap; i<OLast; i++)
		{	if (O[i]==null) continue;
			O[k]=O[i]; O[i]=null;
			if (k==n)
			{	klasse ret=O[k];
				k++; Gap=k;
				if (Gap>=ON)
				{	for (int j=Gap; j<OLast; j++) O[j]=null;
					OLast=ON; Gap=-1;
				}
				return ret;
			}
			k++;
		}
		// never happens
		throw new ArrayIndexOutOfBoundsException(n); 
	}
	
	/**
	Get the array itself (compressed). Make sure, you also use size()
	to determine the true length of the array. Do not change objects
	beyond the size! Do not set objects to null!
	*/
	public Object[] getArray ()
	{	compress();
		return O;
	}
	
	/**
	Copy the array into an object array of at least the same size.
	*/
	public void copyInto (Object o[])
	{	compress();
		System.arraycopy(O,0,o,0,ON);
	}

	/**
	Test for equality with another vector, using equals.
	*/
	public boolean equals (MyVector V)
	{	if (V.ON!=ON) return false;
		V.compress(); compress();
		for (int i=0; i<ON; i++)
		{	if (!V.O[i].equals(O[i])) return false;
		}
		return true;
	}
	
	/**
	Test for equality with another vector, using object equality.
	*/
	public boolean equalsIdentical (MyVector V)
	{	if (V.ON!=ON) return false;
		V.compress(); compress();
		for (int i=0; i<ON; i++)
		{	if (V.O[i]!=O[i]) return false;
		}
		return true;
	}
	
	/**
	Truncate the vector to n elements, if it has more.
	*/
	public void truncate (int n)
	{	if (n>=ON) return;
		compress();
		for (int i=n; i<OLast; i++) O[i]=null;
		OLast=ON=n;
	}
	
	public static void main (String args[])
	{	MyVector V=new MyVector();
		for (int i=1; i<=10; i++)
			V.addElement("Element "+i);
		for (int i=4; i<=9; i++)
			V.removeElement("Element "+i);
		System.out.println("--> "+V.elementAt(3));
		System.out.println(V.ON+" elements, "+V.OLast+" used, "+V.Gap+" gap.");
		System.out.println("--> "+V.elementAt(3));
		System.out.println(V.ON+" elements, "+V.OLast+" used, "+V.Gap+" gap.");
		for (int i=11; i<=20; i++)
			V.addElement("Element "+i);
		System.out.println(V.ON+" elements, "+V.OLast+" used ,"+V.Gap+" gap.");
		Enumeration E=V.elements();
		while (E.hasMoreElements())
		{	System.out.println((String)E.nextElement());
		}
		System.out.println(V.ON+" elements, "+V.OLast+" used, "+V.Gap+" gap.");
	}
}
