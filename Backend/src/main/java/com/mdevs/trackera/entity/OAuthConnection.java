package com.mdevs.trackera.entity;

import com.mdevs.trackera.shared.enums.OAuthProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {@Index(columnList = "USER_ID, PROVIDER")})
@Getter
@Setter
@NoArgsConstructor
public class OAuthConnection extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;

    @ManyToOne(optional = false)
    private UserEmail accountEmail;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String accessToken;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String refreshToken;

    @Column(nullable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime accessTokenExpiry;

    @Column(nullable = false)
    private boolean isRevoked = false;

    public OAuthConnection(User user, OAuthProvider provider) {
        this.user = user;
        this.provider = provider;
    }

    public boolean isExpiringSoon() {
        return LocalDateTime.now().plusMinutes(1).isAfter(accessTokenExpiry); // 1 minute buffer
    }
}
