package com.meetme.plugins.jira.gerrit.webpanel;

import com.meetme.plugins.jira.gerrit.data.GerritConfiguration;
import com.meetme.plugins.jira.gerrit.data.IssueReviewsManager;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;

import com.atlassian.jira.issue.Issue;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;

import java.util.List;
import java.util.Map;

/**
 * Created by jhansche on 9/2/16.
 */
public class ShowReviewsWebPanelCondition implements Condition {
    private static final String KEY_ISSUE = "issue";

    private final GerritConfiguration mConfiguration;
    private final IssueReviewsManager mReviewsManager;

    public ShowReviewsWebPanelCondition(IssueReviewsManager reviewsManager,
            GerritConfiguration configurationManager) {
        mReviewsManager = reviewsManager;
        mConfiguration = configurationManager;
    }

    @Override
    public void init(Map<String, String> map) throws PluginParseException {
    }

    @Override
    public boolean shouldDisplay(Map<String, Object> map) {
        // TODO: allow per-project configuration to hide this

        // Short circuit if we always want to show
        if (mConfiguration.getShowsEmptyPanel()) return true;

        final Issue issue = (Issue) map.get(KEY_ISSUE);

        List<GerritChange> reviews = null;

        try {
            reviews = mReviewsManager.getReviewsForIssue(issue);
        } catch (GerritQueryException e) {
            e.printStackTrace();
        }
//
        return reviews != null && !reviews.isEmpty();
    }
}
