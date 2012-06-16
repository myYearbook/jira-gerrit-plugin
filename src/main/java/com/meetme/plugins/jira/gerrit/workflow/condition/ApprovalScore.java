package com.meetme.plugins.jira.gerrit.workflow.condition;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.workflow.condition.AbstractJiraCondition;
import com.meetme.plugins.jira.gerrit.data.IssueReviewsManager;
import com.meetme.plugins.jira.gerrit.data.dto.GerritApproval;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;

public class ApprovalScore extends AbstractJiraCondition {
    private static final Logger logger = LoggerFactory.getLogger(ApprovalScore.class);

    // Must [or not] have [ operator ] N score for [type]
    public static final String KEY_NEGATIVE = "negative";
    public static final String KEY_COMPARISON = "comparison";
    public static final String KEY_TARGET = "target";
    public static final String KEY_LABEL = "label";

    private IssueReviewsManager reviewsManager;

    public ApprovalScore(IssueReviewsManager reviewsManager) {
        this.reviewsManager = reviewsManager;
    }

    @Override
    public boolean passesCondition(@SuppressWarnings("rawtypes") Map transientVars, @SuppressWarnings("rawtypes") Map args, PropertySet ps)
            throws WorkflowException {
        Issue issue = getIssue(transientVars);
        String issueKey = issue.getKey();
        List<GerritChange> reviews;

        try {
            reviews = reviewsManager.getReviews(issueKey);
        } catch (GerritQueryException e) {
            // If there's an error, best not to block the workflow, and just act like it passes??
            throw new WorkflowException(e);
        }

        boolean isReverse = Boolean.parseBoolean((String) args.get(KEY_NEGATIVE));
        ComparisonOperator op = ComparisonOperator.valueOf((String) args.get(KEY_COMPARISON));
        String label = (String) args.get(KEY_LABEL);
        int targetScore = Integer.parseInt((String) args.get(KEY_TARGET));

        String description = describe(isReverse, label, op, targetScore);
        logger.debug("Condition description: " + description);

        boolean matches = false;
        int matchingChanges = 0;
        int blockingChanges = 0;

        for (GerritChange ch : reviews) {
            int matchingApprovals = 0;

            for (GerritApproval approval : ch.getPatchSet().getApprovals()) {
                if (approval.getType().equals(label)) {
                    if (compareScore(op, approval.getValueAsInt(), targetScore)) {
                        logger.info("Found a match on review " + ch + " for condition: " + description + "; Approver=" + approval);

                        matchingApprovals++;
                    }
                }
            }

            if (matchingApprovals > 0) {
                matchingChanges++;
            } else {
                blockingChanges++;
            }
        }

        // To be considered a match, every change must have at least one matching approval, and no
        // change can be missing a matching approval
        matches = matchingChanges > 0 && blockingChanges == 0;

        if (isReverse) {
            matches = !matches;
            logger.debug("Negating logic, due to 'MUST NOT' condition. NEW matches=" + matches);
        }

        logger.info("Evaluating conditions: " + matches);

        return matches;
    }

    private String describe(boolean isReverse, String label, ComparisonOperator op, int targetScore) {
        return "isReverse=" + isReverse + ", label=" + label + ", op=" + op.name() + ", targetScore=" + targetScore;
    }

    private boolean compareScore(ComparisonOperator oper, int score, int target)
    {
        logger.debug("Comparing score: " + score + oper + target);

        switch (oper) {
            case EQUAL_TO:
                return score == target;
            case LESS_THAN:
                return score < target;
            case LESS_OR_EQUAL:
                return score <= target;
            case GREATER_OR_EQUAL:
                return score >= target;
            case GREATER_THAN:
                return score > target;
        }

        throw new IllegalArgumentException("Unknown operator: " + oper);
    }

    public static enum ComparisonOperator
    {
        LESS_THAN("<"),
        LESS_OR_EQUAL("<="),
        EQUAL_TO("=="),
        GREATER_OR_EQUAL(">="),
        GREATER_THAN(">");

        private final String display;

        private ComparisonOperator(final String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return display;
        }
    }
}
