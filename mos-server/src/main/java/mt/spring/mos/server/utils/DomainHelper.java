//package mt.spring.mos.server.utils;
//
//import mt.spring.mos.server.entity.MosServerProperties;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import javax.servlet.http.HttpServletRequest;
//
///**
// * @Author Martin
// * @Date 2021/9/30
// */
//@Component
//public class DomainHelper {
//	@Autowired
//	private MosServerProperties mosServerProperties;
//
//	public String getDomain(HttpServletRequest request) {
//		if (StringUtils.isNotBlank(mosServerProperties.getDomain())) {
//			return mosServerProperties.getDomain();
//		}
//		if (request == null) {
//			return null;
//		}
//		String s = request.getRequestURL().toString();
//		int i1 = s.indexOf("/", 8);
//		return s.substring(0, i1);
//	}
//}
