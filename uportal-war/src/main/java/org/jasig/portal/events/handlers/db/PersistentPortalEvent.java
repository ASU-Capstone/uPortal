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

package org.jasig.portal.events.handlers.db;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jasig.portal.events.PortalEvent;

/**
 * Persistent wrapper for storing portal events
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
@Entity
@Table(name = "UP_RAW_EVENTS")
@Inheritance(strategy=InheritanceType.JOINED)
@SequenceGenerator(
        name="UP_RAW_EVENTS_GEN",
        sequenceName="UP_RAW_EVENTS_SEQ",
        allocationSize=1000
    )
@TableGenerator(
        name="UP_RAW_EVENTS_GEN",
        pkColumnValue="UP_RAW_EVENTS_PROP",
        allocationSize=1000
    )
@Immutable
public class PersistentPortalEvent implements Serializable {
    private static final long serialVersionUID = 1L;
   
    @Id
    @GeneratedValue(generator = "UP_RAW_EVENTS_GEN")
    @Column(name="EVENT_ID")
    @SuppressWarnings("unused")
    private final long id;
    
    @Index(name = "IDX_UP_RAW_EVENTS_TIMESTAMP")
    @Column(name="TIMESTAMP", nullable=false)
    @SuppressWarnings("unused")
    private final Date timestamp;
    
    @Column(name="SERVER_ID", length=200, nullable=false)
    @SuppressWarnings("unused")
    private final String serverId;
    
    @Column(name="SESSION_ID", length=500, nullable=false)
    @SuppressWarnings("unused")
    private final String eventSessionId;
    
    @Column(name="USER_NAME", length=35, nullable=false)
    @SuppressWarnings("unused")
    private final String userName;
    
    @Column(name="EVENT_TYPE", length=200, nullable=false)
    @Type(type="class")
    private final Class<PortalEvent> eventType;
    
    @Column(name = "EVENT_DATA", nullable=false)
    @Lob
    private final String eventData; 
    
    /**
     * no-arg needed by hibernate
     */
    @SuppressWarnings("unused")
    private PersistentPortalEvent() {
        this.id = -1;
        this.eventData = null;
        this.timestamp = null;
        this.serverId = null;
        this.eventSessionId = null;
        this.userName = null;
        this.eventType = null;
    }
    
    @SuppressWarnings("unchecked")
    PersistentPortalEvent(PortalEvent portalEvent, String eventData) {
        this.id = -1;
        this.eventData = eventData;
        this.timestamp = portalEvent.getTimestampAsDate();
        this.serverId = portalEvent.getServerId();
        this.eventSessionId = portalEvent.getEventSessionId();
        this.userName = portalEvent.getUserName();
        this.eventType = (Class<PortalEvent>)portalEvent.getClass();
    }
    
    public Class<PortalEvent> getEventType() {
        return this.eventType;
    }

    /**
     * @return the eventData
     */
    public String getEventData() {
        return this.eventData;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.eventData;
    }
}
