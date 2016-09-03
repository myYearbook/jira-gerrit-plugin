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

import com.atlassian.core.user.preferences.Preferences;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryHandler;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.Authentication;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.SshException;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IssueReviewsImpl implements IssueReviewsManager {
    private static final Logger log = LoggerFactory.getLogger(IssueReviewsImpl.class);
    private final Map<String, List<GerritChange>> lruCache;

    private GerritConfiguration configuration;

    private IssueManager jiraIssueManager;

    public IssueReviewsImpl(GerritConfiguration configuration, IssueManager jiraIssueManager) {
        this.configuration = configuration;
        this.jiraIssueManager = jiraIssueManager;
        this.lruCache = IssueReviewsCache.getCache();
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
                log.debug("Getting issues from cache");
                changes = lruCache.get(key);
            } else {
                log.debug("Getting issues from Gerrit");
                changes = getReviewsFromGerrit(String.format(configuration.getIssueSearchQuery(), key));
                lruCache.put(key, changes);
            }

            gerritChanges.addAll(changes);
        }

        return gerritChanges;
    }

    protected List<GerritChange> getReviewsFromGerrit(String searchQuery) throws GerritQueryException {
        List<GerritChange> changes;

        if (!configuration.isSshValid()) {
            // return Collections.emptyList();
            throw new GerritConfiguration.NotConfiguredException("Not configured for SSH access");
        }

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

            if (log.isDebugEnabled()) {
                log.trace("doApprovals " + issueKey + ", " + changes + ", " + args + "; result=" + commandResult);
            }

            // Something probably changed!
            lruCache.remove(issueKey);
        }

        return result;
    }
}
