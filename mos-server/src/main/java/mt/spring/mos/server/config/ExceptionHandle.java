package mt.spring.mos.server.config;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import mt.common.entity.ResResult;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.StringUtils;
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
import java.util.ArrayList;
import java.util.List;


@ControllerAdvice
@Slf4j
public class ExceptionHandle {
	
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
			log.error(e.getMessage());
		}
		
		ResResult resResult = new ResResult();
		resResult.setStatus(ResResult.Status.error);
		if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
			resResult.setMessage(e.getMessage());
			log.error(e.getMessage(), e);
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
			log.error("参数校验失败：{}", e.getMessage());
		} else if (e instanceof BindException) {
			BindException bindException = (BindException) e;
			List<FieldError> fieldErrors = bindException.getFieldErrors();
			List<String> errorMsg = new ArrayList<>();
			for (FieldError fieldError : fieldErrors) {
				String defaultMessage = fieldError.getDefaultMessage();
				errorMsg.add(defaultMessage);
			}
			resResult.setMessage(StringUtils.join(errorMsg, ","));
			log.error("参数校验失败：{}", e.getMessage());
		} else if (e instanceof ClientAbortException) {
			resResult.setMessage(e.getMessage());
		} else {
			resResult.setMessage("系统异常");
			log.error(e.getMessage(), e);
		}
		try {
			response.getWriter().write(JSONObject.toJSONString(resResult));
		} catch (Exception ignored) {
		}
		return null;
		
	}
}
