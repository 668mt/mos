package mt.utils.http;//package com.oig.mt.utils.http;
//
//import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.client.utils.HttpClientUtils;
//import org.apache.http.conn.ClientConnectionManager;
//import org.apache.http.conn.scheme.Scheme;
//import org.apache.http.conn.scheme.SchemeRegistry;
//import org.apache.http.conn.ssl.SSLSocketFactory;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.message.BasicHeader;
//import org.apache.http.util.EntityUtils;
//
//import java.io.InputStream;
//import java.security.cert.CertificateException;
//import java.security.cert.X509Certificate;
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.TrustManager;
//import javax.net.ssl.X509TrustManager;
//import org.apache.http.conn.ClientConnectionManager;
//import org.apache.http.conn.scheme.Scheme;
//import org.apache.http.conn.scheme.SchemeRegistry;
//import org.apache.http.conn.ssl.SSLSocketFactory;
//import org.apache.http.impl.client.DefaultHttpClient;
//
///**
// * @Author Martin
// * @Date 2018/8/23
// */
//public class HttpTest2 {
//	
//	public static void main(String[] args) {
//		HttpClient httpClient = null;
//		String url = "https://www.shipmentlink.com/servlet/TDB1_CargoTracking.do";
//		try{
//			httpClient = new SSLClient();
//			HttpGet httpGet = new HttpGet(url);
//			HttpResponse execute = httpClient.execute(httpGet);
//			InputStream content = execute.getEntity().getContent();
//			System.out.println(content);
//		}catch(Exception ex){
//			ex.printStackTrace();
//		}
//	}
//	
//	@SuppressWarnings("resource")
//	public static String doPost(String url,String jsonstr,String charset){
//		HttpClient httpClient = null;
//		HttpPost httpPost = null;
//		String result = null;
//		try{
//			httpClient = new SSLClient();
//			httpPost = new HttpPost(url);
//			httpPost.addHeader("Content-Type", "application/json");
//			StringEntity se = new StringEntity(jsonstr);
//			se.setContentType("text/json");
//			se.setContentEncoding(new BasicHeader("Content-Type", "application/json"));
//			httpPost.setEntity(se);
//			HttpResponse response = httpClient.execute(httpPost);
//			if(response != null){
//				HttpEntity resEntity = response.getEntity();
//				if(resEntity != null){
//					result = EntityUtils.toString(resEntity,charset);
//				}
//			}
//		}catch(Exception ex){
//			ex.printStackTrace();
//		}
//		return result;
//	}
//	
//	public static class SSLClient extends DefaultHttpClient {
//		public SSLClient() throws Exception{
//			super();
//			SSLContext ctx = SSLContext.getInstance("TLS");
//			X509TrustManager tm = new X509TrustManager() {
//				@Override
//				public void checkClientTrusted(X509Certificate[] chain,
//											   String authType) throws CertificateException {
//				}
//				@Override
//				public void checkServerTrusted(X509Certificate[] chain,
//											   String authType) throws CertificateException {
//				}
//				@Override
//				public X509Certificate[] getAcceptedIssuers() {
//					return null;
//				}
//			};
//			ctx.init(null, new TrustManager[]{tm}, null);
//			SSLSocketFactory ssf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//			ClientConnectionManager ccm = this.getConnectionManager();
//			SchemeRegistry sr = ccm.getSchemeRegistry();
//			sr.register(new Scheme("https", 443, ssf));
//		}
//	}
//}
