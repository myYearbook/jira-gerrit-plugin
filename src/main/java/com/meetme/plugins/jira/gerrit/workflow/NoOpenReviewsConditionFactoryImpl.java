package com.meetme.plugins.jira.gerrit.workflow;

import java.util.Map;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginConditionFactory;
import com.meetme.plugins.jira.gerrit.workflow.condition.NoOpenReviews;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.ConditionDescriptor;

public class NoOpenReviewsConditionFactoryImpl extends AbstractWorkflowPluginFactory implements WorkflowPluginConditionFactory {

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, ?> getDescriptorParams(Map<String, Object> conditionParams) {
        if (conditionParams != null && conditionParams.containsKey(NoOpenReviews.KEY_REVERSED))
        {
            return EasyMap.build(NoOpenReviews.KEY_REVERSED, extractSingleParam(conditionParams, NoOpenReviews.KEY_REVERSED));
        }

        return EasyMap.build();
    }

    @Override
    protected void getVelocityParamsForEdit(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
        velocityParams.put(NoOpenReviews.KEY_REVERSED, isReversed(descriptor));
    }

    @Override
    protected void getVelocityParamsForInput(Map<String, Object> velocityParams) {
        // Nothing to choose from, because boolean is only ON/OFF
    }

    @Override
    protected void getVelocityParamsForView(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
        velocityParams.put(NoOpenReviews.KEY_REVERSED, isReversed(descriptor));
    }

    private boolean isReversed(AbstractDescriptor descriptor) {
        if (!(descriptor instanceof ConditionDescriptor)) {
            throw new IllegalArgumentException("Descriptor must be a ConditionDescriptor.");
        }

        ConditionDescriptor conditionDescriptor = (ConditionDescriptor) descriptor;

        String value = (String) conditionDescriptor.getArgs().get(NoOpenReviews.KEY_REVERSED);
        return Boolean.parseBoolean(value);
    }
}
