package com.synapsys.api.infrastructure.config;

import com.synapsys.api.auth.domain.model.AuthException;
import com.synapsys.api.shared.annotation.DomainService;
import org.springframework.aop.Advisor;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
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

        // Target only beans annotated with @DomainService
        AspectJExpressionPointcut aspectJPointcut = new AspectJExpressionPointcut();
        aspectJPointcut.setExpression("@target(com.synapsys.api.shared.annotation.DomainService)");

        // Guard the class filter so Spring never tries to CGLIB-proxy classes it cannot subclass
        Pointcut pointcut = new Pointcut() {
            @Override
            public ClassFilter getClassFilter() {
                return clazz -> clazz.isAnnotationPresent(DomainService.class);
            }

            @Override
            public MethodMatcher getMethodMatcher() {
                return aspectJPointcut.getMethodMatcher();
            }
        };

        return new DefaultPointcutAdvisor(pointcut, new TransactionInterceptor(txManager, source));
    }
}