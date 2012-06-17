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
package com.meetme.plugins.jira.gerrit.data;

import java.io.BufferedReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.Authentication;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.SshConnection;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.SshConnectionFactory;

public class GerritCommand {
    private static final Logger logger = LoggerFactory.getLogger(GerritCommand.class);
    private final static String BASE_COMMAND = "gerrit review";
    private GerritConfiguration config;

    public GerritCommand(GerritConfiguration config) {
        this.config = config;
    }

    public void doReview(GerritChange change, String args) throws IOException {
        final String command = getCommand(change, args);
        runCommand(command);
    }

    @SuppressWarnings("deprecation")
    private String getCommand(GerritChange change, String args) {
        StringBuilder sb = new StringBuilder(BASE_COMMAND);
        sb.append(' ');
        sb.append(change.getNumber()).append(',').append(change.getPatchSet().getNumber());

        // TODO: escape args? Or build manually with String reviewType,int reviewScore,etc..?
        sb.append(' ').append(args);
        return sb.toString();
    }

    private void runCommand(String command) throws IOException {
        SshConnection ssh = null;
        logger.info("Running command: " + command);

        try {
            Authentication auth = new Authentication(config.getSshPrivateKey(), config.getSshUsername());

            // TODO: need to get stderr and exit status. Requires subclassing SshConnectionImpl to
            // provide more than one Reader
            ssh = SshConnectionFactory.getConnection(config.getSshHostname(), config.getSshPort(), auth);
            BufferedReader reader = new BufferedReader(ssh.executeCommandReader(command));
            String incomingLine = null;

            while ((incomingLine = reader.readLine()) != null) {
                // We don't expect any response anyway..
                // But we can get the response and return it if we need to
                logger.info("Incoming line: " + incomingLine);
            }

            logger.info("Closing reader.");
            reader.close();
        } finally {
            if (ssh != null) {
                ssh.disconnect();
            }
        }
    }

}
