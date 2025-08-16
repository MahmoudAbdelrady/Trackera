package com.mdevs.trackera.entity;

import com.mdevs.trackera.shared.oauth_provider.OAuthProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(indexes = {@Index(columnList = "USER_ID, PROVIDER")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserOAuthProvider extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;
}
