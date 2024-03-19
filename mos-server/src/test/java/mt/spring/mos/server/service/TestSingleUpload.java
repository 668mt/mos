//package mt.spring.mos.server.service;
//
//import mt.spring.mos.base.stream.MosEncodeInputStream;
//import mt.spring.mos.sdk.entity.upload.UploadInfo;
//import org.apache.commons.codec.digest.DigestUtils;
//import org.apache.commons.io.FileUtils;
//import org.junit.Test;
//
//import java.io.*;
//import java.security.MessageDigest;
//import java.util.Collection;
//import java.util.Set;
//import java.util.stream.Collectors;
//
///**
// * @Author Martin
// * @Date 2023/9/8
// */
//public class TestSingleUpload extends BaseMosSdkTest {
//	@Test
//	public void test() throws IOException {
//		File file = new File("D:/test/upload/1 (2).mp4");
//		mosSdk.uploadFile(file, new UploadInfo("/test/202309/" + file.getName(), false));
////		Collection<File> files = FileUtils.listFiles(new File("D:/test/upload"), null, false);
////		System.out.println(files.size());
////		Set<String> collect = files.stream().map(file -> {
////			try (FileInputStream fileInputStream = new FileInputStream(file)) {
////				return DigestUtils.md5Hex(fileInputStream);
////			} catch (FileNotFoundException e) {
////				throw new RuntimeException(e);
////			} catch (IOException e) {
////				throw new RuntimeException(e);
////			}
////		}).collect(Collectors.toSet());
////		System.out.println(collect.size());
//	}
//}
