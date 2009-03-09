/**
 * Copyright (c) 2000-2009, Jasig, Inc.
 * See license distributed with this file and available online at
 * https://www.ja-sig.org/svn/jasig-parent/tags/rel-10/license-header.txt
 */
package org.jasig.portal.spring.locator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.portal.portlet.registry.IPortletDefinitionRegistry;
import org.jasig.portal.spring.PortalApplicationContextLocator;
import org.springframework.context.ApplicationContext;

/**
 * @author Eric Dalquist
 * @version $Revision$
 * @deprecated code that needs an IPortletDefinitionRegistry should use direct dependency injection where possible
 */
@Deprecated
public class PortletDefinitionRegistryLocator extends AbstractBeanLocator<IPortletDefinitionRegistry> {
    public static final String BEAN_NAME = "portletDefinitionRegistry";
    
    private static final Log LOG = LogFactory.getLog(PortletDefinitionRegistryLocator.class);
    private static AbstractBeanLocator<IPortletDefinitionRegistry> locatorInstance;

    public static IPortletDefinitionRegistry getPortletDefinitionRegistry() {
        AbstractBeanLocator<IPortletDefinitionRegistry> locator = locatorInstance;
        if (locator == null) {
            LOG.info("Looking up bean '" + BEAN_NAME + "' in ApplicationContext due to context not yet being initialized");
            final ApplicationContext applicationContext = PortalApplicationContextLocator.getApplicationContext();
            applicationContext.getBean(PortletDefinitionRegistryLocator.class.getName());
            
            locator = locatorInstance;
            if (locator == null) {
                LOG.warn("Instance of '" + BEAN_NAME + "' still null after portal application context has been initialized");
                return (IPortletDefinitionRegistry)applicationContext.getBean(BEAN_NAME, IPortletDefinitionRegistry.class);
            }
        }
        
        return locator.getInstance();
    }

    public PortletDefinitionRegistryLocator(IPortletDefinitionRegistry instance) {
        super(instance, IPortletDefinitionRegistry.class);
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.spring.locator.AbstractBeanLocator#getLocator()
     */
    @Override
    protected AbstractBeanLocator<IPortletDefinitionRegistry> getLocator() {
        return locatorInstance;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.spring.locator.AbstractBeanLocator#setLocator(org.jasig.portal.spring.locator.AbstractBeanLocator)
     */
    @Override
    protected void setLocator(AbstractBeanLocator<IPortletDefinitionRegistry> locator) {
        locatorInstance = locator;
    }
}
