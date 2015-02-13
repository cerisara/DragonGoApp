package fr.xtof54.sgfsearch;

/**
 * Encorporated the board into an environment (such as GoFrame).
 */

public interface BoardInterface
{
	public boolean boardShowing ();

	// board displays already or not

	public void activate ();

	// board is painted and displayed for the first time

	// Various Color settings:
	public boolean bwColor (); // black and white only?

	public boolean blackOnly ();

	public boolean blocked ();

	// blocks changed at the end of main variation

	// Board sets two labels, which may be used in a frame
	public void setLabelM (String s); // position of cursor

	public void setLabel (String s); // next move prompt

	public void advanceTextmark ();

	// request setting of the next textmark A,B,C,... or 1,2,3,...

	public void setState (int n, boolean flag);

	// enable/disable navigation buttons
	// 1=left, 2=right, 3=varup, 4=varmain, 5=down, 6=up, 7=main

	public void setState (int n);

	// check the menu item for the current state
	// 1=black, 2=white, 3=setblack, 4=setwhite,
	// 5=mark, 6=letter, 7=hide, 10=textmark

	public void setMarkState (int marker);

	// enable the correct marker item
	// marker is from FIELD.SQUARE etc.

	// Comment area:
	public String getComment ();

	// get the content of the comment area
	public void setComment (String s);

	// set the comment area to that string
	public void appendComment (String s);

	// append something to the comment area only
	public void addComment (String s);

	// used to notify that board did "Pass" and "Undo"
	// usually should call Board.addcomment()

	// get flags:
	public boolean showTarget (); // flag for target rectangle

	public boolean lastNumber (); // flag to show last number

	public boolean askUndo ();

	// should open an "Delete Moves" modal dialog and return,
	// if undo was allowed

	public boolean askInsert ();

	// should open an "Change Game Tree" modal dialog and return,
	// if the node insertion was allowed

	public void yourMove (boolean f);

	// called if a move was received at end of main variation,
	// but current position is not visible

	public void result (int b, int w);

	// sends the result of a game back from the done function
	// when counted at the end of the main tree.

	public String resourceString (String S);

	// translate the Resource for me
	// check Board.java for necessary translations

	public boolean getParameter (String S, boolean f);

	// get a named parameter with Color value
	// check Board.java for necessary parameters
	// default is the given color

	public String version ();

	// return the program version for SGF versioning
}
