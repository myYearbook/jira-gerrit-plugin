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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.SimpleLinkFactory;
import com.atlassian.jira.plugin.webfragment.descriptors.SimpleLinkFactoryModuleDescriptor;
import com.atlassian.jira.plugin.webfragment.model.SimpleLink;
import com.atlassian.jira.plugin.webfragment.model.SimpleLinkImpl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.atlassian.jira.util.velocity.VelocityRequestContext;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.jira.util.velocity.VelocityRequestSession;
import com.meetme.plugins.jira.gerrit.SessionKeys;

public class IssueTypeOptionsFactory implements SimpleLinkFactory
{
    private static final Logger log = LoggerFactory.getLogger(IssueTypeOptionsFactory.class);

    public static final String ISSUE_ONLY = "IssueOnly";
    public static final String SUBTASK_ONLY = "SubtaskOnly";
    public static final String ALL_ISSUES = "All";

    public static final String DEFAULT_ISSUE_TYPE = ISSUE_ONLY;

    private VelocityRequestContextFactory requestContextFactory;
    private JiraAuthenticationContext authenticationContext;

    public IssueTypeOptionsFactory(VelocityRequestContextFactory requestContextFactory, JiraAuthenticationContext authenticationContext) {
        this.requestContextFactory = requestContextFactory;
        this.authenticationContext = authenticationContext;
    }

    @Override
    public void init(SimpleLinkFactoryModuleDescriptor arg0) {
    }

    @Override
    public List<SimpleLink> getLinks(ApplicationUser user, Map<String, Object> params) {
        final VelocityRequestContext requestContext = requestContextFactory.getJiraVelocityRequestContext();
        final I18nHelper i18n = authenticationContext.getI18nHelper();
        final Issue issue = (Issue) params.get("issue");

        final VelocityRequestSession session = requestContext.getSession();
        final String baseUrl = requestContext.getBaseUrl();

        String issueType = (String) session.getAttribute(SessionKeys.VIEWISSUE_REVIEWS_ISSUETYPE);

        if (StringUtils.isEmpty(issueType) || issue.getSubTaskObjects().isEmpty()) {
            issueType = DEFAULT_ISSUE_TYPE;
        }

        final SimpleLink issueOnlyLink = new SimpleLinkImpl("reviews-issuetype-issueonly",
                i18n.getText("gerrit-reviews-left-panel.options.issuetype.issue_only"), null, null,
                getStyleFor(issueType, ISSUE_ONLY),
                getUrlForType(ISSUE_ONLY, baseUrl, issue), null);

        if (issue.getSubTaskObjects().isEmpty()) {
            // Contains no subtasks, so no reason to show the others
            return CollectionBuilder.list(issueOnlyLink);
        }

        // Contains subtasks, expose the other options now
        final SimpleLink subtaskOnlyLink = new SimpleLinkImpl("reviews-issuetype-subtasksonly",
                i18n.getText("gerrit-reviews-left-panel.options.issuetype.subtasks_only"), null, null,
                getStyleFor(issueType, SUBTASK_ONLY),
                getUrlForType(SUBTASK_ONLY, baseUrl, issue), null);

        final SimpleLink allLink = new SimpleLinkImpl("reviews-issuetype-all",
                i18n.getText("gerrit-reviews-left-panel.options.issuetype.all"), null, null,
                getStyleFor(issueType, ALL_ISSUES),
                getUrlForType(ALL_ISSUES, baseUrl, issue), null);

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
