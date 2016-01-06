package com.meetme.plugins.jira.gerrit.workflow;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.mockito.Mock;
import org.mockito.Mockito;

import com.atlassian.core.util.collection.EasyList;
import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.mock.issue.MockIssue;
import com.atlassian.jira.user.MockUser;
import com.atlassian.jira.user.util.MockUserManager;
import com.atlassian.jira.user.util.UserManager;
import com.meetme.plugins.jira.gerrit.data.GerritConfiguration;
import com.meetme.plugins.jira.gerrit.data.IssueReviewsManager;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.opensymphony.workflow.WorkflowContext;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;

/**
 * Base class for setting up mocks
 *
 * @since v5.0
 */
public abstract class AbstractWorkflowTest {
    @SuppressWarnings("rawtypes")
    protected Map transientVars, args;
    protected MockComponentWorker mockComponents;
    protected MutableIssue mockIssue;

    @Mock
    protected ApplicationUser mockUser;
    @Mock
    protected IssueReviewsManager reviewsManager;
    @Mock
    protected GerritConfiguration configuration;
    @Mock
    protected WorkflowContext workflowContext;

    public void setUp() throws Exception {
        initMocks(this);
        createMocks();
        stubMockMethods();
    }

    public void tearDown() throws Exception {
    }

    protected void setUpConfiguration() {
        when(configuration.getSshHostname()).thenReturn("gerrit.company.com");
        when(configuration.getSshUsername()).thenReturn("jira");
        File file = mock(File.class);
        when(configuration.getSshPrivateKey()).thenReturn(file);
        when(file.exists()).thenReturn(true);
    }

    protected void setUpUser() {
        when(workflowContext.getCaller()).thenReturn(mockUser.getName());
  }

    private void createMocks() {
        mockComponents = new MockComponentWorker();
        mockIssue = new MockIssue();

        when(mockUser.getName()).thenReturn("milton");
    }

    private void stubMockMethods() {
        ComponentAccessor.initialiseWorker(mockComponents);
        setUpConfiguration();

        mockIssue.setKey("FOO-123");

        transientVars = EasyMap.build("issue", mockIssue, "context", workflowContext);
        args = EasyMap.build("username", mockUser.getName());
    }

    protected void stubFailingReviews() throws GerritQueryException {
        GerritQueryException gqe = new GerritQueryException("Expected exception");
        when(reviewsManager.getReviewsForIssue(Mockito.anyString())).thenThrow(gqe);
    }

    @SuppressWarnings("unchecked")
    protected void stubEmptyReviews() throws GerritQueryException {
        @SuppressWarnings("rawtypes")
        List reviews = EasyList.build();
        when(reviewsManager.getReviewsForIssue(Mockito.anyString())).thenReturn(reviews);
    }

    @SuppressWarnings("unchecked")
    protected void stubOneReview() throws GerritQueryException {
        GerritChange change = mock(GerritChange.class);
        @SuppressWarnings("rawtypes")
        List reviews = EasyList.build(change);
        when(reviewsManager.getReviewsForIssue(Mockito.anyString())).thenReturn(reviews);
    }

}
