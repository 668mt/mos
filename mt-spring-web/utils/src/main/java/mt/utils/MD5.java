package mt.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5加密算法 返回加密的字符串
 * @author Administrator
 *
 */
public class MD5 {
	public static String makeMD5(String plainText) {  
          try {  
             MessageDigest md = MessageDigest.getInstance("MD5");  
             md.update(plainText.getBytes());  
             byte b[] = md.digest();  
             int i;  
             StringBuffer buf = new StringBuffer("");  
             for (int offset = 0; offset < b.length; offset++) {  
                 i = b[offset];  
                 if (i < 0)  
                     i += 256;  
                 if (i < 16)  
                     buf.append("0");  
                 buf.append(Integer.toHexString(i));  
             }  
             //32位加密  
             return buf.toString();  
             // 16位的加密  
             //return buf.toString().substring(8, 24);  
         } catch (NoSuchAlgorithmException e) {  
             e.printStackTrace();  
             return null;  
         }  
     }  
	
//	@Test
//	public void xx(){
//		String a=makeMD5("yh2017ABC1231505185380088WTE_IM_CHECK77152042");
//		System.out.println(a);
//		try {
//			String b=encodeMd5("yh2017ABC1231505185380088WTE_IM_CHECK77152042");
//			System.out.println(b);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	
	
	/**
	 * 加密证书Md5
	 * @param token
	 * @return
	 * @throws Exception
	 */
	public static String encodeMd5(String token) throws Exception {
		String s = null;
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'a', 'b', 'c', 'd', 'e', 'f' };// 用来将字节转换成16进制表示的字符
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(token.getBytes());
			byte tmp[] = md.digest();// MD5 的计算结果是一个 128 位的长整数，
			// 用字节表示就是 16 个字节
			char str[] = new char[16 * 2];// 每个字节用 16 进制表示的话，使用两个字符， 所以表示成 16
			// 进制需要 32 个字符
			int k = 0;// 表示转换结果中对应的字符位置
			for (int i = 0; i < 16; i++) {// 从第一个字节开始，对 MD5 的每一个字节// 转换成 16
											// 进制字符的转换
				byte byte0 = tmp[i];// 取第 i 个字节
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];// 取字节中高 4 位的数字转换,// >>>
				// 为逻辑右移，将符号位一起右移
				str[k++] = hexDigits[byte0 & 0xf];// 取字节中低 4 位的数字转换

			}
			s = new String(str);// 换后的结果转换为字符串

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return s;
	}
}
