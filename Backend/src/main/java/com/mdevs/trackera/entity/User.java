package com.mdevs.trackera.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Formula;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Audited
@Entity
@Table(indexes = {@Index(columnList = "EMAIL")})
@Getter
@Setter
public class User extends BaseEntity implements UserDetails {
    @Column(nullable = false)
    private String firstname;

    @Column(nullable = false)
    private String lastname;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    private String profilePicture;

    @Column(nullable = false)
    private String avatarColor;

    @Column(nullable = false)
    @ColumnDefault("0")
    private boolean isVerified = false;

    @Formula("EXISTS (SELECT 1 FROM USEROAUTHPROVIDERS uap WHERE uap.USER_ID = ID)")
    @NotAudited
    private boolean isOAuth;

    public User() {
        this.avatarColor = String.format("#%06x", (int) (Math.random() * 0xffffff));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return null;
    }

    public boolean canChangePassword() {
        return !isOAuth || isPasswordSet();
    }

    public boolean isPasswordSet() {
        return !StringUtils.isEmpty(password);
    }
}
