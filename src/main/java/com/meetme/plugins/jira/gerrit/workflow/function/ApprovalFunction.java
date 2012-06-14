package com.meetme.plugins.jira.gerrit.workflow.function;

import java.util.List;
import java.util.Map;

import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.meetme.plugins.jira.gerrit.data.GerritConfiguration;
import com.meetme.plugins.jira.gerrit.data.IssueReviewsManager;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;

public class ApprovalFunction extends AbstractJiraFunctionProvider {
    public static final String KEY_CMD_ARGS = "cmdArgs";
    public static final String DEFAULT_CMD_ARGS = "--verified 1";

    private final IssueReviewsManager reviewsManager;
    private final GerritConfiguration configuration;

    public ApprovalFunction(GerritConfiguration configuration, IssueReviewsManager reviewsManager) {
        super();

        this.configuration = configuration;
        this.reviewsManager = reviewsManager;
    }

    private boolean isConfigurationReady() {
        return configuration.getSshHostname() != null && configuration.getSshUsername() != null
                && configuration.getSshPrivateKey() != null && configuration.getSshPrivateKey().exists();
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") Map transientVars, @SuppressWarnings("rawtypes") Map args, PropertySet ps)
            throws WorkflowException {
        if (!isConfigurationReady()) {
            throw new IllegalStateException("Configure the Gerrit integration from the Administration panel first.");
        }

        String issueKey = getIssue(transientVars).getKey();
        List<GerritChange> issueReviews;

        try {
            issueReviews = reviewsManager.getReviews(issueKey);
        } catch (GerritQueryException e) {
            throw new WorkflowException("Unable to retrieve associated reviews", e);
        }

        for (GerritChange change : issueReviews) {
            // doSubmit(change)?
        }
    }
}
