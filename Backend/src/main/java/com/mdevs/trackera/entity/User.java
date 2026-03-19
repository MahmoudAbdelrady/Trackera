package com.mdevs.trackera.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.envers.Audited;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Audited
@Entity
@Table(indexes = {@Index(columnList = "UUID")})
@Getter
@Setter
public class User extends BaseEntity implements UserDetails {
    @Column(nullable = false)
    private String firstname;

    @Column(nullable = false)
    private String lastname;

    @OneToOne
    private UserEmail primaryEmail;

    @OneToOne
    private UserEmail pendingEmail;

    private String password;

    private String profilePicture;

    @Column(nullable = false)
    private String avatarColor;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean isVerified = false;

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

    public boolean isPasswordSet() {
        return StringUtils.isNotEmpty(password);
    }

    public boolean isEmailLinked(UserEmail email) {
        return email.getId().equals(primaryEmail.getId()) || (pendingEmail != null && email.getId().equals(pendingEmail.getId()));
    }
}
