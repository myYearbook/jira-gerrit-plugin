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
import com.atlassian.jira.project.Project;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryHandler;

import net.sf.json.JSONObject;

import java.io.IOException;
import java.util.List;

public interface IssueReviewsManager {

    /**
     * Gets all Gerrit reviews related to the {@link Issue#getKey() issue key}.
     *
     * @param issueKey the JIRA issue key
     * @return A list of {@link JSONObject}s, as retrieved from Gerrit.
     * @throws GerritQueryException If any failure occurs while querying the Gerrit server.
     * @see GerritQueryHandler
     */
    List<GerritChange> getReviewsForIssue(String issueKey) throws GerritQueryException;

    /**
     * Gets all Gerrit reviews related to the {@link Project#getKey() project}.
     *
     * @param projectKey the JIRA Project key (i.e., {@code JIRA-123}'s projectKey would be {@code JIRA})
     * @return A list of {@link JSONObject}s, as retrieved from Gerrit.
     * @throws GerritQueryException If any failure occurs while querying the Gerrit server.
     * @see GerritQueryHandler
     */
    List<GerritChange> getReviewsForProject(String projectKey) throws GerritQueryException;

    /**
     * Performs an approval/review of a change.
     *
     * @param issueKey the JIRA issue key
     * @param change the Gerrit change
     * @param args arguments to add to the approval
     * @param prefs the {@link Preferences} for the viewing user
     * @return whether the approval was successful
     * @throws IOException if so
     */
    boolean doApproval(String issueKey, GerritChange change, String args, Preferences prefs) throws IOException;

    /**
     * Performs approvals/reviews of all changes.
     *
     * @param issueKey the JIRA issue key
     * @param changes the set of Gerrit changes
     * @param args arguments to add to each approval
     * @param prefs the {@link Preferences} for the viewing user
     * @return whether the approvals were successful
     * @throws IOException if so
     */
    boolean doApprovals(String issueKey, List<GerritChange> changes, String args, Preferences prefs) throws IOException;
}
