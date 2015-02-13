package rene.util.xml;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import rene.util.list.ListElement;
import rene.util.list.Tree;
import rene.util.parser.StringParser;

public class XmlTree extends Tree
	implements Enumeration<XmlTree>, Iterator<XmlTree>, Iterable<XmlTree>
{	public XmlTree (XmlTag t)
	{	super(t);
	}
	
	public XmlTag getTag ()
	{	return (XmlTag)content();
	}
	
	public XmlTree xmlFirstContent ()
	{	if (firstchild()!=null) return (XmlTree)firstchild();
		else return null;
	}
	
	public boolean isText ()
	{	if (!haschildren()) return true;
		if (firstchild()!=lastchild()) return false;
		XmlTree t=(XmlTree)firstchild();
		XmlTag tag=t.getTag();
		if (!(tag instanceof XmlTagText)) return false;
		return true;
	}
	
	public String getText ()
	{	if (!haschildren()) return "";
		XmlTree t=(XmlTree)firstchild();
		XmlTag tag=t.getTag();
		return ((XmlTagText)tag).getContent();
	}
	
	ListElement Current;
	
	public Enumeration<XmlTree> getContent ()
	{	Current=children().first();
		return this;
	}
	
	public boolean hasMoreElements ()
	{	return Current!=null;
	}
	
	public XmlTree nextElement ()
	{	if (Current==null) return null;
		XmlTree c=(XmlTree)(Current.content());
		Current=Current.next();
		return c;
	}
	
	public boolean isTag (String s)
	{	return getTag().name().equals(s);
	}
	
	public String parseComment ()
		throws XmlReaderException
	{	StringBuffer s=new StringBuffer();
		Enumeration e=getContent();
		while (e.hasMoreElements())
		{	XmlTree tree=(XmlTree)e.nextElement();
			XmlTag tag=tree.getTag();
			if (tag.name().equals("P"))
			{	if (!tree.haschildren()) s.append("\n");
				else
				{	XmlTree h=tree.xmlFirstContent();
					String k=((XmlTagText)h.getTag()).getContent();
					k=k.replace('\n',' ');
					StringParser p=new StringParser(k);
					Vector v=p.wraplines(1000);
					for (int i=0; i<v.size(); i++)
					{	s.append((String)v.elementAt(i));
						s.append("\n");
					}
				}
			}
			else if (tag instanceof XmlTagText)
			{	String k=((XmlTagText)tag).getContent();
				StringParser p=new StringParser(k);
				Vector v=p.wraplines(1000);
				for (int i=0; i<v.size(); i++)
				{	s.append((String)v.elementAt(i));
					s.append("\n");
				}			
			}
			else
				throw new XmlReaderException("<"+tag.name()+"> not proper here.");
		}
		return s.toString();
	}
	
	public boolean hasNext()
	{	return Current!=null;
	}
	
	public XmlTree next()
	{	if (Current==null) return null;
		XmlTree c=(XmlTree)(Current.content());
		Current=Current.next();
		return c;
	}
	
	public void remove()
	{
	}
	
	public Iterator iterator()
	{	Current=children().first();
		return this;
	}	
}
