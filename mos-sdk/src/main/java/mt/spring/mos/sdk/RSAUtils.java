package mt.spring.mos.sdk;


import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * RSA生成器
 */
public class RSAUtils {
	private final static Base64.Encoder base64Encoder = Base64.getUrlEncoder();
	private final static Base64.Decoder base64Decoder = Base64.getUrlDecoder();
	
	/**
	 * 随机生成密钥对
	 *
	 * @throws NoSuchAlgorithmException
	 */
	public static String[] genKeyPair() throws NoSuchAlgorithmException {
		// KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
		KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
		// 初始化密钥对生成器，密钥大小为96-1024位
		keyPairGen.initialize(1024, new SecureRandom());
		// 生成一个密钥对，保存在keyPair中
		KeyPair keyPair = keyPairGen.generateKeyPair();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();   // 得到私钥
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();  // 得到公钥
		String publicKeyString = base64Encoder.encodeToString(publicKey.getEncoded());
		// 得到私钥字符串
		String privateKeyString = base64Encoder.encodeToString((privateKey.getEncoded()));
		return new String[]{publicKeyString, privateKeyString};
	}
	
	/**
	 * RSA公钥加密
	 *
	 * @param str       加密字符串
	 * @param publicKey 公钥
	 * @return 密文
	 * @throws Exception 加密过程中的异常信息
	 */
	public static String encrypt(String str, String publicKey) throws Exception {
		//base64编码的公钥
		byte[] decoded = base64Decoder.decode(publicKey);
		RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
		//RSA加密
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, pubKey);
		byte[] bytes = cipher.doFinal(str.getBytes(StandardCharsets.UTF_8));
		byte[] encode = base64Encoder.encode(bytes);
		return new String(encode);
	}
	
	private static List<String> splice(String str) {
		int maxLen = 100;
		byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
		List<String> list = new ArrayList<>();
		if (bytes.length > maxLen) {
			int half = str.length() / 2;
			String s1 = str.substring(0, half);
			String s2 = str.substring(half);
			list.addAll(splice(s1));
			list.addAll(splice(s2));
		} else {
			list.add(str);
		}
		return list;
	}
	
	
	public static String encryptLarge(String str, String publicKey) throws Exception {
		List<String> strs = splice(str);
		List<String> result = new ArrayList<>();
		for (String s : strs) {
			result.add(encrypt(s, publicKey));
		}
		return StringUtils.join(result, "**");
	}
	
	public static String decryptLarge(String str, String privateKey) throws Exception {
		String[] split = str.split("\\*\\*");
		List<String> result = new ArrayList<>();
		for (String s : split) {
			result.add(decrypt(s, privateKey));
		}
		return StringUtils.join(result, "");
	}
	
	/**
	 * RSA私钥解密
	 *
	 * @param str        加密字符串
	 * @param privateKey 私钥
	 * @return 铭文
	 * @throws Exception 解密过程中的异常信息
	 */
	public static String decrypt(String str, String privateKey) throws Exception {
		//64位解码加密后的字符串
		byte[] inputByte = base64Decoder.decode(str.getBytes(StandardCharsets.UTF_8));
		//base64编码的私钥
		byte[] decoded = base64Decoder.decode(privateKey);
		RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
		//RSA解密
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, priKey);
		return new String(cipher.doFinal(inputByte));
	}
	
}