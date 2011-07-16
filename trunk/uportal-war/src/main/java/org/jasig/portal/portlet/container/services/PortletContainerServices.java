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

package org.jasig.portal.portlet.container.services;

import javax.portlet.PortalContext;

import org.apache.pluto.container.CCPPProfileService;
import org.apache.pluto.container.ContainerServices;
import org.apache.pluto.container.EventCoordinationService;
import org.apache.pluto.container.FilterManagerService;
import org.apache.pluto.container.NamespaceMapper;
import org.apache.pluto.container.PortletEnvironmentService;
import org.apache.pluto.container.PortletInvokerService;
import org.apache.pluto.container.PortletPreferencesService;
import org.apache.pluto.container.PortletRequestContextService;
import org.apache.pluto.container.PortletURLListenerService;
import org.apache.pluto.container.RequestDispatcherService;
import org.apache.pluto.container.UserInfoService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * TODO do we need to depend on the pluto-portal-driver JAR? Especially for PortletInvokerService?
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
@Service("containerServices")
public class PortletContainerServices implements ContainerServices, InitializingBean {
    
	private CCPPProfileService cCPPProfileService;
	private EventCoordinationService eventCoordinationService;
	private FilterManagerService filterManagerService;
	private NamespaceMapper namespaceMapper;
	private PortletInvokerService portletInvokerService;
	private PortletRequestContextService portletRequestContextService;
	private PortletURLListenerService portletURLListenerService;
	
	private PortalContext portalContext;
    private PortletEnvironmentService portletEnvironmentService;
    private PortletPreferencesService portletPreferencesService;
    private RequestDispatcherService requestDispatcherService;
    private UserInfoService userInfoService;
    
    @Autowired
    public void setPortalContext(PortalContext portalContext) {
        this.portalContext = portalContext;
    }
    
    @Autowired
    public void setPortletEnvironmentService(PortletEnvironmentService portletEnvironmentService) {
        this.portletEnvironmentService = portletEnvironmentService;
    }
    
    @Autowired
    public void setPortletPreferencesService(PortletPreferencesService portletPreferencesService) {
        this.portletPreferencesService = portletPreferencesService;
    }

    @Autowired(required=false)
    public void setRequestDispatcherService(RequestDispatcherService requestDispatcherService) {
        this.requestDispatcherService = requestDispatcherService;
    }
    
    @Autowired
    public void setUserInfoService(@Qualifier("main") UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        /*
    	if (this.requestDispatcherService == null) {
            this.requestDispatcherService = new RequestDispatcherServiceImpl();
        }
        */
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.container.ContainerServices#getCCPPProfileService()
     */
    @Override
    public CCPPProfileService getCCPPProfileService() {
        return this.cCPPProfileService;
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.container.ContainerServices#getEventCoordinationService()
     */
    @Override
    public EventCoordinationService getEventCoordinationService() {
        return this.eventCoordinationService;
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.container.ContainerServices#getFilterManagerService()
     */
    @Override
    public FilterManagerService getFilterManagerService() {
       return this.filterManagerService;
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.container.ContainerServices#getNamespaceMapper()
     */
    @Override
    public NamespaceMapper getNamespaceMapper() {
        return this.namespaceMapper;
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.container.ContainerServices#getPortalContext()
     */
    @Override
    public PortalContext getPortalContext() {
        return this.portalContext;
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.container.ContainerServices#getPortletEnvironmentService()
     */
    @Override
    public PortletEnvironmentService getPortletEnvironmentService() {
        return this.portletEnvironmentService;
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.container.ContainerServices#getPortletInvokerService()
     */
    @Override
    public PortletInvokerService getPortletInvokerService() {
        return this.portletInvokerService;
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.container.ContainerServices#getPortletPreferencesService()
     */
    @Override
    public PortletPreferencesService getPortletPreferencesService() {
        return this.portletPreferencesService;
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.container.ContainerServices#getPortletRequestContextService()
     */
    @Override
    public PortletRequestContextService getPortletRequestContextService() {
       return this.portletRequestContextService;
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.container.ContainerServices#getPortletURLListenerService()
     */
    @Override
    public PortletURLListenerService getPortletURLListenerService() {
        return this.portletURLListenerService;
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.container.ContainerServices#getRequestDispatcherService()
     */
    @Override
    public RequestDispatcherService getRequestDispatcherService() {
        return this.requestDispatcherService;
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.container.ContainerServices#getUserInfoService()
     */
    @Override
    public UserInfoService getUserInfoService() {
        return this.userInfoService;
    }

}
