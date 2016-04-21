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

import com.atlassian.core.user.preferences.Preferences;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryHandler;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.Authentication;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.SshException;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class IssueReviewsImpl implements IssueReviewsManager {
    private static final Logger log = LoggerFactory.getLogger(IssueReviewsImpl.class);

    /** Max number of items to retain in the cache */
    private static final int CACHE_CAPACITY = 30;

    /** Number of milliseconds an item may stay in cache: 30 seconds */
    private static final long CACHE_EXPIRATION = 30000;

    /**
     * LRU (least recently used) Cache object to avoid slamming the Gerrit server too many times.
     *
     * XXX: This might result in an issue using a stale cache for reviews that change often, but
     * corresponding issues viewed rarely! To account for that, we also have a cache expiration, so
     * that at least after the cache expires, it'll get back in sync.
     */
    protected static final Map<String, List<GerritChange>> lruCache = Collections.synchronizedMap(new TimedCache(CACHE_CAPACITY, CACHE_EXPIRATION));

    private GerritConfiguration configuration;

    private IssueManager jiraIssueManager;

    public IssueReviewsImpl(GerritConfiguration configuration, IssueManager jiraIssueManager) {
        this.configuration = configuration;
        this.jiraIssueManager = jiraIssueManager;
    }

    @Override
    public Set<String> getIssueKeys(Issue issue) {
        return jiraIssueManager.getAllIssueKeys(issue.getId());
    }

    @Override
    public List<GerritChange> getReviewsForIssue(Issue issue) throws GerritQueryException {
        List<GerritChange> gerritChanges = new ArrayList<GerritChange>();

        Set<String> allIssueKeys = getIssueKeys(issue);
        for (String key : allIssueKeys) {
            List<GerritChange> changes;

            if (lruCache.containsKey(key)) {
                changes = lruCache.get(key);
            } else {
                changes = getReviewsFromGerrit(String.format(configuration.getIssueSearchQuery(), key));
                lruCache.put(key, changes);
            }

            gerritChanges.addAll(changes);
        }

        return gerritChanges;
    }

    protected List<GerritChange> getReviewsFromGerrit(String searchQuery) throws GerritQueryException {
        List<GerritChange> changes;
        Authentication auth = new Authentication(configuration.getSshPrivateKey(), configuration.getSshUsername());
        GerritQueryHandler query = new GerritQueryHandler(configuration.getSshHostname(), configuration.getSshPort(), null, auth);
        List<JSONObject> reviews;

        try {
            reviews = query.queryJava(searchQuery, false, true, false);
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

        Collections.sort(changes);
        return changes;
    }

    @Override
    public boolean doApprovals(Issue issue, List<GerritChange> changes, String args, Preferences prefs) throws IOException {
        Set<String> issueKeys = getIssueKeys(issue);

        boolean result = true;
        for (String issueKey : issueKeys) {
            GerritCommand command = new GerritCommand(configuration, prefs);

            boolean commandResult = command.doReviews(changes, args);
            result &= commandResult;

            if (log.isDebugEnabled())
            {
                log.trace("doApprovals " + issueKey + ", " + changes + ", " + args + "; result=" + commandResult);
            }

            // Something probably changed!
            lruCache.remove(issueKey);
        }

        return result;
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
