package mt.spring.mos.sdk;

import mt.spring.mos.sdk.entity.DirAndResource;
import mt.spring.mos.sdk.entity.PageInfo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/9/28
 */
public class MosSdkTest {
	
	private MosSdk sdk;
	
	@Before
	public void setUp() {
		LoggingSystem.get(MosSdkTest.class.getClassLoader()).setLogLevel("root", LogLevel.INFO);
		long openId = 5;
		String bucketName = "default";
		String secretkey = "b-T3wXaUu5umA3vumqEIVA==";
		String url = "http://localhost:9700";
		sdk = new MosSdk(url, openId, bucketName, secretkey);
	}

//	@Test
//	public void testEncryptSpeed() throws Exception {
//		JSONObject sign = new JSONObject();
////		sign.put("pathname", "C:\\\\Users\\\\Administrator\\\\Desktop\\\\mos\\\\server-1.0\\\\application.properties");
//		List<Object> list = new ArrayList<>();
//		sign.put("p", "/mc/201231/asdc/112345.mp4");
//		sign.put("b", "default");
//		sign.put("e", 10);
//		sign.put("s", System.currentTimeMillis());
//		list.add(sign.get("p"));
//		list.add(sign.get("b"));
//		list.add(sign.get("e"));
//		list.add(sign.get("s"));
//		String content = JSONObject.toJSONString(list);
//		String key = UUID.randomUUID().toString();
//		AesUtils.aesEncode(content + "asdas", key + "asdf");
//		long s1 = System.currentTimeMillis();
//		String sign1 = AesUtils.aesEncode(content, key);
//		for (int i = 0; i < 100; i++) {
//			AesUtils.aesEncode(i + content, key);
//		}
//		System.out.println("aes加密用时：" + (System.currentTimeMillis() - s1));
//		System.out.println(sign1);
//		long e1 = System.currentTimeMillis();
//		String r1 = AesUtils.aesDecode(sign1, key);
//		System.out.println("aes解密用时：" + (System.currentTimeMillis() - e1));
//		System.out.println(r1);
//
//
//		long s2 = System.currentTimeMillis();
//		String sign2 = RSAUtils.encryptLarge(content, sdk.getPublicKey());
//		for (int i = 0; i < 100; i++) {
//			RSAUtils.encryptLarge(i + content, sdk.getPublicKey());
//		}
//		System.out.println("rsa加密用时：" + (System.currentTimeMillis() - s2));
//		System.out.println(sign2);
//		long e2 = System.currentTimeMillis();
//		String r2 = RSAUtils.decryptLarge(sign2, "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIWo4wKuaaWU1QeAIg8Mw6Tdf5gwBVDgQDwTWysqRznXF7x0rk5DW-dE50ifFKpMtZpAWXIUgJ4kPhKfZMsidMwT6BOYP4Pzd8J4dDtdxwh88TSdX0e3pZhY5EeVz1C_5uToAcxCJbAbMjCIc1RlY1uW_LLJB3CoCoQmDQ4Yh0PdAgMBAAECgYAK7WnR2EyO2wInoNQoAtUj72_1E0f7-rmFr9duSgCUu_8WyztM7rjeyYVXLJpV6fuW5tfpOb_iLfhdMWbeLKz7u_HVE0whvSo5PexhOdF0AlSroU0F6RPFrsjXo1Zqrcmd7PKe7RFRRyGlqcCSPGwEHYyt5yrKiTTYFztjvIz7QQJBAMlkzLYrXcLAVgJn_E6NRkXcNK2tV9H6mqwl8fqMBSZcYUiETGV6TLTCvxUZ5nlyjp328w0Ptua5eTQ2da6M3eUCQQCp5oNRQCbkjvuyE4ykpV2B3xtc4v79kpZZlTlohNjrnH4mM3KwmwgGc2ssJbuoj2a_E4kVFxU0TXHz3qvpIa6ZAkEAuYQDFyQngIjQMfM79dgpjZzlYb82EJU6-a0SlGt7KvUaIETOYwQfuLtUbcmoBuPCAD1FcxiwsSTVQ4WPLjVRCQJAcrHMoIMgPldQDs0nutZvo4bL4A7VeLUGjn8wfCCCl7vTZVKB8DsYVdKA3RKz9nyoLgGwHu6wP9iwPqNphj5YsQJAJycu2O5sJSj0ka0KplfdC6Ile_qJyVk7pubrJr6wHwsrIkmGGQLQZTH1dwlII149qeLeSdM1fl_i2Vhb_bTUqQ==");
//		System.out.println("rsa解密用时：" + (System.currentTimeMillis() - e2));
//		System.out.println(r2);
//
//	}
	
	@Test
	public void testUpload() throws IOException {
//		sdk.upload("test.properties", new FileInputStream("C:\\Users\\Administrator\\Desktop\\mos\\server-1.0\\application.properties"), true);
		sdk.upload("/test/测试+这是2&.txt",
				new FileInputStream("G:\\test-upload\\10\\test.txt"), true);
		System.out.println("上传完成");
	}
	
	@Test
	public void testDelete() throws IOException {
		sdk.deleteFile("/test/测试+这是2&.txt");
//		sdk.deleteFile("/test");
		boolean exists = sdk.isExists("/test/测试+这是2&.txt");
		System.out.println(exists);
	}
	
	@Test
	public void testList() {
		PageInfo<DirAndResource> list = sdk.list("/test", null, null, null);
		List<DirAndResource> rows = list.getList();
		for (DirAndResource row : rows) {
			System.out.println(row.getIsDir() + "--" + row.getPath() + "--" + row.getFileName());
		}
	}
	
	@Test
	public void getUrl() throws Exception {
		String pathanme = "/测试/脚本AASDASDDDF.sql";
		String url = sdk.getEncodedUrl(pathanme, 30L);
		System.out.println(url);
	}
	
}