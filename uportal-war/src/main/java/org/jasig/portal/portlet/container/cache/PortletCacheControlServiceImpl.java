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
/**
 * 
 */
package org.jasig.portal.portlet.container.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import javax.portlet.CacheControl;
import javax.portlet.MimeResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pluto.container.om.portlet.PortletDefinition;
import org.jasig.portal.portlet.container.CacheControlImpl;
import org.jasig.portal.portlet.om.IPortletDefinitionId;
import org.jasig.portal.portlet.om.IPortletEntity;
import org.jasig.portal.portlet.om.IPortletEntityId;
import org.jasig.portal.portlet.om.IPortletWindow;
import org.jasig.portal.portlet.om.IPortletWindowId;
import org.jasig.portal.portlet.registry.IPortletDefinitionRegistry;
import org.jasig.portal.portlet.registry.IPortletEntityRegistry;
import org.jasig.portal.portlet.registry.IPortletWindowRegistry;
import org.jasig.portal.utils.web.PortalWebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link IPortletCacheControlService}.
 * {@link CacheControl}s are stored in a {@link Map} stored as a {@link HttpServletRequest} attribute.
 * 
 * @author Nicholas Blair
 * @version $Id$
 */
@Service
public class PortletCacheControlServiceImpl implements IPortletCacheControlService {

	protected static final String REQUEST_ATTRIBUTE__PORTLET_CACHE_CONTROL_MAP = PortletCacheControlServiceImpl.class.getName() + ".PORTLET_CACHE_CONTROL_MAP";
	private final Log log = LogFactory.getLog(this.getClass());
	private IPortletWindowRegistry portletWindowRegistry;
	private IPortletEntityRegistry portletEntityRegistry;
	private IPortletDefinitionRegistry portletDefinitionRegistry;
	
	// key=sessionId+windowId+entityId+definitionId+renderParameters; value=CachedPortletData
    private Ehcache privateScopePortletRenderOutputCache;
    // key=definitionId+renderParams+publicRenderParam; value=CachedPortletData
    private Ehcache publicScopePortletRenderOutputCache;
    
    // key=sessionId+windowId+entityId+definitionId+renderParameters; value=CachedPortletData
    private Ehcache privateScopePortletResourceOutputCache;
    // key=definitionId+renderParams+publicRenderParams; value=CachedPortletData
    private Ehcache publicScopePortletResourceOutputCache;
    
    // default to 100 KB
    private int cacheSizeThreshold = 102400;
    /**
	 * @param privateScopePortletRenderOutputCache the privateScopePortletRenderOutputCache to set
	 */
    @Autowired
    @Qualifier("org.jasig.portal.portlet.container.cache.PortletCacheControlServiceImpl.privateScopePortletRenderOutputCache")
	public void setPrivateScopePortletRenderOutputCache(Ehcache privateScopePortletRenderOutputCache) {
		this.privateScopePortletRenderOutputCache = privateScopePortletRenderOutputCache;
	}
	/**
	 * @param publicScopePortletRenderOutputCache the publicScopePortletRenderOutputCache to set
	 */
    @Autowired
    @Qualifier("org.jasig.portal.portlet.container.cache.PortletCacheControlServiceImpl.publicScopePortletRenderOutputCache")
	public void setPublicScopePortletRenderOutputCache(Ehcache publicScopePortletRenderOutputCache) {
		this.publicScopePortletRenderOutputCache = publicScopePortletRenderOutputCache;
	}
    /**
     * @param privateScopePortletResourceOutputCache
     */
    @Autowired
    @Qualifier("org.jasig.portal.portlet.container.cache.PortletCacheControlServiceImpl.privateScopePortletResourceOutputCache")
    public void setPrivateScopePortletResourceOutputCache(
    		Ehcache privateScopePortletResourceOutputCache) {
		this.privateScopePortletResourceOutputCache = privateScopePortletResourceOutputCache;
	}
    /**
     * @param publicScopePortletResourceOutputCache
     */
    @Autowired
    @Qualifier("org.jasig.portal.portlet.container.cache.PortletCacheControlServiceImpl.publicScopePortletResourceOutputCache")
	public void setPublicScopePortletResourceOutputCache(
			Ehcache publicScopePortletResourceOutputCache) {
		this.publicScopePortletResourceOutputCache = publicScopePortletResourceOutputCache;
	}
	/**
	 * @param cacheSizeThreshold the cacheSizeThreshold to set
	 */
	public void setCacheSizeThreshold(int cacheSizeThreshold) {
		this.cacheSizeThreshold = cacheSizeThreshold;
	}
	/*
	 * (non-Javadoc)
	 * @see org.jasig.portal.portlet.container.cache.IPortletCacheControlService#getCacheSizeThreshold()
	 */
	@Override
	public int getCacheSizeThreshold() {
		return cacheSizeThreshold;
	}
	/**
	 * @param portletWindowRegistry
	 */
	@Autowired
	public void setPortletWindowRegistry(
			IPortletWindowRegistry portletWindowRegistry) {
		this.portletWindowRegistry = portletWindowRegistry;
	}
	/**
	 * @param portletEntityRegistry
	 */
	@Autowired
	public void setPortletEntityRegistry(
			IPortletEntityRegistry portletEntityRegistry) {
		this.portletEntityRegistry = portletEntityRegistry;
	}
	/**
	 * @param portletDefinitionRegistry
	 */
	@Autowired
	public void setPortletDefinitionRegistry(
			IPortletDefinitionRegistry portletDefinitionRegistry) {
		this.portletDefinitionRegistry = portletDefinitionRegistry;
	}

	/* (non-Javadoc)
	 * @see org.jasig.portal.portlet.container.services.IPortletCacheService#getPortletCacheControl(org.jasig.portal.portlet.om.IPortletWindowId)
	 */
	@Override
	public CacheControl getPortletRenderCacheControl(IPortletWindowId portletWindowId, HttpServletRequest httpRequest) {
		Map<IPortletWindowId, CacheControl> map = PortalWebUtils.getMapRequestAttribute(httpRequest, REQUEST_ATTRIBUTE__PORTLET_CACHE_CONTROL_MAP);
		CacheControl cacheControl = map.get(portletWindowId);
		if(cacheControl == null) {
			cacheControl = new CacheControlImpl();
			final IPortletWindow portletWindow = this.portletWindowRegistry.getPortletWindow(httpRequest, portletWindowId);
	        if(portletWindow == null) {
	        	log.warn("portletWindowRegistry returned null portletWindow for " + portletWindowId + ", returning default cacheControl");
	        	return cacheControl;
	        }
	        final IPortletEntityId entityId = portletWindow.getPortletEntityId();
	        final IPortletEntity entity = this.portletEntityRegistry.getPortletEntity(httpRequest, entityId);
	        final IPortletDefinitionId definitionId = entity.getPortletDefinitionId();	
			
	        PortletDefinition portletDefinition = this.portletDefinitionRegistry.getParentPortletDescriptor(definitionId);
	        final String cacheScopeValue = portletDefinition.getCacheScope();
	        if(MimeResponse.PUBLIC_SCOPE.equalsIgnoreCase(cacheScopeValue)) {
	        	cacheControl.setPublicScope(true);
	        }
	        cacheControl.setExpirationTime(portletDefinition.getExpirationCache());
	        
	        // check for CachedPortletData to see if there is an etag to set
	        CachedPortletData cachedData = getCachedPortletRenderOutput(portletWindowId, httpRequest);
	        if(cachedData != null) {
	        	cacheControl.setETag(cachedData.getEtag());
	        }
			map.put(portletWindowId, cacheControl);
		}
		return cacheControl;
	}
	/*
	 * (non-Javadoc)
	 * @see org.jasig.portal.portlet.container.cache.IPortletCacheControlService#getPortletResourceCacheControl(org.jasig.portal.portlet.om.IPortletWindowId, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public CacheControl getPortletResourceCacheControl(
			IPortletWindowId portletWindowId, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		Map<IPortletWindowId, CacheControl> map = PortalWebUtils.getMapRequestAttribute(httpRequest, REQUEST_ATTRIBUTE__PORTLET_CACHE_CONTROL_MAP);
		CacheControl cacheControl = map.get(portletWindowId);
		if(cacheControl == null) {
			cacheControl = new CacheControlImpl(httpResponse);
			final IPortletWindow portletWindow = this.portletWindowRegistry.getPortletWindow(httpRequest, portletWindowId);
	        if(portletWindow == null) {
	        	log.warn("portletWindowRegistry returned null portletWindow for " + portletWindowId + ", returning default cacheControl");
	        	return cacheControl;
	        }
	        final IPortletEntityId entityId = portletWindow.getPortletEntityId();
	        final IPortletEntity entity = this.portletEntityRegistry.getPortletEntity(httpRequest, entityId);
	        final IPortletDefinitionId definitionId = entity.getPortletDefinitionId();	
			
	        PortletDefinition portletDefinition = this.portletDefinitionRegistry.getParentPortletDescriptor(definitionId);
	        final String cacheScopeValue = portletDefinition.getCacheScope();
	        if(MimeResponse.PUBLIC_SCOPE.equalsIgnoreCase(cacheScopeValue)) {
	        	cacheControl.setPublicScope(true);
	        }
	        cacheControl.setExpirationTime(portletDefinition.getExpirationCache());
	        
	        // check for CachedPortletData to see if there is an etag to set
	        CachedPortletData cachedData = getCachedPortletResourceOutput(portletWindowId, httpRequest);
	        if(cachedData != null) {
	        	cacheControl.setETag(cachedData.getEtag());
	        }
			map.put(portletWindowId, cacheControl);
		}
		return cacheControl;
	}
	/*
	 * 
	 * (non-Javadoc)
	 * @see org.jasig.portal.portlet.container.cache.IPortletCacheControlService#getCachedPortletOutput(org.jasig.portal.portlet.om.IPortletWindowId, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public CachedPortletData getCachedPortletRenderOutput(
			IPortletWindowId portletWindowId, HttpServletRequest httpRequest) {
		final IPortletWindow portletWindow = this.portletWindowRegistry.getPortletWindow(httpRequest, portletWindowId);
        
        final IPortletEntityId entityId = portletWindow.getPortletEntityId();
        final IPortletEntity entity = this.portletEntityRegistry.getPortletEntity(httpRequest, entityId);
        final IPortletDefinitionId definitionId = entity.getPortletDefinitionId();	
		
		Serializable publicCacheKey = generatePublicScopePortletDataCacheKey(definitionId, portletWindow.getRenderParameters(), portletWindow.getPublicRenderParameters());
		Element publicCacheElement = this.publicScopePortletRenderOutputCache.get(publicCacheKey);
		if(publicCacheElement != null) {
			if(publicCacheElement.isExpired()) {
				this.publicScopePortletRenderOutputCache.remove(publicCacheKey);
				return null;
			} else {
				return (CachedPortletData) publicCacheElement.getValue();
			}
		} else {
			// public cache contained no content, check private
			Serializable privateCacheKey = generatePrivateScopePortletDataCacheKey(httpRequest, portletWindowId, entityId, definitionId, portletWindow.getRenderParameters());
			Element privateCacheElement = this.privateScopePortletRenderOutputCache.get(privateCacheKey);
			if(privateCacheElement != null) {
				if(privateCacheElement.isExpired()) {
					this.privateScopePortletRenderOutputCache.remove(privateCacheKey);
					return null;
				} else {
					return (CachedPortletData) privateCacheElement.getValue();
				}
			}
		}
		
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.jasig.portal.portlet.container.cache.IPortletCacheControlService#getCachedPortletResourceOutput(org.jasig.portal.portlet.om.IPortletWindowId, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public CachedPortletData getCachedPortletResourceOutput(
			IPortletWindowId portletWindowId, HttpServletRequest httpRequest) {
		final IPortletWindow portletWindow = this.portletWindowRegistry.getPortletWindow(httpRequest, portletWindowId);
        
        final IPortletEntityId entityId = portletWindow.getPortletEntityId();
        final IPortletEntity entity = this.portletEntityRegistry.getPortletEntity(httpRequest, entityId);
        final IPortletDefinitionId definitionId = entity.getPortletDefinitionId();	
		
		Serializable publicCacheKey = generatePublicScopePortletDataCacheKey(definitionId, portletWindow.getRenderParameters(), portletWindow.getPublicRenderParameters());
		Element publicCacheElement = this.publicScopePortletResourceOutputCache.get(publicCacheKey);
		if(publicCacheElement != null) {
			if(publicCacheElement.isExpired()) {
				this.publicScopePortletResourceOutputCache.remove(publicCacheKey);
				return null;
			} else {
				return (CachedPortletData) publicCacheElement.getValue();
			}
		} else {
			// public cache contained no content, check private
			Serializable privateCacheKey = generatePrivateScopePortletDataCacheKey(httpRequest, portletWindowId, entityId, definitionId, portletWindow.getRenderParameters());
			Element privateCacheElement = this.privateScopePortletResourceOutputCache.get(privateCacheKey);
			if(privateCacheElement != null) {
				if(privateCacheElement.isExpired()) {
					this.privateScopePortletResourceOutputCache.remove(privateCacheKey);
					return null;
				} else {
					return (CachedPortletData) privateCacheElement.getValue();
				}
			}
		}
		
		return null;
	}
	/*
	 * (non-Javadoc)
	 * @see org.jasig.portal.portlet.container.cache.IPortletCacheControlService#shouldOutputBeCached(org.jasig.portal.portlet.om.IPortletWindowId, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public boolean shouldOutputBeCached(CacheControl cacheControl) {
		if(cacheControl.getExpirationTime() != 0) {
			return true;
		} else {
			return false;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.jasig.portal.portlet.container.cache.IPortletCacheControlService#cachePortletRenderOutput(org.jasig.portal.portlet.om.IPortletWindowId, javax.servlet.http.HttpServletRequest, java.lang.String, javax.portlet.CacheControl)
	 */
	@Override
	public void cachePortletRenderOutput(IPortletWindowId portletWindowId,
			HttpServletRequest httpRequest, String content,
			CacheControl cacheControl) {
		final IPortletWindow portletWindow = this.portletWindowRegistry.getPortletWindow(httpRequest, portletWindowId);
        
        final IPortletEntityId entityId = portletWindow.getPortletEntityId();
        final IPortletEntity entity = this.portletEntityRegistry.getPortletEntity(httpRequest, entityId);
        final IPortletDefinitionId definitionId = entity.getPortletDefinitionId();	
		
        final int expirationTime = cacheControl.getExpirationTime();
		CachedPortletData newData = new CachedPortletData();
		newData.setExpirationTimeSeconds(expirationTime);
		newData.setTimeStored(new Date());
		newData.setStringData(content);
		newData.setEtag(cacheControl.getETag());
		
		if(cacheControl.isPublicScope()) {
			Serializable publicCacheKey = generatePublicScopePortletDataCacheKey(definitionId, portletWindow.getRenderParameters(), portletWindow.getPublicRenderParameters());
			int ttl = findMinimumCacheTTL(publicScopePortletRenderOutputCache.getCacheConfiguration(), cacheControl);
			Element publicCacheElement = constructCacheElement(publicCacheKey, newData, ttl);
			this.publicScopePortletRenderOutputCache.put(publicCacheElement);		
		} else {
			Serializable privateCacheKey = generatePrivateScopePortletDataCacheKey(httpRequest, portletWindowId, entityId, definitionId, portletWindow.getRenderParameters());
			int ttl = findMinimumCacheTTL(privateScopePortletRenderOutputCache.getCacheConfiguration(), cacheControl);
			Element privateCacheElement = constructCacheElement(privateCacheKey, newData, ttl);
			this.privateScopePortletRenderOutputCache.put(privateCacheElement);
		}
	}
	/*
	 * (non-Javadoc)
	 * @see org.jasig.portal.portlet.container.cache.IPortletCacheControlService#cachePortletResourceOutput(org.jasig.portal.portlet.om.IPortletWindowId, javax.servlet.http.HttpServletRequest, byte[], java.lang.String, java.util.Map, javax.portlet.CacheControl)
	 */
	@Override
	public void cachePortletResourceOutput(IPortletWindowId portletWindowId,
			HttpServletRequest httpRequest, byte[] content, String contentType, Map<String, String[]> headers, CacheControl cacheControl) {
		final IPortletWindow portletWindow = this.portletWindowRegistry.getPortletWindow(httpRequest, portletWindowId);
        
        final IPortletEntityId entityId = portletWindow.getPortletEntityId();
        final IPortletEntity entity = this.portletEntityRegistry.getPortletEntity(httpRequest, entityId);
        final IPortletDefinitionId definitionId = entity.getPortletDefinitionId();	
		
        final int expirationTime = cacheControl.getExpirationTime();
		CachedPortletData newData = new CachedPortletData();
		newData.setEtag(cacheControl.getETag());
		newData.setByteData(content);
		newData.setExpirationTimeSeconds(expirationTime);
		newData.setTimeStored(new Date());
		newData.setContentType(contentType);
		newData.setHeaders(headers);
		
		
		if(cacheControl.isPublicScope()) {
			Serializable publicCacheKey = generatePublicScopePortletDataCacheKey(definitionId, portletWindow.getRenderParameters(), portletWindow.getPublicRenderParameters());
			int ttl = findMinimumCacheTTL(publicScopePortletResourceOutputCache.getCacheConfiguration(), cacheControl);
			Element publicCacheElement = constructCacheElement(publicCacheKey, newData, ttl);
			this.publicScopePortletResourceOutputCache.put(publicCacheElement);		
		} else {
			Serializable privateCacheKey = generatePrivateScopePortletDataCacheKey(httpRequest, portletWindowId, entityId, definitionId, portletWindow.getRenderParameters());
			int ttl = findMinimumCacheTTL(privateScopePortletResourceOutputCache.getCacheConfiguration(), cacheControl);
			Element privateCacheElement = constructCacheElement(privateCacheKey, newData, ttl);
			this.privateScopePortletResourceOutputCache.put(privateCacheElement);
		}
	}
	
	/**
	 * @param cacheConfig
	 * @param cacheControl
	 * @return the minimum value between the cache config and the expiration time set by the portlet
	 */
	protected int findMinimumCacheTTL(CacheConfiguration cacheConfig, CacheControl cacheControl) {
		Integer cacheControlTTL = cacheControl.getExpirationTime();
		Long cacheConfigTTL = cacheConfig.getTimeToLiveSeconds();
		if(cacheControlTTL <= 0) {
			return cacheConfigTTL.intValue();
		}
		Long result = Math.min(cacheConfigTTL, cacheControlTTL.longValue());
		
		return result.intValue();
	}
	/**
	 * Construct a cache {@link Element} from the key, data, and time to live.
	 * 
	 * @param cacheKey
	 * @param data
	 * @param timeToLive
	 * @return an appropriate {@link Element}, never null
	 */
	protected Element constructCacheElement(Serializable cacheKey, CachedPortletData data, int timeToLive) {
		if(timeToLive <= 0) {
			return new Element(cacheKey, data);
		}
		return new Element(cacheKey, data, null, null, timeToLive);
	}
	/*
	 * (non-Javadoc)
	 * @see org.jasig.portal.portlet.container.cache.IPortletCacheControlService#purgeCachedPortletData(org.jasig.portal.portlet.om.IPortletWindowId, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public boolean purgeCachedPortletData(IPortletWindowId portletWindowId,
			HttpServletRequest httpRequest, CacheControl cacheControl) {
		
		final IPortletWindow portletWindow = this.portletWindowRegistry.getPortletWindow(httpRequest, portletWindowId);
        
        final IPortletEntityId entityId = portletWindow.getPortletEntityId();
        final IPortletEntity entity = this.portletEntityRegistry.getPortletEntity(httpRequest, entityId);
        final IPortletDefinitionId definitionId = entity.getPortletDefinitionId();	
		if(cacheControl.isPublicScope()) {
			Serializable publicCacheKey = generatePublicScopePortletDataCacheKey(definitionId, portletWindow.getRenderParameters(), portletWindow.getPublicRenderParameters());
			boolean renderPurged = this.publicScopePortletRenderOutputCache.remove(publicCacheKey);
			return this.publicScopePortletResourceOutputCache.remove(publicCacheKey) || renderPurged;
		} else {
			Serializable privateCacheKey = generatePrivateScopePortletDataCacheKey(httpRequest, portletWindowId, entityId, definitionId, portletWindow.getRenderParameters());
			boolean renderPurged = this.privateScopePortletRenderOutputCache.remove(privateCacheKey);
			return this.privateScopePortletResourceOutputCache.remove(privateCacheKey) || renderPurged;
		}
	}
	/**
     * Generate a cache key for the public scope cache.
     *
     * definitionId + renderParams + publicRenderParams
     * 
     * Internally uses {@link ArrayList} as it implements {@link Serializable} and an appropriate equals/hashCode.
     * 
     * @param portletDefinitionId
     * @param renderParameters
     * @param publicRenderParameters
     * @return
     */
    protected Serializable generatePublicScopePortletDataCacheKey(IPortletDefinitionId portletDefinitionId, Map<String,String[]> renderParameters, Map<String,String[]> publicRenderParameters) {
    	ArrayList<Object> key = new ArrayList<Object>();
    	key.add(portletDefinitionId);
    	key.add(renderParameters);
    	key.add(publicRenderParameters);
    	return key;
    }
    /**
     * Generate a cache key for the private scope Cache.
     * 
     * sessionId + windowId + entityId + definitionId + renderParameters
     * 
     * Internally uses {@link ArrayList} as it implements {@link Serializable} and an appropriate equals/hashCode.
     * 
     * @param request
     * @param windowId
     * @param entityId
     * @param definitionId
     * @param renderParameters
     * @return
     */
    protected Serializable generatePrivateScopePortletDataCacheKey(HttpServletRequest request, IPortletWindowId windowId, IPortletEntityId entityId, IPortletDefinitionId definitionId, Map<String,String[]> renderParameters) {
    	ArrayList<Object> key = new ArrayList<Object>();
    	final String sessionId = request.getSession().getId();
    	key.add(sessionId);
    	key.add(windowId);
    	key.add(entityId);
    	key.add(definitionId);
    	key.add(renderParameters);
    	return key;
    }
	
}
