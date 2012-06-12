package com.meetme.plugins.jira.gerrit.data;

import java.io.File;
import java.net.URI;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * {@link GerritConfiguration} implementation that uses {@link PluginSettings} to store
 * configuration data.
 * 
 * @author jhansche
 * 
 */
public class GerritConfigurationImpl implements GerritConfiguration {
    private static final String PLUGIN_STORAGE_KEY = "com.meetme.plugins.jira.gerrit.data";
    private final PluginSettings settings;

    public GerritConfigurationImpl(PluginSettingsFactory pluginSettingsFactory) {
        this.settings = pluginSettingsFactory.createSettingsForKey(PLUGIN_STORAGE_KEY);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.meetme.plugins.jira.gerrit.data.GerritConfiguration#getSshHostname()
     */
    @Override
    public String getSshHostname() {
        return (String) settings.get(FIELD_SSH_HOSTNAME);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.meetme.plugins.jira.gerrit.data.GerritConfiguration#setSshHostname(java.lang.String)
     */
    @Override
    public void setSshHostname(String hostname) {
        settings.put(FIELD_SSH_HOSTNAME, hostname);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.meetme.plugins.jira.gerrit.data.GerritConfiguration#getSshPort()
     */
    @Override
    public int getSshPort() {
        String port = (String) settings.get(FIELD_SSH_PORT);
        return port == null ? DEFAULT_SSH_PORT : Integer.parseInt(port);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.meetme.plugins.jira.gerrit.data.GerritConfiguration#setSshPort(int)
     */
    @Override
    public void setSshPort(int port) {
        settings.put(FIELD_SSH_PORT, Integer.toString(port));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.meetme.plugins.jira.gerrit.data.GerritConfiguration#getSshUsername()
     */
    @Override
    public String getSshUsername() {
        return (String) settings.get(FIELD_SSH_USERNAME);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.meetme.plugins.jira.gerrit.data.GerritConfiguration#setSshUsername(java.lang.String)
     */
    @Override
    public void setSshUsername(String username) {
        settings.put(FIELD_SSH_USERNAME, username);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.meetme.plugins.jira.gerrit.data.GerritConfiguration#getHttpUsername()
     */
    @Override
    public String getHttpUsername() {
        return (String) settings.get(FIELD_HTTP_USERNAME);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.meetme.plugins.jira.gerrit.data.GerritConfiguration#setHttpUsername(java.lang.String)
     */
    @Override
    public void setHttpUsername(String httpUsername) {
        settings.put(FIELD_HTTP_USERNAME, httpUsername);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.meetme.plugins.jira.gerrit.data.GerritConfiguration#getHttpPassword()
     */
    @Override
    public String getHttpPassword() {
        return (String) settings.get(FIELD_HTTP_PASSWORD);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.meetme.plugins.jira.gerrit.data.GerritConfiguration#setHttpPassword(java.lang.String)
     */
    @Override
    public void setHttpPassword(String httpPassword) {
        settings.put(FIELD_HTTP_PASSWORD, httpPassword);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.meetme.plugins.jira.gerrit.data.GerritConfiguration#getHttpBaseUrl()
     */
    @Override
    public URI getHttpBaseUrl() {
        String uri = (String) settings.get(FIELD_HTTP_BASE_URL);
        return uri == null ? null : URI.create(uri);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.meetme.plugins.jira.gerrit.data.GerritConfiguration#setHttpBaseUrl(java.lang.String)
     */
    @Override
    public void setHttpBaseUrl(String httpBaseUrl) {
        settings.put(FIELD_HTTP_BASE_URL, httpBaseUrl == null ? null : URI.create(httpBaseUrl).toASCIIString());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.meetme.plugins.jira.gerrit.data.GerritConfiguration#getSshPrivateKey()
     */
    @Override
    public File getSshPrivateKey() {
        String path = (String) settings.get(FIELD_SSH_PRIVATE_KEY);
        return path == null ? null : new File(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.meetme.plugins.jira.gerrit.data.GerritConfiguration#setSshPrivateKey(java.io.File)
     */
    @Override
    public void setSshPrivateKey(File sshPrivateKey) {
        settings.put(FIELD_SSH_PRIVATE_KEY, sshPrivateKey == null ? null : sshPrivateKey.getPath());
    }

    @Override
    public String toString() {
        return String.format("GerritConfigurationImpl[ssh://{0}@{1}:{2}/, {3}; http://{4}:{5}@{6}/]",
                getSshUsername(), getSshHostname(), getSshPort(), getSshPrivateKey(), getHttpUsername(), getHttpPassword(), getHttpBaseUrl());
    }
}
