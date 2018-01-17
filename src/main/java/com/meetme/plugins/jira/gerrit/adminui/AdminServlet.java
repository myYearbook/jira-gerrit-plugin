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
package com.meetme.plugins.jira.gerrit.adminui;

import com.atlassian.jira.config.util.JiraHome;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.meetme.plugins.jira.gerrit.data.GerritConfiguration;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryException;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.GerritQueryHandler;
import com.sonyericsson.hudson.plugins.gerrit.gerritevents.ssh.Authentication;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminServlet extends HttpServlet {
    private static final long serialVersionUID = -9175363090552720328L;
    private static final Logger log = LoggerFactory.getLogger(AdminServlet.class);

    private static final Object[] PACKAGE_PARTS = new String[] { "com", "meetme", "plugins", "jira", "gerrit" };
    private static final String CONTENT_TYPE = "text/html;charset=utf-8";

    private static final String FIELD_ACTION = "action";
    private static final String ACTION_SAVE = "save";
    private static final String ACTION_TEST = "test";

    private static String TEMPLATE_ADMIN = "templates/admin.vm";

    private final UserManager userManager;
    private final TemplateRenderer renderer;
    private final LoginUriProvider loginUriProvider;
    private final JiraHome jiraHome;
    private final ProjectManager projectManager;
    private final GerritConfiguration configurationManager;

    public AdminServlet(final UserManager userManager, final LoginUriProvider loginUriProvider, final TemplateRenderer renderer,
            final JiraHome jiraHome, final GerritConfiguration configurationManager, final ProjectManager projectManager) {
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
        this.renderer = renderer;
        this.jiraHome = jiraHome;
        this.configurationManager = configurationManager;
        this.projectManager = projectManager;
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        String username = userManager.getRemoteUsername(request);

        if (username == null || !userManager.isSystemAdmin(username)) {
            redirectToLogin(request, response);
            return;
        }

        response.setContentType(CONTENT_TYPE);
        renderer.render(TEMPLATE_ADMIN, configToMap(configurationManager), response.getWriter());
    }

    private Map<String, Object> configToMap(final GerritConfiguration config) {
        Map<String, Object> map = new HashMap<>();

        map.put(GerritConfiguration.FIELD_SSH_HOSTNAME, config.getSshHostname());
        map.put(GerritConfiguration.FIELD_SSH_PORT, config.getSshPort());
        map.put(GerritConfiguration.FIELD_SSH_USERNAME, config.getSshUsername());
        map.put(GerritConfiguration.FIELD_SSH_PRIVATE_KEY, config.getSshPrivateKey());

        map.put(GerritConfiguration.FIELD_QUERY_ISSUE, config.getIssueSearchQuery());
        map.put(GerritConfiguration.FIELD_QUERY_PROJECT, config.getProjectSearchQuery());

        if (config.getHttpBaseUrl() != null) {
            map.put(GerritConfiguration.FIELD_HTTP_BASE_URL, config.getHttpBaseUrl().toASCIIString());
            map.put(GerritConfiguration.FIELD_HTTP_USERNAME, config.getHttpUsername());
            map.put(GerritConfiguration.FIELD_HTTP_PASSWORD, config.getHttpPassword());
        }

        map.put(GerritConfiguration.FIELD_SHOW_EMPTY_PANEL, String.valueOf(config.getShowsEmptyPanel()));
        map.put(GerritConfiguration.FIELD_ALL_PROJECTS, projectManager.getProjects());

        List<Project> projectsUsingGerrit = configurationManager.getIdsOfKnownGerritProjects().stream()
                .map(Long::parseLong).map(projectManager::getProjectObj).collect(Collectors.toList());

        map.put(GerritConfiguration.FIELD_KNOWN_GERRIT_PROJECTS, projectsUsingGerrit);
        map.put(GerritConfiguration.FIELD_USE_GERRIT_PROJECT_WHITELIST, String.valueOf(config
                .getUseGerritProjectWhitelist()));

        return map;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> map;

        try {
            map = handlePost(req, resp);
        } catch (Exception e) {
            e.printStackTrace(resp.getWriter());
            return;
        }

        resp.setContentType(CONTENT_TYPE);
        renderer.render(TEMPLATE_ADMIN, map, resp.getWriter());
    }

    private Map<String, Object> handlePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = userManager.getRemoteUsername(req);

        if (username == null || !userManager.isSystemAdmin(username)) {
            redirectToLogin(req, resp);
            return null;
        }

        File privateKeyPath;
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> items;

        try {
            // Unfortunately "multipart" makes it so every field comes through as a "FileItem"
            items = upload.parseRequest(req);
        } catch (FileUploadException e) {
            e.printStackTrace(resp.getWriter());
            return null;
        }

        setAllFields(items);
        privateKeyPath = this.doUploadPrivateKey(items, configurationManager.getSshHostname());

        if (privateKeyPath != null) {
            // We'll store the *path* to the file in ConfigResource, to make it easy to look it
            // up in the future.
            if (log.isDebugEnabled())
                log.debug("---- Saved ssh private key at: " + privateKeyPath.toString() + " ----");
            configurationManager.setSshPrivateKey(privateKeyPath);
        } else if (configurationManager.getSshPrivateKey() != null) {
            if (log.isDebugEnabled())
                log.debug("---- Private key is already on file, and not being replaced. ----");
        } else {
            // TODO: is this a failure?
            log.info("**** No private key was uploaded, and no key currently on file!  Requests will fail. ****");
        }

        Map<String, Object> map = configToMap(configurationManager);
        String action = getAction(items);

        if (ACTION_TEST.equals(action)) {
            performConnectionTest(configurationManager, map);
        }

        return map;
    }

    private void performConnectionTest(GerritConfiguration configuration, Map<String, Object> map) {
        map.put("testResult", Boolean.FALSE);

        if (!configuration.isSshValid()) {
            map.put("testError", "not configured");
            return;
        }

        Authentication auth = new Authentication(configuration.getSshPrivateKey(), configuration.getSshUsername());
        GerritQueryHandler query = new GerritQueryHandler(configuration.getSshHostname(), configuration.getSshPort(), null, auth);

        try {
            query.queryJava("limit:1", false, false, false);
            map.put("testResult", Boolean.TRUE);
        } catch (IOException e) {
            e.printStackTrace();
            map.put("testError", e.getMessage());
        } catch (GerritQueryException e) {
            e.printStackTrace();
            map.put("testError", e.getMessage());
        }
    }

    private String getAction(List<FileItem> items) {
        for (FileItem item : items) {
            final String fieldName = item.getFieldName();
            if (FIELD_ACTION.equals(fieldName)) return item.getString();
        }

        return null;
    }

    private void setAllFields(final List<FileItem> items) {
        Set<String> allFields = Sets.newHashSet();
        List<String> idsOfSelectedGerritProjects = Lists.newArrayList();

        for (FileItem item : items) {
            final String fieldName = item.getFieldName();
            allFields.add(fieldName);

            if (GerritConfiguration.FIELD_HTTP_BASE_URL.equals(fieldName)) {
                configurationManager.setHttpBaseUrl(item.getString());
            } else if (GerritConfiguration.FIELD_HTTP_USERNAME.equals(fieldName)) {
                configurationManager.setHttpUsername(item.getString());
            } else if (GerritConfiguration.FIELD_HTTP_PASSWORD.equals(fieldName)) {
                configurationManager.setHttpPassword(item.getString());
            } else if (GerritConfiguration.FIELD_SSH_HOSTNAME.equals(fieldName)) {
                configurationManager.setSshHostname(item.getString());
            } else if (GerritConfiguration.FIELD_SSH_USERNAME.equals(fieldName)) {
                configurationManager.setSshUsername(item.getString());
            } else if (GerritConfiguration.FIELD_SSH_PORT.equals(fieldName)) {
                configurationManager.setSshPort(Integer.parseInt(item.getString()));
            } else if (GerritConfiguration.FIELD_QUERY_ISSUE.equals(fieldName)) {
                configurationManager.setIssueSearchQuery(item.getString());
            } else if (GerritConfiguration.FIELD_QUERY_PROJECT.equals(fieldName)) {
                configurationManager.setProjectSearchQuery(item.getString());
            } else if (GerritConfiguration.FIELD_KNOWN_GERRIT_PROJECTS.equals(fieldName)) {
                idsOfSelectedGerritProjects.add(item.getString());
            }
        }

        boolean showsEmptyPanelChecked = allFields.contains(GerritConfiguration.FIELD_SHOW_EMPTY_PANEL);
        configurationManager.setShowEmptyPanel(showsEmptyPanelChecked);

        boolean useGerritProjectWhitelist = allFields.contains(GerritConfiguration.FIELD_USE_GERRIT_PROJECT_WHITELIST);
        configurationManager.setUseGerritProjectWhitelist(useGerritProjectWhitelist);

        configurationManager.setIdsOfKnownGerritProjects(idsOfSelectedGerritProjects);
    }

    private File doUploadPrivateKey(final List<FileItem> items, final String sshHostname) throws IOException {
        File privateKeyPath = null;

        for (FileItem item : items) {
            if (item.getFieldName().equals(GerritConfiguration.FIELD_SSH_PRIVATE_KEY) && item.getSize() > 0) {
                File dataDir = new File(jiraHome.getDataDirectory(), StringUtils.join(PACKAGE_PARTS, File.separatorChar));

                if (!dataDir.exists()) {
                    dataDir.mkdirs();
                }

                String tempFilePrefix = configurationManager.getSshHostname();
                String tempFileSuffix = ".key";

                try {
                    privateKeyPath = File.createTempFile(tempFilePrefix, tempFileSuffix, dataDir);
                } catch (IOException e) {
                    log.info("---- Cannot create temporary file: " + e.getMessage() + ": " + dataDir
                            .toString() + tempFilePrefix + tempFileSuffix + " ----");
                    break;
                }

                privateKeyPath.setReadable(false, false);
                privateKeyPath.setReadable(true, true);

                InputStream is = item.getInputStream();
                FileOutputStream fos = new FileOutputStream(privateKeyPath);
                IOUtils.copy(is, fos);
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(fos);

                item.delete();
                break;
            }
        }

        return privateKeyPath;
    }

    private URI getUri(final HttpServletRequest request) {
        StringBuffer builder = request.getRequestURL();

        if (request.getQueryString() != null) {
            builder.append("?");
            builder.append(request.getQueryString());
        }

        return URI.create(builder.toString());
    }

    private void redirectToLogin(final HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
    }
}
