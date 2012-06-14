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
package com.meetme.plugins.jira.gerrit.issuetabpanels;

import static com.meetme.plugins.jira.gerrit.issuetabpanels.GerritEventKeys.LAST_UPDATED;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.APPROVALS;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.CHANGE;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.PATCHSET;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.PROJECT;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.SUBJECT;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.URL;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueAction;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;
import com.atlassian.jira.user.util.UserManager;
import com.meetme.plugins.jira.gerrit.data.dto.GerritApproval;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;

public class GerritReviewIssueAction extends AbstractIssueAction implements IssueAction {
    private static final String TEMPLATE_DIRECTORY = "templates/";
    private static final String TEMPLATE_NAME = "gerrit-reviews-tabpanel-item.vm";

    private DateTimeFormatterFactory dateTimeFormatterFactory;
    private UserManager userManager;
    private String baseUrl;
    private GerritChange change;

    public GerritReviewIssueAction(IssueTabPanelModuleDescriptor descriptor, GerritChange change, UserManager userManager,
            DateTimeFormatterFactory dateTimeFormatterFactory, String baseUrl) {
        super(descriptor);
        this.userManager = userManager;
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
        this.baseUrl = baseUrl;
        this.change = change;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void populateVelocityParams(@SuppressWarnings("rawtypes") Map params) {
        DateTimeFormatter formatter = dateTimeFormatterFactory.formatter();
        params.putAll(EasyMap.build(URL, change.getUrl(),
                SUBJECT, change.getSubject(),
                PROJECT, change.getProject(),
                CHANGE, change.getNumber(),
                PATCHSET, change.getPatchSet().getNumber(),
                LAST_UPDATED, formatter.format(change.getLastUpdated()),
                "isoLastUpdated", formatter.withStyle(DateTimeStyle.ISO_8601_DATE_TIME).format(change.getLastUpdated()),
                APPROVALS, change.getPatchSet().getApprovals(),
                "mostSignificantScore", getMostSignificantScore(change.getPatchSet().getApprovals()),
                "baseurl", this.baseUrl));
    }

    @Override
    public Date getTimePerformed() {
        return change.getLastUpdated();
    }

    @Override
    public boolean isDisplayActionAllTab() {
        return true;
    }

    private GerritApproval getMostSignificantScore(final List<GerritApproval> approvals) {
        try {
            GerritApproval min = Collections.min(approvals);
            GerritApproval max = Collections.max(approvals);

            if (min == max) {
                // Means there was only 1 vote, so show that one.
                return max;
            }

            if (min.getValueAsInt() < 0) {
                // There exists a negative vote, so show that one.
                return min;
            } else if (max.getValueAsInt() > 0) {
                // No negative votes, and some positive vote, so show the highest positive vote
                return max;
            }
        } catch (NoSuchElementException nsee) {
        }

        return null;
    }
}
