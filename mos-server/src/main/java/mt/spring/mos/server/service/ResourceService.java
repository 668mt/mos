package mt.spring.mos.server.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import mt.common.entity.ResResult;
import mt.common.mybatis.event.AfterInitEvent;
import mt.common.mybatis.mapper.BaseMapper;
import mt.common.mybatis.utils.MyBatisUtils;
import mt.common.service.BaseServiceImpl;
import mt.common.service.DataLockService;
import mt.common.tkmapper.Filter;
import mt.spring.mos.sdk.HttpClientServletUtils;
import mt.spring.mos.server.dao.RelaClientResourceMapper;
import mt.spring.mos.server.dao.ResourceMapper;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.dto.AccessControlAddDto;
import mt.spring.mos.server.entity.po.*;
import mt.spring.mos.server.entity.vo.DirAndResourceVo;
import mt.utils.JsonUtils;
import mt.utils.MyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2020/5/18
 */
@Service
@Slf4j
public class ResourceService extends BaseServiceImpl<Resource> {
	@Autowired
	private ResourceMapper resourceMapper;
	@Autowired
	@Lazy
	private ClientService clientService;
	@Autowired
	private RelaClientResourceMapper relaClientResourceMapper;
	@Autowired
	private MosServerProperties mosServerProperties;
	@Autowired
	private DataLockService lockService;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private DirService dirService;
	@Autowired
	@Lazy
	private BucketService bucketService;
	@Autowired
	private UserService userService;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private AccessControlService accessControlService;
	@Autowired
	@Qualifier("httpRestTemplate")
	private RestTemplate httpRestTemplate;
	@Autowired
	@Qualifier("backRestTemplate")
	private RestTemplate backRestTemplate;
	@Autowired
	private CloseableHttpClient httpClient;
	
	@Override
	public BaseMapper<Resource> getBaseMapper() {
		return resourceMapper;
	}
	
	private void addResource(Resource resource, String clientId, Long bucketId) {
		String pathname = resource.getPathname();
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
			resource.setPathname(pathname);
		}
		Dir dir = addDir(pathname, bucketId);
		Assert.notNull(dir, "文件夹不能为空");
		resource.setDirId(dir.getId());
		save(resource);
		RelaClientResource relaClientResource = new RelaClientResource();
		relaClientResource.setClientId(clientId);
		relaClientResource.setResourceId(resource.getId());
		relaClientResourceMapper.insert(relaClientResource);
	}
	
	private Dir addDir(String pathname, Long bucketId) {
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		int i;
		Dir dir = null;
		while ((i = pathname.indexOf("/")) != -1) {
			String path = "/" + pathname.substring(0, i);
			pathname = pathname.substring(i + 1);
			if (dir == null) {
				dir = new Dir();
				dir.setPath(path);
				dir.setBucketId(bucketId);
				List<Filter> filters = new ArrayList<>();
				filters.add(new Filter("path", Filter.Operator.eq, dir.getPath()));
				filters.add(new Filter("bucketId", Filter.Operator.eq, bucketId));
				Dir findPath = dirService.findOneByFilters(filters);
				if (findPath == null) {
					dirService.save(dir);
				} else {
					dir = findPath;
				}
			} else {
				Dir child = new Dir();
				if (dir.getPath().equals("/")) {
					child.setPath(path);
				} else {
					child.setPath(dir.getPath() + path);
				}
				child.setParentId(dir.getId());
				dir.setChild(child);
				dir = child;
				dir.setBucketId(bucketId);
				List<Filter> filters = new ArrayList<>();
				filters.add(new Filter("path", Filter.Operator.eq, dir.getPath()));
				filters.add(new Filter("bucketId", Filter.Operator.eq, bucketId));
				Dir findPath = dirService.findOneByFilters(filters);
				if (findPath == null) {
					dirService.save(dir);
				} else {
					dir.setId(findPath.getId());
				}
			}
		}
		return dir;
	}
	
	@Transactional
	public void addResourceIfNotExist(Resource resource, String clientId, Long bucketId) {
		jdbcTemplate.queryForMap("select * from mos_bucket where id = ? for update", bucketId);
		String pathname = resource.getPathname();
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		Resource findResource = resourceMapper.findResourceByPathnameAndBucketId(pathname, bucketId);
		if (findResource == null) {
			log.info("{}上新增文件{}", clientId, pathname);
			addResource(resource, clientId, bucketId);
		} else {
			List<Filter> filters = new ArrayList<>();
			filters.add(new Filter("resourceId", Filter.Operator.eq, findResource.getId()));
			filters.add(new Filter("clientId", Filter.Operator.eq, clientId));
			RelaClientResource rela = relaClientResourceMapper.selectOneByExample(MyBatisUtils.createExample(RelaClientResource.class, filters));
			if (rela == null) {
				log.info("{}上新增文件{}", clientId, pathname);
				rela = new RelaClientResource();
				rela.setClientId(clientId);
				rela.setResourceId(findResource.getId());
				relaClientResourceMapper.insert(rela);
			}
		}
	}
	
	/**
	 * 查询需要备份的数据，当前数据小于数据分片数
	 *
	 * @return
	 */
	public List<Long> findNeedBackResources() {
		Integer backTime = mosServerProperties.getDataFragmentsAmount();
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
		//查询存活的服务
		int count = clientService.count(filters);
		//备份数不能大于存活数
		return relaClientResourceMapper.findNeedBackResourceIds(count > backTime ? backTime : count);
	}
	
	public static String getUrlEncodedPathname(String pathname) {
		if (StringUtils.isBlank(pathname)) {
			return pathname;
		}
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		String[] split = pathname.split("/");
		List<String> pathnames = new ArrayList<>();
		for (String s : split) {
			try {
				pathnames.add(URLEncoder.encode(s, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		return StringUtils.join(pathnames, "/");
	}
	
	/**
	 * 备份资源
	 *
	 * @param resourceId
	 */
	@Transactional(rollbackFor = {Exception.class, RuntimeException.class})
	public void backResource(Long resourceId) {
		jdbcTemplate.queryForMap("select * from mos_resource where id = ?", resourceId);
		Integer dataFragmentsAmount = mosServerProperties.getDataFragmentsAmount();
		List<Client> clients = clientService.findAvaliableClients();
		Assert.notEmpty(clients, "无可用资源服务器");
		List<RelaClientResource> relas = relaClientResourceMapper.findList("resourceId", resourceId);
		Assert.notEmpty(relas, "资源不存在");
		if (relas.size() >= dataFragmentsAmount) {
			//已经达到数据分片数量了，不需要再进行备份
			log.info("resource {} 已达到备份数量，不需要再进行备份", resourceId);
			return;
		}
		//数据分片数不能大于当前可用资源服务器数量
		dataFragmentsAmount = clients.size() > dataFragmentsAmount ? dataFragmentsAmount : clients.size();
		Client srcClient = null;
		findSrcClient:
		for (RelaClientResource rela : relas) {
			for (Client client : clients) {
				if (rela.getClientId().equalsIgnoreCase(client.getClientId()) && clientService.isAlive(client)) {
					srcClient = client;
					break findSrcClient;
				}
			}
		}
		Assert.notNull(srcClient, "资源" + resourceId + "无可用资源服务器");
		//备份可用服务器，避免备份到同一主机上
		List<Client> backAvaliable = clients.stream().filter(client -> {
			boolean exists = false;
			for (RelaClientResource rela : relas) {
				if (rela.getClientId().equalsIgnoreCase(client.getClientId())) {
					exists = true;
					break;
				}
			}
			return !exists;
		}).collect(Collectors.toList());
		Assert.notEmpty(backAvaliable, "资源" + resourceId + "不可备份，资源服务器不够");
		Resource resource = findById(resourceId);
		backAvaliable.sort(Comparator.comparing(Client::getUsedPercent));
		int backTime = dataFragmentsAmount - relas.size();
		log.info("数据分片数：{},需要备份次数:{}", dataFragmentsAmount, backTime);
		for (Client desClient : backAvaliable) {
			if (backTime <= 0) {
				return;
			}
			String pathname = resource.getPathname();
			Dir dir = dirService.findById(resource.getDirId());
			Bucket bucket = bucketService.findById(dir.getBucketId());
			String desPathname = getDesPathname(bucket, pathname);
			String srcUrl = srcClient.getUrl() + "/mos" + desPathname;
			log.info("开始备份{}，从{}备份到{}", pathname, srcClient.getUrl(), desClient.getUrl());
			try {
				backRestTemplate.execute(srcUrl, HttpMethod.GET, null, clientHttpResponse -> {
					InputStream inputStream = clientHttpResponse.getBody();
					upload(desClient, inputStream, resource.getSizeByte(), pathname, bucket);
					
					RelaClientResource relaClientResource = new RelaClientResource();
					relaClientResource.setResourceId(resourceId);
					relaClientResource.setClientId(desClient.getClientId());
					relaClientResourceMapper.insert(relaClientResource);
					log.info("备份{}完成!", pathname);
					return null;
				});
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			backTime--;
		}
	}
	
	@EventListener
	public void init(AfterInitEvent afterInitEvent) {
		lockService.initLock("resourceLock", jdbcTemplate);
		
		if (StringUtils.isBlank(mosServerProperties.getAdminUsername())) {
			return;
		}
		
		User user = userService.findOne("username", mosServerProperties.getAdminUsername());
		if (user == null) {
			user = new User();
			user.setUsername(mosServerProperties.getAdminUsername());
			user.setPassword(passwordEncoder.encode(mosServerProperties.getAdminPassword()));
			user.setIsEnable(true);
			user.setIsAdmin(true);
			userService.save(user);
			
		}
		Bucket bucket = bucketService.findOne("bucketName", mosServerProperties.getDefaultBucketName());
		if (bucket == null) {
			bucket = new Bucket();
			bucket.setBucketName(mosServerProperties.getDefaultBucketName());
			bucket.setUserId(user.getId());
			bucketService.save(bucket);
			try {
				AccessControlAddDto accessControlAddDto = new AccessControlAddDto();
				accessControlAddDto.setBucketId(bucket.getId());
				accessControlAddDto.setUseInfo("默认");
				accessControlService.addAccessControl(accessControlAddDto);
			} catch (NoSuchAlgorithmException e) {
				log.error(e.getMessage(), e);
			}
		}
		
	}
	
	public Resource findResourceByPathnameAndBucketId(@NotNull String pathname, @NotNull Long bucketId) {
		return resourceMapper.findResourceByPathnameAndBucketId(pathname, bucketId);
	}
	
	public String getDesPathname(Bucket bucket, String pathname) {
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		return "/" + bucket.getUserId() + "/" + bucket.getId() + pathname;
	}
	
	@Transactional
	public void deleteResources(@NotNull Bucket bucket, @Nullable Long[] dirIds, @Nullable Long[] fileIds) {
		if (dirIds != null) {
			for (Long dirId : dirIds) {
				deleteDir(bucket, dirId);
			}
		}
		if (fileIds != null) {
			for (Long fileId : fileIds) {
				deleteResource(bucket, fileId);
			}
		}
	}
	
	@Transactional
	public void deleteResource(@NotNull Bucket bucket, String pathname) {
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		Resource resource = resourceMapper.findResourceByPathnameAndBucketId(pathname, bucket.getId());
		Assert.notNull(resource, "资源不存在");
		deleteResource(bucket, resource.getId());
	}
	
	@Transactional
	public void deleteResource(@NotNull Bucket bucket, long resourceId) {
		Resource resource = findById(resourceId);
		Assert.notNull(resource, "资源不存在");
		List<RelaClientResource> relas = relaClientResourceMapper.findList("resourceId", resourceId);
		if (MyUtils.isNotEmpty(relas)) {
			for (RelaClientResource rela : relas) {
				try {
					String clientId = rela.getClientId();
					Client client = clientService.findById(clientId);
					String desPathname = getDesPathname(bucket, resource.getPathname());
					httpRestTemplate.delete(client.getUrl() + "/client/deleteFile?pathname={0}", desPathname);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}
		deleteById(resourceId);
	}
	
	/**
	 * 上传文件
	 *
	 * @param client      客户端
	 * @param inputStream 文件流
	 * @param pathname    路径名
	 * @param bucket      桶
	 * @throws IOException
	 */
	public void upload(Client client, InputStream inputStream, long fileSize, String pathname, Bucket bucket) throws IOException {
		try {
			log.info("开始上传{}...", pathname);
			String uri = client.getUrl() + "/client/upload";
			String desPathname = getDesPathname(bucket, pathname);
			ResResult sizeResult = httpRestTemplate.getForObject(client.getUrl() + "/client/size?pathname={0}", ResResult.class, desPathname);
			Assert.notNull(sizeResult, "查询size失败");
			Object size = sizeResult.getResult();
			if (Long.parseLong(size.toString()) == fileSize) {
				//文件已存在，且大小一致
				log.info("文件{}已存在，跳过上传步骤", desPathname);
				return;
			}
			CloseableHttpResponse response = HttpClientServletUtils.httpClientUploadFile(httpClient, uri, inputStream, desPathname);
			HttpEntity entity = response.getEntity();
			Assert.notNull(entity, "客户端返回内容空");
			String result = EntityUtils.toString(entity);
			log.info("{}上传结果：{}", pathname, result);
			ResResult resResult = JsonUtils.toObject(result, ResResult.class);
			Assert.state(resResult.isSuccess(), "上传失败,clientMsg:" + resResult.getMessage());
		} finally {
			if (inputStream != null) {
				IOUtils.closeQuietly(inputStream);
			}
		}
	}
	
	public void deleteDir(Bucket bucket, String path) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("path", Filter.Operator.eq, path));
		filters.add(new Filter("bucketId", Filter.Operator.eq, bucket.getId()));
		Dir dir = dirService.findOneByFilters(filters);
		Assert.notNull(dir, "资源不存在");
		deleteDir(bucket, dir.getId());
	}
	
	public void deleteDir(Bucket bucket, long dirId) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("id", Filter.Operator.eq, dirId));
		filters.add(new Filter("bucketId", Filter.Operator.eq, bucket.getId()));
		Dir dir = dirService.findOneByFilters(filters);
		List<Client> clients = clientService.findAvaliableClients();
		if (MyUtils.isNotEmpty(clients)) {
			for (Client client : clients) {
				String desPah = getDesPathname(bucket, dir.getPath());
				httpRestTemplate.delete(client.getUrl() + "/client/deleteDir?path={0}", desPah);
				log.info("删除{}成功", dir.getPath());
			}
		}
		dirService.deleteById(dir);
	}
	
	public PageInfo<DirAndResourceVo> findDirAndResourceVoListPage(String keyWord, Integer pageNum, Integer pageSize, Long bucketId, Long dirId) {
		if (pageNum != null && pageSize != null) {
			PageHelper.startPage(pageNum, pageSize);
		}
		List<DirAndResourceVo> list = resourceMapper.findChildDirAndResourceList(keyWord, bucketId, dirId);
		return new PageInfo<>(list);
	}
	
	@Transactional
	@Async
	public void deleteAllResources(Long bucketId) {
		List<Dir> dirs = dirService.findList("bucketId", bucketId);
		if (MyUtils.isNotEmpty(dirs)) {
			dirs.sort(Comparator.comparing(Dir::getId));
			Bucket bucket = bucketService.findById(bucketId);
			Assert.notNull(bucket, "bucket不存在");
			for (Dir dir : dirs) {
				deleteDir(bucket, dir.getId());
			}
		}
	}
}
