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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryHandler;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.Authentication;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.SshException;

/**
 * An {@link IssueTabPanel2 issue tab panel} for displaying all Gerrit code reviews related to this
 * issue.
 * 
 * @author Joe Hansche <jhansche@myyearbook.com>
 */
public class GerritReviewsTabPanel extends AbstractIssueTabPanel2 implements IssueTabPanel2 {
    private static final Logger log = LoggerFactory.getLogger(GerritReviewsTabPanel.class);
    private static final String GERRIT_SEARCH = "message:%1$s";

    private final DateTimeFormatterFactory dateTimeFormatterFactory;
    private final UserManager userManager;
    private final ApplicationProperties applicationProperties;
    private final GerritConfiguration configuration;

    public GerritReviewsTabPanel(UserManager userManager, DateTimeFormatterFactory dateTimeFormatterFactory,
            ApplicationProperties applicationProperties, GerritConfiguration configuration) {
        this.userManager = userManager;
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
        this.applicationProperties = applicationProperties;
        this.configuration = configuration;
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
        List<JSONObject> reviews;

        try {
            reviews = getReviews(issueKey);
        } catch (GerritQueryException exc) {
            exc.printStackTrace();
            issueActions.add(new GenericMessageAction(exc.getMessage()));
            return issueActions;
        }

        for (JSONObject obj : reviews) {
            if (obj.has("type") && "stats".equalsIgnoreCase(obj.getString("type"))) {
                continue;
            }

            issueActions.add(new GerritReviewIssueAction(obj, userManager, dateTimeFormatterFactory, applicationProperties.getBaseUrl()));
            // issueActions.add(new GenericMessageAction("<pre>" + obj.toString(4) + "</pre>"));
        }

        return issueActions;
    }

    /**
     * Get all Gerrit reviews related to the {@link Issue#getKey() issue key}.
     * 
     * @param issueKey
     * @return A list of {@link JSONObject}s, as retrieved from Gerrit.
     * @throws GerritQueryException If any failure occurs while querying the Gerrit server.
     * @see GerritQueryHandler
     */
    private List<JSONObject> getReviews(String issueKey) throws GerritQueryException {
        Authentication auth = new Authentication(configuration.getSshPrivateKey(), configuration.getSshUsername());
        GerritQueryHandler h = new GerritQueryHandler(configuration.getSshHostname(), configuration.getSshPort(), auth);

        try {
            return h.queryJava(String.format(GERRIT_SEARCH, issueKey), false, true, false);
        } catch (SshException e) {
            e.printStackTrace();
            throw new GerritQueryException("An ssh error occurred while querying for reviews.", e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new GerritQueryException("An error occurred while querying for reviews.", e);
        }
    }

    private boolean isConfigurationReady() {
        return configuration.getSshHostname() != null && configuration.getSshUsername() != null
                && configuration.getSshPrivateKey() != null && configuration.getSshPrivateKey().exists();
    }
}
