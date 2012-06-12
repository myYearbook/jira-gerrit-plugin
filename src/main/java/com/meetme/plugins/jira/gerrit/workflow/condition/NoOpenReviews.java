package com.meetme.plugins.jira.gerrit.workflow.condition;

import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.workflow.condition.AbstractJiraCondition;
import com.meetme.plugins.jira.gerrit.data.IssueReviewsManager;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;

// TODO: make this into a "Must have <operator> <number> review(s) in status <status>" condition ??
public class NoOpenReviews extends AbstractJiraCondition {
    private static final String KEY_REVERSE = "reverse";

    private IssueReviewsManager reviewsManager;

    public NoOpenReviews(IssueReviewsManager reviewsManager) {
        this.reviewsManager = reviewsManager;
    }

    @Override
    public boolean passesCondition(@SuppressWarnings("rawtypes") Map transientVars, @SuppressWarnings("rawtypes") Map args, PropertySet ps)
            throws WorkflowException {
        Issue issue = getIssue(transientVars);
        String issueKey = issue.getKey();
        List<JSONObject> reviews;

        try {
            reviews = reviewsManager.getReviews(issueKey);
        } catch (GerritQueryException e) {
            // If there's an error, best not to block the workflow, and just act like it passes??
            throw new WorkflowException(e);
        }

        String value = (String) args.get(KEY_REVERSE);
        boolean isReversed = Boolean.parseBoolean(value);

        // Confirm that reviews contains no *open* reviews
        // XXX: is reviews made up of *only* open reviews?
        int numOpenReviews = reviews.size();

        return isReversed ? numOpenReviews > 0 : numOpenReviews == 0;
    }

}
