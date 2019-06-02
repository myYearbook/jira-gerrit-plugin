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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.atlassian.core.util.collection.EasyList;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;

/**
 * @author Joe Hansche
 */
public class SubtaskReviewsIssueActionTest {

    @Mock
    IssueTabPanelModuleDescriptor descriptor;

    @Mock
    Issue subtask;

    @Mock
    GerritChange change1;

    @Mock
    GerritChange change2;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link SubtaskReviewsIssueAction#SubtaskReviewsIssueAction(IssueTabPanelModuleDescriptor, Issue, List)}
     * .
     */
    @Test
    public void testCtor() {
        SubtaskReviewsIssueAction obj = new SubtaskReviewsIssueAction(descriptor, null, null);
        assertTrue(obj instanceof SubtaskReviewsIssueAction);
    }

    /**
     * Test method for {@link SubtaskReviewsIssueAction#isDisplayActionAllTab()} .
     */
    @Test
    public void testIsDisplayActionAllTab() {
        SubtaskReviewsIssueAction obj = new SubtaskReviewsIssueAction(descriptor, null, null);
        assertFalse(obj.isDisplayActionAllTab());
    }

    /**
     * Test method for {@link SubtaskReviewsIssueAction#getTimePerformed()}.
     */
    @Test
    public void testGetTimePerformed() {
        Timestamp ts = mock(Timestamp.class);
        when(subtask.getUpdated()).thenReturn(ts);

        SubtaskReviewsIssueAction obj = new SubtaskReviewsIssueAction(descriptor, subtask, null);

        assertSame(ts, obj.getTimePerformed());
    }

    /**
     * Test method for {@link SubtaskReviewsIssueAction#populateVelocityParams(Map)} .
     */
    @Test
    public void testPopulateVelocityParamsMap_nullChanges() {
        Map<String, Object> params = new HashMap<>();
        SubtaskReviewsIssueAction obj = new SubtaskReviewsIssueAction(descriptor, subtask, null);

        obj.populateVelocityParams(params);

        assertSame(subtask, params.get("subtask"));
        assertNull(params.get("changes"));
    }

    /**
     * Test method for {@link SubtaskReviewsIssueAction#populateVelocityParams(Map)} .
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testPopulateVelocityParamsMap() {
        Map<String, Object> params = new HashMap<>();
        List changes = EasyList.build(change1, change2);

        @SuppressWarnings("unchecked")
        SubtaskReviewsIssueAction obj = new SubtaskReviewsIssueAction(descriptor, subtask, changes);

        obj.populateVelocityParams(params);

        assertSame(subtask, params.get("subtask"));
        assertNotNull(params.get("changes"));
        assertEquals(2, ((List) params.get("changes")).size());
    }
}
