package rene.util;
public class SimpleByteBuffer
{	private int Size,N;
	private byte Buf[];
	public SimpleByteBuffer (int size)
	{	Size=size;
		Buf=new byte[size];
		N=0;
	}
	public SimpleByteBuffer (byte b[])
	{	Size=b.length;
		Buf=b;
		N=0;
	}
	public void append (byte c)
	{	if (N<Size) Buf[N++]=c;
		else
		{	Size=2*Size;
			byte NewBuf[]=new byte[Size];
			for (int i=0; i<N; i++) NewBuf[i]=Buf[i];
			Buf=NewBuf;
			Buf[N++]=c;
		}
	}
	public void clear ()
	{	N=0;
	}
	public byte[] getBuffer ()
	{	return Buf;
	}
	public byte[] getByteArray ()
	{	byte b[]=new byte[N];
		for (int i=0; i<N; i++) b[i]=Buf[i];
		return b;
	}
	public int size ()
	{	return N;
	}
}

