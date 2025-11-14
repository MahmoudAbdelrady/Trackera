package com.mdevs.trackera.entity;

import com.mdevs.trackera.shared.enums.UserPreferenceOption;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(indexes = {@Index(columnList = "_OPTION")})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserPreference extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @Column(nullable = false, name = "_OPTION")
    @Enumerated(EnumType.STRING)
    private UserPreferenceOption option;

    @Column(columnDefinition = "LONGTEXT")
    private String value;
}
