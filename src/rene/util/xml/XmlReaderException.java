package rene.util.xml;

public class XmlReaderException extends Exception
{	String Line;
	int Pos;
	String S;
	public XmlReaderException (String s, String line, int pos)
	{	super(s);
		S=s;
		Line=line;
		Pos=pos;
	}
	public XmlReaderException (String s)
	{	this(s,"",0);
	}
	public String getLine ()
	{	return Line;
	}
	public int getPos ()
	{	return Pos;
	}
	public String getText ()
	{	return S;
	}
}
