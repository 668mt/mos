//package mt.spring.mos.server.controller.open;
//
//import io.swagger.annotations.ApiOperation;
//import mt.common.entity.ResResult;
//import mt.spring.mos.server.annotation.OpenApi;
//import mt.spring.mos.server.config.aop.MosContext;
//import mt.spring.mos.server.entity.BucketPerm;
//import mt.spring.mos.server.entity.po.Audit;
//import mt.spring.mos.server.entity.po.Bucket;
//import mt.spring.mos.server.service.AuditService;
//import mt.spring.mos.server.service.DirService;
//import mt.spring.mos.server.service.FileHouseService;
//import mt.spring.mos.server.service.cron.FileHouseCron;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//import springfox.documentation.annotations.ApiIgnore;
//
///**
// * @Author Martin
// * @Date 2021/1/16
// */
//@RestController
//@RequestMapping("/open/manage")
//public class OpenFileHouseController {
//	@Autowired
//	private FileHouseService fileHouseService;
//	@Autowired
//	private FileHouseCron fileHouseCron;
//
//	@DeleteMapping("/{bucketName}/deleteNotUsedFile/{recentDays}")
//	@OpenApi(perms = BucketPerm.DELETE)
//	public ResResult deleteNotUsedFile(@PathVariable String bucketName, @PathVariable Integer recentDays) {
//		fileHouseCron.checkFileHouseAndDeleteRecent(recentDays, false);
//		return ResResult.success();
//	}
//
//	@ApiOperation("清除回收站")
//	@GetMapping("/clear/trash")
//	public ResResult clearTrash(@RequestParam(defaultValue = "15") Integer beforeDays) {
//		trashCron.deleteTrashBeforeDays(beforeDays, false);
//		return ResResult.success();
//	}
//}
