package mt.spring.mos.server.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mt.common.hits.HitsRecorder;
import mt.spring.mos.server.config.hits.MosHitsRecorder;
import mt.spring.mos.server.config.hits.TimeHits;
import mt.spring.mos.server.controller.ReadableOutputStream;
import mt.spring.mos.server.entity.vo.audit.ChartBy;
import mt.spring.mos.server.entity.vo.audit.ChartFlowData;
import mt.spring.mos.server.entity.vo.audit.ChartRequestData;
import mt.spring.mos.server.entity.vo.audit.StatisticInfo;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author Martin
 * @Date 2020/12/19
 */
@Service
@Slf4j
public class AuditService {
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
		resourceVisitsRecorder.recordHits("default", resourceId, hits);
	}
	
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
