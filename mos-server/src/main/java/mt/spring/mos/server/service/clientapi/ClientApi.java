package mt.spring.mos.server.service.clientapi;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import mt.common.entity.ResResult;
import mt.spring.mos.base.entity.ClientInfo;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.server.entity.dto.MergeFileResult;
import mt.spring.mos.server.entity.po.Client;
import mt.spring.mos.server.utils.HttpClientServletUtils;
import mt.utils.JsonUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2021/1/9
 */
@Slf4j
public class ClientApi implements IClientApi {
	private final Client client;
	private final CloseableHttpClient httpClient;
	private final RestTemplate restTemplate;
	private final ExecutorService executorService;
	
	public ClientApi(Client client, RestTemplate restTemplate, CloseableHttpClient httpClient, ExecutorService executorService) {
		this.client = client;
		this.httpClient = httpClient;
		this.restTemplate = restTemplate;
		this.executorService = executorService;
	}
	
	private void post(String uri, Map<String, Object> params) {
		post(uri, params, Object.class);
	}
	
	private <T> T post(String uri, Map<String, Object> params, Class<T> type) {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		for (Map.Entry<String, Object> stringObjectEntry : params.entrySet()) {
			body.add(stringObjectEntry.getKey(), stringObjectEntry.getValue());
		}
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		JSONObject resResult = restTemplate.postForObject(client.getUrl() + uri, new org.springframework.http.HttpEntity<>(body, httpHeaders), JSONObject.class);
		Assert.state(resResult != null, "请求资源服务器失败");
		Assert.state("ok".equalsIgnoreCase(resResult.getString("status")), "请求资源服务器失败:" + resResult.getString("message"));
		return resResult.getObject("result", type);
	}
	
	@Override
	public Map<String, Boolean> isExists(List<String> pathname) {
		JSONObject params = new JSONObject();
		params.put("pathname", pathname);
		String uri = client.getUrl() + "/client/isExists";
		JSONObject jsonObject = restTemplate.postForObject(uri, new org.springframework.http.HttpEntity<>(params), JSONObject.class);
		Assert.state(jsonObject != null && "ok".equalsIgnoreCase(jsonObject.getString("status")), "查询失败:" + jsonObject);
		Map<String, Boolean> result = new HashMap<>();
		JSONObject resultMap = jsonObject.getJSONObject("result");
		for (Map.Entry<String, Object> stringObjectEntry : resultMap.entrySet()) {
			result.put(stringObjectEntry.getKey(), resultMap.getBoolean(stringObjectEntry.getKey()));
		}
		return result;
	}
	
	@Override
	public void deleteFile(String pathname) {
		Map<String, Object> params = new HashMap<>();
		params.put("pathname", pathname);
		post("/client/deleteFile", params);
	}
	
	@Override
	public void deleteDir(String path) {
		Map<String, Object> params = new HashMap<>();
		params.put("path", path);
		post("/client/deleteDir", params);
	}
	
	@Override
	public void moveFile(String srcPathname, String desPathname, boolean cover) {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("srcPathname", srcPathname);
		params.add("desPathname", desPathname);
		params.add("cover", cover + "");
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		ResponseEntity<ResResult> exchange = restTemplate.exchange(client.getUrl() + "/client/moveFile", HttpMethod.PUT, new org.springframework.http.HttpEntity<>(params, httpHeaders), ResResult.class, srcPathname, desPathname, cover);
		ResResult result = exchange.getBody();
		Assert.state(result != null && result.isSuccess(), "请求客户端失败");
	}
	
	@Override
	public long size(String pathname) {
		try {
			Map<String, Object> params = new HashMap<>();
			params.put("pathname", pathname);
			return post("/client/size", params, Long.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String md5(String pathname) {
		Map<String, Object> params = new HashMap<>();
		params.put("pathname", pathname);
		return post("/client/md5", params, String.class);
	}
	
	@Override
	public boolean isExists(String pathname) {
		return size(pathname) >= 0;
	}
	
	@Override
	public ClientInfo getInfo() {
		JSONObject forObject = restTemplate.getForObject(client.getUrl() + "/client/info", JSONObject.class);
		Assert.notNull(forObject, "请求失败");
		return forObject.toJavaObject(ClientInfo.class);
	}
	
	@Override
	public boolean isEnableImport() {
		ClientInfo info = getInfo();
		if (info == null) {
			return false;
		}
		return info.getIsEnableAutoImport() != null && info.getIsEnableAutoImport();
	}
	
	@Override
	public MergeFileResult mergeFiles(String path, int chunks, String desPathname, boolean getMd5, boolean encode) {
		log.info("开始合并{} -> {},chunks:{},getMd5:{},encode:{}", path, desPathname, chunks, getMd5, encode);
		JSONObject params = new JSONObject();
		params.put("path", path);
		params.put("chunks", chunks);
		params.put("desPathname", desPathname);
		params.put("getMd5", getMd5);
		params.put("encode", encode);
		String uri = client.getUrl() + "/client/mergeFiles";
		JSONObject jsonObject = restTemplate.postForObject(uri, new org.springframework.http.HttpEntity<>(params), JSONObject.class);
		Assert.state(jsonObject != null && "ok".equalsIgnoreCase(jsonObject.getString("status")), "合并失败:" + jsonObject);
		log.info("合并结果：{}", jsonObject);
		return jsonObject.getJSONObject("result").toJavaObject(MergeFileResult.class);
	}
	
	@Override
	public void upload(InputStream inputStream, String pathname) throws IOException {
		try {
			log.info("开始上传{}...", pathname);
			String uri = client.getUrl() + "/client/upload";
			try (CloseableHttpResponse response = HttpClientServletUtils.httpClientUploadFile(httpClient, uri, inputStream, pathname)) {
				org.apache.hc.core5.http.HttpEntity entity = response.getEntity();
				Assert.notNull(entity, "客户端返回内容空");
				String result = org.apache.hc.core5.http.io.entity.EntityUtils.toString(entity);
				log.info("{}上传结果：{}", pathname, result);
				ResResult resResult = JsonUtils.toObject(result, ResResult.class);
				Assert.state(resResult.isSuccess(), "上传失败,clientMsg:" + resResult.getMessage());
			} catch (ParseException e) {
				log.error("上传失败：{}", e.getMessage(), e);
				throw new RuntimeException(e);
			}
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}
	
	@Override
	public boolean isAlive() {
		try {
			Future<?> future = executorService.submit(() -> {
				String result = restTemplate.getForObject(client.getUrl() + "/client/health", String.class);
				Assert.state("ok".equalsIgnoreCase(result), "客户端不可用");
			});
			future.get(5, TimeUnit.SECONDS);
			return true;
		} catch (Exception e) {
			log.warn("客户端不可用：{}", client.getUrl());
			return false;
		}
	}
}
