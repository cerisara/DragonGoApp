/*
 * Created on 20.07.2005
 *
 */
package rene.util.ftp;

import java.io.BufferedReader;
import java.io.IOException;

class Answer
{	int Code;
	String Text;
	public void get (BufferedReader in) throws IOException
	{	String s=in.readLine();
		String CodeString=s.substring(0,3);
		Code=Integer.parseInt(CodeString);
		Text=s.substring(4);
		if (s.charAt(3)=='-')
		{	while (true)
			{	s=in.readLine();
				if (s.startsWith(CodeString))
				{	Text=Text+"\n"+s.substring(4);
					break;
				}
				Text=Text+"\n"+s;
			}
		}
	}
	int code () { return Code; }
	String text () { return Text; }
}
