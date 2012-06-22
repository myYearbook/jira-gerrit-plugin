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
import java.io.InputStreamReader;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelExec;
import com.meetme.plugins.jira.gerrit.data.dto.GerritChange;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.Authentication;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.SshConnection;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.SshConnectionFactory;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.SshException;

public class GerritCommand {
    private static final Logger log = LoggerFactory.getLogger(GerritCommand.class);
    private final static String BASE_COMMAND = "gerrit review";
    private GerritConfiguration config;

    public GerritCommand(GerritConfiguration config) {
        this.config = config;
    }

    public boolean doReview(GerritChange change, String args) throws IOException {
        final String command = getCommand(change, args);
        return runCommand(command);
    }

    public boolean doReviews(List<GerritChange> changes, String args) throws IOException {
        String[] commands = new String[changes.size()];
        int i = 0;

        for (GerritChange change : changes) {
            commands[i++] = getCommand(change, args);
        }

        return runCommands(commands);
    }

    private boolean runCommand(String command) throws IOException {
        return runCommands(new String[] { command });
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

    private boolean runCommands(String[] commands) throws IOException {
        boolean success = true;
        SshConnection ssh = null;

        try {
            Authentication auth = new Authentication(config.getSshPrivateKey(), config.getSshUsername());
            ssh = SshConnectionFactory.getConnection(config.getSshHostname(), config.getSshPort(), auth);

            for (String command : commands) {
                if (!runCommand(ssh, command)) {
                    success = false;
                }
            }
        } finally {
            if (ssh != null) {
                ssh.disconnect();
            }
        }

        return success;
    }

    private boolean runCommand(SshConnection ssh, String command) throws SshException, IOException {
        boolean success = false;
        ChannelExec channel = null;

        log.debug("Running command: " + command);

        try {
            channel = ssh.executeCommandChannel(command);

            BufferedReader reader;
            String incomingLine = null;

            InputStreamReader err = new InputStreamReader(channel.getErrStream());
            InputStreamReader out = new InputStreamReader(channel.getInputStream());

            reader = new BufferedReader(out);

            while ((incomingLine = reader.readLine()) != null) {
                // We don't expect any response anyway..
                // But we can get the response and return it if we need to
                log.trace("Incoming line: " + incomingLine);
            }

            reader.close();
            reader = new BufferedReader(err);

            while ((incomingLine = reader.readLine()) != null) {
                // We don't expect any response anyway..
                // But we can get the response and return it if we need to
                log.warn("Error: " + incomingLine);
            }

            reader.close();

            int exitStatus = channel.getExitStatus();
            success = exitStatus != 0;
            log.info("Command exit status: " + exitStatus);
        } finally {
            channel.disconnect();
        }

        return success;
    }
}
