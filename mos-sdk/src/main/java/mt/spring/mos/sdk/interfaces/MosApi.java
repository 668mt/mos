package mt.spring.mos.sdk.interfaces;

import mt.spring.mos.sdk.entity.DirAndResource;
import mt.spring.mos.sdk.entity.PageInfo;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import mt.spring.mos.sdk.upload.UploadProcessListener;
import mt.spring.mos.sdk.type.EncryptContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
public interface MosApi {
    void shutdown();

    /**
     * 获取访问地址
     *
     * @param pathname        资源
     * @param expiredTime     过期时间，为空则不设过期时间
     * @param expiredTimeUnit 过期单位
     * @param host            主机地址
     * @param render          是否使用渲染器
     * @param gallary         是否获取图集
     * @return 访问地址
     */
    String getUrl(@NotNull String pathname, long expiredTime, @Nullable TimeUnit expiredTimeUnit, String host, boolean render, boolean gallary);

    String getUrl(@NotNull String pathname, @NotNull String sign, String host, boolean render, boolean gallary);

    String getUrl(@NotNull String pathname, long expiredTime, @Nullable TimeUnit expiredTimeUnit);

    String getSign(@NotNull String pathname, long expiredTime, @Nullable TimeUnit expiredTimeUnit);

    String getSign(@NotNull EncryptContent content, long expiredTime, @Nullable TimeUnit expiredTimeUnit);

    /**
     * 是否存在
     *
     * @param pathname 文件路径名
     * @return 是否存在
     */
    boolean isExists(@NotNull String pathname) throws IOException;

    /**
     * 删除文件
     *
     * @param pathname 文件路径名
     * @return 是否删除成功
     * @throws IOException IO异常
     */
    boolean deleteFile(@NotNull String pathname) throws IOException;

    boolean deleteDir(@NotNull String path) throws IOException;

    /**
     * 查询路径和文件列表信息
     *
     * @param path     路径
     * @param keyWord  关键字
     * @param pageNum  页码
     * @param pageSize 分页大小
     * @return 路径和文件信息
     */
    PageInfo<DirAndResource> list(@NotNull String path, @Nullable String keyWord, @Nullable Integer pageNum, @Nullable Integer pageSize) throws IOException;

    /**
     * 上传文件
     *
     * @param file                  上传的文件
     * @param uploadInfo            文件信息
     * @param uploadProcessListener 进度更新监听器
     * @throws IOException IO异常
     */
    void uploadFile(File file, UploadInfo uploadInfo, @Nullable UploadProcessListener uploadProcessListener) throws IOException;

    /**
     * 上传文件
     *
     * @param file       上传的文件
     * @param uploadInfo 文件信息
     * @throws IOException IO异常
     */
    void uploadFile(File file, UploadInfo uploadInfo) throws IOException;

    /**
     * 流式上传
     *
     * @param inputStream 上传的流
     * @param uploadInfo  文件信息
     * @throws IOException IO异常
     */
    void uploadStream(InputStream inputStream, UploadInfo uploadInfo) throws IOException;

    /**
     * 下载文件
     *
     * @param pathname 文件路径
     * @param desFile  目标文件
     * @throws IOException IO异常
     */
    void downloadFile(String pathname, File desFile) throws IOException;

    /**
     * 下载文件
     *
     * @param pathname 文件路径
     * @param desFile  目标文件
     * @param cover    是否覆盖
     * @throws IOException IO异常
     */
    void downloadFile(String pathname, File desFile, boolean cover) throws IOException;
}
