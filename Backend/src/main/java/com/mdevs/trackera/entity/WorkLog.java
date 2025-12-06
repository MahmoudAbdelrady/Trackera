package com.mdevs.trackera.entity;

import com.mdevs.trackera.shared.enums.WorkLogEvaluation;
import com.mdevs.trackera.shared.enums.WorkLogStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDate;

@Entity
@Table(indexes = {@Index(columnList = "NAME"), @Index(columnList = "TOTAL_MINUTES"), @Index(columnList = "WORK_DATE"), @Index(columnList = "STATUS"),
        @Index(columnList = "UUID"), @Index(columnList = "USER_ID, UUID")})
@Audited
@Getter
@Setter
@NoArgsConstructor
public class WorkLog extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer totalMinutes;

    @Column(nullable = false)
    private LocalDate workDate;

    @Transient
    private WorkLogEvaluation evaluation;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkLogStatus status = WorkLogStatus.NOT_SYNCED;

    @ManyToOne(fetch =  FetchType.LAZY, optional = false)
    private User user;

    public WorkLogEvaluation getEvaluation() {
        return WorkLogEvaluation.fromTotalMinutes(totalMinutes);
    }
}
