package mt.spring.mos.server.service.cron;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import mt.common.tkmapper.Filter;
import mt.spring.mos.server.annotation.DistributeJob;
import mt.spring.mos.server.entity.MosServerProperties;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.po.AuditArchive;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.po.ClientWorkLog;
import mt.spring.mos.server.service.AuditArchiveService;
import mt.spring.mos.server.service.AuditService;
import mt.spring.mos.server.service.BucketService;
import mt.spring.mos.server.service.ClientWorkLogService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
@Slf4j
public class StatisticCron {
	@Autowired
	private AuditService auditService;
	@Autowired
	private AuditArchiveService auditArchiveService;
	@Autowired
	private BucketService bucketService;
	@Autowired
	private MosServerProperties mosServerProperties;
	@Autowired
	private ClientWorkLogService clientWorkLogService;
	
	@Scheduled(cron = "${mos.server.clear.cron:0 0 3 * * ?}")
	@DistributeJob
	public void autoClear() {
		MosServerProperties.ClearConfig clear = mosServerProperties.getClear();
		Boolean enabledClearAuditLog = clear.getAuditLogEnabled();
		if (enabledClearAuditLog != null && enabledClearAuditLog) {
			boolean hasNext = true;
			while (hasNext) {
				PageHelper.startPage(1, 1000);
				List<Audit> list = auditService.findByFilter(new Filter("createdDate", Filter.Operator.lt, getBeforeDaysDate(clear.getAuditLogBeforeDays())));
				PageInfo<Audit> pageInfo = new PageInfo<>(list);
				List<Audit> audits = pageInfo.getList();
				if (CollectionUtils.isNotEmpty(audits)) {
					audits.forEach(audit -> {
						log.info("clear audit log {}...", audit.getId());
						auditService.deleteById(audit);
					});
				}
				hasNext = pageInfo.isHasNextPage();
			}
		}
		Boolean enabledClearArchive = clear.getArchiveEnabled();
		if (enabledClearArchive != null && enabledClearArchive) {
			boolean hasNext = true;
			while (hasNext) {
				PageHelper.startPage(1, 1000);
				List<AuditArchive> list = auditArchiveService.findByFilter(new Filter("createdDate", Filter.Operator.lt, getBeforeDaysDate(clear.getArchiveBeforeDays())));
				PageInfo<AuditArchive> pageInfo = new PageInfo<>(list);
				List<AuditArchive> audits = pageInfo.getList();
				if (CollectionUtils.isNotEmpty(audits)) {
					audits.forEach(auditArchive -> {
						log.info("clear archive {}...", auditArchive.getId());
						auditArchiveService.deleteById(auditArchive);
					});
				}
				hasNext = pageInfo.isHasNextPage();
			}
		}
		Boolean enabledClearWorkLog = clear.getWorkLogEnabled();
		if (enabledClearWorkLog != null && enabledClearWorkLog) {
			boolean hasNext = true;
			while (hasNext) {
				PageHelper.startPage(1, 1000);
				List<Filter> filters = new ArrayList<>();
				filters.add(new Filter("createdDate", Filter.Operator.lt, getBeforeDaysDate(clear.getWorkLogBeforeDays())));
				filters.add(new Filter("exeStatus", Filter.Operator.eq, ClientWorkLog.ExeStatus.SUCCESS));
				List<ClientWorkLog> list = clientWorkLogService.findByFilters(filters);
				PageInfo<ClientWorkLog> pageInfo = new PageInfo<>(list);
				List<ClientWorkLog> logs = pageInfo.getList();
				if (CollectionUtils.isNotEmpty(logs)) {
					logs.forEach(clientWorkLog -> {
						log.info("clear clientWorkLog {}", clientWorkLog.getId());
						clientWorkLogService.deleteById(clientWorkLog);
					});
				}
				hasNext = pageInfo.isHasNextPage();
			}
		}
	}
	
	private Date getBeforeDaysDate(int days) {
		Calendar startCalendar = Calendar.getInstance();
		startCalendar.setTime(new Date());
		startCalendar.add(Calendar.DAY_OF_MONTH, -Math.abs(days));
		return startCalendar.getTime();
	}
	
	/**
	 * 自动归档审计数据
	 */
	@Scheduled(cron = "${mos.server.archive.cron:0 0 2 * * ?}")
	@DistributeJob
	public void autoArchive() {
		MosServerProperties.ArchiveConfig archive = mosServerProperties.getArchive();
		Boolean enabled = archive.getEnabled();
		if (enabled == null || !enabled) {
			return;
		}
		
		boolean hasNext = true;
		while (hasNext) {
			PageHelper.startPage(1, 1000);
			List<Audit> list = auditService.findByFilter(new Filter("createdDate", Filter.Operator.lt, getBeforeDaysDate(archive.getBeforeDays())));
			PageInfo<Audit> pageInfo = new PageInfo<>(list);
			List<Audit> audits = pageInfo.getList();
			if (CollectionUtils.isNotEmpty(audits)) {
				auditService.archive(audits);
			}
			hasNext = pageInfo.isHasNextPage();
		}
	}
	
	@Scheduled(fixedDelay = 7200 * 1000L)
	@DistributeJob
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
	@DistributeJob
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
