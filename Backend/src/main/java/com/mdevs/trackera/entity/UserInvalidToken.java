package com.mdevs.trackera.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(indexes = {@Index(columnList = "USER_ID, TYPE"), @Index(columnList = "TYPE"), @Index(columnList = "EXPIRY_DATE")})
@Getter
@Setter
@NoArgsConstructor
public class UserInvalidToken extends BaseEntity {
    public enum Type {
        ACCESS,
        REFRESH
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @Lob
    @Column(nullable = false)
    private String token;

    @Column(nullable = false, columnDefinition = "TIMESTAMP(0)")
    private Date expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;
}
