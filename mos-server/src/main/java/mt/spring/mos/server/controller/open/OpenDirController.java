package mt.spring.mos.server.controller.open;

import mt.common.entity.ResResult;
import mt.spring.mos.server.annotation.OpenApi;
import mt.spring.mos.server.config.aop.MosContext;
import mt.spring.mos.server.entity.BucketPerm;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.service.AuditService;
import mt.spring.mos.server.service.DirService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @Author Martin
 * @Date 2021/1/16
 */
@RestController
@RequestMapping("/open/dir")
public class OpenDirController {
	@Autowired
	private DirService dirService;
	@Autowired
	private AuditService auditService;
	
	@DeleteMapping("/{bucketName}/deleteDir")
	@OpenApi(perms = BucketPerm.DELETE)
	public ResResult deleteDir(@ApiIgnore Bucket bucket, @PathVariable String bucketName, String pathname) {
		auditService.writeRequestsRecord(bucket.getId(), 1);
		return ResResult.success(dirService.realDeleteDir(bucket.getId(), pathname));
	}
}
