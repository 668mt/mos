package mt.spring.mos.server.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import mt.common.entity.ResResult;
import mt.spring.mos.base.utils.Assert;
import mt.spring.mos.server.entity.po.Dir;
import mt.spring.mos.server.entity.po.Resource;
import mt.spring.mos.server.entity.vo.BackVo;
import mt.spring.mos.server.service.*;
import mt.spring.mos.server.service.cron.FileHouseBackCron;
import mt.spring.mos.server.service.cron.FileHouseCron;
import mt.spring.mos.server.service.cron.StatisticCron;
import mt.spring.mos.server.service.cron.TrashCron;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.Future;

/**
 * @Author Martin
 * @Date 2020/5/20
 */
@RestController
@RequestMapping("/admin")
@Api(tags = "管理接口")
public class ManageController {
	@Autowired
	private FileHouseService fileHouseService;
	@Autowired
	private FileHouseCron fileHouseCron;
	@Autowired
	private FileHouseBackCron fileHouseBackCron;
	@Autowired
	private ResourceService resourceService;
	@Autowired
	private StatisticCron statisticCron;
	@Autowired
	private TrashCron trashCron;
	@Autowired
	private ThumbService thumbService;
	@Autowired
	private DirService dirService;
	@Autowired
	private BucketService bucketService;
	@Autowired
	private ResourceMetaService resourceMetaService;
	
	@GetMapping("/back")
	@ApiOperation("备份某个资源")
	public ResResult back(Long fileHouseId, Integer amount) {
		BackVo backVo = new BackVo();
		backVo.setFileHouseId(fileHouseId);
		backVo.setDataFragmentsAmount(amount);
		fileHouseService.backFileHouse(backVo);
		return ResResult.success();
	}
	
	@ApiOperation("备份所有资源")
	@GetMapping("/back/all")
	public ResResult back() {
		fileHouseBackCron.checkBackFileHouse();
		return ResResult.success();
	}
	
	@ApiOperation("删除没有使用的文件")
	@DeleteMapping("/deleteNotUsedFile/{recentDays}")
	public ResResult deleteNotUsedFile(@PathVariable Integer recentDays) {
		fileHouseCron.checkFileHouseAndDeleteRecent(recentDays, false);
		return ResResult.success();
	}
	
	@ApiOperation("生成截图")
	@PostMapping("/createThumb")
	public ResResult createThumb(Long resourceId) throws Exception {
		Resource resource = resourceService.findById(resourceId);
		Assert.notNull(resource, "资源不能为空");
		Dir dir = dirService.findById(resource.getDirId());
		return ResResult.success(thumbService.createThumb(bucketService.findById(dir.getBucketId()), resource.getId()));
	}
	
	@ApiOperation("生成资源属性")
	@PostMapping("/createMeta")
	public ResResult createMeta(Long resourceId) throws Exception {
		Resource resource = resourceService.findById(resourceId);
		Assert.notNull(resource, "资源不能为空");
		Dir dir = dirService.findById(resource.getDirId());
		resourceMetaService.calculateMeta(bucketService.findById(dir.getBucketId()), resourceId);
		return ResResult.success();
	}
	
	@PostMapping("/refreshAll/video/length")
	@ApiOperation("刷新所有视频长度")
	public ResResult refreshAllVideoLength(){
		resourceMetaService.refreshAll();
		return ResResult.success();
	}
	
	@ApiOperation("归档")
	@GetMapping("/statistic/archive")
	public ResResult archive() {
		statisticCron.autoArchive();
		return ResResult.success();
	}
	
	
	@ApiOperation("清除回收站")
	@GetMapping("/clear/trash")
	public ResResult clearTrash(@RequestParam(defaultValue = "15") Integer beforeDays) {
		trashCron.deleteTrashBeforeDays(beforeDays, false);
		return ResResult.success();
	}
}
