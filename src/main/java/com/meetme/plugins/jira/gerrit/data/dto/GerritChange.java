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

import static com.meetme.plugins.jira.gerrit.issuetabpanels.GerritEventKeys.LAST_UPDATED;

import java.util.Date;

import net.sf.json.JSONObject;

import com.meetme.plugins.jira.gerrit.issuetabpanels.GerritEventKeys;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.dto.attr.Change;

public class GerritChange extends Change {

    public GerritChange() {
        super();
    }

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
