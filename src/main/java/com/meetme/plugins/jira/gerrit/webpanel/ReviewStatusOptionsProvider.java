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

import java.util.Map;

public class ReviewStatusOptionsProvider implements WebItemProvider {
    public static final String STATUS_OPEN = "Open";
    public static final String STATUS_ALL = "All";
    public static final String DEFAULT_STATUS = STATUS_ALL;

    private VelocityRequestContextFactory requestContextFactory;
    private JiraAuthenticationContext authenticationContext;

    public ReviewStatusOptionsProvider(VelocityRequestContextFactory requestContextFactory, JiraAuthenticationContext authenticationContext) {
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

        String reviewStatus = (String) session.getAttribute(SessionKeys.VIEWISSUE_REVIEWS_REVIEWSTATUS);

        if (reviewStatus == null) {
            reviewStatus = DEFAULT_STATUS;
        }

        int weight = 10;

        final WebItem allLink = new WebFragmentBuilder(weight += 10)
                .id("reviews-reviewstatus-all")
                .label(i18n.getText("gerrit-reviews-left-panel.options.reviewstatus.all"))
                .styleClass(getStyleFor(reviewStatus, STATUS_ALL))
                .webItem("reviewstatus-view-options")
                .url(getUrlForType(STATUS_ALL, baseUrl, issue))
                .build();

        final WebItem openLink = new WebFragmentBuilder(weight += 10)
                .id("reviews-reviewstatus-open")
                .label(i18n.getText("gerrit-reviews-left-panel.options.reviewstatus.open"))
                .styleClass(getStyleFor(reviewStatus, STATUS_OPEN))
                .webItem("reviewstatus-view-options")
                .url(getUrlForType(STATUS_OPEN, baseUrl, issue))
                .build();

        return CollectionBuilder.list(allLink, openLink);
    }

    private String getUrlForType(String type, String baseUrl, Issue issue) {
        return baseUrl + "/browse/" + issue.getKey() + "?gerritReviewStatus=" + type + "#gerrit-reviews-left-panel";
    }

    private String getStyleFor(String type, String expecting) {
        return expecting.equals(type) ? "aui-list-checked aui-checked" : "aui-list-checked";
    }

    public static boolean wantsClosedReviews(String gerritReviewStatus) {
        return STATUS_ALL.equals(gerritReviewStatus);
    }
}
