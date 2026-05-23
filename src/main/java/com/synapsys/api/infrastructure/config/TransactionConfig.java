package com.synapsys.api.infrastructure.config;

import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.CompositeTransactionAttributeSource;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;

@Configuration
public class TransactionConfig {

    @Bean
    public Advisor applicationServiceTransactionAdvisor(TransactionManager txManager) {
        // 1. Annotation-based source: respects @Transactional on individual methods
        //    (e.g. RefreshTokenHandler.refresh has noRollbackFor = TokenRevoked)
        TransactionAttributeSource annotationSource = new AnnotationTransactionAttributeSource();

        // 2. Fallback source: REQUIRED + rollback-on-any-exception for all other methods
        NameMatchTransactionAttributeSource fallbackSource = new NameMatchTransactionAttributeSource();
        fallbackSource.addTransactionalMethod("*", new DefaultTransactionAttribute());

        // Composite: annotation attributes take priority; falls back to name-match for unannotated methods
        TransactionAttributeSource compositeSource =
            new CompositeTransactionAttributeSource(annotationSource, fallbackSource);

        // Cover all @ApplicationService beans via annotation pointcut and legacy package pointcut
        Pointcut byAnnotation = AnnotationMatchingPointcut.forClassAnnotation(ApplicationService.class);
        AspectJExpressionPointcut byPackage = new AspectJExpressionPointcut();
        byPackage.setExpression("within(com.synapsys.api.auth.application..*)");
        Pointcut pointcut = new ComposablePointcut(byAnnotation).union((Pointcut) byPackage);

        return new DefaultPointcutAdvisor(pointcut, new TransactionInterceptor(txManager, compositeSource));
    }
}