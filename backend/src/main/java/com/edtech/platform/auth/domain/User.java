package com.edtech.platform.auth.domain;

import com.edtech.platform.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private String phone;

    @Column(name = "parent_full_name")
    private String parentFullName;

    @Column(name = "parent_phone")
    private String parentPhone;

    @Column(name = "parent_email")
    private String parentEmail;

    @Column(name = "notify_parent")
    private Boolean notifyParent;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "oauth_provider")
    private String oauthProvider;

    @Column(name = "oauth_subject")
    private String oauthSubject;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Builder
    public User(String email, String passwordHash, String fullName, String phone,
                String parentFullName, String parentPhone, String parentEmail,
                Boolean notifyParent, Role role, UserStatus status, Boolean emailVerified) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        this.parentFullName = parentFullName;
        this.parentPhone = parentPhone;
        this.parentEmail = parentEmail;
        this.notifyParent = notifyParent;
        this.role = role;
        this.status = status;
        this.emailVerified = emailVerified;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
