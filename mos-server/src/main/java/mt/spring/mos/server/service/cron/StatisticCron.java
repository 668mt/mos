package mt.spring.mos.server.service.cron;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.service.AuditService;
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

//	@Scheduled(fixedDelay = 3600 * 1000L)
//	public void statistic() {
//		auditService.findChartFlowList();
//		auditService.findChartRequestList();
//		auditService.findStatisticInfo();
//		auditService.findRequestStatisticFrom();
//		auditService.findFlowStatisticFrom();
//	}
}
