//package mt.spring.mos.server.service;
//
//import mt.common.mybatis.mapper.BaseMapper;
//import mt.common.service.BaseServiceImpl;
//import mt.spring.mos.base.utils.Assert;
//import mt.spring.mos.server.dao.ShareResourceMapper;
//import mt.spring.mos.server.entity.po.Resource;
//import mt.spring.mos.server.entity.po.Share;
//import mt.spring.mos.server.entity.po.ShareResource;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
///**
// * @Author Martin
// * @Date 2021/2/26
// */
//@Service
//public class ShareResourceService extends BaseServiceImpl<ShareResource> {
//	@Autowired
//	private ShareResourceMapper shareResourceMapper;
//	@Autowired
//	private ResourceService resourceService;
//	@Autowired
//	@Lazy
//	private ShareService shareService;
//
//	@Override
//	public BaseMapper<ShareResource> getBaseMapper() {
//		return shareResourceMapper;
//	}
//
//	@Transactional(rollbackFor = Exception.class)
//	public void addShareResource(Long bucketId, Long shareId, Long resourceId) {
//		Share share = shareService.findOneByBucketIdAndId(bucketId, shareId);
//		Assert.notNull(share, "分享不存在");
//		Resource resource = resourceService.findResourceByIdAndBucketId(resourceId, bucketId);
//		Assert.notNull(resource, "文件不存在");
//		ShareResource shareResource = new ShareResource();
//		shareResource.setResourceId(resourceId);
//		shareResource.setShareId(shareId);
//		ShareResource findShareResource = shareResourceMapper.selectOne(shareResource);
//		if (findShareResource == null) {
//			save(shareResource);
//		}
//	}
//}
