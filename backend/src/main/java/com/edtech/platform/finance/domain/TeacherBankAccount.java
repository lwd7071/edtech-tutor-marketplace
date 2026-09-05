package com.edtech.platform.finance.domain;

import com.edtech.platform.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "teacher_bank_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE teacher_bank_accounts SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class TeacherBankAccount extends BaseEntity {

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "bank_bin", nullable = false, length = 20)
    private String bankBin;

    @Column(name = "bank_name", nullable = false, length = 150)
    private String bankName;

    @Column(name = "account_number_encrypted", nullable = false, columnDefinition = "text")
    private String accountNumberEncrypted;

    @Column(name = "account_holder_name", nullable = false, length = 150)
    private String accountHolderName;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    public static TeacherBankAccount create(
            UUID teacherId,
            String bankBin,
            String bankName,
            String accountNumberEncrypted,
            String accountHolderName,
            boolean isDefault
    ) {
        TeacherBankAccount acc = new TeacherBankAccount();
        acc.teacherId = Objects.requireNonNull(teacherId, "teacherId is required");
        acc.bankBin = Objects.requireNonNull(bankBin, "bankBin is required");
        acc.bankName = Objects.requireNonNull(bankName, "bankName is required");
        acc.accountNumberEncrypted = Objects.requireNonNull(accountNumberEncrypted, "accountNumberEncrypted is required");
        acc.accountHolderName = Objects.requireNonNull(accountHolderName, "accountHolderName is required");
        acc.verified = false;
        acc.isDefault = isDefault;
        return acc;
    }

    public void update(
            String bankBin,
            String bankName,
            String accountNumberEncrypted,
            String accountHolderName,
            boolean isDefault
    ) {
        this.bankBin = Objects.requireNonNull(bankBin, "bankBin is required");
        this.bankName = Objects.requireNonNull(bankName, "bankName is required");
        this.accountNumberEncrypted = Objects.requireNonNull(accountNumberEncrypted, "accountNumberEncrypted is required");
        this.accountHolderName = Objects.requireNonNull(accountHolderName, "accountHolderName is required");
        this.isDefault = isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}