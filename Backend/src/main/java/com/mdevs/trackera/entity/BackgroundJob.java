package com.mdevs.trackera.entity;

import com.mdevs.trackera.shared.enums.BackgroundJobStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.envers.Audited;

@Audited
@Entity
@Table(indexes = {@Index(columnList = "NAME")})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BackgroundJob extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BackgroundJobStatus status;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int retryCount;

    private String failureReason;

    @Column(columnDefinition = "LONGTEXT")
    private String failureStackTrace;
}
