package mt.spring.mos.server.service;

import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.spring.mos.server.dao.AuditArchiveMapper;
import mt.spring.mos.server.entity.po.AuditArchive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author Martin
 * @Date 2021/4/9
 */
@Service
public class AuditArchiveService extends BaseServiceImpl<AuditArchive> {
	@Autowired
	private AuditArchiveMapper auditArchiveMapper;
	
	@Override
	public BaseMapper<AuditArchive> getBaseMapper() {
		return auditArchiveMapper;
	}
}
