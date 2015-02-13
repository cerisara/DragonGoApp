package rene.util.parser;

import java.util.Vector;

/**
This is a string simple parser.
*/

public class StringParser
{	char C[];
	int N,L;
	boolean Error;
	
	/**
	@param S the string to be parsed.
	*/
	public StringParser (String S)
	{	C=S.toCharArray();
		N=0;
		L=C.length;
		Error=(N>=L);
	}

	/** 
	@return next character is white?
	*/
	public boolean blank ()
	{	return (C[N]==' ' || C[N]=='\t' || C[N]=='\n' || C[N]=='\r');
	}

	/** 
	@return next character is white or c?
	*/
	public boolean blank (char c)
	{	return (C[N]==' ' || C[N]=='\t' || C[N]=='\n' || C[N]=='\r' || C[N]==c);
	}

	/** 
	Cut off the String upto a character c.
	*/
	public String upto (char c)
	{	if (Error) return "";
		int n=N;
		while (n<L && C[n]!=c) n++;
		if (n>=L) Error=true;
		String s=new String(C,N,n-N);
		N=n;
		return s;
	}

	/**
	Advance one character.
	@return String is not empty.
	*/
	public boolean advance ()
	{	if (N<L) N++;
		if (N>=L) Error=true;
		return !Error;
	}
	
	/**
	Parse a word up to a blank.
	*/
	public String parseword ()
	{	if (Error) return "";
		while (blank())
		{	if (!advance()) return "";
		}
		int n=N;
		while (!Error && !blank()) advance();
		return new String(C,n,N-n);
	}

	/**
	Parse digits upto the character c or blank.
	*/
	public String parsedigits (char c)
	{	if (Error) return "";
		while (blank())
		{	if (!advance()) return "";
		}
		int n=N;
		while (!Error && !blank())
		{	if (N>L || C[N]<'0' || C[N]>'9' || C[N]==c) break;
			advance();
		}
		return new String(C,n,N-n);
	}

	/**
	Parse digits upto a blank.
	*/
	public String parsedigits ()
	{	if (Error) return "";
		while (blank())
		{	if (!advance()) return "";
		}
		int n=N;
		while (!Error && !blank())
		{	if (N>L || C[N]<'0' || C[N]>'9') break;
			advance();
		}
		return new String(C,n,N-n);
	}

	/**
	Parse a word upto the character c or blank.
	*/
	public String parseword (char c)
	{	if (Error) return "";
		while (blank())
		{	if (!advance()) return "";
		}
		int n=N;
		while (!Error && !blank(c)) advance();
		return new String(C,n,N-n);
	}

	/**
	@return next character is a digit?
	*/
	public boolean isint ()
	{	if (Error) return false;
		return (C[N]>='0' && C[N]<='9');
	}

	/**
	Parse an int upto a blank. The int may be negative.
	@return the int
	*/
	public int parseint ()
	{	int sig=1;
		try
		{	skipblanks(); if (Error) return 0;
			if (C[N]=='-')
			{	sig=-1;
				N++;
				if (N>L) { Error=true; return 0; }
			}
			return sig*Integer.parseInt(parsedigits(),10);
		}
		catch (NumberFormatException e)
		{	return 0;
		}
	}

	/**
	Parse an int upto a blank or c. The int may be negative.
	@return the int
	*/
	public int parseint (char c)
	{	int sig=1;
		try
		{	skipblanks(); if (Error) return 0;
			if (C[N]=='-')
			{	sig=-1;
				N++;
				if (N>L) { Error=true; return 0; }
			}
			return sig*Integer.parseInt(parsedigits(c),10);
		}
		catch (NumberFormatException e)
		{	return 0;
		}
	}

	/**
	Skip all white characters.
	*/
	public void skipblanks ()
	{	if (Error) return;
		while (blank())
			if (!advance()) break;
	}

	/**
	Skip everything to the string s.
	@return String was found
	*/
	public boolean skip (String s)
	{	if (Error) return false;
		int l=s.length();
		if (N+l>L) return false;
		if (!new String(C,N,l).equals(s)) return false;
		N+=l;
		if (N>=L) Error=true;
		return true;
	}

	/**
	Get the next character.
	*/
	public char next ()
	{	if (Error) return ' ';
		else
		{	N++;
			if (N>=L) { Error=true; }
			return C[N-1];
		}
	}

	/**
	Return a String, which is parsed from words with limited length.
	@param columns the maximal length of the string
	*/
	public String wrapline (int columns)
	{	int n=N,good=N;
		String s="";
		while (n<L)
		{	if (C[n]=='\n')
			{	if (n>N) s=new String(C,N,n-N);
				N=n+1;
				break;
			}
			if (C[n]==' ' || C[n]=='\t' || C[n]=='\n')
			{	good=n;
			}
			n++;
			if (n-N>=columns && good>N)
			{	s=new String(C,N,good-N);
				N=good+1;
				break;
			}
			if (n>=L)
			{	if (n>N) s=new String(C,N,n-N);
				N=n;
				break;
			}
		}
		if (N>=L) Error=true;
		return s;
	}

	/**
	Parse the string into lines.
	@param columns the maximal length of each line
	@return a Vector with lines
	*/
	public Vector<String> wraplines (int columns)
	{	Vector v=new Vector(10,10);
		String s;
		while (!Error)
		{	s=wrapline(columns);
			v.addElement(s);
		}
		return v;
	}

	public String wraplineword (int columns)
	{	int n=N,good=N;
		String s="";
		while (n<L)
		{	if (C[n]=='\n')
			{	s=new String(C,N,n-N);
				N=n+1;
				break;
			}
			n++;
			if (n>=L)
			{	if (n>N) s=new String(C,N,n-N);
				N=n;
				break;
			}
			if (n-N>=columns && good>N)
			{	s=new String(C,N,good-N);
				N=good+1;
				if (N<L && C[N]!='\n') s=s+"\\";
				break;
			}
		}
		if (N>=L) Error=true;
		return s;
	}

	public Vector wrapwords (int columns)
	{	Vector v=new Vector(10,10);
		String s;
		while (!Error)
		{	s=wraplineword(columns);
			v.addElement(s);
		}
		return v;
	}

	/**
	Replace c1 by c2
	*/
	public void replace(char c1, char c2)
	{	for (int i=0; i<L; i++)
			if (C[i]==c1) C[i]=c2;
	}

	/**
	@return if an error has occured during the parsing.
	*/
	public boolean error () { return Error; }
}
