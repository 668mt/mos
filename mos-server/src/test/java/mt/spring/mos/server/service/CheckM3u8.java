//package mt.spring.mos.server.service;
//
//import lombok.extern.slf4j.Slf4j;
//import mt.spring.mos.base.utils.RegexUtils;
//import mt.spring.mos.sdk.MosSdk;
//import mt.spring.mos.sdk.entity.DirAndResource;
//import mt.spring.mos.sdk.entity.PageInfo;
//import org.apache.commons.collections.CollectionUtils;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.springframework.boot.logging.LogLevel;
//import org.springframework.boot.logging.LoggingSystem;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.stream.Collectors;
//
///**
// * @Author Martin
// * @Date 2020/11/21
// */
//@Slf4j
//public class CheckM3u8 {
//	private MosSdk mosSdk;
//
//	@Before
//	public void setUp() {
//		LoggingSystem.get(getClass().getClassLoader()).setLogLevel("root", LogLevel.INFO);
//		LoggingSystem.get(getClass().getClassLoader()).setLogLevel("mt.spring", LogLevel.DEBUG);
//		mosSdk = new MosSdk("http://192.168.0.2:4100", 4, "default", "zD5bakID7tJiUoKgbmERfw==");
//	}
//
//	@After
//	public void after() {
//		mosSdk.shutdown();
//	}
//
//	@Test
//	public void check() throws IOException {
//		PageInfo<DirAndResource> list = mosSdk.list("/mc/202105", null, null, null);
//		List<String> checks = new ArrayList<>();
//		for (DirAndResource dirAndResource : list.getList()) {
//			String path = dirAndResource.getPath();
////			if (!path.contains("129753")) {
////				continue;
////			}
//			PageInfo<DirAndResource> segmentPage = mosSdk.list(path, null, null, null);
//			List<DirAndResource> segments = segmentPage.getList();
//			DirAndResource index = segments.stream()
//					.filter(dirAndResource1 -> dirAndResource1.getFileName().equalsIgnoreCase("index.m3u8"))
//					.findFirst().orElse(null);
//			if (index == null) {
//				log.warn("{}不完整", path);
//				checks.add(path);
//				continue;
//			}
//			List<Integer> names = segments.stream()
//					.map(DirAndResource::getFileName)
//					.filter(s -> !s.equalsIgnoreCase("index.m3u8"))
//					.map(s -> Integer.parseInt(RegexUtils.findFirst(s, "segment_(\\d+)\\.ts", 1)))
//					.sorted(Integer::compareTo)
//					.collect(Collectors.toList());
//			Collections.reverse(names);
//
//			if (CollectionUtils.isEmpty(names) || names.size() != names.get(0) + 1) {
//				log.warn("{}不完整", path);
//				checks.add(path);
//			}
//		}
//		log.info("检查结果如下：");
//		for (String check : checks) {
//			log.warn("{}不完整", check);
//		}
//	}
//
//	@Test
//	public void deleteDir() throws IOException {
//		List<String> dirs = new ArrayList<>();
//		dirs.add("/mc/202105/129515");
//		dirs.add("/mc/202105/129774");
//		dirs.add("/mc/202105/129775");
//		dirs.add("/mc/202105/128875");
//		dirs.add("/mc/202105/128947");
//		dirs.add("/mc/202105/128982");
//		for (String dir : dirs) {
//			mosSdk.deleteDir(dir);
//		}
//	}
//}
