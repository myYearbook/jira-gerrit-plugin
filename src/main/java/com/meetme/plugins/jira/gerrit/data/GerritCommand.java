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
        try {
            Authentication auth = new Authentication(config.getSshPrivateKey(), config.getSshUsername());
            ssh = SshConnectionFactory.getConnection(config.getSshHostname(), config.getSshPort(), auth);
            BufferedReader reader = new BufferedReader(ssh.executeCommandReader(command));
            String incomingLine = null;

            // We don't expect any response anyway..
            while ((incomingLine = reader.readLine()) != null) {
                logger.trace("Incoming line: {}", incomingLine);
            }

            logger.trace("Closing reader.");
            reader.close();
        } finally {
            if (ssh != null) {
                ssh.disconnect();
            }
        }
    }

}
