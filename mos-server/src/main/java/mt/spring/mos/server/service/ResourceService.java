package mt.spring.mos.server.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import mt.common.mybatis.event.AfterInitEvent;
import mt.common.mybatis.mapper.BaseMapper;
import mt.common.mybatis.utils.MyBatisUtils;
import mt.common.service.BaseServiceImpl;
import mt.common.service.DataLockService;
import mt.common.tkmapper.Filter;
import mt.common.utils.BeanUtils;
import mt.spring.mos.server.dao.RelaClientResourceMapper;
import mt.spring.mos.server.dao.ResourceMapper;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.dto.AccessControlAddDto;
import mt.spring.mos.server.entity.dto.ResourceUpdateDto;
import mt.spring.mos.server.entity.po.*;
import mt.spring.mos.server.entity.vo.BackVo;
import mt.spring.mos.server.entity.vo.DirAndResourceVo;
import mt.spring.mos.server.listener.ClientWorkLogEvent;
import mt.utils.MyUtils;
import mt.utils.RegexUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static mt.common.tkmapper.Filter.Operator.eq;

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
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	private FileHouseService fileHouseService;
	@Autowired
	private FileHouseItemService fileHouseItemService;
	
	
	@Override
	public BaseMapper<Resource> getBaseMapper() {
		return resourceMapper;
	}
	
	private void addResource(Resource resource, Long bucketId) {
		String pathname = resource.getPathname();
		Assert.state(StringUtils.isNotBlank(pathname), "资源名称不能为空");
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
			resource.setPathname(pathname);
		}
		Dir dir = addDir(pathname, bucketId);
		Assert.notNull(dir, "文件夹不能为空");
		Bucket bucket = bucketService.findById(bucketId);
		if (resource.getIsPublic() == null) {
			resource.setIsPublic(bucket.getDefaultIsPublic());
		}
		resource.setDirId(dir.getId());
		save(resource);
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
				if ("/".equals(dir.getPath())) {
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
	
	//	@Transactional
	private void addResourceIfNotExist(Resource resource, Long bucketId) {
//		jdbcTemplate.queryForList("select * from mos_bucket where id = ? for update", bucketId);
		String pathname = resource.getPathname();
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		Resource findResource = resourceMapper.findResourceByPathnameAndBucketId(pathname, bucketId);
		if (findResource == null) {
			log.info("新增文件{}", pathname);
			addResource(resource, bucketId);
		}
	}
	
	/**
	 * 查询需要备份的数据，当前数据小于数据分片数
	 *
	 * @return
	 */
	public List<BackVo> findNeedBackResources() {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("status", Filter.Operator.eq, Client.ClientStatus.UP));
		//查询存活的服务
		int count = clientService.count(filters);
		//备份数不能大于存活数
		return relaClientResourceMapper.findNeedBackResourceIds(count);
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

//	/**
//	 * 备份资源
//	 *
//	 * @param backVo
//	 */
//	@Transactional(rollbackFor = {Exception.class})
//	public void backResource(BackVo backVo) {
//		Long resourceId = backVo.getResourceId();
//		Integer dataFragmentsAmount = backVo.getDataFragmentsAmount();
//		jdbcTemplate.queryForMap("select * from mos_resource where id = ? for update", resourceId);
//		List<Client> clients = clientService.findAvaliableClients();
//		Assert.notEmpty(clients, "无可用资源服务器");
//		List<RelaClientResource> relas = relaClientResourceMapper.findList("resourceId", resourceId);
//		Assert.notEmpty(relas, "资源不存在");
//		if (relas.size() >= dataFragmentsAmount) {
//			//已经达到数据分片数量了，不需要再进行备份
//			log.info("resource {} 已达到备份数量，不需要再进行备份", resourceId);
//			return;
//		}
//		//数据分片数不能大于当前可用资源服务器数量
//		dataFragmentsAmount = clients.size() > dataFragmentsAmount ? dataFragmentsAmount : clients.size();
//		Client srcClient = null;
//		findSrcClient:
//		for (RelaClientResource rela : relas) {
//			for (Client client : clients) {
//				if (rela.getClientId().equalsIgnoreCase(client.getClientId()) && clientService.isAlive(client)) {
//					srcClient = client;
//					break findSrcClient;
//				}
//			}
//		}
//		Assert.notNull(srcClient, "资源" + resourceId + "无可用资源服务器");
//		//备份可用服务器，避免备份到同一主机上
//		List<Client> backAvaliable = clients.stream().filter(client -> {
//			boolean exists = false;
//			for (RelaClientResource rela : relas) {
//				if (rela.getClientId().equalsIgnoreCase(client.getClientId())) {
//					exists = true;
//					break;
//				}
//			}
//			return !exists;
//		}).collect(Collectors.toList());
//		Assert.notEmpty(backAvaliable, "资源" + resourceId + "不可备份，资源服务器不够");
//		Resource resource = findById(resourceId);
//		backAvaliable.sort(Comparator.comparing(Client::getUsedPercent));
//		int backTime = dataFragmentsAmount - relas.size();
//		log.info("数据分片数：{},需要备份次数:{}", dataFragmentsAmount, backTime);
//		for (Client desClient : backAvaliable) {
//			if (backTime <= 0) {
//				return;
//			}
//			try {
//				copyResource(srcClient, desClient, resource);
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			}
//			backTime--;
//		}
//	}

//	public void copyResource(Client srcClient, Client desClient, Resource resource) {
//		Dir dir = dirService.findById(resource.getDirId());
//		Bucket bucket = bucketService.findById(dir.getBucketId());
//		String pathname = resource.getPathname();
//		String desPathname = getDesPathname(bucket, pathname);
//		String srcUrl = srcClient.getUrl() + "/mos" + desPathname;
//		log.info("开始备份{}，从{}备份到{}", pathname, srcClient.getUrl(), desClient.getUrl());
//		backRestTemplate.execute(srcUrl, HttpMethod.GET, null, clientHttpResponse -> {
//			InputStream inputStream = clientHttpResponse.getBody();
//			upload(desClient, inputStream, pathname, bucket);
//			RelaClientResource relaClientResource = new RelaClientResource();
//			relaClientResource.setResourceId(resource.getId());
//			relaClientResource.setClientId(desClient.getClientId());
//			relaClientResourceMapper.insert(relaClientResource);
//			log.info("备份{}完成!", pathname);
//			return null;
//		});
//	}
	
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
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		
	}
	
	public Resource findResourceByPathnameAndBucketId(@NotNull String pathname, @NotNull Long bucketId) {
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		return resourceMapper.findResourceByPathnameAndBucketId(pathname, bucketId);
	}
	
	public String getDesPathname(Bucket bucket, Resource resource) {
		Long fileHouseId = resource.getFileHouseId();
		if (fileHouseId == null) {
			String pathname = resource.getPathname();
			if (!pathname.startsWith("/")) {
				pathname = "/" + pathname;
			}
			return "/" + bucket.getId() + pathname;
		} else {
			FileHouse fileHouse = fileHouseService.findById(fileHouseId);
			return fileHouse.getPathname();
		}
	}
	
	private String getDesPath(Bucket bucket, String path) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return "/" + bucket.getId() + path;
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
				String clientId = rela.getClientId();
				Client client = clientService.findById(clientId);
				if (resource.getFileHouseId() == null) {
					if (clientService.isAlive(client)) {
						Assert.state(StringUtils.isNotBlank(resource.getPathname()), "资源名称不能为空");
						client.apis(httpRestTemplate).deleteFile(getDesPathname(bucket, resource));
						applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_FILE, ClientWorkLog.ExeStatus.SUCCESS, clientId, getDesPathname(bucket, resource)));
					} else {
						applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_FILE, ClientWorkLog.ExeStatus.NOT_START, clientId, getDesPathname(bucket, resource)));
					}
				}
			}
		}
		deleteById(resourceId);
	}

//	/**
//	 * 上传文件
//	 *
//	 * @param client      客户端
//	 * @param inputStream 文件流
//	 * @param pathname    路径名
//	 * @param bucket      桶
//	 * @throws IOException
//	 */
//	public void upload(Client client, InputStream inputStream, String pathname, Bucket bucket) throws IOException {
//		try {
//			log.info("开始上传{}...", pathname);
//			String uri = client.getUrl() + "/client/upload";
//			String desPathname = getDesPathname(bucket, pathname);
//			CloseableHttpResponse response = HttpClientServletUtils.httpClientUploadFile(httpClient, uri, inputStream, desPathname);
//			HttpEntity entity = response.getEntity();
//			Assert.notNull(entity, "客户端返回内容空");
//			String result = EntityUtils.toString(entity);
//			log.info("{}上传结果：{}", pathname, result);
//			ResResult resResult = JsonUtils.toObject(result, ResResult.class);
//			Assert.state(resResult.isSuccess(), "上传失败,clientMsg:" + resResult.getMessage());
//		} finally {
//			if (inputStream != null) {
//				IOUtils.close(inputStream);
//			}
//		}
//	}
	
	@Transactional
	public void upload(InputStream inputStream, Long fileHouseId, String chunkMd5, Integer chunkIndex) throws IOException {
		Assert.notNull(fileHouseId, "fileHouseId不能为空");
		Assert.notNull(chunkIndex, "chunkIndex不能为空");
		Assert.notNull(chunkMd5, "chunkMd5不能为空");
		Assert.notNull(inputStream, "上传文件不能为空");
		Assert.notNull(fileHouseId, "fileHouseId不能为空");
		fileHouseItemService.upload(fileHouseId, chunkMd5, chunkIndex, inputStream);
	}
	
	private String checkPathname(String pathname) {
		Assert.notNull(pathname, "pathname不能为空");
		pathname = pathname.replace("\\", "/");
		List<String> list = RegexUtils.findList(pathname, "[:*?\"<>|]", 0);
		Assert.state(MyUtils.isEmpty(list), "资源名不能包含: * ? \" < > | ");
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		return pathname;
	}
	
	public void deleteDir(Bucket bucket, String path) {
		Assert.state(StringUtils.isNotBlank(path), "路径不能为空");
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
				String clientId = client.getClientId();
				if (clientService.isAlive(client)) {
					client.apis(httpRestTemplate).deleteDir(getDesPath(bucket, dir.getPath()));
					log.info("删除{}成功", dir.getPath());
					applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_DIR, ClientWorkLog.ExeStatus.SUCCESS, clientId, getDesPath(bucket, dir.getPath())));
				} else {
					applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_DIR, ClientWorkLog.ExeStatus.NOT_START, clientId, getDesPath(bucket, dir.getPath())));
				}
			}
		}
		dirService.deleteById(dir);
	}
	
	
	public PageInfo<DirAndResourceVo> findDirAndResourceVoListPage(String keyWord, Integer pageNum, Integer
			pageSize, Long bucketId, Long dirId) {
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
	
	@Transactional
	public void updateResource(ResourceUpdateDto resourceUpdateDto, Long userId, String bucketName) {
		Assert.state(StringUtils.isNotBlank(resourceUpdateDto.getPathname()), "资源名不能为空");
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(userId, bucketName);
		Assert.notNull(bucket, "bucket不存在");
		Resource resource = findById(resourceUpdateDto.getId());
		Assert.notNull(resource, "资源不存在");
		Long dirId = resource.getDirId();
		Dir dir = dirService.findById(dirId);
		Assert.state(dir != null && dir.getBucketId().equals(bucket.getId()), "越权操作");
		if (!resourceUpdateDto.getPathname().startsWith("/")) {
			resourceUpdateDto.setPathname("/" + resourceUpdateDto.getPathname());
		}
		if (!resourceUpdateDto.getPathname().equals(resource.getPathname())) {
			rename(bucketName, resource.getPathname(), resourceUpdateDto.getPathname());
			resource = findById(resource.getId());
		}
		BeanUtils.copyProperties(resourceUpdateDto, resource);
		updateById(resource);
	}
	
	@Transactional
	public void rename(String bucketName, String pathname, String desPathname) {
		Bucket bucket = bucketService.findOne("bucketName", bucketName);
		Assert.notNull(bucket, "bucket不存在");
		Resource resource = findResourceByPathnameAndBucketId(pathname, bucket.getId());
		Assert.notNull(resource, "源资源不存在:" + pathname);
		Resource desResource = findResourceByPathnameAndBucketId(desPathname, bucket.getId());
		Assert.state(desResource == null, "目标文件已存在");
		List<RelaClientResource> relas = relaClientResourceMapper.findList("resourceId", resource.getId());
		for (RelaClientResource rela : relas) {
			String clientId = rela.getClientId();
			Client client = clientService.findById(clientId);
			if (clientService.isAlive(client)) {
				client.apis(httpRestTemplate).moveFile(getDesPath(bucket, pathname), getDesPath(bucket, desPathname));
				applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.MOVE_FILE, ClientWorkLog.ExeStatus.SUCCESS, clientId, pathname, desPathname));
			} else {
				applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.MOVE_FILE, ClientWorkLog.ExeStatus.NOT_START, clientId, pathname, desPathname));
			}
		}
		Dir dir = addDir(desPathname, bucket.getId());
		resource.setPathname(desPathname);
		resource.setDirId(dir.getId());
		updateById(resource);
	}
	
	@Autowired
	private RedissonClient redissonClient;
	
	@Transactional
	public void addOrUpdateResource(String pathname, Boolean isPublic, String contentType, boolean cover, FileHouse fileHouse, Bucket bucket) {
		mt.utils.Assert.notNull(bucket, "bucket不存在");
		Assert.notNull(fileHouse, "fileHouse不能为空");
		Assert.state(fileHouse.getFileStatus() == FileHouse.FileStatus.OK, "fileHouse未完成合并");
		pathname = checkPathname(pathname);
		String lockKey = "bucket-" + bucket.getId();
		RLock lock = null;
		try {
			lock = redissonClient.getLock(lockKey);
			lock.lock(1, TimeUnit.MINUTES);
			Resource resource = findResourceByPathnameAndBucketId(pathname, bucket.getId());
			if (cover && resource != null) {
				//覆盖
				resource.setFileHouseId(fileHouse.getId());
				resource.setContentType(contentType);
				resource.setPathname(pathname);
				resource.setSizeByte(fileHouse.getSizeByte());
				resource.setIsPublic(isPublic);
				updateByIdSelective(resource);
				Long resourceId = resource.getId();
				List<RelaClientResource> relas = relaClientResourceMapper.findList("resourceId", resourceId);
				List<Client> clients = relas.stream().map(relaClientResource -> clientService.findById(relaClientResource.getClientId())).collect(Collectors.toList());
				Resource finalResource = resource;
				clients.forEach(client1 -> {
					List<Filter> filters = new ArrayList<>();
					filters.add(new Filter("resourceId", eq, resourceId));
					filters.add(new Filter("clientId", eq, client1.getClientId()));
					relaClientResourceMapper.deleteByExample(MyBatisUtils.createExample(RelaClientResource.class, filters));
					applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_FILE, ClientWorkLog.ExeStatus.NOT_START, client1.getClientId(), finalResource.getPathname()));
				});
			} else {
				org.springframework.util.Assert.state(resource == null, "资源文件已存在:" + pathname);
				resource = new Resource();
				resource.setPathname(pathname);
				resource.setContentType(contentType);
				resource.setIsPublic(isPublic);
				resource.setSizeByte(fileHouse.getSizeByte());
				resource.setFileHouseId(fileHouse.getId());
				addResourceIfNotExist(resource, bucket.getId());
			}
		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}
	}
}
