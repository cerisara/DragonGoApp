package rene.util.xml;

import java.io.PrintWriter;
import java.util.Vector;

import rene.util.parser.StringParser;

public class XmlWriter
{	PrintWriter Out;
	public XmlWriter (PrintWriter o)
	{	Out=o;
	}
	public void printTag (String tag, String content)
	{	startTag(tag);
		print(content);
		endTag(tag);
	}
	public void printTagNewLine (String tag, String content)
	{	printTag(tag,content);
		Out.println();
	}
	public void printTag (String tag, String arg, String value, String content)
	{	startTag(tag,arg,value);
		print(content);
		endTag(tag);
	}
	public void printTagNewLine (String tag, String arg, String value, String content)
	{	printTag(tag,arg,value,content);
		Out.println();
	}
	public void startTag (String tag)
	{	Out.print("<");
		Out.print(tag);
		Out.print(">");
	}
	public void startTag (String tag, String arg, String value)
	{	Out.print("<");
		Out.print(tag);
		printArg(arg,value);
		Out.print(">");
	}
	public void finishTag (String tag, String arg, String value)
	{	Out.print("<");
		Out.print(tag);
		printArg(arg,value);
		Out.println("/>");
	}
	public void finishTag (String tag)
	{	Out.print("<");
		Out.print(tag);
		Out.print("/>");
	}
	public void finishTagNewLine (String tag)
	{	Out.print("<");
		Out.print(tag);
		Out.println("/>");
	}
	public void startTagStart (String tag)
	{	Out.print("<");
		Out.print(tag);
	}
	public void startTagEnd ()
	{	Out.print(">");
	}
	public void finishTag ()
	{	Out.print("/>");
	}
	public void finishTagNewLine ()
	{	Out.println("/>");
	}
	public void startTagEndNewLine ()
	{	Out.println(">");
	}
	public void printArg (String arg, String value)
	{	Out.print(" ");
		print(arg);
		Out.print("=\"");
		print(value);
		Out.print("\"");
	}
	public void startTagNewLine (String tag, String arg, String value)
	{	startTag(tag,arg,value);
		Out.println();
	}
	public void startTagNewLine (String tag)
	{	startTag(tag);
		Out.println();
	}
	public void endTag (String tag)
	{	Out.print("</");
		Out.print(tag);
		Out.print(">");
	}
	public void endTagNewLine (String tag)
	{	endTag(tag);
		Out.println();
	}
	public void println ()
	{	Out.println();
	}
	public void print (String s)
	{	Out.print(XmlTranslator.toXml(s));
	}
	public void println (String s)
	{	Out.println(XmlTranslator.toXml(s));
	}
	public void printEncoding (String s)
	{	if (s.equals("")) Out.println("<?xml version=\"1.0\"?>");
		else Out.println("<?xml version=\"1.0\" encoding=\""+s+"\"?>");
	}
	public void printXml()
	{	printEncoding("");
	}
	public void printEncoding ()
	{	printEncoding("utf-8");
	}
	public void printXls (String s)
	{	Out.println("<?xml-stylesheet href=\""+s+"\" type=\"text/xsl\"?>");
	}
	public void printParagraphs (String s, int linelength)
	{	StringParser p=new StringParser(s);
		Vector v=p.wrapwords(linelength);
		for (int i=0; i<v.size(); i++)
		{	startTag("P");
			s=(String)v.elementAt(i);
			StringParser q=new StringParser(s);
			Vector w=q.wraplines(linelength);
			for (int j=0; j<w.size(); j++)
			{	if (j>0) println();
				s=(String)w.elementAt(j);
				print(s);
			}
			endTagNewLine("P");
		}
	}
	public void printDoctype (String top, String dtd)
	{	Out.print("<!DOCTYPE ");
		Out.print(top);
		Out.print(" SYSTEM \"");
		Out.print(dtd);
		Out.println("\">");
	}
	
	public void close ()
	{	Out.close();
	}
}
