package rene.util;

import java.io.File;

/**
This is a static class to determine extensions etc. from a file name.
*/

public class FileName
{	static public int ChopLength=48; 
	static public String purefilename (String filename)
	{	char a[]=filename.toCharArray(); 
		int i=a.length-1; 
		char fs=File.separatorChar; 
		while (i>=0)
		{	if (a[i]==fs || a[i]=='/' || i==0)
			{	if (i==0) i=-1; 
				if (i<a.length-1)
				{	int j=a.length-1; 
					while (j>i && a[j]!='.') j--; 
					if (j>i+1) return new String(a,i+1,j-i-1); 
					else return ""; 
				}
				else return ""; 
			}
			i--; 
		}
		return filename; 
	}
	static public String path (String filename)
	{	char a[]=filename.toCharArray(); 
		int i=a.length-1; 
		char fs=File.separatorChar; 
		while (i>0)
		{	if (a[i]==fs || a[i]=='/')
			{	return new String(a,0,i); 
			}
			i--; 
		}
		return ""; 
	}
	static public String pathAndSeparator (String filename)
	{	char a[]=filename.toCharArray(); 
		int i=a.length-1; 
		char fs=File.separatorChar; 
		while (i>0)
		{	if (a[i]==fs || a[i]=='/')
			{	return new String(a,0,i+1); 
			}
			i--; 
		}
		return ""; 
	}
	static public String filename (String filename)
	{	char a[]=filename.toCharArray(); 
		int i=a.length-1; 
		char fs=File.separatorChar; 
		while (i>0)
		{	if (a[i]==fs || a[i]=='/')
			{	if (i+1<a.length) return new String(a,i+1,a.length-i-1); 
				else return ""; 
			}
			i--; 
		}
		return filename; 
	}
	static public String extension (String filename)
	{	char a[]=filename.toCharArray(); 
		int i=a.length-1; 
		char fs=File.separatorChar; 
		while (i>0)
		{	if (a[i]=='.')
			{	if (i+1<a.length) return new String(a,i+1,a.length-i-1); 
				else return ""; 
			}
			if (a[i]==fs || a[i]=='/') break; 
			i--; 
		}
		return ""; 
	}
	static public String chop (String filename, int chop)
		// chop the filename to 32 characters
	{	if (filename.length()>chop)
		{	filename="... "+filename.substring(filename.length()-chop); 
		}
		return filename; 
	}
	static public String chop (String filename)
	{	return chop(filename,ChopLength);
	}
	static public String chop (int start, String filename, int chop)
		// chop the filename.substring(start) to 32 characters
	{	if (filename.length()>start+chop)
		{	filename=filename.substring(0,start)+
				" ... "+
				filename.substring(filename.length()-chop); 
		}
		return filename; 
	}
	static public String chop (int start, String filename)
	{	return chop(start,filename,ChopLength);
	}
	static public String relative (String dir, String filename)
	{	dir=dir+System.getProperty("file.separator"); 
		if (filename.startsWith(dir))
		{	return filename.substring(dir.length()); 
		}
		else return filename; 
	}
	static public String canonical (String filename)
	{	File f=new File(filename); 
		try
		{	String s=f.getCanonicalPath();
			if (s.length()>2 && s.charAt(1)==':')
				s=s.substring(0,2).toLowerCase()+s.substring(2);
			return s; 
		}
		catch (Exception e) {	return f.getAbsolutePath(); }
	}
	static public String toURL (String filename)
	{	int n=filename.indexOf(' ');
		if (n>=0)
			return filename.substring(0,n)+"%20"+toURL(filename.substring(n+1));
		else
			return filename;
	}	
	static boolean match (char filename[], int n, char filter[], int m)
	{	if (filter==null) return true;
		if (m>=filter.length) return n>=filename.length;
		if (n>=filename.length) return m==filter.length-1 && filter[m]=='*';
		if (filter[m]=='?')
		{	return match(filename,n+1,filter,m+1);
		}
		if (filter[m]=='*')
		{	if (m==filter.length-1) return true;
			for (int i=n; i<filename.length; i++)
			{	if (match(filename,i,filter,m+1)) return true;
			}
			return false;
		}
		if (filter[m]==filename[n])	return match(filename,n+1,filter,m+1);
		return false;
	}
	public static boolean match (String filename, String filter)
	{	char fn[]=filename.toCharArray(),f[]=filter.toCharArray();
		return match(fn,0,f,0);
	}
	public static void main (String args[])
	{	System.out.println("-"+toURL(" test test test ")+"-");
	}
}
