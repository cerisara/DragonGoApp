package rene.util.ftp;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;

/**
An FTP protocol handler. See main for an example.
*/

public class SFTP
{	String Server;
	int Port;
	SSLSocket S;
	BufferedReader In;
	PrintWriter Out;
	Answer A;

	/**
	Just denote the server and port (default is 21).
	The connection has to be opened and closed!
	*/
	public SFTP (String server, int port)
	{	Server=server; Port=port;
	}

	public SFTP (String server)
	{	this(server,22);
	}
	
	public static SSLSocket getSSLSocket (String server, int port)
		throws IOException,UnknownHostException
	{	SSLSocketFactory ssf=(SSLSocketFactory)SSLSocketFactory.getDefault(); 
		SSLSocket S=(SSLSocket)ssf.createSocket(server,port);
		S.startHandshake();
		return S;
	}

	/**
	Open the connection to the server, getting the control data
	streams.
	*/
	public void open () 
		throws IOException,UnknownHostException
	{	S=getSSLSocket(Server,Port);
		In=new BufferedReader(new InputStreamReader(
			new DataInputStream(S.getInputStream())));
		Out=new PrintWriter(S.getOutputStream());
		if (getreply()/100!=2)
			throw new IOException("Illegal reply.");
	}

	/**
	Open the server and connect as user with the password.
	*/
	public void open (String user, String password)
		throws IOException,UnknownHostException
	{	open();
		if (!command("USER "+user))
			throw new IOException("User not accepted.");
		if (!command("PASS "+password))
			throw new IOException("Wrong Password");
	}

	/**
	Close the connection to the server and the control data streams.
	*/
	public void close () throws IOException
	{	send("QUIT");
		In.close();
		Out.close();
		S.close();
	}

	/**
	Wait for reply from the server.
	@return the reply code (1xx,2xx,3xx are OK).
	*/
	public int getreply () throws IOException
	{	A=new Answer();
		A.get(In);
		//System.out.println(A.code());
		//System.out.println(A.text());
		return A.code();
	}

	/**
	Send a command to the server and wait for a direct reply.
	This will set the Answer field for checking.
	@return true if the command succeeded immediately.
	*/
	public boolean command (String s) throws IOException
	{	send(s);
		return (getreply()/100<4);
	}
	
	/**
	This returns the Answer to the previous command. The
	meanings of Answer.code() are:
	<ul>
	<li> 1xx - Another reply will follow
	<li> 2xx - The answer is positive
	<li> 3xx - The answer is positive, but needs more action
	<li> 4xx - The answer is negative, but one can try again
	<li> 5xx - The answer is negative
	</ul>
	A.text() will contain the text, the server sent. In case
	of multiline text, this will be separated by \n.
	*/
	public Answer answer ()
	{	return A; 
	}
	
	/**
	Send a command to the server.
	*/
	public void send (String s) throws IOException
	{	Out.println(s); Out.flush();
	}
	
	/**
	Tell the server to wait for a data connection on a port
	of his discretion. The return of the server is scanned
	for the IP and the port and a Socket is generated.
	@return Socket for the data connection to the server
	*/
	public SSLSocket passive ()
		throws IOException,UnknownHostException, NumberFormatException
	{	if (!command("PASV"))
			throw new IOException("Passive mode not supported.");
		StringTokenizer p=new StringTokenizer(A.text(),"(,)");
		if (!p.hasMoreTokens())
			throw new IOException("Wrong answer from server.");
		else p.nextToken();
		int N[]=new int[4];
		for (int i=0; i<4; i++)
		{	N[i]=Integer.parseInt(p.nextToken());
		}
		int k=Integer.parseInt(p.nextToken());
		int P=k*256+Integer.parseInt(p.nextToken());
		String server=N[0]+"."+N[1]+"."+N[2]+"."+N[3];
		return getSSLSocket(server,P);
	}

	SSLSocket DSocket;

	/**
	Get an input stream to the file. getClose() must
	be called!
	*/	
	public InputStream getFile (String file)
		throws IOException,UnknownHostException
	{	DSocket=passive();
		if (!command("TYPE I"))
			throw new IOException("Type I not supported?");
		send("RETR "+file);
		getreply();
		if (A.code()/100>=4) throw new IOException("Get failed.");
		return DSocket.getInputStream();
	}
	
	/**
	Get an input stream to the list. getClose() must
	be called!
	*/	
	public InputStream getDir (String path)
		throws IOException,UnknownHostException
	{	DSocket=passive();
		if (!command("TYPE A"))
			throw new IOException("Type A not supported?");
		if (!path.equals("")) send("LIST "+path);
		else send("LIST");
		getreply();
		if (A.code()/100>=4) throw new IOException("ls failed.");
		return DSocket.getInputStream();
	}
	
	/**
	Get an input stream to the list. getClose() must
	be called!
	*/	
	public InputStream getLs(String path)
		throws IOException,UnknownHostException
	{	DSocket=passive();
		if (!command("TYPE A"))
			throw new IOException("Type A not supported?");
		if (!path.equals("")) send("NLST "+path);
		else send("NLST");
		getreply();
		if (A.code()/100>=4) throw new IOException("ls failed.");
		return DSocket.getInputStream();
	}
	
	/**
	Close the data socket after a get command and wait for
	the transfer complete message.
	*/
	public void getClose ()
		throws IOException
	{	DSocket.close();
		while (true)
		{	getreply();
			if (A.code()==226) return;
			if (A.code()>=400)
				throw new IOException("Put failed.");
		}
	}

	/**
	Get an output stream to the file. putClose() must
	be called!
	*/
	public OutputStream putFile (String file)
		throws IOException,UnknownHostException
	{	DSocket=passive();
		if (!command("TYPE I"))
			throw new IOException("Type I not supported?");
		send("STOR "+file);
		getreply();
		if (A.code()/100>=4) throw new IOException("Put failed.");
		return DSocket.getOutputStream();
	}

	/**
	Close the date socket after a put command and wait for
	the transfer complete message.
	*/	
	public void putClose ()
		throws IOException
	{	DSocket.close();
		while (true)
		{	getreply();
			if (A.code()==226) return;
			if (A.code()>=400)
				throw new IOException("Put failed.");
		}
	}
	
	/**
	Cange the directory to the specified one.
	*/
	public void changeDirectory (String dir)
		throws IOException
	{	if (!command("CWD "+dir))
			throw new IOException("Directory change failed.");
	}
	
	public Vector getDirectory (String dir)
		throws IOException
	{	Vector v=new Vector();
		try
		{	BufferedReader In=new BufferedReader(
				new InputStreamReader(getDir(dir)));
			while (true)
			{	String s=In.readLine();
				if (s==null) break;
				v.addElement(s);
			}
			In.close();
		}
		catch (Exception e)
		{	throw new IOException("Directory list failed.");
		}
		getClose();
		return v;
	}
	
	public Vector getCurrentDirectory ()
		throws IOException
	{	return getDirectory(".");
	}

	static public void main (String args[])
	{	try
		{	SFTP ftp=new SFTP(args[0]);
			ftp.open(args[1],args[2]);
			Enumeration e=ftp.getCurrentDirectory().elements();
			while (e.hasMoreElements())
			{	System.out.println((String)e.nextElement());
			}
			ftp.close();
		}
		catch (Exception e)
		{	System.out.println(e);
			e.printStackTrace();
		}	
	}
	
}
