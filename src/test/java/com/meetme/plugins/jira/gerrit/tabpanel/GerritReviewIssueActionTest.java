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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;
import com.atlassian.jira.web.util.OutlookDate;
import com.meetme.plugins.jira.gerrit.data.dto.GerritApproval;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.meetme.plugins.jira.gerrit.data.dto.GerritPatchSet;

public class GerritReviewIssueActionTest {
    private static final String BASE_URL = "http://localhost:2990/jira";

    private static final long TEST_LAST_UPDATED_TIMESTAMP = 1339987664000L;
    private static final String TEST_PROJECT = "Development/project";
    private static final String TEST_BRANCH = "master";
    private static final String TEST_NUMBER = "1234";
    private static final String TEST_PATCHSET_NUMBER = "2";
    private static final String TEST_SUBJECT = "FOO-1: Hello world";
    private static final String TEST_URL = "http://gerrit.local/1234";
    private static final String TEST_REF = "refs/changes/34/1234/1";
    private static final Date TEST_LAST_UPDATED = new Date(TEST_LAST_UPDATED_TIMESTAMP);

    private static final String TEST_FORMATTED_LAST_UPDATED = "Today 11:16 PM";
    private static final String TEST_ISO_LAST_UPDATED = "2012-06-17T23:16:00-0400";

    private static final ArrayList<GerritApproval> TEST_APPROVALS = new ArrayList<>();
    private static final GerritApproval APPROVAL_NEGATIVE = new GerritApproval();
    private static final GerritApproval APPROVAL_POSITIVE = new GerritApproval();
    private static final GerritApproval APPROVAL_POSITIVE_2 = new GerritApproval();

    @Mock
    private IssueTabPanelModuleDescriptor descriptor;
    @Mock
    private OutlookDate dateTimeFormatter;

    private GerritChange change;

    private GerritReviewIssueAction action;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        setUpDateTimeFormatter();
        setUpApprovals();
        setUpGerritChange();
        action = new GerritReviewIssueAction(descriptor, change, dateTimeFormatter, BASE_URL);

    }

    @After
    public void tearDown() throws Exception {
        change = null;
        action = null;
    }

    private void setUpDateTimeFormatter() {
        when(dateTimeFormatter.format(TEST_LAST_UPDATED)).thenReturn(TEST_FORMATTED_LAST_UPDATED);

        when(dateTimeFormatter.formatIso8601(TEST_LAST_UPDATED)).thenReturn(TEST_ISO_LAST_UPDATED);
    }

    private void setUpApprovals() {
        // setup negative approval
        APPROVAL_NEGATIVE.setValue("-1");
        APPROVAL_NEGATIVE.setType("NEG");

        // setup positive approval
        APPROVAL_POSITIVE.setValue("1");
        APPROVAL_POSITIVE.setType("POS");

        // setup more-positive approval
        APPROVAL_POSITIVE_2.setValue("2");
        APPROVAL_POSITIVE_2.setType("POS");

        TEST_APPROVALS.clear();
        TEST_APPROVALS.add(APPROVAL_NEGATIVE);
        TEST_APPROVALS.add(APPROVAL_POSITIVE);
        TEST_APPROVALS.add(APPROVAL_POSITIVE_2);
    }

    @SuppressWarnings("deprecation")
    private void setUpGerritChange() {
        change = new GerritChange();
        change.setBranch(TEST_BRANCH);
        change.setProject(TEST_PROJECT);
        change.setSubject(TEST_SUBJECT);
        change.setLastUpdated(TEST_LAST_UPDATED);
        change.setNumber(TEST_NUMBER);
        change.setUrl(TEST_URL);

        GerritPatchSet patchSet = new GerritPatchSet();
        patchSet.setNumber(TEST_PATCHSET_NUMBER);
        patchSet.setRef(TEST_REF);
        patchSet.setApprovals(TEST_APPROVALS);
        change.setPatchSet(patchSet);
    }

    @Test
    public void testAllTab() {
        assertTrue(action.isDisplayActionAllTab());
    }

    @Test
    public void testLastUpdated() {
        assertEquals(TEST_LAST_UPDATED, action.getTimePerformed());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testMostSignificantScore() {
        // null input = null output
        assertNull(action.getMostSignificantScore(null));

        List<GerritApproval> approvals = new ArrayList<>();
        // empty input = null output
        assertNull(action.getMostSignificantScore(approvals));

        // Zero-score is not possible, but if it were = null output
        GerritApproval nulApproval = new GerritApproval();
        nulApproval.setValue("0");
        nulApproval.setType("NUL");
        approvals.add(nulApproval);
        assertSame(nulApproval, action.getMostSignificantScore(approvals));

        // One negative input = negative output
        approvals.add(APPROVAL_NEGATIVE);
        assertSame(APPROVAL_NEGATIVE, action.getMostSignificantScore(approvals));

        // One negative + one positive = still negative output
        approvals.add(APPROVAL_POSITIVE);
        approvals.add(APPROVAL_POSITIVE_2);
        assertSame(APPROVAL_NEGATIVE, action.getMostSignificantScore(approvals));

        // Only positive input(s) = positive output
        approvals.remove(APPROVAL_NEGATIVE);
        assertSame(APPROVAL_POSITIVE_2, action.getMostSignificantScore(approvals));

        approvals.remove(APPROVAL_POSITIVE_2);
        assertSame(APPROVAL_POSITIVE, action.getMostSignificantScore(approvals));
    }

    @Test
    public void testPopulateVelocityParams() {
        HashMap<String, Object> velocityParams = new HashMap<>();
        @SuppressWarnings("rawtypes")
        Map expected = setUpExpectedVelocityParams();
        action.populateVelocityParams(velocityParams);
        assertEquals(expected, velocityParams);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> setUpExpectedVelocityParams() {
        return (Map<String, Object>) EasyMap.build("change", (Object) change,
                "formatLastUpdated", (Object) TEST_FORMATTED_LAST_UPDATED,
                "isoLastUpdated", (Object) TEST_ISO_LAST_UPDATED,
                "baseurl", (Object) BASE_URL);
    }
}
