package mt.common.service;

import freemarker.core.Environment;
import freemarker.template.*;
import mt.common.utils.FreeMarkerUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 模板指令 - 基类
 *
 * @author OIG Team
 * @version 5.0
 */
public abstract class BaseDirective implements TemplateDirectiveModel {

	/**
	 * "是否使用缓存"参数名称
	 */
	private static final String USE_CACHE_PARAMETER_NAME = "useCache";

	/**
	 * 是否使用缓存
	 *
	 * @param params
	 *            参数
	 * @return 使用缓存
	 */
	protected boolean useCache(Map<String, TemplateModel> params) throws TemplateModelException {
		Boolean useCache = FreeMarkerUtils.getParameter(USE_CACHE_PARAMETER_NAME, Boolean.class, params);
		return useCache != null ? useCache : true;
	}

	/**
	 * 设置局部变量
	 *
	 * @param name
	 *            名称
	 * @param value
	 *            变量值
	 * @param env
	 *            环境变量
	 * @param body
	 *            模板内容
	 */
	protected void setLocalVariable(String name, Object value, Environment env, TemplateDirectiveBody body) throws TemplateException, IOException {
		TemplateModel preVariable = FreeMarkerUtils.getVariable(name, env);
		try {
			FreeMarkerUtils.setVariable(name, value, env);
			body.render(env.getOut());
		} finally {
			FreeMarkerUtils.setVariable(name, preVariable, env);
		}
	}

	/**
	 * 设置局部变量
	 *
	 * @param variables
	 *            变量
	 * @param env
	 *            环境变量
	 * @param body
	 *            模板内容
	 */
	protected void setLocalVariables(Map<String, Object> variables, Environment env, TemplateDirectiveBody body) throws TemplateException, IOException {
		Map<String, Object> preVariables = new HashMap<String, Object>();
		for (String name : variables.keySet()) {
			TemplateModel preVariable = FreeMarkerUtils.getVariable(name, env);
			preVariables.put(name, preVariable);
		}
		try {
			FreeMarkerUtils.setVariables(variables, env);
			body.render(env.getOut());
		} finally {
			FreeMarkerUtils.setVariables(preVariables, env);
		}
	}

}