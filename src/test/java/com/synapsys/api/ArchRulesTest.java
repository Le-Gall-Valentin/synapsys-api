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
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .check(classes);
    }

    @Test
    void domain_should_not_depend_on_spring_framework() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .check(classes);
    }

    @Test
    void application_should_not_depend_on_infrastructure() {
        noClasses()
            .that().resideInAPackage("..application..")
            .and(DescribedPredicate.describe("excluding tests",
                c -> !c.getSimpleName().endsWith("Test") && !c.getSimpleName().endsWith("IT")))
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .check(classes);
    }

    @Test
    void application_should_not_depend_on_spring_except_transactions() {
        DescribedPredicate<JavaClass> springExceptTransactions = DescribedPredicate.describe(
            "reside in org.springframework.. but not org.springframework.transaction.annotation..",
            clazz -> clazz.getPackageName().startsWith("org.springframework.")
                     && !clazz.getPackageName().startsWith("org.springframework.transaction.annotation")
        );
        noClasses()
            .that().resideInAPackage("..application..")
            .and(DescribedPredicate.describe("excluding tests",
                c -> !c.getSimpleName().endsWith("Test") && !c.getSimpleName().endsWith("IT")))
            .should().dependOnClassesThat(springExceptTransactions)
            .check(classes);
    }

    @Test
    void web_layer_should_not_instantiate_handler_implementations() {
        // Controllers may import UseCase interfaces from application/port/in/
        // and Commands from application/dto/, but must never depend on Handler implementations.
        noClasses()
            .that().resideInAPackage("..infrastructure.web..")
            .should().dependOnClassesThat()
            .resideInAPackage("..application.handler..")
            .check(classes);
    }

    @Test
    void jpa_entities_should_not_reside_in_domain_or_application() {
        noClasses()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..domain..")
            .orShould().resideInAPackage("..application..")
            .check(classes);
    }

    @Test
    void jpa_entities_should_reside_in_persistence_entity_package() {
        classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..infrastructure.persistence.entity..")
            .check(classes);
    }

    @Test
    void application_services_should_reside_in_an_application_package() {
        classes()
            .that().areAnnotatedWith(ApplicationService.class)
            .should().resideInAPackage("..application..")
            .check(classes);
    }

    @Test
    void ports_in_should_be_interfaces() {
        classes()
            .that().resideInAPackage("..domain.port.in..")
            .should().beInterfaces()
            .allowEmptyShould(true)
            .check(classes);
    }

    @Test
    void ports_out_should_be_interfaces() {
        classes()
            .that().resideInAPackage("..domain.port.out..")
            .should().beInterfaces()
            .check(classes);
    }

    @Test
    void mfa_domain_and_application_should_not_depend_on_authentication_or_identity() {
        noClasses()
            .that().resideInAPackage("com.synapsys.api.mfa.domain..")
            .or().resideInAPackage("com.synapsys.api.mfa.application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.synapsys.api.authentication..",
                "com.synapsys.api.identity..")
            .allowEmptyShould(false)
            .check(classes);
    }

    @Test
    void identity_domain_and_application_should_not_depend_on_authentication_or_mfa() {
        noClasses()
            .that().resideInAPackage("com.synapsys.api.identity.domain..")
            .or().resideInAPackage("com.synapsys.api.identity.application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.synapsys.api.authentication..",
                "com.synapsys.api.mfa..")
            .allowEmptyShould(false)
            .check(classes);
    }

    @Test
    void authentication_domain_and_application_should_not_depend_on_identity_or_mfa() {
        noClasses()
            .that().resideInAPackage("com.synapsys.api.authentication.domain..")
            .or().resideInAPackage("com.synapsys.api.authentication.application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.synapsys.api.identity..",
                "com.synapsys.api.mfa..")
            .allowEmptyShould(false)
            .check(classes);
    }

    @Test
    void global_infrastructure_should_not_depend_on_bc_infrastructure() {
        noClasses()
            .that().resideInAPackage("com.synapsys.api.infrastructure..")
            .and(DescribedPredicate.describe("excluding tests and SecurityConfig",
                c -> !c.getSimpleName().endsWith("Test")
                  && !c.getSimpleName().endsWith("IT")
                  && !c.getSimpleName().equals("SecurityConfig")))
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.synapsys.api.authentication.infrastructure..",
                "com.synapsys.api.identity.infrastructure..",
                "com.synapsys.api.mfa.infrastructure..")
            .allowEmptyShould(true)
            .check(classes);
    }

    @Test
    void bc_infrastructure_should_not_depend_on_other_bc_infrastructure() {
        DescribedPredicate<JavaClass> excludeTests = DescribedPredicate.describe("excluding tests",
            c -> !c.getSimpleName().endsWith("Test") && !c.getSimpleName().endsWith("IT"));

        noClasses()
            .that().resideInAPackage("com.synapsys.api.identity.infrastructure..")
            .and(excludeTests)
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.synapsys.api.authentication.infrastructure..",
                "com.synapsys.api.mfa.infrastructure..")
            .allowEmptyShould(false)
            .check(classes);

        noClasses()
            .that().resideInAPackage("com.synapsys.api.mfa.infrastructure..")
            .and(excludeTests)
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.synapsys.api.authentication.infrastructure..",
                "com.synapsys.api.identity.infrastructure..")
            .allowEmptyShould(false)
            .check(classes);
    }

    @Test
    void outbound_adapters_should_implement_a_domain_port() {
        classes()
            .that().resideInAPackage("..infrastructure.persistence.adapter..")
            .and().areAnnotatedWith(org.springframework.stereotype.Component.class)
            .should().implement(DescribedPredicate.describe(
                "a port in ..domain.port.out..",
                iface -> iface.getPackageName().contains(".domain.port.out")
            ))
            .check(classes);
    }

    @Test
    void configuration_classes_should_not_implement_domain_ports() {
        noClasses()
            .that().haveSimpleName("SynapsysProperties")
            .should().implement(com.synapsys.api.authentication.domain.port.out.RefreshTokenConfigPort.class)
            .check(classes);
    }
}