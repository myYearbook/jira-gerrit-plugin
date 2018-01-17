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

import java.io.File;
import java.net.URI;
import java.util.List;

public interface GerritConfiguration {
    int DEFAULT_SSH_PORT = 29418;
    String DEFAULT_QUERY_ISSUE = "tr:%s";
    String DEFAULT_QUERY_PROJECT = "message:%s-*";

    String FIELD_SSH_HOSTNAME = "sshHostname";
    String FIELD_SSH_USERNAME = "sshUsername";
    String FIELD_SSH_PORT = "sshPort";
    String FIELD_SSH_PRIVATE_KEY = "sshPrivateKey";

    String FIELD_QUERY_ISSUE = "issueSearchQuery";
    String FIELD_QUERY_PROJECT = "projectSearchQuery";

    String FIELD_HTTP_BASE_URL = "httpBaseUrl";
    String FIELD_HTTP_USERNAME = "httpUsername";
    String FIELD_HTTP_PASSWORD = "httpPassword";

    String FIELD_SHOW_EMPTY_PANEL = "showEmptyPanel";
    String FIELD_ALL_PROJECTS = "allProjects";
    String FIELD_KNOWN_GERRIT_PROJECTS = "knownGerritProjects";
    String FIELD_USE_GERRIT_PROJECT_WHITELIST = "useGerritProjectWhitelist";

    URI getHttpBaseUrl();

    String getHttpPassword();

    String getHttpUsername();

    String getIssueSearchQuery();

    String getProjectSearchQuery();

    String getSshHostname();

    int getSshPort();

    File getSshPrivateKey();

    String getSshUsername();

    boolean getShowsEmptyPanel();

    void setHttpBaseUrl(String httpBaseUrl);

    void setHttpPassword(String httpPassword);

    void setHttpUsername(String httpUsername);

    void setIssueSearchQuery(String query);

    void setProjectSearchQuery(String query);

    void setSshHostname(String hostname);

    void setSshPort(int port);

    void setSshPrivateKey(File sshPrivateKey);

    void setSshUsername(String username);

    void setShowEmptyPanel(boolean show);

    boolean isSshValid();

    List<String> getIdsOfKnownGerritProjects();
    void setIdsOfKnownGerritProjects(List<String> idsOfSelectedGerritProjects);

    boolean getUseGerritProjectWhitelist();
    void setUseGerritProjectWhitelist(boolean useGerritProjectWhitelist);

    class NotConfiguredException extends RuntimeException {
        public NotConfiguredException() {
        }

        public NotConfiguredException(String message) {
            super(message);
        }

        public NotConfiguredException(String message, Throwable cause) {
            super(message, cause);
        }

        public NotConfiguredException(Throwable cause) {
            super(cause);
        }

        public NotConfiguredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }
}
