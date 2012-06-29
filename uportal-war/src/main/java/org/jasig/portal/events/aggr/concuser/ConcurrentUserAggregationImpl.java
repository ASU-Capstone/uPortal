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

package org.jasig.portal.events.aggr.concuser;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.Cacheable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.NaturalIdCache;
import org.jasig.portal.events.aggr.AggregationInterval;
import org.jasig.portal.events.aggr.BaseAggregationImpl;
import org.jasig.portal.events.aggr.DateDimension;
import org.jasig.portal.events.aggr.TimeDimension;
import org.jasig.portal.events.aggr.groups.AggregatedGroupMapping;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
@Entity
@Table(name = "UP_CONCURRENT_USER_AGGR")
@Inheritance(strategy=InheritanceType.JOINED)
@SequenceGenerator(
        name="UP_CONCURRENT_USER_AGGR_GEN",
        sequenceName="UP_CONCURRENT_USER_AGGR_SEQ",
        allocationSize=1000
    )
@TableGenerator(
        name="UP_CONCURRENT_USER_AGGR_GEN",
        pkColumnValue="UP_CONCURRENT_USER_AGGR_PROP",
        allocationSize=1000
    )
@org.hibernate.annotations.Table(
        appliesTo = "UP_CONCURRENT_USER_AGGR",
        indexes = @Index(name = "IDX_UP_CONC_USER_AGGR_DTI", columnNames = { "DATE_DIMENSION_ID", "TIME_DIMENSION_ID", "AGGR_INTERVAL" }))
@NaturalIdCache
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public final class ConcurrentUserAggregationImpl 
        extends BaseAggregationImpl<ConcurrentUserAggregationKey> 
        implements ConcurrentUserAggregation, Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(generator = "UP_CONCURRENT_USER_AGGR_GEN")
    @Column(name="ID")
    private final long id;
    
    @Column(name = "CONCURRENT_USERS", nullable = false)
    private int concurrentUsers;
    
    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(
            name = "UP_CONCURRENT_USER_AGGR__SIDS",
            joinColumns = @JoinColumn(name = "CONC_USER_AGGR_ID")
        )
    @Column(name="SESSION_ID", nullable=false, updatable=false, length=500)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<String> uniqueSessionIds = new LinkedHashSet<String>();
    
    @Transient
    private ConcurrentUserAggregationKey aggregationKey;
    
    @SuppressWarnings("unused")
    private ConcurrentUserAggregationImpl() {
        super();
        this.id = -1;
    }
    
    ConcurrentUserAggregationImpl(TimeDimension timeDimension, DateDimension dateDimension, 
            AggregationInterval interval, AggregatedGroupMapping aggregatedGroup) {
        super(timeDimension, dateDimension, interval, aggregatedGroup);
        this.id = -1;
    }
    
    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public int getConcurrentUsers() {
        return this.concurrentUsers;
    }

    @Override
    public ConcurrentUserAggregationKey getAggregationKey() {
        ConcurrentUserAggregationKey key = this.aggregationKey;
        if (key == null) {
            key = new ConcurrentUserAggregationKeyImpl(this);
            this.aggregationKey = key;
        }
        return key;
    }

    @Override
    protected boolean isComplete() {
        return this.concurrentUsers > 0 && this.uniqueSessionIds.isEmpty();
    }

    @Override
    protected void completeInterval() {
        this.uniqueSessionIds.clear();
    }
    
    void countSession(String eventSessionId) {
        checkState();
        
        if (this.uniqueSessionIds.add(eventSessionId)) {
            this.concurrentUsers++;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ConcurrentUserAggregation))
            return false;
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "ConcurrentUserAggregationImpl [id=" + id + ", dateDimension=" + getDateDimension() + ", timeDimension=" + getTimeDimension()
                + ", interval=" + getInterval() + ", aggregatedGroup=" + getAggregatedGroup() + ", duration=" + getDuration()
                + ", concurrentUsers=" + concurrentUsers + "]";
    }
}
