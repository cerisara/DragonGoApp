package rene.util.sort;

public class SortStringNoCase extends SortString
{	String Orig;
	public SortStringNoCase (String s)
	{	super(s.toLowerCase());
		Orig=s;
	}
	public String toString ()
	{	return Orig;
	}
}
