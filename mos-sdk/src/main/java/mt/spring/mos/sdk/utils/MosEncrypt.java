package mt.spring.mos.sdk.utils;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Random;

/**
 * @Author Martin
 * @Date 2020/11/3
 */
@Slf4j
public class MosEncrypt {
	public static String encrypt(String key, String pathname, String bucketName, long openId, long expireSeconds) throws Exception {
		JSONArray array = new JSONArray();
		array.add(pathname);
		array.add(bucketName);
		array.add(expireSeconds);
		array.add(System.currentTimeMillis());
		String content = array.toJSONString();
		char lengthChar = (char) ((openId + "").length() + 64);
		StringBuilder sb = new StringBuilder();
		for (char c1 : (openId + "").toCharArray()) {
			char c2 = (char) (Integer.parseInt(c1 + "") + 65);
			sb.append(c2);
		}
		String sign = AesUtils.aesEncode(content, key + openId);
		//65-122
		int random = new Random().nextInt(Math.min(sign.length(), 56));
		String s1 = sign.substring(0, random);
		String s2 = sign.substring(random);
		String openIdStr = lengthChar + sb.toString();
		char rChar = (char) (random + 65);
		return rChar + s1 + openIdStr + s2;
	}
	
	public interface KeyGetor {
		String getKey(long openId);
	}
	
	public static MosEncryptContent decrypt(String key, String sign) throws Exception {
		return decrypt(openId -> key, sign);
	}
	
	public static MosEncryptContent decrypt(KeyGetor keyGetor, String sign) throws Exception {
		int random = sign.charAt(0) - 65 + 1;
		String s1 = sign.substring(1, random);
		String openIdAndS2 = sign.substring(random);
		
		int length = openIdAndS2.charAt(0) - 64;
		String openIdChars = openIdAndS2.substring(1, 1 + length);
		StringBuilder sb = new StringBuilder();
		for (char c : openIdChars.toCharArray()) {
			int o = c - 65;
			sb.append(o);
		}
		long openId = Long.parseLong(sb.toString());
		String s2 = openIdAndS2.substring(1 + length);
		sign = s1 + s2;
		String json = AesUtils.aesDecode(sign, keyGetor.getKey(openId) + openId);
		JSONArray array = JSONArray.parseArray(json);
		Assert.notNull(array, "解密失败");
		MosEncryptContent mosEncryptContent = new MosEncryptContent();
		mosEncryptContent.setPathname(array.getString(0));
		mosEncryptContent.setBucketName(array.getString(1));
		mosEncryptContent.setExpireSeconds(array.getLong(2));
		mosEncryptContent.setSignTime(array.getLong(3));
		mosEncryptContent.setOpenId(openId);
		return mosEncryptContent;
	}
	
	/**
	 * 随机生成秘钥
	 */
	public static String generateKey() throws Exception {
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(128);
		//要生成多少位，只需要修改这里即可128, 192或256
		SecretKey sk = kg.generateKey();
		byte[] b = sk.getEncoded();
		return Base64.getUrlEncoder().encodeToString(b);
	}
	
	@Data
	public static class MosEncryptContent {
		private String pathname;
		private String bucketName;
		private long openId;
		private long expireSeconds;
		private long signTime;
	}

//	public static void main(String[] args) throws Exception {
//		String s = generateKey();
//		String encrypt = encrypt(s, "asdasd", "default", 10, 10);
//		System.out.println(encrypt);
//		MosEncryptContent decrypt = decrypt(s, encrypt);
//		System.out.println(decrypt);
//	}
}
