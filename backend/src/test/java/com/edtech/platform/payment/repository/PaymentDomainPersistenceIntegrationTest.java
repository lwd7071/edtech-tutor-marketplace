package com.edtech.platform.payment.repository;

import com.edtech.platform.common.AbstractIntegrationTest;
import com.edtech.platform.enrollment.domain.StudentPackage;
import com.edtech.platform.enrollment.repository.StudentPackageRepository;
import com.edtech.platform.finance.domain.*;
import com.edtech.platform.finance.repository.LedgerEntryRepository;
import com.edtech.platform.finance.repository.WalletRepository;
import com.edtech.platform.payment.domain.Invoice;
import com.edtech.platform.payment.domain.PaymentTransaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDomainPersistenceIntegrationTest extends AbstractIntegrationTest {
    @Autowired InvoiceCommandRepository invoiceCommands;
    @Autowired InvoiceQueryRepository invoiceQueries;
    @Autowired PaymentTransactionRepository transactions;
    @Autowired PaymentIdentifierRepository identifiers;
    @Autowired StudentPackageRepository packages;
    @Autowired WalletRepository wallets;
    @Autowired LedgerEntryRepository ledger;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;

    @Test @Transactional
    void mappingsLocksSoftDeleteAndAppendOnlyRepositoriesWorkTogether() {
        Fixture f = fixture();
        long orderCode = identifiers.nextPayosOrderCode();
        Invoice invoice = Invoice.pending("INV-TEST-" + UUID.randomUUID(), orderCode, f.studentId, f.teacherId,
                f.pricingId, 100_000, UUID.randomUUID(), "a".repeat(64));
        invoiceCommands.insert(invoice);
        entityManager.flush();
        assertThat(invoiceCommands.findByIdForUpdate(invoice.getId())).isPresent();
        assertThat(entityManager.getLockMode(invoice)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(invoiceQueries.findByIdAndStudentId(invoice.getId(), f.studentId)).isPresent();
        assertThat(invoiceQueries.findByIdAndStudentId(invoice.getId(), UUID.randomUUID())).isEmpty();

        PaymentTransaction transaction = new PaymentTransaction(invoice.getId(), "FAKE", "ref-" + UUID.randomUUID(),
                orderCode, 100_000, Instant.now(), true, null);
        transactions.append(transaction);

        StudentPackage studentPackage = StudentPackage.activateAfterPayment(f.studentId, f.teacherId, f.subjectId,
                f.pricingId, invoice.getId(), "Package", 2, 30, 100_000, new BigDecimal("5.00"), Instant.now());
        packages.saveAndFlush(studentPackage);
        assertThat(packages.findByIdForUpdate(studentPackage.getId())).isPresent();

        Wallet wallet = wallets.saveAndFlush(Wallet.forTeacher(f.teacherId));
        assertThat(wallets.findByTeacherIdForUpdate(f.teacherId)).isPresent();
        LedgerEntry entry = new LedgerEntry(wallet.getId(), LedgerEntryType.PACKAGE_FUNDED, 100_000,
                BalanceBucket.PENDING, LedgerDirection.CREDIT, "INVOICE", invoice.getId(),
                "invoice:" + invoice.getId(), "funding");
        ledger.append(entry);
        entityManager.flush();
        assertThat(transactions.existsByProviderReference(transaction.getProviderReference())).isTrue();
        assertThat(ledger.existsByIdempotencyKey(entry.getIdempotencyKey())).isTrue();

        packages.delete(studentPackage);
        wallets.delete(wallet);
        entityManager.remove(invoice);
        entityManager.flush();
        entityManager.clear();
        assertThat(packages.findById(studentPackage.getId())).isEmpty();
        assertThat(wallets.findByTeacherId(f.teacherId)).isEmpty();
        assertThat(invoiceQueries.findByIdAndStudentId(invoice.getId(), f.studentId)).isEmpty();
        assertThat(jdbc.queryForObject("select is_deleted from invoices where id=?", Boolean.class, invoice.getId())).isTrue();
        assertThat(jdbc.queryForObject("select is_deleted from student_packages where id=?", Boolean.class, studentPackage.getId())).isTrue();
        assertThat(jdbc.queryForObject("select is_deleted from wallets where id=?", Boolean.class, wallet.getId())).isTrue();
    }

    private Fixture fixture() {
        UUID student = UUID.randomUUID(), teacherUser = UUID.randomUUID(), teacher = UUID.randomUUID();
        UUID subject = UUID.randomUUID(), pricing = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id,email,password_hash,full_name,role,status) VALUES (?,?,?,?,?,?)", student, student+"@test", "hash", "Student", "STUDENT", "ACTIVE");
        jdbc.update("INSERT INTO users (id,email,password_hash,full_name,role,status) VALUES (?,?,?,?,?,?)", teacherUser, teacherUser+"@test", "hash", "Teacher", "TEACHER", "ACTIVE");
        jdbc.update("INSERT INTO teacher_profiles (id,user_id,profile_status) VALUES (?,?,?)", teacher, teacherUser, "APPROVED");
        jdbc.update("INSERT INTO subjects (id,code,name,slug) VALUES (?,?,?,?)", subject, "SUB-"+subject, "Subject", "subject-"+subject);
        jdbc.update("INSERT INTO pricing_packages (id,teacher_id,subject_id,name,total_sessions,duration_days,price_vnd,session_duration_minutes,status) VALUES (?,?,?,?,?,?,?,?,?)", pricing, teacher, subject, "Package", 2, 30, 100_000, 60, "ACTIVE");
        return new Fixture(student, teacher, subject, pricing);
    }
    private record Fixture(UUID studentId, UUID teacherId, UUID subjectId, UUID pricingId) { }
}
