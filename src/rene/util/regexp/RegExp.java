package rene.util.regexp;

import java.util.*;

/**
An exception for the scanning of the regular expression.
*/

class RegExpException extends Exception
{	int pos;
	String S;
	public RegExpException (String s, int p)
	{	super(s);
		S=s;
		pos=p;
	}
	public RegExpException (String s)
	{	super(s);
		pos=0;
	}
	public String string () { return S; }
	public int pos () { return pos; }
}

/**
Holds a position in a line of characters. This is used to store
the new postion too, so that matches can advance the position
and return the match state.
*/

class Position
{	public char A[];
	public int K,N;
	public Position (char a[])
	{	A=a; K=0; N=A.length;
	}
	public char get () { return A[K]; }
	public boolean end () { return K>=N; }
	public void advance () { K++; }
	public void advance (int i) { K+=i; }
	public void pos (int k) { K=k; }
	public int pos () { return K; }
	public int length () { return N; }
}

/**
An atom is a single letter a dot, or a range. It has a
multiplication state *, + or ?. The atom can scan
itself from the regular expression, advancing the scan
position, and it can match itself against a sting,
advancing the search position. If asked, it can find
a second match or say that there is none. In the first
case the position needs to be restored to the end
of the matched string.
*/

class Atom
{	/** The regular expression this atom belopngs to */	
	RegExp R;
	/** The multiplicator states */
	final static int mult1=0,mult01=1,mult12=2,mult012=3;
	/** The state of this atom */
	int Mult;
	/** Match position and end of the matching string. */
	int LastMatch,MatchEnd;
	/** Place to store the position, which must be restored for nextMatch. */
	Position P;
	/** There might be a nextMatch() */
	boolean Match;
	public Atom (RegExp r)
	{	R=r;
		Mult=mult1;
	}
	/**
	Scan yourself from the regular expression
	and advance the position.
	@return Success or failure.
	*/
	public boolean scan (Position p) throws RegExpException
	{	return false;
	}
	/**
	Does the position match? Find the longest
	match first.
	@return Success or failure.
	*/
	public boolean match (Position p)
	{	return false;
	}
	/**
	Is there another match? Restore the position,
	if there is.
	*/
	public boolean nextMatch ()
	{	return false;
	}
	/**
	Scan the multiplicator item behind the atom.
	*/
	public void scanMult (Position p)
	{	if (!p.end())
		{	switch (p.get())
			{	case '*' : Mult=mult012; break;
				case '+' : Mult=mult12; break;
				case '?' : Mult=mult01; break;
				default : return;
			}
			p.advance();
		}
	}
	/**
	Note the position structure and the current position.
	Set Match initality to false.
	*/
	public void notePosition (Position p)
	{	P=p; LastMatch=P.pos(); Match=false;
	}
	/**
	Search for one or more repetitions.
	*/
	public boolean canMultiple () { return Mult==mult012 || Mult==mult12; }
	/**
	Satisfied with zero strings or not.
	*/
	public boolean canVoid () { return Mult==mult012 || Mult==mult01; }
}

/**
This is an atom, which is capable of finding the longest match
and upon request by nextMatch() shorter matches.
*/

class Simple
	extends Atom
{	public Simple (RegExp r)
	{	super(r);
	}
	public boolean match (Position p)
	{	notePosition(p);
		if (!p.end() && matchSimple(p))
		{	p.advance();
			Match=true;
			if (canMultiple())
			{	while (!p.end() && matchSimple(p)) p.advance();
			}
			MatchEnd=p.pos();
			return true;
		}
		else
		{	if (canVoid()) return true;
			else return false;
		}
	}
	public boolean nextMatch ()
	{	if (!Match) return false;
		MatchEnd--;
		if (MatchEnd<LastMatch || (MatchEnd==LastMatch && !canVoid()))
		{	Match=false;	
			return false;
		}
		P.pos(MatchEnd);
		Match=true;
		return true;
	}
	/**
	Override this to get useful matches.
	@return The singe character in the position matches this atom.
	*/
	public boolean matchSimple (Position p)
	{	return false;
	}
}

/**
A single character match.
*/

class Char
	extends Simple
{	char C;	
	public Char (RegExp r)
	{	super(r);
	}
	public boolean scan (Position p) throws RegExpException
	{	C=p.get();
		p.advance();
		scanMult(p);
		return true;
	}
	public boolean matchSimple (Position p)
	{	return (R.uppercase(p.get())==C);
	}
}

class SpecialChar
	extends Char
{	public SpecialChar (RegExp r, char c)
	{	super(r);
		C=c;
	}
	public boolean scan (Position p) throws RegExpException
	{	p.advance();
		scanMult(p);
		return true;
	}
}

/**
Matches any character.
*/

class Dot
	extends Simple
{	public Dot (RegExp r)
	{	super(r);
	}
	public boolean scan (Position p) throws RegExpException
	{	p.advance();
		scanMult(p);
		return true;
	}
	public boolean matchSimple (Position p)
	{	return true;
	}	
}

/**
Holds one of the ranges in a character range, or a single character.
The range may include or exclude.
*/

class RangeClass
{	boolean Exclude;
	public RangeClass (boolean exclude)
	{	Exclude=exclude;
	}
	public boolean isExclude () { return Exclude; }
	public boolean inRange (char c) { return false; }
}

class CharRange extends RangeClass
{	int Min,Max;
	public CharRange (int min, int max, boolean exclude)
	{	super(exclude);
		Min=min; Max=max;
	}
	public boolean inRange (char c) { return c>=Min && c<=Max; }
}

class AlphaRange extends RangeClass
{	public AlphaRange (boolean exclude)
	{	super(exclude);
	}
	public boolean inRange (char c) { return Character.isLetter(c); }
}

class AlphaNumericRange extends RangeClass
{	public AlphaNumericRange (boolean exclude)
	{	super(exclude);
	}
	public boolean inRange (char c) { return Character.isLetterOrDigit(c); }
}

class NumericRange extends RangeClass
{	public NumericRange (boolean exclude)
	{	super(exclude);
	}
	public boolean inRange (char c) { return Character.isDigit(c); }
}

class ControlRange extends RangeClass
{	public ControlRange (boolean exclude)
	{	super(exclude);
	}
	public boolean inRange (char c) { return Character.isISOControl(c); }
}

class LowerRange extends RangeClass
{	public LowerRange (boolean exclude)
	{	super(exclude);
	}
	public boolean inRange (char c) { return Character.isLowerCase(c); }
}

class UpperRange extends RangeClass
{	public UpperRange (boolean exclude)
	{	super(exclude);
	}
	public boolean inRange (char c) { return Character.isUpperCase(c); }
}

class SpaceRange extends RangeClass
{	public SpaceRange (boolean exclude)
	{	super(exclude);
	}
	public boolean inRange (char c) { return Character.isSpaceChar(c); }
}

class WhiteSpaceRange extends RangeClass
{	public WhiteSpaceRange (boolean exclude)
	{	super(exclude);
	}
	public boolean inRange (char c) { return Character.isWhitespace(c); }
}

/**
The Range class holds a vector of ranges, single characters, or named
ranges. All are subclasses of RangeClass.
*/

class Range
	extends Simple
{	Vector V;
	boolean Any;
	public Range (RegExp r)
	{	super(r);
		Any=true;
	}
	public boolean scan (Position p) throws RegExpException
	{	V=new Vector();	
		boolean exclude=false;
		p.advance();
		while (!p.end() && p.get()!=']')
		{	if (p.get()=='^')
			{	exclude=true; p.advance();
			}
			if (!exclude) Any=false;
			char a=getNext(p);
			if (a=='[') scanNamedRange(p,exclude);
			else
			{	char b=a;
				if (p.get()=='-')
				{	p.advance();
					b=getNext(p);
				}
				V.addElement(new CharRange(a,b,exclude));
			}
		}
		if (p.end() || p.get()!=']')
			throw new RegExpException("bracket.range",p.pos());
		p.advance();
		scanMult(p);
		return true;
	}
	public void scanNamedRange (Position p, boolean exclude) 
		throws RegExpException
	{	if (getNext(p)!=':')
			throw new RegExpException("bracket.namedrange",p.pos());
		StringBuffer b=new StringBuffer();
		while (true)
		{	char a=getNext(p);
			if (a==':') break;
			b.append(a);
		}
		if (getNext(p)!=']')
			throw new RegExpException("bracket.namedrange",p.pos());
		String s=b.toString();
		if (s.equals("alpha")) V.addElement(new AlphaRange(exclude));
		else if (s.equals("digit")) V.addElement(new NumericRange(exclude));
		else if (s.equals("alnum")) V.addElement(new AlphaNumericRange(exclude));
		else if (s.equals("cntrl")) V.addElement(new ControlRange(exclude));
		else if (s.equals("lower")) V.addElement(new LowerRange(exclude));
		else if (s.equals("upper")) V.addElement(new UpperRange(exclude));
		else if (s.equals("space")) V.addElement(new SpaceRange(exclude));
		else if (s.equals("white")) V.addElement(new WhiteSpaceRange(exclude));
		else
			throw new RegExpException("bracket.namedrange",p.pos());
	}
	/**
	Get the next position and scan \] etc. correctly.
	@return 0 on failure.
	*/
	public char getNext (Position p) throws RegExpException
	{	if (p.end()) throw new RegExpException("bracket.range");
		char c=p.get();
		if (c=='\\')
		{	p.advance();
			if (p.end())
				throw new RegExpException("illegal.backslash",p.pos());
			c=p.get();
			if (c=='t') c=(char)9;
		}
		p.advance();
		if (p.end())
			throw new RegExpException("bracket.range",p.pos());
		return c;
	}
	/**
	Walk through the vector of ranges and set the range.
	*/
	public boolean matchSimple (Position p)
	{	boolean match=Any;	
		for (int i=0; i<V.size(); i++)
		{	RangeClass r=(RangeClass)V.elementAt(i);
			if (r.isExclude())
			{	if (r.inRange(R.uppercase(p.get()))) return false;
			}
			else
			{	if (r.inRange(R.uppercase(p.get()))) match=true;
			}	
		}
		return match;
	}
}

/**
This scans and matches (expression).
*/

class Bracket
	extends Atom
{	Part P;	
	boolean Top;
	int EN;
	Position Pos;
	int K;
	public Bracket (RegExp r, boolean top)
	{	super(r); Top=top;
		EN=r.EN;
		r.EN++;
	}
	public boolean scan (Position p) throws RegExpException
	{	p.advance();
		P=new Part(R,false);
		P.scan(p);
		if (p.end() || p.get()!=')')
			throw new RegExpException("round.bracket",p.pos());
		p.advance();
		return true;
	}
	public boolean match (Position p)
	{	Pos=p;	
		K=p.pos();
		boolean result=P.match(p);
		if (result && Top)
			R.E.insertElementAt(new PositionRange(K,p.pos()),EN);
		return result;
	}
	public boolean nextMatch ()
	{	boolean result=P.nextMatch();
		if (result && Top)
			R.E.insertElementAt(new PositionRange(K,Pos.pos()),EN);
		return result;
	}
}

/**
Pos matches the nullstring at a specified position.
*/

class Pos
	extends Atom
{	int P;
	public Pos (RegExp r, int pos)
	{	super(r);	
		P=pos;
	}
	public boolean scan (Position p)
	{	p.advance();
		return true;
	}
	public boolean match (Position p)
	{	if (P>=0) return (p.pos()==P);
		else return (p.pos()==p.length()+P+1);
	}
}

class Previous
	extends Atom
{	int P;
	public Previous (RegExp r, int p)
	{	super(r);
		P=p;
	}
	public boolean scan (Position p)
	{	p.advance();
		return true;
	}
	public boolean match (Position p)
	{	try
		{	String s=R.getBracket(P);
			if (s==null) return false;
			char a[]=s.toCharArray();
			for (int i=0; i<a.length; i++)
			{	if (p.end() || a[i]!=p.get()) return false;
				p.advance();
			}
			return true;
		}
		catch (Exception e) { return false; }
	}
}

/**
Branches are |ed to get a regular expression. Each branch
consists of a sequence of atoms.
*/

class Branch
{	RegExp R;
	Vector V;
	boolean Top;
	public Branch (RegExp r, boolean top)
	{	R=r; Top=top;
		V=new Vector();
	}
	/**
	Scan for atoms.
	The atoms are recognized by their first letter.
	*/
	public boolean scan (Position p) throws RegExpException
	{	while (!p.end())
		{	char c=p.get();	
			Atom a;
			switch(c)
			{	case '.' :
					a=new Dot(R);
					break;
				case '\\' :
					p.advance();
					if (!p.end())
					{	switch (p.get())
						{	case 't' :
								a=new SpecialChar(R,(char)9); break;
							default :
								if (p.get()>='0' && p.get()<='9')
									a=new Previous(R,p.get()-'0');
								else 
									a=new Char(R);
								break;
						}
					}
					else throw new RegExpException("illegal.escape",p.pos());
					break;
				case '[' :
					a=new Range(R);
					break;
				case '(' :
					a=new Bracket(R,Top);
					break;
				case '|' :
					return true;
				case ')' :
					return true;
				case '^' :
					a=new Pos(R,0);
					break;
				case '$' :
					a=new Pos(R,-1);
					break;
				default :
					a=new Char(R);
					break;
			}
			a.scan(p);
			V.addElement(a);
		}	
		return V.size()>0;
	}
	public boolean match (Position p)
	{	return match(p,0);
	}
	/**
	The match is done by crawling through the atoms recursively.
	The atom i is asked for another match, until everything fails.
	*/
	public boolean match (Position p, int i)
	{	if (i>=V.size()) return false;
		if (i+1>=V.size()) 
		{	Atom a=(Atom)V.elementAt(i);	
			return a.match(p);
		}
		else
		{	Atom a=(Atom)V.elementAt(i);	
			if (a.match(p)) 
			{	if (match(p,i+1)) return true;
				else
				{	while (a.nextMatch())
					{	if (match(p,i+1)) return true;
					}
					return false;
				}
			}
			else return false;
		}
	}
	/**
	Search for another match.
	*/
	public boolean nextMatch ()
	{	return nextMatch(0);
	}
	public boolean nextMatch (int i)
	{	if (i>=V.size()) return false;
		if (i+1>=V.size()) 
		{	Atom a=(Atom)V.elementAt(i);	
			return a.nextMatch();
		}
		else
		{	Atom a=(Atom)V.elementAt(i);	
			if (a.nextMatch()) 
			{	if (nextMatch(i+1)) return true;
				else
				{	while (a.nextMatch())
					{	if (nextMatch(i+1)) return true;
					}
					return false;
				}
			}
			else return false;
		}
	}
}

/**
A part is expression|part or a single expression.
*/

class Part
{	RegExp R;
	Branch Left;
	Part Right;
	boolean Top;
	int EN;
	public Part (RegExp r, boolean top)
	{	R=r; Top=top;
	}
	public boolean scan (Position p) throws RegExpException
	{	if (Top) R.EN=0;
		Left=new Branch(R,Top);
		Left.scan(p);
		if (Top) EN=R.EN;
		if (!p.end() && p.get()=='|')
		{	if (Top) R.EN=0;
			Right=new Part(R,Top);
			p.advance();
			return Right.scan(p);
		}
		return true;
	}
	/**
	The match is true if the first part is true.
	or the remaining parts are true.
	*/
	public boolean match (Position p)
	{	int k=p.pos();
		if (Top)
		{	R.E.removeAllElements();
			R.EN=0;
		}
		if (Left.match(p))
		{	if (Top) R.EN=EN;	
			return true;
		}
		else
		{	p.pos(k);
			if (Right!=null) return Right.match(p);
			else return false;
		}
	}
	/**
	This tests for another match of any sub-branch. 
	*/ 
	public boolean nextMatch () 
	{	if (Left.nextMatch()) return true;
		else 
		{	if (Right!=null) return Right.nextMatch();
		}
		return false;
	}
}


/**
This is a class to scan a string with a regular expression. It follows
the normal rules for regular expressions with some exceptions and
extensions. Any instance of this class can perform as a match tool for
input strings, using the match method.
<p>
Here is a formal description of a regular expression. It is has one or
more branches, separated by |. It matches anything, that matches one
of the branches.
<p>
A branch has one or more atoms concatenated. It matches the string, if
the atoms match strings, which concatenate to the given string.
<p>
An atom is either a string of non-special letters. A special letter
(such as |) becomes a non-special letter, if it is preceded by \. Or
an atom is a regular expression in (). Or it is a sequence of letters
in [] (a range). Or it is a . indicating any character.
<p>
An atom followed by * may repeat zero or more times, followed by + one
or more times, followed by ? zero or one times.
<p>
A range consists of letters, ranges of letters as in A-Z, or a ^
character, indicating that the letters or letter ranges are excluded.
The special letters must be preceded by \.
<p>
There are the predefined ranges [:alpha:], [:digit:], [:alnum:],
[:space:], [:white:], [:cntrl:], [:lower:] and [:upper:]. Note that
the brackes are part of the range definition.
<p>
Contrary to the normal implementation, ] and - must be escaped, when
they are to appear in ranges. Also, ranges may contain include and
exclude character ranges at the same time, as in [a-z^x].
<p> 
The atom ^ matches only the beginning of the line, while $ matches the
line end. 
*/

public class RegExp
{	/** store the regular expression string here */	
	String S;
	/** the regular expression scanned tree */
	Part Left;
	/** the Valid flag */
	boolean Valid=false;
	/** the error string, if Valid is false */
	String ErrorString;
	/** the error position, if Valid is false */
	int Pos;
	/** the minimal length a string must have to match */
	int minLength=0;
	/** the found match */
	int StartMatch,EndMatch;
	/** a vector for the found expressions in brackets */
	Vector E;
	/** A counter to use for the brackets */
	int EN;
	/** Note the searched string here */
	char A[];
	/** Ignore case */
	boolean IgnoreCase=false;
	
	/**
	This scans a regular expression for further usage.
	The error flag may be checked with the valid()
	function.
	@param s The regular expression.
	*/
	public RegExp (String s, boolean ignorecase)
	{	if (ignorecase) s=s.toUpperCase();
		S=s;
		E=new Vector();
		IgnoreCase=ignorecase;
		char A[]=S.toCharArray();
		Position p=new Position(A);
		Left=new Part(this,true);
		ErrorString="";
		try
		{	Left.scan(p);
			Valid=true;
		}
		catch (RegExpException e)
		{	Valid=false;
			ErrorString=e.string();
			Pos=e.pos();
		}
		catch (Exception e)
		{	Valid=false;
			ErrorString="internal.error";
			Pos=0;
		}
	}
	
	/**
	Checks the error state for the regular expression.
	@return true, if there is no error.
	*/
	public boolean valid ()
	{	return Valid;
	}
	
	/**
	The error string tries to explain the error, when
	valid() return false.
	@return the error string
	*/
	public String errorString ()
	{	return ErrorString;
	}
	
	/**
	The position, where the scan error occured.
	@return the error position.
	*/
	public int errorPos ()
	{	return Pos;
	}
	
	/**
	Match the regular expression against a string.
	@return true, if a match was found.
	*/
	public boolean match (String s)
	{	char A[]=s.toCharArray();
		return match(A,0);
	}
	
	public boolean match (char a[], int pos)
	{	A=a;
		Position p=new Position(A);
		int i,n=A.length-minLength;
		for (i=pos; i<=n; i++)
		{	p.pos(i);
			if (Left.match(p)) 
			{	StartMatch=i;
				EndMatch=p.pos();
				return true;
			}
		}
		return false;
	}
	
	/**
	@return start position of matching string.
	*/
	public int startMatch ()
	{	return StartMatch;
	}
	
	/**
	@return end of the matching string.
	*/
	public int endMatch ()
	{	return EndMatch;
	}

	/**
	A main() to test the scanner.
	*/
	public static void main (String args[])
	{	RegExp R=new RegExp(args[0],false);
		if (R.Valid)
		{	if (R.match(args[1]))
			{	System.out.println("Matched from "+R.StartMatch+
					" to "+R.EndMatch);
				System.out.println(R.EN+" brackets assigned");
				for (int i=0; i<R.EN; i++)
				{	System.out.println(i+": "+R.expand("("+i+")"));
				}
			}
		}
		else System.out.println(R.ErrorString+" at "+R.Pos);
	}

	/**
	Return an enumeration with the found brackets. The
	objects are instances of the PositionRange class.
	@see rene.regexp.PositionRange
	*/	
	public Enumeration getBrackets ()
	{	return E.elements();
	}
	
	public int getBracketNumber ()
	{	return E.size();
	}
	
	public String getBracket (int i)
	{	try
		{	PositionRange r=(PositionRange)E.elementAt(i);
			return new String(A,r.start(),r.end()-r.start());
		}
		catch (Exception e)
		{	return null;
		}
	}
	
	/**
	Expand the replacement string and change (1), (2)
	etc. to the found bracket expansions.
	@return expanded string or null on error.
	*/
	public String expand (String s)
	{	try	
		{	StringBuffer B=new StringBuffer();
			s=s.replace("\\t","\t");
			StringTokenizer T=new StringTokenizer(s,"\\()",true);
			while (T.hasMoreTokens())
			{	String a=T.nextToken();
				if (a.equals("("))
				{	String b=T.nextToken();
					String c=T.nextToken();
					if (!c.equals(")")) return null;
					PositionRange p=
						(PositionRange)E.elementAt(Integer.parseInt(b));
					B.append(new String(A,p.start(),p.end()-p.start()));
				}
				else if (a.equals("\\"))
				{	a=T.nextToken();
					B.append(a);
				}
				else B.append(a);
			}
			return B.toString();
		}
		catch (Exception e)
		{	return null;
		}
	}
	
	char uppercase (char c)
	{	if (IgnoreCase) return Character.toUpperCase(c);
		else return c;
	}
}
