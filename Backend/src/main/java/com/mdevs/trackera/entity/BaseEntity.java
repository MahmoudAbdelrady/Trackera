package com.mdevs.trackera.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Audited
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class BaseEntity {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String uuid;

    @Version
    @Column(nullable = false)
    private long version = 0;

    @Column(nullable = false, columnDefinition = "TIMESTAMP(0)")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "TIMESTAMP(0)")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.uuid = UuidCreator.getTimeBased().toString();
    }
}
