package com.item.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: ChangYajie
 * @date: 2019/8/14
 */

@Retention(RetentionPolicy.RUNTIME)

@Target({ElementType.METHOD})    //只针对方法

public @interface Benchmark {

}
