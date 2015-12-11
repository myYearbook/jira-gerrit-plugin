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
package com.meetme.plugins.jira.gerrit.workflow.function;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.core.user.preferences.Preferences;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.preferences.UserPreferencesManager;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.meetme.plugins.jira.gerrit.data.GerritConfiguration;
import com.meetme.plugins.jira.gerrit.data.IssueReviewsManager;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.meetme.plugins.jira.gerrit.workflow.condition.ApprovalScore;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;

/**
 * <p>
 * A Workflow Function that can be used to perform Gerrit approvals as the result of a workflow
 * transition. The input argument is simply a command line argument string (such as "--verified +1",
 * or "--submit", etc). The argument will be appended to the <tt>gerrit review [ChangeId] ...</tt>
 * command line.
 * </p>
 *
 * <p>
 * This function can be used in combination with {@link ApprovalScore} workflow conditions, such
 * that, e.g., a "Merge Change" workflow transition can be used to automatically "submit" a Gerrit
 * review, iff all of the following conditions are met:
 * <ul>
 * <li>MUST have a Code-Review score &gt;= 2</li>
 * <li>MUST have a Verified score &gt;= 1</li>
 * <li>Must NOT have a Code-Review score &lt; 0</li>
 * </ul>
 * </p>
 *
 * <p>
 * This ensures that the workflow transition is only available if the "submit" step will be
 * successful.
 * <p>
 *
 * <p>
 * Another common use for this function would be to automatically provide a "Verified +1" score, via
 * another workflow step, e.g., "Ready for Merge". In that way, a "Ready for Merge" transition may
 * then automatically enable the "Merge Change" transition, as a result of giving the Verified +1
 * score.
 * </p>
 *
 * @author jhansche
 */
public class ApprovalFunction extends AbstractJiraFunctionProvider {
    private static final Logger log = LoggerFactory.getLogger(ApprovalFunction.class);

    public static final String KEY_CMD_ARGS = "cmdArgs";
    public static final String DEFAULT_CMD_ARGS = "--verified 1 --submit";

    private final IssueReviewsManager reviewsManager;
    private final GerritConfiguration configuration;
    private final UserPreferencesManager prefsManager;

    public ApprovalFunction(GerritConfiguration configuration, IssueReviewsManager reviewsManager, UserPreferencesManager prefsManager) {
        super();

        this.configuration = configuration;
        this.reviewsManager = reviewsManager;
        this.prefsManager = prefsManager;
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") Map transientVars, @SuppressWarnings("rawtypes") Map args, PropertySet ps)
            throws WorkflowException {
        if (!isConfigurationReady()) {
            throw new IllegalStateException("Configure the Gerrit integration from the Administration panel first.");
        }

        final String issueKey = getIssueKey(transientVars);
        final List<GerritChange> issueReviews = getReviews(issueKey);
        final Preferences prefs = getUserPrefs(transientVars, args);
        final String cmdArgs = (String) args.get(KEY_CMD_ARGS);

        boolean success = false;

        try {
            success = reviewsManager.doApprovals(issueKey, issueReviews, cmdArgs, prefs);
        } catch (IOException e) {
            throw new WorkflowException("An error occurred while approving the changes", e);
        }

        if (!success) {
            log.warn("doApprovals() returned false!");
            // throw new WorkflowException("Gerrit failed to perform the approvals!");
        }
    }

    protected Preferences getUserPrefs(@SuppressWarnings("rawtypes") Map transientVars, @SuppressWarnings("rawtypes") Map args) {
        final ApplicationUser user = getCaller(transientVars, args);
        return prefsManager.getPreferences(user);
    }

    protected String getIssueKey(@SuppressWarnings("rawtypes") Map transientVars) {
        return getIssue(transientVars).getKey();
    }

    protected List<GerritChange> getReviews(String issueKey) throws WorkflowException {
        try {
            return reviewsManager.getReviewsForIssue(issueKey);
        } catch (GerritQueryException e) {
            throw new WorkflowException("Unable to retrieve associated reviews", e);
        }
    }

    protected boolean isConfigurationReady() {
        return configuration != null && configuration.getSshHostname() != null && configuration.getSshUsername() != null
                && configuration.getSshPrivateKey() != null && configuration.getSshPrivateKey().exists();
    }
}
