package com.meetme.plugins.jira.gerrit.webpanel;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.MockProject;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.meetme.plugins.jira.gerrit.data.GerritConfiguration;
import com.meetme.plugins.jira.gerrit.data.IssueReviewsManager;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ShowReviewsWebPanelConditionTest {

    ShowReviewsWebPanelCondition showReviewsWebPanelCondition;

    @Mock
    private GerritConfiguration gerritConfiguration;

    @Mock
    private Issue issue;

    @Mock
    private IssueReviewsManager issueReviewsManager;

    @Mock
    private ProjectManager projectManager;

    private static final List<Project> projects = Collections.unmodifiableList(new ArrayList<MockProject>() {{
        add(new MockProject(0L, "KEY_0L", "NAME_0L"));
        add(new MockProject(1L, "KEY_1L", "NAME_1L"));
        add(new MockProject(2L, "KEY_2L", "NAME_2L"));
    }});

    @Before
    public void setUp() {
        initMocks(this);
        when(issue.getProjectId()).thenReturn(1L);
        when(issue.getId()).thenReturn(10000000L);
        when(projectManager.getProjects()).thenReturn(projects);
        showReviewsWebPanelCondition = new ShowReviewsWebPanelCondition(issueReviewsManager, gerritConfiguration);
        when(gerritConfiguration.getUseGerritProjectWhitelist()).thenReturn(true);
    }

    @Test
    public void shouldDisplayWithAlwaysFlag() throws GerritQueryException {
        when(gerritConfiguration.getShowsEmptyPanel()).thenReturn(true);
        when(issueReviewsManager.getReviewsForIssue(any(Issue.class))).thenReturn(Lists.newArrayList());
        when(gerritConfiguration.getIdsOfKnownGerritProjects()).thenReturn(projects.stream().map(p -> p.getId()
                .toString()).collect(Collectors.toList()));

        assertTrue(showReviewsWebPanelCondition.shouldDisplay(singletonMap("issue", (Object) issue)));
    }

    @Test
    public void shouldDisplayMapEqualsNull() {
        when(gerritConfiguration.getShowsEmptyPanel()).thenReturn(false);

        assertFalse(showReviewsWebPanelCondition.shouldDisplay(null));
    }

    @Test
    public void shouldDisplayMapWithoutIssue() {
        when(gerritConfiguration.getShowsEmptyPanel()).thenReturn(false);

        assertFalse(showReviewsWebPanelCondition.shouldDisplay(Maps.newHashMap()));
    }

    @Test
    public void shouldDisplayEmptyWhiteList(){
        when(gerritConfiguration.getShowsEmptyPanel()).thenReturn(false);
        when(gerritConfiguration.getIdsOfKnownGerritProjects()).thenReturn(Lists.newArrayList());

        assertFalse(showReviewsWebPanelCondition.shouldDisplay(singletonMap("issue", (Object) issue)));
    }

    @Test
    public void shouldDisplayProjectIsOnWhiteList() throws Exception {
        when(gerritConfiguration.getShowsEmptyPanel()).thenReturn(false);
        when(issueReviewsManager.getReviewsForIssue(any(Issue.class))).thenReturn(singletonList(new GerritChange()));
        when(gerritConfiguration.getIdsOfKnownGerritProjects()).thenReturn(projects.stream().map(p -> p.getId()
                .toString()).collect(Collectors.toList()));
        assertTrue(showReviewsWebPanelCondition.shouldDisplay(singletonMap("issue", (Object) issue)));
    }

    @Test
    public void shouldDisplayProjectIsNotOnWhiteList() {
        when(gerritConfiguration.getShowsEmptyPanel()).thenReturn(false);
        when(gerritConfiguration.getIdsOfKnownGerritProjects()).thenReturn(projects.stream().filter(project -> ! project
                .getId().equals(1L)).map(project -> project.getId().toString()).collect(Collectors.toList()));
        assertFalse(showReviewsWebPanelCondition.shouldDisplay(singletonMap("issue", (Object) issue)));
    }

    @Test
    public void shouldDisplayNoConnectionToGerrit() throws GerritQueryException {


        when(gerritConfiguration.getIdsOfKnownGerritProjects()).thenReturn(projects.stream().map(p -> p.getId()
                .toString()).collect(Collectors.toList()));
        when(issueReviewsManager.getReviewsForIssue(any(Issue.class))).thenThrow(new GerritQueryException());

        when(gerritConfiguration.getShowsEmptyPanel()).thenReturn(true);
        assertTrue(showReviewsWebPanelCondition.shouldDisplay(singletonMap("issue", (Object) issue)));

        when(gerritConfiguration.getShowsEmptyPanel()).thenReturn(false);
        assertFalse(showReviewsWebPanelCondition.shouldDisplay(singletonMap("issue", (Object) issue)));
    }

    @Test
    public void issuePanelshouldDisplayEvenGerritWhitelistIsOff() {
        final GerritConfiguration gerritConfiguration = mock(GerritConfiguration.class);
        when(gerritConfiguration.getUseGerritProjectWhitelist()).thenReturn(false);
        final Issue issue = mock(Issue.class);
        when(gerritConfiguration.getShowsEmptyPanel()).thenReturn(true);
        Map<String, Object> map = new HashMap<>();
        map.put("issue", issue);
        ShowReviewsWebPanelCondition showReviewsWebPanelCondition = new ShowReviewsWebPanelCondition(null,
                gerritConfiguration);
        final boolean shouldDisplay = showReviewsWebPanelCondition.shouldDisplay(map);
        Assert.assertThat(shouldDisplay, Is.is(true));
    }
}