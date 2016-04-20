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
package com.meetme.plugins.jira.gerrit.workflow.function;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.atlassian.core.user.preferences.Preferences;
import com.atlassian.jira.user.preferences.UserPreferencesManager;
import com.meetme.plugins.jira.gerrit.data.GerritConfiguration;
import com.meetme.plugins.jira.gerrit.data.IssueReviewsManager;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.meetme.plugins.jira.gerrit.workflow.AbstractWorkflowTest;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;

/**
 * @author Joe Hansche
 */
public abstract class ApprovalFunctionTest extends AbstractWorkflowTest {
    @Mock
    PropertySet ps;
    @Mock
    UserPreferencesManager userPrefsManager;
    @Mock
    Preferences mockPrefs;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        setUpUser();
        setUpUserPrefs();
    }

    private void setUpUserPrefs() {
        when(userPrefsManager.getPreferences(mockUser)).thenReturn(mockPrefs);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link ApprovalFunction#ApprovalFunction(GerritConfiguration, IssueReviewsManager, UserPreferencesManager)}
     * .
     */
    @Test
    public void testCtor() {
        ApprovalFunction obj = new ApprovalFunction(configuration, reviewsManager, userPrefsManager);
        assertTrue(obj instanceof ApprovalFunction);
    }

    /**
     * Test method for {@link ApprovalFunction#isConfigurationReady()}.
     */
    @Test
    public void testConfigurationReady() {
        ApprovalFunction obj = new ApprovalFunction(null, null, null);
        // configuration is null
        assertFalse(obj.isConfigurationReady());

        obj = new ApprovalFunction(configuration, null, null);
        // configuration is null
        assertTrue(obj.isConfigurationReady());

        // SSH file not exist
        when(configuration.getSshPrivateKey().exists()).thenReturn(false);
        assertFalse(obj.isConfigurationReady());

        // SSH file is null
        when(configuration.getSshPrivateKey()).thenReturn(null);
        assertFalse(obj.isConfigurationReady());

        // Username is null
        when(configuration.getSshUsername()).thenReturn(null);
        assertFalse(obj.isConfigurationReady());

        // Hostname is null
        when(configuration.getSshHostname()).thenReturn(null);
        assertFalse(obj.isConfigurationReady());
    }

    /**
     * Test method for {@link ApprovalFunction#execute(Map, Map, PropertySet)}.
     *
     * @throws WorkflowException
     */
    @Test(expected = IllegalStateException.class)
    public void testExecute_notReady() throws WorkflowException {
        ApprovalFunction obj = new ApprovalFunction(null, null, null);
        obj.execute(null, null, null);
    }

    @Test
    public void testGetIssueKey() {
        ApprovalFunction obj = new ApprovalFunction(configuration, null, null);
        String actual = obj.getIssueKey(transientVars);
        assertEquals("FOO-123", actual);
    }

    @Test
    public void testGetUserPrefs() {
        ApprovalFunction obj = new ApprovalFunction(configuration, reviewsManager, userPrefsManager);
        Preferences actual = obj.getUserPrefs(transientVars, args);
        assertSame(mockPrefs, actual);
    }

    @Test(expected = WorkflowException.class)
    public void testGetReviews_failure() throws WorkflowException, GerritQueryException {
        stubFailingReviews();
        ApprovalFunction obj = new ApprovalFunction(configuration, reviewsManager, userPrefsManager);
        obj.getReviews(mockIssue);
    }

    @Test
    public void testGetReviews_success() throws WorkflowException, GerritQueryException {
        stubOneReview();
        ApprovalFunction obj = new ApprovalFunction(configuration, reviewsManager, userPrefsManager);
        List<GerritChange> actual = obj.getReviews(mockIssue);
        assertEquals(1, actual.size());
    }

    @SuppressWarnings("unchecked")
    @Test(expected = WorkflowException.class)
    public void testExecute_gerritFailed() throws WorkflowException, IOException {
        ApprovalFunction obj = new ApprovalFunction(configuration, reviewsManager, userPrefsManager);
        when(reviewsManager.doApprovals(mockIssue, Mockito.anyList(), Mockito.anyString(), eq(mockPrefs))).thenReturn(false);
        obj.execute(transientVars, args, ps);

        verify(reviewsManager, times(1)).doApprovals(mockIssue, anyList(), anyString(), eq(mockPrefs));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = WorkflowException.class)
    public void testExecute_gerritThrows() throws WorkflowException, IOException {
        IOException exc = new IOException();

        ApprovalFunction obj = new ApprovalFunction(configuration, reviewsManager, userPrefsManager);
        when(reviewsManager.doApprovals(mockIssue, Mockito.anyList(), Mockito.anyString(), eq(mockPrefs))).thenThrow(exc);
        obj.execute(transientVars, args, ps);

        verify(reviewsManager, times(1)).doApprovals(mockIssue, anyList(), anyString(), eq(mockPrefs));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExecute_success() throws WorkflowException, IOException {
        ApprovalFunction obj = new ApprovalFunction(configuration, reviewsManager, userPrefsManager);
        when(reviewsManager.doApprovals(mockIssue, Mockito.anyList(), Mockito.anyString(), eq(mockPrefs))).thenReturn(true);
        obj.execute(transientVars, args, ps);

        verify(reviewsManager, times(1)).doApprovals(mockIssue, anyList(), anyString(), eq(mockPrefs));
    }
}
