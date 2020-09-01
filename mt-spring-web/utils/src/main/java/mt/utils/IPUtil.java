package mt.utils;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * IP工具类
 * @author xiezz
 * @version 1.0.0
 * @date 2015-08-06
 */
public class IPUtil {

	/**
	 * 获取本机网卡IP地址
	 * @return
	 */
	public static String getLocalIp() {
		try {
			Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
			InetAddress ip = null;
			while (allNetInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
				Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					ip = (InetAddress) addresses.nextElement();
					if (ip != null && ip instanceof Inet4Address) {
						String s = ip.getHostAddress();
						if ("127.0.0.1".equals(s)) continue;
						return ip.getHostAddress();
					}
				}
			}
			return "127.0.0.1";
		} catch (SocketException e) {
			return "127.0.0.1";
		}
	}
}
