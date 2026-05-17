package com.synapsys.api;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

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
            .resideInAPackage("..auth.infrastructure..");
        rule.check(classes);
    }

    @Test
    void web_controllers_should_not_depend_on_application_layer() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..auth.infrastructure.web..")
            .should().dependOnClassesThat()
            .resideInAPackage("..auth.application..");
        rule.check(classes);
    }
}