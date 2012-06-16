package com.meetme.plugins.jira.gerrit.data.dto;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.GerritEventKeys;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.attr.PatchSet;

public class GerritPatchSet extends PatchSet {
    private List<GerritApproval> approvals;

    public GerritPatchSet(JSONObject json) {
        super(json);
    }

    @Override
    public void fromJson(JSONObject json) {
        super.fromJson(json);

        if (json.containsKey(GerritEventKeys.APPROVALS)) {
            JSONArray eventApprovals = json.getJSONArray(GerritEventKeys.APPROVALS);
            approvals = new ArrayList<GerritApproval>(eventApprovals.size());

            for (int i = 0; i < eventApprovals.size(); i++) {
                approvals.add(new GerritApproval(eventApprovals.getJSONObject(i)));
            }
        }
    }

    public List<GerritApproval> getApprovals() {
        return approvals;
    }

    public void setApprovals(List<GerritApproval> approvals) {
        this.approvals = approvals;
    }
}
