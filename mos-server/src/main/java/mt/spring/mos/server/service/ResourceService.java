package mt.spring.mos.server.service;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import mt.common.mybatis.mapper.BaseMapper;
import mt.common.mybatis.utils.MapperColumnUtils;
import mt.common.mybatis.utils.MyBatisUtils;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.common.utils.BeanUtils;
import mt.spring.mos.server.dao.RelaClientResourceMapper;
import mt.spring.mos.server.dao.ResourceMapper;
import mt.spring.mos.server.entity.dto.ResourceCopyDto;
import mt.spring.mos.server.entity.dto.ResourceSearchDto;
import mt.spring.mos.server.entity.dto.ResourceUpdateDto;
import mt.spring.mos.server.entity.dto.Thumb;
import mt.spring.mos.server.entity.po.*;
import mt.spring.mos.server.entity.vo.DirAndResourceVo;
import mt.spring.mos.server.listener.ClientWorkLogEvent;
import mt.spring.mos.server.service.clientapi.ClientApiFactory;
import mt.spring.mos.server.service.clientapi.IClientApi;
import mt.spring.mos.server.service.thumb.ThumbSupport;
import mt.utils.RegexUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;
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
	private DirService dirService;
	@Autowired
	@Lazy
	private BucketService bucketService;
	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	private FileHouseService fileHouseService;
	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	@Autowired
	private RedissonClient redissonClient;
	@Autowired
	private List<ThumbSupport> thumbSupports;
	@Autowired
	@Lazy
	private AuditService auditService;
	@Autowired
	private ClientApiFactory clientApiFactory;
	
	@Override
	public BaseMapper<Resource> getBaseMapper() {
		return resourceMapper;
	}
	
	public String getName(String pathname) {
		return new File(pathname).getName();
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void addOrUpdateResource(String pathname, Long lastModified, Boolean isPublic, String contentType, boolean cover, FileHouse fileHouse, Bucket bucket) {
		Assert.notNull(bucket, "bucket不存在");
		Assert.notNull(fileHouse, "fileHouse不能为空");
		Assert.state(fileHouse.getFileStatus() == FileHouse.FileStatus.OK, "fileHouse未完成合并");
		pathname = checkPathname(pathname);
		bucketService.lockForUpdate(bucket.getId());
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
				resource.setSizeByte(fileHouse.getSizeByte());
				resource.setIsPublic(isPublic);
				if (lastModified != null) {
					resource.setLastModified(lastModified);
				}
				updateByIdSelective(resource);
				Long resourceId = resource.getId();
				List<RelaClientResource> relas = relaClientResourceMapper.findList("resourceId", resourceId);
				List<Client> clients = relas.stream().map(relaClientResource -> clientService.findById(relaClientResource.getClientId())).collect(Collectors.toList());
				Resource finalResource = resource;
				clients.forEach(client1 -> {
					List<Filter> filters = new ArrayList<>();
					filters.add(new Filter("resourceId", eq, resourceId));
					filters.add(new Filter("clientId", eq, client1.getId()));
					relaClientResourceMapper.deleteByExample(MyBatisUtils.createExample(RelaClientResource.class, filters));
					applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_FILE, ClientWorkLog.ExeStatus.NOT_START, client1.getId(), getPathname(finalResource)));
				});
			} else {
				org.springframework.util.Assert.state(resource == null, "资源文件已存在:" + pathname);
				resource = new Resource();
				resource.setContentType(contentType);
				resource.setIsPublic(isPublic);
				resource.setSizeByte(fileHouse.getSizeByte());
				resource.setFileHouseId(fileHouse.getId());
				if (lastModified != null) {
					resource.setLastModified(lastModified);
				}
				addResourceIfNotExist(pathname, resource, bucket.getId());
			}
		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}
	}
	
	private void addResource(String pathname, Resource resource, Long bucketId) {
		Assert.state(StringUtils.isNotBlank(pathname), "资源名称不能为空");
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		resource.setName(getName(pathname));
		Dir dir = dirService.addDir(dirService.getParentPath(pathname), bucketId);
		Assert.notNull(dir, "文件夹不能为空");
		Bucket bucket = bucketService.findById(bucketId);
		if (resource.getIsPublic() == null) {
			resource.setIsPublic(bucket.getDefaultIsPublic());
		}
		resource.setDirId(dir.getId());
		resource.setSuffix("." + resource.getExtension());
		resource.setVisits(0L);
		save(resource);
		createThumb(resource.getId());
	}
	
	private void addResourceIfNotExist(String pathname, Resource resource, Long bucketId) {
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		Resource findResource = findResourceByPathnameAndBucketId(pathname, bucketId);
		if (findResource == null) {
			log.info("新增文件{}", pathname);
			addResource(pathname, resource, bucketId);
		}
	}
	
	@Transactional(readOnly = true)
	public Resource findResourceByPathnameAndBucketId(@NotNull String pathname, @NotNull Long bucketId) {
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		File file = new File(pathname);
		String path = file.getParent().replace("\\", "/");
		String name = file.getName();
		Dir dir = dirService.findOneByPathAndBucketId(path, bucketId);
		if (dir == null) {
			return null;
		}
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("dirId", eq, dir.getId()));
		filters.add(new Filter("name", eq, name));
		return findOneByFilters(filters);
	}
	
	@Transactional(readOnly = true)
	public Resource findResourceByIdAndBucketId(Long resourceId, @NotNull Long bucketId) {
		Resource resource = findById(resourceId);
		if (resource != null) {
			Dir dir = dirService.findById(resource.getDirId());
			if (dir.getBucketId().equals(bucketId)) {
				return resource;
			}
		}
		return null;
	}
	
	public String getDesPathname(Bucket bucket, Resource resource) {
		return getDesPathname(bucket, resource, false);
	}
	
	public String getPathname(Resource resource) {
		Dir dir = dirService.findById(resource.getDirId());
		Assert.notNull(dir, "dir " + resource.getDirId() + "不能为空");
		String pathname = dir.getPath() + "/" + resource.getName();
		return pathname.replace("//", "/");
	}
	
	public String getDesUrl(Client client, Bucket bucket, Resource resource, boolean thumb) {
		Long fileHouseId = resource.getFileHouseId();
		String url;
		String pathname = getPathname(resource);
		if (fileHouseId == null) {
			url = "/" + bucket.getId() + pathname;
		} else {
			FileHouse fileHouse;
			if (thumb) {
				Assert.notNull(resource.getThumbFileHouseId(), "资源" + pathname + "无缩略图");
				fileHouse = fileHouseService.findById(resource.getThumbFileHouseId());
			} else {
				fileHouse = fileHouseService.findById(fileHouseId);
			}
			url = fileHouse.getPathname();
			if (fileHouse.getEncode() != null && fileHouse.getEncode()) {
				url += "?encodeKey=" + fileHouse.getPathname();
			}
		}
		url = client.getUrl() + "/mos" + url;
		return url;
	}
	
	public String getDesPathname(Bucket bucket, Resource resource, boolean thumb) {
		Long fileHouseId = resource.getFileHouseId();
		String pathname = getPathname(resource);
		if (fileHouseId == null) {
			return "/" + bucket.getId() + pathname;
		} else {
			FileHouse fileHouse;
			if (thumb) {
				Assert.notNull(resource.getThumbFileHouseId(), "资源" + pathname + "无缩略图");
				fileHouse = fileHouseService.findById(resource.getThumbFileHouseId());
			} else {
				fileHouse = fileHouseService.findById(fileHouseId);
			}
			return fileHouse.getPathname();
		}
	}
	
	@Transactional
	public void deleteResources(@NotNull Bucket bucket, @Nullable Long[] dirIds, @Nullable Long[] fileIds) {
		if (dirIds != null) {
			for (Long dirId : dirIds) {
				dirService.deleteDir(bucket, dirId);
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
		bucketService.lockForUpdate(bucket.getId());
		Resource resource = findResourceByPathnameAndBucketId(pathname, bucket.getId());
		Assert.notNull(resource, "资源不存在");
		deleteResource(bucket, resource.getId());
	}
	
	@Transactional
	public void deleteResource(@NotNull Bucket bucket, long resourceId) {
		Resource resource = findById(resourceId);
		Assert.notNull(resource, "资源不存在");
		String pathname = getPathname(resource);
		auditService.doAudit(bucket.getId(), pathname, Audit.Type.WRITE, Audit.Action.deleteResource, null, 0);
		List<RelaClientResource> relas = relaClientResourceMapper.findList("resourceId", resourceId);
		if (CollectionUtils.isNotEmpty(relas)) {
			for (RelaClientResource rela : relas) {
				Long clientId = rela.getClientId();
				Client client = clientService.findById(clientId);
				if (resource.getFileHouseId() == null) {
					if (clientService.isAlive(client)) {
						Assert.state(StringUtils.isNotBlank(pathname), "资源名称不能为空");
						clientApiFactory.getClientApi(client).deleteFile(getDesPathname(bucket, resource));
						applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_FILE, ClientWorkLog.ExeStatus.SUCCESS, clientId, getDesPathname(bucket, resource)));
					} else {
						applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_FILE, ClientWorkLog.ExeStatus.NOT_START, clientId, getDesPathname(bucket, resource)));
					}
				}
			}
		}
		deleteById(resourceId);
	}
	
	private String checkPathname(String pathname) {
		Assert.notNull(pathname, "pathname不能为空");
		pathname = pathname.replace("\\", "/");
		List<String> list = RegexUtils.findList(pathname, "[:*?\"<>|]", 0);
		Assert.state(CollectionUtils.isEmpty(list), "资源名不能包含: * ? \" < > | ");
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		return pathname;
	}
	
	public final List<String> sortFields = Arrays.asList("path", "sizeByte", "createdDate", "createdBy", "updatedDate", "updatedBy", "isPublic", "contentType", "visits");
	
	
	public PageInfo<DirAndResourceVo> findDirAndResourceVoListPage(ResourceSearchDto resourceSearchDto, Long bucketId) {
		String sortField = resourceSearchDto.getSortField();
		String sortOrder = resourceSearchDto.getSortOrder();
		Integer pageNum = resourceSearchDto.getPageNum();
		Integer pageSize = resourceSearchDto.getPageSize();
		String keyWord = resourceSearchDto.getKeyWord();
		String path = resourceSearchDto.getPath();
		if ("readableSize".equals(sortField)) {
			sortField = "sizeByte";
		}
		if ("name".equals(sortField)) {
			sortField = "path";
		}
		Long dirId = null;
		if (StringUtils.isNotBlank(path)) {
			//当前路径搜索
			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}
			if (StringUtils.isBlank(path)) {
				path = "/";
			}
			Dir dir = dirService.findOneByPathAndBucketId(path, bucketId);
			if (dir == null) {
				return new PageInfo<>(new ArrayList<>());
			} else {
				dirId = dir.getId();
			}
		}
		
		if (StringUtils.isNotBlank(sortOrder) && StringUtils.isNotBlank(sortField) && sortFields.contains(sortField)) {
			String order = "descend".equalsIgnoreCase(sortOrder) ? "desc" : "asc";
			sortField = MapperColumnUtils.parseColumn(sortField);
			PageHelper.orderBy("is_dir desc ," + sortField + " " + order);
		}
		if (pageNum != null && pageSize != null) {
			PageHelper.startPage(pageNum, pageSize);
		}
		return new PageInfo<>(resourceMapper.findChildDirAndResourceList(keyWord, bucketId, dirId));
	}
	
	@Transactional
	@Async
	public void deleteAllResources(Long bucketId) {
		List<Dir> dirs = dirService.findList("bucketId", bucketId);
		if (CollectionUtils.isNotEmpty(dirs)) {
			dirs.sort(Comparator.comparing(Dir::getId));
			Bucket bucket = bucketService.findById(bucketId);
			Assert.notNull(bucket, "bucket不存在");
			for (Dir dir : dirs) {
				dirService.deleteDir(bucket, dir.getId());
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
		JSONObject before = new JSONObject();
		String pathname = getPathname(resource);
		if (resourceUpdateDto.getContentType() != null) {
			before.put("contentType", resource.getContentType());
		}
		if (resourceUpdateDto.getPathname() != null) {
			before.put("pathname", pathname);
		}
		if (resourceUpdateDto.getIsPublic() != null) {
			before.put("isPublic", pathname);
		}
		String auditRemark = "修改前：" + before.toJSONString() + ",修改后:" + JSONObject.toJSONString(resourceUpdateDto);
		auditService.doAudit(bucket.getId(), pathname, Audit.Type.WRITE, Audit.Action.updateResource, auditRemark, 0);
		if (!resourceUpdateDto.getPathname().startsWith("/")) {
			resourceUpdateDto.setPathname("/" + resourceUpdateDto.getPathname());
		}
		if (!resourceUpdateDto.getPathname().equals(pathname)) {
			rename(bucketName, pathname, resourceUpdateDto.getPathname());
			resource = findById(resource.getId());
		}
		BeanUtils.copyProperties(resourceUpdateDto, resource);
		if (resourceUpdateDto.getContentType() != null) {
			stringRedisTemplate.opsForValue().set("refresh-content-type:" + resourceUpdateDto.getId(), "true", 1, TimeUnit.HOURS);
		}
		updateById(resource);
	}
	
	@Transactional
	public void rename(String bucketName, String pathname, String desPathname) {
		Bucket bucket = bucketService.findOne("bucketName", bucketName);
		Assert.notNull(bucket, "bucket不存在");
		bucketService.lockForUpdate(bucket.getId());
		Resource resource = findResourceByPathnameAndBucketId(pathname, bucket.getId());
		Assert.notNull(resource, "源资源不存在:" + pathname);
		Resource desResource = findResourceByPathnameAndBucketId(desPathname, bucket.getId());
		Assert.state(desResource == null, "目标文件已存在");
		Dir dir = dirService.addDir(dirService.getParentPath(desPathname), bucket.getId());
		resource.setName(getName(desPathname));
		resource.setDirId(dir.getId());
		updateById(resource);
	}
	
	public List<Resource> findNeedConvertToFileHouse(int limit) {
		PageHelper.startPage(1, limit);
		return resourceMapper.findNeedConvertToFileHouse();
	}
	
	public FileHouse findFileHouse(Resource resource) {
		if (resource.getFileHouseId() != null) {
			return fileHouseService.findById(resource.getFileHouseId());
		}
		return null;
	}
	
	public List<Resource> findNeedGenerateThumb(int limit) {
		PageHelper.startPage(1, limit);
		List<String> suffixs = new ArrayList<>();
		for (ThumbSupport thumbSupport : thumbSupports) {
			suffixs.addAll(thumbSupport.getSuffixs());
		}
		return resourceMapper.findNeedGenerateThumb(suffixs);
	}
	
	@Async
	public Future<Boolean> createThumb(Long resourceId) {
		Resource resource = findById(resourceId);
		if (resource == null) {
			return new AsyncResult<>(false);
		}
		String pathname = getPathname(resource);
		if (resource.getThumbFileHouseId() != null) {
			log.warn("文件{}已经存在截图，跳过此次截图", pathname);
			return new AsyncResult<>(false);
		}
		if (resource.getFileHouseId() == null) {
			log.warn("文件{}无filehouseId，跳过此次截图", pathname);
			return new AsyncResult<>(false);
		}
		ThumbSupport thumbSupport = thumbSupports.stream().filter(t -> t.match(resource.getSuffix())).findFirst().orElse(null);
		if (thumbSupport == null) {
			log.warn("文件{}无截图生成器，跳过此次截图", pathname);
			return new AsyncResult<>(false);
		}
		try {
			log.info("生成{}截图", pathname);
			Client client = clientService.findRandomAvalibleClientForVisit(resource, false);
			FileHouse fileHouse = findFileHouse(resource);
			Boolean encode = fileHouse.getEncode();
			String encodeKey = encode != null && encode ? fileHouse.getPathname() : null;
			IClientApi clientApi = clientApiFactory.getClientApi(client);
			Thumb thumb = clientApi.createThumb(fileHouse.getPathname(), encodeKey, thumbSupport.getSeconds(), thumbSupport.getWidth());
			FileHouse thumbFileHouse = new FileHouse();
			thumbFileHouse.setEncode(true);
			thumbFileHouse.setPathname(thumb.getPathname());
			thumbFileHouse.setFileStatus(FileHouse.FileStatus.OK);
			thumbFileHouse.setSizeByte(thumb.getSize());
			thumbFileHouse.setMd5(thumb.getMd5());
			thumbFileHouse.setChunks(1);
			thumbFileHouse = fileHouseService.addFileHouseIfNotExists(thumbFileHouse, client);
			resource.setThumbFileHouseId(thumbFileHouse.getId());
			updateByIdSelective(resource);
			log.info("{}截图生成成功:{}", pathname, thumb);
			return new AsyncResult<>(true);
		} catch (RuntimeException e) {
			log.error(pathname + "截图失败：" + e.getMessage(), e);
			Integer thumbFails = resource.getThumbFails();
			thumbFails++;
			resource.setThumbFails(thumbFails);
			updateByIdSelective(resource);
		}
		return new AsyncResult<>(false);
	}
	
	public void addVisits(Long resourceId) {
		resourceMapper.addVisits(resourceId);
	}
	
	@Transactional
	public void changeDir(Long srcDirId, Long desDirId) {
		resourceMapper.changeDir(srcDirId, desDirId);
	}
	
	@Transactional
	public void copyToBucket(ResourceCopyDto resourceCopyDto, Bucket srcBucket, Bucket desBucket) {
		List<Long> dirIds = resourceCopyDto.getDirIds();
		List<Long> resourceIds = resourceCopyDto.getResourceIds();
		if (CollectionUtils.isNotEmpty(dirIds)) {
			for (Long dirId : dirIds) {
				Dir srcDir = dirService.findOneByDirIdAndBucketId(dirId, srcBucket.getId());
				Assert.notNull(srcDir, "未找到srcDir：" + dirId);
				copyDirToBucket(desBucket, srcDir);
			}
		}
		for (Long resourceId : resourceIds) {
			Resource resource = findResourceByIdAndBucketId(resourceId, srcBucket.getId());
			Assert.notNull(resource, "未找到resource:" + resourceId);
			copyResourceToBucket(desBucket, resource);
		}
	}
	
	private void copyDirToBucket(Bucket desBucket, Dir srcDir) {
		List<Resource> resources = findList("dirId", srcDir.getId());
		if (CollectionUtils.isNotEmpty(resources)) {
			copyResourceToBucket(desBucket, resources.toArray(new Resource[0]));
		}
		List<Dir> children = dirService.findList("parentId", srcDir.getId());
		if (CollectionUtils.isNotEmpty(children)) {
			for (Dir child : children) {
				copyDirToBucket(desBucket, child);
			}
		}
	}
	
	private void copyResourceToBucket(Bucket desBucket, Resource... resources) {
		if (resources == null) {
			return;
		}
		for (Resource resource : resources) {
			resource.setId(null);
			resource.setCreatedDate(null);
			resource.setCreatedBy(null);
			resource.setUpdatedDate(null);
			resource.setUpdatedBy(null);
			String pathname = getPathname(resource);
			System.out.println(pathname);
			addResourceIfNotExist(pathname, resource, desBucket.getId());
		}
	}
}
