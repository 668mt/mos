package mt.spring.mos.server.utils;

import mt.spring.mos.sdk.RSAUtils;
import com.alibaba.fastjson.JSONObject;
import mt.utils.Assert;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
public class OssSignUtils {
	public static void checkSign(String pathname, String sign, String secretKey, String bucketName) {
		try {
			Assert.notNull(sign);
			Assert.notNull(secretKey);
			Assert.notNull(pathname);
			Assert.notNull(bucketName);
			if (!pathname.startsWith("/")) {
				pathname = "/" + pathname;
			}
			String decrypt = RSAUtils.decryptLarge(sign, secretKey);
			JSONObject info = JSONObject.parseObject(decrypt);
			String pathname2 = info.getString("pathname");
			if (!pathname2.startsWith("/")) {
				pathname2 = "/" + pathname2;
			}
			Assert.state(pathname.equals(pathname2), "文件名和签名不一致");
			Assert.state(bucketName.equals(info.getString("bucketName")), "bucketName和签名不一致");
			Long expireSeconds = info.getLong("expireSeconds");
			if (expireSeconds != null) {
				Long signTime = info.getLong("signTime");
				Assert.state(System.currentTimeMillis() < signTime + expireSeconds * 1000, "签名已过期");
			}
		} catch (Exception e) {
			throw new RuntimeException("签名验证失败:" + e.getMessage(), e);
		}
	}
}
