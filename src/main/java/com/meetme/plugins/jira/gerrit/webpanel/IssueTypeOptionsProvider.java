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
package com.meetme.plugins.jira.gerrit.webpanel;

import com.meetme.plugins.jira.gerrit.SessionKeys;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.atlassian.jira.util.velocity.VelocityRequestContext;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.jira.util.velocity.VelocityRequestSession;
import com.atlassian.plugin.web.api.WebItem;
import com.atlassian.plugin.web.api.model.WebFragmentBuilder;
import com.atlassian.plugin.web.api.provider.WebItemProvider;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class IssueTypeOptionsProvider implements WebItemProvider {
    private static final Logger log = LoggerFactory.getLogger(IssueTypeOptionsProvider.class);

    public static final String ISSUE_ONLY = "IssueOnly";
    public static final String SUBTASK_ONLY = "SubtaskOnly";
    public static final String ALL_ISSUES = "All";

    public static final String DEFAULT_ISSUE_TYPE = ISSUE_ONLY;

    private VelocityRequestContextFactory requestContextFactory;
    private JiraAuthenticationContext authenticationContext;

    public IssueTypeOptionsProvider(VelocityRequestContextFactory requestContextFactory, JiraAuthenticationContext authenticationContext) {
        this.requestContextFactory = requestContextFactory;
        this.authenticationContext = authenticationContext;
    }

    @Override
    public Iterable<WebItem> getItems(Map<String, Object> params) {
        final VelocityRequestContext requestContext = requestContextFactory.getJiraVelocityRequestContext();
        final I18nHelper i18n = authenticationContext.getI18nHelper();
        final Issue issue = (Issue) params.get("issue");

        final VelocityRequestSession session = requestContext.getSession();
        final String baseUrl = requestContext.getBaseUrl();

        String issueType = (String) session.getAttribute(SessionKeys.VIEWISSUE_REVIEWS_ISSUETYPE);

        if (StringUtils.isEmpty(issueType) || issue.getSubTaskObjects().isEmpty()) {
            issueType = DEFAULT_ISSUE_TYPE;
        }

        int weight = 10;
        WebItem issueOnlyLink = new WebFragmentBuilder(weight += 10)
                .id("reviews-issuetype-issueonly")
                .label(i18n.getText("gerrit-reviews-left-panel.options.issuetype.issue_only"))
                .styleClass(getStyleFor(issueType, ISSUE_ONLY))
                .webItem("issuetype-view-options")
                .url(getUrlForType(ISSUE_ONLY, baseUrl, issue))
                .build();

        if (issue.getSubTaskObjects().isEmpty()) {
            // Contains no subtasks, so no reason to show the others
            return CollectionBuilder.list(issueOnlyLink);
        }

        // Contains subtasks, expose the other options now
        final WebItem subtaskOnlyLink = new WebFragmentBuilder(weight += 10)
                .id("reviews-issuetype-subtasksonly")
                .label(i18n.getText("gerrit-reviews-left-panel.options.issuetype.subtasks_only"))
                .styleClass(getStyleFor(issueType, SUBTASK_ONLY))
                .webItem("issuetype-view-options")
                .url(getUrlForType(SUBTASK_ONLY, baseUrl, issue))
                .build();

        final WebItem allLink = new WebFragmentBuilder(weight += 10)
                .id("reviews-issuetype-all")
                .label(i18n.getText("gerrit-reviews-left-panel.options.issuetype.all"))
                .styleClass(getStyleFor(issueType, ALL_ISSUES))
                .webItem("issuetype-view-options")
                .url(getUrlForType(ALL_ISSUES, baseUrl, issue))
                .build();

        return CollectionBuilder.list(issueOnlyLink, subtaskOnlyLink, allLink);
    }

    private String getUrlForType(String type, String baseUrl, Issue issue) {
        return baseUrl + "/browse/" + issue.getKey() + "?gerritIssueType=" + type
                + "#gerrit-reviews-left-panel";
    }

    private String getStyleFor(String type, String expecting) {
        return expecting.equals(type) ? "aui-list-checked aui-checked" : "aui-list-checked";
    }

    public static final boolean wantsSubtasks(final String gerritIssueType) {
        return SUBTASK_ONLY.equals(gerritIssueType) || ALL_ISSUES.equals(gerritIssueType);
    }

    public static final boolean wantsIssue(final String gerritIssueType) {
        return ISSUE_ONLY.equals(gerritIssueType) || ALL_ISSUES.equals(gerritIssueType);
    }
}
