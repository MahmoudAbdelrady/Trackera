package com.mdevs.trackera.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(indexes = {@Index(columnList = "TYPE"), @Index(columnList = "USER_ID, TYPE"), @Index(columnList = "CREATED_AT")})
@NoArgsConstructor
@Getter
@Setter
public class SecurityToken extends BaseEntity {
    @Getter
    public enum Type {
        ACCOUNT_ACTIVATION("Account Activation"),
        CHANGE_PASSWORD("Change Password"),
        PASSWORD_RESET("Password Reset");

        private final String label;

        Type(String label) {
            this.label = label;
        }
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(columnDefinition = "LONGTEXT")
    private String additionalInfo;

    public SecurityToken(User user, Type type) {
        this.user = user;
        this.type = type;
    }
}
