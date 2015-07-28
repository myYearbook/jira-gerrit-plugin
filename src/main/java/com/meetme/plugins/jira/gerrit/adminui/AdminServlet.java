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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.config.util.JiraHome;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.meetme.plugins.jira.gerrit.data.GerritConfiguration;

public class AdminServlet extends HttpServlet {
    private static final long serialVersionUID = -9175363090552720328L;
    private static final Logger log = LoggerFactory.getLogger(AdminServlet.class);

    private static final Object[] PACKAGE_PARTS = new String[] { "com", "meetme", "plugins", "jira", "gerrit" };
    private static final String CONTENT_TYPE = "text/html;charset=utf-8";

    private static String TEMPLATE_ADMIN = "templates/admin.vm";

    private final UserManager userManager;
    private final TemplateRenderer renderer;
    private final LoginUriProvider loginUriProvider;
    private final JiraHome jiraHome;
    private final GerritConfiguration configurationManager;

    public AdminServlet(final UserManager userManager, final LoginUriProvider loginUriProvider, final TemplateRenderer renderer,
            final JiraHome jiraHome, final GerritConfiguration configurationManager) {
        this.userManager = userManager;
        this.loginUriProvider = loginUriProvider;
        this.renderer = renderer;
        this.jiraHome = jiraHome;
        this.configurationManager = configurationManager;
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
        Map<String, Object> map = new HashMap<String, Object>();

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

        return map;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            String username = userManager.getRemoteUsername(req);

            if (username == null || !userManager.isSystemAdmin(username)) {
                redirectToLogin(req, resp);
                return;
            }

            File privateKeyPath = null;
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            List<FileItem> items;

            try {
                // Unfortunately "multipart" makes it so every field comes through as a "FileItem"

                items = (List<FileItem>) upload.parseRequest(req);
            } catch (FileUploadException e) {
                e.printStackTrace(resp.getWriter());
                return;
            }

            this.setAllFields(items);
            privateKeyPath = this.doUploadPrivateKey(items, configurationManager.getSshHostname());

            if (privateKeyPath != null) {
                // We'll store the *path* to the file in ConfigResource, to make it easy to look it
                // up in the future.
                log.debug("---- Saved ssh private key at: " + privateKeyPath.toString() + " ----");
                configurationManager.setSshPrivateKey(privateKeyPath);
            } else if (configurationManager.getSshPrivateKey() != null) {
                log.debug("---- Private key is already on file, and not being replaced. ----");
            } else {
                // TODO: is this a failure?
                log.info("**** No private key was uploaded, and no key currently on file!  Requests will fail. ****");
            }
        } catch (Exception e) {
            e.printStackTrace(resp.getWriter());
            return;
        }

        resp.setContentType(CONTENT_TYPE);
        renderer.render(TEMPLATE_ADMIN, configToMap(configurationManager), resp.getWriter());
    }

    private void setAllFields(final List<FileItem> items) {
        for (FileItem item : items) {
            final String fieldName = item.getFieldName();

            if (fieldName.equals(GerritConfiguration.FIELD_HTTP_BASE_URL)) {
                configurationManager.setHttpBaseUrl(item.getString());
            } else if (fieldName.equals(GerritConfiguration.FIELD_HTTP_USERNAME)) {
                configurationManager.setHttpUsername(item.getString());
            } else if (fieldName.equals(GerritConfiguration.FIELD_HTTP_PASSWORD)) {
                configurationManager.setHttpPassword(item.getString());
            } else if (fieldName.equals(GerritConfiguration.FIELD_SSH_HOSTNAME)) {
                configurationManager.setSshHostname(item.getString());
            } else if (fieldName.equals(GerritConfiguration.FIELD_SSH_USERNAME)) {
                configurationManager.setSshUsername(item.getString());
            } else if (fieldName.equals(GerritConfiguration.FIELD_SSH_PORT)) {
                configurationManager.setSshPort(Integer.parseInt(item.getString()));
            } else if (fieldName.equals(GerritConfiguration.FIELD_QUERY_ISSUE)) {
                configurationManager.setIssueSearchQuery(item.getString());
            } else if (fieldName.equals(GerritConfiguration.FIELD_QUERY_PROJECT)) {
                configurationManager.setProjectSearchQuery(item.getString());
            }
        }
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
                }
                catch (IOException e) {
                    log.info("---- Cannot create temporary file: " + e.getMessage() + ": " + dataDir.toString() + tempFilePrefix + tempFileSuffix + " ----");
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
