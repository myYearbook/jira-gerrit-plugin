package com.meetme.plugins.jira.gerrit.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import com.atlassian.jira.util.collect.LRUMap;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryHandler;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.Authentication;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.SshException;

public class IssueReviewsImpl implements IssueReviewsManager {
    private static final String GERRIT_SEARCH = "message:%1$s";
    private static final int CACHE_CAPACITY = 20;

    private GerritConfiguration configuration;

    /**
     * LRU (least recently used) Cache object to avoid slamming the Gerrit server too many times.
     * 
     * TODO: This might result in an issue using a stale cache for reviews that change often, but
     * corresponding issues viewed rarely!
     */
    protected static final Map<String, List<GerritChange>> lruCache =
            LRUMap.<String, List<GerritChange>> synchronizedLRUMap(CACHE_CAPACITY);

    public IssueReviewsImpl(GerritConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public List<GerritChange> getReviews(String issueKey) throws GerritQueryException {
        List<GerritChange> changes;

        if (lruCache.containsKey(issueKey)) {
            changes = lruCache.get(issueKey);
        } else {
            changes = getReviewsFromGerrit(issueKey);
            lruCache.put(issueKey, changes);
        }

        return changes;
    }

    private List<GerritChange> getReviewsFromGerrit(String issueKey) throws GerritQueryException {
        List<GerritChange> changes;
        Authentication auth = new Authentication(configuration.getSshPrivateKey(), configuration.getSshUsername());
        GerritQueryHandler h = new GerritQueryHandler(configuration.getSshHostname(), configuration.getSshPort(), auth);
        List<JSONObject> reviews;

        try {
            reviews = h.queryJava(String.format(GERRIT_SEARCH, issueKey), false, true, false);
        } catch (SshException e) {
            throw new GerritQueryException("An ssh error occurred while querying for reviews.", e);
        } catch (IOException e) {
            throw new GerritQueryException("An error occurred while querying for reviews.", e);
        }

        changes = new ArrayList<GerritChange>(reviews.size());

        for (JSONObject obj : reviews) {
            if (obj.has("type") && "stats".equalsIgnoreCase(obj.getString("type"))) {
                continue;
            }

            changes.add(new GerritChange(obj));
        }

        return changes;
    }

    @Override
    public void doApproval(String issueKey, GerritChange change, String args) throws IOException {
        GerritCommand command = new GerritCommand(configuration);
        command.doReview(change, args);

        // Something probably changed!
        lruCache.remove(issueKey);
    }
}
