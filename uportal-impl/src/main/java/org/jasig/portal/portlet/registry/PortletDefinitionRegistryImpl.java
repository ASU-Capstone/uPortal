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

package org.jasig.portal.portlet.registry;

import javax.servlet.ServletContext;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pluto.OptionalContainerServices;
import org.apache.pluto.PortletContainerException;
import org.apache.pluto.descriptors.portlet.PortletAppDD;
import org.apache.pluto.descriptors.portlet.PortletDD;
import org.apache.pluto.spi.optional.PortletRegistryService;
import org.jasig.portal.IChannelRegistryStore;
import org.jasig.portal.channel.IChannelDefinition;
import org.jasig.portal.channel.IChannelParameter;
import org.jasig.portal.channels.portlet.IPortletAdaptor;
import org.jasig.portal.portlet.dao.IPortletDefinitionDao;
import org.jasig.portal.portlet.om.IPortletDefinition;
import org.jasig.portal.portlet.om.IPortletDefinitionId;
import org.jasig.portal.utils.Tuple;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.web.context.ServletContextAware;

/**
 * Implementation of the definition registry, pulls together the related parts of the framework for creation and access
 * of {@link IPortletDefinition}s.
 * 
 * TODO this needs to listen for channel deletion events and remove the corresponding portlet definition, this would likley need a hook in ChannelRegistryManager.removeChannel
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public class PortletDefinitionRegistryImpl implements IPortletDefinitionRegistry, ServletContextAware {
    protected final Log logger = LogFactory.getLog(this.getClass());
    
    private IChannelRegistryStore channelRegistryStore;
    private IPortletDefinitionDao portletDefinitionDao;
    private OptionalContainerServices optionalContainerServices;
    private ServletContext servletContext;
    
    /**
     * @return the portletDefinitionDao
     */
    public IPortletDefinitionDao getPortletDefinitionDao() {
        return this.portletDefinitionDao;
    }
    /**
     * @param portletDefinitionDao the portletDefinitionDao to set
     */
    @Required
    public void setPortletDefinitionDao(IPortletDefinitionDao portletDefinitionDao) {
        Validate.notNull(portletDefinitionDao);
        this.portletDefinitionDao = portletDefinitionDao;
    }
    
    /**
     * @return the optionalContainerServices
     */
    public OptionalContainerServices getOptionalContainerServices() {
        return this.optionalContainerServices;
    }
    /**
     * @param optionalContainerServices the optionalContainerServices to set
     */
    @Required
    public void setOptionalContainerServices(OptionalContainerServices optionalContainerServices) {
        Validate.notNull(optionalContainerServices);
        this.optionalContainerServices = optionalContainerServices;
    }
    
    /**
     * @return the channelRegistryStore
     */
    public IChannelRegistryStore getChannelRegistryStore() {
        return channelRegistryStore;
    }
    /**
     * @param channelRegistryStore the channelRegistryStore to set
     */
    @Required
    public void setChannelRegistryStore(IChannelRegistryStore channelRegistryStore) {
        Validate.notNull(channelRegistryStore);
        this.channelRegistryStore = channelRegistryStore;
    }
    
    /* (non-Javadoc)
     * @see org.springframework.web.context.ServletContextAware#setServletContext(javax.servlet.ServletContext)
     */
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
    
    /* (non-Javadoc)
     * @see org.jasig.portal.portlet.registry.IPortletDefinitionRegistry#createPortletDefinition(int)
     */
    public IPortletDefinition createPortletDefinition(int channelPublishId) {
        return this.portletDefinitionDao.getPortletDefinition(channelPublishId);
    }
    
    /* (non-Javadoc)
     * @see org.jasig.portal.portlet.registry.IPortletDefinitionRegistry#getPortletDefinition(int)
     */
    public IPortletDefinition getPortletDefinition(int channelPublishId) {
        return this.portletDefinitionDao.getPortletDefinition(channelPublishId);
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.portlet.registry.IPortletDefinitionRegistry#getOrCreatePortletDefinition(int)
     */
    public IPortletDefinition getOrCreatePortletDefinition(int channelPublishId) {
        final IPortletDefinition portletDefinition = this.getPortletDefinition(channelPublishId);
        if (portletDefinition != null) {
            return portletDefinition;
        }
        
        return this.createPortletDefinition(channelPublishId);
    }
    
    /* (non-Javadoc)
     * @see org.jasig.portal.portlet.registry.IPortletDefinitionRegistry#getPortletDefinition(org.jasig.portal.portlet.om.IPortletDefinitionId)
     */
    public IPortletDefinition getPortletDefinition(IPortletDefinitionId portletDefinitionId) {
        Validate.notNull(portletDefinitionId, "portletDefinitionId can not be null");
        
        return this.portletDefinitionDao.getPortletDefinition(portletDefinitionId);
    }
    /* (non-Javadoc)
     * @see org.jasig.portal.portlet.registry.IPortletDefinitionRegistry#updatePortletDefinition(org.jasig.portal.portlet.om.IPortletDefinition)
     */
    public void updatePortletDefinition(IPortletDefinition portletDefinition) {
        Validate.notNull(portletDefinition, "portletDefinition can not be null");
        
        this.portletDefinitionDao.updatePortletDefinition(portletDefinition);
    }
    
    /* (non-Javadoc)
     * @see org.jasig.portal.portlet.registry.IPortletDefinitionRegistry#getParentPortletApplicationDescriptor(org.jasig.portal.portlet.om.IPortletDefinitionId)
     */
    public PortletAppDD getParentPortletApplicationDescriptor(IPortletDefinitionId portletDefinitionId) throws PortletContainerException {
        final IPortletDefinition portletDefinition = this.getPortletDefinition(portletDefinitionId);
        if (portletDefinition == null) {
            return null;
        }
        
        final Tuple<String, String> portletDescriptorKeys = this.getPortletDescriptorKeys(portletDefinition);
        
        final PortletRegistryService portletRegistryService = this.optionalContainerServices.getPortletRegistryService();
        return portletRegistryService.getPortletApplicationDescriptor(portletDescriptorKeys.first);
    }
    
    /* (non-Javadoc)
     * @see org.jasig.portal.portlet.registry.IPortletDefinitionRegistry#getParentPortletDescriptor(org.jasig.portal.portlet.om.IPortletDefinitionId)
     */
    public PortletDD getParentPortletDescriptor(IPortletDefinitionId portletDefinitionId) throws PortletContainerException {
        final IPortletDefinition portletDefinition = this.getPortletDefinition(portletDefinitionId);
        if (portletDefinition == null) {
            return null;
        }
        
        final Tuple<String, String> portletDescriptorKeys = this.getPortletDescriptorKeys(portletDefinition);
        
        final PortletRegistryService portletRegistryService = this.optionalContainerServices.getPortletRegistryService();
        return portletRegistryService.getPortletDescriptor(portletDescriptorKeys.first, portletDescriptorKeys.second);
    }
    
    /**
     * Get the portletApplicationId and portletName for the specified channel definition id. The portletApplicationId
     * will be {@link Tuple#first} and the portletName will be {@link Tuple#second}
     */
    public Tuple<String, String> getPortletDescriptorKeys(IPortletDefinition portletDefinition) {
        final IChannelDefinition channelDefinition = portletDefinition.getChannelDefinition();
        
        final String portletApplicationId;
        final IChannelParameter isFrameworkPortletParam = channelDefinition.getParameter(IPortletAdaptor.CHANNEL_PARAM__IS_FRAMEWORK_PORTLET);
        if (isFrameworkPortletParam != null && Boolean.valueOf(isFrameworkPortletParam.getValue())) {
            portletApplicationId = this.servletContext.getContextPath();
        }
        else {
            final IChannelParameter portletApplicaitonIdParam = channelDefinition.getParameter(IPortletAdaptor.CHANNEL_PARAM__PORTLET_APPLICATION_ID);
            if (portletApplicaitonIdParam == null) {
                throw new DataRetrievalFailureException("The specified ChannelDefinition does not provide the needed channel parameter '" + IPortletAdaptor.CHANNEL_PARAM__PORTLET_APPLICATION_ID + "'. ChannelDefinition=" + channelDefinition);
            }
            
            portletApplicationId = portletApplicaitonIdParam.getValue();
        }
        
        final IChannelParameter portletNameParam = channelDefinition.getParameter(IPortletAdaptor.CHANNEL_PARAM__PORTLET_NAME);
        if (portletNameParam == null) {
            throw new DataRetrievalFailureException("The specified ChannelDefinition does not provide the needed channel parameter '" + IPortletAdaptor.CHANNEL_PARAM__PORTLET_NAME + "'. ChannelDefinition=" + channelDefinition);
        }
        final String portletName = portletNameParam.getValue();
        
        return new Tuple<String, String>(portletApplicationId, portletName);
    }
}
