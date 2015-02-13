package rene.util.xml;

import rene.util.SimpleStringBuffer;

public class XmlTranslator
{	static SimpleStringBuffer H=new SimpleStringBuffer(10000);
	static String toXml (String s)
	{	int m=s.length();
		H.clear();
		for (int i=0; i<m; i++)
		{	char c=s.charAt(i);
			switch(c)
			{	case '<' : toH("&lt;"); break;
				case '>' : toH("&gt;"); break;
				case '&' : toH("&amp;"); break;
				case '\'' : toH("&apos;"); break;
				case '\"' : toH("&quot;"); break;
				default : H.append(c);
			}
		}
		return H.toString();
	}
	static void toH (String s)
	{	int m=s.length();
		for (int i=0; i<m; i++)
		{	H.append(s.charAt(i));
		}
	}
	static String toText (String s)
	{	int m=s.length();
		H.clear();
		for (int i=0; i<m; i++)
		{	char c=s.charAt(i);
			if (c=='&')
			{	if (find(s,i,"&lt;"))
				{	H.append('<');
					i+=3;
				}
				else if (find(s,i,"&gt;"))
				{	H.append('>');
					i+=3;
				}
				else if (find(s,i,"&quot;"))
				{	H.append('\"');
					i+=5;
				}
				else if (find(s,i,"&apos;"))
				{	H.append('\'');
					i+=5;
				}
				else if (find(s,i,"&amp;"))
				{	H.append('&');
					i+=4;
				}
				else H.append(c);
			}
			else H.append(c);
		}
		return H.toString();
	}
	static boolean find (String s, int pos, String t)
	{	try
		{	for (int i=0; i<t.length(); i++)
			{	if (s.charAt(pos+i)!=t.charAt(i)) return false;
			}
			return true;
		}
		catch (Exception e)
		{	return false;
		}
	}
}
