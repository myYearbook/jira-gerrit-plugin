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
package com.meetme.plugins.jira.gerrit.tabpanel;

import com.meetme.plugins.jira.gerrit.data.GerritConfiguration;
import com.meetme.plugins.jira.gerrit.data.IssueReviewsManager;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.issuetabpanel.*;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SubtaskReviewsTabPanel extends AbstractIssueTabPanel2 implements IssueTabPanel2 {
    private final GerritConfiguration configuration;
    private final IssueReviewsManager reviewsManager;

    public SubtaskReviewsTabPanel(GerritConfiguration configuration,
            IssueReviewsManager reviewsManager) {
        this.configuration = configuration;
        this.reviewsManager = reviewsManager;
    }

    @Override
    public GetActionsReply getActions(GetActionsRequest request) {
        Collection<Issue> subtasks = request.issue().getSubTaskObjects();
        List<IssueAction> actions = new ArrayList<>();
        List<GerritChange> changes;

        for (Issue subtask : subtasks) {
            try {
                changes = getChanges(subtask);
            } catch (GerritQueryException e) {
                throw new RuntimeException(e);
            }

            actions.add(new SubtaskReviewsIssueAction(descriptor(), subtask, changes));
        }

        return GetActionsReply.create(actions);
    }

    @Override
    public ShowPanelReply showPanel(ShowPanelRequest request) {
        boolean show = false;

        if (isConfigurationReady()) {
            Collection<Issue> subtasks = request.issue().getSubTaskObjects();
            show = subtasks != null && subtasks.size() > 0;
        }

        return ShowPanelReply.create(show);
    }

    private List<GerritChange> getChanges(Issue subtask) throws GerritQueryException {
        return reviewsManager.getReviewsForIssue(subtask);
    }

    private boolean isConfigurationReady() {
        final GerritConfiguration configuration = this.configuration;

        return configuration != null && configuration.getSshHostname() != null && configuration.getSshUsername() != null
                && configuration.getSshPrivateKey() != null && configuration.getSshPrivateKey().exists();
    }
}
