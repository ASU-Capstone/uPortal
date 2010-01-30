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

package org.jasig.portal.layout.dlm;

import org.danann.cernunnos.Task;
import org.jasig.portal.utils.threading.SingletonDoubleCheckedCreator;
import org.springframework.beans.factory.annotation.Required;

public class LayoutStoreProvider {

    private Task lookupNoderefTask;
    private Task lookupPathrefTask;

    private final SingletonDoubleCheckedCreator<RDBMDistributedLayoutStore> creator = new SingletonDoubleCheckedCreator<RDBMDistributedLayoutStore>() {
        protected RDBMDistributedLayoutStore createSingleton(Object... args) {
            RDBMDistributedLayoutStore rslt = null;
            try {
                rslt = new RDBMDistributedLayoutStore();
                rslt.setLookupNoderefTask(lookupNoderefTask);
                rslt.setLookupPathrefTask(lookupPathrefTask);
            } catch (Throwable t) {
                String msg = "Failed to instantiate RDBMDistributedLayoutStore.";
                throw new RuntimeException(msg, t);
            }
            return rslt;
        }
    };

    @Required
    public void setLookupNoderefTask(Task k) {
        this.lookupNoderefTask = k;
    }

    @Required
    public void setLookupPathrefTask(Task k) {
        this.lookupPathrefTask = k;
    }
    
    public RDBMDistributedLayoutStore getLayoutStore() {
        return creator.get();
    }

}
