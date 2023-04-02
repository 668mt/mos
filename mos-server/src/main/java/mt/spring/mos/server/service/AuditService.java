package mt.spring.mos.server.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mt.common.hits.HitsRecorder;
import mt.common.service.BaseServiceImpl;
import mt.spring.mos.server.config.MosUserContext;
import mt.spring.mos.server.config.hits.MosHitsRecorder;
import mt.spring.mos.server.config.hits.TimeHits;
import mt.spring.mos.server.controller.ReadableOutputStream;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.vo.audit.ChartBy;
import mt.spring.mos.server.entity.vo.audit.ChartFlowData;
import mt.spring.mos.server.entity.vo.audit.ChartRequestData;
import mt.spring.mos.server.entity.vo.audit.StatisticInfo;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2020/12/19
 */
@Service
@Slf4j
public class AuditService extends BaseServiceImpl<Audit> {
	@Autowired
	@Lazy
	private BucketService bucketService;
	
	@Autowired
	private MosUserContext mosUserContext;
	private final ExecutorService executorService = Executors.newFixedThreadPool(5);
	
	@Autowired
	private HitsRecorder<String, Long> resourceVisitsRecorder;
	@Autowired
	private MosHitsRecorder hourReadBytesRecorder;
	@Autowired
	private MosHitsRecorder hourWriteBytesRecorder;
	@Autowired
	private MosHitsRecorder hourReadRequestsRecorder;
	@Autowired
	private MosHitsRecorder hourWriteRequestsRecorder;
	@Autowired
	private MosHitsRecorder dayReadBytesRecorder;
	@Autowired
	private MosHitsRecorder dayWriteBytesRecorder;
	@Autowired
	private MosHitsRecorder dayReadRequestsRecorder;
	@Autowired
	private MosHitsRecorder dayWriteRequestsRecorder;
	
	public void addResourceHits(@NotNull Long resourceId, int hits) {
		resourceVisitsRecorder.recordHits(resourceId, hits);
	}
	
	//	public List<FlowStatisticVo> findFlowStatisticFrom(Long userId, Audit.Type type, String startDate) {
//		List<BucketVo> bucketList = bucketService.findBucketList(userId);
//		if (CollectionUtils.isEmpty(bucketList)) {
//			return null;
//		}
//		return bucketList.stream().map(bucketVo -> findFlowStatisticFrom(bucketVo, type, startDate)).collect(Collectors.toList());
//	}
//
//	public FlowStatisticVo findFlowStatisticFrom(Bucket bucket, Audit.Type type, String startDate) {
//		FlowStatisticVo flowStatisticVo = new FlowStatisticVo();
//		flowStatisticVo.setBucketName(bucket.getBucketName());
//		flowStatisticVo.setStartDate(startDate);
//		flowStatisticVo.setType(type);
//		long bytes = auditMapper.findTotalFlowFromDate(bucket.getId(), type, startDate);
//		flowStatisticVo.setReadableFlow(SizeUtils.getReadableSize(bytes));
//		return flowStatisticVo;
//	}
//	public List<RequestStatisticVo> findRequestStatisticFrom(Long userId, Audit.Type type, String startDate) {
//		List<BucketVo> bucketList = bucketService.findBucketList(userId);
//		if (CollectionUtils.isEmpty(bucketList)) {
//			return null;
//		}
//		return bucketList.stream().map(bucketVo -> findRequestStatisticFrom(bucketVo, type, startDate)).collect(Collectors.toList());
//	}
//
//	public RequestStatisticVo findRequestStatisticFrom(Bucket bucket, Audit.Type type, String startDate) {
//		RequestStatisticVo requestStatisticVo = new RequestStatisticVo();
//		requestStatisticVo.setBucketName(bucket.getBucketName());
//		requestStatisticVo.setStartDate(startDate);
//		requestStatisticVo.setType(type);
//		long requests = auditMapper.findTotalRequestFromDate(bucket.getId(), type, startDate);
//		requestStatisticVo.setRequests(requests);
//		return requestStatisticVo;
//	}
//
//	public List<String> createXList(ChartBy by, String startDate, String endDate) {
//		try {
//			Date start = DateUtils.parse(startDate);
//			Date end = DateUtils.parse(endDate);
//			List<String> list = new ArrayList<>();
//			SimpleDateFormat dateFormat;
//			Calendar startCalendar = Calendar.getInstance();
//			startCalendar.setTime(start);
//			long endTime = end.getTime();
//			switch (by) {
//				case hour:
//					startCalendar.set(Calendar.MINUTE, 0);
//					dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:00");
//					while (startCalendar.getTime().getTime() <= endTime) {
//						list.add(dateFormat.format(startCalendar.getTime()));
//						startCalendar.add(Calendar.HOUR, 1);
//					}
//					break;
//				case day:
//					startCalendar.set(Calendar.MINUTE, 0);
//					startCalendar.set(Calendar.HOUR_OF_DAY, 0);
//					dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//					while (startCalendar.getTime().getTime() <= endTime) {
//						list.add(dateFormat.format(startCalendar.getTime()));
//						startCalendar.add(Calendar.DAY_OF_MONTH, 1);
//					}
//					break;
//				case month:
//					startCalendar.set(Calendar.MINUTE, 0);
//					startCalendar.set(Calendar.HOUR_OF_DAY, 0);
//					startCalendar.set(Calendar.DAY_OF_MONTH, 1);
//					dateFormat = new SimpleDateFormat("yyyy-MM");
//					while (startCalendar.getTime().getTime() <= endTime) {
//						list.add(dateFormat.format(startCalendar.getTime()));
//						startCalendar.add(Calendar.MONTH, 1);
//					}
//					break;
//				case year:
//					startCalendar.set(Calendar.MINUTE, 0);
//					startCalendar.set(Calendar.HOUR_OF_DAY, 0);
//					startCalendar.set(Calendar.DAY_OF_MONTH, 1);
//					startCalendar.set(Calendar.MONTH, 0);
//					dateFormat = new SimpleDateFormat("yyyy");
//					while (startCalendar.getTime().getTime() <= endTime) {
//						list.add(dateFormat.format(startCalendar.getTime()));
//						startCalendar.add(Calendar.YEAR, 1);
//					}
//					break;
//			}
//			return list;
//		} catch (ParseException e) {
//			log.error(e.getMessage(), e);
//			throw new RuntimeException(e);
//		}
//	}
//
	
	public List<ChartRequestData> findChartRequestList(@NotNull Long bucketId, @NotNull Date startDate, @Nullable Date endDate, @NotNull ChartBy by) {
		if (endDate == null) {
			endDate = new Date();
		}
		List<TimeHits> readRequests;
		List<TimeHits> writeRequests;
		if (by == ChartBy.hour) {
			readRequests = hourReadRequestsRecorder.getHitsDownHandler().getData(bucketId, startDate, endDate);
			writeRequests = hourWriteRequestsRecorder.getHitsDownHandler().getData(bucketId, startDate, endDate);
		} else {
			readRequests = dayReadRequestsRecorder.getHitsDownHandler().getData(bucketId, startDate, endDate);
			writeRequests = dayWriteRequestsRecorder.getHitsDownHandler().getData(bucketId, startDate, endDate);
		}
		Map<String, TimeHits> readMap = readRequests.stream().collect(Collectors.toMap(TimeHits::getTime, Function.identity()));
		Map<String, TimeHits> writeMap = writeRequests.stream().collect(Collectors.toMap(TimeHits::getTime, Function.identity()));
		
		List<ChartRequestData> results = new ArrayList<>();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDate);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		SimpleDateFormat dateFormat = new SimpleDateFormat(by == ChartBy.hour ? "yyyyMMddHH" : "yyyyMMdd");
		SimpleDateFormat displayDateFormat = new SimpleDateFormat(by == ChartBy.hour ? "yyyy-MM-dd HH:00" : "yyyy-MM-dd");
		do {
			Date calendarTime = calendar.getTime();
			String time = dateFormat.format(calendarTime);
			String displayTime = displayDateFormat.format(calendarTime);
			ChartRequestData data = new ChartRequestData();
			data.setTime(displayTime);
			data.setReadRequests(readMap.getOrDefault(time, new TimeHits(time, 0L, null)).getHits());
			data.setWriteRequests(writeMap.getOrDefault(time, new TimeHits(time, 0L, null)).getHits());
			results.add(data);
			if (by == ChartBy.hour) {
				calendar.add(Calendar.HOUR_OF_DAY, 1);
			} else {
				calendar.add(Calendar.DAY_OF_YEAR, 1);
			}
		} while (calendar.getTime().getTime() < endDate.getTime());
		return results;
	}
	
	public List<ChartFlowData> findChartBytesList(@NotNull Long bucketId, @NotNull Date startDate, @Nullable Date endDate, @NotNull ChartBy by) {
		if (endDate == null) {
			endDate = new Date();
		}
		List<TimeHits> readRequests;
		List<TimeHits> writeRequests;
		if (by == ChartBy.hour) {
			readRequests = hourReadBytesRecorder.getHitsDownHandler().getData(bucketId, startDate, endDate);
			writeRequests = hourWriteBytesRecorder.getHitsDownHandler().getData(bucketId, startDate, endDate);
		} else {
			readRequests = dayReadBytesRecorder.getHitsDownHandler().getData(bucketId, startDate, endDate);
			writeRequests = dayWriteBytesRecorder.getHitsDownHandler().getData(bucketId, startDate, endDate);
		}
		Map<String, TimeHits> readMap = readRequests.stream().collect(Collectors.toMap(TimeHits::getTime, Function.identity()));
		Map<String, TimeHits> writeMap = writeRequests.stream().collect(Collectors.toMap(TimeHits::getTime, Function.identity()));
		
		List<ChartFlowData> results = new ArrayList<>();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDate);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		SimpleDateFormat dateFormat = new SimpleDateFormat(by == ChartBy.hour ? "yyyyMMddHH" : "yyyyMMdd");
		SimpleDateFormat displayDateFormat = new SimpleDateFormat(by == ChartBy.hour ? "yyyy-MM-dd HH:00" : "yyyy-MM-dd");
		do {
			Date calendarTime = calendar.getTime();
			String time = dateFormat.format(calendarTime);
			String displayTime = displayDateFormat.format(calendarTime);
			ChartFlowData data = new ChartFlowData();
			data.setTime(displayTime);
			Long readBytes = readMap.getOrDefault(time, new TimeHits(time, 0L, null)).getHits();
			Long writeBytes = writeMap.getOrDefault(time, new TimeHits(time, 0L, null)).getHits();
			data.setReadMb(toMB(readBytes));
			data.setWriteMb(toMB(writeBytes));
			results.add(data);
			if (by == ChartBy.hour) {
				calendar.add(Calendar.HOUR_OF_DAY, 1);
			} else {
				calendar.add(Calendar.DAY_OF_YEAR, 1);
			}
		} while (calendar.getTime().getTime() < endDate.getTime());
		return results;
	}
	
	private double toMB(long bytes) {
		return BigDecimal.valueOf(bytes).divide(BigDecimal.valueOf(FileUtils.ONE_MB), 2, RoundingMode.HALF_UP).doubleValue();
	}
	
	public List<ChartFlowData> find24HoursFlowList(Long bucketId) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, -1);
		return findChartBytesList(bucketId, calendar.getTime(), null, ChartBy.hour);
	}
	
	public List<ChartFlowData> find30DaysFlowList(Long bucketId) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, -30);
		return findChartBytesList(bucketId, calendar.getTime(), null, ChartBy.day);
	}
	
	
	public List<ChartRequestData> find24HoursRequestList(Long bucketId) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, -1);
		return findChartRequestList(bucketId, calendar.getTime(), new Date(), ChartBy.hour);
	}
	
	
	public List<ChartRequestData> find30DaysRequestList(Long bucketId) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, -30);
		return findChartRequestList(bucketId, calendar.getTime(), null, ChartBy.day);
	}
	
	@SneakyThrows
	public StatisticInfo findStatisticInfo(Long bucketId) {
		LocalDate now = LocalDate.now();
		String thisDay = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String thisMonth = now.format(DateTimeFormatter.ofPattern("yyyyMM01"));
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		Date today = dateFormat.parse(thisDay);
		Date thisMonthDate = dateFormat.parse(thisMonth);
		
		StatisticInfo statisticInfo = new StatisticInfo();
		statisticInfo.setThisDayReadBytes(dayReadBytesRecorder.getHitsDownHandler().getTotal(bucketId, today, null));
		statisticInfo.setThisDayReadRequests(dayReadRequestsRecorder.getHitsDownHandler().getTotal(bucketId, today, null));
		statisticInfo.setThisDayWriteBytes(dayWriteBytesRecorder.getHitsDownHandler().getTotal(bucketId, today, null));
		statisticInfo.setThisDayWriteRequests(dayWriteRequestsRecorder.getHitsDownHandler().getTotal(bucketId, today, null));
		
		statisticInfo.setThisMonthReadBytes(dayReadBytesRecorder.getHitsDownHandler().getTotal(bucketId, thisMonthDate, null));
		statisticInfo.setThisMonthReadRequests(dayReadRequestsRecorder.getHitsDownHandler().getTotal(bucketId, thisMonthDate, null));
		statisticInfo.setThisMonthWriteBytes(dayWriteBytesRecorder.getHitsDownHandler().getTotal(bucketId, thisMonthDate, null));
		statisticInfo.setThisMonthWriteRequests(dayWriteRequestsRecorder.getHitsDownHandler().getTotal(bucketId, thisMonthDate, null));
		return statisticInfo;
	}

//	@Autowired
//	@Lazy
//	private AuditArchiveService auditArchiveService;
//
//	@Transactional(rollbackFor = Exception.class)
//	public void archive(List<Audit> audits) {
//		if (CollectionUtils.isEmpty(audits)) {
//			return;
//		}
//		for (Audit audit : audits) {
//			log.info("归档audit log {}中...", audit.getId());
//			AuditArchive auditArchive = new AuditArchive();
//			BeanUtils.copyProperties(audit, auditArchive);
//			auditArchiveService.save(auditArchive);
//			deleteById(audit);
//		}
//	}
//
//	class SaveAuditTask implements Callable<Audit> {
//		public SaveAuditTask(Audit audit) {
//			this.audit = audit;
//		}
//
//		private final Audit audit;
//
//		@Override
//		public Audit call() {
//			save(audit);
//			return audit;
//		}
//	}
//
//	class UpdateAuditTask implements Runnable {
//		public UpdateAuditTask(Audit audit) {
//			this.audit = audit;
//		}
//
//		private final Audit audit;
//
//		@Override
//		public void run() {
//			updateById(audit);
//		}
//	}
//
//	public void doAudit(MosContext mosContext, Audit.Type type, Audit.Action action, String remark) {
//		if (mosContext == null) {
//			return;
//		}
//		Audit audit = createAudit(mosContext.getBucketId(), mosContext.getCurrentUserId(), mosContext.getOpenId(), mosContext.getContent(), type, action, 0, remark);
//		executorService.submit(new SaveAuditTask(audit));
//	}
//
//	public void doAudit(MosContext mosContext, Audit.Type type, Audit.Action action) {
//		if (mosContext == null) {
//			return;
//		}
//		Audit audit = createAudit(mosContext.getBucketId(), mosContext.getCurrentUserId(), mosContext.getOpenId(), mosContext.getContent(), type, action, 0, null);
//		executorService.submit(new SaveAuditTask(audit));
//	}
//
//	public void doAudit(MosContext mosContext, Audit.Type type, Audit.Action action, String remark, long bytes) {
//		if (mosContext == null) {
//			return;
//		}
//		Audit audit = createAudit(mosContext.getBucketId(), mosContext.getCurrentUserId(), mosContext.getOpenId(), mosContext.getContent(), type, action, bytes, remark);
//		executorService.submit(new SaveAuditTask(audit));
//	}
//
//	public void doAudit(Long bucketId, EncryptContent target, Audit.Type type, Audit.Action action, String remark, long bytes) {
//		Audit audit = createAudit(bucketId, target, type, action, bytes, remark);
//		executorService.submit(new SaveAuditTask(audit));
//	}
//
//	public void doAudit(Long bucketId, EncryptContent target, Audit.Type type, Audit.Action action) {
//		Audit audit = createAudit(bucketId, target, type, action, 0, null);
//		executorService.submit(new SaveAuditTask(audit));
//	}
//
//	public Audit startAudit(MosContext mosContext, Audit.Type type, Audit.Action action, String remark) {
//		if (mosContext == null) {
//			return null;
//		}
//		Audit audit = createAudit(mosContext.getBucketId(), mosContext.getContent(), type, action, 0, remark);
//		save(audit);
//		return audit;
//	}
//
//	private Audit createAudit(Long bucketId, EncryptContent content, Audit.Type type, Audit.Action action, long bytes, String remark) {
//		MosContext context = MosContext.getContext();
//		return createAudit(bucketId, mosUserContext.getCurrentUserId(), context != null ? context.getOpenId() : null, content, type, action, bytes, remark);
//	}
//
//	private Audit createAudit(Long bucketId, Long userId, Long openId, EncryptContent content, Audit.Type type, Audit.Action action, long bytes, String remark) {
//		Audit audit = new Audit();
//		audit.setBucketId(bucketId);
//		audit.setUserId(userId);
//		audit.setOpenId(openId);
//		if (content != null) {
//			audit.setTarget(content.toString());
//		}
//		audit.setType(type);
//		audit.setBytes(bytes);
//		audit.setAction(action);
//		audit.setRemark(remark);
//		audit.setCreatedBy(mosUserContext.getCurrentUserName());
//		audit.setUpdatedBy(mosUserContext.getCurrentUserName());
//		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//		if (requestAttributes != null) {
//			HttpServletRequest request = requestAttributes.getRequest();
//			audit.setIp(getIpAddr(request));
//		}
//		return audit;
//	}
//
//	public static String getIpAddr(HttpServletRequest request) {
//		String ip = request.getHeader("x-forwarded-for");
//		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//			ip = request.getHeader("Proxy-Client-IP");
//		}
//		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//			ip = request.getHeader("WL-Proxy-Client-IP");
//		}
//		if (ip == null || ip.length() == 0 || "X-Real-IP".equalsIgnoreCase(ip)) {
//			ip = request.getHeader("X-Real-IP");
//		}
//		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
//			ip = request.getRemoteAddr();
//		}
//		if (StringUtils.isNotBlank(ip) && ip.contains(",")) {
//			return ip.split(",")[0];
//		}
//		return ip;
//	}
	
	public OutputStream createReadAuditStream(OutputStream outputStream, long bucketId) {
		return new ReadableOutputStream(outputStream, new ReadableOutputStream.ReadEvent() {
			@Override
			public void onFlush(long readed) {
				readBytesRecord(bucketId, readed);
			}
			
			@Override
			public void onClose(long readed) {
				readBytesRecord(bucketId, readed);
			}
			
			@Override
			public void onException(long readed, IOException e) {
				readBytesRecord(bucketId, readed);
			}
		});
	}
	
	public void readBytesRecord(long bucketId, long bytes) {
		String hourKey = new SimpleDateFormat("yyyyMMddHH").format(new Date());
		String dayKey = new SimpleDateFormat("yyyyMMdd").format(new Date());
		hourReadBytesRecorder.recordHits(bucketId, hourKey, bytes);
		dayReadBytesRecorder.recordHits(bucketId, dayKey, bytes);
	}
	
	public void writeBytesRecord(long bucketId, long bytes) {
		String hourKey = new SimpleDateFormat("yyyyMMddHH").format(new Date());
		String dayKey = new SimpleDateFormat("yyyyMMdd").format(new Date());
		hourWriteBytesRecorder.recordHits(bucketId, hourKey, bytes);
		dayWriteBytesRecorder.recordHits(bucketId, dayKey, bytes);
	}
	
	public void readRequestsRecord(long bucketId, long hits) {
		String hourKey = new SimpleDateFormat("yyyyMMddHH").format(new Date());
		String dayKey = new SimpleDateFormat("yyyyMMdd").format(new Date());
		hourReadRequestsRecorder.recordHits(bucketId, hourKey, hits);
		dayReadRequestsRecorder.recordHits(bucketId, dayKey, hits);
	}
	
	public void writeRequestsRecord(long bucketId, long hits) {
		String hourKey = new SimpleDateFormat("yyyyMMddHH").format(new Date());
		String dayKey = new SimpleDateFormat("yyyyMMdd").format(new Date());
		hourWriteRequestsRecorder.recordHits(bucketId, hourKey, hits);
		dayWriteRequestsRecorder.recordHits(bucketId, dayKey, hits);
	}
	
}
