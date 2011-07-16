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

package org.jasig.portal.services.stats;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.portal.IUserProfile;
import org.jasig.portal.layout.node.IUserLayoutChannelDescription;
import org.jasig.portal.layout.node.IUserLayoutFolderDescription;
import org.jasig.portal.portlet.om.IPortletDefinition;
import org.jasig.portal.security.IPerson;

/**
 * Stats recorder implementation which conditionally propogates IStatsRecorder
 * events to a target IStatsRecorder.
 * 
 * This class just applies configured rules about which events to propogate.  It
 * requires that a target IStatsRecorder instance be injected via the 
 * setTargetStatsRecorder() setter method.
 * 
 * @version $Revision$ $Date$
 * @since uPortal 2.5.1
 */
public final class ConditionalStatsRecorder 
    implements IStatsRecorder {
    
    private final Log log = LogFactory.getLog(getClass());
    
    /**
     * IStatsRecorder to which we will conditionally propogate IStatsRecorder 
     * method calls.
     */
    private IStatsRecorder targetStatsRecorder;
    
    /**
     * Bundle of boolean flags indicating whether we should propogate each
     * IStatsRecorder method.
     */
    private IStatsRecorderFlags flags = new StatsRecorderFlagsImpl();

    /**
     * Returns the IStatsRecorder to which we will or will not propogate IStatsRecorder
     * method calls depending upon our configuration.
     * @return Returns the targetStatsRecorder.
     */
    public IStatsRecorder getTargetStatsRecorder() {
        return this.targetStatsRecorder;
    }
    
    /**
     * Set the IStatsRecorder to which we will (or will not) propogate IStatsRecorder
     * method calls depending upon our configuration.
     * @param targetStatsRecorder The targetStatsRecorder to set.
     */
    public void setTargetStatsRecorder(IStatsRecorder targetStatsRecorder) {
        
        if (targetStatsRecorder == null) {
            throw new IllegalArgumentException("Cannot set targetStatsRecorder to null");
        }
        
        this.targetStatsRecorder = targetStatsRecorder;
    }
    

    
    /**
     * Get the StatsRecorderFlags instance defining which IStatsRecorder method
     * calls we should propogate and which we should not.
     * @return flags indicating which methods we should propogate and which we should not.
     */
    public IStatsRecorderFlags getFlags() {
        return this.flags;
    }
    
    /**
     * Set the boolean flags indicating which IStatsRecorder method calls we
     * should propogate and which we should not.
     * @param flags The flags to set.
     */
    public void setFlags(IStatsRecorderFlags flags) {
        
        if (flags == null) {
            throw new IllegalArgumentException("Cannot set flags to null.");
        }
        
        this.flags = flags;
    }
    

    public void recordLogin(IPerson person) {
        if (this.flags.isRecordLogin()) {

            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder was illegally null");
            } else {
                this.targetStatsRecorder.recordLogin(person);
            }
        }
    }
    
    public void recordLogout(IPerson person) {
        if (this.flags.isRecordLogout()) {
            
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordLogout(person);
            }
            
        }
    }
    
    
    public void recordSessionCreated(IPerson person) {
        
        if (this.flags.isRecordSessionCreated()) {
            
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordSessionCreated(person);
            }
            
        }
    }

    public void recordSessionDestroyed(IPerson person) {
        if (this.flags.isRecordSessionDestroyed()) {
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordSessionDestroyed(person);
            }
        }
    }

    public void recordChannelDefinitionPublished(IPerson person, IPortletDefinition channelDef) {
        if (this.flags.isRecordChannelDefinitionPublished()) {
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordChannelDefinitionPublished(person, channelDef);
            }
        }
    }

    public void recordChannelDefinitionModified(IPerson person, IPortletDefinition channelDef) {
        if (this.flags.isRecordChannelDefinitionModified()) {
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordChannelDefinitionModified(person, channelDef);
            }
        }
    }

    public void recordChannelDefinitionRemoved(IPerson person, IPortletDefinition channelDef) {
        if (this.flags.isRecordChannelDefinitionRemoved()) {
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordChannelDefinitionRemoved(person, channelDef);
            }
        }
    }

    public void recordChannelAddedToLayout(IPerson person, IUserProfile profile, IUserLayoutChannelDescription channelDesc) {
        if (this.flags.isRecordChannelAddedToLayout()) {
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordChannelAddedToLayout(person, profile, channelDesc);
            }
        }
    }

    public void recordChannelUpdatedInLayout(IPerson person, IUserProfile profile, IUserLayoutChannelDescription channelDesc) {
        if (this.flags.isRecordChannelUpdatedInLayout()) {
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordChannelUpdatedInLayout(person, profile, channelDesc);
            }
        }
    }

    public void recordChannelMovedInLayout(IPerson person, IUserProfile profile, IUserLayoutChannelDescription channelDesc) {
        if (this.flags.isRecordChannelMovedInLayout()) {
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordChannelMovedInLayout(person, profile, channelDesc);
            }
        }
    }

    public void recordChannelRemovedFromLayout(IPerson person, IUserProfile profile, IUserLayoutChannelDescription channelDesc) {
        if (this.flags.isRecordChannelRemovedFromLayout()) {
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordChannelRemovedFromLayout(person, profile, channelDesc);
            }
        }
    }

    public void recordFolderAddedToLayout(IPerson person, IUserProfile profile, IUserLayoutFolderDescription folderDesc) {
        if (this.flags.isRecordFolderAddedToLayout()) {
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordFolderAddedToLayout(person, profile, folderDesc);
            }
        }
    }

    public void recordFolderUpdatedInLayout(IPerson person, IUserProfile profile, IUserLayoutFolderDescription folderDesc) {
        if (this.flags.isRecordFolderUpdatedInLayout()) {
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordFolderUpdatedInLayout(person, profile, folderDesc);
            }
        }
    }

    public void recordFolderMovedInLayout(IPerson person, IUserProfile profile, IUserLayoutFolderDescription folderDesc) {
        if (this.flags.isRecordFolderMovedInLayout()) {
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordFolderMovedInLayout(person, profile, folderDesc);
            }
        }
    }

    public void recordFolderRemovedFromLayout(IPerson person, IUserProfile profile, IUserLayoutFolderDescription folderDesc) {
        if (this.flags.isRecordFolderRemovedFromLayout()) {
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordFolderRemovedFromLayout(person, profile, folderDesc);
            }
        }
    }

    public void recordChannelInstantiated(IPerson person, IUserProfile profile, IUserLayoutChannelDescription channelDesc) {
        if (this.flags.isRecordChannelInstantiated()) {
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordChannelInstantiated(person, profile, channelDesc);
            }
        }
    }

    public void recordChannelRendered(IPerson person, IUserProfile profile, IUserLayoutChannelDescription channelDesc) {
        if (this.flags.isRecordChannelRendered()) {
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordChannelRendered(person, profile, channelDesc);
            }
        }
    }

    public void recordChannelTargeted(IPerson person, IUserProfile profile, IUserLayoutChannelDescription channelDesc) {
        if (this.flags.isRecordChannelTargeted()) {
            if (this.targetStatsRecorder == null) {
                log.error("targetStatsRecorder of ConditionalStatsRecorder illegally null");
            } else {
                this.targetStatsRecorder.recordChannelTargeted(person, profile, channelDesc);
            }
        }
    }

}

