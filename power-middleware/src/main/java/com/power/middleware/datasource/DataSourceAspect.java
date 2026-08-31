package com.power.middleware.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Order(0)
@Component
@ConditionalOnProperty(prefix = "power.datasource", name = "multi-enabled", havingValue = "true")
public class DataSourceAspect {

    @Around("@annotation(ds)")
    public Object around(ProceedingJoinPoint pjp, DS ds) throws Throwable {
        try {
            MultiDataSourceAutoConfiguration.setDataSource(ds.value());
            return pjp.proceed();
        } finally {
            MultiDataSourceAutoConfiguration.clear();
        }
    }
}
