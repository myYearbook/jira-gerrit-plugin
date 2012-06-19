/*
 * Copyright 2012 MeetMe, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.meetme.plugins.jira.gerrit.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryHandler;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.Authentication;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.SshException;

public class IssueReviewsImpl implements IssueReviewsManager {
    private static final Logger log = LoggerFactory.getLogger(IssueReviewsImpl.class);

    private static final String GERRIT_SEARCH = "message:%1$s";
    private static final int CACHE_CAPACITY = 20;

    /** Number of milliseconds an item may stay in cache: 30 seconds */
    private static final long CACHE_EXPIRATION = 30000;

    private GerritConfiguration configuration;

    /**
     * LRU (least recently used) Cache object to avoid slamming the Gerrit server too many times.
     * 
     * XXX: This might result in an issue using a stale cache for reviews that change often, but
     * corresponding issues viewed rarely!
     */
    protected static final Map<String, List<GerritChange>> lruCache = Collections.synchronizedMap(new TimedCache(CACHE_CAPACITY, CACHE_EXPIRATION));

    public IssueReviewsImpl(GerritConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public List<GerritChange> getReviews(String issueKey) throws GerritQueryException {
        List<GerritChange> changes;

        if (lruCache.containsKey(issueKey)) {
            changes = lruCache.get(issueKey);
        } else {
            changes = getReviewsFromGerrit(issueKey);
            lruCache.put(issueKey, changes);
        }

        return changes;
    }

    private List<GerritChange> getReviewsFromGerrit(String issueKey) throws GerritQueryException {
        List<GerritChange> changes;
        Authentication auth = new Authentication(configuration.getSshPrivateKey(), configuration.getSshUsername());
        GerritQueryHandler query = new GerritQueryHandler(configuration.getSshHostname(), configuration.getSshPort(), auth);
        List<JSONObject> reviews;

        try {
            reviews = query.queryJava(String.format(GERRIT_SEARCH, issueKey), false, true, false);
        } catch (SshException e) {
            throw new GerritQueryException("An ssh error occurred while querying for reviews.", e);
        } catch (IOException e) {
            throw new GerritQueryException("An error occurred while querying for reviews.", e);
        }

        changes = new ArrayList<GerritChange>(reviews.size());

        for (JSONObject obj : reviews) {
            if (obj.has("type") && "stats".equalsIgnoreCase(obj.getString("type"))) {
                // The final JSON object in the query results is just a set of statistics
                if (log.isDebugEnabled()) {
                    log.trace("Results from QUERY: " + obj.optString("rowCount", "(unknown)") + " rows; runtime: "
                            + obj.optString("runTimeMilliseconds", "(unknown)") + " ms");
                }
                continue;
            }

            changes.add(new GerritChange(obj));
        }

        return changes;
    }

    @Override
    public void doApproval(String issueKey, GerritChange change, String args) throws IOException {
        GerritCommand command = new GerritCommand(configuration);
        command.doReview(change, args);

        // Something probably changed!
        lruCache.remove(issueKey);
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
         * @param key
         * @return
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
