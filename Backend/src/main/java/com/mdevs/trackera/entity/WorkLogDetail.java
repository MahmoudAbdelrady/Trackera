package com.mdevs.trackera.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

@Entity
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

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal duration;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkLog.Status status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private WorkLog workLog;

    public void setDuration(BigDecimal duration) {
        this.duration = duration.setScale(2, RoundingMode.HALF_UP);
    }
}
