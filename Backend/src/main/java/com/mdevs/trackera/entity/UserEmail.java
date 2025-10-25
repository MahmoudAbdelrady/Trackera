package com.mdevs.trackera.entity;

import com.mdevs.trackera.shared.enums.EmailTag;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ColumnDefault;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(indexes = {@Index(columnList = "USER_ID, EMAIL"), @Index(columnList = "EMAIL")})
@NoArgsConstructor
@Getter
@Setter
public class UserEmail extends BaseEntity {
    @ManyToOne(optional = false)
    private User user;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean isPrimary = false;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean verified = false;

    @Column(columnDefinition = "LONGTEXT")
    private String tags;

    public UserEmail(User user, String email, boolean isPrimary, boolean verified) {
        this.user = user;
        this.email = email;
        this.isPrimary = isPrimary;
        this.verified = verified;
    }

    public UserEmail(User user, String email, boolean isPrimary, boolean verified, List<EmailTag> tags) {
        this.user = user;
        this.email = email;
        this.isPrimary = isPrimary;
        this.verified = verified;
        addTags(tags);
    }

    public void addTags(List<EmailTag> tags) {
        for (EmailTag tag : tags) {
            addTag(tag);
        }
    }

    public void addTag(EmailTag tag) {
        if (StringUtils.isEmpty(this.tags)) {
            this.tags = tag.name();
        } else {
            Set<String> existingTags = new HashSet<>(Set.of(this.tags.split(",")));
            existingTags.add(tag.name());
            this.tags = StringUtils.join(existingTags, ",");
        }
    }

    public void removeTag(EmailTag tag) {
        if (StringUtils.isEmpty(this.tags)) {
            return;
        }
        Set<String> existingTags = new HashSet<>(Set.of(this.tags.split(",")));
        existingTags.remove(tag.name());
        this.tags = StringUtils.join(existingTags, ",");
    }

    public boolean hasTag(EmailTag tag) {
        if (StringUtils.isEmpty(this.tags)) {
            return false;
        }
        Set<String> existingTags = new HashSet<>(Set.of(this.tags.split(",")));
        return existingTags.contains(tag.name());
    }

    public List<EmailTag> getTags() {
        return StringUtils.isEmpty(this.tags) ? List.of() : java.util.Arrays.stream(this.tags.split(",")).map(EmailTag::valueOf).toList();
    }
}
