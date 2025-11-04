package com.mdevs.trackera.entity;

import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {@Index(columnList = "USER_ID, PROVIDER"), @Index(columnList = "EMAIL")})
@Getter
@Setter
@NoArgsConstructor
public class UserOAuthProvider extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;

    @ManyToOne(optional = false)
    private UserEmail providerEmail;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String accessToken;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String refreshToken;

    @Column(nullable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime accessTokenExpiry;

    @Column(nullable = false)
    private boolean isRevoked = false;

    public UserOAuthProvider(User user, OAuthProvider provider) {
        this.user = user;
        this.provider = provider;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(accessTokenExpiry);
    }
}
