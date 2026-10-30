package mt.spring.mos.sdk;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.sdk.entity.DirAndResource;
import mt.spring.mos.sdk.entity.MosConfig;
import mt.spring.mos.sdk.entity.PageInfo;
import mt.spring.mos.sdk.entity.Resource;
import mt.spring.mos.sdk.entity.params.UrlBuildParams;
import mt.spring.mos.sdk.entity.upload.MosUploadConfig;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import mt.spring.mos.sdk.http.ServiceClient;
import mt.spring.mos.sdk.interfaces.MosApi;
import mt.spring.mos.sdk.type.EncryptContent;
import mt.spring.mos.sdk.type.PathnamesEncryptContent;
import mt.spring.mos.sdk.upload.MultipartOperation;
import mt.spring.mos.sdk.upload.UploadProcessListener;
import mt.spring.mos.sdk.utils.Host;
import okhttp3.Response;
import okhttp3.ResponseBody;
import mt.spring.mos.sdk.utils.MosEncrypt;
import mt.spring.mos.sdk.utils.PathnameDefine;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@Data
@Slf4j
public class MosSdk implements MosApi, Closeable {
	private ServiceClient client;
	private MosConfig mosConfig;
	private MosUploadConfig mosUploadConfig;
	private MultipartOperation multipartOperation;
	
	private MosSdk() {
	
	}
	
	@Override
	public void shutdown() {
		multipartOperation.shutdown();
		client.shutdown();
	}
	
	public MosSdk(MosConfig mosConfig) {
		this(mosConfig, new MosUploadConfig());
	}
	
	public MosSdk(MosConfig mosConfig, MosUploadConfig mosUploadConfig) {
		this.mosConfig = mosConfig;
		this.mosUploadConfig = mosUploadConfig;
		client = new ServiceClient(this.mosConfig);
		this.multipartOperation = new MultipartOperation(this, mosConfig, mosUploadConfig, client);
	}
	
	public void setMosUploadConfig(MosUploadConfig mosUploadConfig) {
		this.multipartOperation.setMosUploadConfig(mosUploadConfig);
	}
	
	@Override
	public Host getHostInfo() {
		return mosConfig.getHostInfo();
	}
	
	/**
	 * 生成签名。expireSeconds 为 -1 表示永不过期（expiredTimeUnit 传 null）
	 */
	public String getSign(@NotNull String pathname, long expired, @Nullable TimeUnit expiredTimeUnit) {
		return getSign(new PathnamesEncryptContent(new PathnameDefine(pathname).getPathname()), expired, expiredTimeUnit);
	}
	
	/**
	 * 生成签名。expireSeconds 为 -1 表示永不过期（expiredTimeUnit 传 null）
	 */
	public String getSign(@NotNull EncryptContent content, long expired, @Nullable TimeUnit expiredTimeUnit) {
		try {
			long expireSeconds;
			if (expiredTimeUnit == null) {
				expireSeconds = -1L;
			} else {
				expireSeconds = expiredTimeUnit.toSeconds(expired);
			}
			String encrypt = MosEncrypt.encrypt(mosConfig.getSecretKey(), content, mosConfig.getBucketName(), mosConfig.getOpenId(), expireSeconds);
			log.debug("{} 签名结果：{}", content, encrypt);
			return encrypt;
		} catch (Exception e) {
			throw new RuntimeException("加签失败：" + e.getMessage(), e);
		}
	}
	
@Override
	public String generateUrl(@NotNull UrlBuildParams urlBuildParams) {
		String pathname = urlBuildParams.getPathname();
		//地址有两个来源：
		//   1. 显式指定 host（如当前请求的域名）—— 直接用它
		//   2. 没指定时按 forInner / forExtern 从选中的服务器信息里取（配置里 [] 中的是外网地址）
		String explicitHost = urlBuildParams.getHost();
		String urlHost = explicitHost;
		String innerHost = explicitHost;
		if (StringUtils.isBlank(explicitHost)) {
			Host hostInfo = this.getMosConfig().getHostInfo();
			Assert.state(hostInfo != null, "没有可用的服务器");
			//forInner=true 且没显式要求对外时用内网地址，否则用外网地址
			boolean forInner = Boolean.TRUE.equals(urlBuildParams.getForInner()) && !Boolean.TRUE.equals(urlBuildParams.getForExtern());
			urlHost = forInner ? hostInfo.getHost() : hostInfo.getExternHostOrDefault();
			innerHost = hostInfo.getHost();
		}
		String sign = urlBuildParams.getSign();
		if (StringUtils.isBlank(sign)) {
			sign = getSign(pathname, urlBuildParams.getExpiredTime(), urlBuildParams.getExpiredTimeUnit());
		}
		if (Boolean.TRUE.equals(urlBuildParams.getUseShortUrl())) {
			return buildShortUrl(innerHost, urlHost, pathname, sign, urlBuildParams);
		}
		PathnameDefine pathnameDefine = new PathnameDefine(pathname);
		pathnameDefine.setRender(urlBuildParams.getRender() != null && urlBuildParams.getRender());
		pathnameDefine.setGallary(urlBuildParams.getGallery() != null && urlBuildParams.getGallery());
		return pathnameDefine.getUrl(urlHost, mosConfig.getBucketName(), sign);
	}

	/**
	 * 生成短链接
	 *
	 * @param innerHost 调 mos-server（POST /s）用的内网地址
	 * @param urlHost   拼给最终用户的对外地址
	 */
	private String buildShortUrl(@NotNull String innerHost, @NotNull String urlHost, @NotNull String pathname, @NotNull String sign, @NotNull UrlBuildParams urlBuildParams) {
		long expireSeconds;
		TimeUnit unit = urlBuildParams.getExpiredTimeUnit();
		Long expiredTime = urlBuildParams.getExpiredTime();
		if (expiredTime == null || unit == null) {
			// 调用方没传过期时间时兜底为 1 小时，避免服务端校验失败导致 500
			expireSeconds = TimeUnit.HOURS.toSeconds(1);
		} else {
			expireSeconds = unit.toSeconds(expiredTime);
		}
		String queryString = buildQueryString(urlBuildParams);
		// 内网请求 mos-server 走 innerHost；拼给外网用户的短链走 urlHost
		StringBuilder url = new StringBuilder(innerHost).append("/s?");
		url.append("bucketName=").append(URLEncoder.encode(mosConfig.getBucketName(), StandardCharsets.UTF_8));
		url.append("&pathname=").append(URLEncoder.encode(pathname, StandardCharsets.UTF_8));
		url.append("&sign=").append(URLEncoder.encode(sign, StandardCharsets.UTF_8));
		JSONObject body = new JSONObject();
		body.put("expireSeconds", expireSeconds);
		if (StringUtils.isNotBlank(queryString)) {
			body.put("queryString", queryString);
		}
		String shortCode;
		try {
			shortCode = client.postJson(url.toString(), body, JSONObject.class).getString("shortCode");
		} catch (IOException e) {
			throw new RuntimeException("生成短链接失败：" + e.getMessage(), e);
		}
		return urlHost + "/s/" + shortCode;
	}
	
	private String buildQueryString(@NotNull UrlBuildParams urlBuildParams) {
		StringBuilder sb = new StringBuilder();
		boolean render = urlBuildParams.getRender() != null && urlBuildParams.getRender();
		boolean gallary = urlBuildParams.getGallery() != null && urlBuildParams.getGallery();
		if (render) {
			sb.append("render=true");
		}
		if (gallary) {
			if (!sb.isEmpty()) {
				sb.append('&');
			}
			sb.append("gallary=true");
		}
		return sb.toString();
	}
	
	private String getSignQueryParams(String pathname) {
		String sign = getSign(pathname, 10L, TimeUnit.MINUTES);
		try {
			pathname = URLEncoder.encode(pathname, StandardCharsets.UTF_8);
			sign = URLEncoder.encode(sign, StandardCharsets.UTF_8);
			return "sign=" + sign + "&pathname=" + pathname;
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
		return client.get(mosConfig.getHost() + "/open/resource/" + mosConfig.getBucketName() + "/isExists?" + getSignQueryParams(pathname), Boolean.class);
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
		try (Response response = client.delete(mosConfig.getHost() + "/open/resource/" + mosConfig.getBucketName() + "/deleteFile?" + getSignQueryParams(pathname))) {
			ResponseBody body = response.body();
			String result = body != null ? body.string() : null;
			log.debug("删除结果：{}", result);
			Assert.notNull(result, "请求资源服务器失败");
			JSONObject jsonObject = JSONObject.parseObject(result);
			Boolean deleteFile = jsonObject.getBoolean("result");
			return deleteFile != null && deleteFile;
		}
	}
	
	@Override
	public boolean deleteDir(@NotNull String path) throws IOException {
		log.info("删除文件夹：{}", path);
		try (Response response = client.delete(mosConfig.getHost() + "/open/dir/" + mosConfig.getBucketName() + "/deleteDir?" + getSignQueryParams(path))) {
			ResponseBody body = response.body();
			String result = body != null ? body.string() : null;
			log.debug("删除结果：{}", result);
			Assert.notNull(result, "请求资源服务器失败");
			JSONObject jsonObject = JSONObject.parseObject(result);
			Boolean deleteDir = jsonObject.getBoolean("result");
			return deleteDir != null && deleteDir;
		}
	}
	
	public Resource getFileInfo(@NotNull String pathname) throws IOException {
		if (!isExists(pathname)) {
			return null;
		}
		log.debug("查询文件信息：{}", pathname);
		try (Response response = client.get(mosConfig.getHost() + "/open/resource/" + mosConfig.getBucketName() + "/info?" + getSignQueryParams(pathname))) {
			ResponseBody body = response.body();
			String result = body != null ? body.string() : null;
			log.debug("查询结果：{}", result);
			Assert.notNull(result, "请求资源服务器失败");
			JSONObject jsonObject = JSONObject.parseObject(result);
			return jsonObject.getObject("result", Resource.class);
		}
	}
	
	public boolean isFileModified(@NotNull String pathname, @NotNull File file) throws IOException {
		if (!file.exists()) {
			return true;
		}
		Resource fileInfo = getFileInfo(pathname);
		if (fileInfo == null) {
			return true;
		}
		try (InputStream inputStream = new FileInputStream(file)) {
			String md5 = DigestUtils.md5Hex(inputStream);
			return !md5.equals(fileInfo.getMd5());
		}
	}
	
	@Override
	public PageInfo<DirAndResource> list(@NotNull String path, @Nullable String keyWord, @Nullable Integer pageNum, @Nullable Integer pageSize) throws IOException {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		log.debug("查询文件列表:{}", path);
		String url = mosConfig.getHost() +
			"/open/resource/" +
			mosConfig.getBucketName() +
			"/list" +
			"?sign=" +
			getSign(path, 30L, TimeUnit.SECONDS);
		url += "&path=" + URLEncoder.encode(path, StandardCharsets.UTF_8);
		if (StringUtils.isNotBlank(keyWord)) {
			url += "&keyWord=" + keyWord;
		}
		if (pageNum != null) {
			url += "&pageNum=" + pageNum;
		}
		if (pageSize != null) {
			url += "&pageSize=" + pageSize;
		}
		JSONObject pageInfo;
		try (Response response = client.get(url)) {
			pageInfo = client.checkSuccessAndGetResult(response, JSONObject.class);
		}
		return pageInfo.toJavaObject(new TypeReference<PageInfo<DirAndResource>>() {
		});
	}
	
	@Override
	public void uploadFile(File file, UploadInfo uploadInfo, @Nullable UploadProcessListener uploadProcessListener) throws IOException {
		multipartOperation.uploadFile(file, uploadInfo, uploadProcessListener);
	}
	
	@Override
	public void uploadFile(File file, UploadInfo uploadInfo) throws IOException {
		uploadFile(file, uploadInfo, null);
	}
	
	@Override
	public void uploadStream(InputStream inputStream, UploadInfo uploadInfo) throws IOException {
		multipartOperation.uploadStream(inputStream, uploadInfo);
	}
	
	@Override
	public void downloadFile(String pathname, File desFile) throws IOException {
		multipartOperation.downloadFile(pathname, desFile);
	}
	
	@Override
	public void downloadFile(String pathname, File desFile, boolean cover) throws IOException {
		multipartOperation.downloadFile(pathname, desFile, cover);
	}
	
	@Override
	public void downloadFile(String pathname, File desFile, boolean cover, long limitSpeedKbSeconds) throws IOException {
		multipartOperation.downloadFile(pathname, desFile, cover, limitSpeedKbSeconds);
	}
	
	@Override
	public void close() throws IOException {
		shutdown();
	}
}
