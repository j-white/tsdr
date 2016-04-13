/*
 * Copyright (c) 2016 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.newts;

import org.opennms.newts.api.Context;

/**
 * Configuration class for Newts
 */
public class NewtsConfig {

    private String host;
    private Integer port;
    private String keyspace;
    private String user;
    private String password;
    private String compression;
    private Integer cacheSize;
    private Integer ttl;
    private String resourcePrefix;
    private String resourceSuffix;
    private Context context;

    public String getHost() {
        return host != null ? host : env(NewtsConstants.NEWTS_HOST_KEY, NewtsConstants.NEWTS_HOST_VALUE);
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port != null ? port : Integer.valueOf(env(NewtsConstants.NEWTS_PORT_KEY, NewtsConstants.NEWTS_PORT_VALUE));
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getKeyspace() {
        return keyspace != null ? keyspace : env(NewtsConstants.NEWTS_KEYSPACE_KEY, NewtsConstants.NEWTS_KEYSPACE_VALUE);
    }

    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }

    public String getUser() {
        return user != null ? user : env(NewtsConstants.NEWTS_USER_KEY, NewtsConstants.NEWTS_USER_VALUE);
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password != null ? password : env(NewtsConstants.NEWTS_PASSWORD_KEY, NewtsConstants.NEWTS_PASSWORD_VALUE);
    }

    public boolean getSsl() {
        // TODO
        return false;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCompression() {
        return compression != null ? compression : env(NewtsConstants.NEWTS_COMPRESSION_KEY, NewtsConstants.NEWTS_COMPRESSION_VALUE);
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public int getCacheSize() {
        return cacheSize != null ? cacheSize : Integer.valueOf(env(NewtsConstants.NEWTS_CACHE_SIZE_KEY, NewtsConstants.NEWTS_CACHE_SIZE_VALUE));
    }

    public void setCacheSize(Integer cacheSize) {
        this.cacheSize = cacheSize;
    }

    public int getTTL() {
        return ttl != null ? ttl : Integer.valueOf(env(NewtsConstants.NEWTS_TTL_KEY, NewtsConstants.NEWTS_TTL_VALUE));
    }

    public void setTTL(Integer ttl) {
        this.ttl = ttl;
    }

    public String getResourcePrefix() {
        return resourcePrefix != null ? resourcePrefix : env(NewtsConstants.NEWTS_RESOURCE_PREFIX_KEY, NewtsConstants.NEWTS_RESOURCE_PREFIX_VALUE);
    }

    public void setResourcePrefix(String resourcePrefix) {
        this.resourcePrefix = resourcePrefix;
    }

    public String getResourceSuffix() {
        return resourceSuffix != null ? resourceSuffix : env(NewtsConstants.NEWTS_RESOURCE_SUFFIX_KEY, NewtsConstants.NEWTS_RESOURCE_SUFFIX_VALUE);
    }

    public void setResourceSuffix(String resourceSuffix) {
        this.resourceSuffix = resourceSuffix;
    }

    public Context getContext() {
        return context != null ? context : new Context(env(NewtsConstants.NEWTS_CONTEXT_KEY, NewtsConstants.NEWTS_CONTEXT_VALUE));
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public boolean isHierarchicalIndexingEnabled() {
        return false;
    }

    public boolean isOpenNMSIndexingEnabled() {
        return true;
    }

    private static String env(String key, String defaultValue) {
        String rc = System.getenv(key);
        if (rc == null) {
            return defaultValue;
        }
        return rc;
    }
}
