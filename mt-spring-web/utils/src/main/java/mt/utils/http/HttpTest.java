package mt.utils.http;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.net.URL;

/**
 * @Author Martin
 * @Date 2018/8/23
 */
public class HttpTest {
	private static void trustAllHttpsCertificates() throws Exception {
		javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
		javax.net.ssl.TrustManager tm = new miTM();
		trustAllCerts[0] = tm;
		javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext
				.getInstance("SSL");
		sc.init(null, trustAllCerts, null);
		javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc
				.getSocketFactory());
	}
	static class miTM implements javax.net.ssl.TrustManager,
			javax.net.ssl.X509TrustManager {
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}
		
		public boolean isServerTrusted(
				java.security.cert.X509Certificate[] certs) {
			return true;
		}
		
		public boolean isClientTrusted(
				java.security.cert.X509Certificate[] certs) {
			return true;
		}
		
		public void checkServerTrusted(
				java.security.cert.X509Certificate[] certs, String authType)
				throws java.security.cert.CertificateException {
			return;
		}
		
		public void checkClientTrusted(
				java.security.cert.X509Certificate[] certs, String authType)
				throws java.security.cert.CertificateException {
			return;
		}
	}
	
	public static void main(String[] args) {
		String url = "https://www.shipmentlink.com/servlet/TDB1_CargoTracking.do";
		try {
			trustAllHttpsCertificates();
			HostnameVerifier hv = new HostnameVerifier() {
				public boolean verify(String urlHostName, SSLSession session) {
					return true;
				}
			};
			HttpsURLConnection.setDefaultHostnameVerifier(hv);
			
			URL url2 = new URL(url);
			HttpsURLConnection connection = (HttpsURLConnection) url2.openConnection();
			connection.setHostnameVerifier(hv);
			javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
			javax.net.ssl.TrustManager tm = new miTM();
			trustAllCerts[0] = tm;
			javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext
					.getInstance("TLS");
			sc.init(null, trustAllCerts, null);
			connection.setSSLSocketFactory(sc
					.getSocketFactory());
			

//			SSLContext sc = SSLContext.getInstance("TLS");
//			sc.init(null, trustAllCerts, new java.security.SecureRandom());
//			connection.setSSLSocketFactory(sc.getSocketFactory());
//			connection.setHostnameVerifier(DO_NOT_VERIFY);

			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setUseCaches(false);

			connection.setReadTimeout(5000);
			connection.setConnectTimeout(5000);

			connection.setRequestProperty("Connection", "Keep-Alive");
			connection.connect();
			Object content = connection.getContent();
			System.out.println(content);
//			OutputStream outputStream = connection.getOutputStream();
//			System.out.println(outputStream);
		} catch (Exception e) {
			e.printStackTrace();
		}
//		MyHttp myHttp = new MyHttp(url);
//		String connect = myHttp.connect();
//		System.out.println(connect);
	}
}
