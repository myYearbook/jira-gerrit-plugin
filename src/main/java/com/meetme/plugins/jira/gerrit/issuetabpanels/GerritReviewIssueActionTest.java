package com.meetme.plugins.jira.gerrit.issuetabpanels;

import static com.meetme.plugins.jira.gerrit.issuetabpanels.GerritEventKeys.APPROVALS;
import static com.meetme.plugins.jira.gerrit.issuetabpanels.GerritEventKeys.LAST_UPDATED;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.CHANGE;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.PATCHSET;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.PROJECT;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.SUBJECT;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.URL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;
import com.meetme.plugins.jira.gerrit.data.dto.GerritApproval;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.meetme.plugins.jira.gerrit.data.dto.GerritPatchSet;

public class GerritReviewIssueActionTest extends TestCase {
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

    private static final ArrayList<GerritApproval> TEST_APPROVALS = new ArrayList<GerritApproval>();
    private static final GerritApproval APPROVAL_NEGATIVE = new GerritApproval();
    private static final GerritApproval APPROVAL_POSITIVE = new GerritApproval();
    private static final GerritApproval APPROVAL_POSITIVE_2 = new GerritApproval();
    private static final GerritApproval TEST_MOST_SIGNIFICANT_APPROVAL = APPROVAL_NEGATIVE;

    @Mock
    private IssueTabPanelModuleDescriptor descriptor;
    @Mock
    private DateTimeFormatterFactory dateTimeFormatterFactory;

    private GerritChange change;

    private GerritReviewIssueAction action;

    @Before
    protected void setUp() throws Exception {
        super.setUp();
        initMocks(this);

        setUpDateTimeFormatter();
        setUpApprovals();
        setUpGerritChange();
        action = new GerritReviewIssueAction(descriptor, change, dateTimeFormatterFactory, BASE_URL);

    }

    @After
    protected void tearDown() throws Exception {
        change = null;
        action = null;
    }

    private void setUpDateTimeFormatter() {
        DateTimeFormatter mockFormatter = mock(DateTimeFormatter.class);
        when(dateTimeFormatterFactory.formatter()).thenReturn(mockFormatter);
        when(mockFormatter.format(TEST_LAST_UPDATED)).thenReturn(TEST_FORMATTED_LAST_UPDATED);

        DateTimeFormatter mockFormatterIso = mock(DateTimeFormatter.class);
        when(mockFormatter.withStyle(DateTimeStyle.ISO_8601_DATE_TIME)).thenReturn(mockFormatterIso);
        when(mockFormatterIso.format(TEST_LAST_UPDATED)).thenReturn(TEST_ISO_LAST_UPDATED);
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

    @Test
    public void testMostSignificantScore() {

        // null input = null output
        assertNull(action.getMostSignificantScore(null));

        List<GerritApproval> approvals = new ArrayList<GerritApproval>();
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
        HashMap<String, Object> velocityParams = new HashMap<String, Object>();
        @SuppressWarnings("rawtypes")
        Map expected = setUpExpectedVelocityParams();
        action.populateVelocityParams(velocityParams);
        assertEquals(expected, velocityParams);
    }

    @Test
    public void testPopulateVelocityParams_PostiveSignificant() {
        HashMap<String, Object> velocityParams = new HashMap<String, Object>();
        Map<String, Object> expected = setUpExpectedVelocityParams();

        TEST_APPROVALS.remove(APPROVAL_NEGATIVE);
        expected.put("mostSignificantScore", APPROVAL_POSITIVE_2);

        action.populateVelocityParams(velocityParams);
        assertEquals(expected, velocityParams);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> setUpExpectedVelocityParams() {
        return EasyMap.build(URL, TEST_URL,
                SUBJECT, TEST_SUBJECT,
                PROJECT, TEST_PROJECT,
                CHANGE, TEST_NUMBER,
                PATCHSET, TEST_PATCHSET_NUMBER,
                LAST_UPDATED, TEST_FORMATTED_LAST_UPDATED,
                "isoLastUpdated", TEST_ISO_LAST_UPDATED,
                APPROVALS, TEST_APPROVALS,
                "mostSignificantScore", TEST_MOST_SIGNIFICANT_APPROVAL,
                "baseurl", BASE_URL);
    }
}
