/**
 * Copyright 2007 The JA-SIG Collaborative.  All rights reserved.
 * See license distributed with this file and
 * available online at http://www.uportal.org/license.html
 */
package org.jasig.portal.security.xslt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.portal.ChannelDefinition;
import org.jasig.portal.IChannelRegistryStore;
import org.jasig.portal.groups.IEntity;
import org.jasig.portal.groups.IEntityGroup;
import org.jasig.portal.security.IPerson;
import org.jasig.portal.services.GroupService;
import org.springframework.beans.factory.annotation.Required;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public class XalanGroupMembershipHelperBean implements IXalanGroupMembershipHelper {
    protected final Log logger = LogFactory.getLog(this.getClass());
    
    private IChannelRegistryStore channelRegistryStore;
    
    public IChannelRegistryStore getChannelRegistryStore() {
        return this.channelRegistryStore;
    }
    /**
     * @param channelRegistryStore the channelRegistryStore to set
     */
    @Required
    public void setChannelRegistryStore(IChannelRegistryStore channelRegistryStore) {
        this.channelRegistryStore = channelRegistryStore;
    }


    /* (non-Javadoc)
     * @see org.jasig.portal.security.xslt.IXalanGroupMembershipHelper#isChannelDeepMemberOf(java.lang.String, java.lang.String)
     */
    public boolean isChannelDeepMemberOf(String fname, String groupKey) {
        final IEntityGroup distinguishedGroup = GroupService.findGroup(groupKey);
        if (distinguishedGroup == null) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("No group found for key '" + groupKey + "'");
            }
            
            return false;
        }
        
        final ChannelDefinition channelDefinition;
        try {
            channelDefinition = this.channelRegistryStore.getChannelDefinition(fname);
        }
        catch (Exception e) {
            this.logger.warn("Caught exception while retrieving channel definition for fname '" + fname + "'", e);
            return false;
        }
        
        if (channelDefinition == null) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("No channel found for key '" + fname + "'");
            }
            
            return false;
        }
        
        final Integer channelId = channelDefinition.getId();
        final IEntity entity = GroupService.getEntity(channelId.toString(), ChannelDefinition.class);
        if (entity == null) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("No channel found for id '" + channelId + "'");
            }
            
            return false;
        }
        
        return distinguishedGroup.deepContains(entity);
    }


    /* (non-Javadoc)
     * @see org.jasig.portal.security.xslt.IXalanGroupMembershipHelper#isUserDeepMemberOf(java.lang.String, java.lang.String)
     */
    public boolean isUserDeepMemberOf(String userName, String groupKey) {
        final IEntityGroup distinguishedGroup = GroupService.findGroup(groupKey);
        if (distinguishedGroup == null) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("No group found for key '" + groupKey + "'");
            }
            
            return false;
        }
        
        final IEntity entity = GroupService.getEntity(userName, IPerson.class);
        if (entity == null) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("No user found for key '" + userName + "'");
            }
            
            return false;
        }
        
        return distinguishedGroup.deepContains(entity);
    }
}
