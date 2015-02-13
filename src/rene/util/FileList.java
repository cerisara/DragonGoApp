package rene.util;

import java.io.File;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import rene.util.sort.SortObject;
import rene.util.sort.Sorter;

class SortFile extends File
	implements SortObject
{	String S;
	static int SortBy=0;
	final public static int NAME=0,DATE=1;
	public SortFile (File dir, String name)
	{	super(dir,name);
		try
		{	S=getCanonicalPath().toUpperCase();
		}
		catch (Exception e)
		{ S=""; }
	}
	public int compare (SortObject o)
	{	SortFile f=(SortFile)o;
		if (SortBy==DATE)
		{	long n=f.lastModified();
			long m=lastModified();
			if (n<m) return -1;
			if (n>m) return 1;
			return 0;
		}
		return -f.S.compareTo(S);
	}
}

class FileFilter
{	char F[][];
	public FileFilter (String s)
	{	StringTokenizer t=new StringTokenizer(s);
		int n=t.countTokens();
		F=new char[n][];
		for (int i=0; i<n; i++)
		{	F[i]=t.nextToken().toCharArray();
		}
	}
	public char[] filter (int i)
	{	return F[i];
	}
	public int filterCount ()
	{	return F.length;
	}
}

/**
This class parses a subtree for files that match a pattern.
The pattern may contain one or more * and ? as usual.
The class delivers an enumerator for the files, or may be subclassed
to handle the files directly. The routines directory and file can
be used to return, if more scanning is necessary.
*/

public class FileList
{	Vector V=new Vector(),Vdir=new Vector();
	boolean Stop;
	boolean Recurse;
	String Dir,Filter;
	boolean UseCase=false;
	public FileList (String dir, String filter, boolean recurse)
	{	Stop=false;
		Recurse=recurse;
		Dir=dir;
		Filter=filter;
		if (Dir.equals("-"))
		{	Dir=".";
			Recurse=false;
		}
		else if (Dir.startsWith("-"))
		{	Dir=Dir.substring(1);
			Recurse=false;
		}
	}
	public FileList (String dir, String filter)
	{	this(dir,filter,true);
	}
	public FileList (String dir)
	{	this(dir,"*",true);
	}
	public void setCase (boolean usecase)
	{	UseCase=usecase;
	}
	public void search ()
	{	Stop=false;	
		File file=new File(Dir);
		if (!UseCase) Filter=Filter.toLowerCase();
		if (file.isDirectory()) find(file,new FileFilter(Filter));
	}
	void find (File dir, FileFilter filter)
	{	if (!directory(dir)) return;
		String list[]=dir.list();
		loop : for (int i=0; i<list.length; i++)
		{	SortFile file=new SortFile(dir,list[i]);
			if (file.isDirectory()) 
			{	Vdir.addElement(file);
				if (Recurse) find(file,filter);
			}
			else 
			{	String filename=file.getName();
				if (!UseCase) filename=filename.toLowerCase();
				char fn[]=filename.toCharArray();
				for (int j=0; j<filter.filterCount(); j++)
				{	if (match(fn,0,filter.filter(j),0))
					{	Stop=!file(file);
						if (Stop) break loop;
						V.addElement(file);
					}
				}
			}
			if (Stop) break;
		}
		parsed(dir);
	}
	boolean match (char filename[], int n, char filter[], int m)
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
	/**
	Return an Enumeration with the files.
	*/
	public Enumeration files ()
	{	return V.elements();
	}
	/**
	Return an Enumeration with the directories.
	*/
	public Enumeration dirs ()
	{	return Vdir.elements();
	}
	/**
	@return The number of files found.
	*/
	public int size ()
	{	return V.size();
	}
	/**
	Sort the result.
	*/
	public void sort ()
	{	int i,n=V.size();
		SortObject v[]=new SortObject[n];
		for (i=0; i<n; i++) v[i]=(SortFile)V.elementAt(i);
		Sorter.sort(v);
		for (i=0; i<n; i++) V.setElementAt(v[i],i);
		n=Vdir.size();
		v=new SortObject[n];
		for (i=0; i<n; i++) v[i]=(SortFile)Vdir.elementAt(i);
		Sorter.sort(v);
		for (i=0; i<n; i++) Vdir.setElementAt(v[i],i);
	}
	public void sort (int type)
	{	SortFile.SortBy=type;
		sort();
		SortFile.SortBy=SortFile.NAME;
	}
	/**
	@param file The directory that has been found.
	@return false if recursion should stop here.
	(i.e. that directory needs not be parsed).
	*/
	protected boolean directory (File dir)
	{	return true;
	}
	/**
	@param file The file that has been found.
	@return false if you need no more file at all.
	*/
	protected boolean file (File file)
	{	return true;
	}
	/**
	@param parsed The directory that has been parsed.
	*/
	protected void parsed (File dir)
	{
	}
	/**
	This stops the search from other threads.
	*/
	public void stopIt ()
	{	Stop=true;
	}
	/**
	 * Returns a canonical version of the directory
	*/
	public String getDir ()
	{	File dir=new File(Dir);
		try
		{	return (dir.getCanonicalPath());
		}
		catch (Exception e)
		{	return "Dir does not exist!";
		}
	}
}
