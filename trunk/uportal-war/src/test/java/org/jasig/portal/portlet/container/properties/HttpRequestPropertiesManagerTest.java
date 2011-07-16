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

package org.jasig.portal.portlet.container.properties;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.jasig.portal.portlet.om.IPortletWindow;
import org.jasig.portal.url.IPortalRequestUtils;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public class HttpRequestPropertiesManagerTest extends TestCase {
    private HttpRequestPropertiesManager httpRequestPropertiesManager;
    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        this.httpRequestPropertiesManager = new HttpRequestPropertiesManager();
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        this.httpRequestPropertiesManager = null;
    }

    public void testGetRequestProperties() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("1.2.3.4");
        request.setMethod("POST");
        
        final IPortletWindow portletWindow = EasyMock.createMock(IPortletWindow.class);
        
        final IPortalRequestUtils portalRequestUtils = EasyMock.createMock(IPortalRequestUtils.class);
        EasyMock.expect(portalRequestUtils.getOriginalPortalRequest(request)).andReturn(request);
        
        EasyMock.replay(portletWindow, portalRequestUtils);
        
        this.httpRequestPropertiesManager.setPortalRequestUtils(portalRequestUtils);
        
        final Map<String, String[]> properties = this.httpRequestPropertiesManager.getRequestProperties(request, portletWindow);

        assertNotNull("properties Map should not be null", properties);
        assertEquals("properties Map should have 2 values", 2, properties.size());
        assertEquals(Collections.singletonList("1.2.3.4"), Arrays.asList(properties.get("REMOTE_ADDR")));
        assertEquals(Collections.singletonList("POST"), Arrays.asList(properties.get("REQUEST_METHOD")));
        
        EasyMock.verify(portletWindow, portalRequestUtils);
    }
}
