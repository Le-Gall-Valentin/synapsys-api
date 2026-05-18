package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.domain.model.AuthException;
import com.synapsys.api.shared.annotation.ApplicationService;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@Configuration
public class TransactionConfig {

    @Bean
    public Advisor applicationServiceTransactionAdvisor(TransactionManager txManager) {
        NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();

        // refresh(): revocation must commit even if TokenRevoked is thrown afterward
        RuleBasedTransactionAttribute refreshAttr = new RuleBasedTransactionAttribute();
        refreshAttr.getRollbackRules().add(new NoRollbackRuleAttribute(AuthException.TokenRevoked.class));
        source.addTransactionalMethod("refresh", refreshAttr);

        // All other methods: default transactional behavior (REQUIRED, rollback on any exception)
        source.addTransactionalMethod("*", new DefaultTransactionAttribute());

        Pointcut pointcut = AnnotationMatchingPointcut.forClassAnnotation(ApplicationService.class);
        return new DefaultPointcutAdvisor(pointcut, new TransactionInterceptor(txManager, source));
    }
}