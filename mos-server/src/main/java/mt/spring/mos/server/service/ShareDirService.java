package mt.spring.mos.server.service;

import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.server.dao.ShareDirMapper;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.po.Share;
import mt.spring.mos.server.entity.po.ShareDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author Martin
 * @Date 2021/2/26
 */
@Service
public class ShareDirService extends BaseServiceImpl<ShareDir> {
	@Autowired
	private ShareDirMapper shareDirMapper;
	@Autowired
	private DirService dirService;
	@Autowired
	@Lazy
	private ShareService shareService;
	
	@Override
	public BaseMapper<ShareDir> getBaseMapper() {
		return shareDirMapper;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void addShareDir(Long bucketId, Long shareId, Long dirId) {
		Share share = shareService.findOneByBucketIdAndId(bucketId, shareId);
		Assert.notNull(share, "分享不存在");
		Dir dir = dirService.findOneByDirIdAndBucketId(bucketId, dirId);
		Assert.notNull(dir, "文件夹不存在");
		ShareDir shareDir = new ShareDir();
		shareDir.setShareId(shareId);
		shareDir.setDirId(dirId);
		ShareDir findShareDir = shareDirMapper.selectOne(shareDir);
		if (findShareDir == null) {
			shareDirMapper.insert(shareDir);
		}
	}
}
