package mt.common.entity;

import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * easyui前台交互数据模型
* @ClassName: PageBean
* @Description: 
* @author Martin
* @date 2017-11-6 下午10:08:03
*
* @param <T>
 */
public class PageBean<T> extends PageInfo<T>{
	private static final long serialVersionUID = 8262737937098398068L;
	/**
	 * 数据结合
	 */
	private List<T> rows = new ArrayList<T>();
	/**
	 * 脚部数据
	 */
	private List<T> footer;
	
	/**
	 * 排序字段
	 */
	private String sort;
	/**
	 * 排序顺序
	 */
	private String order;
	
	private int page;
	private int pageNum;
	private int pageSize;
	private boolean first = false;
	

	/**
	 * 组合成排序sql语句
	 * @return
	 */
	@Override
	public String getOrderBy(){
		if(StringUtils.isNotBlank(super.getOrderBy())){
			return super.getOrderBy();
		}
		if(StringUtils.isNotBlank(sort) && StringUtils.isNotBlank(order)){
			String orderBy = "";
			String[] sorts = sort.split(",");
			String[] orders = order.split(",");
			for (int i=0;i<sorts.length;i++) {
				if(i == sorts.length - 1){
					orderBy += sorts[i] + " " + orders[i];
				}else{
					orderBy += sorts[i] + " " + orders[i] + ",";
				}
			}
			return orderBy;
		}
		return null;
	}
	
	
	public boolean isFirst() {
		return first;
	}

	public void setFirst(boolean first) {
		this.first = first;
	}

	public int getPage() {
		return page;
	}
	public void setPage(int page) {
		this.page = page;
		this.pageNum = page;
	}
	
	public int getPageNum() {
		return pageNum;
	}

	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
		this.page = pageNum;
	}


	public int getPageSize() {
		return pageSize;
	}


	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}


	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public List<T> getRows() {
		return rows;
	}

	public void setRows(List<T> rows) {
		this.rows = rows;
		setList(rows);
	}

	public List<T> getFooter() {
		return footer;
	}

	public void setFooter(List<T> footer) {
		this.footer = footer;
	}

	public PageBean() {
		super();
	}
	public PageBean(List<T> rows, int pageNum, int pageSize, String orderBy) {
		super(rows);
		this.rows = getList();
		setPageNum(pageNum);
		setPageSize(pageSize);
		setOrderBy(orderBy);
	}
	public PageBean(List<T> rows, int pageNum, int pageSize) {
		super(rows);
		this.rows = getList();
		setPageNum(pageNum);
		setPageSize(pageSize);
	}
	public PageBean(List<T> rows) {
		super(rows);
		this.rows = getList();
		
	}
	public PageBean(List<T> rows, boolean isMessage) {
		super(rows);
		this.rows = rows;
	}

	public PageBean(int pageNum, int pageSize) {
		super();
		setPageNum(pageNum);
		setPageSize(pageSize);
	}
	
}
