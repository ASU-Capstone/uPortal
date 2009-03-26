/**
 * Copyright 2007 The JA-SIG Collaborative.  All rights reserved.
 * See license distributed with this file and
 * available online at http://www.uportal.org/license.html
 */
package org.jasig.portal.portlet.container.services;

import org.apache.commons.lang.Validate;
import org.apache.pluto.core.DefaultOptionalContainerServices;
import org.apache.pluto.core.DefaultPortalAdministrationService;
import org.apache.pluto.spi.optional.PortalAdministrationService;
import org.apache.pluto.spi.optional.PortletEnvironmentService;
import org.apache.pluto.spi.optional.PortletPreferencesService;
import org.apache.pluto.spi.optional.UserInfoService;
import org.springframework.beans.factory.annotation.Required;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public class OptionalContainerServicesImpl extends DefaultOptionalContainerServices {
    private UserInfoService userInfoService;
    private PortalAdministrationService portalAdministrationService = new DefaultPortalAdministrationService();
    private PortletPreferencesService portletPreferencesService;
    private PortletEnvironmentService portletEnvironmentService;
    
    /**
     * @param userInfoService the userInfoService to set
     */
    @Required
    public void setUserInfoService(UserInfoService userInfoService) {
        Validate.notNull(userInfoService);
        this.userInfoService = userInfoService;
    }
    
    /**
     * @param portalAdministrationService the portalAdministrationService to set
     */
    public void setPortalAdministrationService(PortalAdministrationService portalAdministrationService) {
        Validate.notNull(portalAdministrationService);
        this.portalAdministrationService = portalAdministrationService;
    }
    
    /**
     * @param portletPreferencesService the portletPreferencesService to set
     */
    @Required
    public void setPortletPreferencesService(PortletPreferencesService portletPreferencesService) {
        Validate.notNull(portletPreferencesService);
        this.portletPreferencesService = portletPreferencesService;
    }

    /**
     * @param portletEnvironmentService the portletEnvironmentService to set
     */
    @Required
    public void setPortletEnvironmentService(PortletEnvironmentService portletEnvironmentService) {
        Validate.notNull(portletEnvironmentService);
        this.portletEnvironmentService = portletEnvironmentService;
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.core.DefaultOptionalContainerServices#getPortalAdministrationService()
     */
    @Override
    public PortalAdministrationService getPortalAdministrationService() {
        return this.portalAdministrationService;
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.core.DefaultOptionalContainerServices#getPortletPreferencesService()
     */
    @Override
    public PortletPreferencesService getPortletPreferencesService() {
        return this.portletPreferencesService;
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.core.DefaultOptionalContainerServices#getUserInfoService()
     */
    @Override
    public UserInfoService getUserInfoService() {
        return this.userInfoService;
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.core.DefaultOptionalContainerServices#getPortletEnvironmentService()
     */
    @Override
    public PortletEnvironmentService getPortletEnvironmentService() {
        return this.portletEnvironmentService;
    }

}
