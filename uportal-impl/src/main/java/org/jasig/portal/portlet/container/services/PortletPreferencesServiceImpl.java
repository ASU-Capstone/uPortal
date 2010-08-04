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

package org.jasig.portal.portlet.container.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.Validate;
import org.apache.pluto.PortletContainerException;
import org.apache.pluto.PortletWindow;
import org.apache.pluto.descriptors.portlet.PortletDD;
import org.apache.pluto.descriptors.portlet.PortletPreferenceDD;
import org.apache.pluto.descriptors.portlet.PortletPreferencesDD;
import org.apache.pluto.internal.InternalPortletPreference;
import org.apache.pluto.spi.optional.PortletPreferencesService;
import org.jasig.portal.channels.portlet.IPortletAdaptor;
import org.jasig.portal.portlet.dao.jpa.PortletPreferenceImpl;
import org.jasig.portal.portlet.om.IPortletDefinition;
import org.jasig.portal.portlet.om.IPortletEntity;
import org.jasig.portal.portlet.om.IPortletEntityId;
import org.jasig.portal.portlet.om.IPortletPreference;
import org.jasig.portal.portlet.om.IPortletPreferences;
import org.jasig.portal.portlet.om.IPortletWindow;
import org.jasig.portal.portlet.registry.IPortletDefinitionRegistry;
import org.jasig.portal.portlet.registry.IPortletEntityRegistry;
import org.jasig.portal.portlet.registry.IPortletWindowRegistry;
import org.jasig.portal.security.IPerson;
import org.jasig.portal.security.IPersonManager;
import org.jasig.portal.url.IPortalRequestUtils;
import org.springframework.beans.factory.annotation.Required;

/**
 * Hooks into uPortal portlet preferences object model
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public class PortletPreferencesServiceImpl implements PortletPreferencesService {
    protected static final String PORTLET_PREFERENCES_MAP_ATTRIBUTE = PortletPreferencesServiceImpl.class.getName() + ".PORTLET_PREFERENCES_MAP";
    
    private IPersonManager personManager;
    private IPortletWindowRegistry portletWindowRegistry;
    private IPortletEntityRegistry portletEntityRegistry;
    private IPortletDefinitionRegistry portletDefinitionRegistry;
    private IPortalRequestUtils portalRequestUtils;
    
    private boolean loadGuestPreferencesFromMemory = true;
    private boolean loadGuestPreferencesFromEntity = true;
    private boolean storeGuestPreferencesInMemory = true;
    private boolean storeGuestPreferencesInEntity = false;
    
    /**
     * @return the portalRequestUtils
     */
    public IPortalRequestUtils getPortalRequestUtils() {
        return portalRequestUtils;
    }
    /**
     * @param portalRequestUtils the portalRequestUtils to set
     */
    @Required
    public void setPortalRequestUtils(IPortalRequestUtils portalRequestUtils) {
        Validate.notNull(portalRequestUtils);
        this.portalRequestUtils = portalRequestUtils;
    }
    
    /**
     * @return the portletWindowRegistry
     */
    public IPortletWindowRegistry getPortletWindowRegistry() {
        return this.portletWindowRegistry;
    }
    /**
     * @param portletWindowRegistry the portletWindowRegistry to set
     */
    @Required
    public void setPortletWindowRegistry(IPortletWindowRegistry portletWindowRegistry) {
        Validate.notNull(portletWindowRegistry);
        this.portletWindowRegistry = portletWindowRegistry;
    }

    /**
     * @return the portletEntityRegistry
     */
    public IPortletEntityRegistry getPortletEntityRegistry() {
        return this.portletEntityRegistry;
    }
    /**
     * @param portletEntityRegistry the portletEntityRegistry to set
     */
    @Required
    public void setPortletEntityRegistry(IPortletEntityRegistry portletEntityRegistry) {
        Validate.notNull(portletEntityRegistry);
        this.portletEntityRegistry = portletEntityRegistry;
    }

    /**
     * @return the portletDefinitionRegistry
     */
    public IPortletDefinitionRegistry getPortletDefinitionRegistry() {
        return this.portletDefinitionRegistry;
    }
    /**
     * @param portletDefinitionRegistry the portletDefinitionRegistry to set
     */
    @Required
    public void setPortletDefinitionRegistry(IPortletDefinitionRegistry portletDefinitionRegistry) {
        Validate.notNull(portletDefinitionRegistry);
        this.portletDefinitionRegistry = portletDefinitionRegistry;
    }
    
    /**
     * @return the personManager
     */
    public IPersonManager getPersonManager() {
        return personManager;
    }
    /**
     * @param personManager the personManager to set
     */
    @Required
    public void setPersonManager(IPersonManager personManager) {
        Validate.notNull(personManager);
        this.personManager = personManager;
    }

    
    public boolean isLoadGuestPreferencesFromMemory() {
		return loadGuestPreferencesFromMemory;
	}
	public void setLoadGuestPreferencesFromMemory(
			boolean loadGuestPreferencesFromMemory) {
		this.loadGuestPreferencesFromMemory = loadGuestPreferencesFromMemory;
	}
	public boolean isLoadGuestPreferencesFromEntity() {
		return loadGuestPreferencesFromEntity;
	}
	public void setLoadGuestPreferencesFromEntity(
			boolean loadGuestPreferencesFromEntity) {
		this.loadGuestPreferencesFromEntity = loadGuestPreferencesFromEntity;
	}
	public boolean isStoreGuestPreferencesInMemory() {
		return storeGuestPreferencesInMemory;
	}
	public void setStoreGuestPreferencesInMemory(
			boolean storeGuestPreferencesInMemory) {
		this.storeGuestPreferencesInMemory = storeGuestPreferencesInMemory;
	}
	public boolean isStoreGuestPreferencesInEntity() {
		return storeGuestPreferencesInEntity;
	}
	public void setStoreGuestPreferencesInEntity(
			boolean storeGuestPreferencesInEntity) {
		this.storeGuestPreferencesInEntity = storeGuestPreferencesInEntity;
	}
	public boolean isStoreInEntity(PortletRequest portletRequest) { 
        final HttpServletRequest httpServletRequest = this.portalRequestUtils.getOriginalPortletAdaptorRequest(portletRequest);
    	if (!isGuestUser(httpServletRequest) || this.storeGuestPreferencesInEntity)
    		return true;
    	else
    		return false; 
    }
    
    public boolean isLoadFromEntity(PortletRequest portletRequest) { 
        final HttpServletRequest httpServletRequest = this.portalRequestUtils.getOriginalPortletAdaptorRequest(portletRequest);
    	if (!isGuestUser(httpServletRequest) || this.loadGuestPreferencesFromEntity)
    		return true;
    	else
    		return false; 
    }
    
    public boolean isStoreInMemory(PortletRequest portletRequest) { 
        final HttpServletRequest httpServletRequest = this.portalRequestUtils.getOriginalPortletAdaptorRequest(portletRequest);
    	if (isGuestUser(httpServletRequest) && this.storeGuestPreferencesInMemory)
    		return true;
    	else
    		return false; 
    }

    public boolean isLoadFromMemory(PortletRequest portletRequest) { 
        final HttpServletRequest httpServletRequest = this.portalRequestUtils.getOriginalPortletAdaptorRequest(portletRequest);
    	if (isGuestUser(httpServletRequest) && this.loadGuestPreferencesFromMemory)
    		return true;
    	else
    		return false; 
    }
    
    /* (non-Javadoc)
     * @see org.apache.pluto.spi.optional.PortletPreferencesService#getStoredPreferences(org.apache.pluto.PortletWindow, javax.portlet.PortletRequest)
     */
    public InternalPortletPreference[] getStoredPreferences(PortletWindow plutoPortletWindow, PortletRequest portletRequest) throws PortletContainerException {
        final HttpServletRequest httpServletRequest = this.portalRequestUtils.getOriginalPortletAdaptorRequest(portletRequest);
        
        final IPortletWindow portletWindow = this.portletWindowRegistry.convertPortletWindow(httpServletRequest, plutoPortletWindow);
        final IPortletEntity portletEntity = this.portletWindowRegistry.getParentPortletEntity(httpServletRequest, portletWindow.getPortletWindowId());
        final IPortletDefinition portletDefinition = this.portletEntityRegistry.getParentPortletDefinition(portletEntity.getPortletEntityId());
        final PortletDD portletDescriptor = this.portletDefinitionRegistry.getParentPortletDescriptor(portletDefinition.getPortletDefinitionId());

        
        //Linked hash map used to add preferences in order loaded from the descriptor, definition and entity
        final LinkedHashMap<String, InternalPortletPreference> preferencesMap = new LinkedHashMap<String, InternalPortletPreference>();
        
        //Add descriptor preferences
        final List<IPortletPreference> descriptorPreferencesList = this.getDescriptorPreferences(portletDescriptor);
        this.addPreferencesToMap(descriptorPreferencesList, preferencesMap);
        
        //Add definition preferences
        final IPortletPreferences definitionPreferences = portletDefinition.getPortletPreferences();
        final List<IPortletPreference> definitionPreferencesList = definitionPreferences.getPortletPreferences();
        this.addPreferencesToMap(definitionPreferencesList, preferencesMap);
        
        //Determine if the user is a guest
        final boolean isGuest = isGuestUser(httpServletRequest);
        
        if (!IPortletAdaptor.CONFIG.equals(portletWindow.getPortletMode())) {
            //If not guest or storing shared guest prefs get the prefs from the portlet entity
            if (this.isLoadFromEntity(portletRequest)) {
                //Add entity preferences
                final IPortletPreferences entityPreferences = portletEntity.getPortletPreferences();
                final List<IPortletPreference> entityPreferencesList = entityPreferences.getPortletPreferences();
                this.addPreferencesToMap(entityPreferencesList, preferencesMap);
    
                if (!this.isLoadFromMemory(portletRequest) && !this.isStoreInEntity(portletRequest) && this.isStoreInMemory(portletRequest)) {
                    store(plutoPortletWindow, portletRequest, preferencesMap.values().toArray(new InternalPortletPreference[preferencesMap.size()]));
                }
            }
            //If a guest and storing non-shared guest prefs get the prefs from the session
            if (this.isLoadFromMemory(portletRequest)) {
                //Add memory preferences
                final List<IPortletPreference> entityPreferencesList = this.getSessionPreferences(portletEntity.getPortletEntityId(), httpServletRequest);
                this.addPreferencesToMap(entityPreferencesList, preferencesMap);
            }
        }

        return preferencesMap.values().toArray(new InternalPortletPreference[preferencesMap.size()]);
    }

    /* (non-Javadoc)
     * @see org.apache.pluto.spi.optional.PortletPreferencesService#store(org.apache.pluto.PortletWindow, javax.portlet.PortletRequest, org.apache.pluto.internal.InternalPortletPreference[])
     */
    public void store(PortletWindow plutoPortletWindow, PortletRequest portletRequest, InternalPortletPreference[] newPreferences) throws PortletContainerException {
        final HttpServletRequest httpServletRequest = this.portalRequestUtils.getOriginalPortletAdaptorRequest(portletRequest);
        
        //Determine if the user is a guest
        final boolean isGuest = isGuestUser(httpServletRequest);
        
        //If this is a guest and no prefs are being stored just return as the rest of the method is not needed for this case
        if (isGuest && !(this.isStoreInEntity(portletRequest) || this.isStoreInMemory(portletRequest))) {
            return;
        }

        final IPortletWindow portletWindow = this.portletWindowRegistry.convertPortletWindow(httpServletRequest, plutoPortletWindow);
        final IPortletEntity portletEntity = this.portletWindowRegistry.getParentPortletEntity(httpServletRequest, portletWindow.getPortletWindowId());
        final IPortletDefinition portletDefinition = this.portletEntityRegistry.getParentPortletDefinition(portletEntity.getPortletEntityId());
        final PortletDD portletDescriptor = this.portletDefinitionRegistry.getParentPortletDescriptor(portletDefinition.getPortletDefinitionId());

        //Is this CONFIG mode
        final boolean configMode = IPortletAdaptor.CONFIG.equals(portletWindow.getPortletMode());
        
        //Get Map of descriptor and definition preferences to check new preferences against
        final Map<String, InternalPortletPreference> basePreferences = new HashMap<String, InternalPortletPreference>();
        
        //Add deploy preferences
        final List<IPortletPreference> descriptorPreferencesList = this.getDescriptorPreferences(portletDescriptor);
        this.addPreferencesToMap(descriptorPreferencesList, basePreferences);
        
        //Add definition preferences if not config mode
        final IPortletPreferences definitionPreferences = portletDefinition.getPortletPreferences();
        if (!configMode) {
            final List<IPortletPreference> definitionPreferencesList = definitionPreferences.getPortletPreferences();
            this.addPreferencesToMap(definitionPreferencesList, basePreferences);
        }

        final List<IPortletPreference> preferencesList = new ArrayList<IPortletPreference>(newPreferences.length);
        
        for (InternalPortletPreference internalPreference : newPreferences) {
            //Ignore preferences with null names
            final String name = internalPreference.getName();
            if (name == null) {
                continue;
            }

            //Convert to a uPortal preference class to ensure quality check and persistence works
            final IPortletPreference preference = new PortletPreferenceImpl(internalPreference);
            
            //If the preference exactly equals a descriptor or definition preference ignore it
            final InternalPortletPreference basePreference = basePreferences.get(name);
            if (preference.equals(basePreference)) {
                continue;
            }
            
            //New preference, add it to the list
            preferencesList.add(preference);
        }

        //If in config mode store the preferences on the definition
        if (configMode) {
            definitionPreferences.setPortletPreferences(preferencesList);
            this.portletDefinitionRegistry.updatePortletDefinition(portletDefinition);
        }
        //If not a guest or if guest prefs are shared store them on the entity
        else if (this.isStoreInEntity(portletRequest)) {
            //Update the portlet entity with the new preferences
            final IPortletPreferences entityPreferences = portletEntity.getPortletPreferences();
            entityPreferences.setPortletPreferences(preferencesList);
            this.portletEntityRegistry.storePortletEntity(portletEntity);
        }
        //Must be a guest and share must be off so store the prefs on the session
        else {
            //Store memory preferences
            this.storeSessionPreferences(portletEntity.getPortletEntityId(), httpServletRequest, preferencesList);
        }
    }
    
    /**
     * Gets the preferences for a portlet descriptor converted to the uPortal IPortletPreference
     * interface.
     */
    protected List<IPortletPreference> getDescriptorPreferences(PortletDD portletDescriptor) {
        final List<IPortletPreference> preferences = new LinkedList<IPortletPreference>();
        
        final PortletPreferencesDD descriptorPreferences = portletDescriptor.getPortletPreferences();
        if (descriptorPreferences != null) {
            final List<PortletPreferenceDD> descriptorPreferencesList = descriptorPreferences.getPortletPreferences();
            for (final PortletPreferenceDD descriptorPreference : descriptorPreferencesList) {
                final IPortletPreference internaldescriptorPreference = new PortletPreferenceImpl(descriptorPreference);
                preferences.add(internaldescriptorPreference);
            }
        }
        
        return preferences;
    }
    
    /**
     * Add all of the preferences in the List to the Map using the preference name as the key
     */
    protected void addPreferencesToMap(List<IPortletPreference> preferencesList, Map<String, InternalPortletPreference> preferencesMap) {
        if (preferencesList == null) {
            return;
        }

        for (final IPortletPreference definitionPreference : preferencesList) {
            preferencesMap.put(definitionPreference.getName(), new PortletPreferenceImpl(definitionPreference));
        }
    }
    
    
    /**
     * Determine if the user for the specified request is a guest as it pertains to shared portlet preferences.
     */
    protected boolean isGuestUser(final HttpServletRequest httpServletRequest) {
        final IPerson person = this.personManager.getPerson(httpServletRequest);
        return person.isGuest();
//        final ISecurityContext securityContext = person.getSecurityContext();
//        return !securityContext.isAuthenticated();
    }
    
    /**
     * Gets the session-stored list of IPortletPreferences for the specified request and IPortletEntityId.
     * 
     * @return List of IPortletPreferences for the entity and session, may be null if no preferences have been set.
     */
    protected List<IPortletPreference> getSessionPreferences(IPortletEntityId portletEntityId, HttpServletRequest httpServletRequest) {
        final HttpSession session = httpServletRequest.getSession();
        
        final Map<IPortletEntityId, List<IPortletPreference>> portletPreferences;
        
        //Sync on the session to ensure the Map isn't in the process of being created
        synchronized (session) {
            portletPreferences = (Map<IPortletEntityId, List<IPortletPreference>>)session.getAttribute(PORTLET_PREFERENCES_MAP_ATTRIBUTE);
        }
        
        if (portletPreferences == null) {
            return null;
        }

        return portletPreferences.get(portletEntityId);
    }
    
    protected void storeSessionPreferences(IPortletEntityId portletEntityId, HttpServletRequest httpServletRequest, List<IPortletPreference> preferences) {
        final HttpSession session = httpServletRequest.getSession();
        
        Map<IPortletEntityId, List<IPortletPreference>> portletPreferences;
        
        //Sync on the session to ensure other threads aren't creating the Map at the same time
        synchronized (session) {
            portletPreferences = (Map<IPortletEntityId, List<IPortletPreference>>)session.getAttribute(PORTLET_PREFERENCES_MAP_ATTRIBUTE);
            if (portletPreferences == null) {
                portletPreferences = new ConcurrentHashMap<IPortletEntityId, List<IPortletPreference>>();
                session.setAttribute(PORTLET_PREFERENCES_MAP_ATTRIBUTE, portletPreferences);
            }
        }
        
        portletPreferences.put(portletEntityId, preferences);
    }
}
