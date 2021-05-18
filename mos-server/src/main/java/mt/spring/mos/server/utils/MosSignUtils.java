package mt.spring.mos.server.utils;

import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.sdk.utils.MosEncrypt;
import mt.utils.common.Assert;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@Slf4j
public class MosSignUtils {
	public static MosEncrypt.MosEncryptContent checkSign(String pathname, String sign, MosEncrypt.KeyGetor keyGetor, String bucketName) {
		try {
			Assert.notNull(sign, "sign must not null");
			Assert.notNull(keyGetor, "keyGetor must not null");
			Assert.notNull(pathname, "pathname must not null");
			Assert.notNull(bucketName, "bucketName must not null");
			if (!pathname.startsWith("/")) {
				pathname = "/" + pathname;
			}
			MosEncrypt.MosEncryptContent content = MosEncrypt.decrypt(keyGetor, sign);
			String pathname2 = content.getPathname();
			if (!pathname2.startsWith("/")) {
				pathname2 = "/" + pathname2;
			}
			Assert.state(pathname.equals(pathname2), "文件名" + pathname + "和签名中的文件" + pathname2 + "不一致");
			Assert.state(bucketName.equals(content.getBucketName()), "bucketName和签名不一致:" + pathname);
			long expireSeconds = content.getExpireSeconds();
			if (expireSeconds > 0) {
				long signTime = content.getSignTime();
				Assert.state(System.currentTimeMillis() < signTime + expireSeconds * 1000, "签名已过期：" + pathname);
			}
			return content;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new IllegalStateException("签名验证失败:" + e.getMessage() + ":" + pathname, e);
		}
	}
}
