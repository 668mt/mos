package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.sdk.type.DirPathsEncryptContent;
import mt.spring.mos.server.entity.dto.DirUpdateDto;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.entity.vo.DirDetailInfo;
import mt.utils.common.Assert;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@Service
@Slf4j
public class DirService extends BaseServiceImpl<Dir> {
	@Autowired
	private BucketService bucketService;
	@Autowired
	private AuditService auditService;
	@Autowired
	@Lazy
	private ResourceService resourceService;
	
	public List<Dir> findAllParentDir(Dir dir) {
		List<Dir> dirs = new ArrayList<>();
		Long parentId = dir.getParentId();
		while (parentId != null) {
			Dir parent = findById(parentId);
			dirs.add(parent);
			parentId = parent.getParentId();
		}
		return dirs;
	}
	
	@Transactional(readOnly = true)
	public Dir findOneByPathAndBucketId(String path, Long bucketId, Boolean isDelete) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("path", Filter.Operator.eq, path));
		filters.add(new Filter("bucketId", Filter.Operator.eq, bucketId));
		if (isDelete != null) {
			filters.add(new Filter("isDelete", Filter.Operator.eq, isDelete));
		}
		return findOneByFilters(filters);
	}
	
	@Transactional(readOnly = true)
	public Dir findOneByDirIdAndBucketId(Long dirId, Long bucketId, Boolean isDelete) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("id", Filter.Operator.eq, dirId));
		filters.add(new Filter("bucketId", Filter.Operator.eq, bucketId));
		if (isDelete != null) {
			filters.add(new Filter("isDelete", Filter.Operator.eq, isDelete));
		}
		return findOneByFilters(filters);
	}
	
	public String getParentPath(String pathname) {
		if (!pathname.startsWith("/")) {
			pathname = "/" + pathname;
		}
		int lastIndexOf = pathname.lastIndexOf("/");
		String parentPath = pathname.substring(0, lastIndexOf);
		if (StringUtils.isBlank(parentPath)) {
			return "/";
		}
		return parentPath;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public Dir addDir(String path, Long bucketId) {
		bucketService.lockForUpdate(bucketId);
		return addDir0(path, bucketId);
	}
	
	public Dir addDir0(String path, Long bucketId) {
		if (!"/".equals(path) && path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		String finalPath = path;
		
		Dir findDir = findOneByPathAndBucketId(finalPath, bucketId, null);
		if (findDir != null) {
			log.debug("dir[{}]存在", finalPath);
			if (findDir.getIsDelete()) {
				recover(bucketId, findDir.getId(), false, true);
				return findById(findDir.getId());
			} else {
				return findDir;
			}
		}
		log.debug("dir[{}]不存在，进行创建", finalPath);
		Dir parentDir = null;
		if (!"/".equalsIgnoreCase(finalPath)) {
			String parentPath = getParentPath(finalPath);
			parentDir = addDir0(parentPath, bucketId);
		}
		
		Dir dir = new Dir();
		dir.setPath(finalPath);
		dir.setBucketId(bucketId);
		if (parentDir != null) {
			dir.setParentId(parentDir.getId());
		}
		log.debug("创建dir:{}", finalPath);
		save(dir);
		auditService.writeRequestsRecord(bucketId, 1);
		return dir;
	}
	
	@Override
	public Dir findOneByFilters(List<Filter> filters) {
		return super.findOneByFilters(filters);
	}
	
	@Transactional
	public void updatePath(Long bucketId, DirUpdateDto dirUpdateDto) {
		String newPath = dirUpdateDto.getPath();
		Assert.notBlank(newPath, "路径不能为空");
		Assert.state(!newPath.contains(".."), "非法路径：" + newPath);
		if (!newPath.startsWith("/")) {
			newPath = "/" + newPath;
		}
		bucketService.lockForUpdate(bucketId);
		Dir findDir = findOneByPathAndBucketId(newPath, bucketId, null);
		if (findDir != null) {
			if (findDir.getIsDelete()) {
				deleteById(findDir);
			} else {
				throw new IllegalStateException("路径" + newPath + "已存在");
			}
		}
		Dir parentDir = addDir(getParentPath(newPath), bucketId);
		Dir currentDir = findById(dirUpdateDto.getId());
		Assert.state(!"/".equals(currentDir.getPath()), "不能修改根的路径");
		auditService.writeRequestsRecord(bucketId, 1);
		currentDir.setParentId(parentDir.getId());
		currentDir.setPath(newPath);
		updateById(currentDir);
		updateChildDirPath(currentDir);
	}
	
	@Transactional
	public void updateChildDirPath(Dir dir) {
		List<Dir> children = findList("parentId", dir.getId());
		String parentPath = dir.getPath();
		if (CollectionUtils.isNotEmpty(children)) {
			for (Dir child : children) {
				child.setPath(parentPath + child.getName());
				updateById(child);
				updateChildDirPath(child);
			}
		}
	}
	
	private void updateParentDir(Dir dir, Dir parentDir) {
		String path = dir.getPath();
		File file = new File(path);
		String name = file.getName();
		String desPath = parentDir.getPath() + "/" + name;
		dir.setParentId(parentDir.getId());
		dir.setPath(desPath);
		updateById(dir);
		List<Dir> children = findList("parentId", dir.getId());
		if (CollectionUtils.isNotEmpty(children)) {
			for (Dir child : children) {
				updateParentDir(child, dir);
			}
		}
	}
	
	/**
	 * 合并文件夹，同名文件将进行覆盖
	 *
	 * @param bucketId 桶
	 * @param srcId    源路径
	 * @param desId    目标路径
	 */
	@Transactional
	public void mergeDir(Long bucketId, Long srcId, Long desId) {
		Dir srcDir = findOneByDirIdAndBucketId(srcId, bucketId, false);
		Dir desDir = findOneByDirIdAndBucketId(desId, bucketId, false);
		Assert.notNull(srcDir, "源路径不存在");
		Assert.notNull(desDir, "目标路径不存在");
		//把srcDir下的子目录移过去
		List<Dir> children = findList("parentId", srcDir.getId());
		if (CollectionUtils.isNotEmpty(children)) {
			for (Dir child : children) {
				updateParentDir(child, desDir);
			}
		}
		//把srcDir下的文件移过去
		List<Resource> desResources = resourceService.findByFilter(new Filter("dirId", Filter.Operator.eq, desId));
		if (CollectionUtils.isNotEmpty(desResources)) {
			//判断文件是否重名
			for (Resource desResource : desResources) {
				List<Filter> filters = new ArrayList<>();
				filters.add(new Filter("dirId", Filter.Operator.eq, srcId));
				filters.add(new Filter("name", Filter.Operator.eq, desResource.getName()));
				Resource findResource = resourceService.findOneByFilters(filters);
				if (findResource != null) {
					//重名文件存在，删除进行覆盖
					resourceService.deleteById(desResource);
				}
			}
		}
		resourceService.changeDir(srcId, desId);
		//删除原文件夹
		deleteById(srcDir);
	}
	
	@Transactional
	public void deleteDir(Long bucketId, long dirId) {
		bucketService.lockForUpdate(bucketId);
		Dir dir = findOneByDirIdAndBucketId(dirId, bucketId, false);
		if (dir == null) {
			return;
		}
		dir.setIsDelete(true);
		dir.setDeleteTime(new Date());
		updateById(dir);
		List<Resource> resources = resourceService.findResourcesInDir(dirId);
		if (CollectionUtils.isNotEmpty(resources)) {
			for (Resource resource : resources) {
				resourceService.deleteResource(bucketId, resource.getId());
			}
		}
		List<Dir> children = findChildren(dirId);
		if (CollectionUtils.isNotEmpty(children)) {
			for (Dir child : children) {
				deleteDir(bucketId, child.getId());
			}
		}
	}
	
	@Transactional
	public void recover(Long bucketId, Long dirId, boolean recoverChildren, boolean recoverParent) {
		bucketService.lockForUpdate(bucketId);
		Dir dir = findOneByDirIdAndBucketId(dirId, bucketId, true);
		if (dir == null) {
			return;
		}
		dir.setIsDelete(false);
		dir.setDeleteTime(null);
		updateById(dir);
		if (recoverChildren) {
			List<Resource> resources = resourceService.findResourcesInDir(dirId);
			if (CollectionUtils.isNotEmpty(resources)) {
				for (Resource resource : resources) {
					resourceService.recover(bucketId, resource.getId(), false);
				}
			}
			List<Dir> children = findChildren(dirId);
			if (CollectionUtils.isNotEmpty(children)) {
				for (Dir child : children) {
					recover(bucketId, child.getId(), true, false);
				}
			}
		}
		Long parentId = dir.getParentId();
		if (recoverParent && parentId != null) {
			recover(bucketId, parentId, false, true);
		}
	}
	
	public List<Dir> findChildren(long dirId) {
		return findList("parentId", dirId);
	}
	
	
	@Transactional
	public void realDeleteDir(Long bucketId, long dirId) {
		bucketService.lockForUpdate(bucketId);
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("id", Filter.Operator.eq, dirId));
		filters.add(new Filter("bucketId", Filter.Operator.eq, bucketId));
		Dir dir = findOneByFilters(filters);
		org.springframework.util.Assert.notNull(dir, "路径不存在");
		auditService.writeRequestsRecord(bucketId, 1);
		deleteById(dir);
	}
	
	@Transactional(rollbackFor = Exception.class)
	public boolean realDeleteDir(Long bucketId, String path) {
		org.springframework.util.Assert.state(StringUtils.isNotBlank(path), "路径不能为空");
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("path", Filter.Operator.eq, path));
		filters.add(new Filter("bucketId", Filter.Operator.eq, bucketId));
		Dir dir = findOneByFilters(filters);
		if (dir != null) {
			realDeleteDir(bucketId, dir.getId());
			return true;
		}
		return false;
	}
	
	@Transactional
	public List<Dir> getRealDeleteDirsBefore(Integer beforeDays) {
		Calendar instance = Calendar.getInstance();
		instance.add(Calendar.DAY_OF_MONTH, -Math.abs(beforeDays));
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("isDelete", Filter.Operator.eq, true));
		filters.add(new Filter("deleteTime", Filter.Operator.le, instance.getTime()));
		return findByFilters(filters);
	}
	
	public DirDetailInfo findDetailInfo(@NotNull Long id, int thumbCount) {
		List<Resource> thumbs = resourceService.findDirThumbs(id, thumbCount);
		DirDetailInfo dirDetailInfo = new DirDetailInfo();
		dirDetailInfo.setThumbs(thumbs);
		dirDetailInfo.setDirCount((long) count(Collections.singletonList(new Filter("parentId", Filter.Operator.eq, id))));
		dirDetailInfo.setFileCount((long) resourceService.count(Collections.singletonList(new Filter("dirId", Filter.Operator.eq, id))));
		return dirDetailInfo;
	}
}
