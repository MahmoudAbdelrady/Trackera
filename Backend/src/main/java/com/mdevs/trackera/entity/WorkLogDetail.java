package com.mdevs.trackera.entity;

import com.mdevs.trackera.shared.enums.WorkLogStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.envers.Audited;

import java.time.LocalTime;

@Entity
@Table(indexes = {@Index(columnList = "TASK_NAME"), @Index(columnList = "UUID")})
@Audited
@Getter
@Setter
@NoArgsConstructor
public class WorkLogDetail extends BaseEntity {
    @Column(nullable = false)
    private String taskName;

    @Column(nullable = false, columnDefinition = "TIME(0)")
    private LocalTime startTime;

    @Column(nullable = false, columnDefinition = "TIME(0)")
    private LocalTime endTime;

    @Column(nullable = false)
    private Integer duration;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkLogStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private WorkLog workLog;

    private String jiraId;
}
