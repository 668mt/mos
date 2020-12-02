package mt.spring.mos.base.utils;

/**
 * @Author Martin
 * @Date 2020/12/2
 */
public class MosFileEncodeUtils {
	public static byte[] getFileHead(String content) throws Exception {
		return AesUtils.aesEncryptToBytes(content, "mos");
	}
}
