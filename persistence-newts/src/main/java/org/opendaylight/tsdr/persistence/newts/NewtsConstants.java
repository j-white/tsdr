/*
 * Copyright (c) 2016 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.newts;

/**
 * Constants of Newts
 */
public class NewtsConstants {

    private NewtsConstants() {
        throw new AssertionError("Instantiating utility class NewtsConstants.");
    }

    public static final String NEWTS_HOST_KEY = "NEWTS_HOST";
    public static final String NEWTS_HOST_VALUE = "localhost";

    public static final String NEWTS_PORT_KEY = "NEWTS_PORT";
    public static final String NEWTS_PORT_VALUE = "9042";

    public static final String NEWTS_KEYSPACE_KEY = "NEWTS_KEYSPACE";
    public static final String NEWTS_KEYSPACE_VALUE = "newts";

    public static final String NEWTS_USER_KEY = "NEWTS_USER";
    public static final String NEWTS_USER_VALUE = "admin";

    public static final String NEWTS_PASSWORD_KEY = "NEWTS_PASSWORD";
    public static final String NEWTS_PASSWORD_VALUE = "admin";

    public static final String NEWTS_COMPRESSION_KEY = "NEWTS_COMPRESSION";
    public static final String NEWTS_COMPRESSION_VALUE = "NONE";

    public static final String NEWTS_CACHE_SIZE_KEY = "NEWTS_CACHE_SIZE";
    public static final String NEWTS_CACHE_SIZE_VALUE = "8096";

    public static final String NEWTS_TTL_KEY = "NEWTS_TTL";
    public static final String NEWTS_TTL_VALUE = "" + 365*7*24*60*60;

    public static final String NEWTS_RESOURCE_PREFIX_KEY = "NEWTS_RESOURCE_PREFIX";
    public static final String NEWTS_RESOURCE_PREFIX_VALUE = "";
    
    public static final String NEWTS_RESOURCE_SUFFIX_KEY = "NEWTS_RESOURCE_SUFFIX";
    public static final String NEWTS_RESOURCE_SUFFIX_VALUE = "";

    public static final String NEWTS_CONTEXT_KEY = "NEWTS_CONTEXT";
    public static final String NEWTS_CONTEXT_VALUE = "G";
}
