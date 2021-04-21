package mt.spring.mos.base.utils;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * @Author Martin
 * @Date 2020/6/9
 */
public class IpUtils {
	public static String getHostIp(@Nullable String prefix) {
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
						String hostAddress = ip.getHostAddress();
						if (StringUtils.isBlank(prefix) || hostAddress.startsWith(prefix)) {
							return hostAddress;
						}
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		throw new IllegalStateException("获取主机ip失败");
	}
}
