package mt.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/*
	public static final String base64Encode(InputStream is) throws IOException
	{
		StringBuffer  buffer = new StringBuffer();
		int remainder = 0;  int bitsRemainder = 0;
		int outCount = 0;
		for(;;)
		{
			if(bitsRemainder>=6)
			{
				//os.write(toBase64Code((remainder>>(bitsRemainder-6))&63));
				buffer.append((char)toBase64Code((remainder>>(bitsRemainder-6))&63));
				outCount++;
				bitsRemainder -= 6;
				continue;
			}
			int indata = is.read();
			if( indata<0 ) break;
			remainder = (remainder<<8)|(indata&0xff);  bitsRemainder+=8;
		}
		if( bitsRemainder>0 ) // bitsRemainder<6
		{
			//os.write(toBase64Code((remainder<<(6-bitsRemainder))&63));
			buffer.append((char)toBase64Code((remainder<<(6-bitsRemainder))&63));
			outCount++;
		}
		for(;(outCount&3)!=0;outCount++)
			buffer.append('=');
			//os.write('=');
		return buffer.toString();
		//return outCount;
	}
*/
//import java.io.*;
/*
"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
*/
public class Base64Encode extends OutputStream
{
	//final StringBuffer  buffer = new StringBuffer();
	//final ByteArrayOutputStream os = new ByteArrayOutputStream() ;
      final java.io.OutputStream os;
	int remainder = 0;  int bitsRemainder = 0;
	int outCount = 0;
	public Base64Encode()
	{
          os = new ByteArrayOutputStream() ;
	}
	public Base64Encode(java.io.OutputStream os)
	{
          this.os = os;
	}
  /**
   * Writes the specified byte to this output stream. The general
   * contract for <code>write</code> is that one byte is written
   * to the output stream. The byte to be written is the eight
   * low-order bits of the argument <code>b</code>. The 24
   * high-order bits of <code>b</code> are ignored.
   * <p>
   * Subclasses of <code>OutputStream</code> must provide an
   * implementation for this method.
   *
   * @param      b   the <code>byte</code>.
   * @exception IOException  if an I/O error occurs. In particular,
   *             an <code>IOException</code> may be thrown if the
   *             output stream has been closed.
   */
	@Override
	public void write(int indata) throws IOException
	{
		indata &= 0xff;
		if( closed ) // || indata>255
			throw new IOException();
		for(;;)
		{
			if(bitsRemainder>=6)
			{
				os.write(toBase64Code((remainder>>(bitsRemainder-6))&63));
				//buffer.append((char)toBase64Code((remainder>>(bitsRemainder-6))&63));
				//super.write(toBase64Code((remainder>>(bitsRemainder-6))&63));
				outCount++;
				bitsRemainder -= 6;
				continue;
			}
			// int indata = is.read();
			if( indata<0 ) break;
			remainder = (remainder<<8)|(indata&0xff);  bitsRemainder+=8;
			indata = -1;
		}
	}
	boolean closed;
	@Override
	public void close() throws IOException
	{
      if( !closed )
      {
		if( bitsRemainder>0 ) // bitsRemainder<6
		{
			os.write(toBase64Code((remainder<<(6-bitsRemainder))&63));
			//buffer.append((char)toBase64Code((remainder<<(6-bitsRemainder))&63));
			//super.write(toBase64Code((remainder<<(6-bitsRemainder))&63));
			outCount++;
		}
		for(;(outCount&3)!=0;outCount++)
			//buffer.append('=');
			os.write('=');
		os.close();
		closed = true;
      }
	}
	public static final int toBase64Code(int x)
	{
		if(x<0 || x>=64)
			throw new IllegalArgumentException();
		if(x<=25) return (char)('A'+x);
		if(x<=51) return (char)('a'+x-26);
		if(x<=61) return (char)('0'+x-52);
		if(x==62) return '+';
		if(x==63) return '/';
		return '=';
	}
  public byte[] toByteArray()  throws IOException
  {
      if( !closed )
          throw new IOException();
      if( !(os instanceof ByteArrayOutputStream) )
          throw new IOException();
      return ((ByteArrayOutputStream)os).toByteArray();//buffer.toString();
  }
	public String getAsString()  throws IOException
	{
		if( !closed )
			throw new IOException();
      if( !(os instanceof ByteArrayOutputStream) )
          throw new IOException();
		return new String(((ByteArrayOutputStream)os).toByteArray(),"8859_1");//buffer.toString();
	}
	static public String encode(byte a[]) throws IOException
	{
      Base64Encode encode = new Base64Encode();
      encode.write(a);
      encode.close();
      return encode.getAsString();
	}
  static public String encodeString(String text, String charset) throws IOException
  {
  	return encode(charset==null?text.getBytes():text.getBytes(charset));
  }
	/*
	public byte[] getAsByteArray()  throws IOException
	{
		if( !closed )
			throw new IOException();
		return os.toByteArray();//buffer.toString();
	}
	*/
}

