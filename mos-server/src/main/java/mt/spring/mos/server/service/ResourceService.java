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
import mt.spring.mos.sdk.type.PathnamesEncryptContent;
import mt.spring.mos.server.dao.RelaClientResourceMapper;
import mt.spring.mos.server.dao.ResourceMapper;
import mt.spring.mos.server.entity.dto.ResourceCopyDto;
import mt.spring.mos.server.entity.dto.ResourceSearchDto;
import mt.spring.mos.server.entity.dto.ResourceUpdateDto;
import mt.spring.mos.server.entity.po.*;
import mt.spring.mos.server.entity.vo.DirAndResourceVo;
import mt.spring.mos.server.listener.ClientWorkLogEvent;
import mt.spring.mos.server.service.clientapi.ClientApiFactory;
import mt.spring.mos.server.utils.UrlEncodeUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static mt.common.tkmapper.Filter.Operator.*;

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
	private CacheControlService cacheControlService;
	@Autowired
	@Lazy
	private AuditService auditService;
	@Autowired
	private ClientApiFactory clientApiFactory;
	@Autowired
	@Lazy
	private ResourceMetaService resourceMetaService;
	
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
		Resource resource = findResourceByPathnameAndBucketId(pathname, bucket.getId(), false);
		if (resource != null && cover) {
			//覆盖
			Assert.notNull(resource, "文件不存在:" + pathname);
			cacheControlService.setNoCache(resource.getId());
			resource.setFileHouseId(fileHouse.getId());
			resource.setContentType(contentType);
			resource.setSizeByte(fileHouse.getSizeByte());
			resource.setIsPublic(isPublic);
			resource.setThumbFileHouseId(null);
			resource.setThumbFails(0);
			if (lastModified != null) {
				resource.setLastModified(lastModified);
			}
			updateById(resource);
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
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					resourceMetaService.calculateMeta(bucket, resourceId);
				}
			});
		} else {
			Assert.isNull(resource, "资源文件已存在:" + pathname);
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
	}
	
	private void addResourceIfNotExist(String pathname, Resource resource, Long bucketId) {
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		Resource findResource = findResourceByPathnameAndBucketId(pathname, bucketId, false);
		if (findResource == null) {
			log.info("新增文件{}", pathname);
			addResource(pathname, resource, bucketId);
		}
	}
	
	private void addResource(String pathname, Resource resource, Long bucketId) {
		Assert.state(StringUtils.isNotBlank(pathname), "资源名称不能为空");
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		String name = getName(pathname);
		resource.setName(name);
		Dir dir = dirService.addDir(dirService.getParentPath(pathname), bucketId);
		Assert.notNull(dir, "文件夹不能为空");
		//如果文件被删除过，则将其覆盖
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("dirId", eq, dir.getId()));
		filters.add(new Filter("name", eq, name));
		filters.add(new Filter("isDelete", eq, true));
		Resource deletedResource = findOneByFilters(filters);
		if (deletedResource != null) {
			deleteById(deletedResource);
		}
		
		Bucket bucket = bucketService.findById(bucketId);
		if (resource.getIsPublic() == null) {
			resource.setIsPublic(bucket.getDefaultIsPublic());
		}
		resource.setDirId(dir.getId());
		String extension = resource.getExtension();
		if (extension != null) {
			resource.setSuffix("." + extension);
		}
		resource.setVisits(0L);
		save(resource);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				resourceMetaService.calculateMeta(bucket, resource.getId());
			}
		});
	}
	
	@Transactional(readOnly = true)
	public Resource findResourceByPathnameAndBucketId(@NotNull String pathname, @NotNull Long bucketId, Boolean isDelete) {
		Assert.state(StringUtils.isNotBlank(pathname), "pathname不能为空");
		if ("/".equals(pathname)) {
			return null;
		}
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		File file = new File(pathname);
		String parent = file.getParent();
		if (parent == null) {
			parent = "/";
		}
		String path = parent.replace("\\", "/");
		String name = file.getName();
		Dir dir = dirService.findOneByPathAndBucketId(path, bucketId, null);
		if (dir == null) {
			return null;
		}
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("dirId", eq, dir.getId()));
		filters.add(new Filter("name", eq, name));
		if (isDelete != null) {
			filters.add(new Filter("isDelete", eq, isDelete));
		}
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
	
	public String getDesPathname(Long bucketId, Resource resource) {
		return getDesPathname(bucketId, resource, false);
	}
	
	public String getPathname(Resource resource) {
		Dir dir = dirService.findById(resource.getDirId());
		Assert.notNull(dir, "dir " + resource.getDirId() + "不能为空");
		String pathname = dir.getPath() + "/" + resource.getName();
		return pathname.replace("//", "/");
	}
	
	public String getDesUrl(Client client, Bucket bucket, Resource resource, boolean thumb) {
		Long fileHouseId = resource.getFileHouseId();
		String pathname = getPathname(resource);
		if (fileHouseId == null) {
			String url = "/" + bucket.getId() + pathname;
			url = client.getUrl() + "/mos" + url;
			return url;
		} else {
			FileHouse fileHouse;
			if (thumb) {
				Assert.notNull(resource.getThumbFileHouseId(), "资源" + pathname + "无缩略图");
				fileHouse = fileHouseService.findById(resource.getThumbFileHouseId());
			} else {
				fileHouse = fileHouseService.findById(fileHouseId);
			}
			return getDesUrl(client, fileHouse);
		}
	}
	
	public String getDesUrl(Client client, FileHouse fileHouse) {
		String url = client.getUrl() + "/mos" + fileHouse.getPathname();
		if (fileHouse.getEncode() != null && fileHouse.getEncode()) {
			url += "?encodeKey=" + fileHouse.getPathname();
		}
		return url;
	}
	
	public String getDesPathname(Long bucketId, Resource resource, boolean thumb) {
		Long fileHouseId = resource.getFileHouseId();
		String pathname = getPathname(resource);
		if (fileHouseId == null) {
			return "/" + bucketId + pathname;
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
	public void deleteResources(@NotNull Long bucketId, @Nullable Long[] dirIds, @Nullable Long[] fileIds) {
		bucketService.lockForUpdate(bucketId);
		if (dirIds != null) {
			for (Long dirId : dirIds) {
				dirService.deleteDir(bucketId, dirId);
			}
		}
		if (fileIds != null) {
			for (Long fileId : fileIds) {
				deleteResource(bucketId, fileId);
			}
		}
	}
	
	@Transactional
	public void recovers(@NotNull Long bucketId, @Nullable Long[] dirIds, @Nullable Long[] fileIds) {
		if (dirIds != null) {
			for (Long dirId : dirIds) {
				dirService.recover(bucketId, dirId, true, true);
			}
		}
		if (fileIds != null) {
			for (Long fileId : fileIds) {
				recover(bucketId, fileId, true);
			}
		}
	}
	
	@Transactional
	public void recover(Long bucketId, Long resourceId, boolean recoverDir) {
		Resource resource = findResourceByIdAndBucketId(resourceId, bucketId);
		Assert.notNull(resource, "文件找不到：" + resourceId);
		resource.setIsDelete(false);
		resource.setDeleteTime(null);
		updateById(resource);
		if (recoverDir) {
			Long dirId = resource.getDirId();
			dirService.recover(bucketId, dirId, false, true);
		}
	}
	
	@Transactional
	public void realDeleteResources(Long bucketId, @Nullable Long[] dirIds, @Nullable Long[] fileIds) {
		bucketService.lockForUpdate(bucketId);
		if (dirIds != null) {
			for (Long dirId : dirIds) {
				dirService.realDeleteDir(bucketId, dirId);
			}
		}
		if (fileIds != null) {
			for (Long fileId : fileIds) {
				realDeleteResource(bucketId, fileId);
			}
		}
	}
	
	@Transactional
	public boolean realDeleteResource(Long bucketId, String pathname) {
		bucketService.lockForUpdate(bucketId);
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		Resource resource = findResourceByPathnameAndBucketId(pathname, bucketId, null);
		if (resource != null) {
			return realDeleteResource(bucketId, resource.getId());
		}
		return false;
	}
	
	@Transactional
	public void deleteResource(@NotNull Long bucketId, long resourceId) {
		bucketService.lockForUpdate(bucketId);
		Resource resource = findResourceByIdAndBucketId(resourceId, bucketId);
		if (resource == null) {
			return;
		}
		resource.setIsDelete(true);
		resource.setDeleteTime(new Date());
		updateById(resource);
	}
	
	@Transactional
	public boolean realDeleteResource(Long bucketId, long resourceId) {
		bucketService.lockForUpdate(bucketId);
		Resource resource = findResourceByIdAndBucketId(resourceId, bucketId);
		if (resource == null) {
			return false;
		}
		String pathname = getPathname(resource);
		auditService.doAudit(bucketId, new PathnamesEncryptContent(pathname), Audit.Type.WRITE, Audit.Action.realDeleteResource, null, 0);
		List<RelaClientResource> relas = relaClientResourceMapper.findList("resourceId", resourceId);
		if (CollectionUtils.isNotEmpty(relas)) {
			for (RelaClientResource rela : relas) {
				Long clientId = rela.getClientId();
				Client client = clientService.findById(clientId);
				if (resource.getFileHouseId() == null) {
					if (clientService.isAlive(client)) {
						Assert.state(StringUtils.isNotBlank(pathname), "资源名称不能为空");
						clientApiFactory.getClientApi(client).deleteFile(getDesPathname(bucketId, resource));
						applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_FILE, ClientWorkLog.ExeStatus.SUCCESS, clientId, getDesPathname(bucketId, resource)));
					} else {
						applicationEventPublisher.publishEvent(new ClientWorkLogEvent(this, ClientWorkLog.Action.DELETE_FILE, ClientWorkLog.ExeStatus.NOT_START, clientId, getDesPathname(bucketId, resource)));
					}
				}
			}
		}
		deleteById(resourceId);
		return true;
	}
	
	private String checkPathname(String pathname) {
		Assert.notNull(pathname, "pathname不能为空");
		pathname = pathname.replace("\\", "/");
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
		Boolean isDelete = resourceSearchDto.getIsDelete();
		String path = resourceSearchDto.getPath();
		List<String> pathKeyWords = new ArrayList<>();
		List<String> nameKeyWords = new ArrayList<>();
		List<String> pathExcludeKeyWords = new ArrayList<>();
		List<String> nameExcludeKeyWords = new ArrayList<>();
		if (StringUtils.isNotBlank(keyWord)) {
			String[] words = keyWord.split("\\s+");
			for (String word : words) {
				if (StringUtils.isBlank(word)) {
					continue;
				}
				word = word.replaceFirst("^([efp]+)：(.+)$", "$1:$2");
				if (word.startsWith("f:")
						|| word.startsWith("p:")
						|| word.startsWith("e:")
						|| word.startsWith("ef:")
						|| word.startsWith("ep:")) {
					String[] split = word.split(":");
					switch (split[0]) {
						case "f":
							nameKeyWords.add(word.substring(2));
							break;
						case "p":
							pathKeyWords.add(word.substring(2));
							break;
						case "e":
							nameExcludeKeyWords.add(word.substring(2));
							pathExcludeKeyWords.add(word.substring(2));
							break;
						case "ef":
							nameExcludeKeyWords.add(word.substring(3));
							break;
						case "ep":
							pathExcludeKeyWords.add(word.substring(3));
							break;
					}
				} else {
					pathKeyWords.add(word);
					nameKeyWords.add(word);
				}
			}
		}
		nameKeyWords = nameKeyWords.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
		pathKeyWords = pathKeyWords.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
		nameExcludeKeyWords = nameExcludeKeyWords.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
		pathExcludeKeyWords = pathExcludeKeyWords.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
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
			Dir dir = dirService.findOneByPathAndBucketId(path, bucketId, false);
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
		if (pageNum != null && pageSize != null && pageNum > 0 && pageSize > 0) {
			PageHelper.startPage(pageNum, pageSize);
		}
		return new PageInfo<>(resourceMapper.findChildDirAndResourceList(pathKeyWords,
				pathExcludeKeyWords,
				nameKeyWords,
				nameExcludeKeyWords,
				bucketId,
				isDelete,
				dirId,
				resourceSearchDto.getResourceId(),
				resourceSearchDto.getSuffixs(),
				resourceSearchDto.getIsFile(),
				resourceSearchDto.getIsDir()
		));
	}
	
	public DirAndResourceVo findFileInfo(Long bucketId, List<String> suffixs, String path, Long resourceId) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return resourceMapper.findFileInfo(bucketId, suffixs, path, resourceId);
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
				dirService.realDeleteDir(bucketId, dir.getId());
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
		auditService.doAudit(bucket.getId(), new PathnamesEncryptContent(pathname), Audit.Type.WRITE, Audit.Action.updateResource, auditRemark, 0);
		if (!resourceUpdateDto.getPathname().startsWith("/")) {
			resourceUpdateDto.setPathname("/" + resourceUpdateDto.getPathname());
		}
		if (!resourceUpdateDto.getPathname().equals(pathname)) {
			rename(bucketName, pathname, resourceUpdateDto.getPathname());
			resource = findById(resource.getId());
		}
		BeanUtils.copyProperties(resourceUpdateDto, resource);
		if (resourceUpdateDto.getContentType() != null) {
			cacheControlService.setNoCache(resource.getId());
		}
		String extension = resource.getExtension();
		if (extension != null) {
			resource.setSuffix("." + extension);
		}
		updateById(resource);
	}
	
	@Transactional
	public void rename(String bucketName, String pathname, String desPathname) {
		Bucket bucket = bucketService.findOne("bucketName", bucketName);
		Assert.notNull(bucket, "bucket不存在");
		bucketService.lockForUpdate(bucket.getId());
		Resource resource = findResourceByPathnameAndBucketId(pathname, bucket.getId(), false);
		Assert.notNull(resource, "源资源不存在:" + pathname);
		Resource desResource = findResourceByPathnameAndBucketId(desPathname, bucket.getId(), null);
		if (desResource != null) {
			if (desResource.getIsDelete()) {
				deleteById(desResource);
			} else {
				throw new IllegalStateException("目标文件已存在");
			}
		}
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
	
	public void addVisits(Long resourceId) {
		resourceMapper.addVisits(resourceId);
	}
	
	@Transactional
	public void changeDir(Long srcDirId, Long desDirId) {
		resourceMapper.changeDir(srcDirId, desDirId);
	}
	
	/**
	 * 复制文件或文件夹
	 *
	 * @param resourceCopyDto 复制的对象
	 * @param srcBucket       源桶
	 * @param desBucket       目标桶
	 */
	@Transactional
	public void copyToBucket(ResourceCopyDto resourceCopyDto, Bucket srcBucket, Bucket desBucket) {
		List<Long> dirIds = resourceCopyDto.getDirIds();
		List<Long> resourceIds = resourceCopyDto.getResourceIds();
		if (CollectionUtils.isNotEmpty(dirIds)) {
			for (Long dirId : dirIds) {
				Dir srcDir = dirService.findOneByDirIdAndBucketId(dirId, srcBucket.getId(), false);
				Assert.notNull(srcDir, "未找到srcDir：" + dirId);
				String desPath = StringUtils.isBlank(resourceCopyDto.getDesPath()) ? srcDir.getPath() : resourceCopyDto.getDesPath() + srcDir.getName();
				copyDirToBucket(desBucket, srcDir, desPath);
			}
		}
		if (CollectionUtils.isNotEmpty(resourceIds)) {
			for (Long resourceId : resourceIds) {
				Resource resource = findResourceByIdAndBucketId(resourceId, srcBucket.getId());
				Assert.notNull(resource, "未找到resource:" + resourceId);
				copyResourceToBucket(desBucket, resourceCopyDto.getDesPath(), resource);
			}
		}
	}
	
	@Transactional
	public void moveToBucket(@NotNull ResourceCopyDto resourceCopyDto, @NotNull Bucket srcBucket, @NotNull Bucket desBucket) {
		List<Long> dirIds = resourceCopyDto.getDirIds();
		List<Long> resourceIds = resourceCopyDto.getResourceIds();
		copyToBucket(resourceCopyDto, srcBucket, desBucket);
		realDeleteResources(srcBucket.getId(), dirIds.toArray(new Long[0]), resourceIds.toArray(new Long[0]));
	}
	
	private void copyDirToBucket(Bucket desBucket, Dir srcDir, String desPath) {
		List<Resource> resources = findList("dirId", srcDir.getId());
		if (CollectionUtils.isNotEmpty(resources)) {
			copyResourceToBucket(desBucket, desPath, resources.toArray(new Resource[0]));
		}
		List<Dir> children = dirService.findList("parentId", srcDir.getId());
		if (CollectionUtils.isNotEmpty(children)) {
			for (Dir child : children) {
				copyDirToBucket(desBucket, child, desPath + child.getName());
			}
		}
	}
	
	private void copyResourceToBucket(Bucket desBucket, @Nullable String desPath, Resource... resources) {
		if (resources == null) {
			return;
		}
		if (StringUtils.isNotBlank(desPath)) {
			desPath = desPath.replace("\\", "/");
			desPath = desPath.replace("//", "/");
			if (!desPath.endsWith("/")) {
				desPath += "/";
			}
		}
		for (Resource resource : resources) {
			resource.setId(null);
			resource.setCreatedDate(null);
			resource.setCreatedBy(null);
			resource.setUpdatedDate(null);
			resource.setUpdatedBy(null);
			String pathname = StringUtils.isNotBlank(desPath) ? desPath + resource.getName() : getPathname(resource);
			Resource findResource = findResourceByPathnameAndBucketId(pathname, desBucket.getId(), null);
			if (findResource != null) {
				deleteById(findResource);
			}
			addResource(pathname, resource, desBucket.getId());
			cacheControlService.setNoCache(resource.getId());
		}
	}
	
	public List<Resource> findResourcesInDir(long dirId) {
		return findList("dirId", dirId);
	}
	
	public List<Resource> getRealDeleteResourceBefore(Integer beforeDays) {
		Calendar instance = Calendar.getInstance();
		instance.add(Calendar.DAY_OF_MONTH, -Math.abs(beforeDays));
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("isDelete", eq, true));
		filters.add(new Filter("deleteTime", le, instance.getTime()));
		return findByFilters(filters);
	}
	
	@Transactional
	public void realDeleteResource(Resource resource) {
		Long dirId = resource.getDirId();
		Dir dir = dirService.findById(dirId);
		realDeleteResource(dir.getBucketId(), resource.getId());
	}
	
	public List<Resource> findDirThumbs(@NotNull Long dirId, int count) {
		Dir dir = dirService.findById(dirId);
		if (dir == null) {
			return new ArrayList<>();
		}
		PageHelper.startPage(1, count, "id desc");
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("dirId", eq, dirId));
		filters.add(new Filter("isDelete", eq, false));
		filters.add(new Filter("thumbFileHouseId", isNotNull));
		List<Resource> list = findByFilters(filters);
		for (Resource resource : list) {
			resource.setUrlEncodePath(UrlEncodeUtils.encodePathname(dir.getPath() + "/" + resource.getName()));
		}
		return list;
	}
}
