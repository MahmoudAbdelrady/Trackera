package com.mdevs.trackera.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(indexes = {@Index(columnList = "_KEY")})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserPreference extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @Column(nullable = false, name = "_KEY")
    private String key;

    @Column(columnDefinition = "LONGTEXT")
    private String value;
}
