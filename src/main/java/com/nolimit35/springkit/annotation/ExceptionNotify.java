package com.nolimit35.springkit.annotation;

import java.lang.annotation.*;

/**
 * 标记需要异常通知的类或方法
 * 可以标记在类上或方法上
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExceptionNotify {

	/**
	 * 异常通知的标题
	 */
	String title() default "";

	/**
	 * 是否启用异常通知
	 */
	boolean enabled() default true;
}