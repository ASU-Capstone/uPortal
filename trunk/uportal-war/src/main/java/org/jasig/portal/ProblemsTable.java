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

package org.jasig.portal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jasig.portal.properties.PropertiesManager;

/**
 * A cache of recently reported PortalExceptions.
 * @author Howard Gilbert
 * @author andrew.petro@yale.edu
 * @version $Revision$
 */
public class ProblemsTable {

    /** TreeMap from ErrorID Categories to TreeMaps.
     *   The enclosed TreeMaps map from Specifics (ErrorID subcategories)
     *  to CountID objects.  The CountID objects cache the PortalExceptions
     *  that were in the Specific.
     */
	public static Map<String, Map<String, CountID>> registeredIds = new TreeMap<String, Map<String, CountID>>();
    
    /**
     * List of recently modified CountID instances.
     */
	public static LinkedList<CountID> recentIds = new LinkedList<CountID>();
    
    /**
     * List of recently reported PortalExceptions, regardless of category.
     */
    private static LinkedList<PortalException> recentPortalExceptions = new LinkedList<PortalException>();
    
    /**
     * The name of the PropertiesManager property the value of which should be the 
     * number of recent PortalExceptions you would like stored for each specific subcategory of ErrorID.
     */
    public static final String MAX_RECENT_ERRORS_PER_SPECIFIC_PROPERTY = "org.jasig.portal.ProblemsTable.maxRecentErrorsPerSpecific";
    
    /**
     * The default number of recent PortalExceptions that will be stored for each specific subcategory of ErrorID
     * in the case where the relevant property is not set.
     */
	private static final int DEFAULT_MAX_RECENT_PER_SPECIFIC = 10;
    
    /**
     * The number of recent PortalExceptions that will be stored for each specific subcategory of ErrorID.
     */
    private static final int maxRecent = PropertiesManager.getPropertyAsInt(MAX_RECENT_ERRORS_PER_SPECIFIC_PROPERTY, DEFAULT_MAX_RECENT_PER_SPECIFIC);
    
    /**
     * The name of the propertiesManager property the value of which should be the number of recent
     * PortalExceptions you would like stored in the overall FIFO cache, regardless of ErrorID.
     */
    public static final String OVERALL_RECENT_ERRORS_PROPERTY = "org.jasig.portal.ProblemsTable.recentErrorsOverall";
    
    /**
     * The default number of recent PortalExceptions that will be stored in the overall FIFO queue
     * regardless of ErrorID, which will be used in the case where the relevant property is not set.
     */
    private static final int DEFAULT_OVERALL_RECENT_ERRORS_COUNT = 40;
    
    /**
     * The number of recent PortalExceptions that will be stored in the overall FIFO queue
     * regardless of ErrorID.
     */
    private static final int overallErrorsCount = PropertiesManager.getPropertyAsInt(OVERALL_RECENT_ERRORS_PROPERTY, DEFAULT_OVERALL_RECENT_ERRORS_COUNT);

	/**
	 * Add ErrorID to TreeMaps
	 * 
	 * @param id ErrorID (ignored if duplicate)
	 */
	public synchronized static void register(ErrorID id) {
		if (id == null)
			return;
		String category = id.getCategory();
		String specific = id.getSpecific();
		Map<String, CountID> minor = registeredIds.get(category);
		if (minor == null) {
			minor = new TreeMap<String, CountID>();
			registeredIds.put(category, minor);
		}
		if (!minor.containsKey(specific)) {
			minor.put(specific, new CountID(id));
		}
	}

	/**
	 * Store a PortalException in the tables.
	 * 
	 * @param pe PortalException to be tabulated
	 */
	public synchronized static void store(PortalException pe) {
		if (pe == null)
			return; // bad argument
        if (recentPortalExceptions.contains(pe))
            return; // already recorded
		ErrorID id = pe.getErrorID();
		if (id == null)
			return; // no ErrorID (Msg only PortalException)
		String category = id.getCategory();
		String specific = id.getSpecific();
		Map<String, CountID> minor = registeredIds.get(category);
		if (minor == null)
			return; // ErrorID not registered
		CountID countid = minor.get(specific);
		if (countid == null)
			return; // ErrorID not registered

		countid.count++;
		countid.lastPortalException = pe;

		recentIds.remove(countid);         
		recentIds.addFirst(countid);
		if (recentIds.size()>maxRecent)
			recentIds.removeLast();
        
        // store this PortalException in the overall FIFO queue of recent PortalExceptions.
        ProblemsTable.recentPortalExceptions.addFirst(pe);
        if (ProblemsTable.recentPortalExceptions.size() > ProblemsTable.overallErrorsCount)
            ProblemsTable.recentPortalExceptions.removeLast();
	}
    
    /**
     * Get an unmodifiable shallow copy of the list of recent PortalExceptions.
     * @return an unmodifiable shallow copy of the list of recent PortalExceptions.
     */
    public synchronized static List<PortalException> getRecentPortalExceptions(){
        return Collections.unmodifiableList(new ArrayList<PortalException>(ProblemsTable.recentPortalExceptions));
    }

}

/**
 * ErrorID tabulation class
 * 
 * The TreeMaps yield an instance of this class
 */
class CountID {
	ErrorID errorID = null;
	int count = 0;
	PortalException lastPortalException = null;

	CountID(ErrorID id) {
		errorID = id;
	}
}