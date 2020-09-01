package mt.utils;

import java.io.*;

public class Base64Decode extends InputStream
{
	int remainder = 0;  int bitsRemainder = 0;
	int outCount = 0;
	final InputStream inputStream;
	public Base64Decode(InputStream is)
	{
		this.inputStream = is;
	}
	public Base64Decode(byte[] ba)
	{
		this( new ByteArrayInputStream(ba) );
	}
	//final String text ;  int iText ;
	//final int  lenText ;
	public Base64Decode(String text)
	{
		//this.text = text;
		//lenText = text.length();
		this( new ByteArrayInputStream(text.getBytes()) );
	}
	boolean skipBlank;
	public void setSkipBlank(boolean skipBlank)
	{
		this.skipBlank = skipBlank;
	}
	@Override
	public int read() throws IOException
	{
	   for(;;)
	   {
			if(bitsRemainder>=8)
			{
				int c = remainder>>(bitsRemainder-8); //os.write(remainder>>(bitsRemainder-8));
				outCount++;
				bitsRemainder -= 8;
				return c&0xff; //continue;
			}
		//	if( iText>=lenText )
		//		return -1;
			int indata;
			if( skipBlank )
			{
				for(;;)
				{
					indata = inputStream.read(); //text.charAt(iText++); //is.read();
					if( indata<0 || indata>' ')
						break;
				}
			} else
			{
				indata = inputStream.read(); //text.charAt(iText++); //is.read();
			}
			if( indata<0 ) 
				return -1;//break;
			if( indata=='=' )
				continue;
			remainder = (remainder<<6)|fromBase64Code(indata);  bitsRemainder+=6;
	   }
	   //return -1;
	}
	
	public static final int fromBase64Code(int x)
	{
		if(x>='A' && x<='Z') return x-'A';
		if(x>='a' && x<='z') return x-'a'+26;
		if(x>='0' && x<='9') return x-'0'+52;
		if(x=='+') return 62;
		if(x=='/') return 63;
		throw new IllegalArgumentException("code="+(char)x+'('+x+')');
	}
	 static public int copy(InputStream in, OutputStream out) throws IOException
	    {
	        int n = 0;
	 //       ByteArrayOutputStream
	        byte[] buffer = new byte[64*1024];
	        for(;;)
	        {
	            int count = in.read(buffer);
	            if (count < 0)
	                break;
	            out.write(buffer,0,count);
	            n += count;
	        }
	        return n;
	    }
    static public byte[] toByteArray(InputStream in) throws IOException
    {
        if( in==null ) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy( in, out );
        return out.toByteArray();
    }
	static public byte[] decode(String text) throws java.io.IOException
	{
	    return toByteArray(new Base64Decode(text));
	}
	static private int toHex(char c1)
	{
	    if( c1>='0'&&c1<='9')
	        return c1-'0';
	    if( c1>='A'&&c1<='F' )
	        return c1-'A'+10;
	    if( c1>='a'&&c1<='f' )
	        return c1-'a'+10;
	    return -1;
	}
	static public byte[] decodeQ(String text) // throws java.io.IOException
	{
	    //“=68=65=6C=6C=6F”
	    final int ltext = text.length();
	    byte a[] = new byte[ltext]; int n = 0;
	    for(int i=0;i<ltext;i++)
	    {
	        char c = text.charAt(i);
	        if( c=='=' )
	        {
	            int c1 = toHex(text.charAt(++i));
	            int c2 = toHex(text.charAt(++i));
	            if( c1<0 || c2<0 )
	                throw new java.lang.IllegalArgumentException(text);
	            a[n++] = (byte)((c1<<4)|c2);
	            //if( c1>='0'
	        } else
	        {
	            a[n++] = (byte)c;
	        }
	    }
	    byte newa[] = new byte[n]; System.arraycopy(a,0,newa,0,n);
	    return newa;
	}
	
	static public String convertPasswordString(String text, boolean asStore) throws IOException
	{
		if( text==null || text.length()==0 )
			return text;
		if( asStore )
		{
			Base64Encode os = new Base64Encode();
			byte ba[] = text.getBytes();
			for(int i=0;i<ba.length;i++) ba[i] ^= 0x13;
			os.write( ba );
			os.close();
			return "`"+os.getAsString()+"`";
		} else
		{
			int ltext = text.length();
	//Message.out.println(text);
			if( ltext>=2 && text.charAt(0)=='`' && text.charAt(ltext-1)=='`' )
			{
	//Message.out.println(text.substring(1,ltext-1));
				Base64Decode is = new Base64Decode(text.substring(1,ltext-1));
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				for(;;)
				{
					int c = is.read();
					if(c<=0)
						break;
					os.write(c^0x13);
				}
	//Message.out.println(new String( os.toByteArray()));
				return new String( os.toByteArray() );
			}
		}
		return text;
	}
	public static void main(String[] args) {
		try {
			//true是加密，false是解密
			System.out.println(convertPasswordString("111111", false));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

