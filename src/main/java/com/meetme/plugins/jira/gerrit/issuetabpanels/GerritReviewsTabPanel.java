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
package com.meetme.plugins.jira.gerrit.issuetabpanels;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel2;
import com.atlassian.jira.plugin.issuetabpanel.GetActionsReply;
import com.atlassian.jira.plugin.issuetabpanel.GetActionsRequest;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanel2;
import com.atlassian.jira.plugin.issuetabpanel.ShowPanelReply;
import com.atlassian.jira.plugin.issuetabpanel.ShowPanelRequest;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.meetme.plugins.jira.gerrit.data.GerritConfiguration;
import com.meetme.plugins.jira.gerrit.data.IssueReviewsManager;
import com.meetme.plugins.jira.gerrit.data.dto.GerritApproval;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;

/**
 * An {@link IssueTabPanel2 issue tab panel} for displaying all Gerrit code reviews related to this
 * issue.
 * 
 * @author Joe Hansche <jhansche@myyearbook.com>
 */
public class GerritReviewsTabPanel extends AbstractIssueTabPanel2 implements IssueTabPanel2 {
    private static final Logger log = LoggerFactory.getLogger(GerritReviewsTabPanel.class);

    private final DateTimeFormatterFactory dateTimeFormatterFactory;
    private final UserManager userManager;
    private final ApplicationProperties applicationProperties;
    private final GerritConfiguration configuration;
    private final IssueReviewsManager reviewsManager;

    public GerritReviewsTabPanel(UserManager userManager, DateTimeFormatterFactory dateTimeFormatterFactory,
            ApplicationProperties applicationProperties, GerritConfiguration configuration,
            IssueReviewsManager reviewsManager) {
        this.userManager = userManager;
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
        this.applicationProperties = applicationProperties;
        this.configuration = configuration;
        this.reviewsManager = reviewsManager;
    }

    @Override
    protected void init() {
        log.debug("Initializing Gerrit Reviews tab panel");
    }

    @Override
    public GetActionsReply getActions(GetActionsRequest request) {
        List<IssueAction> issueActions;

        if (configuration.getSshHostname() == null || configuration.getSshUsername() == null || configuration.getSshPrivateKey() == null) {
            // Show not-configured error.
            issueActions = new ArrayList<IssueAction>();
            issueActions.add(new GenericMessageAction("Configure Gerrit in Administration interface first."));
        } else {
            // List of items we will be showing in the tab panel.
            issueActions = getActions(request.issue().getKey());
        }
        return GetActionsReply.create(issueActions);
    }

    /**
     * Whether this panel should show up in the view-issue page.
     * 
     * Current implementation only shows the tab if the issue is editable (e.g., not closed)
     */
    @Override
    public ShowPanelReply showPanel(ShowPanelRequest request) {
        boolean isShowing = true;

        if (!isConfigurationReady())
        {
            isShowing = false;
        }

        return ShowPanelReply.create(isShowing);
    }

    /**
     * Get all {@link GerritReviewIssueAction}s related to the specified {@link Issue#getKey() issue
     * key}.
     * 
     * @param issueKey
     * @return
     */
    private List<IssueAction> getActions(String issueKey) {
        log.debug("Getting actions for issue: {0}", issueKey);

        List<IssueAction> issueActions = new ArrayList<IssueAction>();
        List<GerritChange> reviews;

        try {
            reviews = reviewsManager.getReviews(issueKey);
        } catch (GerritQueryException exc) {
            exc.printStackTrace();
            issueActions.add(new GenericMessageAction(exc.getMessage()));
            return issueActions;
        }

        for (GerritChange change : reviews) {
            for (GerritApproval approval : change.getPatchSet().getApprovals()) {
                String byEmail = approval.getByEmail();
                approval.setUser(getUserByEmail(byEmail));
            }
            issueActions.add(new GerritReviewIssueAction(descriptor(), change,
                    userManager, dateTimeFormatterFactory, applicationProperties.getBaseUrl()));
            // issueActions.add(new GenericMessageAction("<pre>" + obj.toString(4) + "</pre>"));
        }

        return issueActions;
    }

    private User getUserByEmail(String email) {
        User user = null;

        if (email != null) {
            for (User iUser : userManager.getUsers()) {
                if (email.equalsIgnoreCase(iUser.getEmailAddress()))
                {
                    user = iUser;
                    break;
                }
            }
        }

        return user;
    }

    private boolean isConfigurationReady() {
        return configuration.getSshHostname() != null && configuration.getSshUsername() != null
                && configuration.getSshPrivateKey() != null && configuration.getSshPrivateKey().exists();
    }
}
