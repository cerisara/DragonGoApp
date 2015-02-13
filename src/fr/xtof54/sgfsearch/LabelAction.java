package fr.xtof54.sgfsearch;

import java.io.PrintWriter;

import rene.util.list.ListElement;

/**
This action class takes special care to print labels in SGF form.
Jago notes labes in consecutive letters, but SGF does not have this
feature, thus it outputs labels as LB[field:letter].
*/

public class LabelAction extends Action
{	BoardInterface GF;
	public LabelAction (String arg, BoardInterface gf)
	{	super("L",arg);
		GF=gf;
	}
	public LabelAction (BoardInterface gf)
	{	super("L");
		GF=gf;
	}
	public void print (PrintWriter o)
	{	if (GF.getParameter("puresgf",false))
		{	o.println();
			o.print("LB");
			char[] c=new char[1];
			int i=0;
			ListElement p=Arguments.first();
			while (p!=null)
			{	c[0]=(char)('a'+i);
				o.print("["+(String)(p.content())+":"+new String(c)+"]");
				i++;
				p=p.next();
			}
		}
		else super.print(o);
	}
}
