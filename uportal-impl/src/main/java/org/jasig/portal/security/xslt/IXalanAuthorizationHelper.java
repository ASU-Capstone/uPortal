/**
 * Copyright (c) 2000-2009, Jasig, Inc.
 * See license distributed with this file and available online at
 * https://www.ja-sig.org/svn/jasig-parent/tags/rel-10/license-header.txt
 */
package org.jasig.portal.security.xslt;

/**
 * Authorization helper APIs.
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public interface IXalanAuthorizationHelper {
    /**
     * Checks if the specified user can render the specified channel.
     * 
     * @param userName Looks up the user with the matching {@link org.jasig.portal.security.IPerson#USERNAME}
     * @param channelFName Looks up the {@link org.jasig.portal.ChannelDefinition} with the matching fname
     * @return true if the user has permission to render the channel, false for any other case.
     */
    public boolean canRender(final String userName, final String channelFName);
}
