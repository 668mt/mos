package mt.spring.mos.sdk;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.base.utils.RegexUtils;
import mt.spring.mos.sdk.entity.DirAndResource;
import mt.spring.mos.sdk.entity.MosConfig;
import mt.spring.mos.sdk.entity.PageInfo;
import mt.spring.mos.sdk.entity.Resource;
import mt.spring.mos.sdk.entity.upload.MosUploadConfig;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import mt.spring.mos.sdk.http.ServiceClient;
import mt.spring.mos.sdk.interfaces.MosApi;
import mt.spring.mos.sdk.upload.MultipartOperation;
import mt.spring.mos.sdk.upload.UploadProcessListener;
import mt.spring.mos.sdk.utils.EncryptContent;
import mt.spring.mos.sdk.utils.MosEncrypt;
import mt.spring.mos.sdk.utils.PathnamesEncryptContent;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
    private MosUploadConfig mosUploadConfig;
    private MultipartOperation multipartOperation;

    private MosSdk() {
    }

    @Override
    public void shutdown() {
        multipartOperation.shutdown();
        client.shutdown();
    }

    public MosSdk(String host, long openId, String bucketName, String secretKey) {
        this(host, openId, bucketName, secretKey, new MosUploadConfig());
    }

    public MosSdk(String host, long openId, String bucketName, String secretKey, MosUploadConfig mosUploadConfig) {
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        this.mosConfig = new MosConfig(host, bucketName, secretKey, openId);
        this.mosUploadConfig = mosUploadConfig;
        client = new ServiceClient();
        this.multipartOperation = new MultipartOperation(this, mosConfig, mosUploadConfig, client);
    }

    public void setMosUploadConfig(MosUploadConfig mosUploadConfig) {
        this.multipartOperation.setMosUploadConfig(mosUploadConfig);
    }

    @Override
    public String getSign(@NotNull String pathname, long expired, @Nullable TimeUnit expiredTimeUnit) {
        return getSign(new PathnamesEncryptContent(pathname), expired, expiredTimeUnit);
    }

    @Override
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
    public String getUrl(@NotNull String pathname, long expired, @Nullable TimeUnit expiredTimeUnit) {
        return getUrl(pathname, expired, expiredTimeUnit, this.mosConfig.getHost(), false, false);
    }

    @Override
    public String getUrl(@NotNull String pathname, long expired, @Nullable TimeUnit timeUnit, String host, boolean render, boolean gallary) {
        return getUrl(pathname, getSign(new PathnamesEncryptContent(pathname), expired, timeUnit), host, render, gallary);
    }

    @Override
    public String getUrl(@NotNull String pathname, @NotNull String sign, String host, boolean render, boolean gallary) {
        Set<String> params = new HashSet<>();
        if (pathname.startsWith("@")) {
            String[] group = RegexUtils.findFirst(pathname, "^@(.+?)@*:(.+)$", new Integer[]{1, 2});
            Assert.notNull(group, "pathname格式不正确");
            params.addAll(Arrays.asList(group[0].split("-")));
            pathname = group[1];
        }
        if (!pathname.startsWith("/")) {
            pathname = "/" + pathname;
        }
        pathname = Stream.of(pathname.split("/")).map(s -> {
            try {
                return URLEncoder.encode(s, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.joining("/"));
        if (StringUtils.isBlank(host)) {
            host = this.getMosConfig().getHost();
        }
        try {
            String url = host +
                    "/mos/" +
                    mosConfig.getBucketName() +
                    pathname +
                    "?sign=" +
                    sign;
            if (render) {
                params.add("render");
            } else {
                params.remove("render");
            }
            if (gallary) {
                params.add("gallary");
            } else {
                params.remove("gallary");
            }
            for (String param : params) {
                url += "&" + param + "=true";
            }
            return url;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getSignQueryParams(String pathname) {
        String sign = getSign(pathname, 10L, TimeUnit.MINUTES);
        try {
            pathname = URLEncoder.encode(pathname, "UTF-8");
            sign = URLEncoder.encode(sign, "UTF-8");
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
        CloseableHttpResponse closeableHttpResponse = client.delete(mosConfig.getHost() + "/open/resource/" + mosConfig.getBucketName() + "/deleteFile?" + getSignQueryParams(pathname));
        HttpEntity entity = closeableHttpResponse.getEntity();
        String result = EntityUtils.toString(entity, "UTF-8");
        log.debug("删除结果：{}", result);
        Assert.notNull(result, "请求资源服务器失败");
        JSONObject jsonObject = JSONObject.parseObject(result);
        Boolean deleteFile = jsonObject.getBoolean("result");
        return deleteFile == null ? false : deleteFile;
    }

    @Override
    public boolean deleteDir(@NotNull String path) throws IOException {
        log.info("删除文件夹：{}", path);
        CloseableHttpResponse closeableHttpResponse = client.delete(mosConfig.getHost() + "/open/dir/" + mosConfig.getBucketName() + "/deleteDir?" + getSignQueryParams(path));
        HttpEntity entity = closeableHttpResponse.getEntity();
        String result = EntityUtils.toString(entity, "UTF-8");
        log.debug("删除结果：{}", result);
        Assert.notNull(result, "请求资源服务器失败");
        JSONObject jsonObject = JSONObject.parseObject(result);
        Boolean deleteDir = jsonObject.getBoolean("result");
        return deleteDir == null ? false : deleteDir;
    }

    public Resource getFileInfo(@NotNull String pathname) throws IOException {
        if (!isExists(pathname)) {
            return null;
        }
        log.debug("查询文件信息：{}", pathname);
        CloseableHttpResponse closeableHttpResponse = client.get(mosConfig.getHost() + "/open/resource/" + mosConfig.getBucketName() + "/info?" + getSignQueryParams(pathname));
        HttpEntity entity = closeableHttpResponse.getEntity();
        String result = EntityUtils.toString(entity, "UTF-8");
        log.debug("查询结果：{}", result);
        Assert.notNull(result, "请求资源服务器失败");
        JSONObject jsonObject = JSONObject.parseObject(result);
        return jsonObject.getObject("result", Resource.class);
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
        url += "&path=" + URLEncoder.encode(path, "UTF-8");
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

}
