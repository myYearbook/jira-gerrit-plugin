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
