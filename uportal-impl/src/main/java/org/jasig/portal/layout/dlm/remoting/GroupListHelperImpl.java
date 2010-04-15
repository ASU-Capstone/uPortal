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

package org.jasig.portal.layout.dlm.remoting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.portal.EntityIdentifier;
import org.jasig.portal.channel.IChannelDefinition;
import org.jasig.portal.groups.IEntityGroup;
import org.jasig.portal.groups.IEntityNameFinder;
import org.jasig.portal.groups.IGroupMember;
import org.jasig.portal.portlets.groupselector.EntityEnum;
import org.jasig.portal.security.IPerson;
import org.jasig.portal.services.EntityNameFinderService;
import org.jasig.portal.services.GroupService;

public class GroupListHelperImpl implements IGroupListHelper {

	private static final Log log = LogFactory.getLog(GroupListHelperImpl.class);
	
	@SuppressWarnings("unchecked")
	public Set<JsonEntityBean> search(String entityType, String searchTerm) {
		
		Set<JsonEntityBean> results = new HashSet<JsonEntityBean>();

		EntityEnum entityEnum = EntityEnum.getEntityEnum(entityType);

		EntityIdentifier[] identifiers;
		
		Class identifierType;
		
		// if the entity type is a group, use the group service's findGroup method
		// to locate it
		if (entityEnum.isGroup()) {
			identifiers = GroupService.searchForGroups(searchTerm, GroupService.CONTAINS, 
					entityEnum.getClazz());
			identifierType = IEntityGroup.class;
		} 
		
		// otherwise use the getGroupMember method
		else {
			identifiers = GroupService.searchForEntities(searchTerm, GroupService.CONTAINS,
					entityEnum.getClazz());
			identifierType = entityEnum.getClazz();
		}
		
		for(int i=0;i<identifiers.length;i++) {
			if(identifiers[i].getType().equals(identifierType)) {
				IGroupMember entity = GroupService.getGroupMember(identifiers[i]);
				if(entity instanceof IEntityGroup) {
					/* Don't look up the children for a search. */
					JsonEntityBean jsonBean = new JsonEntityBean((IEntityGroup) entity, getEntityType(entity));
					results.add(jsonBean);
				} else {
					JsonEntityBean jsonBean = new JsonEntityBean(entity, getEntityType(entity));
					jsonBean.setName(lookupEntityName(jsonBean));
					results.add(jsonBean);
				}
			}
		}
		
		return results;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.jasig.portal.layout.dlm.remoting.IGroupListHelper#getEntity(java.lang.String, java.lang.String, boolean)
	 */
	public JsonEntityBean getEntity(String entityType, String entityId, boolean populateChildren) {

		// get the EntityEnum for the specified entity type
		EntityEnum entityEnum = EntityEnum.getEntityEnum(entityType);
		
		// if the entity type is a group, use the group service's findGroup method
		// to locate it
		if(entityEnum.isGroup()) {
			// attempt to find the entity
			IEntityGroup entity = GroupService.findGroup(entityId);
			if(entity == null) {
				return null;
			} else {
				JsonEntityBean jsonBean = new JsonEntityBean(entity, entityEnum.toString());
				if (populateChildren) {
					@SuppressWarnings("unchecked")
					Iterator<IGroupMember> members = (Iterator<IGroupMember>) entity.getMembers();
					jsonBean = populateChildren(jsonBean, members);
				}
				return jsonBean;
			}
		} 
		
		// otherwise use the getGroupMember method
		else {
			IGroupMember entity = GroupService.getGroupMember(entityId, entityEnum.getClazz());
			if(entity == null || entity instanceof IEntityGroup) {
				return null;
			}
			JsonEntityBean jsonBean = new JsonEntityBean(entity, entityEnum.toString());
			
			// the group member interface doesn't include the entity name, so
			// we'll need to look that up manually
			jsonBean.setName(lookupEntityName(jsonBean));
			return jsonBean;
		}
		
	}

	/**
	 * <p>Populates the children of the JsonEntityBean.  Creates new
	 * JsonEntityBeans for the known types (person, group, or category), and
	 * adds them as children to the current bean.</p> 
	 * @param jsonBean Entity bean to which the children are added
	 * @param children An Iterator containing IGroupMember elements.  Usually
	 * obtained from entity.getMembers().
	 * @return jsonBean with the children populated
	 */
	private JsonEntityBean populateChildren(JsonEntityBean jsonBean, Iterator<IGroupMember> children) {
		
		while(children.hasNext()) {
			
			IGroupMember member = children.next();
			
			// get the type of this member entity
			String entityType = getEntityType(member);
			EntityEnum entityEnum = EntityEnum.getEntityEnum(entityType);
			
			// construct a new entity bean for this entity
			JsonEntityBean jsonChild;
			if (entityEnum.isGroup()) {
				jsonChild = new JsonEntityBean((IEntityGroup) member, entityEnum.toString());
			} else {
				jsonChild = new JsonEntityBean(member, entityEnum.toString());
			}
			
			
			// if the name hasn't been set yet, look up the entity name
			if (jsonChild.getName() == null) {
				jsonChild.setName(lookupEntityName(jsonChild));
			}
			
			// add the entity bean to the list of children
			jsonBean.addChild(jsonChild);
		}
		
		// mark this entity bean as having had it's child list initialized
		jsonBean.setChildrenInitialized(true);

		return jsonBean;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.jasig.portal.layout.dlm.remoting.IGroupListHelper#getEntityType(org.jasig.portal.groups.IGroupMember)
	 */
	public String getEntityType(IGroupMember entity) {
		
		// first check the possible person entity types
		if(entity.getEntityType().equals(IPerson.class)) {
			if(entity instanceof IEntityGroup) {
				return EntityEnum.GROUP.toString();
			} else {
				return EntityEnum.PERSON.toString();
			}
		} 
		
		// next check the possible channel entity types  
		else if(entity.getEntityType().equals(IChannelDefinition.class)) {
			if (entity instanceof IEntityGroup) {
				return EntityEnum.CATEGORY.toString();
			} else {
				return EntityEnum.CHANNEL.toString();
			}
		} 
		
		// We don't know what this is.  Just give up and return null. 
		else {
			return null;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.jasig.portal.portlets.groupselector.GroupsSelectorHelper#getEntityBeans(java.util.List)
	 */
	public List<JsonEntityBean> getEntityBeans(List<String> params) {
	    // if no parameters have been supplied, just return an empty list
	    if (params == null || params.isEmpty()) {
	        return Collections.<JsonEntityBean>emptyList();
	    }
	    
		List<JsonEntityBean> beans = new ArrayList<JsonEntityBean>();
		for (String param : params) {
			String[] parts = param.split(":");
			JsonEntityBean member = getEntity(parts[0], parts[1], false);
			beans.add(member);
		}
		return beans;
	}

	/**
	 * <p>Convenience method that looks up the name of the given group member.
	 * Used for person types.</p>
	 * @param groupMember Entity to look up
	 * @return groupMember's name or null if there's an error
	 */
	public String lookupEntityName(JsonEntityBean entity) {
		
		EntityEnum entityEnum = EntityEnum.getEntityEnum(entity.getEntityType());
		IEntityNameFinder finder;
		if (entityEnum.isGroup()) {
			finder = EntityNameFinderService.instance()
				.getNameFinder(IEntityGroup.class);
		} else {
			finder = EntityNameFinderService.instance()
				.getNameFinder(entityEnum.getClazz());
		}
		
		try {
			return finder.getName(entity.getId());
		} catch (Exception e) {
			/* An exception here isn't the end of the world.  Just log it
			   and return null. */
			log.warn("Couldn't find name for entity " + entity.getId(), e);
			return null;
		}
	}
	
}
