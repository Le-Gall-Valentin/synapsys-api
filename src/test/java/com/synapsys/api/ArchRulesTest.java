package com.synapsys.api;

import com.synapsys.api.shared.annotation.ApplicationService;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchRulesTest {

    private final JavaClasses classes = new ClassFileImporter()
        .importPackages("com.synapsys.api");

    @Test
    void domain_should_not_depend_on_infrastructure() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..auth.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..auth.infrastructure..");
        rule.check(classes);
    }

    @Test
    void domain_should_not_depend_on_spring_framework() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..auth.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..");
        rule.check(classes);
    }

    @Test
    void application_should_not_depend_on_infrastructure() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..auth.application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..auth.infrastructure..", "..infrastructure..");
        rule.check(classes);
    }

    @Test
    void web_layer_should_not_bypass_use_case_ports() {
        DescribedPredicate<JavaClass> applicationHandlers = DescribedPredicate.describe(
            "reside in auth.application and have name ending with Handler",
            clazz -> clazz.getPackageName().contains(".auth.application")
                     && clazz.getSimpleName().endsWith("Handler")
        );
        noClasses()
            .that().resideInAPackage("..auth.infrastructure.web..")
            .should().dependOnClassesThat(applicationHandlers)
            .check(classes);
    }

    @Test
    void application_should_not_depend_on_spring_framework() {
        // @Transactional is intentionally allowed: transaction boundaries are an application-level concern.
        // All other Spring framework dependencies remain forbidden in this layer.
        DescribedPredicate<JavaClass> springExceptTransactions = DescribedPredicate.describe(
            "reside in org.springframework.. but not org.springframework.transaction.annotation..",
            clazz -> clazz.getPackageName().startsWith("org.springframework.")
                     && !clazz.getPackageName().startsWith("org.springframework.transaction.annotation")
        );
        noClasses()
            .that().resideInAPackage("..auth.application..")
            .should().dependOnClassesThat(springExceptTransactions)
            .check(classes);
    }

    @Test
    void jpa_entities_should_not_reside_in_domain_or_application() {
        ArchRule rule = noClasses()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..domain..")
            .orShould().resideInAPackage("..application..");
        rule.check(classes);
    }

    @Test
    void jpa_entities_should_reside_in_persistence_entity_package() {
        ArchRule rule = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..infrastructure.persistence.entity..");
        rule.check(classes);
    }

    @Test
    void application_services_should_reside_in_application_package() {
        ArchRule rule = classes()
            .that().areAnnotatedWith(ApplicationService.class)
            .should().resideInAPackage("..auth.application..");
        rule.check(classes);
    }

    @Test
    void ports_in_should_be_interfaces() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain.port.in..")
            .should().beInterfaces();
        rule.check(classes);
    }

    @Test
    void ports_out_should_be_interfaces() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain.port.out..")
            .should().beInterfaces();
        rule.check(classes);
    }
}