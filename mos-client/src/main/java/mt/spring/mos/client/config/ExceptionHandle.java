package mt.spring.mos.client.config;

import com.alibaba.fastjson.JSONObject;
import mt.spring.mos.client.entity.ResResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@ControllerAdvice
public class ExceptionHandle {
	private final static Log LOGGER = LogFactory.getLog(ExceptionHandle.class);
	
	/**
	 * 判断错误是否是已定义的已知错误，不是则由未知错误代替，同时记录在log中
	 *
	 * @param e
	 * @return
	 */
	@ExceptionHandler(value = Exception.class)
	public ModelAndView exceptionGet(HttpServletRequest request, HttpServletResponse response, Exception e) {
		response.setContentType("application/json;charset=utf-8");
		if (e instanceof NoHandlerFoundException) {
			LOGGER.info(e.getMessage());
		}
		
		ResResult resResult = new ResResult();
		resResult.setStatus(ResResult.ERROR);
		if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
			resResult.setMessage(e.getMessage());
			LOGGER.error("异常统一处理：", e);
		} else if (e instanceof MethodArgumentNotValidException) {
			//获取校验错误信息
			MethodArgumentNotValidException methodArgumentNotValidException = (MethodArgumentNotValidException) e;
			BindingResult bindingResult = methodArgumentNotValidException.getBindingResult();
			List<FieldError> fieldErrors = bindingResult.getFieldErrors();
			List<String> errorMsg = new ArrayList<>();
			for (FieldError fieldError : fieldErrors) {
				String defaultMessage = fieldError.getDefaultMessage();
				errorMsg.add(defaultMessage);
			}
			resResult.setMessage(StringUtils.join(errorMsg, ","));
			LOGGER.error(e.getMessage());
		} else if (e instanceof BindException) {
			BindException bindException = (BindException) e;
			List<FieldError> fieldErrors = bindException.getFieldErrors();
			List<String> errorMsg = new ArrayList<>();
			for (FieldError fieldError : fieldErrors) {
				String defaultMessage = fieldError.getDefaultMessage();
				errorMsg.add(defaultMessage);
			}
			resResult.setMessage(StringUtils.join(errorMsg, ","));
			LOGGER.error(e.getMessage());
		} else {
			resResult.setMessage(e.getMessage());
			response.setStatus(500);
			LOGGER.error("异常统一处理：", e);
		}
		try {
			response.getWriter().write(JSONObject.toJSONString(resResult));
		} catch (IOException ignored) {
		}
		return null;
		
	}
}
