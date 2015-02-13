package fr.xtof54.sgfsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

import rene.util.list.ListElement;
import rene.util.parser.StringParser;
import rene.util.xml.XmlReader;
import rene.util.xml.XmlReaderException;
import rene.util.xml.XmlTag;
import rene.util.xml.XmlTagPI;
import rene.util.xml.XmlTagText;
import rene.util.xml.XmlTree;
import rene.util.xml.XmlWriter;

/**
This is a class wich contains a TreeNode. It used to store complete
game trees.
@see jagoclient.board.TreeNode
*/

public class SGFTree
{	protected TreeNode History; // the game history

	/** initlialize with a specific Node */
	public SGFTree (Node n)
	{	History=new TreeNode(n);
		History.node().main(true);
	}
	
	/** return the top node of this game tree */
	public TreeNode top () { return History; }

	final int maxbuffer=4096;
	char[] Buffer=new char[maxbuffer]; // the buffer for reading of files
	int BufferN;
	BoardInterface GF;

	char readnext (BufferedReader in) throws IOException
	{	int c=readchar(in);
		while (c=='\n' || c=='\t' || c==' ')
		{	if (c==-1) throw new IOException();
			c=readchar(in);
		}
		return (char)c;
	}

	static int lastnl=0;
	
	char readchar (BufferedReader in) throws IOException
	{	int c;
		while (true)
		{	c=in.read();
			if (c==-1) throw new IOException();
			if (c==13)
			{	if (lastnl==10) lastnl=0;
				else
				{	lastnl=13; return '\n';
				}
			}
			else if (c==10)
			{	if (lastnl==13) lastnl=0;
				else
				{	lastnl=10; return '\n';
				}
			}
			else
			{	lastnl=0;
				return (char)c;
			}
		}
	}

	// read a node assuming that ; has been found
	// return the character, which did not fit into node properties,
	// usually ;, ( or )
	char readnode (TreeNode p, BufferedReader in) throws IOException
	{	boolean sgf=GF.getParameter("sgfcomments",false);
		char c=readnext(in);
		Action a;
		Node n=new Node(((Node)p.content()).number());
		String s;
		loop: while (true) // read all actions
		{	BufferN=0;
			while (true)
			{	if (c>='A' && c<='Z') store(c);
					// note only capital letters
				else if (c=='(' || c==';' || c==')') break loop;
					// last property reached
					// BufferN should be 0 then
				else if (c=='[') break;
					// end of porperty type, arguments starting
				else if (c<'a' || c>'z') throw new IOException();
					// this is an error
				c=readnext(in);
			}
			if (BufferN==0) throw new IOException();
			s=new String(Buffer,0,BufferN);
			if (s.equals("L")) a=new LabelAction(GF);
			else if (s.equals("M")) a=new MarkAction(GF);
			else a=new Action(s);
			while (c=='[')
			{	BufferN=0;
				while (true)
				{	c=readchar(in);
					if (c=='\\')
					{	c=readchar(in);
						if (sgf && c=='\n')
						{	if (BufferN>1 && Buffer[BufferN-1]==' ') continue;
							else c=' ';
						}
					}
					else if (c==']') break;
					store(c);
				}
				c=readnext(in); // prepare next argument
				String s1;
				if (BufferN>0) s1=new String(Buffer,0,BufferN);
				else s1="";
				if (!expand(a,s1)) a.addargument(s1);
			}
			// no more arguments
			n.addaction(a);
			if (a.type().equals("B") || a.type().equals("W"))
			{	n.number(n.number()+1);
			}
		} // end of actions has been found
		// append node
		n.main(p);
		TreeNode newp;
		if (((Node)p.content()).actions()==null)
			p.content(n);
		else
		{	p.addchild(newp=new TreeNode(n));
			n.main(p);
			p=newp;
			if (p.parentPos()!=null && p!=p.parentPos().firstChild())
				((Node)p.content()).number(2);
		}
		return c;
	}

	/**
	Store c into the Buffer extending its length, if necessary.
	This is a fix by Bogdar Creanga from 2000-10-17 (Many Thanks)
	*/
	private void store (char c)
	{	try
		{	Buffer[BufferN]=c;
			BufferN++;
		}
		catch (ArrayIndexOutOfBoundsException e)
		{	int newLength = Buffer.length + maxbuffer;
			char[] newBuffer = new char[newLength];
			System.arraycopy(Buffer,0,newBuffer,0,Buffer.length);
			Buffer = newBuffer;
			Buffer[BufferN++]=c;
		}
	}

	// Check for the terrible compressed point list and expand into
	// single points
	boolean expand (Action a, String s)
	{	String t=a.type();
		if (!(t.equals("MA") || t.equals("SQ") || t.equals("TR") ||
			 t.equals("CR") || t.equals("AW") || t.equals("AB") ||
			  t.equals("AE") || t.equals("SL"))) return false;
		if (s.length()!=5 || s.charAt(2)!=':') return false;
		String s0=s.substring(0,2),s1=s.substring(3);
		int i0=Field.i(s0),j0=Field.j(s0);
		int i1=Field.i(s1),j1=Field.j(s1);
		if (i1<i0 || j1<j0) return false;
		int i,j;
		for (i=i0; i<=i1; i++)
			for (j=j0; j<=j1; j++)
			{	a.addargument(Field.string(i,j));
			}
		return true;
	}

	/**
	Read the nodes belonging to a tree.
	this assumes that ( has been found.
	*/
	void readnodes (TreeNode p, BufferedReader in)
		throws IOException
	{	char c=readnext(in);
		while (true)
		{	if (c==';')
			{	c=readnode(p,in);
				if (p.haschildren()) p=p.lastChild();
				continue;
			}
			else if (c=='(')
			{	readnodes(p,in);
			}
			else if (c==')') break;
			c=readnext(in);
		}
	}
	
	/**
	Read the tree from an BufferedReader in SGF format.
	The BoardInterfaces is only used to determine the "sgfcomments" parameter.
	*/	
	public static Vector load (BufferedReader in, BoardInterface gf)
		throws IOException
	{	Vector v=new Vector();
		boolean linestart=true;
		int c;
		reading : while (true)
		{	SGFTree T=new SGFTree(new Node(1));
			while (true) // search for ( at line start
			{	try
				{	c=T.readchar(in);
				}
				catch (IOException ex)
				{	break reading;
				}
				if (linestart && c=='(') break;
				if (c=='\n') linestart=true;
				else linestart=false;
			}
			T.GF=gf;
			T.readnodes(T.History,in); // read the nodes
			v.addElement(T);
		}
		return v;
	}

	/*
	XML Reader Stuff
	*/
	
	// Assumption on Boardsize of xml file, if <BoardSize> is not found
	static int BoardSize=19;	
	static String GameName="";
	
	/**
	Read all games from a tree.
	@return Vector of trees.
	*/
	static Vector readnodes (XmlTree tree, BoardInterface gf)
		throws XmlReaderException
	{	Vector v=new Vector();
		Enumeration root=tree.getContent();
		while (root.hasMoreElements())
		{	tree=(XmlTree)root.nextElement();	
			XmlTag tag=tree.getTag();
			if (tag instanceof XmlTagPI) continue;
			testTag(tag,"Go");
			Enumeration trees=tree.getContent();
			while (trees.hasMoreElements())
			{	tree=(XmlTree)trees.nextElement();
				tag=tree.getTag();
				testTag(tag,"GoGame");
				if (tag.hasParam("name"))
				{	GameName=tag.getValue("name");
				}
				Enumeration e=tree.getContent();
				if (!e.hasMoreElements()) xmlMissing("Information");
				XmlTree information=(XmlTree)e.nextElement();
				testTag(information.getTag(),"Information");
				getBoardSize(information);
				SGFTree t=new SGFTree(new Node(1));
				t.GF=gf;
				TreeNode p=t.readnodes(e,null,tree,true,1);
				if (p!=null) setInformation(p,information);
				t.History=p;
				if (p!=null) v.addElement(t);
			}
		}
		return v;
	}
	
	public static void setInformation (TreeNode p, XmlTree information)
		throws XmlReaderException
	{	Enumeration e=information.getContent();
		while (e.hasMoreElements())
		{	XmlTree tree=(XmlTree)e.nextElement();
			XmlTag tag=tree.getTag();
			if (tag.name().equals("BoardSize"))
			{	p.addaction(new Action("SZ",""+BoardSize));
			}
			else if (tag.name().equals("BlackPlayer"))
			{	p.addaction(new Action("PB",getText(tree)));
			}
			else if (tag.name().equals("BlackRank"))
			{	p.addaction(new Action("BR",getText(tree)));
			}
			else if (tag.name().equals("WhitePlayer"))
			{	p.addaction(new Action("PW",getText(tree)));
			}
			else if (tag.name().equals("WhiteRank"))
			{	p.addaction(new Action("WR",getText(tree)));
			}
			else if (tag.name().equals("Date"))
			{	p.addaction(new Action("DT",getText(tree)));
			}
			else if (tag.name().equals("Time"))
			{	p.addaction(new Action("TM",getText(tree)));
			}
			else if (tag.name().equals("Komi"))
			{	p.addaction(new Action("KM",getText(tree)));
			}
			else if (tag.name().equals("Result"))
			{	p.addaction(new Action("RE",getText(tree)));
			}
			else if (tag.name().equals("Handicap"))
			{	p.addaction(new Action("HA",getText(tree)));
			}
			else if (tag.name().equals("User"))
			{	p.addaction(new Action("US",getText(tree)));
			}
			else if (tag.name().equals("Copyright"))
			{	p.addaction(new Action("CP",parseComment(tree)));
			}
		}
		if (!GameName.equals(""))
			p.addaction(new Action("GN",GameName));
	}
	
	public static String getText (XmlTree tree)
		throws XmlReaderException
	{	Enumeration e=tree.getContent();
		if (!e.hasMoreElements()) return "";
		XmlTree t=(XmlTree)e.nextElement();
		XmlTag tag=t.getTag();
		if (!(tag instanceof XmlTagText) || e.hasMoreElements())
			throw new XmlReaderException(
				"<"+tree.getTag().name()+"> has wrong content.");
		return ((XmlTagText)tag).getContent();
	}
	
	public static void getBoardSize (XmlTree tree)
		throws XmlReaderException
	{	Enumeration e=tree.getContent();
		BoardSize=19;
		while (e.hasMoreElements())
		{	tree=(XmlTree)e.nextElement();
			if (tree.getTag().name().equals("BoardSize"))
			{	tree=tree.xmlFirstContent();
				XmlTag tag=tree.getTag();
				if (tag instanceof XmlTagText)
				{	try
					{	BoardSize=Integer.parseInt(
							((XmlTagText)tag).getContent());
					}
					catch (Exception ex)
					{	throw new XmlReaderException(
							"Illegal <BoardSize>");
					}
				}
				else
					throw new XmlReaderException(
							"Illegal <BoardSize>");
				break;
			}
		}
	}
	
	TreeNode readnodes (Enumeration e, TreeNode p, XmlTree father, boolean main,
		int number)
		throws XmlReaderException
	{	TreeNode ret=null;
		while (e.hasMoreElements())
		{	XmlTree tree=(XmlTree)e.nextElement();
			XmlTag tag=tree.getTag();
			if (tag.name().equals("Nodes"))
			{	return readnodes(tree.getContent(),p,father,main,number);
			}
			else if (tag.name().equals("Node"))
			{	if (p!=null) number=((Node)p.content()).number();
				Node n=readnode(number,tree);
				n.main(main);
				TreeNode newp=new TreeNode(n);
				if (p==null) ret=newp;
				if (p!=null) p.addchild(newp);
				p=newp;
			}
			else if (tag.name().equals("White"))
			{	if (p!=null) number=((Node)p.content()).number();
				Node n=new Node(number);
				try
				{	n.addaction(new Action("W",xmlToSgf(tree)));
					n.number(n.number()+1);
					n.main(main);
				}
				catch (XmlReaderException ey) { n.addaction(new Action("C","Pass")); }
				if (tag.hasParam("name"))
				{	n.addaction(new Action("N",tag.getValue("name")));
				}
				if (tag.hasParam("timeleft"))
				{	n.addaction(new Action("WL",tag.getValue("timeleft")));
				}
				TreeNode newp=new TreeNode(n);
				if (p==null) ret=newp;
				if (p!=null) p.addchild(newp);
				p=newp;
			}
			else if (tag.name().equals("Black"))
			{	if (p!=null) number=((Node)p.content()).number();
				Node n=new Node(number);
				try
				{	n.addaction(new Action("B",xmlToSgf(tree)));
					n.number(n.number()+1);
					n.main(main);
				}
				catch (XmlReaderException ey) { n.addaction(new Action("C","Pass")); }
				if (tag.hasParam("name"))
				{	n.addaction(new Action("N",tag.getValue("name")));
				}
				if (tag.hasParam("timeleft"))
				{	n.addaction(new Action("BL",tag.getValue("timeleft")));
				}
				TreeNode newp=new TreeNode(n);
				if (p==null) ret=newp;
				if (p!=null) p.addchild(newp);
				p=newp;
			}
			else if (tag.name().equals("Comment"))
			{	if (p==null)
				{	Node n=new Node(number);
					n.main(main);
					p=new TreeNode(n);
					ret=p;					
				}
				Node n=(Node)p.content();
				n.addaction(new Action("C",parseComment(tree)));
			}
			else if (tag.name().equals("Variation"))
			{	TreeNode parent=(TreeNode)p.parent();
				if (parent==null)
					throw new XmlReaderException("Root node cannot have variation");
				TreeNode newp=readnodes(tree.getContent(),null,tree,false,1);
				parent.addchild(newp);
			}
			else
			{	throw new XmlReaderException(
					"Illegal Node or Variation <"+tag.name()+">");
			}
		}
		return ret;
	}
	
	public Node readnode (int number, XmlTree tree)
		throws XmlReaderException
	{	Node n=new Node(number);
		XmlTag tag=tree.getTag();
		if (tag.hasParam("name"))
		{	n.addaction(new Action("N",tag.getValue("name")));
		}
		if (tag.hasParam("blacktime"))
		{	n.addaction(new Action("BL",tag.getValue("blacktime")));
		}
		if (tag.hasParam("whitetime"))
		{	n.addaction(new Action("WL",tag.getValue("whitetime")));
		}
		Enumeration e=tree.getContent();
		while (e.hasMoreElements())
		{	XmlTree t=(XmlTree)e.nextElement();
			tag=t.getTag();
			if (tag.name().equals("Black"))
			{	try
				{	n.addaction(new Action("B",xmlToSgf(t)));
					n.number(n.number()+1);
				}
				catch (XmlReaderException ey) {}
			}
			else if (tag.name().equals("White"))
			{	try
				{	n.addaction(new Action("W",xmlToSgf(t)));
					n.number(n.number()+1);
				}
				catch (XmlReaderException ey) {}
			}
			else if (tag.name().equals("AddBlack"))
			{	n.addaction(new Action("AB",xmlToSgf(t)));
			}
			else if (tag.name().equals("AddWhite"))
			{	n.addaction(new Action("AW",xmlToSgf(t)));
			}
			else if (tag.name().equals("Delete"))
			{	n.expandaction(new Action("AE",xmlToSgf(t)));
			}
			else if (tag.name().equals("Mark"))
			{	if (tag.hasParam("type"))
				{	String s=tag.getValue("type");
					if (s.equals("triangle"))
					{	n.expandaction(new Action("TR",xmlToSgf(t)));
					}
					else if (s.equals("square"))
					{	n.expandaction(new Action("SQ",xmlToSgf(t)));
					}
					else if (s.equals("circle"))
					{	n.expandaction(new Action("CR",xmlToSgf(t)));
					}
				}
				else if (tag.hasParam("label"))
				{	String s=tag.getValue("label");
					n.expandaction(new Action("LB",
						xmlToSgf(t)+":"+s));
				}
				else if (tag.hasParam("territory"))
				{	String s=tag.getValue("territory");
					if (s.equals("white"))
					{	n.expandaction(new Action("TW",xmlToSgf(t)));
					}
					else if (s.equals("black"))
					{	n.expandaction(new Action("TB",xmlToSgf(t)));
					}
				}
				else n.expandaction(new MarkAction(xmlToSgf(t),GF));
			}
			else if (tag.name().equals("BlackTimeLeft"))
			{	n.addaction(new Action("BL",getText(t)));
			}
			else if (tag.name().equals("WhiteTimeLeft"))
			{	n.addaction(new Action("WL",getText(t)));
			}
			else if (tag.name().equals("Comment"))
			{	n.addaction(new Action("C",parseComment(t)));
			}
			else if (tag.name().equals("SGF"))
			{	if (!tag.hasParam("type"))
					throw new XmlReaderException("Illegal <SGF> tag.");
				Action a;
				if (tag.getValue("type").equals("M")) a=new MarkAction(GF);
				else a=new Action(tag.getValue("type"));
				Enumeration eh=t.getContent();
				while (eh.hasMoreElements())
				{	XmlTree th=(XmlTree)eh.nextElement();
					XmlTag tagh=th.getTag();
					if (!tagh.name().equals("Arg"))
						throw new XmlReaderException("Illegal <SGF> tag.");
					if (!th.isText())
						throw new XmlReaderException("Illegal <SGF> tag.");
					else a.addargument(th.getText());
				}
				n.addaction(a);
			}
		}
		return n;
	}
	
	public static String parseComment (XmlTree t)
		throws XmlReaderException
	{	StringBuffer s=new StringBuffer();
		Enumeration e=t.getContent();
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
					k=k.replace('\n',' ');
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
	
	public String xmlToSgf (XmlTree tree)
		throws XmlReaderException
	{	XmlTag tag=tree.getTag();
		if (tag.hasParam("at"))
		{	return xmlToSgf(tag.getValue("at"));
		}
		Enumeration e=tree.getContent();
		if (!e.hasMoreElements())
			throw new XmlReaderException("Missing board position.");
		tag=((XmlTree)e.nextElement()).getTag();
		if (tag instanceof XmlTagText)
		{	String pos=((XmlTagText)tag).getContent();
			return xmlToSgf(pos);
		}
		else if (tag.name().equals("at"))
		{	String pos=((XmlTagText)tag).getContent();
			return xmlToSgf(pos);
		}
		else
			throw new XmlReaderException(tag.name()
				+" contains wrong board position.");
	}
	
	public String xmlToSgf (String pos)
		throws XmlReaderException
	{	if (pos.length()<2) wrongBoardPosition(pos);
		int n=pos.indexOf(",");
		if (n>0 && n<pos.length())
		{	String s1=pos.substring(0,n),s2=pos.substring(n+1);
			try
			{	int i=Integer.parseInt(s1)-1;
				int j=Integer.parseInt(s2);
				j=BoardSize-j;
				if (i<0 || i>=BoardSize || j<0 || j>=BoardSize)
					wrongBoardPosition(pos);
				return Field.string(i,j);
			}
			catch (Exception ex)
			{	wrongBoardPosition(pos);
			}
		}
		char c=Character.toUpperCase(pos.charAt(0));
		if (c>='J') c--;
		int i=c-'A';
		int j=0;
		try
		{	j=Integer.parseInt(pos.substring(1));
		}
		catch (Exception ex)
		{	wrongBoardPosition(pos);
		}
		j=BoardSize-j;
		if (i<0 || i>=BoardSize || j<0 || j>=BoardSize)
			wrongBoardPosition(pos);
		return Field.string(i,j);
	}
	
	public void wrongBoardPosition (String s)
		throws XmlReaderException
	{	throw new XmlReaderException("Wrong Board Position "+s);
	}
	
	public static void xmlMissing (String s)
		throws XmlReaderException
	{	throw new XmlReaderException("Missing <"+s+">");
	}
	
	public static void testTag (XmlTag tag, String name)
		throws XmlReaderException
	{	if (!tag.name().equals(name))
		{	throw new XmlReaderException(
				"<"+name+"> expected instead of <"+tag.name()+">");
		}
	}

	/**
	Read a number of trees from an XML file.
	*/
	public static Vector load (XmlReader xml, BoardInterface gf)
		throws XmlReaderException
	{	XmlTree t=xml.scan();
		if (t==null) throw new XmlReaderException("Illegal file format");
		Vector v=readnodes(t,gf);
		return v;
	}

	/**
	Print the tree to the specified PrintWriter.
	@param p the subtree to be printed
	*/
	void printtree (TreeNode p, PrintWriter o)
	{	o.println("(");
	    while (true)
		{	p.node().print(o);
			if (!p.haschildren()) break;
			if (p.lastChild()!=p.firstChild())
			{	ListElement e=p.children().first();
				while (e!=null)
				{	printtree((TreeNode)e.content(),o);
					e=e.next();
				}
				break;
			}
			p=p.firstChild();
		}
		o.println(")");
	}

	/**
	Print the tree to the specified PrintWriter.
	@param p the subtree to be printed
	*/
	void printtree (TreeNode p, XmlWriter xml, int size, boolean top)
	{	if (top)
		{	String s=p.getaction("GN");
			if (s!=null && !s.equals(""))
				xml.startTagNewLine("GoGame","name",s);
			else
				xml.startTagNewLine("GoGame");
			xml.startTagNewLine("Information");
			printInformation(xml,p,"AP","Application");
			printInformation(xml,p,"SZ","BoardSize");
			printInformation(xml,p,"PB","BlackPlayer");
			printInformation(xml,p,"BR","BlackRank");
			printInformation(xml,p,"PW","WhitePlayer");
			printInformation(xml,p,"WR","WhiteRank");
			printInformation(xml,p,"DT","Date");
			printInformation(xml,p,"TM","Time");
			printInformation(xml,p,"KM","Komi");
			printInformation(xml,p,"RE","Result");
			printInformation(xml,p,"HA","Handicap");
			printInformation(xml,p,"US","User");
			printInformationText(xml,p,"CP","Copyright");
			xml.endTagNewLine("Information");
		}
		else xml.startTagNewLine("Variation");
		if (top) xml.startTagNewLine("Nodes");
		while (true)
		{	p.node().print(xml,size);
			if (!p.haschildren()) break;
			if (p.lastChild()!=p.firstChild())
			{	ListElement e=p.children().first();
				p=p.firstChild();
				p.node().print(xml,size);
				e=e.next();
				while (e!=null)
				{	printtree((TreeNode)e.content(),xml,size,false);
					e=e.next();
				}
				if (!p.haschildren()) break;
			}
			p=p.firstChild();
		}
		if (top) xml.endTagNewLine("Nodes");
		if (top) xml.endTagNewLine("GoGame");
		else xml.endTagNewLine("Variation");
	}

	public void printInformation (XmlWriter xml, TreeNode p,
		String tag, String xmltag)
	{	String s=p.getaction(tag);
		if (s!=null && !s.equals(""))
			xml.printTagNewLine(xmltag,s);
	}

	public void printInformationText (XmlWriter xml, TreeNode p,
		String tag, String xmltag)
	{	String s=p.getaction(tag);
		if (s!=null && !s.equals(""))
		{	xml.startTagNewLine(xmltag);
			xml.printParagraphs(s,60);
			xml.endTagNewLine(xmltag);
		}
	}

	/**
	Print this tree to the PrintWriter starting at the root node.
	*/
	public void print (PrintWriter o)
	{	printtree(History,o);
	}
	
	public void printXML (XmlWriter xml)
	{	printtree(History,xml,getSize(),true);
	}
	
	public int getSize ()
	{	try
		{	return Integer.parseInt(History.getaction("SZ"));
		}
		catch (Exception e)
		{	return 19;
		}
	}
}
