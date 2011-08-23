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
package org.jasig.portal.portlet.rendering;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.portlet.CacheControl;
import javax.portlet.PortletException;
import javax.servlet.http.HttpServletRequest;

import junit.framework.Assert;

import org.apache.commons.lang.time.DateUtils;
import org.apache.pluto.container.PortletContainer;
import org.apache.pluto.container.PortletContainerException;
import org.apache.pluto.container.PortletWindow;
import org.jasig.portal.api.portlet.PortletDelegationLocator;
import org.jasig.portal.portlet.container.CacheControlImpl;
import org.jasig.portal.portlet.container.cache.CachedPortletData;
import org.jasig.portal.portlet.container.cache.IPortletCacheControlService;
import org.jasig.portal.portlet.om.IPortletWindow;
import org.jasig.portal.portlet.om.IPortletWindowId;
import org.jasig.portal.portlet.registry.IPortletWindowRegistry;
import org.jasig.portal.utils.web.PortletHttpServletRequestWrapper;
import org.jasig.portal.utils.web.PortletHttpServletResponseWrapper;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests for {@link PortletRendererImpl}.
 * 
 * @author Nicholas Blair
 * @version $Id$
 */
public class PortletRendererImplTest {

	/**
	 * {@link CacheControl} says don't cache, make sure no caching.
	 * 
	 * @throws PortletException
	 * @throws IOException
	 * @throws PortletContainerException
	 */
	@Test
	public void doRenderMarkupNoCacheControl() throws PortletException, IOException, PortletContainerException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		CacheControlImpl cacheControl = new CacheControlImpl();
		cacheControl.setUseCachedContent(false);
		cacheControl.setExpirationTime(0);
		
		PortletRendererImpl portletRenderer = new PortletRendererImpl();
		IPortletWindowId portletWindowId = mock(IPortletWindowId.class);
		IPortletWindow portletWindow = mock(IPortletWindow.class);
		
		IPortletCacheControlService portletCacheControlService = mock(IPortletCacheControlService.class);
		portletRenderer.setPortletCacheControlService(portletCacheControlService);
		IPortletWindowRegistry portletWindowRegistry = mock(IPortletWindowRegistry.class);
		portletRenderer.setPortletWindowRegistry(portletWindowRegistry);
		PortletContainer portletContainer = mock(PortletContainer.class);
		portletRenderer.setPortletContainer(portletContainer);
		PortletDelegationLocator portletDelegationLocator = mock(PortletDelegationLocator.class);
		portletRenderer.setPortletDelegationLocator(portletDelegationLocator);
		
		PortletWindow plutoPortletWindow = mock(PortletWindow.class);
		when(portletWindow.getPlutoPortletWindow()).thenReturn(plutoPortletWindow);
		when(portletCacheControlService.getPortletRenderCacheControl(portletWindowId, request)).thenReturn(cacheControl);
		when(portletCacheControlService.getCachedPortletRenderOutput(portletWindowId, request)).thenReturn(null);
		when(portletCacheControlService.shouldOutputBeCached(cacheControl)).thenReturn(false);
		when(portletWindowRegistry.getPortletWindow(request, portletWindowId)).thenReturn(portletWindow);
		
		StringWriter writer = new StringWriter();
		portletRenderer.doRenderMarkup(portletWindowId, request, response, writer);
		
		// call 2 times
		portletRenderer.doRenderMarkup(portletWindowId, request, response, writer);
		
		verify(portletContainer, times(2)).doRender(isA(PortletWindow.class), isA(PortletHttpServletRequestWrapper.class), isA(PortletHttpServletResponseWrapper.class));
		verify(portletCacheControlService, never()).cachePortletRenderOutput(isA(IPortletWindowId.class), isA(HttpServletRequest.class), isA(String.class), isA(CacheControl.class));

	}
	
	/**
	 * No cached data exists, but mock a {@link CacheControl} that will trigger the portletContainer#doRender, 
	 * capture the output, and give to the portlet cachecontrol service.
	 * 
	 * @throws PortletException
	 * @throws IOException
	 * @throws PortletContainerException
	 */
	@Test
	public void doRenderMarkupCapture() throws PortletException, IOException, PortletContainerException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		CacheControlImpl cacheControl = new CacheControlImpl();
		cacheControl.setUseCachedContent(false);
		cacheControl.setExpirationTime(300);
		
		PortletRendererImpl portletRenderer = new PortletRendererImpl();
		IPortletWindowId portletWindowId = mock(IPortletWindowId.class);
		IPortletWindow portletWindow = mock(IPortletWindow.class);
		
		IPortletCacheControlService portletCacheControlService = mock(IPortletCacheControlService.class);
		portletRenderer.setPortletCacheControlService(portletCacheControlService);
		IPortletWindowRegistry portletWindowRegistry = mock(IPortletWindowRegistry.class);
		portletRenderer.setPortletWindowRegistry(portletWindowRegistry);
		PortletContainer portletContainer = mock(PortletContainer.class);
		portletRenderer.setPortletContainer(portletContainer);
		PortletDelegationLocator portletDelegationLocator = mock(PortletDelegationLocator.class);
		portletRenderer.setPortletDelegationLocator(portletDelegationLocator);
		
		PortletWindow plutoPortletWindow = mock(PortletWindow.class);
		when(portletWindow.getPlutoPortletWindow()).thenReturn(plutoPortletWindow);
		when(portletCacheControlService.getPortletRenderCacheControl(portletWindowId, request)).thenReturn(cacheControl);
		when(portletCacheControlService.getCachedPortletRenderOutput(portletWindowId, request)).thenReturn(null);
		when(portletCacheControlService.shouldOutputBeCached(cacheControl)).thenReturn(true);
		when(portletWindowRegistry.getPortletWindow(request, portletWindowId)).thenReturn(portletWindow);
		
		StringWriter writer = new StringWriter();
		portletRenderer.doRenderMarkup(portletWindowId, request, response, writer);
		
		verify(portletContainer, times(1)).doRender(isA(PortletWindow.class), isA(PortletHttpServletRequestWrapper.class), isA(PortletHttpServletResponseWrapper.class));
		
		verify(portletCacheControlService, times(1)).cachePortletRenderOutput(isA(IPortletWindowId.class), isA(HttpServletRequest.class), isA(String.class), isA(CacheControl.class));
	}
	
	/**
	 * No cached data exists, but mock a {@link CacheControl} with a negative value for expirationtime.
	 * Will trigger the portletContainer#doRender, 
	 * capture the output, and give to the portlet cachecontrol service.
	 * 
	 * negative value for cacheControl expiration time means "cache forever."
	 * 
	 * @throws PortletException
	 * @throws IOException
	 * @throws PortletContainerException
	 */
	@Test
	public void doRenderMarkupCaptureNegativeExpirationTime() throws PortletException, IOException, PortletContainerException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		CacheControlImpl cacheControl = new CacheControlImpl();
		cacheControl.setUseCachedContent(false);
		cacheControl.setExpirationTime(-1);
		
		PortletRendererImpl portletRenderer = new PortletRendererImpl();
		IPortletWindowId portletWindowId = mock(IPortletWindowId.class);
		IPortletWindow portletWindow = mock(IPortletWindow.class);
		
		IPortletCacheControlService portletCacheControlService = mock(IPortletCacheControlService.class);
		portletRenderer.setPortletCacheControlService(portletCacheControlService);
		IPortletWindowRegistry portletWindowRegistry = mock(IPortletWindowRegistry.class);
		portletRenderer.setPortletWindowRegistry(portletWindowRegistry);
		PortletContainer portletContainer = mock(PortletContainer.class);
		portletRenderer.setPortletContainer(portletContainer);
		PortletDelegationLocator portletDelegationLocator = mock(PortletDelegationLocator.class);
		portletRenderer.setPortletDelegationLocator(portletDelegationLocator);
		
		PortletWindow plutoPortletWindow = mock(PortletWindow.class);
		when(portletWindow.getPlutoPortletWindow()).thenReturn(plutoPortletWindow);
		when(portletCacheControlService.getPortletRenderCacheControl(portletWindowId, request)).thenReturn(cacheControl);
		when(portletCacheControlService.getCachedPortletRenderOutput(portletWindowId, request)).thenReturn(null);
		when(portletCacheControlService.shouldOutputBeCached(cacheControl)).thenReturn(true);
		when(portletWindowRegistry.getPortletWindow(request, portletWindowId)).thenReturn(portletWindow);
		
		StringWriter writer = new StringWriter();
		portletRenderer.doRenderMarkup(portletWindowId, request, response, writer);
		
		verify(portletContainer, times(1)).doRender(isA(PortletWindow.class), isA(PortletHttpServletRequestWrapper.class), isA(PortletHttpServletResponseWrapper.class));
		
		verify(portletCacheControlService, times(1)).cachePortletRenderOutput(isA(IPortletWindowId.class), isA(HttpServletRequest.class), isA(String.class), isA(CacheControl.class));
	}
	
	/**
	 * Mimic workflow when cached portlet data using "expiration" method is available.
	 * 
	 * @throws PortletContainerException 
	 * @throws IOException 
	 * @throws PortletException 
	 */
	@Test
	public void doRenderMarkupCachedContentExpirationMethodTest() throws PortletException, IOException, PortletContainerException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		Date now = new Date();
		CacheControlImpl cacheControl = new CacheControlImpl();
		cacheControl.setUseCachedContent(true);
		cacheControl.setExpirationTime(300);
		CachedPortletData cachedPortletData = new CachedPortletData();
		cachedPortletData.setStringData("<p>Some content</p>");
		cachedPortletData.setExpirationTimeSeconds(cacheControl.getExpirationTime());
		cachedPortletData.setTimeStored(now);
		
		PortletRendererImpl portletRenderer = new PortletRendererImpl();
		IPortletWindowId portletWindowId = mock(IPortletWindowId.class);
		IPortletWindow portletWindow = mock(IPortletWindow.class);
		IPortletCacheControlService portletCacheControlService = mock(IPortletCacheControlService.class);
		portletRenderer.setPortletCacheControlService(portletCacheControlService);
		IPortletWindowRegistry portletWindowRegistry = mock(IPortletWindowRegistry.class);
		portletRenderer.setPortletWindowRegistry(portletWindowRegistry);
		PortletDelegationLocator portletDelegationLocator = mock(PortletDelegationLocator.class);
		portletRenderer.setPortletDelegationLocator(portletDelegationLocator);
		PortletContainer portletContainer = mock(PortletContainer.class);
		portletRenderer.setPortletContainer(portletContainer);
		
		PortletWindow plutoPortletWindow = mock(PortletWindow.class);
		when(portletWindow.getPlutoPortletWindow()).thenReturn(plutoPortletWindow);
		when(portletCacheControlService.getPortletRenderCacheControl(portletWindowId, request)).thenReturn(cacheControl);
		when(portletCacheControlService.getCachedPortletRenderOutput(portletWindowId, request)).thenReturn(cachedPortletData);
		when(portletWindowRegistry.getPortletWindow(request, portletWindowId)).thenReturn(portletWindow);
		
		StringWriter writer = new StringWriter();
		portletRenderer.doRenderMarkup(portletWindowId, request, response, writer);
		Assert.assertEquals("<p>Some content</p>", writer.toString());
		
		// verify we enter the first branch and never execute portletContainer#doRender
		verify(portletContainer, never()).doRender(isA(PortletWindow.class), isA(PortletHttpServletRequestWrapper.class), isA(PortletHttpServletResponseWrapper.class));
		// verify we never enter the other branch of the "should render cached output" if statement
		verify(portletCacheControlService, never()).shouldOutputBeCached(cacheControl);
	}
	/**
	 * Mimic workflow when data cached portlet data using "validation" method is available.
	 * 
	 * @throws PortletContainerException 
	 * @throws IOException 
	 * @throws PortletException 
	 */
	@Test
	public void doRenderMarkupCachedContentValidationNotExpiredMethodTest() throws PortletException, IOException, PortletContainerException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		Date now = new Date();
		CacheControlImpl cacheControl = new CacheControlImpl();
		cacheControl.setUseCachedContent(true);
		cacheControl.setETag("123456");
		cacheControl.setExpirationTime(300);
		CachedPortletData cachedPortletData = new CachedPortletData();
		cachedPortletData.setStringData("<p>Some content</p>");
		cachedPortletData.setEtag("123456");
		cachedPortletData.setExpirationTimeSeconds(cacheControl.getExpirationTime());
		cachedPortletData.setTimeStored(now);
		
		PortletRendererImpl portletRenderer = new PortletRendererImpl();
		IPortletWindowId portletWindowId = mock(IPortletWindowId.class);
		IPortletWindow portletWindow = mock(IPortletWindow.class);
		IPortletCacheControlService portletCacheControlService = mock(IPortletCacheControlService.class);
		portletRenderer.setPortletCacheControlService(portletCacheControlService);
		IPortletWindowRegistry portletWindowRegistry = mock(IPortletWindowRegistry.class);
		portletRenderer.setPortletWindowRegistry(portletWindowRegistry);
		PortletDelegationLocator portletDelegationLocator = mock(PortletDelegationLocator.class);
		portletRenderer.setPortletDelegationLocator(portletDelegationLocator);
		PortletContainer portletContainer = mock(PortletContainer.class);
		portletRenderer.setPortletContainer(portletContainer);
		
		PortletWindow plutoPortletWindow = mock(PortletWindow.class);
		when(portletWindow.getPlutoPortletWindow()).thenReturn(plutoPortletWindow);
		when(portletCacheControlService.getPortletRenderCacheControl(portletWindowId, request)).thenReturn(cacheControl);
		when(portletCacheControlService.getCachedPortletRenderOutput(portletWindowId, request)).thenReturn(cachedPortletData);
		when(portletWindowRegistry.getPortletWindow(request, portletWindowId)).thenReturn(portletWindow);
		
		StringWriter writer = new StringWriter();
		portletRenderer.doRenderMarkup(portletWindowId, request, response, writer);
		Assert.assertEquals("<p>Some content</p>", writer.toString());
		
		// verify we enter the first branch and never execute portletContainer#doRender
		verify(portletContainer, never()).doRender(isA(PortletWindow.class), isA(PortletHttpServletRequestWrapper.class), isA(PortletHttpServletResponseWrapper.class));
		// verify we never enter the other branch of the "should render cached output" if statement
		verify(portletCacheControlService, never()).shouldOutputBeCached(cacheControl);
	}
	
	/**
	 * Mimic workflow when data cached portlet data using "validation" method is available.
	 * 
	 * @throws PortletContainerException 
	 * @throws IOException 
	 * @throws PortletException 
	 */
	@Test
	public void doRenderMarkupCachedContentValidationMethodExpiredTest() throws PortletException, IOException, PortletContainerException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		Date now = new Date();
		CacheControlImpl cacheControl = new CacheControlImpl();
		// by setting useCachedContent to true, we are saying even though content is expired, replay it anyways (since etag is still valid)
		cacheControl.setUseCachedContent(true);
		cacheControl.setETag("123456");
		cacheControl.setExpirationTime(300);
		CachedPortletData cachedPortletData = new CachedPortletData();
		cachedPortletData.setStringData("<p>Some content</p>");
		cachedPortletData.setEtag("123456");
		cachedPortletData.setExpirationTimeSeconds(cacheControl.getExpirationTime());
		// set the time stored to be a time that will trigger the content to be expired
		Date expiredTime = DateUtils.addSeconds(now, -301); 
		cachedPortletData.setTimeStored(expiredTime);
		
		PortletRendererImpl portletRenderer = new PortletRendererImpl();
		IPortletWindowId portletWindowId = mock(IPortletWindowId.class);
		IPortletWindow portletWindow = mock(IPortletWindow.class);
		IPortletCacheControlService portletCacheControlService = mock(IPortletCacheControlService.class);
		portletRenderer.setPortletCacheControlService(portletCacheControlService);
		IPortletWindowRegistry portletWindowRegistry = mock(IPortletWindowRegistry.class);
		portletRenderer.setPortletWindowRegistry(portletWindowRegistry);
		PortletDelegationLocator portletDelegationLocator = mock(PortletDelegationLocator.class);
		portletRenderer.setPortletDelegationLocator(portletDelegationLocator);
		PortletContainer portletContainer = mock(PortletContainer.class);
		portletRenderer.setPortletContainer(portletContainer);
		
		PortletWindow plutoPortletWindow = mock(PortletWindow.class);
		when(portletWindow.getPlutoPortletWindow()).thenReturn(plutoPortletWindow);
		when(portletCacheControlService.getPortletRenderCacheControl(portletWindowId, request)).thenReturn(cacheControl);
		when(portletCacheControlService.getCachedPortletRenderOutput(portletWindowId, request)).thenReturn(cachedPortletData);
		when(portletWindowRegistry.getPortletWindow(request, portletWindowId)).thenReturn(portletWindow);
		
		StringWriter writer = new StringWriter();
		portletRenderer.doRenderMarkup(portletWindowId, request, response, writer);
		Assert.assertEquals("<p>Some content</p>", writer.toString());
		
		// context is expired, triggers doRender
		verify(portletContainer, times(1)).doRender(isA(PortletWindow.class), isA(PortletHttpServletRequestWrapper.class), isA(PortletHttpServletResponseWrapper.class));
		// verify we never enter the other branch of the "should render cached output" if statement
		verify(portletCacheControlService, never()).shouldOutputBeCached(isA(CacheControl.class));
	}
	/**
	 * Verify invoking portletRenderer#doAction removes cached content.
	 * 
	 * @throws PortletContainerException 
	 * @throws IOException 
	 * @throws PortletException 
	 */
	@Test
	public void doActionPurgesCachedContent() throws PortletException, IOException, PortletContainerException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		Date now = new Date();
		CacheControlImpl cacheControl = new CacheControlImpl();
		
		cacheControl.setETag("123456");
		cacheControl.setExpirationTime(300);
		CachedPortletData cachedPortletData = new CachedPortletData();
		cachedPortletData.setStringData("<p>Some content</p>");
		cachedPortletData.setEtag("123456");
		cachedPortletData.setExpirationTimeSeconds(cacheControl.getExpirationTime());
		cachedPortletData.setTimeStored(now);
		
		PortletRendererImpl portletRenderer = new PortletRendererImpl();
		IPortletWindowId portletWindowId = mock(IPortletWindowId.class);
		IPortletWindow portletWindow = mock(IPortletWindow.class);
		IPortletCacheControlService portletCacheControlService = mock(IPortletCacheControlService.class);
		portletRenderer.setPortletCacheControlService(portletCacheControlService);
		IPortletWindowRegistry portletWindowRegistry = mock(IPortletWindowRegistry.class);
		portletRenderer.setPortletWindowRegistry(portletWindowRegistry);
		PortletDelegationLocator portletDelegationLocator = mock(PortletDelegationLocator.class);
		portletRenderer.setPortletDelegationLocator(portletDelegationLocator);
		PortletContainer portletContainer = mock(PortletContainer.class);
		
		portletRenderer.setPortletContainer(portletContainer);
		
		PortletWindow plutoPortletWindow = mock(PortletWindow.class);
		when(portletWindow.getPlutoPortletWindow()).thenReturn(plutoPortletWindow);
		when(portletCacheControlService.getPortletRenderCacheControl(portletWindowId, request)).thenReturn(cacheControl);
		when(portletCacheControlService.getCachedPortletRenderOutput(portletWindowId, request)).thenReturn(null);
		when(portletCacheControlService.shouldOutputBeCached(cacheControl)).thenReturn(true);
		when(portletWindowRegistry.getPortletWindow(request, portletWindowId)).thenReturn(portletWindow);
		

		StringWriter writer = new StringWriter();
		
		// doAction will trigger purge
		portletRenderer.doAction(portletWindowId, request, response);
		verify(portletCacheControlService, times(1)).purgeCachedPortletData(portletWindowId, request, cacheControl);
		verify(portletContainer, times(1)).doAction(isA(PortletWindow.class), isA(PortletHttpServletRequestWrapper.class), isA(PortletHttpServletResponseWrapper.class));
		
		portletRenderer.doRenderMarkup(portletWindowId, request, response, writer);
		
		verify(portletContainer, times(1)).doRender(isA(PortletWindow.class), isA(PortletHttpServletRequestWrapper.class), isA(PortletHttpServletResponseWrapper.class));
		// verify we never enter the other branch of the "should render cached output" if statement
		verify(portletCacheControlService, times(1)).cachePortletRenderOutput(isA(IPortletWindowId.class), isA(HttpServletRequest.class), isA(String.class), isA(CacheControl.class));
		
	}
	
	/**
	 * {@link CacheControl} says don't cache, make sure no caching for doServeResource.
	 * 
	 * @throws PortletException
	 * @throws IOException
	 * @throws PortletContainerException
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testDoServeResourceNoCache() throws PortletException, IOException, PortletContainerException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		CacheControlImpl cacheControl = new CacheControlImpl();
		cacheControl.setUseCachedContent(false);
		cacheControl.setExpirationTime(0);
		
		PortletRendererImpl portletRenderer = new PortletRendererImpl();
		IPortletWindowId portletWindowId = mock(IPortletWindowId.class);
		IPortletWindow portletWindow = mock(IPortletWindow.class);
		
		IPortletCacheControlService portletCacheControlService = mock(IPortletCacheControlService.class);
		portletRenderer.setPortletCacheControlService(portletCacheControlService);
		IPortletWindowRegistry portletWindowRegistry = mock(IPortletWindowRegistry.class);
		portletRenderer.setPortletWindowRegistry(portletWindowRegistry);
		PortletContainer portletContainer = mock(PortletContainer.class);
		portletRenderer.setPortletContainer(portletContainer);
		PortletDelegationLocator portletDelegationLocator = mock(PortletDelegationLocator.class);
		portletRenderer.setPortletDelegationLocator(portletDelegationLocator);
		
		PortletWindow plutoPortletWindow = mock(PortletWindow.class);
		when(portletWindow.getPlutoPortletWindow()).thenReturn(plutoPortletWindow);
		when(portletCacheControlService.getPortletResourceCacheControl(portletWindowId, request, response)).thenReturn(cacheControl);
		when(portletCacheControlService.getCachedPortletResourceOutput(portletWindowId, request)).thenReturn(null);
		when(portletCacheControlService.shouldOutputBeCached(cacheControl)).thenReturn(false);
		when(portletWindowRegistry.getPortletWindow(request, portletWindowId)).thenReturn(portletWindow);
		
		portletRenderer.doServeResource(portletWindowId, request, response);
		
		// call 2 times
		portletRenderer.doServeResource(portletWindowId, request, response);
		
		verify(portletContainer, times(2)).doServeResource(isA(PortletWindow.class), isA(PortletHttpServletRequestWrapper.class), isA(PortletHttpServletResponseWrapper.class));
		verify(portletCacheControlService, never()).cachePortletResourceOutput(isA(IPortletWindowId.class), isA(HttpServletRequest.class), isA(byte[].class), isA(String.class), isA(Map.class), isA(CacheControl.class));
	}
	
	/**
	 * No cached data exists, but mock a {@link CacheControl} that will trigger the portletContainer#doServeResource, 
	 * capture the output, and give to the portlet cachecontrol service.
	 * 
	 * @throws PortletException
	 * @throws IOException
	 * @throws PortletContainerException
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void doServeResourceCapture() throws PortletException, IOException, PortletContainerException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setContentType("application/octet-stream");
		CacheControlImpl cacheControl = new CacheControlImpl();
		cacheControl.setUseCachedContent(false);
		cacheControl.setExpirationTime(300);
		
		PortletRendererImpl portletRenderer = new PortletRendererImpl();
		IPortletWindowId portletWindowId = mock(IPortletWindowId.class);
		IPortletWindow portletWindow = mock(IPortletWindow.class);
		
		IPortletCacheControlService portletCacheControlService = mock(IPortletCacheControlService.class);
		portletRenderer.setPortletCacheControlService(portletCacheControlService);
		IPortletWindowRegistry portletWindowRegistry = mock(IPortletWindowRegistry.class);
		portletRenderer.setPortletWindowRegistry(portletWindowRegistry);
		PortletContainer portletContainer = mock(PortletContainer.class);
		portletRenderer.setPortletContainer(portletContainer);
		PortletDelegationLocator portletDelegationLocator = mock(PortletDelegationLocator.class);
		portletRenderer.setPortletDelegationLocator(portletDelegationLocator);
		
		PortletWindow plutoPortletWindow = mock(PortletWindow.class);
		when(portletWindow.getPlutoPortletWindow()).thenReturn(plutoPortletWindow);
		when(portletCacheControlService.getPortletResourceCacheControl(portletWindowId, request, response)).thenReturn(cacheControl);
		when(portletCacheControlService.getCachedPortletResourceOutput(portletWindowId, request)).thenReturn(null);
		when(portletCacheControlService.shouldOutputBeCached(cacheControl)).thenReturn(true);
		when(portletWindowRegistry.getPortletWindow(request, portletWindowId)).thenReturn(portletWindow);
		
		portletRenderer.doServeResource(portletWindowId, request, response);
		
		verify(portletContainer, times(1)).doServeResource(isA(PortletWindow.class), isA(PortletHttpServletRequestWrapper.class), isA(PortletHttpServletResponseWrapper.class));
		
		verify(portletCacheControlService, times(1)).cachePortletResourceOutput(isA(IPortletWindowId.class), isA(HttpServletRequest.class), isA(byte[].class), isA(String.class), isA(Map.class), isA(CacheControl.class));
	}
	
	/**
	 * No cached data exists, but mock a {@link CacheControl} with a negative value for expirationtime.
	 * Will trigger the portletContainer#doServeResource, 
	 * capture the output, and give to the portlet cachecontrol service.
	 * 
	 * negative value for cacheControl expiration time means "cache forever."
	 * 
	 * @throws PortletException
	 * @throws IOException
	 * @throws PortletContainerException
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void doServeResourceMarkupCaptureNegativeExpirationTime() throws PortletException, IOException, PortletContainerException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setContentType("application/octet-stream");
		CacheControlImpl cacheControl = new CacheControlImpl();
		cacheControl.setUseCachedContent(false);
		cacheControl.setExpirationTime(-1);
		
		PortletRendererImpl portletRenderer = new PortletRendererImpl();
		IPortletWindowId portletWindowId = mock(IPortletWindowId.class);
		IPortletWindow portletWindow = mock(IPortletWindow.class);
		
		IPortletCacheControlService portletCacheControlService = mock(IPortletCacheControlService.class);
		portletRenderer.setPortletCacheControlService(portletCacheControlService);
		IPortletWindowRegistry portletWindowRegistry = mock(IPortletWindowRegistry.class);
		portletRenderer.setPortletWindowRegistry(portletWindowRegistry);
		PortletContainer portletContainer = mock(PortletContainer.class);
		portletRenderer.setPortletContainer(portletContainer);
		PortletDelegationLocator portletDelegationLocator = mock(PortletDelegationLocator.class);
		portletRenderer.setPortletDelegationLocator(portletDelegationLocator);
		
		PortletWindow plutoPortletWindow = mock(PortletWindow.class);
		when(portletWindow.getPlutoPortletWindow()).thenReturn(plutoPortletWindow);
		when(portletCacheControlService.getPortletResourceCacheControl(portletWindowId, request, response)).thenReturn(cacheControl);
		when(portletCacheControlService.getCachedPortletRenderOutput(portletWindowId, request)).thenReturn(null);
		when(portletCacheControlService.shouldOutputBeCached(cacheControl)).thenReturn(true);
		when(portletWindowRegistry.getPortletWindow(request, portletWindowId)).thenReturn(portletWindow);
		
		portletRenderer.doServeResource(portletWindowId, request, response);
		
		verify(portletContainer, times(1)).doServeResource(isA(PortletWindow.class), isA(PortletHttpServletRequestWrapper.class), isA(PortletHttpServletResponseWrapper.class));
		
		verify(portletCacheControlService, times(1)).cachePortletResourceOutput(isA(IPortletWindowId.class), isA(HttpServletRequest.class), isA(byte[].class), isA(String.class), isA(Map.class), isA(CacheControl.class));
	}
	
	/**
	 * Mimic workflow when cached portlet data using "expiration" method is available.
	 * 
	 * @throws PortletContainerException 
	 * @throws IOException 
	 * @throws PortletException 
	 */
	@Test
	public void doServeResourceCachedContentExpirationMethodTest() throws PortletException, IOException, PortletContainerException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		Date now = new Date();
		CacheControlImpl cacheControl = new CacheControlImpl();
		cacheControl.setUseCachedContent(true);
		cacheControl.setExpirationTime(300);
		CachedPortletData cachedPortletData = new CachedPortletData();
		cachedPortletData.setContentType("application/json");
		byte [] content = "{ \"hello\": \"world\" }".getBytes();
		cachedPortletData.setByteData(content);
		cachedPortletData.setExpirationTimeSeconds(cacheControl.getExpirationTime());
		cachedPortletData.setTimeStored(now);
		
		PortletRendererImpl portletRenderer = new PortletRendererImpl();
		IPortletWindowId portletWindowId = mock(IPortletWindowId.class);
		IPortletWindow portletWindow = mock(IPortletWindow.class);
		IPortletCacheControlService portletCacheControlService = mock(IPortletCacheControlService.class);
		portletRenderer.setPortletCacheControlService(portletCacheControlService);
		IPortletWindowRegistry portletWindowRegistry = mock(IPortletWindowRegistry.class);
		portletRenderer.setPortletWindowRegistry(portletWindowRegistry);
		PortletDelegationLocator portletDelegationLocator = mock(PortletDelegationLocator.class);
		portletRenderer.setPortletDelegationLocator(portletDelegationLocator);
		PortletContainer portletContainer = mock(PortletContainer.class);
		portletRenderer.setPortletContainer(portletContainer);
		
		PortletWindow plutoPortletWindow = mock(PortletWindow.class);
		when(portletWindow.getPlutoPortletWindow()).thenReturn(plutoPortletWindow);
		when(portletCacheControlService.getPortletResourceCacheControl(portletWindowId, request, response)).thenReturn(cacheControl);
		when(portletCacheControlService.getCachedPortletResourceOutput(portletWindowId, request)).thenReturn(cachedPortletData);
		when(portletWindowRegistry.getPortletWindow(request, portletWindowId)).thenReturn(portletWindow);
		
		portletRenderer.doServeResource(portletWindowId, request, response);
		// verify content matches what was in cache (no array support in Assert.assertEquals, check byte for byte)
		byte [] fromResponse = response.getContentAsByteArray();
		Assert.assertEquals(content.length, fromResponse.length);
		for(int i = 0; i < content.length; i++) {
			Assert.assertEquals(content[i], fromResponse[i]);
		}
		// verify we enter the first branch and never execute portletContainer#doServeResource
		verify(portletContainer, never()).doServeResource(isA(PortletWindow.class), isA(PortletHttpServletRequestWrapper.class), isA(PortletHttpServletResponseWrapper.class));
		// verify we never enter the other branch of the "should render cached output" if statement
		verify(portletCacheControlService, never()).shouldOutputBeCached(isA(CacheControl.class));
	}
	
	/**
	 * Verify headers stored in cache are replayed on the response for cached doServeResource content.
	 * 
	 * @throws PortletContainerException 
	 * @throws IOException 
	 * @throws PortletException 
	 */
	@Test
	public void doServeResourceCachedContentReplayHeadersTest() throws PortletException, IOException, PortletContainerException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		Date now = new Date();
		CacheControlImpl cacheControl = new CacheControlImpl();
		cacheControl.setUseCachedContent(true);
		cacheControl.setExpirationTime(300);
		CachedPortletData cachedPortletData = new CachedPortletData();
		cachedPortletData.setContentType("application/json");
		byte [] content = "{ \"hello\": \"world\" }".getBytes();
		Map<String, String[]> headers = new HashMap<String, String[]>();
		headers.put("header1", new String[] {"value1"});
		headers.put("header2", new String[] {"value2", "value3"});
		cachedPortletData.setHeaders(headers);
		cachedPortletData.setByteData(content);
		cachedPortletData.setExpirationTimeSeconds(cacheControl.getExpirationTime());
		cachedPortletData.setTimeStored(now);
		
		PortletRendererImpl portletRenderer = new PortletRendererImpl();
		IPortletWindowId portletWindowId = mock(IPortletWindowId.class);
		IPortletWindow portletWindow = mock(IPortletWindow.class);
		IPortletCacheControlService portletCacheControlService = mock(IPortletCacheControlService.class);
		portletRenderer.setPortletCacheControlService(portletCacheControlService);
		IPortletWindowRegistry portletWindowRegistry = mock(IPortletWindowRegistry.class);
		portletRenderer.setPortletWindowRegistry(portletWindowRegistry);
		PortletDelegationLocator portletDelegationLocator = mock(PortletDelegationLocator.class);
		portletRenderer.setPortletDelegationLocator(portletDelegationLocator);
		PortletContainer portletContainer = mock(PortletContainer.class);
		portletRenderer.setPortletContainer(portletContainer);
		
		PortletWindow plutoPortletWindow = mock(PortletWindow.class);
		when(portletWindow.getPlutoPortletWindow()).thenReturn(plutoPortletWindow);
		when(portletCacheControlService.getPortletResourceCacheControl(portletWindowId, request, response)).thenReturn(cacheControl);
		when(portletCacheControlService.getCachedPortletResourceOutput(portletWindowId, request)).thenReturn(cachedPortletData);
		when(portletWindowRegistry.getPortletWindow(request, portletWindowId)).thenReturn(portletWindow);
		
		portletRenderer.doServeResource(portletWindowId, request, response);
		// verify content matches what was in cache (no array support in Assert.assertEquals, check byte for byte)
		byte [] fromResponse = response.getContentAsByteArray();
		Assert.assertEquals(content.length, fromResponse.length);
		for(int i = 0; i < content.length; i++) {
			Assert.assertEquals(content[i], fromResponse[i]);
		}
		Assert.assertEquals("value1", response.getHeader("header1"));
		Assert.assertEquals(Arrays.asList(new String[] {"value2", "value3"}), response.getHeaders("header2"));
		// verify we enter the first branch and never execute portletContainer#doServeResource
		verify(portletContainer, never()).doServeResource(isA(PortletWindow.class), isA(PortletHttpServletRequestWrapper.class), isA(PortletHttpServletResponseWrapper.class));
		// verify we never enter the other branch of the "should render cached output" if statement
		verify(portletCacheControlService, never()).shouldOutputBeCached(isA(CacheControl.class));
	}
}
