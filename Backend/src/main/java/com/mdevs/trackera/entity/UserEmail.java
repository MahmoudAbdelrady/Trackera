package com.mdevs.trackera.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(indexes = {@Index(columnList = "USER_ID, EMAIL"), @Index(columnList = "EMAIL")})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserEmail extends BaseEntity {
    @ManyToOne(optional = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean verified = false;
}
