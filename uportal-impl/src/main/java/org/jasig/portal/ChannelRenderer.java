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

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.portal.channels.portlet.IPortletAdaptor;
import org.jasig.portal.channels.support.IChannelTitle;
import org.jasig.portal.channels.support.IDynamicChannelTitleRenderer;
import org.jasig.portal.layout.node.IUserLayoutChannelDescription;
import org.jasig.portal.portlet.url.RequestType;
import org.jasig.portal.properties.PropertiesManager;
import org.jasig.portal.spring.locator.CacheFactoryLocator;
import org.jasig.portal.spring.locator.JpaInterceptorLocator;
import org.jasig.portal.utils.SAX2BufferImpl;
import org.jasig.portal.utils.SetCheckInSemaphore;
import org.jasig.portal.utils.cache.CacheFactory;
import org.jasig.portal.utils.threading.BaseTask;
import org.jasig.portal.utils.threading.Task;
import org.jasig.portal.utils.threading.TrackingThreadLocal;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.orm.jpa.JpaInterceptor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * This class takes care of initiating channel rendering thread,
 * monitoring it for timeouts, retreiving cache, and returning
 * rendering results and status.
 * @author Peter Kharchenko  {@link <a href="mailto:pkharchenko@interactivebusiness.com"">pkharchenko@interactivebusiness.com"</a>}
 * @version $Revision$
 * @deprecated IChannel rendering code will be replaced with portlet specific rendering code in a future release
 */
@Deprecated
public class ChannelRenderer
    implements IChannelRenderer, IDynamicChannelTitleRenderer
{

    protected final Log log = LogFactory.getLog(getClass());

    /**
     * Default value for CACHE_CHANNELS.
     * This value will be used when the corresponding property cannot be loaded.
     */
    private static final boolean DEFAULT_CACHE_CHANNELS = false;

    public static final boolean CACHE_CHANNELS=PropertiesManager.getPropertyAsBoolean("org.jasig.portal.ChannelRenderer.cache_channels", DEFAULT_CACHE_CHANNELS);

    public static final String[] renderingStatus={"successful","failed","timed out"};
    
    public static final String SYSTEM_WIDE_CHANNEL_CACHE = "org.jasig.portal.ChannelRenderer.SYSTEM_WIDE_CHANNEL_CACHE";

    protected final IUserLayoutChannelDescription channelDesc;
    protected final IChannel channel;
    protected final ChannelRuntimeData rd;
    protected Map<String, ChannelCacheEntry> channelCache;
    protected Map<IChannel, Map<String, ChannelCacheEntry>> cacheTables;

    protected boolean rendering;
    protected boolean donerendering;

    protected Thread workerThread;

    protected IWorker worker;
    protected Future<?> workTracker;

    protected long startTime;
    protected long timeOut = java.lang.Long.MAX_VALUE;

    protected boolean ccacheable;

    protected static ExecutorService tp=null;

    protected SetCheckInSemaphore groupSemaphore;
    protected Object groupRenderingKey;

    /**
     * Default contstructor
     *
     * @param chan an <code>IChannel</code> value
     * @param runtimeData a <code>ChannelRuntimeData</code> value
     * @param threadPool a <code>ThreadPool</code> value
     */
    public ChannelRenderer (IUserLayoutChannelDescription channelDesc, IChannel chan,ChannelRuntimeData runtimeData, ExecutorService threadPool) {
        Validate.notNull(channelDesc, "IUserLayoutChannelDescription can not be null");
        Validate.notNull(chan, "IChannel can not be null");
        this.channelDesc = channelDesc;
        this.channel=chan;
        this.rd=runtimeData;
        this.rendering = false;
        this.ccacheable=false;
        tp = threadPool;

        this.groupSemaphore=null;
        this.groupRenderingKey=null;
    }


    /**
     * Default contstructor
     *
     * @param chan an <code>IChannel</code> value
     * @param runtimeData a <code>ChannelRuntimeData</code> value
     * @param threadPool a <code>ThreadPool</code> value
     * @param groupSemaphore a <code>SetCheckInSemaphore</code> for the current rendering group
     * @param groupRenderingKey an <code>Object</code> to be used for check ins with the group semaphore
     */
    public ChannelRenderer (IUserLayoutChannelDescription channelDesc, IChannel chan,ChannelRuntimeData runtimeData, ExecutorService threadPool, SetCheckInSemaphore groupSemaphore, Object groupRenderingKey) {
        this(channelDesc, chan,runtimeData,threadPool);
        this.groupSemaphore=groupSemaphore;
        this.groupRenderingKey=groupRenderingKey;
    }

    /**
     * Obtains a content cache specific for this channel instance.
     *
     * @return a key->rendering map for this channel
     */
    // XXX is this thread safe?
    Map<String, ChannelCacheEntry> getChannelCache() {
        if (this.channelCache == null) {
            if ((this.channelCache = this.cacheTables.get(this.channel)) == null) {
                this.channelCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT, 2, .75f, true);
                this.cacheTables.put(this.channel, this.channelCache);
            }
        }
        return this.channelCache;
    }


    /**
     * Set the timeout value
     * @param value timeout in milliseconds
     */
    public void setTimeout (long value) {
        this.timeOut = value;
    }

    public void setCacheTables(Map<IChannel, Map<String, ChannelCacheEntry>> cacheTables) {
        this.cacheTables=cacheTables;
    }

    /**
     * Informs IChannelRenderer that a character caching scheme
     * will be used for the current rendering.
     * @param setting a <code>boolean</code> value
     */
    public void setCharacterCacheable(boolean setting) {
        this.ccacheable=setting;
    }

  /**
   * Start rendering of the channel in a new thread.
   * Note that rendered information will be accumulated in a
   * buffer until outputRendering() function is called.
   * startRendering() is a non-blocking function.
   */
  public void startRendering ()
  {
    // start the rendering thread

    final IWorker targetWorker = new Worker(this.channelDesc, this.channel, this.rd);
    
    // Obtain JcrInterceptor bean
    final JpaInterceptor jpaInterceptor = JpaInterceptorLocator.getJpaInterceptor();

    // Proxy worker so that Jpa EntityManager will be properly handled
    final ProxyFactoryBean pfb = new ProxyFactoryBean();
    pfb.setTarget(targetWorker);
    pfb.setInterfaces(targetWorker.getClass().getInterfaces());
    pfb.addAdvice(jpaInterceptor);
   
    this.worker = (IWorker)pfb.getObject();

    this.workTracker = tp.submit(this.worker); // XXX is execute okay?
    this.rendering = true;
    this.startTime = System.currentTimeMillis ();
  }

    public void startRendering(SetCheckInSemaphore groupSemaphore, Object groupRenderingKey) {
        this.groupSemaphore=groupSemaphore;
        this.groupRenderingKey=groupRenderingKey;
        this.startRendering();
    }

    /**
     * <p>Cancels the rendering job.
     **/
    public void cancelRendering()
    {
        if (null != this.workTracker) {
            this.workTracker.cancel(true);
        }
    }

  /**
   * Output channel rendering through a given ContentHandler.
   * Note: call of outputRendering() without prior call to startRendering() is equivalent to
   * sequential calling of startRendering() and then outputRendering().
   * outputRendering() is a blocking function. It will return only when the channel completes rendering
   * or fails to render by exceeding allowed rendering time.
   * @param out Document Handler that will receive information rendered by the channel.
   * @return error code. 0 - successful rendering; 1 - rendering failed; 2 - rendering timedOut;
   */
    public int outputRendering (ContentHandler out) throws Throwable {
        int renderingStatus=completeRendering();
        if(renderingStatus==RENDERING_SUCCESSFUL) {
            SAX2BufferImpl buffer;
            if ((buffer=this.worker.getBuffer())!=null) {
                // unplug the buffer :)
                try {
                    buffer.setAllHandlers(out);
                    buffer.outputBuffer();
                    return RENDERING_SUCCESSFUL;
                } catch (SAXException e) {
                    // worst case scenario: partial content output :(
                    log.error( "outputRendering() : following SAX exception occured : "+e);
                    throw e;
                }
            } else {
                log.error( "outputRendering() : output buffer is null even though rendering was a success?! trying to rendering for ccaching ?");
                throw new PortalException("unable to obtain rendering buffer");
            }
        }
        return renderingStatus;
    }


    /**
     * Requests renderer to complete rendering and return status.
     * This does exactly the same things as outputRendering except for the
     * actual stream output.
     *
     * @return an <code>int</code> return status value
     */

    public int completeRendering() throws Throwable {
        if (!this.rendering) {
            this.startRendering ();
        }
        boolean abandoned=false;
        long timeOutTarget = this.startTime + this.timeOut;

        // separate waits caused by rendering group
        if(this.groupSemaphore!=null) {
            while(!this.worker.isSetRuntimeDataComplete() && System.currentTimeMillis() < timeOutTarget && !this.workTracker.isDone()) {
                long wait=timeOutTarget-System.currentTimeMillis();
                if(wait<=0) { wait=1; }
                try {
                    synchronized(this.groupSemaphore) {
                        this.groupSemaphore.wait(wait);
                    }
                } catch (InterruptedException ie) {}
            }
            if(!this.worker.isSetRuntimeDataComplete() && !this.workTracker.isDone()) {
                this.workTracker.cancel(true);
                abandoned=true;
                if (log.isDebugEnabled())
                    log.debug("outputRendering() : killed. " +
                            "(key="+this.groupRenderingKey.toString()+")");
            } else {
                this.groupSemaphore.waitOn();
            }
            // reset timer for rendering
            timeOutTarget=System.currentTimeMillis()+this.timeOut;
        }

        if(!abandoned) {
            try {
                this.workTracker.get(this.timeOut, TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                if (log.isDebugEnabled()) {
                    log.debug("outputRendering() : channel [" + this.channel + "] timed out", te);
                }
            } catch (CancellationException ce) {

                if (log.isDebugEnabled()) {
                    Throwable t = null;
                    try {
                        // in a try block to ensure further errors don't block reporting
                        // the CancellationException.
                        t = this.worker.getException();
                    } catch (Exception e) {
                        // ignore problem in getting the exception to report.
                    }
                    log.debug("outputRendering() : channel [" + this.channel + "] threw an exception [" + t + "] and so its task was cancelled.");
                }

            } catch (Exception e) {
                // no matter what went wrong (CancellationException, a NullPointerException, etc.)
                // the recovery code following this attempt to get the result from the workTracker Future
                // should be allowed to run.
                log.error("Unexpected exceptional condition trying to get the result from the workTracker Future rendering channel [" + this.channel + "].", e);
            }

            if(!this.workTracker.isDone()) {
                this.workTracker.cancel(true);
                abandoned=true;
                if (log.isDebugEnabled())
                    log.debug("outputRendering() : killed.");
            } else {
                boolean successful = this.workTracker.isDone() && !this.workTracker.isCancelled() && this.worker.getException() == null;
                abandoned=!successful;
            }

        }

        if (!abandoned && this.worker.done ()) {
            if (this.worker.successful() && ((this.rd != null && RequestType.ACTION.equals(this.rd.getRequestType())) || this.worker.getBuffer() != null || this.worker.getCharacters() != null)) {
                return RENDERING_SUCCESSFUL;
            }
         
            // rendering was not successful
            Throwable e;
            if((e=this.worker.getException())!=null) throw new InternalPortalException(e);
            // should never get there, unless thread.stop() has seriously messed things up for the worker thread.
            return RENDERING_FAILED;
        }
        
        Throwable e = null;
        if (this.worker != null) {
          e = this.worker.getException();
        }

        if (e != null) {
            throw new InternalPortalException(e);
        }
        
        // Assume rendering has timed out
        return RENDERING_TIMED_OUT;
    }


    /**
     * Returns rendered buffer.
     * This method does not perform any status checks, so make sure to call completeRendering() prior to invoking this method.
     *
     * @return rendered buffer
     */
    public SAX2BufferImpl getBuffer() {
        return this.worker != null ? this.worker.getBuffer() : null;
    }

    /**
     * Returns a character output of a channel rendering.
     */
    public String getCharacters() {
        if(this.worker!=null) {
            return this.worker.getCharacters();
        }

        if (log.isDebugEnabled()) {
            log.debug("getCharacters() : worker is null already !");
        }

        return null;
    }
    
    /* (non-Javadoc)
     * @see org.jasig.portal.IChannelRenderer#getRenderTime()
     */
    public long getRenderTime() {
        return this.worker.getRenderTime();
    }


    /* (non-Javadoc)
     * @see org.jasig.portal.IChannelRenderer#isRenderedFromCache()
     */
    public boolean isRenderedFromCache() {
        return this.worker.isRenderedFromCache();
    }


    /**
     * Sets a character cache for the current rendering.
     */
    public void setCharacterCache(String chars) {
        if(this.worker!=null) {
            this.worker.setCharacterCache(chars);
        }
    }

    /**
     * This method suppose to take care of the runaway rendering threads.
     * This method will be called from ChannelManager explictly.
     */
    public void kill() {
        if(this.workTracker!=null && !this.workTracker.isDone())
            this.workTracker.cancel(true);
    }

    public String toString() {

        return this.getClass().getSimpleName() + " [" +
                "channel=" + this.channel + ", " +
                "rendering=" + this.rendering + ", " +
                "doneRendering=" + this.donerendering + ", " +
                "startTime=" + this.startTime + ", " +
                "timeOut=" + this.timeOut + "]";
    }

    public String getChannelTitle() {


        if (log.isTraceEnabled()) {
            log.trace("Getting channel title for ChannelRenderer " + this);
        }

        // default to null, which indicates the ChannelRenderer doesn't have
        // a dynamic channel title available.
        String channelTitle = null;
        try {
            // block on channel rendering to allow channel opportunity to
            // provide dynamic title.
            int renderingStatus = completeRendering();
            if (renderingStatus == RENDERING_SUCCESSFUL) {
                channelTitle = this.worker.getChannelTitle();
            }
        } catch (Throwable t) {
            log.error("Channel rendering failed while getting title for channel renderer " + this, t);
        }

        // will be null indicating no dynamic title unless successfully obtained title.
        return channelTitle;

    }


    protected class Worker extends BaseTask implements IWorker {
        private final IUserLayoutChannelDescription channelDesc;
        private final IChannel channel;
        private final ChannelRuntimeData rd;
        private final Map<TrackingThreadLocal<Object>, Object> currentData;
        private final RequestAttributes requestAttributes;
        private final Locale locale;
        
        private boolean successful;
        private boolean done;
        private boolean setRuntimeDataComplete;
        private SAX2BufferImpl buffer;
        private String cbuffer;
        
        private volatile Long threadReceivedTime = null;
        private long renderTime;
        private boolean renderedFromCache = false;

        /**
         * The dynamic title of the channel, if any.  Null otherwise.
         */
        private String channelTitle = null;

        public Worker(IUserLayoutChannelDescription channelDesc, IChannel ch, ChannelRuntimeData runtimeData) {
            this.channelDesc = channelDesc;
            this.channel = ch;
            this.rd = runtimeData;
            this.requestAttributes = RequestContextHolder.getRequestAttributes();
            this.locale = LocaleContextHolder.getLocale();

            successful = false;
            done = false;
            setRuntimeDataComplete = false;
            buffer = null;
            cbuffer = null;
            currentData = TrackingThreadLocal.getCurrentData();
            
            if (log.isTraceEnabled()) {
                log.trace("Created " + this.toString());
            }
        }
        
        public IUserLayoutChannelDescription getUserLayoutChannelDescription() {
            return channelDesc;
        }

        public boolean isSetRuntimeDataComplete() {
            return this.setRuntimeDataComplete;
        }

        //TODO review this for clarity
        public void execute () throws Exception {
            if (log.isTraceEnabled()) {
                log.trace("Started execution " + this.toString());
            }
            
            threadReceivedTime = System.currentTimeMillis();
            try {
                TrackingThreadLocal.setCurrentData(this.currentData);
                RequestContextHolder.setRequestAttributes(this.requestAttributes);
                LocaleContextHolder.setLocale(this.locale);
                if (log.isDebugEnabled()) {
                    log.debug("Bound request attributes to thread: " + this.requestAttributes);
                }
                
                if(rd!=null) {
                    channel.setRuntimeData(rd);
                    
                    if (RequestType.ACTION.equals(rd.getRequestType())) {
                        if (channel instanceof IPortletAdaptor) {
                            try {
                                ((IPortletAdaptor)channel).processAction();
                                successful = true;
                            }
                            catch (Exception e) {
                                this.setException(e);
                            }
                        }
                        else {
                            this.setException(new ClassCastException("Action request for channel '" + channel + "' that does not implement '" + IPortletAdaptor.class + "'"));
                        }

                        done = true;

                        return;
                    }
                }
                setRuntimeDataComplete=true;
                
                if(groupSemaphore!=null) {
                    groupSemaphore.checkInAndWaitOn(groupRenderingKey);
                }

                if(CACHE_CHANNELS) {
                    // try to obtain rendering from cache
                    if(channel instanceof ICacheable ) {
                        final CacheFactory cacheFactory = CacheFactoryLocator.getCacheFactory();
                        final Map<Serializable, ChannelCacheEntry> systemCache = cacheFactory.getCache(SYSTEM_WIDE_CHANNEL_CACHE);
                        
                        ChannelCacheKey key=((ICacheable)channel).generateKey();
                        if (log.isTraceEnabled()) {
                            log.trace("Generated cache key " + (key != null ? key.getKey() : null) + " for worker " + this.toString());
                        }
                        if(key!=null) {
                            if(key.getKeyScope()==ChannelCacheKey.SYSTEM_KEY_SCOPE) {
                                ChannelCacheEntry entry=systemCache.get(key.getKey());
                                if(entry!=null) {
                                    // found cached page
                                    // check page validity
                                    if(((ICacheable)channel).isCacheValid(entry.validity) && (entry.buffer!=null)) {
                                        // use it
                                        if(ccacheable && (entry.buffer instanceof String)) {
                                            cbuffer=(String)entry.buffer;
                                            if (log.isDebugEnabled()) {
                                                log.debug("retrieved system-wide cached character content based on a key \""+key.getKey()+"\"");
                                            }
                                        } else if(entry.buffer instanceof SAX2BufferImpl) {
                                            buffer=(SAX2BufferImpl) entry.buffer;
                                            if (log.isDebugEnabled()) {
                                                log.debug("retrieved system-wide cached content based on a key \""+key.getKey()+"\"");
                                            }
                                        }
                                        this.channelTitle = entry.title;
                                        
                                        this.renderedFromCache = true;
                                    } else {
                                        // remove it
                                        systemCache.remove(key.getKey());
                                        if (log.isDebugEnabled()) {
                                            log.debug("removed system-wide unvalidated cache based on a key \""+key.getKey()+"\"");
                                        }
                                    }
                                }
                            } else {
                                // by default we assume INSTANCE_KEY_SCOPE
                                ChannelCacheEntry entry=getChannelCache().get(key.getKey());
                                if(entry!=null) {
                                    // found cached page
                                    // check page validity
                                    if(((ICacheable)channel).isCacheValid(entry.validity) && (entry.buffer!=null)) {
                                        // use it
                                        if(ccacheable && (entry.buffer instanceof String)) {
                                            cbuffer=(String)entry.buffer;
                                            if (log.isDebugEnabled()) {
                                                log.debug("retrieved instance-cached character content based on a key \""+key.getKey()+"\" by " + this.toString());
                                            }

                                        } else if(entry.buffer instanceof SAX2BufferImpl) {
                                            buffer=(SAX2BufferImpl) entry.buffer;
                                            if (log.isDebugEnabled()) {
                                                log.debug("retrieved instance-cached content based on a key \""+key.getKey()+"\" by " + this.toString());
                                            }
                                        }
                                        this.channelTitle = entry.title;
                                        
                                        this.renderedFromCache = true;
                                    } else {
                                        // remove it
                                        getChannelCache().remove(key.getKey());
                                        if (log.isDebugEnabled()) {
                                            log.debug("removed unvalidated instance-cache based on a key \""+key.getKey()+"\" by " + this.toString());
                                        }
                                    }
                                }
                            }
                        }

                        // future work: here we should synchronize based on a particular cache key.
                        // Imagine a VERY popular cache entry timing out, then portal will attempt
                        // to re-render the page in many threads (serving many requests) simultaneously.
                        // If one was to synchronize on writing cache for a particular key, one thread
                        // would render and others would wait for it to complete.

                        // check if need to render
                        if(cbuffer==null && buffer==null) {
                            if (channel instanceof ICharacterChannel) {
                                StringWriter sw = new StringWriter(100);
                                PrintWriter pw = new PrintWriter(sw);
                                ((ICharacterChannel)channel).renderCharacters(pw);
                                
                                processChannelRuntimeProperties();

                                pw.flush();
                                cbuffer = sw.toString();
                                // save cache
                                if (ccacheable && key != null) {
                                    if (key.getKeyScope() == ChannelCacheKey.SYSTEM_KEY_SCOPE) {
                                        systemCache.put(key.getKey(), new ChannelCacheEntry(cbuffer, this.channelTitle, key.getKeyValidity()));
                                        if (log.isDebugEnabled()) {
                                            log.debug("recorded system character cache based on a key \"" + key.getKey() + "\" by " + this.toString());
                                        }
                                    } else {
                                        getChannelCache().put(key.getKey(), new ChannelCacheEntry(cbuffer, this.channelTitle, key.getKeyValidity()));
                                        if (log.isDebugEnabled()) {
                                            log.debug("recorded instance character cache based on a key \"" + key.getKey() + "\" by " + this.toString());
                                        }
                                    }
                                }
                            } else {
                                // need to render again and cache the output
                                buffer = new SAX2BufferImpl ();
                                buffer.startBuffering();
                                channel.renderXML(buffer);

                                processChannelRuntimeProperties();

                                // save cache
                                if(key!=null) {

                                    if(key.getKeyScope()==ChannelCacheKey.SYSTEM_KEY_SCOPE) {
                                        systemCache.put(key.getKey(),new ChannelCacheEntry(buffer, this.channelTitle, key.getKeyValidity()));
                                        if (log.isDebugEnabled()) {
                                            log.debug("recorded system cache based on a key \""+key.getKey()+"\"");
                                        }
                                    } else {
                                        getChannelCache().put(key.getKey(),new ChannelCacheEntry(buffer, this.channelTitle, key.getKeyValidity()));
                                        if (log.isDebugEnabled()) {
                                            log.debug("recorded instance cache based on a key \""+key.getKey()+"\"");
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (ccacheable && channel instanceof ICharacterChannel) {
                            StringWriter sw = new StringWriter(100);
                            PrintWriter pw = new PrintWriter(sw);
                            ((ICharacterChannel)channel).renderCharacters(pw);
                            pw.flush();
                            cbuffer = sw.toString();
                        } else {
                            buffer = new SAX2BufferImpl ();
                            buffer.startBuffering();
                            channel.renderXML(buffer);
                        }
                    }
                } else  {
                    // in the case when channel cache is not enabled
                    buffer = new SAX2BufferImpl ();
                    buffer.startBuffering();
                    channel.renderXML (buffer);
                }
                successful = true;
            } catch (Exception e) {
                if(groupSemaphore!=null) {
                    groupSemaphore.checkIn(groupRenderingKey);
                }
                this.setException(e);
            }
            finally {
                TrackingThreadLocal.clearCurrentData(this.currentData.keySet());
                RequestContextHolder.resetRequestAttributes();
                LocaleContextHolder.resetLocaleContext();
                
                this.renderTime = System.currentTimeMillis() - threadReceivedTime;
            }

            done = true;
            
            if (log.isTraceEnabled()) {
                log.trace("Completed execution " + this.toString());
            }
        }

        /**
         * Query the channel for ChannelRuntimePRoperties and process those
         * properties.
         *
         * Currently, only handles the optional {@link IChannelTitle} interface.
         */
        private void processChannelRuntimeProperties() {
            ChannelRuntimeProperties channelProps = this.channel.getRuntimeProperties();

            if (channelProps != null) {
                if (channelProps instanceof IChannelTitle) {

                    this.channelTitle = ((IChannelTitle) channelProps).getChannelTitle();
                    if (log.isTraceEnabled()) {
                        log.trace("Read title [" + this.channelTitle + "] by " + this.toString());
                    }
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("ChannelRuntimeProperties were non-null but did not implement ITitleable.");
                    }
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("ChannelRuntimeProperties were null from channel " + channel);
                }
            }
        }

        public boolean successful () {
            return this.successful;
        }

        public SAX2BufferImpl getBuffer() {
            return this.buffer;
        }

        /**
         * Returns a character output of a channel rendering.
         */
        public String getCharacters() {
            if(!ccacheable) {
                log.error("Attempting to obtain character data for '" + channel + "' while character caching is not enabled !", new Throwable());
                return null;
            }

            if (log.isTraceEnabled()) {
                log.trace("Getting channel characters (" + (this.cbuffer != null ? this.cbuffer.length() : 0) + ") for " + this);
            }
            
            return this.cbuffer;
        }


        /**
         * Sets a character cache for the current rendering.
         */
        public void setCharacterCache(String chars) {
            cbuffer=chars;
            if(CACHE_CHANNELS) {
                // try to obtain rendering from cache
                if(channel instanceof ICacheable ) {
                    final CacheFactory cacheFactory = CacheFactoryLocator.getCacheFactory();
                    final Map<Serializable, ChannelCacheEntry> systemCache = cacheFactory.getCache(SYSTEM_WIDE_CHANNEL_CACHE);
                    ChannelCacheKey key=((ICacheable)channel).generateKey();
                    if(key!=null) {
                        if (log.isDebugEnabled()) {
                            log.debug("setCharacterCache() : called on a key \""+key.getKey()+"\"");
                        }
                        ChannelCacheEntry entry=null;
                        if(key.getKeyScope()==ChannelCacheKey.SYSTEM_KEY_SCOPE) {
                            entry=systemCache.get(key.getKey());
                            if(entry==null) {
                                if (log.isDebugEnabled()) {
                                    log.debug("setCharacterCache() : setting character cache buffer based on a system key \""+key.getKey()+"\"");
                                }
                                entry=new ChannelCacheEntry(chars, this.channelTitle, key.getKeyValidity());
                            } else {
                                entry.buffer=chars;
                                entry.title=this.channelTitle;
                            }
                            systemCache.put(key.getKey(),entry);
                        } else {
                            // by default we assume INSTANCE_KEY_SCOPE
                            entry=getChannelCache().get(key.getKey());
                            if(entry==null) {
                                if (log.isDebugEnabled()) {
                                    log.debug("setCharacterCache() : no existing cache on a key \""+key.getKey()+"\"");
                                }
                                entry=new ChannelCacheEntry(chars, this.channelTitle, key.getKeyValidity());
                            } else {
                                entry.buffer=chars;
                                entry.title=this.channelTitle;
                            }
                            getChannelCache().put(key.getKey(),entry);
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("setCharacterCache() : channel cache key is null.");
                        }
                    }
                }
            }
        }

        public boolean done () {
            return this.done;
        }

        /**
         * Get a Sring representing the dynamic channel title reported by the
         * IChannel this ChannelRenderer rendered.  Returns null if the channel
         * reported no such title or the channel isn't done rendering.
         *
         * @return dynamic channel title, or null if no title available.
         */
        public String getChannelTitle() {

            if (log.isTraceEnabled()) {
                log.trace("Getting channel title (" + this.channelTitle + ") for " + this);
            }

            // currently, just provides no dynamic title if not done rendering
            if (this.done) {
                return this.channelTitle;
            } else {
                return null;
            }
        }
        
        public Long getThreadReceivedTime() {
            return threadReceivedTime;
        }

        /**
         * @return the renderTime
         */
        public long getRenderTime() {
            return renderTime;
        }

        /**
         * @return the renderedFromCache
         */
        public boolean isRenderedFromCache() {
            return renderedFromCache;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + " [" +
                "channel=" + this.channel + ", " +
                "done=" + this.done + ", " + 
                "successful=" + this.successful + ", " + 
                "renderTime=" + this.renderTime + ", " + 
                "renderedFromCache=" + this.renderedFromCache + ", " + 
                "cbuffer.length=" + (this.cbuffer != null ? this.cbuffer.length() : 0) + ", " +
                "parent=" + ChannelRenderer.this + "]";
        }
    }
    
    protected interface IWorker extends Task {
        
        public IUserLayoutChannelDescription getUserLayoutChannelDescription();

        public boolean isSetRuntimeDataComplete();

        //TODO review this for clarity
        public void execute() throws Exception;

        public boolean successful();

        public SAX2BufferImpl getBuffer();

        /**
         * Returns a character output of a channel rendering.
         */
        public String getCharacters();

        /**
         * Sets a character cache for the current rendering.
         */
        public void setCharacterCache(String chars);

        public boolean done();
        
        public Long getThreadReceivedTime();

        /**
         * @return the renderTime
         */
        public long getRenderTime();

        /**
         * @return the renderedFromCache
         */
        public boolean isRenderedFromCache();

        /**
         * Get a Sring representing the dynamic channel title reported by the
         * IChannel this ChannelRenderer rendered.  Returns null if the channel
         * reported no such title or the channel isn't done rendering.
         *
         * @return dynamic channel title, or null if no title available.
         */
        public String getChannelTitle();
    }
}
