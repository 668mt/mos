package mt.spring.mos.server.service;

import lombok.extern.slf4j.Slf4j;
import mt.common.mybatis.mapper.BaseMapper;
import mt.common.service.BaseServiceImpl;
import mt.spring.mos.base.utils.SizeUtils;
import mt.spring.mos.server.config.MosUserContext;
import mt.spring.mos.server.config.aop.MosContext;
import mt.spring.mos.server.controller.ReadableOutputStream;
import mt.spring.mos.server.dao.AuditMapper;
import mt.spring.mos.server.entity.po.Audit;
import mt.spring.mos.server.entity.po.Bucket;
import mt.spring.mos.server.entity.vo.BucketVo;
import mt.spring.mos.server.entity.vo.audit.*;
import mt.utils.DateUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static mt.spring.mos.base.utils.IOUtils.MB;

/**
 * @Author Martin
 * @Date 2020/12/19
 */
@Service
@Slf4j
public class AuditService extends BaseServiceImpl<Audit> {
	@Autowired
	private AuditMapper auditMapper;
	@Autowired
	@Lazy
	private BucketService bucketService;
	
	@Override
	public BaseMapper<Audit> getBaseMapper() {
		return auditMapper;
	}
	
	@Autowired
	private MosUserContext mosUserContext;
	@Autowired
	@Lazy
	private ResourceService resourceService;
	private final ExecutorService executorService = Executors.newFixedThreadPool(5);
	
	public void auditResourceVisits(Long resourceId) {
		executorService.submit(() -> resourceService.addVisits(resourceId));
	}
	
	public List<FlowStatisticVo> findFlowStatisticFrom(Long userId, Audit.Type type, String startDate) {
		List<BucketVo> bucketList = bucketService.findBucketList(userId);
		if (CollectionUtils.isEmpty(bucketList)) {
			return null;
		}
		return bucketList.stream().map(bucketVo -> {
			FlowStatisticVo flowStatisticVo = new FlowStatisticVo();
			flowStatisticVo.setBucketName(bucketVo.getBucketName());
			flowStatisticVo.setStartDate(startDate);
			flowStatisticVo.setType(type);
			long bytes = auditMapper.findTotalFlowFromDate(bucketVo.getId(), type, startDate);
			flowStatisticVo.setReadableFlow(SizeUtils.getReadableSize(bytes));
			return flowStatisticVo;
		}).collect(Collectors.toList());
	}
	
	public List<RequestStatisticVo> findRequestStatisticFrom(Long userId, Audit.Type type, String startDate) {
		List<BucketVo> bucketList = bucketService.findBucketList(userId);
		if (CollectionUtils.isEmpty(bucketList)) {
			return null;
		}
		return bucketList.stream().map(bucketVo -> {
			RequestStatisticVo requestStatisticVo = new RequestStatisticVo();
			requestStatisticVo.setBucketName(bucketVo.getBucketName());
			requestStatisticVo.setStartDate(startDate);
			requestStatisticVo.setType(type);
			long requests = auditMapper.findTotalRequestFromDate(bucketVo.getId(), type, startDate);
			requestStatisticVo.setRequests(requests);
			return requestStatisticVo;
		}).collect(Collectors.toList());
	}
	
	public List<String> createXList(ChartBy by, String startDate, String endDate) {
		try {
			Date start = DateUtils.parse(startDate);
			Date end = DateUtils.parse(endDate);
			List<String> list = new ArrayList<>();
			SimpleDateFormat dateFormat;
			Calendar startCalendar = Calendar.getInstance();
			startCalendar.setTime(start);
			long endTime = end.getTime();
			switch (by) {
				case hour:
					startCalendar.set(Calendar.MINUTE, 0);
					dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:00");
					while (startCalendar.getTime().getTime() <= endTime) {
						list.add(dateFormat.format(startCalendar.getTime()));
						startCalendar.add(Calendar.HOUR, 1);
					}
					break;
				case day:
					startCalendar.set(Calendar.MINUTE, 0);
					startCalendar.set(Calendar.HOUR_OF_DAY, 0);
					dateFormat = new SimpleDateFormat("yyyy-MM-dd");
					while (startCalendar.getTime().getTime() <= endTime) {
						list.add(dateFormat.format(startCalendar.getTime()));
						startCalendar.add(Calendar.DAY_OF_MONTH, 1);
					}
					break;
				case month:
					startCalendar.set(Calendar.MINUTE, 0);
					startCalendar.set(Calendar.HOUR_OF_DAY, 0);
					startCalendar.set(Calendar.DAY_OF_MONTH, 1);
					dateFormat = new SimpleDateFormat("yyyy-MM");
					while (startCalendar.getTime().getTime() <= endTime) {
						list.add(dateFormat.format(startCalendar.getTime()));
						startCalendar.add(Calendar.MONTH, 1);
					}
					break;
				case year:
					startCalendar.set(Calendar.MINUTE, 0);
					startCalendar.set(Calendar.HOUR_OF_DAY, 0);
					startCalendar.set(Calendar.DAY_OF_MONTH, 1);
					startCalendar.set(Calendar.MONTH, 0);
					dateFormat = new SimpleDateFormat("yyyy");
					while (startCalendar.getTime().getTime() <= endTime) {
						list.add(dateFormat.format(startCalendar.getTime()));
						startCalendar.add(Calendar.YEAR, 1);
					}
					break;
			}
			return list;
		} catch (ParseException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}
	
	private ChartVo findChartVoByTime(List<ChartVo> list, String time) {
		return list.stream().filter(f -> f.getX().equals(time)).findFirst().orElse(new ChartVo(time, BigDecimal.ZERO));
	}
	
	public List<ChartFlowData> findChartFlowList(Bucket bucket, String startDate, @Nullable String endDate, ChartBy by) {
		if (endDate == null) {
			endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		}
		List<ChartVo> readBytes = auditMapper.findChartFlowDataList(bucket.getId(), startDate, endDate, Audit.Type.READ, by);
		List<ChartVo> writeBytes = auditMapper.findChartFlowDataList(bucket.getId(), startDate, endDate, Audit.Type.WRITE, by);
		List<String> list = createXList(by, startDate, endDate);
		return list.stream().map(time -> {
			ChartFlowData chartFlowData = new ChartFlowData();
			chartFlowData.setTime(time);
			double readMb = findChartVoByTime(readBytes, time).getY().divide(BigDecimal.valueOf(MB), 1, RoundingMode.HALF_UP).doubleValue();
			chartFlowData.setReadMb(readMb);
			double writeMb = findChartVoByTime(writeBytes, time).getY().divide(BigDecimal.valueOf(MB), 1, RoundingMode.HALF_UP).doubleValue();
			chartFlowData.setWriteMb(writeMb);
			return chartFlowData;
		}).collect(Collectors.toList());
	}
	
	public List<ChartRequestData> findChartRequestList(Bucket bucket, String startDate, @Nullable String endDate, ChartBy by) {
		if (endDate == null) {
			endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		}
		List<ChartVo> readRequests = auditMapper.findChartRequestDataList(bucket.getId(), startDate, endDate, Audit.Type.READ, by);
		List<ChartVo> writeRequests = auditMapper.findChartRequestDataList(bucket.getId(), startDate, endDate, Audit.Type.WRITE, by);
		List<String> list = createXList(by, startDate, endDate);
		return list.stream().map(time -> {
			ChartRequestData data = new ChartRequestData();
			data.setTime(time);
			data.setReadRequests(findChartVoByTime(readRequests, time).getY().longValue());
			data.setWriteRequests(findChartVoByTime(writeRequests, time).getY().longValue());
			return data;
		}).collect(Collectors.toList());
	}
	
	public List<StatisticInfo> findStatisticInfo(Long userId) {
		List<BucketVo> bucketList = bucketService.findBucketList(userId);
		LocalDate now = LocalDate.now();
		String thisDay = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String thisMonth = now.format(DateTimeFormatter.ofPattern("yyyy-MM-01"));
		return bucketList.stream().map(bucketVo -> {
			StatisticInfo statisticInfo = new StatisticInfo();
			statisticInfo.setBucket(bucketVo);
			
			Long bucketId = bucketVo.getId();
			statisticInfo.setThisDayReadBytes(auditMapper.findTotalFlowFromDate(bucketId, Audit.Type.READ, thisDay));
			statisticInfo.setThisDayReadRequests(auditMapper.findTotalRequestFromDate(bucketId, Audit.Type.READ, thisDay));
			statisticInfo.setThisDayWriteBytes(auditMapper.findTotalFlowFromDate(bucketId, Audit.Type.WRITE, thisDay));
			statisticInfo.setThisDayWriteRequests(auditMapper.findTotalRequestFromDate(bucketId, Audit.Type.WRITE, thisDay));
			
			statisticInfo.setThisMonthReadBytes(auditMapper.findTotalFlowFromDate(bucketId, Audit.Type.READ, thisMonth));
			statisticInfo.setThisMonthReadRequests(auditMapper.findTotalRequestFromDate(bucketId, Audit.Type.READ, thisMonth));
			statisticInfo.setThisMonthWriteBytes(auditMapper.findTotalFlowFromDate(bucketId, Audit.Type.WRITE, thisMonth));
			statisticInfo.setThisMonthWriteRequests(auditMapper.findTotalRequestFromDate(bucketId, Audit.Type.WRITE, thisMonth));
			return statisticInfo;
		}).collect(Collectors.toList());
	}
	
	class SaveAuditTask implements Callable<Audit> {
		public SaveAuditTask(Audit audit) {
			this.audit = audit;
		}
		
		private final Audit audit;
		
		@Override
		public Audit call() {
			save(audit);
			return audit;
		}
	}
	
	class UpdateAuditTask implements Runnable {
		public UpdateAuditTask(Audit audit) {
			this.audit = audit;
		}
		
		private final Audit audit;
		
		@Override
		public void run() {
			updateById(audit);
		}
	}
	
	public void doAudit(MosContext mosContext, Audit.Type type, Audit.Action action, String remark) {
		Audit audit = createAudit(mosContext.getBucketId(), mosContext.getCurrentUserId(), mosContext.getOpenId(), mosContext.getPathname(), type, action, 0, remark);
		executorService.submit(new SaveAuditTask(audit));
	}
	
	public void doAudit(MosContext mosContext, Audit.Type type, Audit.Action action) {
		Audit audit = createAudit(mosContext.getBucketId(), mosContext.getCurrentUserId(), mosContext.getOpenId(), mosContext.getPathname(), type, action, 0, null);
		executorService.submit(new SaveAuditTask(audit));
	}
	
	public void doAudit(MosContext mosContext, Audit.Type type, Audit.Action action, String remark, long bytes) {
		Audit audit = createAudit(mosContext.getBucketId(), mosContext.getCurrentUserId(), mosContext.getOpenId(), mosContext.getPathname(), type, action, bytes, remark);
		executorService.submit(new SaveAuditTask(audit));
	}
	
	public void doAudit(Long bucketId, String target, Audit.Type type, Audit.Action action, String remark, long bytes) {
		Audit audit = createAudit(bucketId, target, type, action, bytes, remark);
		executorService.submit(new SaveAuditTask(audit));
	}
	
	public void doAudit(Long bucketId, String target, Audit.Type type, Audit.Action action) {
		Audit audit = createAudit(bucketId, target, type, action, 0, null);
		executorService.submit(new SaveAuditTask(audit));
	}
	
	public Audit startAudit(MosContext mosContext, Audit.Type type, Audit.Action action, String remark) {
		Audit audit = createAudit(mosContext.getBucketId(), mosContext.getPathname(), type, action, 0, remark);
		save(audit);
		return audit;
	}
	
	private Audit createAudit(Long bucketId, String target, Audit.Type type, Audit.Action action, long bytes, String remark) {
		MosContext context = MosContext.getContext();
		return createAudit(bucketId, mosUserContext.getCurrentUserId(), context != null ? context.getOpenId() : null, target, type, action, bytes, remark);
	}
	
	private Audit createAudit(Long bucketId, Long userId, Long openId, String target, Audit.Type type, Audit.Action action, long bytes, String remark) {
		Audit audit = new Audit();
		audit.setBucketId(bucketId);
		audit.setUserId(userId);
		audit.setOpenId(openId);
		audit.setTarget(target);
		audit.setType(type);
		audit.setBytes(bytes);
		audit.setAction(action);
		audit.setRemark(remark);
		audit.setCreatedBy(mosUserContext.getCurrentUserName());
		audit.setUpdatedBy(mosUserContext.getCurrentUserName());
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (requestAttributes != null) {
			HttpServletRequest request = requestAttributes.getRequest();
			audit.setIp(getIpAddr(request));
		}
		return audit;
	}
	
	public static String getIpAddr(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "X-Real-IP".equalsIgnoreCase(ip)) {
			ip = request.getHeader("X-Real-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		if (StringUtils.isNotBlank(ip) && ip.contains(",")) {
			return ip.split(",")[0];
		}
		return ip;
	}
	
	public OutputStream createAuditStream(OutputStream outputStream, Audit audit) {
		return new ReadableOutputStream(outputStream, new ReadableOutputStream.ReadEvent() {
			@Override
			public void onFlush(long readed) {
				System.out.println("onFlush读取：" + SizeUtils.getReadableSize(readed));
				endAudit(audit, readed);
			}
			
			@Override
			public void onClose(long readed) {
				System.out.println("onClose读取：" + SizeUtils.getReadableSize(readed));
				endAudit(audit, readed);
			}
			
			@Override
			public void onException(long readed, IOException e) {
				System.out.println("onException读取：" + SizeUtils.getReadableSize(readed));
				endAudit(audit, readed);
			}
		});
	}
	
	public void endAudit(Audit audit, long bytes) {
		audit.setBytes(bytes);
		executorService.submit(new UpdateAuditTask(audit));
	}
	
}
