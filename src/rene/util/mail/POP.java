package rene.util.mail;

import java.io.*;
import java.net.*;
import java.util.*;

class POPException extends Exception
{	public POPException (String s)
	{	super(s);
	}
}

/**
Implements a POP3 client.
*/

public class POP
{	BufferedReader In;
	PrintWriter Out;
	Socket S;
	String Server;
	int Port;
	String Answer;
	int TotalSize;
	
	/**
	Open the connection to the server, default is port 110.
	*/
	public POP (String server, int port)
	{	Server=server; Port=port;
	}
	public POP (String server)
	{	this(server,110);
	}
	
	/**
	Open a connection and wait for a positive answer.
	*/
	public void open () throws IOException
	{	S=new Socket(Server,Port);
		In=new BufferedReader(new InputStreamReader(
			new DataInputStream(S.getInputStream())));
		Out=new PrintWriter(S.getOutputStream());
		if (!expectAnswer()) throw new IOException("Could not connect!");
	}
	
	/**
	Close the connection.
	*/
	public void close () throws IOException
	{	In.close();
		Out.close();
	}
	
	/**
	Log into the server.
	@return successful login.
	*/
	public boolean login (String user, String password)
		throws IOException
	{	send("USER "+user);
		if (!expectAnswer()) return false;
		send("PASS "+password);
		if (!expectAnswer()) return false;
		return true;
	}
	
	/**
	Get the number of messages and the total size. The size is
	stored in TotalSize.
	@return Number of messages.
	*/
	public int getNumberOfMessages ()
		throws IOException,POPException
	{	send("STAT");
		if (!expectAnswer()) throw new POPException("status");
		StringTokenizer s=new StringTokenizer(Answer," ");
		if (!s.hasMoreTokens()) throw new POPException("status");
		int n;
		try
		{	n=Integer.parseInt(s.nextToken());
		}
		catch (Exception e) { throw new POPException("status"); }
		if (!s.hasMoreTokens()) throw new POPException("status");
		try
		{	TotalSize=Integer.parseInt(s.nextToken());
		}		
		catch (Exception e) { throw new POPException("status"); }
		return n;
	}
	
	/**
	Get the message (header only) with number i.
	@param i Number of message
	*/
	public MailMessage getMessageHeader (int i)
		throws IOException,POPException
	{	send("TOP "+i+" "+0);
		if (!expectAnswer())
		{	send("RETR "+i);
			if (!expectAnswer())
				throw new POPException("retr");
		}
		return getMessageText();
	}
	
	MailMessage getMessageText ()
		throws IOException,POPException
	{	MailMessage m=new MailMessage();
		while (true)
		{	String s=In.readLine();
			if (s==null) throw new POPException("retr");
			if (s.equals(".")) break;
			m.addLine(s);
		}
		return m;
	}
	
	/**
	Get an answer from the server, ignore everything but +OK
	and -ERR. Store the answer string in Answer.
	@return True, if answer is +OK
	*/
	boolean expectAnswer () throws IOException
	{	while (true)
		{	String s=In.readLine();
			if (s==null) throw new IOException("Connection closed");
			if (s.startsWith("+OK"))
			{	Answer=stripAnswer(s,3);
				return true;
			}
			else if (s.startsWith("-ERR"))
			{	Answer=stripAnswer(s,4);
				return false;
			}			
		}
	}
	
	/**
	Send a text to the server.
	*/
	public void send (String s) throws IOException
	{	Out.println(s);
		Out.flush();
	}
	
	/**
	Strip the answer string.
	*/
	public String stripAnswer (String s, int pos)
	{	return s.substring(pos).trim();
	}
	
	/**
	Only available after getNumberOfMessages()!
	@return Total size of messages.
	*/
	public int getTotalSize ()
	{	return TotalSize;
	}
	
	/**
	@return The answer of the last command
	*/
	public String getAnswer ()
	{	return Answer;
	}

	/**
	Save the UDILs of the messages to the Hash table.
	*/
	public void saveUIDL (Hashtable h)
		throws POPException,IOException
	{	send("UIDL");
		if (!expectAnswer())
			throw new POPException("uidl");
		MailMessage m=getMessageText();
		Enumeration e=m.getMessage();
		while (e.hasMoreElements())
		{	String s=(String)e.nextElement();
			int i=s.indexOf(' ');
			if (i>=0) h.put(s.substring(i).trim(),s);
		}	
	}

	/**
	Return an integer array of new message ids
	*/
	public int[] getNewMessages (Hashtable h)
		throws POPException,IOException	
	{	send("UIDL");
		if (!expectAnswer())
			throw new POPException("uidl");
		MailMessage m=getMessageText();
		Enumeration e=m.getMessage();
		Vector V=new Vector();
		while (e.hasMoreElements())
		{	String s=(String)e.nextElement();
			int i=s.indexOf(' ');
			if (h.get(s.substring(i).trim())==null)
			{	int id;
				try
				{	id=Integer.parseInt(s.substring(0,i));
				}
				catch (Exception ex)
				{	throw new POPException("uidl");
				}
				V.addElement(new Integer(id));
			}
		}
		int ids[]=new int[V.size()];
		for (int i=0; i<V.size(); i++)
			ids[i]=((Integer)V.elementAt(i)).intValue();
		return ids;
	}
	
	public static void main (String args[])
	{	try
		{	POP pop=new POP(args[0]);
			pop.open();
			pop.login(args[1],args[2]);
			int n=pop.getNumberOfMessages();
			System.out.println(n+" Messages!");
			Hashtable h=new Hashtable();
			pop.saveUIDL(h);
			h=new Hashtable();
			int ids[]=pop.getNewMessages(h);
			for (int i=0; i<ids.length; i++)
			{	System.out.println("----- New Message :");
				MailMessage m=pop.getMessageHeader(ids[i]);
				if (m!=null)
				{	System.out.println("Last Message:");
					System.out.println(m.from()+", "+m.date());
					System.out.println(m.subject());
				}				
			}
			pop.close();
		}
		catch (Exception e)
		{	System.out.println(e);
		}
	}
}
