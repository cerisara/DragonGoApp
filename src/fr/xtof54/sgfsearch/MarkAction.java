package fr.xtof54.sgfsearch;

import java.io.PrintWriter;

import rene.util.list.ListElement;

/**
This is a special action for marks. It will print it content
depending on the "puresgf" parameter. This is because the
new SGF format no longer allows the "M" tag.
@see jagoclient.board.Action
*/

public class MarkAction extends Action
{	BoardInterface GF;
	public MarkAction (String arg, BoardInterface gf)
	{	super("M",arg);
		GF=gf;
	}
	public MarkAction (BoardInterface gf)
	{	super("M");
		GF=gf;
	}
	public void print (PrintWriter o)
	{	if (GF.getParameter("puresgf",false))
		{	o.println(); o.print("MA");
			ListElement p=Arguments.first();
			while (p!=null)
			{	o.print("["+(String)(p.content())+"]");
				p=p.next();
			}
		}
		else super.print(o);
	}
}
