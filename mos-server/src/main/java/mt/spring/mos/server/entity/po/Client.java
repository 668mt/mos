package mt.spring.mos.server.entity.po;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mt.common.entity.ResResult;
import mt.spring.mos.sdk.utils.Assert;
import mt.spring.mos.server.entity.BaseEntity;
import mt.spring.mos.server.entity.dto.MergeFileResult;
import mt.spring.mos.server.utils.HttpClientServletUtils;
import mt.utils.JsonUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @Author Martin
 * @Date 2020/5/16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "mos_client")
public class Client extends BaseEntity {
	
	private static final long serialVersionUID = -7609365042803611738L;
	@Id
	private String clientId;
	private String ip;
	private Integer port;
	private String remark;
	private Integer weight;
	private Long totalStorageByte;
	private Long usedStorageByte;
	@Column(scale = 3)
	private BigDecimal totalStorageGb;
	@Column(scale = 3)
	private BigDecimal usedStorageGb;
	@Column(scale = 3)
	private BigDecimal usedPercent;
	private ClientStatus status;
	private Date lastBeatTime;
	@Transient
	private int priority_min;
	@Transient
	private int priority_max;
	
	public Integer getWeight() {
		return weight == null ? 50 : weight;
	}
	
	@Transient
	public String getUrl() {
		return "http://" + this.ip + ":" + this.port;
	}
	
	public BigDecimal getUsedPercent() {
		if (usedStorageByte == null || totalStorageByte == null) {
			return BigDecimal.ZERO;
		}
		return totalStorageByte == 0L ? BigDecimal.ZERO : BigDecimal.valueOf(usedStorageByte).divide(BigDecimal.valueOf(totalStorageByte), 3, RoundingMode.HALF_UP);
	}
	
	public BigDecimal getTotalStorageGb() {
		if (totalStorageByte == null) {
			return null;
		}
		return BigDecimal.valueOf(totalStorageByte).divide(BigDecimal.valueOf(1024L * 1024 * 1024), 3, RoundingMode.HALF_UP);
	}
	
	public BigDecimal getUsedStorageGb() {
		if (usedStorageByte == null) {
			return null;
		}
		return BigDecimal.valueOf(usedStorageByte).divide(BigDecimal.valueOf(1024L * 1024 * 1024), 3, RoundingMode.HALF_UP);
	}
	
	public enum ClientStatus {
		UP, DOWN
	}
	
	public ClientApi apis(RestTemplate restTemplate) {
		return new ClientApi(restTemplate, this);
	}
	
	@Data
	@Slf4j
	public static class ClientApi {
		private final RestTemplate restTemplate;
		private final Client client;
		
		public ClientApi(RestTemplate restTemplate, Client client) {
			this.restTemplate = restTemplate;
			this.client = client;
		}
		
		private ResResult post(String uri, Map<String, Object> params) {
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			for (Map.Entry<String, Object> stringObjectEntry : params.entrySet()) {
				body.add(stringObjectEntry.getKey(), stringObjectEntry.getValue());
			}
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			ResResult resResult = restTemplate.postForObject(client.getUrl() + uri, new org.springframework.http.HttpEntity<>(body, httpHeaders), ResResult.class);
			Assert.state(resResult != null, "请求资源服务器失败");
			Assert.state(resResult.isSuccess(), "请求资源服务器失败:" + resResult.getMessage());
			return resResult;
		}
		
		public void deleteFile(String pathname) {
			Map<String, Object> params = new HashMap<>();
			params.put("pathname", pathname);
			post("/client/deleteFile", params);
		}
		
		public void deleteDir(String path) {
			Map<String, Object> params = new HashMap<>();
			params.put("path", path);
			post("/client/deleteDir", params);
		}
		
		public void moveFile(String srcPathname, String desPathname) {
			moveFile(srcPathname, desPathname, false);
		}
		
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
		
		public long size(String pathname) {
			try {
				Map<String, Object> params = new HashMap<>();
				params.put("pathname", pathname);
				ResResult result = post("/client/size", params);
				return Long.parseLong(result.getResult() + "");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		@SneakyThrows
		public String md5(String pathname) {
			Map<String, Object> params = new HashMap<>();
			params.put("pathname", pathname);
			ResResult result = post("/client/md5", params);
			return (String) result.getResult();
		}
		
		public boolean isExists(String desPathname) {
			return size(desPathname) >= 0;
		}
		
		@SuppressWarnings("rawtypes")
		@Transient
		public Map getInfo(RestTemplate restTemplate) {
			return restTemplate.getForObject(client.getUrl() + "/client/info", Map.class);
		}
		
		@Transient
		@SuppressWarnings("rawtypes")
		public List getClientResources() {
			try {
				return restTemplate.getForObject(client.getUrl() + "/client/resources", List.class);
			} catch (Exception e) {
				return Collections.emptyList();
			}
		}
		
		@Transient
		@SuppressWarnings("rawtypes")
		public boolean isEnableImport() {
			Map info = getInfo(restTemplate);
			if (info == null) {
				return false;
			}
			return "true".equals(info.get("isEnableAutoImport") + "");
		}
		
		public MergeFileResult mergeFiles(String path, int chunks, String desPathname, boolean getMd5) {
			log.info("开始合并{}", desPathname);
			JSONObject params = new JSONObject();
			params.put("path", path);
			params.put("chunks", chunks);
			params.put("desPathname", desPathname);
			params.put("getMd5", getMd5);
			String uri = client.getUrl() + "/client/mergeFiles";
			JSONObject jsonObject = restTemplate.postForObject(uri, new org.springframework.http.HttpEntity<>(params), JSONObject.class);
			Assert.state(jsonObject != null && "ok".equalsIgnoreCase(jsonObject.getString("status")), "合并失败:" + jsonObject);
			log.info("合并结果：{}", jsonObject);
			return jsonObject.getJSONObject("result").toJavaObject(MergeFileResult.class);
		}
		
		public void upload(CloseableHttpClient httpClient, InputStream inputStream, String pathname) throws IOException {
			try {
				log.info("开始上传{}...", pathname);
				String uri = client.getUrl() + "/client/upload";
				CloseableHttpResponse response = HttpClientServletUtils.httpClientUploadFile(httpClient, uri, inputStream, pathname);
				HttpEntity entity = response.getEntity();
				Assert.notNull(entity, "客户端返回内容空");
				String result = EntityUtils.toString(entity);
				log.info("{}上传结果：{}", pathname, result);
				ResResult resResult = JsonUtils.toObject(result, ResResult.class);
				Assert.state(resResult.isSuccess(), "上传失败,clientMsg:" + resResult.getMessage());
			} finally {
				if (inputStream != null) {
					IOUtils.close(inputStream);
				}
			}
		}
	}
}
