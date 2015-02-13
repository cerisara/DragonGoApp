package rene.util.xml;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import rene.util.SimpleByteBuffer;
import rene.util.SimpleStringBuffer;
import rene.util.list.ListElement;

public class XmlReader
{	BufferedReader In;
	SimpleStringBuffer buf=new SimpleStringBuffer(10000);

	public XmlReader ()
	{	In=null;
	}

	public XmlReader (BufferedReader in)
	{	In=in;
	}

	public XmlReader (InputStream in)
		throws XmlReaderException
	{	try
		{	// read the file into a buffer
			BufferedInputStream rin=new BufferedInputStream(in);
			SimpleByteBuffer bb=new SimpleByteBuffer(10000);
			while (true)
			{	int k=rin.read();
				if (k<0) break;
				bb.append((byte)k);
			}
			rin.close();
			byte b[]=bb.getByteArray();
			
			// Try to open an ASCII stream, or a default stream
			ByteArrayInputStream bin=new ByteArrayInputStream(b);
			XmlReader R=null;
			try
			{	R=new XmlReader(new BufferedReader(new InputStreamReader(bin,"ASCII")));
			}
			catch (UnsupportedEncodingException ex)
			{	R=new XmlReader(new BufferedReader(new InputStreamReader(bin)));
			}		
						
			// Determine the encoding
			String Encoding=null;
			while (true)
			{	while (true)
				{	int c=R.read();
					if (c==-1) throw new Exception("<?xml> tag not found");
					if (c=='<') break;
				}
				if (R.found("?xml"))
				{	String s=R.scanFor("?>");
					if (s==null) throw new Exception("<?xml> tag error");
					int n=s.indexOf("encoding=\"");
					if (n>=0)
					{	n+="encoding=\"".length();
						s=s.substring(n);
						int m=s.indexOf('\"');
						if (m<0) throw new Exception("Closing bracket missing");
						Encoding=s.substring(0,m).toUpperCase();
						if (Encoding.equals("UTF-8")) Encoding="UTF8";
							// for IE5 !
						break;
					}
					break;
				}
			}
			
			// Open a stream with this encoding
			bin=new ByteArrayInputStream(b);
			if (Encoding==null)
				In=new BufferedReader(new InputStreamReader(bin));
			else
				try 
				{	In=new BufferedReader(new InputStreamReader(
						bin,Encoding));
				}
				catch (UnsupportedEncodingException e)
				{	try
					{	In=new BufferedReader(new InputStreamReader(
							bin,"ASCII"));
					}
					catch (UnsupportedEncodingException ex)
					{	In=new BufferedReader(new InputStreamReader(bin));
					}					
				}
		}
		catch (Exception e)
		{	throw new XmlReaderException(e.toString());
		}
	}

	public void init (InputStream in)
		throws XmlReaderException
	{	try
		{	// read the file into a buffer
			BufferedInputStream rin=new BufferedInputStream(in);
			SimpleByteBuffer bb=new SimpleByteBuffer(10000);
			while (true)
			{	int k=rin.read();
				if (k<0) break;
				bb.append((byte)k);
			}
			rin.close();
			byte b[]=bb.getByteArray();
			
			// Try to open an ASCII stream, or a default stream
			ByteArrayInputStream bin=new ByteArrayInputStream(b);
			XmlReader R=null;
			try
			{	R=new XmlReader(new BufferedReader(new InputStreamReader(bin,"ASCII")));
			}
			catch (UnsupportedEncodingException ex)
			{	R=new XmlReader(new BufferedReader(new InputStreamReader(bin)));
			}		
						
			// Determine the encoding
			String Encoding=null;
			while (true)
			{	while (true)
				{	int c=R.read();
					if (c==-1) throw new Exception("<?xml> tag not found");
					if (c=='<') break;
				}
				if (R.found("?xml"))
				{	String s=R.scanFor("?>");
					if (s==null) throw new Exception("<?xml> tag error");
					int n=s.indexOf("encoding=\"");
					if (n>=0)
					{	n+="encoding=\"".length();
						s=s.substring(n);
						int m=s.indexOf('\"');
						if (m<0) throw new Exception("Closing bracket missing");
						Encoding=s.substring(0,m).toUpperCase();
						if (Encoding.equals("UTF-8")) Encoding="UTF8";
							// for IE5 !
						break;
					}
					break;
				}
			}
			
			// Open a stream with this encoding
			bin=new ByteArrayInputStream(b);
			if (Encoding==null)
				In=new BufferedReader(new InputStreamReader(bin));
			else
				try 
				{	In=new BufferedReader(new InputStreamReader(
						bin,Encoding));
				}
				catch (UnsupportedEncodingException e)
				{	try
					{	In=new BufferedReader(new InputStreamReader(
							bin,"ASCII"));
					}
					catch (UnsupportedEncodingException ex)
					{	In=new BufferedReader(new InputStreamReader(bin));
					}					
				}
		}
		catch (Exception e)
		{	throw new XmlReaderException(e.toString());
		}
	}

	/**
	Scan an xml file. This function reads, until <?xml is found.
	then it skips this declaration and scans the rest of the
	items.
	*/
	public XmlTree scan ()
		throws XmlReaderException
	{	while (true)
		{	while (true)
			{	int c=read();
				if (c==-1) return null;
				if (c=='<') break;
			}
			if (found("?xml"))
			{	String s=scanFor("?>");
				if (s==null) return null;
				XmlTree t=new XmlTree(new XmlTagRoot());
				t.addchild(new XmlTree(new XmlTagPI(s)));
				scanContent(t);
				return t;
			}
		}
	}
	
	public void scanContent (XmlTree t)
		throws XmlReaderException
	{	//System.out.println("Sanning for "+t.getTag().name()+" ("+
		//	t.getTag().countParams()+")");
		while (true)
		{	String s=scanFor('<');
			if (s==null)
			{	if (t.getTag() instanceof XmlTagRoot) return;
				exception("File ended surprisingly");
			}
			if (!empty(s))
			{	t.addchild(new XmlTree(new XmlTagText(
					XmlTranslator.toText(s))));
			}
			if (found("!--"))
			{	s=scanFor("-->");
				continue;
			}
			if (found("!"))
			{	s=scanTagFor('>');
				continue;
			}
			if (found("?"))
			{	s=scanTagFor("?>");
				t.addchild(new XmlTree(new XmlTagPI(s)));
				continue;
			}
			s=scanTagFor('>');
			if (s==null)
				exception("> missing");
			if (s.startsWith("/"))
			{	if (s.substring(1).equals(t.getTag().name()))
					return;
				else 
					exception("End tag without start tag");
			}
			if (s.endsWith("/"))
			{	t.addchild(new XmlTree(new XmlTag(s.substring(0,s.length()-1))));
			}
			else
			{	XmlTree t0=new XmlTree(new XmlTag(s));
				scanContent(t0);
				t.addchild(t0);
			}
		}
	}

	public boolean empty (String s)
	{	int n=s.length();
		for (int i=0; i<n; i++)
		{	char c=s.charAt(i);
			if (c!=' ' && c!='\n' && c!='\t') return false;
		}
		return true;
	}

	/**
	Skip Blanks.
	@return Non-blank character or -1 for EOF.
	*/
	public int skipBlanks ()
		throws XmlReaderException
	{	while (true)
		{	int c=read();
			if (c==' ' || c=='\t' || c=='\n') continue;
			else return c;
		}
	}

	/**
	Scan for an end character.
	@return String between the current position and the end character, or null.
	*/
	public String scanFor (char end)
		throws XmlReaderException
	{	buf.clear();
		int c=read();
		while (c!=end)
		{	buf.append((char)c);
			c=read();
			if (c<0) return null;
		}
		return buf.toString();
	}
	
	/**
	Scan for a specific string.
	@return String between the current position and the string.
	*/
	public String scanFor (String s)
		throws XmlReaderException
	{	buf.clear();
		while (!found(s))
		{	int c=read();
			if (c<0) return null;
			buf.append((char)c);
		}
		for (int i=0; i<s.length(); i++) read();
		return buf.toString();
	}
	
	/**
	Scan tag for an end character (interpreting " and ')
	@return String between the current position and the end character, or null.
	*/
	public String scanTagFor (char end)
		throws XmlReaderException
	{	buf.clear();
		int c=read();
		while (c!=end)
		{	if (c=='\"')
			{	buf.append((char)c);
				while (true)
				{	c=read();
					if (c<0) return null;
					if (c=='\"') break;
					buf.append((char)c);
				}
				buf.append((char)c);
			}
			else if (c=='\'')
			{	buf.append((char)c);
				while (true)
				{	c=read();
					if (c<0) return null;
					if (c=='\'') break;
					buf.append((char)c);
				}
				buf.append((char)c);
			}
			else buf.append((char)c);
			c=read();
			if (c<0) return null;
		}
		return buf.toString();
	}
	
	/**
	Scan tag for a specific string (interpreting " and ')
	@return String between the current position and the string.
	*/
	public String scanTagFor (String s)
		throws XmlReaderException
	{	buf.clear();
		while (!found(s))
		{	int c=read();
			if (c<0) return null;
			if (c=='\"')
			{	buf.append((char)c);
				while (true)
				{	c=read();
					if (c<0) return null;
					if (c=='\"') break;
					buf.append((char)c);
				}
				buf.append((char)c);
			}
			else if (c=='\'')
			{	buf.append((char)c);
				while (true)
				{	c=read();
					if (c<0) return null;
					if (c=='\'') break;
					buf.append((char)c);
				}
				buf.append((char)c);
			}
			else buf.append((char)c);
		}
		for (int i=0; i<s.length(); i++) read();
		return buf.toString();
	}
	
	String Line=null;
	int LinePos;
	
	public int read ()
		throws XmlReaderException
	{	try
		{	if (Line==null)
			{	Line=In.readLine();
				LinePos=0;
				// System.out.println("Read --> "+Line);
			}
			if (Line==null) return -1;
			if (LinePos>=Line.length())
			{	Line=null;
				return '\n';
			}
			return Line.charAt(LinePos++);
		}
		catch (Exception e)
		{	return -1;
		}
	}

	/**
	@return If the string is at the current line position.
	*/
	public boolean found (String s)
	{	int n=s.length();
		if (LinePos+n>Line.length()) return false;
		for (int i=0; i<n; i++)
			if (s.charAt(i)!=Line.charAt(LinePos+i)) return false;
		return true;
	}
	
	public void exception (String s)
		throws XmlReaderException
	{	throw new XmlReaderException(s,Line,LinePos);
	}
	
	/**
	A test program.
	*/
	public static void main (String args[])
	{	try
		{	BufferedReader in=new BufferedReader(
				new InputStreamReader(
				new FileInputStream("rene\\util\\xml\\test.xml"),"UTF8"));
			XmlReader reader=new XmlReader(in);
			XmlTree tree=reader.scan();
			in.close();
			print(tree);
		}
		catch (XmlReaderException e)
		{	System.out.println(e.toString()+"\n"+
				e.getLine()+"\n"+"Position : "+e.getPos());
		}
		catch (IOException e)
		{	System.out.println(e);
		}
	}
	
	public static void print (XmlTree t)
	{	XmlTag tag=t.getTag();
		System.out.print("<"+tag.name());
		for (int i=0; i<tag.countParams(); i++)
		{	System.out.print(" "+tag.getParam(i)+"=\""+tag.getValue(i)+"\"");
		}
		System.out.println(">");
		ListElement el=t.children().first();
		while (el!=null)
		{	print((XmlTree)(el.content()));
			el=el.next();
		}
		System.out.println("</"+tag.name()+">");
	}
	
	public static boolean testXml (String s)
	{	int i=0;
		while (i<s.length())
		{	char c=s.charAt(i);
			if (c=='<') break;
			i++;
		}
		if (i>=s.length()) return false;
		if (s.substring(i).startsWith("<?xml")) return true;
		return false;
	}
}
