package com.meetme.plugins.jira.gerrit.workflow;

import java.util.Map;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginFunctionFactory;
import com.meetme.plugins.jira.gerrit.workflow.function.ApprovalFunction;
import com.opensymphony.workflow.loader.AbstractDescriptor;

public class ApproveReviewFactoryImpl extends AbstractWorkflowPluginFactory implements
        WorkflowPluginFunctionFactory {

    // 1. Review type and score (e.g., "--verified 1") -- one input or two?
    // 2. Message (e.g., --message "Ready for Test")
    // 3. Submit (--submit)

    // OR: [args] and let the admin fill it out?

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, ?> getDescriptorParams(Map<String, Object> params) {
        if (params != null && params.containsKey(ApprovalFunction.KEY_CMD_ARGS)) {
            return EasyMap.build(ApprovalFunction.KEY_CMD_ARGS, extractSingleParam(params, ApprovalFunction.KEY_CMD_ARGS));
        }

        // Create a 'hard coded' parameter
        return EasyMap.build(ApprovalFunction.KEY_CMD_ARGS, ApprovalFunction.DEFAULT_CMD_ARGS);
    }

    @Override
    protected void getVelocityParamsForEdit(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {

    }

    @Override
    protected void getVelocityParamsForInput(Map<String, Object> velocityParams) {

    }

    @Override
    protected void getVelocityParamsForView(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {

    }
}
