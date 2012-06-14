package com.meetme.plugins.jira.gerrit.data.dto;

import static com.meetme.plugins.jira.gerrit.issuetabpanels.GerritEventKeys.LAST_UPDATED;

import java.util.Date;

import net.sf.json.JSONObject;

import com.meetme.plugins.jira.gerrit.issuetabpanels.GerritEventKeys;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.attr.Change;

public class GerritChange extends Change {

    public GerritChange(JSONObject obj) {
        super(obj);
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public GerritPatchSet getPatchSet() {
        return patchSet;
    }

    public void setPatchSet(GerritPatchSet patchSet) {
        this.patchSet = patchSet;
    }

    private Date lastUpdated;
    private GerritPatchSet patchSet;

    @Override
    public void fromJson(JSONObject json) {
        super.fromJson(json);

        this.lastUpdated = new Date(1000 * json.getLong(LAST_UPDATED));

        if (json.containsKey(GerritEventKeys.CURRENT_PATCH_SET)) {
            this.patchSet = new GerritPatchSet(json.getJSONObject(GerritEventKeys.CURRENT_PATCH_SET));
        }
    }
}
