package fr.xtof54.sgfsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Vector;

import rene.util.list.ListElement;
import rene.util.xml.XmlReader;
import rene.util.xml.XmlReaderException;
import rene.util.xml.XmlWriter;

//******************* Board ***********************

/**
 * This is the main file for presenting a Go board.
 * <P>
 * Handles the complete display and storage of a Go board. The display is kept
 * on an offscreen image. Stores a Go game in a node list (with variants).
 * Handles the display of the current node.
 * <P>
 * This class handles mouse input to set the next move. It also has methods to
 * move in the node tree from external sources.
 * <P>
 * A BoardInterface is used to encorporate the board into an environment.
 */

public class Board 
{
	int O, W, D, S, OT, OTU, OP; // pixel coordinates
	// O=offset, W=total width, D=field width, S=board size (9,11,13,19)
	// OT=offset for coordinates to the right and below
	// OTU=offset above the board and below for coordinates
	int lasti = -1, lastj = 0; // last move (used to highlight the move)
	boolean showlast; // internal flag, if last move is to be highlighted
	// offscreen images of empty and current board
	SGFTree T; // the game tree
	Vector Trees; // the game trees (one of them is T)
	int CurrentTree; // the currently displayed tree
	Position P; // current board position
	int number; // number of the next move
	TreeNode Pos; // the current board position in this presentation
	int State; // states: 1 is black, 2 is white, 3 is set black etc.
	// see GoFrame.setState(int)
	BoardInterface GF; // frame containing the board
	boolean Active;
	int MainColor = 1;
	public int MyColor = 0;
	int sendi = -1, sendj;
	// board position which has been sended to server
	int SpecialMarker = Field.SQUARE;
	String TextMarker = "A";
	public int Pw, Pb; // Prisoners (white and black)
	BufferedReader LaterLoad = null; // File to be loaded at repaint
	int Range = -1; // Numbers display from this one
	boolean KeepRange = false;
	String NodeName = "", LText = "";
	boolean DisplayNodeName = false;
	public boolean Removing = false;
	boolean Activated = false;
	public boolean Teaching = false; // enable teaching mode
	boolean VCurrent = false; // show variations to current move
	boolean VHide = false; // hide variation markers

	// ******************** initialize board *******************

	public Board (int size, BoardInterface gf)
	{
		S = size;
		D = 16;
		W = S * D;
		showlast = true;
		GF = gf;
		State = 1;
		P = new Position(S);
		number = 1;
		T = new SGFTree(new Node(number));
		Trees = new Vector();
		Trees.addElement(T);
		CurrentTree = 0;
		Pos = T.top();
		Active = true;
		Pw = Pb = 0;
		VHide = GF.getParameter("vhide", false);
		VCurrent = GF.getParameter("vcurrent", true);
	}

	// set things on the board

	void gotovariation (int i, int j)
	// goto the variation at (i,j)
	{
		TreeNode newpos = P.tree(i, j);
		getinformation();
		if (VCurrent && newpos.parentPos() == Pos.parentPos())
		{
			goback();
			Pos = newpos;
			setnode();
			setlast();
		}
		else if ( !VCurrent && newpos.parentPos() == Pos)
		{
			Pos = newpos;
			setnode();
			setlast();
		}
		copy();
		showinformation();
	}

	public void set (int i, int j)
	// set a new move, if the board position is empty
	{
		Action a;
		synchronized (Pos)
		{
			if (P.color(i, j) == 0) // empty?
			{
				if (Pos.node().actions() != null || Pos.parentPos() == null)
				// create a new node, if there the current node is not
				// empty, else use the current node. exception is the first
				// move, where we always create a new move.
				{
					Node n = new Node(++number);
					if (P.color() > 0)
					{
						a = new Action("B", Field.string(i, j));
					}
					else
					{
						a = new Action("W", Field.string(i, j));
					}
					n.addaction(a); // note the move action
					setaction(n, a, P.color()); // display the action
					// !!! We alow for suicide moves
					TreeNode newpos = new TreeNode(n);
					Pos.addchild(newpos); // note the move
					n.main(Pos);
					Pos = newpos; // update current position pointer
				}
				else
				{
					Node n = Pos.node();
					if (P.color() > 0)
					{
						a = new Action("B", Field.string(i, j));
					}
					else
					{
						a = new Action("W", Field.string(i, j));
					}
					n.addaction(a); // note the move action
					setaction(n, a, P.color()); // display the action
					// !!! We alow for suicide moves
				}
			}
		}
	}

	public void delete (int i, int j)
	// delete the stone and note it
	{
		if (P.color(i, j) == 0) return;
		synchronized (Pos)
		{
			Node n = Pos.node();
			if (GF.getParameter("puresgf", true)
				&& (n.contains("B") || n.contains("W"))) n = newnode();
			String field = Field.string(i, j);
			if (n.contains("AB", field))
			{
				undonode();
				n.toggleaction(new Action("AB", field));
				setnode();
			}
			else if (n.contains("AW", field))
			{
				undonode();
				n.toggleaction(new Action("AW", field));
				setnode();
			}
			else if (n.contains("B", field))
			{
				undonode();
				n.toggleaction(new Action("B", field));
				setnode();
			}
			else if (n.contains("W", field))
			{
				undonode();
				n.toggleaction(new Action("W", field));
				setnode();
			}
			else
			{
				Action a = new Action("AE", field);
				n.expandaction(a);
				n.addchange(new Change(i, j, P.color(i, j)));
				P.color(i, j, 0);
				update(i, j);
			}
			showinformation();
			copy();
		}
	}

	public void changemove (int i, int j)
	// change a move to a new field (dangerous!)
	{
		if (P.color(i, j) != 0) return;
		synchronized (Pos)
		{
			ListElement la = Pos.node().actions();
			while (la != null)
			{
				Action a = (Action)la.content();
				if (a.type().equals("B") || a.type().equals("W"))
				{
					undonode();
					a.arguments().content(Field.string(i, j));
					setnode();
					break;
				}
			}
		}
	}

	public void removegroup (int i0, int j0)
	// completely remove a group (at end of game, before count)
	// note all removals
	{
		if (Pos.haschildren()) return;
		if (P.color(i0, j0) == 0) return;
		Action a;
		P.markgroup(i0, j0);
		int i, j;
		int c = P.color(i0, j0);
		Node n = Pos.node();
		if (n.contains("B") || n.contains("W")) n = newnode();
		for (i = 0; i < S; i++)
			for (j = 0; j < S; j++)
			{
				if (P.marked(i, j))
				{
					a = new Action("AE", Field.string(i, j));
					n
						.addchange(new Change(i, j, P.color(i, j), P.number(i,
							j)));
					n.expandaction(a);
					if (P.color(i, j) > 0)
					{
						n.Pb++;
						Pb++;
					}
					else
					{
						n.Pw++;
						Pw++;
					}
					P.color(i, j, 0);
					update(i, j);
				}
			}
		copy();
	}

	public void mark (int i, int j)
	// Emphasize the field at i,j
	{
		Node n = Pos.node();
		Action a = new MarkAction(Field.string(i, j), GF);
		n.toggleaction(a);
		update(i, j);
	}

	public void specialmark (int i, int j)
	// Emphasize with the SpecialMarker
	{
		Node n = Pos.node();
		String s;
		switch (SpecialMarker)
		{
			case Field.SQUARE:
				s = "SQ";
				break;
			case Field.CIRCLE:
				s = "CR";
				break;
			case Field.TRIANGLE:
				s = "TR";
				break;
			default:
				s = "MA";
				break;
		}
		Action a = new Action(s, Field.string(i, j));
		n.toggleaction(a);
		update(i, j);
	}

	public void markterritory (int i, int j, int color)
	{
		Action a;
		if (color > 0)
			a = new Action("TB", Field.string(i, j));
		else a = new Action("TW", Field.string(i, j));
		Pos.node().expandaction(a);
		update(i, j);
	}

	public void textmark (int i, int j)
	{
		Action a = new Action("LB", Field.string(i, j) + ":" + TextMarker);
		Pos.node().expandaction(a);
		update(i, j);
		GF.advanceTextmark();
	}

	public void letter (int i, int j)
	// Write a character to the field at i,j
	{
		Action a = new LabelAction(Field.string(i, j), GF);
		Pos.node().toggleaction(a);
		update(i, j);
	}

	public Node newnode ()
	{
		Node n = new Node(++number);
		TreeNode newpos = new TreeNode(n);
		Pos.addchild(newpos); // note the move
		n.main(Pos);
		Pos = newpos; // update current position pointerAction a;
		setlast();
		return n;
	}

	public void set (int i, int j, int c)
	// set a new stone, if the board position is empty
	// and we are on the last node.
	{
		if (Pos.haschildren()) return;
		setc(i, j, c);
	}

	public void setc (int i, int j, int c)
	{
		synchronized (Pos)
		{
			Action a;
			if (P.color(i, j) == 0) // empty?
			{
				Node n = Pos.node();
				if (GF.getParameter("puresgf", true)
					&& (n.contains("B") || n.contains("W"))) n = newnode();
				n.addchange(new Change(i, j, 0));
				if (c > 0)
				{
					a = new Action("AB", Field.string(i, j));
				}
				else
				{
					a = new Action("AW", Field.string(i, j));
				}
				n.expandaction(a); // note the move action
				P.color(i, j, c);
				update(i, j);
			}
		}
	}

	int captured = 0, capturei, capturej;

	public void capture (int i, int j, Node n)
	// capture neighboring groups without liberties
	// capture own group on suicide
	{
		int c = -P.color(i, j);
		captured = 0;
		if (i > 0) capturegroup(i - 1, j, c, n);
		if (j > 0) capturegroup(i, j - 1, c, n);
		if (i < S - 1) capturegroup(i + 1, j, c, n);
		if (j < S - 1) capturegroup(i, j + 1, c, n);
		if (P.color(i, j) == -c)
		{
			capturegroup(i, j, -c, n);
		}
		if (captured == 1 && P.count(i, j) != 1) captured = 0;
		if ( !GF.getParameter("korule", true)) captured = 0;
	}

	public void capturegroup (int i, int j, int c, Node n)
	// Used by capture to determine the state of the groupt at (i,j)
	// Remove it, if it has no liberties and note the removals
	// as actions in the current node.
	{
		int ii, jj;
		Action a;
		if (P.color(i, j) != c) return;
		if ( !P.markgrouptest(i, j, 0)) // liberties?
		{
			for (ii = 0; ii < S; ii++)
				for (jj = 0; jj < S; jj++)
				{
					if (P.marked(ii, jj))
					{
						n.addchange(new Change(ii, jj, P.color(ii, jj), P
							.number(ii, jj)));
						if (P.color(ii, jj) > 0)
						{
							Pb++;
							n.Pb++;
						}
						else
						{
							Pw++;
							n.Pw++;
						}
						P.color(ii, jj, 0);
						update(ii, jj); // redraw the field (offscreen)
						captured++;
						capturei = ii;
						capturej = jj;
					}
				}
		}
	}

	public void variation (int i, int j)
	{
		if (Pos.parentPos() == null) return;
		if (P.color(i, j) == 0) // empty?
		{
			int c = P.color();
			goback();
			P.color( -c);
			set(i, j);
			if ( !GF.getParameter("variationnumbers", false))
			{
				P.number(i, j, 1);
				number = 2;
				Pos.node().number(2);
			}
			update(i, j);
		}
	}

	public String formtime (int t)
	{
		int s, m, h = t / 3600;
		if (h >= 1)
		{
			t = t - 3600 * h;
			m = t / 60;
			s = t - 60 * m;
			return "" + h + ":" + twodigits(m) + ":" + twodigits(s);
		}
		else
		{
			m = t / 60;
			s = t - 60 * m;
			return "" + m + ":" + twodigits(s);
		}
	}

	public String twodigits (int n)
	{
		if (n < 10)
			return "0" + n;
		else return "" + n;
	}

	public String lookuptime (String type)
	{
		int t;
		if (Pos.parentPos() != null)
		{
			String s = Pos.parentPos().node().getaction(type);
			if ( !s.equals(""))
			{
				try
				{
					t = Integer.parseInt(s);
					return formtime(t);
				}
				catch (Exception e)
				{
					return "";
				}
			}
			else return "";
		}
		return "";
	}

	public void showinformation ()
	// update the label to display the next move and who's turn it is
	// and disable variation buttons
	// update the navigation buttons
	// update the comment
	{
		Node n = Pos.node();
		number = n.number();
		NodeName = n.getaction("N");
		String ms = "";
		if (n.main())
		{
			if ( !Pos.haschildren())
				ms = "** ";
			else ms = "* ";
		}
		switch (State)
		{
			case 3:
				LText = ms + GF.resourceString("Set_black_stones");
				break;
			case 4:
				LText = ms + GF.resourceString("Set_white_stones");
				break;
			case 5:
				LText = ms + GF.resourceString("Mark_fields");
				break;
			case 6:
				LText = ms + GF.resourceString("Place_letters");
				break;
			case 7:
				LText = ms + GF.resourceString("Delete_stones");
				break;
			case 8:
				LText = ms + GF.resourceString("Remove_prisoners");
				break;
			case 9:
				LText = ms + GF.resourceString("Set_special_marker");
				break;
			case 10:
				LText = ms + GF.resourceString("Text__") + TextMarker;
				break;
			default:
				if (P.color() > 0)
				{
					String s = lookuptime("BL");
					if ( !s.equals(""))
						LText = ms + GF.resourceString("Next_move__Black_")
							+ number + " (" + s + ")";
					else LText = ms + GF.resourceString("Next_move__Black_")
						+ number;
				}
				else
				{
					String s = lookuptime("WL");
					if ( !s.equals(""))
						LText = ms + GF.resourceString("Next_move__White_")
							+ number + " (" + s + ")";
					else LText = ms + GF.resourceString("Next_move__White_")
						+ number;
				}
		}
		LText = LText + " (" + siblings() + " " + GF.resourceString("Siblings")
			+ ", " + children() + " " + GF.resourceString("Children") + ")";
		if (NodeName.equals(""))
		{
			GF.setLabel(LText);
			DisplayNodeName = false;
		}
		else
		{
			GF.setLabel(NodeName);
			DisplayNodeName = true;
		}
		GF.setState(3, !n.main());
		GF.setState(4, !n.main());
		GF.setState(7, !n.main() || Pos.haschildren());
		if (State == 1 || State == 2)
		{
			if (P.color() == 1)
				State = 1;
			else State = 2;
		}
		GF.setState(1, Pos.parentPos() != null
			&& Pos.parentPos().firstChild() != Pos);
		GF.setState(2, Pos.parentPos() != null
			&& Pos.parentPos().lastChild() != Pos);
		GF.setState(5, Pos.haschildren());
		GF.setState(6, Pos.parentPos() != null);
		if (State != 9)
			GF.setState(State);
		else GF.setMarkState(SpecialMarker);
		int i, j;
		// delete all marks and variations
		for (i = 0; i < S; i++)
			for (j = 0; j < S; j++)
			{
				if (P.tree(i, j) != null)
				{
					P.tree(i, j, null);
					update(i, j);
				}
				if (P.marker(i, j) != Field.NONE)
				{
					P.marker(i, j, Field.NONE);
					update(i, j);
				}
				if (P.letter(i, j) != 0)
				{
					P.letter(i, j, 0);
					update(i, j);
				}
				if (P.haslabel(i, j))
				{
					P.clearlabel(i, j);
					update(i, j);
				}
			}
		ListElement la = Pos.node().actions();
		Action a;
		String s;
		String sc = "";
		int let = 1;
		while (la != null) // setup the marks and letters
		{
			a = (Action)la.content();
			if (a.type().equals("C"))
			{
				sc = (String)a.arguments().content();
			}
			else if (a.type().equals("SQ") || a.type().equals("SL"))
			{
				ListElement larg = a.arguments();
				while (larg != null)
				{
					s = (String)larg.content();
					i = Field.i(s);
					j = Field.j(s);
					if (valid(i, j))
					{
						P.marker(i, j, Field.SQUARE);
						update(i, j);
					}
					larg = larg.next();
				}
			}
			else if (a.type().equals("MA") || a.type().equals("M")
				|| a.type().equals("TW") || a.type().equals("TB"))
			{
				ListElement larg = a.arguments();
				while (larg != null)
				{
					s = (String)larg.content();
					i = Field.i(s);
					j = Field.j(s);
					if (valid(i, j))
					{
						P.marker(i, j, Field.CROSS);
						update(i, j);
					}
					larg = larg.next();
				}
			}
			else if (a.type().equals("TR"))
			{
				ListElement larg = a.arguments();
				while (larg != null)
				{
					s = (String)larg.content();
					i = Field.i(s);
					j = Field.j(s);
					if (valid(i, j))
					{
						P.marker(i, j, Field.TRIANGLE);
						update(i, j);
					}
					larg = larg.next();
				}
			}
			else if (a.type().equals("CR"))
			{
				ListElement larg = a.arguments();
				while (larg != null)
				{
					s = (String)larg.content();
					i = Field.i(s);
					j = Field.j(s);
					if (valid(i, j))
					{
						P.marker(i, j, Field.CIRCLE);
						update(i, j);
					}
					larg = larg.next();
				}
			}
			else if (a.type().equals("L"))
			{
				ListElement larg = a.arguments();
				while (larg != null)
				{
					s = (String)larg.content();
					i = Field.i(s);
					j = Field.j(s);
					if (valid(i, j))
					{
						P.letter(i, j, let);
						let++;
						update(i, j);
					}
					larg = larg.next();
				}
			}
			else if (a.type().equals("LB"))
			{
				ListElement larg = a.arguments();
				while (larg != null)
				{
					s = (String)larg.content();
					i = Field.i(s);
					j = Field.j(s);
					if (valid(i, j) && s.length() >= 4 && s.charAt(2) == ':')
					{
						P.setlabel(i, j, s.substring(3));
						update(i, j);
					}
					larg = larg.next();
				}
			}
			la = la.next();
		}
		TreeNode p;
		ListElement l = null;
		if (VCurrent)
		{
			p = Pos.parentPos();
			if (p != null) l = p.firstChild().listelement();
		}
		else if (Pos.haschildren() && Pos.firstChild() != Pos.lastChild())
		{
			l = Pos.firstChild().listelement();
		}
		while (l != null)
		{
			p = (TreeNode)l.content();
			if (p != Pos)
			{
				la = p.node().actions();
				while (la != null)
				{
					a = (Action)la.content();
					if (a.type().equals("W") || a.type().equals("B"))
					{
						s = (String)a.arguments().content();
						i = Field.i(s);
						j = Field.j(s);
						if (valid(i, j))
						{
							P.tree(i, j, p);
							update(i, j);
						}
						break;
					}
					la = la.next();
				}
			}
			l = l.next();
		}
		if ( !GF.getComment().equals(sc))
		{
			GF.setComment(sc);
		}
		if (Range >= 0 && !KeepRange) clearrange();
	}

	public int siblings ()
	{
		ListElement l = Pos.listelement();
		if (l == null) return 0;
		while (l.previous() != null)
			l = l.previous();
		int count = 0;
		while (l.next() != null)
		{
			l = l.next();
			count++;
		}
		return count;
	}

	public int children ()
	{
		if ( !Pos.haschildren()) return 0;
		TreeNode p = Pos.firstChild();
		if (p == null) return 0;
		ListElement l = p.listelement();
		if (l == null) return 0;
		while (l.previous() != null)
			l = l.previous();
		int count = 1;
		while (l.next() != null)
		{
			l = l.next();
			count++;
		}
		return count;
	}

	public void clearsend ()
	{
		if (sendi >= 0)
		{
			int i = sendi;
			sendi = -1;
			update(i, sendj);
		}
	}

	public void getinformation ()
	// update the comment, when leaving the position
	{
		ListElement la = Pos.node().actions();
		Action a;
		clearsend();
		while (la != null)
		{
			a = (Action)la.content();
			if (a.type().equals("C"))
			{
				if (GF.getComment().equals(""))
					Pos.node().removeaction(la);
				else a.arguments().content(GF.getComment());
				return;
			}
			la = la.next();
		}
		String s = GF.getComment();
		if ( !s.equals(""))
		{
			Pos.addaction(new Action("C", s));
		}
	}

	public void update (int i, int j)
	// update the field (i,j) in the offscreen image Active
	// in dependance of the board position P.
	// display the last move mark, if applicable.
	{
	}

	public void copy ()
	// copy the offscreen board to the screen
	{
	}

	public void undonode ()
	// Undo everything that has been changed in the node.
	// (This will not correct the last move marker!)
	{
		Node n = Pos.node();
		clearrange();
		ListElement p = n.lastchange();
		while (p != null)
		{
			Change c = (Change)p.content();
			P.color(c.I, c.J, c.C);
			P.number(c.I, c.J, c.N);
			update(c.I, c.J);
			p = p.previous();
		}
		n.clearchanges();
		Pw -= n.Pw;
		Pb -= n.Pb;
	}

	public void setaction (Node n, Action a, int c)
	// interpret a set move action, update the last move marker,
	// c being the color of the move.
	{
		String s = (String)a.arguments().content();
		int i = Field.i(s);
		int j = Field.j(s);
		if ( !valid(i, j)) return;
		n.addchange(new Change(i, j, P.color(i, j), P.number(i, j)));
		P.color(i, j, c);
		P.number(i, j, n.number() - 1);
		showlast = false;
		update(lasti, lastj);
		showlast = true;
		lasti = i;
		lastj = j;
		update(i, j);
		P.color( -c);
		capture(i, j, n);
	}

	public void placeaction (Node n, Action a, int c)
	// interpret a set move action, update the last move marker,
	// c being the color of the move.
	{
		int i, j;
		ListElement larg = a.arguments();
		while (larg != null)
		{
			String s = (String)larg.content();
			i = Field.i(s);
			j = Field.j(s);
			if (valid(i, j))
			{
				n.addchange(new Change(i, j, P.color(i, j), P.number(i, j)));
				P.color(i, j, c);
				update(i, j);
			}
			larg = larg.next();
		}
	}

	public void emptyaction (Node n, Action a)
	// interpret a remove stone action
	{
		int i, j, r = 1;
		ListElement larg = a.arguments();
		while (larg != null)
		{
			String s = (String)larg.content();
			i = Field.i(s);
			j = Field.j(s);
			if (valid(i, j))
			{
				n.addchange(new Change(i, j, P.color(i, j), P.number(i, j)));
				if (P.color(i, j) < 0)
				{
					n.Pw++;
					Pw++;
				}
				else if (P.color(i, j) > 0)
				{
					n.Pb++;
					Pb++;
				}
				P.color(i, j, 0);
				update(i, j);
			}
			larg = larg.next();
		}
	}

	public void setnode ()
	// interpret all actions of a node
	{
		Node n = Pos.node();
		ListElement p = n.actions();
		if (p == null) return;
		Action a;
		String s;
		int i, j;
		while (p != null)
		{
			a = (Action)p.content();
			if (a.type().equals("SZ"))
			{
				if (Pos.parentPos() == null)
				// only at first node
				{
					try
					{
						int ss = Integer.parseInt(a.argument().trim());
						if (ss != S)
						{
							S = ss;
							P = new Position(S);
							updateall();
							copy();
						}
					}
					catch (NumberFormatException e)
					{}
				}
			}
			p = p.next();
		}
		n.clearchanges();
		n.Pw = n.Pb = 0;
		p = n.actions();
		while (p != null)
		{
			a = (Action)p.content();
			if (a.type().equals("B"))
			{
				setaction(n, a, 1);
			}
			else if (a.type().equals("W"))
			{
				setaction(n, a, -1);
			}
			if (a.type().equals("AB"))
			{
				placeaction(n, a, 1);
			}
			if (a.type().equals("AW"))
			{
				placeaction(n, a, -1);
			}
			else if (a.type().equals("AE"))
			{
				emptyaction(n, a);
			}
			p = p.next();
		}
	}

	public void setlast ()
	// update the last move marker applying all
	// set move actions in the node
	{
		Node n = Pos.node();
		ListElement l = n.actions();
		Action a;
		String s;
		int i = lasti, j = lastj;
		lasti = -1;
		lastj = -1;
		update(i, j);
		while (l != null)
		{
			a = (Action)l.content();
			if (a.type().equals("B") || a.type().equals("W"))
			{
				s = (String)a.arguments().content();
				i = Field.i(s);
				j = Field.j(s);
				if (valid(i, j))
				{
					lasti = i;
					lastj = j;
					update(lasti, lastj);
					P.color( -P.color(i, j));
				}
			}
			l = l.next();
		}
		number = n.number();
	}

	public void undo ()
	// take back the last move, ask if necessary
	{ // System.out.println("undo");
		if (Pos.haschildren() || Pos.parent() != null
			&& Pos.parent().lastchild() != Pos.parent().firstchild()
			&& Pos == Pos.parent().firstchild())
		{
			if (GF.askUndo()) doundo(Pos);
		}
		else doundo(Pos);
	}

	public void doundo (TreeNode pos1)
	{
		if (pos1 != Pos) return;
		if (Pos.parentPos() == null)
		{
			undonode();
			Pos.removeall();
			Pos.node().removeactions();
			showinformation();
			copy();
			return;
		}
		TreeNode pos = Pos;
		goback();
		if (pos == Pos.firstchild())
			Pos.removeall();
		else Pos.remove(pos);
		goforward();
		showinformation();
		copy();
	}

	public void goback ()
	// go one move back
	{
		State = 1;
		if (Pos.parentPos() == null) return;
		undonode();
		Pos = Pos.parentPos();
		setlast();
	}

	public void goforward ()
	// go one move forward
	{
		if ( !Pos.haschildren()) return;
		Pos = Pos.firstChild();
		setnode();
		setlast();
	}

	public void gotoMove (int move)
	{
		while (number <= move && Pos.firstChild() != null)
		{
			goforward();
		}
	}

	public void tovarleft ()
	{
		ListElement l = Pos.listelement();
		if (l == null) return;
		if (l.previous() == null) return;
		TreeNode newpos = (TreeNode)l.previous().content();
		goback();
		Pos = newpos;
		setnode();
	}

	public void tovarright ()
	{
		ListElement l = Pos.listelement();
		if (l == null) return;
		if (l.next() == null) return;
		TreeNode newpos = (TreeNode)l.next().content();
		goback();
		Pos = newpos;
		setnode();
	}

	public boolean hasvariation ()
	{
		ListElement l = Pos.listelement();
		if (l == null) return false;
		if (l.next() == null) return false;
		return true;
	}

	static public TreeNode getNext (TreeNode p)
	{
		ListElement l = p.listelement();
		if (l == null) return null;
		if (l.next() == null) return null;
		return (TreeNode)l.next().content();
	}

	public void territory (int i, int j)
	{
		mark(i, j);
		copy();
	}

	public void setpass ()
	{
		TreeNode p = T.top();
		while (p.haschildren())
			p = p.firstChild();
		Node n = new Node(number);
		p.addchild(new TreeNode(n));
		n.main(p);
		GF.yourMove(Pos != p);
		if (Pos == p)
		{
			getinformation();
			int c = P.color();
			goforward();
			P.color( -c);
			showinformation();
			GF.addComment(GF.resourceString("Pass"));
		}
		MainColor = -MainColor;
		captured = 0;
	}

	public void resume ()
	// Resume playing after marking
	{
		getinformation();
		State = 1;
		showinformation();
	}

	Node newtree ()
	{
		number = 1;
		Pw = Pb = 0;
		Node n = new Node(number);
		T = new SGFTree(n);
		Trees.setElementAt(T, CurrentTree);
		resettree();
		return n;
	}

	void resettree ()
	{
		Pos = T.top();
		P = new Position(S);
		lasti = lastj = -1;
		Pb = Pw = 0;
		updateall();
		copy();
	}

	public boolean deltree ()
	{
		newtree();
		return true;
	}

	public void active (boolean f)
	{
		Active = f;
	}

	public int getboardsize ()
	{
		return S;
	}

	public boolean canfinish ()
	{
		return Pos.isLastMain();
	}

	public int maincolor ()
	{
		return MainColor;
	}

	public boolean ismain ()
	{
		return Pos.isLastMain();
	}

	Node firstnode ()
	{
		return T.top().node();
	}

	boolean valid (int i, int j)
	{
		return i >= 0 && i < S && j >= 0 && j < S;
	}

	public void clearrange ()
	{
		if (Range == -1) return;
		Range = -1;
		updateall();
		copy();
	}

	// *****************************************
	// Methods to be called from outside sources
	// *****************************************

	// ****** navigational things **************

	// methods to move in the game tree, including
	// update of the visible board:

	public synchronized void back ()
	// one move up
	{
		State = 1;
		getinformation();
		goback();
		showinformation();
		copy();
	}

	public synchronized void forward ()
	// one move down
	{
		State = 1;
		getinformation();
		goforward();
		showinformation();
		copy();
	}

	public synchronized void fastback ()
	// 10 moves up
	{
		getinformation();
		for (int i = 0; i < 10; i++)
			goback();
		showinformation();
		copy();
	}

	public synchronized void fastforward ()
	// 10 moves down
	{
		getinformation();
		for (int i = 0; i < 10; i++)
			goforward();
		showinformation();
		copy();
	}

	public synchronized void allback ()
	// to top of tree
	{
		getinformation();
		while (Pos.parentPos() != null)
			goback();
		showinformation();
		copy();
	}

	public synchronized void allforward ()
	// to end of variation
	{
		getinformation();
		while (Pos.haschildren())
			goforward();
		showinformation();
		copy();
	}

	public synchronized void varleft ()
	// one variation to the left
	{
		State = 1;
		getinformation();
		ListElement l = Pos.listelement();
		if (l == null) return;
		if (l.previous() == null) return;
		TreeNode newpos = (TreeNode)l.previous().content();
		goback();
		Pos = newpos;
		setnode();
		showinformation();
		copy();
	}

	public synchronized void varright ()
	// one variation to the right
	{
		State = 1;
		getinformation();
		ListElement l = Pos.listelement();
		if (l == null) return;
		if (l.next() == null) return;
		TreeNode newpos = (TreeNode)l.next().content();
		goback();
		Pos = newpos;
		setnode();
		showinformation();
		copy();
	}

	public synchronized void varmain ()
	// to the main variation
	{
		State = 1;
		getinformation();
		while (Pos.parentPos() != null && !Pos.node().main())
		{
			goback();
		}
		if (Pos.haschildren()) goforward();
		showinformation();
		copy();
	}

	public synchronized void varmaindown ()
	// to end of main variation
	{
		State = 1;
		getinformation();
		while (Pos.parentPos() != null && !Pos.node().main())
		{
			goback();
		}
		while (Pos.haschildren())
		{
			goforward();
		}
		showinformation();
		copy();
	}

	public synchronized void varup ()
	// to the start of the variation
	{
		State = 1;
		getinformation();
		if (Pos.parentPos() != null) goback();
		while (Pos.parentPos() != null
			&& Pos.parentPos().firstChild() == Pos.parentPos().lastChild()
			&& !Pos.node().main())
		{
			goback();
		}
		showinformation();
		copy();
	}

	public synchronized void gotonext ()
	// goto next named node
	{
		State = 1;
		getinformation();
		goforward();
		while (Pos.node().getaction("N").equals(""))
		{
			if ( !Pos.haschildren()) break;
			goforward();
		}
		showinformation();
		copy();
	}

	public synchronized void gotoprevious ()
	// gotoprevious named node
	{
		State = 1;
		getinformation();
		goback();
		while (Pos.node().getaction("N").equals(""))
		{
			if (Pos.parentPos() == null) break;
			goback();
		}
		showinformation();
		copy();
	}

	public synchronized void gotonextmain ()
	// goto next game tree
	{
		if (CurrentTree + 1 >= Trees.size()) return;
		State = 1;
		getinformation();
		T.top().setaction("AP", "Jago:" + GF.version(), true);
		T.top().setaction("SZ", "" + S, true);
		T.top().setaction("GM", "1", true);
		T.top()
			.setaction("FF", GF.getParameter("puresgf", false)?"4":"1", true);
		CurrentTree++;
		T = (SGFTree)Trees.elementAt(CurrentTree);
		resettree();
		setnode();
		showinformation();
		copy();
	}

	public synchronized void gotopreviousmain ()
	// goto previous game tree
	{
		if (CurrentTree == 0) return;
		State = 1;
		getinformation();
		T.top().setaction("AP", "Jago:" + GF.version(), true);
		T.top().setaction("SZ", "" + S, true);
		T.top().setaction("GM", "1", true);
		T.top()
			.setaction("FF", GF.getParameter("puresgf", false)?"4":"1", true);
		CurrentTree--;
		T = (SGFTree)Trees.elementAt(CurrentTree);
		resettree();
		setnode();
		showinformation();
		copy();
	}

	public synchronized void addnewgame ()
	{
		State = 1;
		getinformation();
		T.top().setaction("AP", "Jago:" + GF.version(), true);
		T.top().setaction("SZ", "" + S, true);
		T.top().setaction("GM", "1", true);
		T.top()
			.setaction("FF", GF.getParameter("puresgf", false)?"4":"1", true);
		Node n = new Node(number);
		T = new SGFTree(n);
		CurrentTree++;
		if (CurrentTree >= Trees.size())
			Trees.addElement(T);
		else Trees.insertElementAt(T, CurrentTree);
		resettree();
		setnode();
		showinformation();
		copy();
	}

	public synchronized void removegame ()
	{
		if (Trees.size() == 1) return;
		Trees.removeElementAt(CurrentTree);
		if (CurrentTree >= Trees.size()) CurrentTree--;
		T = (SGFTree)Trees.elementAt(CurrentTree);
		resettree();
		setnode();
		showinformation();
		copy();
	}

	// ***** change the node at end of main tree ********
	// usually called by another board or server

	public synchronized void black (int i, int j)
	// white move at i,j at the end of the main tree
	{
		if (i < 0 || j < 0 || i >= S || j >= S) return;
		TreeNode p = T.top();
		while (p.haschildren())
			p = p.firstChild();
		Action a = new Action("B", Field.string(i, j));
		Node n = new Node(p.node().number() + 1);
		n.addaction(a);
		p.addchild(new TreeNode(n));
		n.main(p);
		GF.yourMove(Pos != p);
		if (Pos == p) forward();
		MainColor = -1;
	}

	public synchronized void white (int i, int j)
	// black move at i,j at the end of the main tree
	{
		if (i < 0 || j < 0 || i >= S || j >= S) return;
		TreeNode p = T.top();
		while (p.haschildren())
			p = p.firstChild();
		Action a = new Action("W", Field.string(i, j));
		Node n = new Node(p.node().number() + 1);
		n.addaction(a);
		p.addchild(new TreeNode(n));
		n.main(p);
		GF.yourMove(Pos != p);
		if (Pos == p) forward();
		MainColor = 1;
	}

	public synchronized void setblack (int i, int j)
	// set a white stone at i,j at the end of the main tree
	{
		if (i < 0 || j < 0 || i >= S || j >= S) return;
		TreeNode p = T.top();
		while (p.haschildren())
			p = p.firstChild();
		Action a = new Action("AB", Field.string(i, j));
		Node n;
		if (p == T.top())
		{
			TreeNode newpos;
			p.addchild(newpos = new TreeNode(1));
			if (Pos == p) Pos = newpos;
			p = newpos;
			p.main(true);
		}
		n = p.node();
		n.expandaction(a);
		if (Pos == p)
		{
			n.addchange(new Change(i, j, P.color(i, j), P.number(i, j)));
			P.color(i, j, 1);
			update(i, j);
			copy();
		}
		MainColor = -1;
	}

	public synchronized void setwhite (int i, int j)
	// set a white stone at i,j at the end of the main tree
	{
		if (i < 0 || j < 0 || i >= S || j >= S) return;
		TreeNode p = T.top();
		while (p.haschildren())
			p = p.firstChild();
		Action a = new Action("AW", Field.string(i, j));
		Node n;
		if (p == T.top())
		{
			TreeNode newpos;
			p.addchild(newpos = new TreeNode(1));
			if (Pos == p) Pos = newpos;
			p = newpos;
			p.main(true);
		}
		n = p.node();
		n.expandaction(a);
		if (Pos == p)
		{
			n.addchange(new Change(i, j, P.color(i, j), P.number(i, j)));
			P.color(i, j, -1);
			update(i, j);
			copy();
		}
		MainColor = 1;
	}

	public synchronized void pass ()
	// pass at current node
	// notify BoardInterface
	{
		if (Pos.haschildren()) return;
		if (GF.blocked() && Pos.node().main()) return;
		getinformation();
		P.color( -P.color());
		Node n = new Node(number);
		Pos.addchild(new TreeNode(n));
		n.main(Pos);
		goforward();
		setlast();
		showinformation();
		copy();
		GF.addComment(GF.resourceString("Pass"));
		captured = 0;
	}

	public synchronized void remove (int i0, int j0)
	// completely remove a group there
	{
		int s = State;
		varmaindown();
		State = s;
		if (P.color(i0, j0) == 0) return;
		Action a;
		P.markgroup(i0, j0);
		int i, j;
		int c = P.color(i0, j0);
		Node n = Pos.node();
		if (GF.getParameter("puresgf", true)
			&& (n.contains("B") || n.contains("W"))) n = newnode();
		for (i = 0; i < S; i++)
			for (j = 0; j < S; j++)
			{
				if (P.marked(i, j))
				{
					a = new Action("AE", Field.string(i, j));
					n
						.addchange(new Change(i, j, P.color(i, j), P.number(i,
							j)));
					n.expandaction(a);
					if (P.color(i, j) > 0)
					{
						n.Pb++;
						Pb++;
					}
					else
					{
						n.Pw++;
						Pw++;
					}
					P.color(i, j, 0);
					update(i, j);
				}
			}
		copy();
	}

	// ************ change the current node *****************

	public void clearmarks ()
	// clear all marks in the current node
	{
		getinformation();
		undonode();
		ListElement la = Pos.node().actions(), lan;
		Action a;
		while (la != null)
		{
			a = (Action)la.content();
			lan = la.next();
			if (a.type().equals("M") || a.type().equals("L")
				|| a.type().equals("MA") || a.type().equals("SQ")
				|| a.type().equals("SL") || a.type().equals("CR")
				|| a.type().equals("TR") || a.type().equals("LB"))
			{
				Pos.node().removeaction(la);
			}
			la = lan;
		}
		setnode();
		showinformation();
		copy();
	}

	public void clearremovals ()
	// undo all removals in the current node
	{
		if (Pos.haschildren()) return;
		getinformation();
		undonode();
		ListElement la = Pos.node().actions(), lan;
		Action a;
		while (la != null)
		{
			a = (Action)la.content();
			lan = la.next();
			if (a.type().equals("AB") || a.type().equals("AW")
				|| a.type().equals("AE"))
			{
				Pos.node().removeaction(la);
			}
			la = lan;
		}
		setnode();
		showinformation();
		copy();
	}

	// *************** change the game tree ***********

	public void insertnode ()
	// insert an empty node
	{
		if (Pos.haschildren() && !GF.askInsert()) return;
		Node n = new Node(Pos.node().number());
		Pos.insertchild(new TreeNode(n));
		n.main(Pos);
		getinformation();
		Pos = Pos.lastChild();
		setlast();
		showinformation();
		copy();
	}

	public void insertvariation ()
	// insert an empty variation to the current
	{
		if (Pos.parentPos() == null) return;
		getinformation();
		int c = P.color();
		back();
		Node n = new Node(2);
		Pos.addchild(new TreeNode(n));
		n.main(Pos);
		Pos = Pos.lastChild();
		setlast();
		P.color( -c);
		showinformation();
		copy();
	}

	public void undo (int n)
	// undo the n last moves, notify BoardInterface
	{
		varmaindown();
		for (int i = 0; i < n; i++)
		{
			goback();
			Pos.removeall();
			showinformation();
			copy();
		}
		GF.addComment(GF.resourceString("Undo"));
	}

	// ********** set board state ******************

	public void setblack ()
	// set black mode
	{
		getinformation();
		State = 3;
		showinformation();
	}

	public void setwhite ()
	// set white mode
	{
		getinformation();
		State = 4;
		showinformation();
	}

	public void black ()
	// black to play
	{
		getinformation();
		State = 1;
		P.color(1);
		showinformation();
	}

	public void white ()
	// white to play
	{
		getinformation();
		State = 2;
		P.color( -1);
		showinformation();
	}

	public void mark ()
	// marking
	{
		getinformation();
		State = 5;
		showinformation();
	}

	public void specialmark (int i)
	// marking
	{
		getinformation();
		State = 9;
		SpecialMarker = i;
		showinformation();
	}

	public void textmark (String s)
	// marking
	{
		getinformation();
		State = 10;
		TextMarker = s;
		showinformation();
	}

	public void letter ()
	// letter
	{
		getinformation();
		State = 6;
		showinformation();
	}

	public void deletestones ()
	// hide stones
	{
		getinformation();
		State = 7;
		showinformation();
	}

	public boolean score ()
	// board state is removing groups
	{
		if (Pos.haschildren()) return false;
		getinformation();
		State = 8;
		Removing = true;
		showinformation();
		if (Pos.node().main())
			return true;
		else return false;
	}

	synchronized public void setsize (int s)
	// set the board size
	// clears the board !!!
	{
		if (s < 5 || s > 59) return;
		S = s;
		P = new Position(S);
		number = 1;
		Node n = new Node(number);
		n.main(true);
		lasti = lastj = -1;
		T = new SGFTree(n);
		Trees.setElementAt(T, CurrentTree);
		Pos = T.top();
		showinformation();
		copy();
	}

	// ******** set board information **********

	void setname (String s)
	// set the name of the node
	{
		Pos.setaction("N", s, true);
		showinformation();
	}

	public void setinformation (String black, String blackrank, String white,
		String whiterank, String komi, String handicap)
	// set various things like names, rank etc.
	{
		T.top().setaction("PB", black, true);
		T.top().setaction("BR", blackrank, true);
		T.top().setaction("PW", white, true);
		T.top().setaction("WR", whiterank, true);
		T.top().setaction("KM", komi, true);
		T.top().setaction("HA", handicap, true);
		T.top().setaction("GN", white + "-" + black, true);
		T.top().setaction("DT", new Date().toString());
	}

	// ************ get board information ******

	String getname ()
	// get node name
	{
		return T.top().getaction("N");
	}

	public String getKomi ()
	// get Komi string
	{
		return T.top().getaction("KM");
	}

	public String extraInformation ()
	// get a mixture from handicap, komi and prisoners
	{
		StringBuffer b = new StringBuffer(GF.resourceString("_("));
		Node n = T.top().node();
		if (n.contains("HA"))
		{
			b.append(GF.resourceString("Ha_"));
			b.append(n.getaction("HA"));
		}
		if (n.contains("KM"))
		{
			b.append(GF.resourceString("__Ko"));
			b.append(n.getaction("KM"));
		}
		b.append(GF.resourceString("__B"));
		b.append("" + Pw);
		b.append(GF.resourceString("__W"));
		b.append("" + Pb);
		b.append(GF.resourceString("_)"));
		return b.toString();
	}

	// ***************** several other things ******

	public void lastrange (int n)
	// set the range for stone numbers
	{
		int l = Pos.node().number() - 2;
		Range = l / n * n;
		if (Range < 0) Range = 0;
		KeepRange = true;
		updateall();
		copy();
		KeepRange = false;
	}

	public void addcomment (String s)
	// add a string to the comments, notifies comment area
	{
		TreeNode p = T.top();
		while (p.haschildren())
			p = p.firstChild();
		if (Pos == p) getinformation();
		ListElement la = p.node().actions();
		Action a;
		String Added = "";
		ListElement larg;
		outer: while (true)
		{
			while (la != null)
			{
				a = (Action)la.content();
				if (a.type().equals("C"))
				{
					larg = a.arguments();
					if (((String)larg.content()).equals(""))
					{
						larg.content(s);
						Added = s;
					}
					else
					{
						larg.content((String)larg.content() + "\n" + s);
						Added = "\n" + s;
					}
					break outer;
				}
				la = la.next();
			}
			p.addaction(new Action("C", s));
			break;
		}
		if (Pos == p)
		{
			GF.appendComment(Added);
			showinformation();
		}
	}

	public String done ()
	// count territory and return result string
	// notifies BoardInterface
	{
		if (Pos.haschildren()) return null;
		clearmarks();
		getinformation();
		int i, j;
		int tb = 0, tw = 0, sb = 0, sw = 0;
		P.getterritory();
		for (i = 0; i < S; i++)
			for (j = 0; j < S; j++)
			{
				if (P.territory(i, j) == 1 || P.territory(i, j) == -1)
				{
					markterritory(i, j, P.territory(i, j));
					if (P.territory(i, j) > 0)
						tb++;
					else tw++;
				}
				else
				{
					if (P.color(i, j) > 0)
						sb++;
					else if (P.color(i, j) < 0) sw++;
				}
			}
		String s = GF.resourceString("Chinese_count_") + "\n"
			+ GF.resourceString("Black__") + (sb + tb)
			+ GF.resourceString("__White__") + (sw + tw) + "\n"
			+ GF.resourceString("Japanese_count_") + "\n"
			+ GF.resourceString("Black__") + (Pw + tb)
			+ GF.resourceString("__White__") + (Pb + tw);
		showinformation();
		copy();
		if (Pos.node().main())
		{
			GF.result(tb, tw);
		}
		return s;
	}

	public String docount ()
	// maka a local count and return result string
	{
		clearmarks();
		getinformation();
		int i, j;
		int tb = 0, tw = 0, sb = 0, sw = 0;
		P.getterritory();
		for (i = 0; i < S; i++)
			for (j = 0; j < S; j++)
			{
				if (P.territory(i, j) == 1 || P.territory(i, j) == -1)
				{
					markterritory(i, j, P.territory(i, j));
					if (P.territory(i, j) > 0)
						tb++;
					else tw++;
				}
				else
				{
					if (P.color(i, j) > 0)
						sb++;
					else if (P.color(i, j) < 0) sw++;
				}
			}
		showinformation();
		copy();
		return GF.resourceString("Chinese_count_") + "\n"
			+ GF.resourceString("Black__") + (sb + tb)
			+ GF.resourceString("__White__") + (sw + tw) + "\n"
			+ GF.resourceString("Japanese_count_") + "\n"
			+ GF.resourceString("Black__") + (Pw + tb)
			+ GF.resourceString("__White__") + (Pb + tw);
	}

	public void load (BufferedReader in) throws IOException
	// load a game from the stream
	{
		Vector v = SGFTree.load(in, GF);
		synchronized (this)
		{
			if (v.size() == 0) return;
			showlast = false;
			update(lasti, lastj);
			showlast = true;
			lasti = lastj = -1;
			newtree();
			Trees = v;
			T = (SGFTree)v.elementAt(0);
			CurrentTree = 0;
			resettree();
			setnode();
			showinformation();
			copy();
		}
	}

	public void loadXml (XmlReader xml) throws XmlReaderException
	// load a game from the stream
	{
		Vector v = SGFTree.load(xml, GF);
		synchronized (this)
		{
			if (v.size() == 0) return;
			showlast = false;
			update(lasti, lastj);
			showlast = true;
			lasti = lastj = -1;
			newtree();
			Trees = v;
			T = (SGFTree)v.elementAt(0);
			CurrentTree = 0;
			resettree();
			setnode();
			showinformation();
			copy();
		}
	}

	public void save (PrintWriter o)
	// saves the file to the specified print stream
	// in SGF
	{
		getinformation();
		T.top().setaction("AP", "Jago:" + GF.version(), true);
		T.top().setaction("SZ", "" + S, true);
		T.top().setaction("GM", "1", true);
		T.top()
			.setaction("FF", GF.getParameter("puresgf", false)?"4":"1", true);
		for (int i = 0; i < Trees.size(); i++)
			((SGFTree)Trees.elementAt(i)).print(o);
	}

	public void savePos (PrintWriter o)
	// saves the file to the specified print stream
	// in SGF
	{
		getinformation();
		Node n = new Node(0);
		positionToNode(n);
		o.println("(");
		n.print(o);
		o.println(")");
	}

	public void saveXML (PrintWriter o, String encoding)
	// save the file in Jago's XML format
	{
		getinformation();
		T.top().setaction("AP", "Jago:" + GF.version(), true);
		T.top().setaction("SZ", "" + S, true);
		T.top().setaction("GM", "1", true);
		T.top()
			.setaction("FF", GF.getParameter("puresgf", false)?"4":"1", true);
		XmlWriter xml = new XmlWriter(o);
		xml.printEncoding(encoding);
		xml.printXls("go.xsl");
		xml.printDoctype("Go", "go.dtd");
		xml.startTagNewLine("Go");
		for (int i = 0; i < Trees.size(); i++)
		{
			((SGFTree)Trees.elementAt(i)).printXML(xml);
		}
		xml.endTagNewLine("Go");
	}

	public void saveXMLPos (PrintWriter o, String encoding)
	// save the file in Jago's XML format
	{
		getinformation();
		T.top().setaction("AP", "Jago:" + GF.version(), true);
		T.top().setaction("SZ", "" + S, true);
		T.top().setaction("GM", "1", true);
		T.top()
			.setaction("FF", GF.getParameter("puresgf", false)?"4":"1", true);
		XmlWriter xml = new XmlWriter(o);
		xml.printEncoding(encoding);
		xml.printXls("go.xsl");
		xml.printDoctype("Go", "go.dtd");
		xml.startTagNewLine("Go");
		Node n = new Node(0);
		positionToNode(n);
		SGFTree t = new SGFTree(n);
		t.printXML(xml);
		xml.endTagNewLine("Go");
	}

	public void asciisave (PrintWriter o)
	// an ASCII image of the board.
	{
		int i, j;
		o.println(T.top().getaction("GN"));
		o.print("      ");
		for (i = 0; i < S; i++)
		{
			char a;
			if (i <= 7)
				a = (char)('A' + i);
			else a = (char)('A' + i + 1);
			o.print(" " + a);
		}
		o.println();
		o.print("      ");
		for (i = 0; i < S; i++)
			o.print("--");
		o.println("-");
		for (i = 0; i < S; i++)
		{
			o.print("  ");
			if (S - i < 10)
				o.print(" " + (S - i));
			else o.print(S - i);
			o.print(" |");
			for (j = 0; j < S; j++)
			{
				switch (P.color(j, i))
				{
					case 1:
						o.print(" #");
						break;
					case -1:
						o.print(" O");
						break;
					case 0:
						if (P.haslabel(j, i))
							o.print(" " + P.label(j, i));
						else if (P.letter(j, i) > 0)
							o.print(" " + (char)(P.letter(j, i) + 'a' - 1));
						else if (P.marker(j, i) > 0)
							o.print(" X");
						else if (ishand(i) && ishand(j))
							o.print(" ,");
						else o.print(" .");
						break;
				}
			}
			o.print(" | ");
			if (S - i < 10)
				o.print(" " + (S - i));
			else o.print(S - i);
			o.println();
		}
		o.print("      ");
		for (i = 0; i < S; i++)
			o.print("--");
		o.println("-");
		o.print("      ");
		for (i = 0; i < S; i++)
		{
			char a;
			if (i <= 7)
				a = (char)('A' + i);
			else a = (char)('A' + i + 1);
			o.print(" " + a);
		}
		o.println();
	}

	public void positionToNode (Node n)
	// copy the current position to a node.
	{
		n.setaction("AP", "Jago:" + GF.version(), true);
		n.setaction("SZ", "" + S, true);
		n.setaction("GM", "1", true);
		n.setaction("FF", GF.getParameter("puresgf", false)?"4":"1", true);
		n.copyAction(T.top().node(), "GN");
		n.copyAction(T.top().node(), "DT");
		n.copyAction(T.top().node(), "PB");
		n.copyAction(T.top().node(), "BR");
		n.copyAction(T.top().node(), "PW");
		n.copyAction(T.top().node(), "WR");
		n.copyAction(T.top().node(), "PW");
		n.copyAction(T.top().node(), "US");
		n.copyAction(T.top().node(), "CP");
		int i, j;
		for (i = 0; i < S; i++)
		{
			for (j = 0; j < S; j++)
			{
				String field = Field.string(i, j);
				switch (P.color(i, j))
				{
					case 1:
						n.expandaction(new Action("AB", field));
						break;
					case -1:
						n.expandaction(new Action("AW", field));
						break;
				}
				if (P.marker(i, j) > 0)
				{
					switch (P.marker(i, j))
					{
						case Field.SQUARE:
							n.expandaction(new Action("SQ", field));
							break;
						case Field.TRIANGLE:
							n.expandaction(new Action("TR", field));
							break;
						case Field.CIRCLE:
							n.expandaction(new Action("CR", field));
							break;
						default:
							n.expandaction(new MarkAction(field, GF));
					}
				}
				else if (P.haslabel(i, j))
					n
						.expandaction(new Action("LB", field + ":"
							+ P.label(i, j)));
				else if (P.letter(i, j) > 0)
					n.expandaction(new Action("LB", field + ":"
						+ P.letter(i, j)));
			}
		}
	}

	boolean ishand (int i)
	{
		if (S > 13)
		{
			return i == 3 || i == S - 4 || i == S / 2;
		}
		else if (S > 9)
		{
			return i == 3 || i == S - 4;
		}
		else return false;
	}

	public void handicap (int n)
	// set number of handicap points
	{
		int h = S < 13?3:4;
		if (n > 5)
		{
			setblack(h - 1, S / 2);
			setblack(S - h, S / 2);
		}
		if (n > 7)
		{
			setblack(S / 2, h - 1);
			setblack(S / 2, S - h);
		}
		switch (n)
		{
			case 9:
			case 7:
			case 5:
				setblack(S / 2, S / 2);
			case 8:
			case 6:
			case 4:
				setblack(S - h, S - h);
			case 3:
				setblack(h - 1, h - 1);
			case 2:
				setblack(h - 1, S - h);
			case 1:
				setblack(S - h, h - 1);
		}
		MainColor = -1;
	}

	public void updateall ()
	// update all of the board
	{
	}

	public void updateboard ()
	// redraw the board and its background
	{
	}

	/**
	 * Search the string as substring of a comment, go to that node and report
	 * success. On failure this routine will go up to the root node.
	 */
	public boolean search (String s)
	{
		State = 1;
		getinformation();
		TreeNode pos = Pos;
		boolean found = true;
		outer: while (Pos.node().getaction("C").indexOf(s) < 0 || Pos == pos)
		{
			if ( !Pos.haschildren())
			{
				while ( !hasvariation())
				{
					if (Pos.parent() == null)
					{
						found = false;
						break outer;
					}
					else goback();
				}
				tovarright();
			}
			else goforward();
		}
		showinformation();
		copy();
		return found;
	}

	// *****************************************************
	// procedures that might be overloaded for more control
	// (Callback to server etc.)
	// *****************************************************

	void movemouse (int i, int j)
	// set a move at i,j
	{
		if (Pos.haschildren()) return;
		if (captured == 1 && capturei == i && capturej == j
			&& GF.getParameter("preventko", true)) return;
		set(i, j); // try to set a new move
	}

	void setmouse (int i, int j, int c)
	// set a stone at i,j with specified color
	{
		set(i, j, c);
		undonode();
		setnode();
		showinformation();
	}

	void setmousec (int i, int j, int c)
	// set a stone at i,j with specified color
	{
		setc(i, j, c);
		undonode();
		setnode();
		showinformation();
	}

	void deletemouse (int i, int j)
	// delete a stone at i,j
	{
		if (Pos.haschildren()) return;
		deletemousec(i, j);
	}

	void deletemousec (int i, int j)
	// delete a stone at i,j
	{
		delete(i, j);
		undonode();
		setnode();
		showinformation();
	}

	void removemouse (int i, int j)
	// remove a group at i,j
	{
		if (Pos.haschildren()) return;
		removegroup(i, j);
		undonode();
		setnode();
		showinformation();
	}

	void setVariationStyle (boolean hide, boolean current)
	{
		undonode();
		VHide = hide;
		VCurrent = current;
		setnode();
		updateall();
		copy();
	}
}
