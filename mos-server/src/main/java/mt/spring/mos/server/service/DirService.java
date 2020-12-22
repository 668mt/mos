package mt.spring.mos.server.service;

import com.alibaba.fastjson.JSONObject;
import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.dao.DirMapper;
import mt.spring.mos.server.entity.dto.DirUpdateDto;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.Dir;
import mt.utils.Assert;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@Service
public class DirService extends BaseServiceImpl<Dir> {
	@Autowired
	private DirMapper dirMapper;
	@Autowired
	private BucketService bucketService;
	@Autowired
	private LockService lockService;
	@Autowired
	private AuditService auditService;
	
	@Override
	public BaseMapper<Dir> getBaseMapper() {
		return dirMapper;
	}
	
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
	
	public Dir findOneByPathAndBucketId(String path, Long bucketId) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("path", Filter.Operator.eq, path));
		filters.add(new Filter("bucketId", Filter.Operator.eq, bucketId));
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
	
	@Transactional
	public Dir addDir(String path, Long bucketId) {
		if (!"/".equals(path) && path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		String finalPath = path;
		
		return lockService.doWithLock("addDir-" + path + "-" + bucketId, LockService.LockType.WRITE, 5, () -> {
			Dir findDir = findOneByPathAndBucketId(finalPath, bucketId);
			if (findDir != null) {
				return findDir;
			}
			Dir parentDir = null;
			if (!"/".equalsIgnoreCase(finalPath)) {
				String parentPath = getParentPath(finalPath);
				parentDir = addDir(parentPath, bucketId);
			}
			
			Dir dir = new Dir();
			dir.setPath(finalPath);
			dir.setBucketId(bucketId);
			if (parentDir != null) {
				dir.setParentId(parentDir.getId());
			}
			auditService.doAudit(bucketId, finalPath, Audit.Type.WRITE, Audit.Action.addDir);
			save(dir);
			return dir;
		});
	}
	
	@Override
	public Dir findOneByFilters(List<Filter> filters) {
		return super.findOneByFilters(filters);
	}
	
	@Transactional
	public void updatePath(Long userId, DirUpdateDto dirUpdateDto) {
		String newPath = dirUpdateDto.getPath();
		Assert.notBlank(newPath, "路径不能为空");
		Assert.state(!newPath.contains(".."), "非法路径：" + newPath);
		Bucket bucket = bucketService.findBucketByUserIdAndBucketName(userId, dirUpdateDto.getBucketName());
		Assert.notNull(bucket, "bucket" + dirUpdateDto.getBucketName() + "不存在");
		if (!newPath.startsWith("/")) {
			newPath = "/" + newPath;
		}
		Dir findDir = findOneByPathAndBucketId(newPath, bucket.getId());
		Assert.state(findDir == null, "路径" + newPath + "已存在");
		Dir parentDir = addDir(getParentPath(newPath), bucket.getId());
		Dir currentDir = findById(dirUpdateDto.getId());
		auditService.doAudit(currentDir.getBucketId(), currentDir.getPath(), Audit.Type.WRITE, Audit.Action.updateDir, currentDir.getPath() + "->" + newPath, 0);
		currentDir.setParentId(parentDir.getId());
		currentDir.setPath(newPath);
		updateById(currentDir);
	}
}
