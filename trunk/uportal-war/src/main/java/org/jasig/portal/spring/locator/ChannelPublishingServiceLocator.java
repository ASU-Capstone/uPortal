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

package org.jasig.portal.spring.locator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.portal.channel.IPortletPublishingService;
import org.jasig.portal.spring.PortalApplicationContextLocator;
import org.springframework.context.ApplicationContext;

public class ChannelPublishingServiceLocator extends AbstractBeanLocator<IPortletPublishingService> {
    public static final String BEAN_NAME = "portletPublishingService";
    
    private static final Log LOG = LogFactory.getLog(ChannelPublishingServiceLocator.class);
    private static AbstractBeanLocator<IPortletPublishingService> locatorInstance;

    public static IPortletPublishingService getIChannelPublishingService() {
        AbstractBeanLocator<IPortletPublishingService> locator = locatorInstance;
        if (locator == null) {
            LOG.info("Looking up bean '" + BEAN_NAME + "' in ApplicationContext due to context not yet being initialized");
            final ApplicationContext applicationContext = PortalApplicationContextLocator.getApplicationContext();
            applicationContext.getBean(ChannelPublishingServiceLocator.class.getName());
            
            locator = locatorInstance;
            if (locator == null) {
                LOG.warn("Instance of '" + BEAN_NAME + "' still null after portal application context has been initialized");
                return (IPortletPublishingService)applicationContext.getBean(BEAN_NAME, IPortletPublishingService.class);
            }
        }
        
        return locator.getInstance();
    }

    public ChannelPublishingServiceLocator(IPortletPublishingService instance) {
        super(instance, IPortletPublishingService.class);
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.spring.locator.AbstractBeanLocator#getLocator()
     */
    @Override
    protected AbstractBeanLocator<IPortletPublishingService> getLocator() {
        return locatorInstance;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.spring.locator.AbstractBeanLocator#setLocator(org.jasig.portal.spring.locator.AbstractBeanLocator)
     */
    @Override
    protected void setLocator(AbstractBeanLocator<IPortletPublishingService> locator) {
        locatorInstance = locator;
    }
}