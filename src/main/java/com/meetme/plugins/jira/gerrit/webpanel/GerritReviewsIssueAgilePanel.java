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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.meetme.plugins.jira.gerrit.data.IssueReviewsManager;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;

@SuppressWarnings("unchecked")
public class GerritReviewsIssueAgilePanel extends AbstractJiraContextProvider {
	private static final String KEY_ISSUE = "issue";
	private static final String KEY_CHANGES = "changes";
	private static final String KEY_ERROR = "error";

	private IssueReviewsManager reviewsManager;

	public GerritReviewsIssueAgilePanel(IssueReviewsManager reviewsManager) {
		super();
		this.reviewsManager = reviewsManager;
	}

	@Override
	public Map<String, Object> getContextMap(ApplicationUser user, JiraHelper jiraHelper) {
		HashMap<String, Object> contextMap = new HashMap<>();

		Issue currentIssue = (Issue) jiraHelper.getContextParams().get(KEY_ISSUE);

		try {
			List<GerritChange> changes = reviewsManager.getReviewsForIssue(currentIssue);
			contextMap.put(KEY_CHANGES, changes);
			contextMap.put("atl.gh.issue.details.tab.count", (long) changes.size());
		} catch (GerritQueryException e) {
			contextMap.put(KEY_ERROR, e.getMessage());
		}

		return contextMap;
	}

}
