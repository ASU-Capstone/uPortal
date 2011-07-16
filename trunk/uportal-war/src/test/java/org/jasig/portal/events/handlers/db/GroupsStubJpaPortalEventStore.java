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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jasig.portal.groups.GroupsException;
import org.jasig.portal.security.IPerson;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public class GroupsStubJpaPortalEventStore extends JpaPortalEventStore implements ITestablePortalEventStore {
    private Map<IPerson, Set<String>> personGroups = new HashMap<IPerson, Set<String>>();
    
    /**
     * @return the personGroups
     */
    public Map<IPerson, Set<String>> getPersonGroups() {
        return personGroups;
    }
    /**
     * @param personGroups the personGroups to set
     */
    public void setPersonGroups(Map<IPerson, Set<String>> personGroups) {
        this.personGroups = personGroups;
    }
    
    public void addPersonGroups(IPerson person, Set<String> groups) {
        this.personGroups.put(person, groups);
    }


    /* (non-Javadoc)
     * @see org.jasig.portal.events.handlers.db.JpaPortalEventStore#updateStatsSessionGroups(org.jasig.portal.events.handlers.db.StatsSession, org.jasig.portal.security.IPerson)
     */
    @Override
    protected void updateStatsSessionGroups(StatsSession session, IPerson person) throws GroupsException {
        final Set<String> groups = this.personGroups.get(person);
        session.setGroups(groups);
    }

}
