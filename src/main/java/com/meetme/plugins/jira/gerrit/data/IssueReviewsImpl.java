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

import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheException;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.core.user.preferences.Preferences;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class IssueReviewsImpl implements IssueReviewsManager {
    private static final Logger log = LoggerFactory.getLogger(IssueReviewsImpl.class);

    private final Cache<String, List<GerritChange>> cache;

    private GerritConfiguration configuration;

    private IssueManager jiraIssueManager;

    public IssueReviewsImpl(
            GerritConfiguration configuration,
            IssueManager jiraIssueManager,
            CacheManager cacheManager,
            IssueReviewsCacheLoader cacheLoader
    ) {
        this.configuration = configuration;
        this.jiraIssueManager = jiraIssueManager;
        this.cache = cacheManager.getCache(
                IssueReviewsManager.class.getName() + ".issueChanges.cache",
                cacheLoader, //new IssueReviewsCacheLoader(configuration),
                new CacheSettingsBuilder()
                        .flushable()
                        .statisticsEnabled()
                        .maxEntries(100)
                        .replicateAsynchronously()
                        .expireAfterAccess(30, TimeUnit.MINUTES)
                        .build()
        );
    }

    @Override
    public Set<String> getIssueKeys(Issue issue) {
        return jiraIssueManager.getAllIssueKeys(issue.getId());
    }

    @Override
    public List<GerritChange> getReviewsForIssue(Issue issue) throws GerritQueryException {
        List<GerritChange> gerritChanges = new ArrayList<>();

        Set<String> allIssueKeys = getIssueKeys(issue);
        for (String key : allIssueKeys) {
            try {
                List<GerritChange> changes = cache.get(key);
                if (changes != null) gerritChanges.addAll(changes);
            } catch (CacheException exc) {
                if (exc.getCause() instanceof GerritQueryException) {
                    // TODO: is this really necessary?
                    // If we swallow the error, then there's no indication on the UI that an error occurred.
                    // The CacheLoader has to wrap the underlying exception in CacheException in order to throw it.
                    throw (GerritQueryException) exc.getCause();
                }

                log.error("Error fetching from cache", exc);
                throw exc;
            }
        }

        return gerritChanges;
    }

    @Override
    public boolean doApprovals(Issue issue, List<GerritChange> changes, String args, Preferences prefs) throws IOException {
        Set<String> issueKeys = getIssueKeys(issue);

        boolean result = true;
        for (String issueKey : issueKeys) {
            GerritCommand command = new GerritCommand(configuration, prefs);

            boolean commandResult = command.doReviews(changes, args);
            result &= commandResult;

            if (log.isDebugEnabled()) {
                log.trace("doApprovals " + issueKey + ", " + changes + ", " + args + "; result=" + commandResult);
            }

            // Something probably changed!
            cache.remove(issueKey);
        }

        return result;
    }
}
