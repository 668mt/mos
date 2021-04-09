//package mt.spring.mos.server.service.cron;
//
//import mt.common.service.DataLockService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
///**
// * @Author Martin
// * @Date 2021/4/9
// */
//@Service
//public class DistributeJobService {
//	@Autowired
//	private DataLockService dataLockService;
//	@Autowired
//	private JdbcTemplate jdbcTemplate;
//
//	@Transactional(rollbackFor = Exception.class)
//	public void executeDistributeJob(String lockKey, Job job) {
//		dataLockService.lock(lockKey, jdbcTemplate);
//		job.execute();
//	}
//
//	public void initLock(String lockKey) {
//		dataLockService.initLock(lockKey, jdbcTemplate);
//	}
//
//	public interface Job {
//		void execute();
//	}
//}
