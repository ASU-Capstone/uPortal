/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.portal.services;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

public class HttpClientManagerTest extends TestCase {
    private int port;
    private ServerSocket serverSocket;
    private Thread serverThread;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        serverSocket = new ServerSocket();
        serverThread = new Thread() {
            public void run() {
                try {
                    serverSocket.bind(null);
                    serverSocket.accept();
                    
                    synchronized (serverSocket) {
                        serverSocket.notifyAll();
                    }
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                }
            }
        };
        serverThread.setDaemon(true);
        serverThread.start();
        
        synchronized (serverSocket) {
            serverSocket.wait(100);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        serverSocket.close();
    }

    public void testHttpClientReadTimeout() throws Exception {
        HttpClient client = HttpClientManager.getNewHTTPClient();
        final InetAddress localAddress = serverSocket.getInetAddress();
        final String hostAddress = localAddress.getHostAddress();
        final int localPort = serverSocket.getLocalPort();
        final String testUrl = "http://" + hostAddress + ":" + localPort;
        final GetMethod get = new GetMethod(testUrl);

        try {
            client.executeMethod(get);
        }
        catch (SocketTimeoutException ste) {
            //expected
        }
        catch (SocketException se) {
            //expected
        }
    }
}
