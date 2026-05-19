package com.synapsys.api.infrastructure.ratelimit;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Bean
    public Advisor rateLimitAdvisor(RateLimitMethodInterceptor interceptor) {
        return new DefaultPointcutAdvisor(
            AnnotationMatchingPointcut.forMethodAnnotation(RateLimiting.class),
            interceptor
        );
    }
}