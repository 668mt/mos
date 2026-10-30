package mt.spring.mos.sdk.interfaces;

import mt.spring.mos.sdk.entity.DirAndResource;
import mt.spring.mos.sdk.entity.PageInfo;
import mt.spring.mos.sdk.entity.params.UrlBuildParams;
import mt.spring.mos.sdk.entity.upload.UploadInfo;
import mt.spring.mos.sdk.upload.UploadProcessListener;
import mt.spring.mos.sdk.utils.Host;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @Author Martin
 * @Date 2020/11/25
 */
public interface MosApi {
	void shutdown();
	
	/**
	 * 获取当前可用的服务器信息（含内网地址、外网地址、权重）
	 *
	 * @return 没有可用服务器时返回 null
	 */
	Host getHostInfo();
	
	/**
	 * 生成资源访问链接
	 *
	 * @param urlBuildParams 链接参数
	 * @return 资源访问链接
	 */
	String generateUrl(@NotNull UrlBuildParams urlBuildParams);
	
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
	
	/**
	 * 删除文件夹
	 *
	 * @param path 文件夹路径
	 * @return 是否删除成功
	 * @throws IOException
	 */
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
	
	/**
	 * 下载文件
	 *
	 * @param pathname            文件路径
	 * @param desFile             目标文件
	 * @param cover               是否覆盖
	 * @param limitSpeedKbSeconds 限速，单位kb/s
	 * @throws IOException IO异常
	 */
	void downloadFile(String pathname, File desFile, boolean cover, long limitSpeedKbSeconds) throws IOException;
}
