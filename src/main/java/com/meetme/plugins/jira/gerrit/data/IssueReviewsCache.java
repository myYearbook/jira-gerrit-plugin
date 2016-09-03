package com.meetme.plugins.jira.gerrit.data;

import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jhansche on 9/2/16.
 */
public class IssueReviewsCache {
    /** Max number of items to retain in the cache */
    private static final int CACHE_CAPACITY = 30;

    /** Number of milliseconds an item may stay in cache: 30 seconds */
    private static final long CACHE_EXPIRATION = 30000;

    /**
     * LRU (least recently used) Cache object to avoid slamming the Gerrit server too many times.
     * <p>
     * XXX: This might result in an issue using a stale cache for reviews that change often, but
     * corresponding issues viewed rarely! To account for that, we also have a cache expiration, so
     * that at least after the cache expires, it'll get back in sync.
     */
    protected static final Map<String, List<GerritChange>> lruCache = Collections.synchronizedMap(new TimedCache(CACHE_CAPACITY, CACHE_EXPIRATION));

    private static IssueReviewsCache sInstance;

    public static synchronized Map<String, List<GerritChange>> getCache() {
        return lruCache;
    }

    private static class TimedCache extends LinkedHashMap<String, List<GerritChange>> {
        private static final long serialVersionUID = 296909003142207307L;

        private final int capacity;
        private final Map<String, Long> timestamps;
        private final long expiration;

        public TimedCache(final int capacity, final long expiration) {
            super(capacity + 1, 1.0f, true);
            this.capacity = capacity;
            this.timestamps = new LinkedHashMap<String, Long>(capacity + 1, 1.0f, true);
            this.expiration = expiration;
        }

        /**
         * Returns true if the requested key has expired from the cache.
         *
         * @param key the key
         * @return whether the associated value exists and has expired
         */
        private boolean hasKeyExpired(Object key) {
            if (timestamps.containsKey(key)) {
                Long lastUpdatedTimestamp = timestamps.get(key);
                return lastUpdatedTimestamp <= System.currentTimeMillis() - expiration;
            }

            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            if (hasKeyExpired(key)) {
                this.remove(key);
                return false;
            }

            return super.containsKey(key);
        }

        @Override
        public List<GerritChange> get(Object key) {
            if (hasKeyExpired(key)) {
                this.remove(key);
                return null;
            }

            return super.get(key);
        }

        /**
         * Removes the eldest entry from the cache if 1) it exceeds the max capacity, or 2) it has
         * been in cache for too long.
         */
        @Override
        protected boolean removeEldestEntry(final Map.Entry<String, List<GerritChange>> eldest) {
            return super.size() > capacity || timestamps.get(eldest.getKey()) <= System.currentTimeMillis() - expiration;
        }

        @Override
        public List<GerritChange> put(String key, List<GerritChange> value) {
            timestamps.put(key, System.currentTimeMillis());
            return super.put(key, value);
        }

        @Override
        public List<GerritChange> remove(Object key) {
            timestamps.remove(key);
            return super.remove(key);
        }
    }
}
