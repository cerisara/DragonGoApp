package fr.xtof54.sgfsearch;

/**
Holds position changes at one field.
*/

public class Change
{	public int I,J,C;
	public int N;
	/**
	Board position i,j changed from c.
	*/
	public Change (int i, int j, int c, int n)
	{	I=i; J=j; C=c; N=n;
	}
	public Change (int  i, int j, int c)
	{	this(i,j,c,0);
	}
}
