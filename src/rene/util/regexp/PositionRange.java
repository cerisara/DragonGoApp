package rene.util.regexp;

/**
This is to mark the found ranges.
*/

public class PositionRange
{	int Start,End;	
	public PositionRange (int start, int end)
	{	Start=start; End=end;
	}
	public int start () { return Start; }
	public int end () { return End; }
}
