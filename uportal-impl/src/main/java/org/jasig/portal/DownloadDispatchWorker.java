/**
 * Copyright (c) 2000-2009, Jasig, Inc.
 * See license distributed with this file and available online at
 * https://www.ja-sig.org/svn/jasig-parent/tags/rel-10/license-header.txt
 */
package org.jasig.portal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.portal.spring.locator.ChannelRequestParameterManagerLocator;
import org.jasig.portal.url.support.IChannelRequestParameterManager;

/**
 * Provides file download capability for the portal.
 *
 * @author <a href="mailto:svenkatesh@interactivebusiness.com">Sridhar Venkatesh</a>
 * @author Peter Kharchenko  {@link <a href="mailto:pkharchenko@interactivebusiness.com"">pkharchenko@interactivebusiness.com"</a>}
 */
public class DownloadDispatchWorker implements IWorkerRequestProcessor {
    
    private static final Log log = LogFactory.getLog(DownloadDispatchWorker.class);
    
    public void processWorkerDispatch(PortalControlStructures pcs) throws PortalException {
        HttpServletRequest req=pcs.getHttpServletRequest();
        HttpServletResponse res=pcs.getHttpServletResponse();

        // determine the channel, follow the same logic as the standard uPortal processing.
        // (although, in general, worker processors can make their own rules
        String channelTarget=null;
        Map<String, Object> targetParams = new Hashtable<String, Object>();
        
        final ChannelManager channelManager = pcs.getChannelManager();

        final String fnameTarget = req.getParameter("uP_fname");
        if (fnameTarget != null) {
            try {
                channelTarget = channelManager.getSubscribeId(fnameTarget);
            }
            catch (PortalException pe) {
                log.error("Unable to get subscribe ID for fname=" + fnameTarget, pe);
            }
        }
        
        // check if the uP_channelTarget parameter has been passed
        if (channelTarget == null) {
            channelTarget=req.getParameter("uP_channelTarget");
            if(channelTarget==null) {
                // determine target channel id
                UPFileSpec upfs=new UPFileSpec(req);
                channelTarget=upfs.getTargetNodeId();
            }
        }

        // gather parameters
        if(channelTarget!=null) {
            final IChannelRequestParameterManager channelParameterManager = ChannelRequestParameterManagerLocator.getChannelRequestParameterManager();
            
            final Map<String, Object[]> channelParameters = channelParameterManager.getChannelParameters(req, channelTarget);
            
            Enumeration en = req.getParameterNames();
            if (en != null) {
                while (en.hasMoreElements()) {
                    String pName= (String) en.nextElement();
                    if (!pName.equals ("uP_channelTarget")) {
                        Object[] val= (Object[]) req.getParameterValues(pName);
                        if (val == null) {
                            val = channelParameters.get(pName);
                        }
                        targetParams.put(pName, val);
                    }
                }
            }

            final IChannel ch = channelManager.getChannelInstance(pcs.getHttpServletRequest(), pcs.getHttpServletResponse(), channelTarget);

            if(ch!=null) {
                // set pcs
                if(ch instanceof IPrivileged) {
                    ((IPrivileged)ch).setPortalControlStructures(pcs);
                }
                // set runtime data
                ChannelRuntimeData rd = new ChannelRuntimeData();
                rd.setParameters(targetParams);
                rd.setBrowserInfo(new BrowserInfo(req));
                rd.setHttpRequestMethod(req.getMethod());
				rd.setRemoteAddress(req.getRemoteAddr());
                rd.setUPFile(new UPFileSpec(UPFileSpec.RENDER_METHOD,UPFileSpec.USER_LAYOUT_ROOT_NODE,channelTarget,null));
                
                if (ch instanceof org.jasig.portal.IMimeResponse) {
                  ch.setRuntimeData(rd);

                  org.jasig.portal.IMimeResponse ds = (org.jasig.portal.IMimeResponse)ch;
                  ServletOutputStream out = null;
                  InputStream ios = null;
                    try {

                        // Set the headers if available
                        Map httpHeaders = ds.getHeaders();
                        if (httpHeaders != null) {
                            Set headerKeys = httpHeaders.keySet();
                            Iterator it = headerKeys.iterator();
                            while (it.hasNext()) {
                                String param = (String)it.next();
                                String value = (String)httpHeaders.get(param);
                                res.setHeader(param, value);
                            }
                            httpHeaders.clear();
                        }

                        // Set the MIME content type
                        res.setContentType (ds.getContentType());

                        // Set the data
                        out = res.getOutputStream();
                        ios = ds.getInputStream();
                        if (ios != null) {
                            int size = 0;
                            byte[] contentBytes = new byte[8192];
                            while ((size = ios.read(contentBytes)) != -1) {
                                out.write(contentBytes,0, size);
                            }
                        } else {
                            /**
                             * The channel has more complicated processing it needs to do on the
                             * output stream
                             */
                            ds.downloadData(out);
                        }
                        out.flush();
                    } catch (Exception e) {
                        ds.reportDownloadError(e);
                    } finally {
                        try {
                            if (ios != null) 
                                ios.close();
                            if (out != null) 
                                out.close();
                        } catch (IOException ioe) {
                            log.error("unable to close IOStream ", ioe);
                        }
                    }
                } else if (ch instanceof org.jasig.portal.IDirectResponse) {
                    //We are allowing the rendering of URLs in the IDirectResponse interface
                    //so the tag needs to be set for the uPfile
                    rd.setTargeted(true);
                    ch.setRuntimeData(rd);
                    
                    org.jasig.portal.IDirectResponse dirResp = (org.jasig.portal.IDirectResponse)ch;
                    
                    dirResp.setResponse(res);                    
                } else {
                    log.error("Channel (instanceId='"+channelTarget+"' needs to implement either the '" + IMimeResponse.class + "' or '" + IDirectResponse.class + "' interface in order to download files.");
                }
            } else {
                log.warn("unable to obtain instance a channel. instanceId='"+channelTarget+"'.");
            }
        } else {
            log.error("unable to determine instance Id of the target channel. requestURL='"+pcs.getHttpServletRequest().getRequestURI()+"'.");
        }
    }
}
