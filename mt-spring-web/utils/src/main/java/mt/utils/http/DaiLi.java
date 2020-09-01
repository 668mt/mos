package mt.utils.http;

import java.util.Date;

public class DaiLi {

	/**
	 * 创建时间
	 */
	private Date createdDate;
	/**
	 * ip
	 */
	private String ip;
	/**
	 * 端口
	 */
	private Integer port;
	/**
	 * 区域
	 */
	private String area;
	/**
	 * 是否匿名
	 */
	private String isHidden;
	/**
	 * 类型http/socket/https
	 */
	private String type;
	/**
	 * 速度
	 */
	private Double speed;
	/**
	 * 连接时间
	 */
	private Double connectTime;
	/**
	 * 存活时间
	 */
	private String survive;
	/**
	 * 验证时间
	 */
	private String verifyTime;
	/**
	 * 成功次数
	 */
	private int success;
	/**
	 * 失败次数
	 */
	private int error;

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getArea() {
		return area;
	}

	public void setArea(String area) {
		this.area = area;
	}

	public String getIsHidden() {
		return isHidden;
	}

	public void setIsHidden(String isHidden) {
		this.isHidden = isHidden;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Double getSpeed() {
		return speed;
	}

	public void setSpeed(Double speed) {
		this.speed = speed;
	}

	public Double getConnectTime() {
		return connectTime == null ? 0 : connectTime;
	}

	public void setConnectTime(Double connectTime) {
		this.connectTime = connectTime;
	}

	public String getSurvive() {
		return survive;
	}

	public void setSurvive(String survive) {
		this.survive = survive;
	}

	public String getVerifyTime() {
		return verifyTime;
	}

	public void setVerifyTime(String verifyTime) {
		this.verifyTime = verifyTime;
	}

	public int getSuccess() {
		return success;
	}

	public void setSuccess(int success) {
		this.success = success;
	}

	public int getError() {
		return error;
	}

	public void setError(int error) {
		this.error = error;
	}

	public DaiLi(String ip, Integer port, String area,
				 String isHidden, String type, Double speed, Double connectTime,
				 String survive, String verifyTime) {
		super();
		this.createdDate = new Date();
		this.ip = ip;
		this.port = port;
		this.area = area;
		this.isHidden = isHidden;
		this.type = type;
		this.speed = speed;
		this.connectTime = connectTime;
		this.survive = survive;
		this.verifyTime = verifyTime;
		this.success = 0;
		this.error = 0;
	}

	public DaiLi() {
		super();
		this.createdDate = new Date();
		this.success = 0;
		this.error = 0;
	}

	public void addError() {
		error++;
	}

	public void addSuccess() {
		success++;
	}


}
