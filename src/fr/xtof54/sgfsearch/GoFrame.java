package fr.xtof54.sgfsearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import rene.util.FileName;
import rene.util.xml.XmlReader;

public class GoFrame implements BoardInterface
{
	String Dir; // FileDialog directory
	public Board B; // The board itself
	// menu check items:
	public boolean BWColor = false, LastNumber = false, ShowTarget = false;
	String Text = "text";
	boolean Show;
	String DefaultTitle = "";

	public GoFrame (String s)
	// For children, who set up their own menus
	{
		DefaultTitle = s;
	}

	public void addmenuitems ()
	// for children to add menu items (because of bug in Linux Java 1.5)
	{}

	public void iconPressed (String s)
	{
		if (s.equals("undo"))
			doAction("Undo");
		else if (s.equals("allback"))
			doAction("I<<");
		else if (s.equals("fastback"))
			doAction("<<");
		else if (s.equals("back"))
			doAction("<");
		else if (s.equals("forward"))
			doAction(">");
		else if (s.equals("fastforward"))
			doAction(">>");
		else if (s.equals("allforward"))
			doAction(">>I");
		else if (s.equals("variationback"))
			doAction("<V");
		else if (s.equals("variationstart"))
			doAction("V");
		else if (s.equals("variationforward"))
			doAction("V>");
		else if (s.equals("main"))
			doAction("*");
		else if (s.equals("mainend"))
			doAction("**");
		else if (s.equals("mark"))
			B.mark();
		else if (s.equals("mark"))
			B.mark();
		else if (s.equals("square"))
			B.specialmark(Field.SQUARE);
		else if (s.equals("triangle"))
			B.specialmark(Field.TRIANGLE);
		else if (s.equals("circle"))
			B.specialmark(Field.CIRCLE);
		else if (s.equals("letter"))
			B.letter();
		else if (s.equals("text"))
		{
			B.textmark(Text);
//			if (TMQ == null) TMQ = new TextMarkQuestion(this, Text);
		}
		else if (s.equals("black"))
			B.black();
		else if (s.equals("white"))
			B.white();
		else if (s.equals("setblack"))
			B.setblack();
		else if (s.equals("setwhite"))
			B.setwhite();
		else if (s.equals("delete"))
			B.deletestones();
		else if (s.equals("deletemarks"))
			B.clearmarks();
		else if (s.equals("play")) B.resume();
	}

	public void doAction (String o)
	{
		if ("Undo".equals(o))
		{
			B.undo();
		}
		else if ("<".equals(o))
		{
			B.back();
		}
		else if (">".equals(o))
		{
			B.forward();
		}
		else if (">>".equals(o))
		{
			B.fastforward();
		}
		else if ("<<".equals(o))
		{
			B.fastback();
		}
		else if ("I<<".equals(o))
		{
			B.allback();
		}
		else if (">>I".equals(o))
		{
			B.allforward();
		}
		else if ("<V".equals(o))
		{
			B.varleft();
		}
		else if ("V>".equals(o))
		{
			B.varright();
		}
		else if ("V".equals(o))
		{
			B.varup();
		}
		else if ("**".equals(o))
		{
			B.varmaindown();
		}
		else if ("*".equals(o))
		{
			B.varmain();
		}
		else if ("Pass".equals(o))
		{
			B.pass();
			notepass();
		}
		else if ("Resume_playing".equals(o))
		{
			B.resume();
		}
		else if ("Clear_all_marks".equals(o))
		{
			B.clearmarks();
		}
		else if ("Undo_Adding_Removing".equals(o))
		{
			B.clearremovals();
		}
		else if ("Remove_groups".equals(o))
		{
			B.score();
		}
		else if ("Score".equals(o))
		{
			String s = B.done();
//			if (s != null) new Message(this, s);
		}
		else if ("Local_Count".equals(o))
		{
//			new Message(this, B.docount());
		}
		else if ("New".equals(o))
		{
			B.deltree();
			B.copy();
//			setTitle(DefaultTitle);
		}
		else if ("Mail".equals(o)) // mail the game
		{
//			ByteArrayOutputStream ba = new ByteArrayOutputStream(50000);
//			try
//			{
//				if (Global.getParameter("xml", false))
//				{
//					PrintWriter po = new PrintWriter(new OutputStreamWriter(ba,
//							"UTF8"), true);
//					B.saveXML(po, "utf-8");
//					po.close();
//				}
//				else
//				{
//					PrintWriter po = new PrintWriter(ba, true);
//					B.save(po);
//					po.close();
//				}
//			}
//			catch (Exception ex)
//			{}
//			new MailDialog(this, ba.toString());
//			return;
		}
		else if ("Ascii_Mail".equals(o))
			// ascii dump of the game
		{
//			ByteArrayOutputStream ba = new ByteArrayOutputStream(10000);
//			PrintWriter po = new PrintWriter(ba, true);
//			try
//			{
//				B.asciisave(po);
//			}
//			catch (Exception ex)
//			{}
//			new MailDialog(this, ba.toString());
//			return;
		}
		else if ("Print".equals(o)) // print the game
		{
//			B.print(Global.frame());
		}
		else if ("Save".equals(o)) // save the game
		{ // File dialog handling
	//		FileDialog fd = new FileDialog(this, Global.resourceString("Save"),
//					FileDialog.SAVE);
//			if ( !Dir.equals("")) fd.setDirectory(Dir);
//			String s = B.firstnode().getaction("GN");
//			if (s != null && !s.equals(""))
//				fd.setFile(s
//						+ "."
//						+ Global.getParameter("extension", Global.getParameter(
//								"xml", false)?"xml":"sgf"));
//			else fd.setFile("*."
//					+ Global.getParameter("extension", Global.getParameter("xml",
//							false)?"xml":"sgf"));
//			fd.setFilenameFilter(this);
//			center(fd);
//			fd.setVisible(true);
//			String fn = fd.getFile();
//			if (fn == null) return;
//			setGameTitle(FileName.purefilename(fn));
//			Dir = fd.getDirectory();
//			try
//			// print out using the board class
//			{
//				PrintWriter fo;
//				if (Global.getParameter("xml", false))
//				{
//					if (Global.isApplet())
//					{
//						fo = new PrintWriter(new OutputStreamWriter(
//								new FileOutputStream(fd.getDirectory() + fn),
//								"UTF8"));
//						B.saveXML(fo, "utf-8");
//					}
//					else
//					{
//						String Encoding = Global.getParameter("encoding",
//								System.getProperty("file.encoding")).toUpperCase();
//						if (Encoding.equals(""))
//						{
//							fo = new PrintWriter(new OutputStreamWriter(
//									new FileOutputStream(fd.getDirectory() + fn),
//									"UTF8"));
//							B.saveXML(fo, "utf-8");
//						}
//						else
//						{
//							String XMLEncoding = "";
//							if (Encoding.equals("CP1252")
//									|| Encoding.equals("ISO8859_1"))
//							{
//								Encoding = "ISO8859_1";
//								XMLEncoding = "iso-8859-1";
//							}
//							else
//							{
//								Encoding = "UTF8";
//								XMLEncoding = "utf-8";
//							}
//							FileOutputStream fos = new FileOutputStream(fd
//									.getDirectory()
//									+ fn);
//							try
//							{
//								fo = new PrintWriter(new OutputStreamWriter(
//										fos, Encoding));
//							}
//							catch (Exception e)
//							{
//								Encoding = "UTF8";
//								XMLEncoding = "utf-8";
//								fo = new PrintWriter(new OutputStreamWriter(
//										fos, Encoding));
//							}
//							B.saveXML(fo, XMLEncoding);
//						}
//					}
//				}
//				else
//				{
//					if (Global.isApplet())
//						fo = new PrintWriter(new OutputStreamWriter(
//								new FileOutputStream(fd.getDirectory() + fn),
//								Global.getParameter("encoding", "ASCII")));
//					else fo = new PrintWriter(new OutputStreamWriter(
//							new FileOutputStream(fd.getDirectory() + fn), Global
//							.getParameter("encoding", System
//									.getProperty("file.encoding"))));
//					B.save(fo);
//				}
//				fo.close();
//			}
//			catch (IOException ex)
//			{
//				new Message(this, Global.resourceString("Write_error_") + "\n"
//						+ ex.toString());
//				return;
//			}
		}
		else if ("Save_Position".equals(o)) // save the
			// position
		{ // File dialog handling
//			FileDialog fd = new FileDialog(this, Global
//					.resourceString("Save Position"), FileDialog.SAVE);
//			if ( !Dir.equals("")) fd.setDirectory(Dir);
//			String s = B.firstnode().getaction("GN");
//			if (s != null && !s.equals(""))
//				fd.setFile(s
//						+ "."
//						+ Global.getParameter("extension", Global.getParameter(
//								"xml", false)?"xml":"sgf"));
//			else fd.setFile("*."
//					+ Global.getParameter("extension", Global.getParameter("xml",
//							false)?"xml":"sgf"));
//			fd.setFilenameFilter(this);
//			center(fd);
//			fd.setVisible(true);
//			String fn = fd.getFile();
//			if (fn == null) return;
//			Dir = fd.getDirectory();
//			try
//			// print out using the board class
//			{
//				PrintWriter fo;
//				if (Global.getParameter("xml", false))
//				{
//					if (Global.isApplet())
//					{
//						fo = new PrintWriter(new OutputStreamWriter(
//								new FileOutputStream(fd.getDirectory() + fn),
//								"UTF8"));
//						B.saveXML(fo, "utf-8");
//					}
//					else
//					{
//						String Encoding = Global.getParameter("encoding",
//								System.getProperty("file.encoding")).toUpperCase();
//						if (Encoding.equals(""))
//						{
//							fo = new PrintWriter(new OutputStreamWriter(
//									new FileOutputStream(fd.getDirectory() + fn),
//									"UTF8"));
//							B.saveXMLPos(fo, "utf-8");
//						}
//						else
//						{
//							String XMLEncoding = "";
//							if (Encoding.equals("CP1252")
//									|| Encoding.equals("ISO8859_1"))
//							{
//								Encoding = "ISO8859_1";
//								XMLEncoding = "iso-8859-1";
//							}
//							else
//							{
//								Encoding = "UTF8";
//								XMLEncoding = "utf-8";
//							}
//							FileOutputStream fos = new FileOutputStream(fd
//									.getDirectory()
//									+ fn);
//							try
//							{
//								fo = new PrintWriter(new OutputStreamWriter(
//										fos, Encoding));
//							}
//							catch (Exception e)
//							{
//								Encoding = "UTF8";
//								XMLEncoding = "utf-8";
//								fo = new PrintWriter(new OutputStreamWriter(
//										fos, Encoding));
//							}
//							B.saveXMLPos(fo, XMLEncoding);
//						}
//					}
//				}
//				else
//				{
//					if (Global.isApplet())
//						fo = new PrintWriter(new OutputStreamWriter(
//								new FileOutputStream(fd.getDirectory() + fn),
//								Global.getParameter("encoding", "ASCII")));
//					else fo = new PrintWriter(new OutputStreamWriter(
//							new FileOutputStream(fd.getDirectory() + fn), Global
//							.getParameter("encoding", System
//									.getProperty("file.encoding"))));
//					B.savePos(fo);
//				}
//				fo.close();
//			}
//			catch (IOException ex)
//			{
//				new Message(this, Global.resourceString("Write_error_") + "\n"
//						+ ex.toString());
//				return;
//			}
		}
		else if ("Save_Bitmap".equals(o)) // save the
			// game
		{ // File dialog handling
//			FileDialog fd = new FileDialog(this, Global
//					.resourceString("Save_Bitmap"), FileDialog.SAVE);
//			if ( !Dir.equals("")) fd.setDirectory(Dir);
//			String s = B.firstnode().getaction("GN");
//			if (s != null && !s.equals(""))
//				fd.setFile(s + "." + Global.getParameter("extension", "bmp"));
//			else fd.setFile("*." + Global.getParameter("extension", "bmp"));
//			fd.setFilenameFilter(this);
//			center(fd);
//			fd.setVisible(true);
//			String fn = fd.getFile();
//			if (fn == null) return;
//			Dir = fd.getDirectory();
//			try
//			// print out using the board class
//			{
//				BMPFile F = new BMPFile();
//				Dimension d = B.getBoardImageSize();
//				F.saveBitmap(fd.getDirectory() + fn, B.getBoardImage(),
//						d.width, d.height);
//			}
//			catch (Exception ex)
//			{
//				new Message(this, Global.resourceString("Write_error_") + "\n"
//						+ ex.toString());
//				return;
//			}
		}
		else if ("Load".equals(o)) // load a game
		{ // File dialog handling
		
			String fn = "toto.sgf";
			if (fn == null) return;
			Dir = "./";
			try
			// print out using the board class
			{
			
				{
					BufferedReader fi;
					
					fi = new BufferedReader(new InputStreamReader(
							new FileInputStream(fn),"UTF-8"));
					try
					{
						B.load(fi);
					}
					catch (IOException e)
					{
					}
					fi.close();
				}
			}
			catch (IOException ex)
			{
				return;
			}
			String s = B.firstnode().getaction("GN");
			{
				B.firstnode().setaction("GN", FileName.purefilename(fn));
			}
			if (fn.toLowerCase().indexOf("kogo") >= 0)
				B.setVariationStyle(false, false);
		}
	}


	// This can be used to set a board position
	// The board is updated directly, if it is at the
	// last move.
	/** set a black move at i,j */
	public void black (int i, int j)
	{
		B.black(i, j);
	}

	/** set a white move at i,j */
	public void white (int i, int j)
	{
		B.white(i, j);
	}

	/** set a black stone at i,j */
	public void setblack (int i, int j)
	{
		B.setblack(i, j);
	}

	/** set a black stone at i,j */
	public void setwhite (int i, int j)
	{
		B.setwhite(i, j);
	}

	/** mark the field at i,j as territory */
	public void territory (int i, int j)
	{
		B.territory(i, j);
	}

	/** Next to move */
	public void color (int c)
	{
		if (c == -1)
			B.white();
		else B.black();
	}

	/**
	 * Called from board to check the proper menu for markers.
	 * 
	 * @param i
	 *            the number of the marker type.
	 */
	public void setMarkState (int i)
	{
	
	}

	/**
	 * Called from board to enable and disable navigation buttons.
	 * 
	 * @param i
	 *            the number of the button
	 * @param f
	 *            enable/disable the button
	 */
	public void setState (int i, boolean f)
	{
		switch (i)
		{
		case 1:
			break;
		case 2:
			break;
		case 3:
			break;
		case 4:
			break;
		case 5:
			break;
		case 6:
			break;
		case 7:
			break;
		}
	}

	/** tests, if a name is accepted as a SGF file name */
	public boolean accept (File dir, String name)
	{
		if (name.endsWith(".sgf"))
			return true;
		else return false;
	}

	/**
	 * Called from the edit marker label dialog, when its text has been entered
	 * by the user.
	 * 
	 * @param s
	 *            the marker to be used by the board
	 */
	void setTextmark (String s)
	{
		B.textmark(s);
	}

	/** A blocked board cannot react to the user. */
	public boolean blocked ()
	{
		return false;
	}

	// The following are used from external board
	// drivers to set stones, handicap etc. (like
	// distributors for IGS commands)
	/** see, if the board is already acrive */
	public void active (boolean f)
	{
		B.active(f);
	}

	/** pass the Board */
	public void pass ()
	{
		B.pass();
	}

	public void setpass ()
	{
		B.setpass();
	}

	/** Notify about pass */
	public void notepass ()
	{}

	/**
	 * Set a handicap to the Board.
	 * 
	 * @param n
	 *            number of stones
	 */
	public void handicap (int n)
	{
		B.handicap(n);
	}

	/** set a move at i,j (called from Board) */
	public boolean moveset (int i, int j)
	{
		return true;
	}

	/** pass (only proceeded from ConnectedGoFrame) */
	public void movepass ()
	{}

	/**
	 * Undo moves on the board (called from a distributor e.g.)
	 * 
	 * @param n
	 *            numbers of moves to undo.
	 */
	public void undo (int n)
	{
		B.undo(n);
	}

	/** undo (only processed from ConnectedGoFrame) */
	public void undo ()
	{}

	/**
	 * Called from the BoardsizeQuestion dialog.
	 * 
	 * @param s
	 *            the size of the board.
	 */
	public void doboardsize (int s)
	{
		B.setsize(s);
	}

	/**
	 * Determine the board size (for external purpose)
	 * 
	 * @return the board size
	 */
	public int getboardsize ()
	{
		return B.getboardsize();
	}

	/** add a comment to the board (called from external sources) */
	public void addComment (String s)
	{
		B.addcomment(s);
	}

	public void result (int b, int w)
	{}

	public void yourMove (boolean notinpos)
	{}

	InputStreamReader LaterLoad = null;
	boolean LaterLoadXml;
	int LaterMove = 0;
	String LaterFilename = "";
	boolean Activated = false;

	/**
	 * Note that the board must only load a file, when it is ready. This is to
	 * interpret a command line argument SGF filename.
	 */
	public synchronized void load (String file, int move)
	{
		LaterFilename = FileName.purefilename(file);
		LaterMove = move;
		try
		{
			if (file.endsWith(".xml"))
			{
				LaterLoad = new InputStreamReader(new FileInputStream(file),
						"UTF8");
				LaterLoadXml = true;
			}
			else
			{
				LaterLoad = new InputStreamReader(new FileInputStream(file));
				LaterLoadXml = false;
			}
		}
		catch (Exception e)
		{
			LaterLoad = null;
		}
		if (LaterLoad != null && Activated) activate();
	}

	public void load (String file)
	{
		load(file, 0);
	}

	/**
	 * Note that the board must load a file, when it is ready. This is to
	 * interpret a command line argument SGF filename.
	 */
	public void load (URL file)
	{
		LaterFilename = file.toString();
		try
		{
			if (file.toExternalForm().endsWith(".xml"))
			{
				LaterLoad = new InputStreamReader(file.openStream(), "UTF8");
				LaterLoadXml = true;
			}
			else
			{
				LaterLoad = new InputStreamReader(file.openStream());
				LaterLoadXml = false;
			}
		}
		catch (Exception e)
		{
			LaterLoad = null;
		}
	}

	/** Actually do the loading, when the board is ready. */
	public void doload (Reader file)
	{
		try
		{
			B.load(new BufferedReader(file));
			file.close();
			B.gotoMove(LaterMove);
		}
		catch (Exception ex)
		{
		}
	}

	/** Actually do the loading, when the board is ready. */
	public void doloadXml (Reader file)
	{
		try
		{
			XmlReader xml = new XmlReader(new BufferedReader(file));
			B.loadXml(xml);
			file.close();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public synchronized void activate ()
	{
		Activated = true;
		if (LaterLoad != null)
		{
			if (LaterLoadXml)
				doloadXml(LaterLoad);
			else doload(LaterLoad);
		}
		LaterLoad = null;
	}

	/** Repaint the board, when color or font changes. */
	public void updateall ()
	{
		B.updateboard();
	}

	/**
	 * Sets the name of the Board (called from a Distributor)
	 * 
	 * @see jagoclient.igs.Distributor
	 */
	public void setname (String s)
	{
		B.setname(s);
	}

	/**
	 * Remove a group at i,j in the board.
	 */
	public void remove (int i, int j)
	{
		B.remove(i, j);
	}

	public boolean boardShowing ()
	{
		return Show;
	}

	public boolean lastNumber ()
	{
		return LastNumber;
	}

	public boolean showTarget ()
	{
		return ShowTarget;
	}
	// interface routines for the BoardInterface

	@Override
	public boolean bwColor() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean blackOnly() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setLabelM(String s) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLabel(String s) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void advanceTextmark() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setState(int n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getComment() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setComment(String s) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void appendComment(String s) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean askUndo() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean askInsert() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String resourceString(String S) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getParameter(String S, boolean f) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String version() {
		// TODO Auto-generated method stub
		return null;
	}

}
