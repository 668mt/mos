//package mt.spring.mos.server.controller.testconfig;
//
//import javassist.ByteArrayClassPath;
//import javassist.ClassClassPath;
//import javassist.ClassPool;
//import javassist.CtClass;
//import mt.spring.mos.server.entity.po.Client;
//
///**
// * @Author Martin
// * @Date 2021/8/28
// */
//public class Test2 {
//	public static void main(String[] args) throws Exception {
//		ClassPool pool = ClassPool.getDefault();
//		CtClass ctClass = pool.makeInterface("mt.spring.mos.server.controller.testconfig.TestMapper", pool.getCtClass(mt.common.mybatis.mapper.BaseMapper.class.getName()));
//		ctClass.setGenericSignature("Ljava/lang/Object;L" + mt.common.mybatis.mapper.BaseMapper.class.getName().replace(".", "/") + "<L" + Client.class.getName().replace(".", "/") + ";>;");
//		// 将类搜索路径插入到搜索路径之前
//		pool.appendClassPath(new ClassClassPath(ctClass.toClass()));
//		// 将类搜索路径添加到搜索路径之后
//		Class<?> aClass = Class.forName("mt.spring.mos.server.controller.testconfig.TestMapper");
//		System.out.println(aClass);
//	}
//}
