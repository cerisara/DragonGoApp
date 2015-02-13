package rene.util;
public class SimpleStringBuffer
{	private int Size,N;
	private char Buf[];
	public SimpleStringBuffer (int size)
	{	Size=size;
		Buf=new char[size];
		N=0;
	}
	public SimpleStringBuffer (char b[])
	{	Size=b.length;
		Buf=b;
		N=0;
	}
	public void append (char c)
	{	if (N<Size) Buf[N++]=c;
		else
		{	Size=2*Size;
			char NewBuf[]=new char[Size];
			for (int i=0; i<N; i++) NewBuf[i]=Buf[i];
			Buf=NewBuf;
			Buf[N++]=c;
		}
	}
	public void append (String s)
	{	int n=s.length();
		for (int i=0; i<n; i++) append(s.charAt(i));
	}
	public void clear ()
	{	N=0;
	}
	public String toString ()
	{	if (N==0) return "";
		return new String(Buf,0,N);
	}
}

