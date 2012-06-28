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
package com.meetme.plugins.jira.gerrit.workflow.condition;

import java.util.List;
import java.util.Map;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.workflow.condition.AbstractJiraCondition;
import com.meetme.plugins.jira.gerrit.data.IssueReviewsManager;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;

/**
 * Workflow condition that can be used to enforce that an issue "MUST", or "MUST NOT" have any open
 * Gerrit reviews.
 * 
 * @author Joe Hansche <jhansche@myyearbook.com>
 */
public class NoOpenReviews extends AbstractJiraCondition {
    public static final String KEY_REVERSED = "reversed";

    private IssueReviewsManager reviewsManager;

    public NoOpenReviews(IssueReviewsManager reviewsManager) {
        this.reviewsManager = reviewsManager;
    }

    @Override
    public boolean passesCondition(@SuppressWarnings("rawtypes") Map transientVars, @SuppressWarnings("rawtypes") Map args, PropertySet ps)
            throws WorkflowException {
        Issue issue = getIssue(transientVars);
        String issueKey = issue.getKey();
        List<GerritChange> reviews;

        try {
            reviews = reviewsManager.getReviewsForIssue(issueKey);
        } catch (GerritQueryException e) {
            // If there's an error, best not to block the workflow, and just act like it passes??
            throw new WorkflowException(e);
        }

        String value = (String) args.get(KEY_REVERSED);
        boolean isReversed = Boolean.parseBoolean(value);

        // The ReviewsManager will only return issues that are "status:open" by default.
        int numOpenReviews = countReviewStatus(reviews, true);

        return isReversed ? numOpenReviews > 0 : numOpenReviews == 0;
    }

    /**
     * Counts the number of reviews that are open or closed.
     * 
     * @param reviews
     * @param isOpen
     * @return
     */
    public static int countReviewStatus(List<GerritChange> reviews, boolean isOpen) {
        int count = 0;

        for (GerritChange change : reviews) {
            if (change.isOpen() == isOpen) {
                count += 1;
            }
        }

        return count;
    }

}
