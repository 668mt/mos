//package mt.utils;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import org.apache.commons.lang3.StringUtils;
//import sun.misc.BASE64Decoder;
//import sun.misc.BASE64Encoder;
//
//import javax.crypto.Cipher;
//import javax.crypto.KeyGenerator;
//import javax.crypto.spec.SecretKeySpec;
//import java.security.MessageDigest;
//import java.security.SecureRandom;
//import java.util.*;
//import java.util.Map.Entry;
//
///**
// * 加密类
//* @ClassName: JiaMi
//* @Description:
//* @author Martin
//* @date 2017-7-21 下午5:54:01
//*
// */
//@SuppressWarnings("restriction")
//public class JiaMi {
//	private final static String OIG_KEY = MyUtils.getParamFromProp("jiaMi.oigKey");
//    /**
//     * AES加密
//     * @param content 待加密的内容
//     * @param encryptKey 加密密钥
//     * @return 加密后的byte[]
//     * @throws Exception
//     */
//    public static byte[] aesEncryptToBytes(String content, String encryptKey) throws Exception {
//        KeyGenerator kgen = KeyGenerator.getInstance("AES");
//        kgen.init(128, new SecureRandom(encryptKey.getBytes()));
//
//        Cipher cipher = Cipher.getInstance("AES");
//        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(kgen.generateKey().getEncoded(), "AES"));
//
//        return cipher.doFinal(content.getBytes("utf-8"));
//    }
//    /**
//     * AES解密
//     * @param encryptBytes 待解密的byte[]
//     * @param decryptKey 解密密钥
//     * @return 解密后的String
//     * @throws Exception
//     */
//    public static String aesDecryptByBytes(byte[] encryptBytes, String decryptKey) throws Exception {
//        KeyGenerator kgen = KeyGenerator.getInstance("AES");
//        kgen.init(128, new SecureRandom(decryptKey.getBytes()));
//
//        Cipher cipher = Cipher.getInstance("AES");
//        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(kgen.generateKey().getEncoded(), "AES"));
//        byte[] decryptBytes = cipher.doFinal(encryptBytes);
//
//        return new String(decryptBytes);
//    }
//    /**
//     * base 64 encode
//     * @param bytes 待编码的byte[]
//     * @return 编码后的base 64 code
//     */
//	public static String base64Encode(byte[] bytes){
//    	BASE64Encoder base64Encoder = new BASE64Encoder();
//		return base64Encoder.encode(bytes);
//    }
//    /**
//     * AES加密为base 64 code
//     * @param content 待加密的内容
//     * @param encryptKey 加密密钥
//     * @return 加密后的base 64 code
//     * @throws Exception
//     */
//    public static String aesEncode(String content, String encryptKey) throws Exception {
//        return base64Encode(aesEncryptToBytes(content, encryptKey));
//    }
//    /**
//     * base 64 decode
//     * @param base64Code 待解码的base 64 code
//     * @return 解码后的byte[]
//     * @throws Exception
//     */
//	public static byte[] base64Decode(String base64Code) throws Exception {
//        return StringUtils.isEmpty(base64Code) ? null : new BASE64Decoder().decodeBuffer(base64Code);
//    }
//    /**
//     * 将base 64 code AES解密
//     * @param encryptStr 待解密的base 64 code
//     * @param decryptKey 解密密钥
//     * @return 解密后的string
//     * @throws Exception
//     */
//    public static String aesDecode(String encryptStr, String decryptKey) throws Exception {
//        return StringUtils.isEmpty(encryptStr) ? null : aesDecryptByBytes(base64Decode(encryptStr), decryptKey);
//    }
//    /**
//     * 加密
//     * @param params
//     * @return
//     * @throws Exception
//     */
//    public static String encode(Map<String, String> params) throws Exception {
//    	Assert.notNull(params);
//    	String jsonUnicode = JsonUtils.toJsonUnicode(params);
//		return aesEncode(jsonUnicode, OIG_KEY);
//    }
//    /**
//     * 解密
//     * @param code
//     * @return
//     * @throws Exception
//     */
//    public static Map<String, String> decode(String code) throws Exception {
//    	Assert.notNull(code);
//    	code = aesDecode(code, OIG_KEY);
//    	Map<String, String> params = JsonUtils.toObjectUnicode(code, new TypeReference<Map<String, String>>(){});
//    	return params;
//    }
//    /**
//     * 先md5加密，在base64加密
//     * @param str
//     * @return
//     * @throws Exception
//     */
//	public static String md5AndBase64(String str) throws Exception {
//		MessageDigest md5 = java.security.MessageDigest.getInstance("MD5");
//		sun.misc.BASE64Encoder base64Encoder = new sun.misc.BASE64Encoder();
//		return base64Encoder.encode (md5.digest(str.getBytes("utf-8")));
//	}
//
//	/**
//	 * 获取同步安全密匙
//	 * @param params
//	 * @return
//	 */
//	public static String getSecretKey(Map<String, String> params) {
//		Assert.notNull(params);
//		params.put("OIG_KEY", OIG_KEY);
//		Set<Entry<String, String>> entrySet = params.entrySet();
//		List<Entry<String, String>> list = new ArrayList<Entry<String,String>>(entrySet);
//		Collections.sort(list,new Comparator<Entry<String, String>>() {
//			@Override
//			public int compare(Entry<String, String> o1,
//					Entry<String, String> o2) {
//				return o1.getKey().compareTo(o2.getKey());
//			}
//		});
//		String str = "";
//		for (Entry<String, String> entry : list) {
//			String key = entry.getKey();
//			String value = entry.getValue();
//			str += key + "=" +value + "&";
//		}
//		str = str.substring(0,str.length()-1);
//		try {
//			return md5AndBase64(str);
//		} catch (Exception e) {
//			e.printStackTrace();
//		};
//		return null;
//	}
//}
