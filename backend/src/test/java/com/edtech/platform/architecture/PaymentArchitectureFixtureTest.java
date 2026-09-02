package com.edtech.platform.architecture;

import com.edtech.platform.architecture.paymentfixtures.BadCrudInvoiceRepository;
import com.edtech.platform.architecture.paymentfixtures.BadNamedInvoiceRepository;
import com.edtech.platform.architecture.paymentfixtures.command.BadCommandService;
import com.edtech.platform.architecture.paymentfixtures.query.BadQueryService;
import com.edtech.platform.architecture.paymentfixtures.service.BadEntityManagerService;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentArchitectureFixtureTest {
    @Test void crudInheritanceFixtureIsCaught() {
        var imported = new ClassFileImporter().importClasses(BadCrudInvoiceRepository.class);
        assertThrows(AssertionError.class, () -> classes().should().notBeAssignableTo(Repository.class).check(imported));
    }
    @Test void bannedMethodFixtureIsCaught() {
        var imported = new ClassFileImporter().importClasses(BadNamedInvoiceRepository.class);
        assertThrows(AssertionError.class, () -> classes().should(PaymentArchitectureTest.NOT_DECLARE_GENERIC_MUTATIONS).check(imported));
    }
    @Test void entityManagerServiceFixtureIsCaught() {
        var imported = new ClassFileImporter().importClasses(BadEntityManagerService.class);
        assertThrows(AssertionError.class, () -> noClasses().should().dependOnClassesThat().haveFullyQualifiedName("jakarta.persistence.EntityManager").check(imported));
    }
    @Test void commandQueryFixturesAreCaught() {
        var command = new ClassFileImporter().importClasses(BadCommandService.class);
        var query = new ClassFileImporter().importClasses(BadQueryService.class);
        assertThrows(AssertionError.class, () -> noClasses().should().dependOnClassesThat().haveSimpleName("InvoiceQueryRepository").check(command));
        assertThrows(AssertionError.class, () -> noClasses().should().dependOnClassesThat().haveSimpleName("InvoiceCommandRepository").check(query));
    }
}
