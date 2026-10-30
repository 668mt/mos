package mt.spring.mos.sdk;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import mt.spring.mos.sdk.entity.MosConfig;
import mt.spring.mos.sdk.http.ServiceClient;
import okhttp3.Response;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 用本地 http server 验证 okhttp 版的 ServiceClient
 */
public class OkHttpClientTest {
	
	private static final AtomicReference<String> lastBody = new AtomicReference<>("");
	private static final AtomicReference<String> lastContentType = new AtomicReference<>("");
	private static final AtomicReference<String> lastMethod = new AtomicReference<>("");
	
	@Test
	public void testServiceClient() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try (InputStream in = exchange.getRequestBody()) {
				in.transferTo(out);
			}
			lastBody.set(out.toString(StandardCharsets.UTF_8));
			lastMethod.set(exchange.getRequestMethod());
			lastContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
			byte[] resp = ("{\"status\":\"ok\",\"result\":\"" + exchange.getRequestMethod() + "\"}").getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, resp.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(resp);
			}
		});
		server.start();
		int port = server.getAddress().getPort();
		try {
			MosConfig config = new MosConfig("http://127.0.0.1:" + port, "bucket", "key", 1L);
			ServiceClient client = new ServiceClient(config);
			
			// 1) GET + JSON 解析
			String r1 = client.get("http://127.0.0.1:" + port + "/open/x?a=1", String.class);
			Assert.assertEquals("GET", r1);
			Assert.assertEquals("GET", lastMethod.get());
			
			// 2) POST JSON
			Map<String, Object> body = new HashMap<>();
			body.put("k", "v");
			String r2 = client.postJson("http://127.0.0.1:" + port + "/s", body, String.class);
			Assert.assertEquals("POST", r2);
			Assert.assertEquals("POST", lastMethod.get());
			Assert.assertTrue("请求体应为 json: " + lastBody.get(), lastBody.get().contains("\"k\""));
			Assert.assertTrue("Content-Type 应为 json: " + lastContentType.get(), lastContentType.get().startsWith("application/json"));
			
			// 3) POST multipart（带文件流 + 其它参数）
			Map<String, Object> params = new HashMap<>();
			params.put("pathname", "/a/b.txt");
			try (InputStream in = new java.io.ByteArrayInputStream("hello-mos".getBytes(StandardCharsets.UTF_8))) {
				try (Response response = client.upload("http://127.0.0.1:" + port + "/upload", in, params, 9)) {
					Assert.assertTrue(response.isSuccessful());
				}
			}
			String multipart = lastBody.get();
			Assert.assertTrue("应包含文件内容: " + multipart, multipart.contains("hello-mos"));
			Assert.assertTrue("应包含文件名: " + multipart, multipart.contains("filename=\"file\""));
			Assert.assertTrue("应包含表单参数: " + multipart, multipart.contains("pathname"));
			Assert.assertTrue("Content-Type 应为 multipart: " + lastContentType.get(), lastContentType.get().startsWith("multipart/form-data"));
			
			// 4) DELETE
			try (Response response = client.delete("http://127.0.0.1:" + port + "/open/del")) {
				Assert.assertTrue(response.isSuccessful());
			}
			Assert.assertEquals("DELETE", lastMethod.get());
			
			// 5) 错误状态码
			HttpServer errServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			errServer.createContext("/", exchange -> {
				exchange.sendResponseHeaders(500, -1);
				exchange.close();
			});
			errServer.start();
			try {
				ServiceClient errClient = new ServiceClient(new MosConfig("http://127.0.0.1:" + errServer.getAddress().getPort(), "b", "k", 1L));
				try {
					errClient.get("http://127.0.0.1:" + errServer.getAddress().getPort() + "/x", String.class);
					Assert.fail("500 应该抛异常");
				} catch (IllegalStateException e) {
					Assert.assertTrue(e.getMessage().contains("请求错误:500"));
				}
			} finally {
				errServer.stop(0);
			}
		} finally {
			server.stop(0);
		}
	}
}
