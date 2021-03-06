package mt.spring.mos.server.service;

import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.common.tkmapper.Filter;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.server.dao.ShareDirMapper;
import mt.spring.mos.server.dao.ShareMapper;
import mt.spring.mos.server.dao.ShareResourceMapper;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.po.Share;
import mt.spring.mos.server.entity.po.ShareDir;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author Martin
 * @Date 2021/2/26
 */
@Service
public class ShareService extends BaseServiceImpl<Share> {
	@Autowired
	private ShareMapper shareMapper;
	
	@Override
	public BaseMapper<Share> getBaseMapper() {
		return shareMapper;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public Share createShare(@NotNull String name, @NotNull Long bucketId, long expireSeconds) {
		Share share = new Share();
		share.setBucketId(bucketId);
		share.setExpiredDate(new Date(System.currentTimeMillis() + expireSeconds * 1000));
		share.setName(name);
		save(share);
		return share;
	}
	
	public Share findOneByBucketIdAndId(Long bucketId, Long id) {
		List<Filter> filters = new ArrayList<>();
		filters.add(new Filter("bucketId", Filter.Operator.eq, bucketId));
		filters.add(new Filter("id", Filter.Operator.eq, id));
		return findOneByFilters(filters);
	}
}
