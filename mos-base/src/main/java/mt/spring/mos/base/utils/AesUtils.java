package mt.spring.mos.base.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
public class AesUtils {
	/**
	 * AES加密
	 *
	 * @param content    待加密的内容
	 * @param encryptKey 加密密钥
	 * @return 加密后的byte[]
	 * @throws Exception
	 */
	public static byte[] aesEncryptToBytes(String content, String encryptKey) throws Exception {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
		random.setSeed(encryptKey.getBytes());
		kgen.init(128, random);
		
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(kgen.generateKey().getEncoded(), "AES"));
		
		return cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * AES解密
	 *
	 * @param encryptBytes 待解密的byte[]
	 * @param decryptKey   解密密钥
	 * @return 解密后的String
	 * @throws Exception
	 */
	public static String aesDecryptByBytes(byte[] encryptBytes, String decryptKey) throws Exception {
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
		random.setSeed(decryptKey.getBytes());
		kgen.init(128, random);
		
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(kgen.generateKey().getEncoded(), "AES"));
		byte[] decryptBytes = cipher.doFinal(encryptBytes);
		
		return new String(decryptBytes, StandardCharsets.UTF_8);
	}
	
	/**
	 * base 64 encode
	 *
	 * @param bytes 待编码的byte[]
	 * @return 编码后的base 64 code
	 */
	public static String base64Encode(byte[] bytes) {
		return Base64.getUrlEncoder().encodeToString(bytes);
	}
	
	/**
	 * AES加密为base 64 code
	 *
	 * @param content    待加密的内容
	 * @param encryptKey 加密密钥
	 * @return 加密后的base 64 code
	 * @throws Exception
	 */
	public static String aesEncode(String content, String encryptKey) throws Exception {
		return base64Encode(aesEncryptToBytes(content, encryptKey)).replace("\r", "").replace("\n", "");
	}
	
	/**
	 * base 64 decode
	 *
	 * @param base64Code 待解码的base 64 code
	 * @return 解码后的byte[]
	 * @throws Exception
	 */
	public static byte[] base64Decode(String base64Code) throws Exception {
		return StringUtils.isEmpty(base64Code) ? null : Base64.getUrlDecoder().decode(base64Code);
	}
	
	/**
	 * 将base 64 code AES解密
	 *
	 * @param encryptStr 待解密的base 64 code
	 * @param decryptKey 解密密钥
	 * @return 解密后的string
	 * @throws Exception
	 */
	public static String aesDecode(String encryptStr, String decryptKey) throws Exception {
		return StringUtils.isEmpty(encryptStr) ? null : aesDecryptByBytes(base64Decode(encryptStr), decryptKey);
	}
	
}
