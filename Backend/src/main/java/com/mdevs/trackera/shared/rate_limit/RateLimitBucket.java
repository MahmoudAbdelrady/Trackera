package com.mdevs.trackera.shared.rate_limit;

import io.github.bucket4j.Bucket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitBucket {
    private Bucket bucket;

    private volatile long lastAccessed; // volatile to ensure visibility across threads

    public void updateLastAccess() {
        this.lastAccessed = System.currentTimeMillis();
    }
}
