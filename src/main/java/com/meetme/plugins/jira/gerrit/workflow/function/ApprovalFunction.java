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
package com.meetme.plugins.jira.gerrit.workflow.function;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.meetme.plugins.jira.gerrit.data.GerritConfiguration;
import com.meetme.plugins.jira.gerrit.data.IssueReviewsManager;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;

/**
 * <p>
 * A Workflow Function that can be used to perform Gerrit approvals as the result of a workflow
 * transition. The input argument is simply a command line argument string (such as "--verified +1",
 * or "--submit", etc). The argument will be appended to the <tt>gerrit review [ChangeId] ...</tt>
 * command line.
 * </p>
 * 
 * <p>
 * This function can be used in combination with {@link ApprovalScore} workflow conditions, such
 * that, e.g., a "Merge Change" workflow transition can be used to automatically "submit" a Gerrit
 * review, iff all of the following conditions are met:
 * <ul>
 * <li>MUST have a CRVW score &gt;= 2</li>
 * <li>MUST have a VRIF score &gt;= 1</li>
 * <li>Must NOT have a CRVW score &lt; 0</li>
 * </ul>
 * </p>
 * 
 * <p>
 * This ensures that the workflow transition is only available if the "submit" step will be
 * successful.
 * <p>
 * 
 * <p>
 * Another common use for this function would be to automatically provide a "Verified +1" score, via
 * another workflow step, e.g., "Ready for Merge". In that way, a "Ready for Merge" transition may
 * then automatically enable the "Merge Change" transition, as a result of giving the Verified +1
 * score.
 * </p>
 * 
 * @author jhansche
 */
public class ApprovalFunction extends AbstractJiraFunctionProvider {
    private static final Logger log = LoggerFactory.getLogger(ApprovalFunction.class);

    public static final String KEY_CMD_ARGS = "cmdArgs";
    public static final String DEFAULT_CMD_ARGS = "--verified 1 --submit";

    private final IssueReviewsManager reviewsManager;
    private final GerritConfiguration configuration;

    public ApprovalFunction(GerritConfiguration configuration, IssueReviewsManager reviewsManager) {
        super();

        this.configuration = configuration;
        this.reviewsManager = reviewsManager;
    }

    private boolean isConfigurationReady() {
        return configuration.getSshHostname() != null && configuration.getSshUsername() != null
                && configuration.getSshPrivateKey() != null && configuration.getSshPrivateKey().exists();
    }

    @Override
    public void execute(@SuppressWarnings("rawtypes") Map transientVars, @SuppressWarnings("rawtypes") Map args, PropertySet ps)
            throws WorkflowException {
        if (!isConfigurationReady()) {
            throw new IllegalStateException("Configure the Gerrit integration from the Administration panel first.");
        }

        String issueKey = getIssue(transientVars).getKey();
        List<GerritChange> issueReviews;
        String cmdArgs = (String) args.get(KEY_CMD_ARGS);

        try {
            issueReviews = reviewsManager.getReviews(issueKey);
        } catch (GerritQueryException e) {
            throw new WorkflowException("Unable to retrieve associated reviews", e);
        }

        Collections.sort(issueReviews, SortByChangeId.INSTANCE);

        for (GerritChange change : issueReviews) {
            log.debug("Attempting to approve " + change);

            try {
                reviewsManager.doApproval(issueKey, change, cmdArgs);
            } catch (IOException e) {
                throw new WorkflowException("An error occurred while approving the change", e);
            }
        }
    }

    /**
     * Sorts {@link GerritChange}s in order by their Gerrit change number. For the purpose of this
     * approval function, changes are considered equal if their projects <b>do not</b> match
     * (because then order doesn't matter). However, for the same project, earlier change numbers
     * <i>generally</i> should be acted on first.
     * 
     * TODO: To be completely accurate, the changes should impose a dependency-tree ordering (via
     * <tt>--dependencies</tt> option) to GerritQuery! It is possible for an earlier ChangeId to be
     * refactored such that it is then dependent on a <i>later</i> change!
     * 
     * <p>
     * Note: this comparator imposes orderings that are inconsistent with <tt>equals()</tt>, because
     * in this case, the {@link GerritChange#getProject()} results do not matter.
     * </p>
     * 
     * @author jhansche
     */
    public static class SortByChangeId implements Comparator<GerritChange>
    {
        public static final SortByChangeId INSTANCE = new SortByChangeId();

        /** This is a singleton */
        private SortByChangeId() {
        }

        @Override
        public int compare(GerritChange a, GerritChange b) {
            if (a != b && a != null && b != null) {
                String aProject = a.getProject();
                String bProject = b.getProject();

                if (aProject != null && bProject != null && aProject.equals(bProject)) {
                    @SuppressWarnings("deprecation")
                    int aNum = Integer.parseInt(a.getNumber());
                    @SuppressWarnings("deprecation")
                    int bNum = Integer.parseInt(b.getNumber());

                    if (aNum == bNum) {
                        return 0;
                    } else {
                        return aNum < bNum ? -1 : 1;
                    }
                }
            }
            return 0;
        }
    }
}
