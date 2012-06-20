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

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueAction;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;

/**
 * @author jhansche
 */
public class SubtaskReviewsIssueAction extends AbstractIssueAction implements IssueAction {

    private Issue subtask;
    private List<GerritChange> changes;

    /**
     * @param descriptor
     */
    public SubtaskReviewsIssueAction(IssueTabPanelModuleDescriptor descriptor, Issue subtask, List<GerritChange> changes) {
        super(descriptor);

        this.subtask = subtask;
        this.changes = changes;
    }

    @Override
    public Date getTimePerformed() {
        return subtask.getUpdated();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void populateVelocityParams(@SuppressWarnings("rawtypes") Map velocityParams) {
        // push data
        velocityParams.put("subtask", subtask);
        velocityParams.put("changes", changes);
    }

    @Override
    public boolean isDisplayActionAllTab() {
        return false;
    }
}
