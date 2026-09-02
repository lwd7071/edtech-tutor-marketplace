package com.edtech.platform.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PaymentArchitectureTest {
    static final Set<String> BANNED_MUTATION_METHODS = Set.of("save", "saveAll", "update", "delete", "deleteById", "deleteAll");
    static final ArchCondition<JavaClass> NOT_DECLARE_GENERIC_MUTATIONS = new ArchCondition<>("not declare generic mutation methods") {
        public void check(JavaClass item, ConditionEvents events) {
            for (JavaMethod method : item.getMethods()) {
                if (BANNED_MUTATION_METHODS.contains(method.getName())) {
                    events.add(SimpleConditionEvent.violated(method, item.getName() + " declares banned method " + method.getName()));
                }
            }
        }
    };

    private final JavaClasses production = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.edtech.platform.payment");

    @Test
    void invoiceRepositoriesAreCustomAndExposeNoGenericMutations() {
        com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
                .that().resideInAPackage("..payment.repository..")
                .and().haveSimpleNameContaining("Invoice")
                .should().notBeAssignableTo(Repository.class).check(production);
        com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
                .that().resideInAPackage("..payment.repository..")
                .should(NOT_DECLARE_GENERIC_MUTATIONS).check(production);
    }

    @Test
    void entityManagerAndCommandQueryBoundariesAreEnforced() {
        noClasses().that().resideInAPackage("..payment.service..")
                .should().dependOnClassesThat().haveFullyQualifiedName("jakarta.persistence.EntityManager")
                .allowEmptyShould(true).check(production);
        noClasses().that().resideInAPackage("..payment.service.command..")
                .should().dependOnClassesThat().haveSimpleName("InvoiceQueryRepository")
                .allowEmptyShould(true).check(production);
        noClasses().that().resideInAPackage("..payment.service.query..")
                .should().dependOnClassesThat().haveSimpleName("InvoiceCommandRepository")
                .allowEmptyShould(true).check(production);
        noClasses().that().resideInAPackage("..payment..")
                .and().resideOutsideOfPackage("..payment.repository..")
                .should().dependOnClassesThat().haveFullyQualifiedName("jakarta.persistence.EntityManager")
                .allowEmptyShould(true).check(production);
    }
}
