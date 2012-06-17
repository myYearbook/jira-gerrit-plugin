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

import java.util.Map;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginFunctionFactory;
import com.meetme.plugins.jira.gerrit.workflow.function.ApprovalFunction;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.FunctionDescriptor;

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
        velocityParams.put(ApprovalFunction.KEY_CMD_ARGS, getCommandArgs(descriptor));
    }

    @Override
    protected void getVelocityParamsForInput(Map<String, Object> velocityParams) {
        velocityParams.put(ApprovalFunction.KEY_CMD_ARGS, ApprovalFunction.DEFAULT_CMD_ARGS);
    }

    @Override
    protected void getVelocityParamsForView(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
        velocityParams.put(ApprovalFunction.KEY_CMD_ARGS, getCommandArgs(descriptor));
    }

    private Object getCommandArgs(AbstractDescriptor descriptor) {
        if (!(descriptor instanceof FunctionDescriptor)) {
            throw new IllegalArgumentException("Descriptor must be a FunctionDescriptor.");
        }

        FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
        String args = (String) functionDescriptor.getArgs().get(ApprovalFunction.KEY_CMD_ARGS);

        if (args != null && args.trim().length() > 0) {
            return args;
        } else {
            return ApprovalFunction.DEFAULT_CMD_ARGS;
        }
    }
}
