package com.mdevs.trackera.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.Date;

@Entity
@Table(indexes = {@Index(columnList = "USER_ID, TYPE"), @Index(columnList = "TYPE"), @Index(columnList = "EXPIRY_DATE")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserInvalidToken extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @Lob
    @Column(nullable = false)
    private String token;

    @Column(nullable = false, columnDefinition = "TIMESTAMP(0)")
    private Date expiryDate;

    @Column(nullable = false)
    @ColumnDefault("1")
    private boolean isAccessToken = true;
}
