package com.edtech.platform.architecture;

import com.edtech.platform.auth.service.OAuthAuthorizationPort;
import com.edtech.platform.mail.MailTransport;
import com.edtech.platform.payment.gateway.PaymentGateway;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.Repository;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class SolidGuardrailsArchitectureTest {
    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.edtech.platform");

    @Test
    void controllersDoNotAccessPersistenceDirectly() {
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().areAssignableTo(Repository.class)
                .allowEmptyShould(true)
                .check(production);
    }

    @Test
    void configurationComesThroughValidatedPropertyObjects() {
        fields().that().areAnnotatedWith(Value.class)
                .should().beDeclaredInClassesThat().resideInAPackage("..config..")
                .allowEmptyShould(true)
                .check(production);
        noClasses().should().callMethod(System.class, "getenv", String.class)
                .allowEmptyShould(true)
                .check(production);
        noClasses().should().callMethod(System.class, "getProperty", String.class)
                .allowEmptyShould(true)
                .check(production);
    }

    @Test
    void domainDoesNotDependOnDeliveryOrIntegrationLayers() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..controller..", "..dto.request..", "..gateway..", "..adapter..", "..integration..")
                .allowEmptyShould(true)
                .check(production);
    }

    @Test
    void vendorSdksStayInsideTheirAdapters() {
        noClasses().that().resideOutsideOfPackages("..common.storage..")
                .should().dependOnClassesThat().resideInAPackage("com.cloudinary..")
                .allowEmptyShould(true)
                .check(production);
        noClasses().that().resideOutsideOfPackages("..payment.config..", "..payment.gateway..")
                .should().dependOnClassesThat().resideInAPackage("vn.payos..")
                .allowEmptyShould(true)
                .check(production);
        noClasses().that().resideOutsideOfPackages("..mail..")
                .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.mail.javamail.JavaMailSender")
                .allowEmptyShould(true)
                .check(production);
    }

    @Test
    void externalAdaptersUseExplicitSeams() {
        classes().that().haveSimpleNameEndingWith("MailTransport")
                .and().areNotInterfaces()
                .should().beAssignableTo(MailTransport.class)
                .check(production);
        classes().that().haveSimpleName("PayOsPaymentGateway")
                .should().beAssignableTo(PaymentGateway.class)
                .check(production);
        classes().that().haveSimpleName("OAuth2AuthenticationSuccessHandler")
                .should().dependOnClassesThat().areAssignableTo(OAuthAuthorizationPort.class)
                .check(production);
    }
}
