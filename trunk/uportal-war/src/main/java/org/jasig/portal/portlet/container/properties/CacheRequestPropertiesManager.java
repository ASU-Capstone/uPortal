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

import java.util.Collections;
import java.util.Map;

import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.pluto.container.om.portlet.PortletDefinition;
import org.jasig.portal.PortalException;
import org.jasig.portal.portlet.om.IPortletDefinition;
import org.jasig.portal.portlet.om.IPortletEntity;
import org.jasig.portal.portlet.om.IPortletWindow;
import org.jasig.portal.portlet.registry.IPortletDefinitionRegistry;
import org.jasig.portal.portlet.registry.IPortletWindowRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;

/**
 * Deals with seting and retrieving the {@link RenderResponse#EXPIRATION_CACHE} property.
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
@Service("cacheRequestPropertiesManager")
public class CacheRequestPropertiesManager extends BaseRequestPropertiesManager {

    private IPortletDefinitionRegistry portletDefinitionRegistry;
    private IPortletWindowRegistry portletWindowRegistry;

    @Autowired
    public void setPortletDefinitionRegistry(IPortletDefinitionRegistry portletDefinitionRegistry) {
        this.portletDefinitionRegistry = portletDefinitionRegistry;
    }

    @Autowired
    public void setPortletWindowRegistry(IPortletWindowRegistry portletWindowRegistry) {
        this.portletWindowRegistry = portletWindowRegistry;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.portlet.container.properties.BaseRequestPropertiesManager#getRequestProperties(javax.servlet.http.HttpServletRequest, org.jasig.portal.portlet.om.IPortletWindow)
     */
    @Override
    public Map<String, String[]> getRequestProperties(HttpServletRequest portletRequest, IPortletWindow portletWindow) {
        Integer expirationCache = portletWindow.getExpirationCache();
        
        if (expirationCache == null) {
            final PortletDefinition portletDeployment = this.getPortletDeployment(portletRequest, portletWindow);
            final int descriptorExpirationCache = portletDeployment.getExpirationCache();
            
            // only set if greater than 0
            // in Portlet 2.0, -1 means cache does not expire (not defined as a constant) (see PLT 22.1)
            // Pluto 1.1.7 had a constant named EXPIRATION_CACHE_UNSET, which was Integer.MIN_VALUE
            if (descriptorExpirationCache >= 0) {
                expirationCache = descriptorExpirationCache;
            }
        }

        if (expirationCache != null) {
            return Collections.singletonMap(RenderResponse.EXPIRATION_CACHE, new String[] { expirationCache.toString() });
        }

        return Collections.emptyMap();
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.portlet.container.properties.BaseRequestPropertiesManager#setResponseProperty(javax.servlet.http.HttpServletRequest, org.jasig.portal.portlet.om.IPortletWindow, java.lang.String, java.lang.String)
     */
    @Override
    public void setResponseProperty(HttpServletRequest portletRequest, IPortletWindow portletWindow, String property, String value) {
        if (RenderResponse.EXPIRATION_CACHE.equals(property)) {
            final PortletDefinition portletDeployment = this.getPortletDeployment(portletRequest, portletWindow);
            final int descriptorExpirationCache = portletDeployment.getExpirationCache();
            
            // only set if greater than 0
            // in Portlet 2.0, -1 means cache does not expire (not defined as a constant) (see PLT 22.1)
            // Pluto 1.1.7 had a constant named EXPIRATION_CACHE_UNSET, which was Integer.MIN_VALUE
            if (descriptorExpirationCache >= 0) {
                Integer cacheExpiration = portletWindow.getExpirationCache();
                try {
                    cacheExpiration = Integer.valueOf(value);
                }
                catch (NumberFormatException nfe) {
                    this.logger.info("Portlet '" + portletWindow + "' tried to set a cache expiration time of '" + value + "' which could not be parsed into an Integer. The previous value of '" + cacheExpiration + "' will be used.");
                }
            
                portletWindow.setExpirationCache(cacheExpiration);
                
                this.portletWindowRegistry.storePortletWindow(portletRequest, portletWindow);
            }
        }
    }
    
    /**
     * Returns {@link Ordered#LOWEST_PRECEDENCE}.
     *  
     * @see org.springframework.core.Ordered#getOrder()
     */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}
	/**
     * Gets the Portlet Deployment for a IPortletWindow object
     * 
     * @param httpServletRequest The portal's request (not the portlet context request)
     * @param portletWindow The window to get the parent PortletDD for.
     * @return The parent portlet descriptor for the window
     * @throws PortalException if the PortletDD fails to load.
     */
    protected PortletDefinition getPortletDeployment(HttpServletRequest httpServletRequest, IPortletWindow portletWindow) {
        final IPortletEntity portletEntity = portletWindow.getPortletEntity();
        final IPortletDefinition portletDefinition = portletEntity.getPortletDefinition();
        
        try {
            return this.portletDefinitionRegistry.getParentPortletDescriptor(portletDefinition.getPortletDefinitionId());
        }
        catch (DataRetrievalFailureException e) {
            throw new PortalException("Failed to retrieve the PortletDD for portlet window: " + portletWindow, e);
        }
    }
}
