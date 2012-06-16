package com.meetme.plugins.jira.gerrit.data.dto;

import static com.meetme.plugins.jira.gerrit.issuetabpanels.GerritEventKeys.BY;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.EMAIL;
import static com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys.NAME;
import net.sf.json.JSONObject;

import com.atlassian.crowd.embedded.api.User;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.attr.Approval;

public class GerritApproval extends Approval implements Comparable<GerritApproval> {
    /** The approver's name */
    private String by;
    /** The approver's email */
    private String byEmail;
    /** The JIRA user associated with the same email */
    private User user;

    /**
     * Creates the PatchSetApproval from a {@link JSONObject}.
     * 
     * @param json
     */
    public GerritApproval(JSONObject json) {
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
    public int compareTo(GerritApproval o) {
        int lhs = getValueAsInt();
        int rhs = o.getValueAsInt();

        if (lhs == rhs) {
            return 0;
        }

        return lhs > rhs ? 1 : -1;
    }

    @Override
    public String toString() {
        return super.toString() + " by " + getBy();
    }
}
