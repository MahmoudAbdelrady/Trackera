package com.mdevs.trackera.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

@Entity
@Table(indexes = {@Index(columnList = "TASK_NAME")})
@Audited
@Getter
@Setter
@NoArgsConstructor
public class WorkLogDetail extends BaseEntity {
    @Column(nullable = false)
    private String taskName;

    private String taskUrl;

    @Column(nullable = false, columnDefinition = "TIME(0)")
    private LocalTime startTime;

    @Column(nullable = false, columnDefinition = "TIME(0)")
    private LocalTime endTime;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal duration;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String description;

    @Column(nullable = false)
    @ColumnDefault("0")
    private boolean synced = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private WorkLog workLog;

    public void setDuration(BigDecimal duration) {
        this.duration = duration.setScale(3, RoundingMode.HALF_UP);
    }
}
