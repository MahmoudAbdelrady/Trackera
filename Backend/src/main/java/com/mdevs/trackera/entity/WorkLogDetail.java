package com.mdevs.trackera.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalTime;

@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
public class WorkLogDetail extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private WorkLog workLog;

    @Column(nullable = false)
    private String taskName;

    @Column(nullable = false)
    private String taskUrl;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Double duration;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkLog.Status status;
}
