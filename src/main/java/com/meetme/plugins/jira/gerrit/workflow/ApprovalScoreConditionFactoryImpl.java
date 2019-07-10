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
package com.meetme.plugins.jira.gerrit.workflow;

import com.meetme.plugins.jira.gerrit.data.dto.GerritApproval;
import com.meetme.plugins.jira.gerrit.workflow.condition.ApprovalScore;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginConditionFactory;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.ConditionDescriptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ApprovalScoreConditionFactoryImpl extends AbstractWorkflowPluginFactory implements WorkflowPluginConditionFactory {
    private static final List<String> ALL_PARAMS = Collections.unmodifiableList(Arrays.asList(ApprovalScore.KEY_NEGATIVE,
            ApprovalScore.KEY_COMPARISON, ApprovalScore.KEY_TARGET, ApprovalScore.KEY_LABEL));

    private static final boolean DEFAULT_NEGATIVE = false;
    private static final ApprovalScore.ComparisonOperator DEFAULT_COMPARISON = ApprovalScore.ComparisonOperator.EQUAL_TO;
    private static final int DEFAULT_TARGET = 0;
    private static final String DEFAULT_LABEL = "Code-Review";

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, ?> getDescriptorParams(Map<String, Object> conditionParams) {
        if (conditionParams != null && conditionParams.containsKey(ApprovalScore.KEY_NEGATIVE)
                && conditionParams.containsKey(ApprovalScore.KEY_COMPARISON) && conditionParams.containsKey(ApprovalScore.KEY_TARGET)
                && conditionParams.containsKey(ApprovalScore.KEY_LABEL)) {
            return extractMultipleParams(conditionParams, ALL_PARAMS);
        }

        return EasyMap.build();
    }

    @Override
    protected void getVelocityParamsForEdit(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
        velocityParams.put(ApprovalScore.KEY_NEGATIVE, isReversed(descriptor));
        velocityParams.put(ApprovalScore.KEY_COMPARISON, getComparison(descriptor));
        velocityParams.put(ApprovalScore.KEY_TARGET, getTarget(descriptor));
        velocityParams.put(ApprovalScore.KEY_LABEL, getLabel(descriptor));
    }

    @Override
    protected void getVelocityParamsForInput(Map<String, Object> velocityParams) {
        velocityParams.put(ApprovalScore.KEY_NEGATIVE, DEFAULT_NEGATIVE);
        velocityParams.put(ApprovalScore.KEY_COMPARISON, DEFAULT_COMPARISON);
        velocityParams.put(ApprovalScore.KEY_TARGET, DEFAULT_TARGET);
        velocityParams.put(ApprovalScore.KEY_LABEL, DEFAULT_LABEL);
    }

    @Override
    protected void getVelocityParamsForView(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
        velocityParams.put(ApprovalScore.KEY_NEGATIVE, isReversed(descriptor));
        velocityParams.put(ApprovalScore.KEY_COMPARISON, getComparison(descriptor));
        velocityParams.put(ApprovalScore.KEY_TARGET, getTarget(descriptor));
        velocityParams.put(ApprovalScore.KEY_LABEL, getLabel(descriptor));
    }

    private String getStringFromDescriptor(AbstractDescriptor descriptor, String key) {
        if (!(descriptor instanceof ConditionDescriptor)) {
            throw new IllegalArgumentException("Descriptor must be a ConditionDescriptor.");
        }

        ConditionDescriptor conditionDescriptor = (ConditionDescriptor) descriptor;

        return (String) conditionDescriptor.getArgs().get(key);
    }

    private boolean isReversed(AbstractDescriptor descriptor) {
        String value = getStringFromDescriptor(descriptor, ApprovalScore.KEY_NEGATIVE);
        return Boolean.parseBoolean(value);
    }

    private String getLabel(AbstractDescriptor descriptor) {
        return GerritApproval.getUpgradedLabelType(getStringFromDescriptor(descriptor, ApprovalScore.KEY_LABEL));
    }

    private int getTarget(AbstractDescriptor descriptor) {
        String value = getStringFromDescriptor(descriptor, ApprovalScore.KEY_TARGET);
        return Integer.parseInt(value);
    }

    private ApprovalScore.ComparisonOperator getComparison(AbstractDescriptor descriptor) {
        String value = getStringFromDescriptor(descriptor, ApprovalScore.KEY_COMPARISON);
        return ApprovalScore.ComparisonOperator.valueOf(value);
    }
}
