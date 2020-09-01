//package mt.common.utils;
//
//import com.github.pagehelper.PageHelper;
//import mt.common.entity.PageBean;
//import org.apache.commons.lang3.StringUtils;
//
//import java.util.List;
//
///**
// * @Author LIMAOTAO236
// * @Date 2019-1-7
// */
//public class PageUtils {
//	public interface GetList<T> {
//		List<T> getList();
//	}
//
//	public static <T> PageBean<T> doPage(PageBean<T> pageBean, String defaultOrderBy, GetList<T> getList) {
//		defaultOrderBy = StringUtils.isBlank(defaultOrderBy) ? "createdDate desc" : defaultOrderBy;
//		int pageNum = 0;
//		int pageSize = 0;
//		String orderBy = null;
//		if (pageBean != null && pageBean.getPageNum() > 0 && pageBean.getPageSize() > 0) {
//			pageNum = pageBean.getPageNum();
//			pageSize = pageBean.getPageSize();
//			orderBy = pageBean.getOrderBy();
//			orderBy = StringUtils.isBlank(orderBy) ? defaultOrderBy : orderBy;
//			PageHelper.startPage(pageNum, pageSize, orderBy);
//		}
//		List<T> rows = getList.getList();
//		return new PageBean<>(rows, pageNum, pageSize, orderBy);
//	}
//
//	public static <T> PageBean<T> doPage(PageBean<T> pageBean, GetList<T> getList) {
//		return doPage(pageBean, null, getList);
//	}
//}
