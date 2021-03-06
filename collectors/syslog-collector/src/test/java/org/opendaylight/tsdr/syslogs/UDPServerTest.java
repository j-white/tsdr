/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.tsdr.syslogs.server.SyslogUDPServer;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;

import java.util.LinkedList;
import java.util.List;

/**
 * This is the test for UDP Server.
 *
 * @author Kun Chen(kunch@tethrnet.com)
 */
public class UDPServerTest {
    private List<Message> messageList = new  LinkedList<>();
    private SyslogUDPServer server = new SyslogUDPServer(messageList);


    @Before
    public void setUp() throws InterruptedException {
        try {
            server.setPort(8989);
            server.startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("UDP Server starts at port 8989.");
    }

    @Test
    public void testMessageHandle() throws InterruptedException {
        Assert.assertTrue(server.isRunning());
        Assert.assertEquals("UDP",server.getProtocol());

        try {
            SyslogGenerator generator = new SyslogGenerator("localhost",8989);
            generator.sendSyslog("This is a test message.",4);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Thread.sleep(10000);
        Assert.assertEquals(4,messageList.size());
    }

    @After
    public void tearDown() throws InterruptedException {
        server.stopServer();
    }
}
