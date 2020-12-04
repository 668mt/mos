package mt.spring.mos.base.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * @Author Martin
 * @Date 2020/6/9
 */
public class IpUtils {
	public static String getHostIp() {
		try {
			Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
			while (allNetInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = allNetInterfaces.nextElement();
				Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress ip = addresses.nextElement();
					if (ip instanceof Inet4Address
							&& !ip.isLoopbackAddress() //loopback地址即本机地址，IPv4的loopback范围是127.0.0.0 ~ 127.255.255.255
							&& !ip.getHostAddress().contains(":")) {
						return ip.getHostAddress();
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return "127.0.0.1";
	}
}
