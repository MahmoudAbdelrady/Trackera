package com.mdevs.trackera.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {@Index(columnList = "EXPIRY_DATE"), @Index(columnList = "USER_ID, TOKEN_FINGERPRINT, IS_ACCESS_TOKEN")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserInvalidToken extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @Column(nullable = false, length = 64, unique = true)
    private String tokenFingerprint;

    @Column(nullable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean isAccessToken = true;
}
