package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.domain.model.AuthException;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
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

        // refresh(): token revocation must commit even if AuthException is thrown afterward
        RuleBasedTransactionAttribute refreshAttr = new RuleBasedTransactionAttribute();
        refreshAttr.getRollbackRules().add(new NoRollbackRuleAttribute(AuthException.class));
        source.addTransactionalMethod("refresh", refreshAttr);

        // All other methods: default transactional behavior (REQUIRED, rollback on any exception)
        source.addTransactionalMethod("*", new DefaultTransactionAttribute());

        // Target only classes in *.application.* packages
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(* com.synapsys.api.*.application.*.*(..))");

        return new DefaultPointcutAdvisor(pointcut, new TransactionInterceptor(txManager, source));
    }
}