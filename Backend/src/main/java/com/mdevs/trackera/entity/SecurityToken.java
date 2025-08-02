package com.mdevs.trackera.entity;

import com.mdevs.trackera.config.AppConfig;
import com.mdevs.trackera.shared.utils.TrackeraHasher;
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
        CHANGE_EMAIL("Change Email"),
        CHANGE_PASSWORD("Change Password"),
        PASSWORD_RESET("Password Reset"),
        ACCOUNT_ACTIVATION("Account Activation");

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

    @Column(nullable = false, unique = true)
    private String token;

    @Lob
    private String additionalInfo;

    public SecurityToken(User user, Type type) {
        this.user = user;
        this.type = type;
        this.token = AppConfig.getApplicationContext().getBean(TrackeraHasher.class).hashForSecurityToken(user.getId(), type);
    }
}
