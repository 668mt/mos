package mt.spring.mos.sdk.utils;

import mt.spring.mos.sdk.entity.MosConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * 地址解析测试（纯解析，不访问网络）
 *
 * @Author Martin
 * @Date 2026/10/30
 */
public class HostChooseUtilsTest {
	
	@Test
	public void testParseAddress() {
		//单地址
		HostChooseUtils.ParseResult r1 = HostChooseUtils.parseAddress("http://localhost:9700");
		Assert.assertEquals("http://localhost:9700", r1.getHost());
		Assert.assertNull(r1.getExternHost());
		Assert.assertEquals(100, r1.getWeight());
		
		//带外网地址
		HostChooseUtils.ParseResult r2 = HostChooseUtils.parseAddress("http://localhost:9700[http://rs1.com]");
		Assert.assertEquals("http://localhost:9700", r2.getHost());
		Assert.assertEquals("http://rs1.com", r2.getExternHost());
		Assert.assertEquals(100, r2.getWeight());
		
		//带权重
		HostChooseUtils.ParseResult r3 = HostChooseUtils.parseAddress("http://localhost:9700(70)");
		Assert.assertEquals("http://localhost:9700", r3.getHost());
		Assert.assertNull(r3.getExternHost());
		Assert.assertEquals(70, r3.getWeight());
		
		//带外网地址 + 权重
		HostChooseUtils.ParseResult r4 = HostChooseUtils.parseAddress("http://localhost:9700[http://rs1.com](70)");
		Assert.assertEquals("http://localhost:9700", r4.getHost());
		Assert.assertEquals("http://rs1.com", r4.getExternHost());
		Assert.assertEquals(70, r4.getWeight());
		
		//没写协议时补 http://
		HostChooseUtils.ParseResult r5 = HostChooseUtils.parseAddress("192.168.0.1:4100");
		Assert.assertEquals("http://192.168.0.1:4100", r5.getHost());
		
		//外网地址没写协议时也补
		HostChooseUtils.ParseResult r6 = HostChooseUtils.parseAddress("http://localhost:9700[rs1.com](30)");
		Assert.assertEquals("http://localhost:9700", r6.getHost());
		Assert.assertEquals("http://rs1.com", r6.getExternHost());
		Assert.assertEquals(30, r6.getWeight());
		
		//https 保留
		HostChooseUtils.ParseResult r7 = HostChooseUtils.parseAddress("https://mos.example.com[https://rs.example.com]");
		Assert.assertEquals("https://mos.example.com", r7.getHost());
		Assert.assertEquals("https://rs.example.com", r7.getExternHost());
	}
	
	@Test
	public void testGetAvailableHostByWeight() {
		List<String> hosts = Arrays.asList(
				"http://192.168.0.174:4102[http://rs1.com](100)",
				"http://192.168.0.2:4100[http://rs2.com](0)");
		//权重 0 的不参与选择
		for (int i = 0; i < 20; i++) {
			Host host = HostChooseUtils.getAvailableHostByWeight(hosts, null);
			Assert.assertNotNull(host);
			Assert.assertEquals("http://192.168.0.174:4102", host.getHost());
			Assert.assertEquals("http://rs1.com", host.getExternHost());
			Assert.assertEquals(Double.valueOf(100), host.getWeight());
		}
	}
	
	@Test
	public void testGetAvailableHostByWeightWithoutExtern() {
		List<String> hosts = Arrays.asList("http://192.168.0.174:4102(100)");
		Host host = HostChooseUtils.getAvailableHostByWeight(hosts, null);
		Assert.assertNotNull(host);
		Assert.assertEquals("http://192.168.0.174:4102", host.getHost());
		Assert.assertNull(host.getExternHost());
		//没配外网地址时，对外地址回退到内网地址
		Assert.assertEquals("http://192.168.0.174:4102", host.getExternHostOrDefault());
	}
	
	@Test
	public void testMosConfigHost() {
		//连不通的地址 -> 没有可用服务器
		MosConfig config = new MosConfig(Arrays.asList("http://127.0.0.1:19999[http://rs1.com](100)"), "default", "key", 5L);
		Assert.assertNull(config.getHostInfo());
		Assert.assertNull(config.getHost());
		Assert.assertNull(config.getDomain());
	}
}
