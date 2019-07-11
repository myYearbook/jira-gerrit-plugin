package com.meetme.plugins.jira.gerrit.data;

import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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

public class IssueReviewsManagerTest {
    public static final String ISSUE_KEY_OLD = "OLD-123";
    private static final String ISSUE_KEY_NEW = "NEW-123";

    @Mock
    private MutableIssue mockIssue;

    @Mock
    private GerritConfiguration configuration;

    @Mock
    private IssueManager mockJiraIssueManager;

    @Mock
    private CacheManager mockCacheManager;

    @Mock
    private Cache<String, List<GerritChange>> mockCache;

    private IssueReviewsManager issueReviewsManager;

    @Before
    public void setUp() {
        initMocks(this);

        // gerrit configuration
        when(configuration.getIssueSearchQuery()).thenReturn(GerritConfiguration.DEFAULT_QUERY_ISSUE);
        when(configuration.getProjectSearchQuery()).thenReturn(GerritConfiguration.DEFAULT_QUERY_PROJECT);

        // issue
        when(mockIssue.getKey()).thenReturn(ISSUE_KEY_NEW);

        // issue key history
        when(mockJiraIssueManager.getIssueByKeyIgnoreCase(Mockito.anyString())).thenReturn(mockIssue);
        Set<String> allIssueKeys = new HashSet<>();
        allIssueKeys.add(ISSUE_KEY_OLD);
        allIssueKeys.add(ISSUE_KEY_NEW);
        when(mockJiraIssueManager.getAllIssueKeys(mockIssue.getId())).thenReturn(allIssueKeys);

        // mock gerrit review retrieval
        when(mockCacheManager.getCache(
                eq("com.meetme.plugins.jira.gerrit.data.IssueReviewsManager.issueChanges.cache"),
                Mockito.<CacheLoader<String, List<GerritChange>>>any(),
                any()
        )).thenReturn(mockCache);
        issueReviewsManager = new IssueReviewsImpl(configuration, mockJiraIssueManager, mockCacheManager, null);

        GerritChange oldChange = createMockChange(ISSUE_KEY_OLD);
        GerritChange newChange = createMockChange(ISSUE_KEY_NEW);
        when(mockCache.get(eq(ISSUE_KEY_OLD))).thenReturn(Collections.singletonList(oldChange));
        when(mockCache.get(eq(ISSUE_KEY_NEW))).thenReturn(Collections.singletonList(newChange));
    }

    private GerritChange createMockChange(String key) {
        GerritChange change = mock(GerritChange.class);
        when(change.getSubject()).thenReturn(key);
        return change;
    }

    @Test
    public void testGetReviewsForIssue() throws Exception {
        List<GerritChange> reviewsForIssue = issueReviewsManager.getReviewsForIssue(mockIssue);
        assertEquals(2, reviewsForIssue.size());

        Set<String> reviewSubjects = new HashSet<>();
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
