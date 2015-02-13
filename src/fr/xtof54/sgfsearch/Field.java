package fr.xtof54.sgfsearch;

// ************** Field **************

/**
A class to hold a single field in the game board.
Contains data for labels, numbers, marks etc. and
of course the color of the stone on the board.
<P>
It may contain a reference to a tree, which is a variation
starting at this (empty) board position.
<P>
The Mark field is used for several purposes, like marking
a group of stones or a territory.
*/

public class Field
{	int C; // the state of the field (-1,0,1), 1 is black
	boolean Mark; // for several purposes (see Position.markgroup)
	TreeNode T; // Tree that starts this variation
	int Letter; // Letter to be displayed
	String LabelLetter; // Strings from the LB tag.
	boolean HaveLabel; // flag to indicate there is a label
	int Territory; // For Territory counting
	int Marker; // emphasized field
	static final int NONE=0;
	static final int CROSS=1;
	static final int SQUARE=2;
	static final int TRIANGLE=3;
	static final int CIRCLE=4;
	int Number;

	//** set the field to 0 initially */
	public Field ()
	{	C=0;
		T=null;
		Letter=0;
		HaveLabel=false;
		Number=0;
	}

	/** return its state */
	int color ()
	{	return C;
	}

	/** set its state */
	void color (int c)
	{	C=c;
		Number=0;
	}
	
	final static int az='z'-'a';

	/** return a string containing the coordinates in SGF */
	static String string (int i, int j)
	{	char[] a=new char[2];
		if (i>=az) a[0]=(char)('A'+i-az-1);
		else a[0]=(char)('a'+i); 
		if (j>=az) a[1]=(char)('A'+j-az-1);
		else a[1]=(char)('a'+j);
		return new String(a);
	}

	/** return a string containing the coordinates in SGF */
	static String coordinate (int i, int j, int s)
	{	if (s>25)
		{	return (i+1)+","+(s-j);
		}
		else
		{	if (i>=8) i++;
			return ""+(char)('A'+i)+(s-j);
		}
	}

	/** get the first coordinate from the SGF string */
	static int i (String s)
	{	if (s.length()<2) return -1;
		char c=s.charAt(0);
		if (c<'a')  return c-'A'+az+1;
		return c-'a';
	}

	/** get the second coordinate from the SGF string */
	static int j (String s)
	{	if (s.length()<2) return -1;
		char c=s.charAt(1);
		if (c<'a')  return c-'A'+az+1;
		return c-'a';
	}

	// modifiers:
	void mark (boolean f) { Mark=f; } // set Mark
	void tree (TreeNode t) { T=t; } // set Tree
	void marker (int f) { Marker=f; }
	void letter (int l) { Letter=l; }
	void territory (int c) { Territory=c; }
	void setlabel (String s) { HaveLabel=true; LabelLetter=s; }
	void clearlabel () { HaveLabel=false; }
	void number (int n) { Number=n; }

	// access functions:
	boolean mark () { return Mark; } // ask Mark
	int marker () { return Marker; }
	TreeNode tree () { return T; }
	int letter () { return Letter; }
	int territory () { return Territory; }
	boolean havelabel () { return HaveLabel; }
	String label () { return LabelLetter; }
	int number() { return Number; }
}
