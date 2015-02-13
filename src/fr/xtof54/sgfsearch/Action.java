package fr.xtof54.sgfsearch;

import java.io.PrintWriter;
import java.util.Vector;

import rene.util.list.ListClass;
import rene.util.list.ListElement;
import rene.util.parser.StringParser;
import rene.util.xml.XmlWriter;

/** 
Has a type and arguments (as in SGF, e.g. B[ih] of type "B" and
Methods include the printing on a PrintWriter.
*/
public class Action
{	String Type; // the type
	ListClass Arguments; // the list of argument strings
	
	/**
	Initialize with type only
	*/
	public Action (String s)
	{	Type=s;
		Arguments=new ListClass();
	}

	/**
	Initialize with type and one argument to that type tag.
	*/
	public Action (String s, String arg)
	{	Type=s;
		Arguments=new ListClass();
		addargument(arg);
	}
	
	public void addargument (String s)
	// add an argument ot the list (at end)
	{	Arguments.append(new ListElement(s));
	}
	
	public void toggleargument (String s)
	// add an argument ot the list (at end)
	{	ListElement ap=Arguments.first();
		while (ap!=null)
		{	String t=(String)ap.content();
			if (t.equals(s))
			{	Arguments.remove(ap);
				return;
			}
			ap=ap.next();
		}
		Arguments.append(new ListElement(s));
	}

	/** Find an argument */	
	public boolean contains (String s)
	{	ListElement ap=Arguments.first();
		while (ap!=null)
		{	String t=(String)ap.content();
			if (t.equals(s)) return true;
			ap=ap.next();
		}
		return false;
	}
	
	public void print (PrintWriter o)
	// print the action
	{	if (Arguments.first()==null ||
			(Arguments.first()==Arguments.last() &&
			((String)Arguments.first().content()).equals("")))
			return;
		o.println();
		o.print(Type);
		ListElement ap=Arguments.first();
		while (ap!=null)
		{	o.print("[");
			String s=(String)ap.content();
			StringParser p=new StringParser(s);
			Vector v=p.wrapwords(60);
			for (int i=0; i<v.size(); i++)
			{	s=(String)v.elementAt(i);
				if (i>0) o.println();
				int k=s.indexOf(']');
				while (k>=0)
				{	if (k>0) o.print(s.substring(0,k));
					o.print("\\]");
					s=s.substring(k+1);
					k=s.indexOf(']');
				}
				o.print(s);
			}
			o.print("]");
			ap=ap.next();
		}
	}
	
	/**
	Print the node content in XML form.
	*/
	public void print (XmlWriter xml, int size, int number)
	{	if (Type.equals("C"))
		{	xml.startTagNewLine("Comment");
			printTextArgument(xml);
			xml.endTagNewLine("Comment");
		}
		else if (Type.equals("GN") 
			|| Type.equals("AP")
			|| Type.equals("FF")
			|| Type.equals("GM")
			|| Type.equals("N")
			|| Type.equals("SZ")
			|| Type.equals("PB")
			|| Type.equals("BR")
			|| Type.equals("PW")
			|| Type.equals("WR")
			|| Type.equals("HA")
			|| Type.equals("KM")
			|| Type.equals("RE")
			|| Type.equals("DT")
			|| Type.equals("TM")
			|| Type.equals("US")
			|| Type.equals("WL")
			|| Type.equals("BL")
			|| Type.equals("CP")
			)
		{
		}
		else if (Type.equals("B"))
		{	xml.startTagStart("Black");
			xml.printArg("number",""+number);
			xml.printArg("at",getXMLMove(size));
			xml.finishTagNewLine();
		}
		else if (Type.equals("W"))
		{	xml.startTagStart("White");
			xml.printArg("number",""+number);
			xml.printArg("at",getXMLMove(size));
			xml.finishTagNewLine();
		}
		else if (Type.equals("AB"))
		{	printAllFields(xml,size,"AddBlack");
		}
		else if (Type.equals("AW"))
		{	printAllFields(xml,size,"AddWhite");
		}
		else if (Type.equals("AE"))
		{	printAllFields(xml,size,"Delete");
		}
		else if (Type.equals("MA"))
		{	printAllFields(xml,size,"Mark");
		}
		else if (Type.equals("M"))
		{	printAllFields(xml,size,"Mark");
		}
		else if (Type.equals("SQ"))
		{	printAllFields(xml,size,"Mark","type","square");
		}
		else if (Type.equals("CR"))
		{	printAllFields(xml,size,"Mark","type","circle");
		}
		else if (Type.equals("TR"))
		{	printAllFields(xml,size,"Mark","type","triangle");
		}
		else if (Type.equals("TB"))
		{	printAllFields(xml,size,"Mark","territory","black");
		}
		else if (Type.equals("TW"))
		{	printAllFields(xml,size,"Mark","territory","white");
		}
		else if (Type.equals("LB"))
		{	printAllSpecialFields(xml,size,"Mark","label");
		}
		else
		{	xml.startTag("SGF","type",Type);
			ListElement ap=Arguments.first();
			while (ap!=null)
			{	xml.startTag("Arg");
				String s=(String)ap.content();
				StringParser p=new StringParser(s);
				Vector v=p.wrapwords(60);
				for (int i=0; i<v.size(); i++)
				{	s=(String)v.elementAt(i);
					if (i>0) xml.println();
					xml.print(s);
				}
				ap=ap.next();
				xml.endTag("Arg");
			}
			xml.endTagNewLine("SGF");
		}
	}

	/**
	Print the node content of a move in XML form and take care of times
	and names.
	*/
	public void printMove (XmlWriter xml, int size, int number, Node n)
	{	String s="";
		if (Type.equals("B")) s="Black";
		else if (Type.equals("W")) s="White";
		else return;
		xml.startTagStart(s);
		xml.printArg("number",""+number);
		if (n.contains("N")) xml.printArg("name",n.getaction("N"));
		if (s.equals("Black") && n.contains("BL"))
			xml.printArg("timeleft",n.getaction("BL"));
		if (s.equals("White") && n.contains("WL"))
			xml.printArg("timeleft",n.getaction("WL"));
		xml.printArg("at",getXMLMove(size));
		xml.finishTagNewLine();
	}

	/**
	Test, if this action contains printed information
	*/
	public boolean isRelevant ()
	{	if (Type.equals("GN") 
			|| Type.equals("AP")
			|| Type.equals("FF")
			|| Type.equals("GM")
			|| Type.equals("N")
			|| Type.equals("SZ")
			|| Type.equals("PB")
			|| Type.equals("BR")
			|| Type.equals("PW")
			|| Type.equals("WR")
			|| Type.equals("HA")
			|| Type.equals("KM")
			|| Type.equals("RE")
			|| Type.equals("DT")
			|| Type.equals("TM")
			|| Type.equals("US")
			|| Type.equals("CP")
			|| Type.equals("BL")
			|| Type.equals("WL")
			|| Type.equals("C")
			)
		return false;
		else return true;
	}

	/**
	Print all arguments as field positions with the specified tag.
	*/
	public void printAllFields (XmlWriter xml, int size, String tag)
	{	ListElement ap=Arguments.first();
		while (ap!=null)
		{	String s=(String)ap.content();
			xml.startTagStart(tag);
			xml.printArg("at",getXMLMove(ap,size));
			xml.finishTagNewLine();
			ap=ap.next();
		}
	}
	
	public void printAllFields (XmlWriter xml, int size, String tag,
		String argument, String value)
	{	ListElement ap=Arguments.first();
		while (ap!=null)
		{	String s=(String)ap.content();
			xml.startTagStart(tag);
			xml.printArg(argument,value);
			xml.printArg("at",getXMLMove(ap,size));
			xml.finishTagNewLine();
			ap=ap.next();
		}
	}

	public void printAllSpecialFields (XmlWriter xml, int size, String tag,
		String argument)
	{	ListElement ap=Arguments.first();
		while (ap!=null)
		{	String s=(String)ap.content();
			StringParser p=new StringParser(s);
			s=p.parseword(':');
			p.skip(":");
			String value=p.parseword();
			xml.startTagStart(tag);
			xml.printArg(argument,value);
			xml.printArg("at",getXMLMove(ap,size));
			xml.finishTagNewLine();
			ap=ap.next();
		}
	}

	/**
	@return The readable coordinate version (Q16) of a move,
	stored in first argument.
	*/
	public String getXMLMove (ListElement ap, int size)
	{	if (ap==null) return "";
		String s=(String)ap.content();
		if (s==null) return "";
		int i=Field.i(s),j=Field.j(s);
		if (i<0 || i>=size || j<0 || j>=size) return "";
		return Field.coordinate(Field.i(s),Field.j(s),size);
	}
	
	public String getXMLMove (int size)
	{	ListElement ap=Arguments.first();
		return getXMLMove(ap,size);
	}

	public void printTextArgument (XmlWriter xml)
	{	ListElement ap=Arguments.first();
		if (ap==null) return;
		xml.printParagraphs((String)ap.content(),60);
	}

	// modifiers
	public void type (String s) { Type=s; }

	// access methods:
	public String type () { return Type; }
	public ListElement arguments () { return Arguments.first(); }
	public String argument ()
	{	if (arguments()==null) return "";
		else return (String)arguments().content();
	}
}
