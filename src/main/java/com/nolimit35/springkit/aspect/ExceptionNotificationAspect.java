package com.nolimit35.springkit.aspect;

import com.nolimit35.springkit.service.ExceptionNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ExceptionNotificationAspect {

	private final ExceptionNotificationService notificationService;

	public ExceptionNotificationAspect(ExceptionNotificationService notificationService) {
		this.notificationService = notificationService;
	}

	// 定义切点：所有被 @Controller 或 @RestController 标记的类的所有方法,或者 @ExceptionNotify 标记的类
	@Pointcut("@within(org.springframework.stereotype.Controller) || @within(org.springframework.web.bind.annotation.RestController) || @annotation(com.nolimit35.springkit.annotation.ExceptionNotify)")
	public void allPointcut() {
	}

	/**
	 * 当拦截的切点返回抛出异常时,会执行此方法 且在 @ControllerAdvice 和 @RestControllerAdvice 前执行
	 * * @param ex
	 */
	@AfterThrowing(
			pointcut = "allPointcut()",
			throwing = "ex"
	)
	public void handleException(Exception ex) {
		try {
			notificationService.processException(ex);
		} catch (Exception e) {
			log.error("Error in exception notification aspect", e);
		}
	}
}