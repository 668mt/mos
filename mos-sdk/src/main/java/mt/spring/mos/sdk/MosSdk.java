package mt.spring.mos.sdk;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.sdk.entity.DirAndResource;
import mt.spring.mos.sdk.entity.MosConfig;
import mt.spring.mos.sdk.entity.PageInfo;
import mt.spring.mos.sdk.http.ServiceClient;
import mt.spring.mos.sdk.interfaces.MosApi;
import mt.spring.mos.sdk.entity.upload.UploadConfig;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import mt.spring.mos.sdk.upload.UploadOperation;
import mt.spring.mos.sdk.upload.UploadProcessListener;
import mt.spring.mos.sdk.utils.Assert;
import mt.spring.mos.sdk.utils.MosEncrypt;
import mt.spring.mos.sdk.utils.RegexUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@Data
@Slf4j
public class MosSdk implements MosApi {
	private ServiceClient client;
	private MosConfig mosConfig;
	private UploadConfig uploadConfig;
	private UploadOperation uploadOperation;
	
	@Override
	public void shutdown() {
		uploadOperation.shutdown();
		client.shutdown();
	}
	
	public MosSdk(String host, long openId, String bucketName, String secretKey) {
		this(host, openId, bucketName, secretKey, new UploadConfig());
	}
	
	public MosSdk(String host, long openId, String bucketName, String secretKey, UploadConfig uploadConfig) {
		if (host.endsWith("/")) {
			host = host.substring(0, host.length() - 1);
		}
		this.mosConfig = new MosConfig(host, bucketName, secretKey, openId);
		this.uploadConfig = uploadConfig;
		client = new ServiceClient();
		this.uploadOperation = new UploadOperation(this, mosConfig, uploadConfig, client);
	}
	
	public void setUploadConfig(UploadConfig uploadConfig){
		this.uploadOperation.setUploadConfig(uploadConfig);
	}
	
	@Override
	public String getSign(@NotNull String pathname, @Nullable Integer expired, @Nullable TimeUnit expiredTimeUnit) {
		try {
			long expireSeconds;
			if (expired == null || expiredTimeUnit == null) {
				expireSeconds = -1L;
			} else {
				expireSeconds = expiredTimeUnit.toSeconds(expired);
			}
			String encrypt = MosEncrypt.encrypt(mosConfig.getSecretKey(), pathname, mosConfig.getBucketName(), mosConfig.getOpenId(), expireSeconds);
			log.debug("{} 签名结果：{}", pathname, encrypt);
			return encrypt;
		} catch (Exception e) {
			throw new RuntimeException("加签失败：" + e.getMessage(), e);
		}
	}
	
	@Override
	public String getUrl(@NotNull String pathname, @Nullable Integer expired, @Nullable TimeUnit expiredTimeUnit) {
		return getUrl(pathname, expired, expiredTimeUnit, false, this.mosConfig.getHost());
	}
	
	@Override
	public String getEncodedUrl(@NotNull String pathname, @Nullable Integer expired, @Nullable TimeUnit expiredTimeUnit) {
		return getUrl(pathname, expired, expiredTimeUnit, true, this.mosConfig.getHost());
	}
	
	@Override
	public String getUrl(@NotNull String pathname, @Nullable Integer expired, @Nullable TimeUnit timeUnit, boolean urlEncode, String host) {
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		String sign = getSign(pathname, expired, timeUnit);
		if (urlEncode) {
			pathname = Stream.of(pathname.split("/")).map(s -> {
				try {
					return URLEncoder.encode(s, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.joining("/"));
		}
		try {
			return host +
					"/mos/" +
					mosConfig.getBucketName() +
					pathname +
					"?sign=" +
					sign;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	@Override
	public String getSafelyPathname(@NotNull String pathname) {
		Assert.notNull(pathname, "pathname不能为空");
		return pathname.replaceAll("\\.{2,}", "")
				.replaceAll("[:*?\"<>|,]", "");
	}
	
	@Override
	public String checkPathname(String pathname) {
		Assert.notNull(pathname, "pathname不能为空");
		pathname = pathname.replace("\\", "/");
		List<String> list = RegexUtils.findList(pathname, "[:*?\"<>|]", 0);
		Assert.state(CollectionUtils.isEmpty(list), "资源名不能包含: * ? \" < > | ");
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		return pathname;
	}
	
	private String getSignQueryParams(String pathname, Integer expireSeconds, boolean appendBucketName) {
		String sign = getSign(pathname, expireSeconds, TimeUnit.SECONDS);
		try {
			pathname = URLEncoder.encode(pathname, "UTF-8");
			sign = URLEncoder.encode(sign, "UTF-8");
			if (appendBucketName) {
				return "sign=" + sign + "&pathname=" + pathname + "&bucketName=" + mosConfig.getBucketName();
			} else {
				return "sign=" + sign + "&pathname=" + pathname;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 判断文件是否存在
	 *
	 * @param pathname 文件路径
	 * @return 文件是否存在
	 */
	@Override
	public boolean isExists(@NotNull String pathname) throws IOException {
		return client.get(mosConfig.getHost() + "/upload/" + mosConfig.getBucketName() + "/isExists?" + getSignQueryParams(pathname, 30, false), Boolean.class);
	}
	
	/**
	 * 删除文件
	 *
	 * @param pathname 文件路径
	 * @return 删除结果
	 */
	@Override
	public boolean deleteFile(@NotNull String pathname) throws IOException {
		log.info("删除文件：{}", pathname);
		CloseableHttpResponse closeableHttpResponse = client.delete(mosConfig.getHost() + "/upload/" + mosConfig.getBucketName() + "/deleteFile?" + getSignQueryParams(pathname, 30, false));
		HttpEntity entity = closeableHttpResponse.getEntity();
		String result = EntityUtils.toString(entity, "UTF-8");
		log.info("删除结果：{}", result);
		Assert.notNull(result, "请求资源服务器失败");
		JSONObject jsonObject = JSONObject.parseObject(result);
		return jsonObject.getBoolean("result");
	}
	
	@Override
	public PageInfo<DirAndResource> list(@NotNull String path, @Nullable String keyWord, @Nullable Integer pageNum, @Nullable Integer pageSize) throws IOException {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		log.info("查询文件列表:{}", path);
		String url = mosConfig.getHost() +
				"/list/" +
				mosConfig.getBucketName() +
				path +
				"?sign=" +
				getSign(path, 30, TimeUnit.SECONDS);
		if (StringUtils.isNotBlank(keyWord)) {
			url += "&keyWord=" + keyWord;
		}
		if (pageNum != null) {
			url += "&pageNum=" + pageNum;
		}
		if (pageSize != null) {
			url += "&pageSize=" + pageSize;
		}
		CloseableHttpResponse closeableHttpResponse;
		closeableHttpResponse = client.get(url);
		JSONObject pageInfo = client.checkSuccessAndGetResult(closeableHttpResponse, JSONObject.class);
		return pageInfo.toJavaObject(new TypeReference<PageInfo<DirAndResource>>() {
		});
	}
	
	@Override
	public void uploadFile(File file, UploadInfo uploadInfo, @Nullable UploadProcessListener uploadProcessListener) throws IOException {
		uploadOperation.uploadFile(file, uploadInfo, uploadProcessListener);
	}
	
	@Override
	public void uploadFile(File file, UploadInfo uploadInfo) throws IOException {
		uploadFile(file, uploadInfo, null);
	}
	
	@Override
	public void uploadStream(InputStream inputStream, UploadInfo uploadInfo) throws IOException {
		uploadOperation.uploadStream(inputStream, uploadInfo);
	}
}
