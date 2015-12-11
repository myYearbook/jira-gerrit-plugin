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

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

public class IssueStatusOptionsFactory implements SimpleLinkFactory
{
    private static final Logger log = LoggerFactory.getLogger(IssueStatusOptionsFactory.class);

    public static final String STATUS_OPEN = "Open";
    public static final String STATUS_ALL = "All";
    public static final String DEFAULT_STATUS = STATUS_OPEN;

    private VelocityRequestContextFactory requestContextFactory;
    private JiraAuthenticationContext authenticationContext;

    public IssueStatusOptionsFactory(VelocityRequestContextFactory requestContextFactory, JiraAuthenticationContext authenticationContext) {
        this.requestContextFactory = requestContextFactory;
        this.authenticationContext = authenticationContext;
    }

    @Override
    public void init(SimpleLinkFactoryModuleDescriptor descriptor) {
    }

    @Override
    public List<SimpleLink> getLinks(ApplicationUser user, Map<String, Object> params) {
        final VelocityRequestContext requestContext = requestContextFactory.getJiraVelocityRequestContext();
        final I18nHelper i18n = authenticationContext.getI18nHelper();
        final Issue issue = (Issue) params.get("issue");

        final VelocityRequestSession session = requestContext.getSession();
        final String baseUrl = requestContext.getBaseUrl();

        String issueStatus = (String) session.getAttribute(SessionKeys.VIEWISSUE_REVIEWS_ISSUESTATUS);

        if (issueStatus == null) {
            issueStatus = DEFAULT_STATUS;
        }

        if (issue.getSubTaskObjects().isEmpty() && isIssueOpen(issue)) {
            return Collections.emptyList();
        }

        final SimpleLink allLink = new SimpleLinkImpl("reviews-issuestatus-all",
                i18n.getText("gerrit-reviews-left-panel.options.issuestatus.all"), null, null,
                getStyleFor(issueStatus, STATUS_ALL),
                getUrlForType(STATUS_ALL, baseUrl, issue), null);

        final SimpleLink openLink = new SimpleLinkImpl("reviews-issuestatus-open",
                i18n.getText("gerrit-reviews-left-panel.options.issuestatus.open"), null, null,
                getStyleFor(issueStatus, STATUS_OPEN),
                getUrlForType(STATUS_OPEN, baseUrl, issue), null);

        return CollectionBuilder.list(allLink, openLink);
    }

    private String getUrlForType(String type, String baseUrl, Issue issue) {
        return baseUrl + "/browse/" + issue.getKey() + "?gerritIssueStatus=" + type + "#gerrit-reviews-left-panel";
    }

    private String getStyleFor(String type, String expecting) {
        return expecting.equals(type) ? "aui-list-checked aui-checked" : "aui-list-checked";
    }

    public static boolean isIssueOpen(Issue issue) {
        log.debug("Checking if " + issue.getKey() + " is open: " + issue.getResolutionObject());
        return issue.getResolutionObject() == null;
    }

    public static final boolean wantsUnresolved(final String gerritIssueStatus) {
        return STATUS_ALL.equals(gerritIssueStatus);
    }
}
