package com.mdevs.trackera.entity;

import com.mdevs.trackera.shared.BaseEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDate;

@Entity
@Table(indexes = {@Index(columnList = "NAME"), @Index(columnList = "TOTAL_HOURS"), @Index(columnList = "WORK_DATE"), @Index(columnList = "STATUS")})
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

        public static Evaluation fromTotalHours(Double totalHours) {
            if (totalHours >= 8) {
                return EXCELLENT;
            } else if (totalHours >= 7.5) {
                return GOOD;
            } else if (totalHours >= 7) {
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

    @Column(nullable = false)
    private Double totalHours;

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
}
