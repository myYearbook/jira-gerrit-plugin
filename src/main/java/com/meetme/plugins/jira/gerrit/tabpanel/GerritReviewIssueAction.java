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

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueAction;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;
import com.atlassian.jira.web.util.OutlookDate;
import com.meetme.plugins.jira.gerrit.data.dto.GerritApproval;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;

import java.util.*;

public class GerritReviewIssueAction extends AbstractIssueAction implements IssueAction {
    private String baseUrl;
    private GerritChange change;
    private OutlookDate dateTimeFormatter;

    public GerritReviewIssueAction(IssueTabPanelModuleDescriptor descriptor, GerritChange change,
            OutlookDate dateTimeFormatter, String baseUrl) {
        super(descriptor);
        this.dateTimeFormatter = dateTimeFormatter;
        this.baseUrl = baseUrl;
        this.change = change;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void populateVelocityParams(@SuppressWarnings("rawtypes") Map params) {
        params.putAll(EasyMap.build("change", change,
                "formatLastUpdated", formatLastUpdated(),
                "isoLastUpdated", isoFormatLastUpdated(),
                "baseurl", this.baseUrl));
    }

    String formatLastUpdated() {
        return dateTimeFormatter.format(change.getLastUpdated());
    }

    String isoFormatLastUpdated() {
        return dateTimeFormatter.formatIso8601(change.getLastUpdated());
    }

    @Override
    public Date getTimePerformed() {
        return change.getLastUpdated();
    }

    @Override
    public boolean isDisplayActionAllTab() {
        return true;
    }

    /**
     * Returns the lowest score below 0 if available; otherwise the highest score above 0.
     *
     * @param approvals the approvals found on the Gerrit review
     * @return the approval that is deemed the "most significant"
     * @deprecated This functionality can now be found in the velocity template
     */
    @Deprecated
    GerritApproval getMostSignificantScore(final List<GerritApproval> approvals) {
        if (approvals != null) {
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
                } else {
                    // NOTE: Technically not possible to have a 0-score, but if one exists, use it!
                    // No negative votes, so show the highest positive vote
                    return max;
                }
            } catch (NoSuchElementException nsee) {
                // Collection was empty
            }
        }

        return null;
    }
}
