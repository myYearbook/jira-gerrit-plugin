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
package com.meetme.plugins.jira.gerrit.tabpanel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.user.ApplicationUser;
import com.meetme.plugins.jira.gerrit.data.GerritConfiguration;
import com.meetme.plugins.jira.gerrit.data.IssueReviewsManager;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;

/**
 * @author Joe Hansche
 */
public class SubtaskReviewsTabPanelTest {

    @Mock
    private GerritConfiguration configuration;
    @Mock
    private IssueReviewsManager reviewsManager;

    @Mock
    Issue issue;
    @Mock
    ApplicationUser user;

    Issue subtask1;
    Issue subtask2;
    Issue subtask3;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        setUpConfiguration();
    }

    @After
    public void tearDown() throws Exception {
    }

    private void setUpConfiguration() {
        when(configuration.getSshHostname()).thenReturn("gerrit.company.com");
        when(configuration.getSshUsername()).thenReturn("jira");
        File file = mock(File.class);
        when(configuration.getSshPrivateKey()).thenReturn(file);
        when(file.exists()).thenReturn(true);
    }

    private List<Issue> setUpSubtasks() {
        List<Issue> issues = new ArrayList<>();

        subtask1 = mock(Issue.class);
        subtask2 = mock(Issue.class);
        subtask3 = mock(Issue.class);

        when(subtask1.getKey()).thenReturn("SUB-1");
        when(subtask2.getKey()).thenReturn("SUB-2");
        when(subtask3.getKey()).thenReturn("SUB-3");

        issues.add(subtask1);
        issues.add(subtask2);
        issues.add(subtask3);

        return issues;
    }

    /**
     * Test method for
     * {@link SubtaskReviewsTabPanel#SubtaskReviewsTabPanel(GerritConfiguration, IssueReviewsManager)}
     */
    @Test
    public void testCtor() {
        SubtaskReviewsTabPanel obj = new SubtaskReviewsTabPanel(configuration, reviewsManager);
        assertTrue(obj instanceof SubtaskReviewsTabPanel);
    }

    /**
     * Test method for
     * {@link SubtaskReviewsTabPanel#showPanel(com.atlassian.jira.plugin.issuetabpanel.ShowPanelRequest)}
     */
    @Test
    public void testShowPanelShowPanelRequest_empty() {
        SubtaskReviewsTabPanel obj = new SubtaskReviewsTabPanel(configuration, reviewsManager);
        // Returns false because the subtasks are empty
        assertFalse(obj.showPanel(issue, user));

        when(issue.getSubTaskObjects()).thenReturn(null);
        // Returns false because subtasks is null
        assertFalse(obj.showPanel(issue, user));
    }

    /**
     * Test method for
     * {@link SubtaskReviewsTabPanel#showPanel(com.atlassian.jira.plugin.issuetabpanel.ShowPanelRequest)}
     */
    @Test
    public void testShowPanelShowPanelRequest_nullSubtasks() {
        SubtaskReviewsTabPanel obj = new SubtaskReviewsTabPanel(configuration, reviewsManager);
        // Returns false because the configuration is empty
        assertFalse(obj.showPanel(issue, user));
    }

    /**
     * Test method for
     * {@link SubtaskReviewsTabPanel#showPanel(com.atlassian.jira.plugin.issuetabpanel.ShowPanelRequest)}
     */
    @Test
    public void testShowPanelShowPanelRequest_noSubtasks() {
        when(issue.getSubTaskObjects()).thenReturn(new ArrayList<>());

        SubtaskReviewsTabPanel obj = new SubtaskReviewsTabPanel(configuration, reviewsManager);
        // Returns false because the configuration is empty
        assertFalse(obj.showPanel(issue, user));
    }

    @Test
    public void testGetActions_noSubtasks() {
        SubtaskReviewsTabPanel obj = new SubtaskReviewsTabPanel(configuration, reviewsManager);
        // Returns false because the configuration is empty
        List<IssueAction> actions = obj.getActions(issue, user);
        assertEquals(0, actions.size());
    }

    @Test
    public void testGetActions_someSubtasks() {
        List<Issue> subtasks = setUpSubtasks();
        when(issue.getSubTaskObjects()).thenReturn(subtasks);

        SubtaskReviewsTabPanel obj = new SubtaskReviewsTabPanel(configuration, reviewsManager);
        // Returns false because the configuration is empty
        List<IssueAction> actions = obj.getActions(issue, user);
        assertEquals(3, actions.size());
    }

    @Test(expected = RuntimeException.class)
    public void testGetActions_gerritError() throws RuntimeException, GerritQueryException {
        SubtaskReviewsTabPanel obj = new SubtaskReviewsTabPanel(configuration, reviewsManager);

        List<Issue> subtasks = setUpSubtasks();
        when(issue.getSubTaskObjects()).thenReturn(subtasks);

        GerritQueryException exc = new GerritQueryException();
        when(reviewsManager.getReviewsForIssue(subtask2)).thenThrow(exc);

        obj.getActions(issue, user);

    }

    /**
     * Test method for {@link SubtaskReviewsTabPanel#isConfigurationReady()} to indicate
     * Configuration is not ready if certain conditions are not met.
     */
    @Test
    public void testConfigurationNotReady() {
        SubtaskReviewsTabPanel obj = new SubtaskReviewsTabPanel(null, null);
        // False because configuration == null
        assertFalse(obj.showPanel(issue, user));

        // Now setup the normal mock configuration
        obj = new SubtaskReviewsTabPanel(configuration, null);

        // SSH file not exist
        when(configuration.getSshPrivateKey().exists()).thenReturn(false);
        assertFalse(obj.showPanel(issue, user));

        // SSH file is null
        when(configuration.getSshPrivateKey()).thenReturn(null);
        assertFalse(obj.showPanel(issue, user));

        // Username is null
        when(configuration.getSshUsername()).thenReturn(null);
        assertFalse(obj.showPanel(issue, user));

        // Hostname is null
        when(configuration.getSshHostname()).thenReturn(null);
        assertFalse(obj.showPanel(issue, user));
    }

    /**
     * Test method for
     * {@link SubtaskReviewsTabPanel#showPanel(com.atlassian.jira.plugin.issuetabpanel.ShowPanelRequest)}
     * when the issue has subtasks.
     */
    @Test
    public void testShowPanelShowPanelRequest_withSubtasks() {
        List<Issue> subtasks = setUpSubtasks();
        when(issue.getSubTaskObjects()).thenReturn(subtasks);

        SubtaskReviewsTabPanel obj = new SubtaskReviewsTabPanel(configuration, reviewsManager);
        assertTrue(obj.showPanel(issue, user));
    }
}
