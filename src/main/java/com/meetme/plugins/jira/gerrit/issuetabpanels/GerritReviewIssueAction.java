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

import static com.meetme.plugins.jira.gerrit.issuetabpanels.GerritEventKeys.BY;
import static com.meetme.plugins.jira.gerrit.issuetabpanels.GerritEventKeys.CURRENT_PATCH_SET;
import static com.meetme.plugins.jira.gerrit.issuetabpanels.GerritEventKeys.LAST_UPDATED;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.APPROVALS;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.CHANGE;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.EMAIL;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.NAME;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.NUMBER;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.PATCHSET;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.PROJECT;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.SUBJECT;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;

import net.sf.json.JSONObject;

import org.apache.velocity.exception.VelocityException;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.velocity.VelocityManager;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.attr.Approval;

public class GerritReviewIssueAction implements IssueAction {
    private static final String TEMPLATE_DIRECTORY = "templates/";
    private static final String TEMPLATE_NAME = "gerrit-reviews-tabpanel-item.vm";

    private String url;
    private String subject;
    private String changeId;
    private String patchSet;
    private ArrayList<PatchSetApproval> approvals = new ArrayList<PatchSetApproval>();;
    private Object project;
    private Date lastUpdated;
    private DateTimeFormatterFactory dateTimeFormatterFactory;
    private UserManager userManager;
    private String baseUrl;

    public GerritReviewIssueAction(JSONObject review, UserManager userManager, DateTimeFormatterFactory dateTimeFormatterFactory, String baseUrl) {
        this.userManager = userManager;
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
        this.fromJson(review);
        this.baseUrl = baseUrl;
    }

    /**
     * Initializes the review action from a {@link JSONObject}.
     * 
     * @param review The {@link JSONObject}, as returned from the Gerrit server.
     */
    private void fromJson(JSONObject review) {
        this.url = review.getString(URL);
        this.subject = review.getString(SUBJECT);
        this.changeId = review.getString(NUMBER);
        this.project = review.getString(PROJECT);
        this.lastUpdated = new Date(1000 * review.getLong(LAST_UPDATED));

        JSONObject currentPatchSet = review.getJSONObject(CURRENT_PATCH_SET);

        this.patchSet = currentPatchSet.getString(NUMBER);

        if (currentPatchSet.containsKey(APPROVALS)) {
            for (Object obj : currentPatchSet.getJSONArray(APPROVALS)) {
                if (obj instanceof JSONObject) {
                    PatchSetApproval approval = new PatchSetApproval((JSONObject) obj);
                    approval.setUser(getUserByEmail(approval.getByEmail()));

                    approvals.add(approval);
                }
            }
        }
    }

    /**
     * Returns the JIRA {@link User} object associated with the given email address.
     * 
     * @param email
     * @return
     */
    private User getUserByEmail(String email) {
        User user = null;

        if (email != null) {
            for (User iUser : userManager.getUsers()) {
                if (email.equalsIgnoreCase(iUser.getEmailAddress()))
                {
                    user = iUser;
                    break;
                }
            }
        }

        return user;
    }

    @Override
    public String getHtml() {
        VelocityManager vm = ComponentAccessor.getVelocityManager();
        DateTimeFormatter formatter = dateTimeFormatterFactory.formatter();

        @SuppressWarnings("rawtypes")
        Map params = EasyMap.build(URL, url,
                SUBJECT, subject,
                PROJECT, project,
                CHANGE, changeId,
                PATCHSET, patchSet,
                LAST_UPDATED, formatter.format(lastUpdated),
                "isoLastUpdated", formatter.withStyle(DateTimeStyle.ISO_8601_DATE_TIME).format(lastUpdated),
                APPROVALS, approvals,
                "mostSignificantScore", getMostSignificantScore(approvals),
                "baseurl", this.baseUrl);

        try {
            return vm.getBody(TEMPLATE_DIRECTORY, TEMPLATE_NAME, params);
        } catch (VelocityException e) {
            e.printStackTrace();
            return "Velocity template generation failed: " + e.getMessage();
        }
    }

    private PatchSetApproval getMostSignificantScore(final ArrayList<PatchSetApproval> approvals) {
        try {
            PatchSetApproval min = Collections.min(approvals);
            PatchSetApproval max = Collections.max(approvals);

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

    @Override
    public Date getTimePerformed() {
        return lastUpdated;
    }

    @Override
    public boolean isDisplayActionAllTab() {
        return true;
    }

    /**
     * Extension of {@link Approval} that includes the approver's name.
     * 
     * @author jhansche
     */
    public static class PatchSetApproval extends Approval implements Comparable<PatchSetApproval>
    {
        /**
         * The approver's name
         */
        private String by;
        private String byEmail;
        private User user;

        /**
         * Creates the PatchSetApproval from a {@link JSONObject}.
         * 
         * @param json
         */
        public PatchSetApproval(JSONObject json) {
            this.fromJson(json);
        }

        public void setUser(User user) {
            this.user = user;
        }

        public User getUser() {
            return this.user;
        }

        @Override
        public void fromJson(JSONObject json) {
            super.fromJson(json);

            if (json.containsKey(BY)) {
                JSONObject by = json.getJSONObject(BY);

                if (by.containsKey(NAME))
                {
                    this.setBy(by.getString(NAME));
                }

                if (by.containsKey(EMAIL)) {
                    this.setByEmail(by.getString(EMAIL));
                }
            }
        }

        /**
         * Returns the approver's name.
         * 
         * @return Approver's name as a string.
         */
        public String getBy() {
            return by;
        }

        /**
         * Sets the approver's name.
         * 
         * @param by Approver's name
         */
        public void setBy(String by) {
            this.by = by;
        }

        /**
         * Returns the approval score as an integer.
         * 
         * @return
         */
        public int getValueAsInt() {
            return Integer.parseInt(getValue(), 10);
        }

        public String getByEmail() {
            return byEmail;
        }

        public void setByEmail(String byEmail) {
            this.byEmail = byEmail;
        }

        @Override
        public int compareTo(PatchSetApproval o) {
            int lhs = getValueAsInt();
            int rhs = o.getValueAsInt();

            if (lhs == rhs) {
                return 0;
            }

            return lhs > rhs ? 1 : -1;
        }
    }
}
