package com.mdevs.trackera.entity;

import com.mdevs.trackera.shared.enums.BaseEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(indexes = {@Index(columnList = "NAME"), @Index(columnList = "TOTAL_HOURS"), @Index(columnList = "WORK_DATE"), @Index(columnList = "STATUS"),
        @Index(columnList = "UUID"), @Index(columnList = "USER_ID, UUID")})
@Audited
@Getter
@Setter
@NoArgsConstructor
public class WorkLog extends BaseEntity {
    @Getter
    @AllArgsConstructor
    public enum Evaluation implements BaseEnum {
        EXCELLENT("Excellent"),
        GOOD("Good"),
        MODERATE("Moderate"),
        POOR("Poor");

        private final String label;

        public static Evaluation fromTotalHours(BigDecimal totalHours) {
            if (totalHours.compareTo(BigDecimal.valueOf(8)) >= 0) {
                return EXCELLENT;
            } else if (totalHours.compareTo(BigDecimal.valueOf(7.5)) >= 0) {
                return GOOD;
            } else if (totalHours.compareTo(BigDecimal.valueOf(7)) >= 0) {
                return MODERATE;
            } else {
                return POOR;
            }
        }
    }

    @Getter
    @AllArgsConstructor
    public enum Status implements BaseEnum {
        SYNCED("Synced"),
        PARTIALLY("Partially"),
        NOT_SYNCED("Not Synced");

        private final String label;
    }

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal totalHours;

    @Column(nullable = false)
    private LocalDate workDate;

    @Transient
    private Evaluation evaluation;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne(fetch =  FetchType.LAZY, optional = false)
    private User user;

    public Evaluation getEvaluation() {
        return Evaluation.fromTotalHours(totalHours);
    }

    public void setTotalHours(BigDecimal totalHours) {
        this.totalHours = totalHours.setScale(3, RoundingMode.HALF_UP);
    }
}
