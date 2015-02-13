package rene.util.sort;

public class SortString implements SortObject
{	String S;
	public SortString (String s)
	{	S=s;
	}
	public int compare (SortObject o)
	{	SortString s=(SortString)o;
		return S.compareTo(s.S);
	}
	public String toString ()
	{	return S;
	}
}
