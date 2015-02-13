package rene.util.mail;

import java.util.*;

public class MailMessage
{	Vector V;
	public MailMessage ()
	{	V=new Vector();
	}
	public void addLine (String s)
	{	V.addElement(s);
	}
	public String find (String a)
	{	for (int i=0; i<V.size(); i++)
		{	String s=(String)V.elementAt(i);
			if (s.toLowerCase().startsWith(a))
				return s.substring(a.length()).trim();
		}
		return "???";
	}
	public String from ()
	{	return find("from:");
	}
	public String subject ()
	{	return find("subject:");
	}
	public String date ()
	{	return find("date:");
	}
	Enumeration getMessage ()
	{	return V.elements();
	}
}

