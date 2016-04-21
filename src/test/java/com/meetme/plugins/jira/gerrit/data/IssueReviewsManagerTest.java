package com.meetme.plugins.jira.gerrit.data;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.junit.rules.MockitoContainer;
import com.atlassian.jira.mock.MockIssueManager;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.mock.issue.MockIssue;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/*
 * Copyright 2016 Pavel Tarasenko
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

public class IssueReviewsManagerTest
{
    public static final String ISSUE_KEY_OLD = "OLD-123";
    private static final String ISSUE_KEY_NEW = "NEW-123";

    @Mock
    private MutableIssue mockIssue;

    @Mock
    private GerritConfiguration configuration;

    @Mock
    private IssueManager mockJiraIssueManager;

    private IssueReviewsManager issueReviewsManager;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        // gerrit configuration
        when(configuration.getIssueSearchQuery()).thenReturn(GerritConfiguration.DEFAULT_QUERY_ISSUE);
        when(configuration.getProjectSearchQuery()).thenReturn(GerritConfiguration.DEFAULT_QUERY_PROJECT);

        // issue
        when(mockIssue.getKey()).thenReturn(ISSUE_KEY_NEW);

        // issue key history
        when(mockJiraIssueManager.getIssueByKeyIgnoreCase(Mockito.anyString())).thenReturn(mockIssue);
        Set<String> allIssueKeys = new HashSet<String>();
        allIssueKeys.add(ISSUE_KEY_OLD);
        allIssueKeys.add(ISSUE_KEY_NEW);
        when(mockJiraIssueManager.getAllIssueKeys(mockIssue.getId())).thenReturn(allIssueKeys);

        // mock gerrit review retrieval
        issueReviewsManager = new IssueReviewsImpl(configuration, mockJiraIssueManager) {
            @Override protected List<GerritChange> getReviewsFromGerrit(String searchQuery) throws GerritQueryException
            {
                List<GerritChange> reviews = new ArrayList<GerritChange>();

                if (searchQuery.contains(ISSUE_KEY_OLD)) {
                    GerritChange oldChangeMock = mock(GerritChange.class);
                    when(oldChangeMock.getSubject()).thenReturn(ISSUE_KEY_OLD);
                    reviews.add(oldChangeMock);
                }

                if (searchQuery.contains(ISSUE_KEY_NEW)) {
                    GerritChange newChangeMock = mock(GerritChange.class);
                    when(newChangeMock.getSubject()).thenReturn(ISSUE_KEY_NEW);
                    reviews.add(newChangeMock);
                }

                return reviews;
            }
        };
    }

    @Test
    public void testGetReviewsForIssue() throws Exception {
        List<GerritChange> reviewsForIssue = issueReviewsManager.getReviewsForIssue(mockIssue);
        assertEquals(2, reviewsForIssue.size());

        Set<String> reviewSubjects = new HashSet<String>();
        for (GerritChange review : reviewsForIssue) {
            reviewSubjects.add(review.getSubject());
        }

        assertThat(reviewSubjects, containsInAnyOrder(ISSUE_KEY_OLD, ISSUE_KEY_NEW));
    }

    @Test
    public void testDoApprovals() throws Exception {

    }

    @Test
    public void testGetIssueKeys() throws Exception {
        Set<String> issueKeys = issueReviewsManager.getIssueKeys(mockIssue);
        assertEquals(2, issueKeys.size());
        assertThat(issueKeys, containsInAnyOrder(mockIssue.getKey(), ISSUE_KEY_OLD));
    }
}
