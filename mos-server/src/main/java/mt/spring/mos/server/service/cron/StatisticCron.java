package mt.spring.mos.server.service.cron;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.service.AuditService;
import mt.spring.mos.server.service.BucketService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 统计
 *
 * @Author Martin
 * @Date 2021/2/10
 */
@Component
public class StatisticCron {
	@Autowired
	private AuditService auditService;
	@Autowired
	private BucketService bucketService;
	
	/**
	 * 自动归档30天以前的审计数据
	 */
	@Scheduled(cron = "0 0 4 * * ?")
	public void autoArchive() {
		Calendar startCalendar = Calendar.getInstance();
		startCalendar.setTime(new Date());
		startCalendar.add(Calendar.MONTH, -1);
		Date time = startCalendar.getTime();
		String beforeDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
		boolean hasNext = true;
		while (hasNext) {
			PageHelper.startPage(1, 1000);
			List<Audit> list = auditService.findByFilter(new Filter("createdDate", Filter.Operator.lt, beforeDate));
			PageInfo<Audit> pageInfo = new PageInfo<>(list);
			List<Audit> audits = pageInfo.getList();
			if (CollectionUtils.isNotEmpty(audits)) {
				auditService.archive(audits);
			}
			hasNext = pageInfo.isHasNextPage();
		}
	}
	
	@Scheduled(fixedDelay = 7200 * 1000L)
	public void statisticCache() {
		List<Bucket> all = bucketService.findAll();
		if (CollectionUtils.isEmpty(all)) {
			return;
		}
		for (Bucket bucket : all) {
			auditService.updateStatisticInfoFromCache(bucket.getId());
			auditService.update24HoursRequestListFromCache(bucket.getId());
			auditService.update24HoursFlowListFromCache(bucket.getId());
		}
	}
	
	@Scheduled(cron = "0 0 3 * * ?")
	public void statisticCache2() {
		List<Bucket> all = bucketService.findAll();
		if (CollectionUtils.isEmpty(all)) {
			return;
		}
		for (Bucket bucket : all) {
			auditService.update30DaysFlowListFromCache(bucket.getId());
			auditService.update30DaysRequestListFromCache(bucket.getId());
		}
	}
}
