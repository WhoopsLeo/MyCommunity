package com.nowcoder.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 注解只能作用在方法上
@Target(ElementType.METHOD)
// 注解在程序运行时生效
@Retention(RetentionPolicy.RUNTIME)

// 只有被@LoginRequired修饰的方法，若没有登录，会被拦截
public @interface LoginRequired {



}
