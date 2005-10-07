/* Copyright 2002,2005 The JA-SIG Collaborative.  All rights reserved.
*  See license distributed with this file and
*  available online at http://www.uportal.org/license.html
 */


package org.jasig.portal.layout;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;

import org.jasig.portal.IUserLayoutStore;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;


import org.jasig.portal.PortalException;
import org.jasig.portal.UserProfile;
import org.jasig.portal.security.IPerson;
import org.jasig.portal.serialize.OutputFormat;
import org.jasig.portal.serialize.XMLSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.portal.utils.DocumentFactory;

import org.jasig.portal.utils.XSLT;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import org.jasig.portal.utils.XML;


/**
 * An implementation of a user layout manager that uses 2.0-release store implementations.
 *
 * @author <a href="mailto:pkharchenko@interactivebusiness.com">Peter Kharchenko</a>
 * @version $Revision$
 */
public class SimpleUserLayoutManager implements IUserLayoutManager {
    private static final Log log = LogFactory.getLog(SimpleUserLayoutManager.class);
    
    protected final IPerson owner;
    protected final UserProfile profile;
    protected IUserLayoutStore store=null;
    protected Set listeners=new HashSet();

    protected IUserLayout userLayout = null;
    protected Document userLayoutDocument=null;
    protected Document markedUserLayout=null;

    protected static Random rnd=new Random();
    protected String cacheKey="initialKey";
    protected String rootNodeId;

    private boolean dirtyState=false;

    // marking mode variables
    private String markingMode=null; // null means markings are turned off
    private String markingNode;

    // The names for marking nodes
    private static final String ADD_COMMAND = "add";
    private static final String MOVE_COMMAND = "move";

    // marking stylesheet
    private static final String MARKING_XSLT_URI="/org/jasig/portal/layout/MarkUserLayout.xsl";


    public SimpleUserLayoutManager(IPerson owner, UserProfile profile, IUserLayoutStore store) throws PortalException {
        if(owner==null) {
            throw new PortalException("A non-null owner needs to be specified.");
        }

        if(profile==null) {
            throw new PortalException("A non-null profile needs to be specified.");
        }

        this.owner=owner;
        this.profile=profile;
        this.rootNodeId = null;
        this.setLayoutStore(store);
        this.loadUserLayout();
        this.markingMode=null;
        this.markingNode=null;
    }

    public IUserLayout getUserLayout() throws PortalException {
        // Temporary until we use IUserLayout for real
        return new SimpleLayout(String.valueOf(profile.getLayoutId()), this.userLayoutDocument);
    }

    public void setUserLayout(IUserLayout userLayout) throws PortalException {
        // Temporary until we use IUserLayout for real
        Document doc = DocumentFactory.getNewDocument();
        try {
            userLayout.writeTo(doc);
        } catch (PortalException pe) {
        }
        this.userLayoutDocument=doc;
        this.markedUserLayout=null;
        this.updateCacheKey();
    }

    private void setUserLayoutDOM(Document doc) {
        this.userLayoutDocument=doc;
        this.markedUserLayout=null;
        this.updateCacheKey();
    }

    public Document getUserLayoutDOM() {
        return this.userLayoutDocument;
    }

    public void getUserLayout(ContentHandler ch) throws PortalException {
        Document ulm=this.getUserLayoutDOM();
        if(ulm==null) {
            throw new PortalException("User layout has not been initialized");
        } else {
            getUserLayout(ulm,ch);
        }
    }

    public void getUserLayout(String nodeId, ContentHandler ch) throws PortalException {
        Document ulm=this.getUserLayoutDOM();

        if(ulm==null) {
            throw new PortalException("User layout has not been initialized");
        } else {
            Node rootNode=ulm.getElementById(nodeId);

            if(rootNode==null) {
                throw new PortalException("A requested root node (with id=\""+nodeId+"\") is not in the user layout.");
            } else {
                getUserLayout(rootNode,ch);
            }
        }
    }

    protected void getUserLayout(Node n,ContentHandler ch) throws PortalException {
        // do a DOM2SAX transformation, invoking marking transformation if necessary
        try {
            if(markingMode!=null) {
                Hashtable stylesheetParams=new Hashtable(1);
                stylesheetParams.put("operation",markingMode);
                if(markingNode!=null) {
                    stylesheetParams.put("targetId",markingNode);
                }
                XSLT xslt = XSLT.getTransformer(this);
                xslt.setXML(n); 
                xslt.setTarget(ch);
                xslt.setStylesheetParameters(stylesheetParams);
                xslt.setXSL(MARKING_XSLT_URI);
                xslt.transform();
            } else {
                Transformer emptyt=TransformerFactory.newInstance().newTransformer();
                emptyt.transform(new DOMSource(n), new SAXResult(ch));
            }
        } catch (Exception e) {
            throw new PortalException("Encountered an exception trying to output user layout",e);
        }
    }


    public void setLayoutStore(IUserLayoutStore store) {
        this.store=store;
    }

    protected IUserLayoutStore getLayoutStore() {
        return this.store;
    }


    public void loadUserLayout() throws PortalException {
        if(this.getLayoutStore()==null) {
            throw new PortalException("Store implementation has not been set.");
        } else {
            try {
                Document uli=this.getLayoutStore().getUserLayout(this.owner,this.profile);
                if(uli!=null) {
                    this.setUserLayoutDOM(uli);
                    clearDirtyFlag();
                    // inform listeners
                    for(Iterator i=listeners.iterator();i.hasNext();) {
                        LayoutEventListener lel=(LayoutEventListener)i.next();
                        lel.layoutLoaded();
                    }
                    updateCacheKey();
                } else {
                    throw new PortalException("Null user layout returned for ownerId=\""+owner.getID()+"\", profileId=\""+profile.getProfileId()+"\", layoutId=\""+profile.getLayoutId()+"\"");
                }
            } catch (PortalException pe) {
                throw pe;
            } catch (Exception e) {
                throw new PortalException("Exception encountered while reading a layout for userId="+this.owner.getID()+", profileId="+this.profile.getProfileId(),e);
            }
        }
    }

    public void saveUserLayout() throws PortalException{
        if(isLayoutDirty()) {
            Document ulm=this.getUserLayoutDOM();
            if(ulm==null) {
                throw new PortalException("UserLayout has not been initialized.");
            } else {
                if(this.getLayoutStore()==null) {
                    throw new PortalException("Store implementation has not been set.");
                } else {
                    try {
                        this.getLayoutStore().setUserLayout(this.owner,this.profile,ulm,true);
                        // inform listeners
                        for(Iterator i=listeners.iterator();i.hasNext();) {
                            LayoutEventListener lel=(LayoutEventListener)i.next();
                            lel.layoutSaved();
                        }
                    } catch (PortalException pe) {
                        throw pe;
                    } catch (Exception e) {
                        throw new PortalException("Exception encountered while trying to save a layout for userId="+this.owner.getID()+", profileId="+this.profile.getProfileId(),e);
                    }
                }
            }
        }
    }

    public IUserLayoutNodeDescription getNode(String nodeId) throws PortalException {
        Document ulm=this.getUserLayoutDOM();
        if(ulm==null) {
            throw new PortalException("UserLayout has not been initialized.");
        }

        // find an element with a given id
        Element element = (Element) ulm.getElementById(nodeId);

        if(element==null) {
            throw new PortalException("Element with ID=\""+nodeId+"\" doesn't exist.");
        }
        return UserLayoutNodeDescription.createUserLayoutNodeDescription(element);
    }

    /**
     * Returns the depth of a node in the layout tree.
     *
     * @param nodeId a <code>String</code> value
     * @return a depth value
     * @exception PortalException if an error occurs
     */
    public int getDepth(String nodeId) throws PortalException {
        int depth = 0;
        for (String parentId = getParentId(nodeId); parentId != null; parentId = getParentId(parentId), depth++);
        return depth;
    }

    public IUserLayoutNodeDescription addNode(IUserLayoutNodeDescription node, String parentId, String nextSiblingId) throws PortalException {
        boolean isChannel=false;
        IUserLayoutNodeDescription parent=this.getNode(parentId);
        if(canAddNode(node,parent,nextSiblingId)) {
            // assign new Id

            if(this.getLayoutStore()==null) {
                throw new PortalException("Store implementation has not been set.");
            } else {
                try {
                    if(node instanceof IUserLayoutChannelDescription) {
                        isChannel=true;
                        node.setId(this.getLayoutStore().generateNewChannelSubscribeId(owner));
                    } else {
                        node.setId(this.getLayoutStore().generateNewFolderId(owner));
                    }
                } catch (PortalException pe) {
                    throw pe;
                } catch (Exception e) {
                    throw new PortalException("Exception encountered while generating new usre layout node Id for userId="+this.owner.getID());
                }
            }

            Document ulm=this.getUserLayoutDOM();
            Element childElement=node.getXML(ulm);
            Element parentElement=(Element)ulm.getElementById(parentId);
            if(nextSiblingId==null) {
                parentElement.appendChild(childElement);
            } else {
                Node nextSibling=ulm.getElementById(nextSiblingId);
                parentElement.insertBefore(childElement,nextSibling);
            }
            markLayoutDirty();
            // register element id
          
            childElement.setIdAttribute("ID", true);
            childElement.setAttribute("ID",node.getId());
            

            this.updateCacheKey();
            this.clearMarkings();

            // inform the listeners
            LayoutEvent ev=new LayoutEvent(this,node);
            for(Iterator i=listeners.iterator();i.hasNext();) {
                LayoutEventListener lel=(LayoutEventListener)i.next();
                if(isChannel) {
                    lel.channelAdded(ev);
                } else {
                    lel.folderAdded(ev);
                }
            }
            return node;
        }
        return null;
    }

    public void clearMarkings() {
        markingMode=null;
        markingNode=null;
    }

    public boolean moveNode(String nodeId, String parentId, String nextSiblingId) throws PortalException  {

        IUserLayoutNodeDescription parent=this.getNode(parentId);
        IUserLayoutNodeDescription node=this.getNode(nodeId);
        String oldParentNodeId=getParentId(nodeId);
        if(canMoveNode(node,parent,nextSiblingId)) {
            // must be a folder
            Document ulm=this.getUserLayoutDOM();
            Element childElement=(Element)ulm.getElementById(nodeId);
            Element parentElement=(Element)ulm.getElementById(parentId);
            if(nextSiblingId==null) {
                parentElement.appendChild(childElement);
            } else {
                Node nextSibling=ulm.getElementById(nextSiblingId);
                parentElement.insertBefore(childElement,nextSibling);
            }
            markLayoutDirty();
            clearMarkings();
            updateCacheKey();

            // inform the listeners
            boolean isChannel=false;
            if(node instanceof IUserLayoutChannelDescription) {
                isChannel=true;
            }
            LayoutMoveEvent ev=new LayoutMoveEvent(this,node,oldParentNodeId);
            for(Iterator i=listeners.iterator();i.hasNext();) {
                LayoutEventListener lel=(LayoutEventListener)i.next();
                if(isChannel) {
                    lel.channelMoved(ev);
                } else {
                    lel.folderMoved(ev);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean deleteNode(String nodeId) throws PortalException {
        if(canDeleteNode(nodeId)) {
            IUserLayoutNodeDescription nodeDescription=this.getNode(nodeId);
            String parentNodeId=this.getParentId(nodeId);

            Document ulm=this.getUserLayoutDOM();
            Element childElement=(Element)ulm.getElementById(nodeId);
            Node parent=childElement.getParentNode();
            if(parent!=null) {
                parent.removeChild(childElement);
            } else {
                throw new PortalException("Node \""+nodeId+"\" has a NULL parent ! Owner UID=\""+owner.getID()+"\"");
            }
            markLayoutDirty();
            // clearMarkings(); // this one is questionable
            this.updateCacheKey();

            // inform the listeners
            boolean isChannel=false;
            if(nodeDescription instanceof IUserLayoutChannelDescription) {
                isChannel=true;
            }
            LayoutMoveEvent ev=new LayoutMoveEvent(this,nodeDescription,parentNodeId);
            for(Iterator i=listeners.iterator();i.hasNext();) {
                LayoutEventListener lel=(LayoutEventListener)i.next();
                if(isChannel) {
                    lel.channelDeleted(ev);
                } else {
                    lel.folderDeleted(ev);
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public String getRootFolderId() {
     try {
            if (this.rootNodeId == null) {
                String expression = "//layout/folder";
                XPathFactory fac = XPathFactory.newInstance();
                XPath xpath = fac.newXPath();
                Element rootNode = (Element) xpath.evaluate(expression, this
                        .getUserLayoutDOM(), XPathConstants.NODE);

                this.rootNodeId = rootNode.getAttribute("ID");
      }
     } catch ( Exception e ) {
            log.error("Exception getting root folder id.", e);
       }
        return this.rootNodeId;
    }

    public synchronized boolean updateNode(IUserLayoutNodeDescription node) throws PortalException {
        boolean isChannel=false;
        if(canUpdateNode(node)) {
            // normally here, one would determine what has changed
            // but we'll just make sure that the node type has not
            // changed and then regenerate the node Element from scratch,
            // and attach any children it might have had to it.

            String nodeId=node.getId();
            String nextSiblingId=getNextSiblingId(nodeId);
            Element nextSibling=null;
            if(nextSiblingId!=null) {
                Document ulm=this.getUserLayoutDOM();
                nextSibling=ulm.getElementById(nextSiblingId);
            }

            IUserLayoutNodeDescription oldNode=getNode(nodeId);

            if(oldNode instanceof IUserLayoutChannelDescription) {
                IUserLayoutChannelDescription oldChannel=(IUserLayoutChannelDescription) oldNode;
                if(node instanceof IUserLayoutChannelDescription) {
                    isChannel=true;
                    Document ulm=this.getUserLayoutDOM();
                    // generate new XML Element
                    Element newChannelElement=node.getXML(ulm);
                    Element oldChannelElement=(Element)ulm.getElementById(nodeId);
                    Node parent=oldChannelElement.getParentNode();
                    parent.removeChild(oldChannelElement);
                    parent.insertBefore(newChannelElement,nextSibling);
                    // register new child instead
                    newChannelElement.setIdAttribute("ID", true);
                    newChannelElement.setAttribute("ID",node.getId());

                    // inform the listeners
                    LayoutEvent ev=new LayoutEvent(this,node);
                    for(Iterator i=listeners.iterator();i.hasNext();) {
                        LayoutEventListener lel=(LayoutEventListener)i.next();
                        lel.channelUpdated(ev);
                    }
                } else {
                    throw new PortalException("Change channel to folder is not allowed by updateNode() method!");
                }
            } else {
                 // must be a folder
                IUserLayoutFolderDescription oldFolder=(IUserLayoutFolderDescription) oldNode;
                if(node instanceof IUserLayoutFolderDescription) {
                    Document ulm=this.getUserLayoutDOM();
                    // generate new XML Element
                    Element newFolderElement=node.getXML(ulm);
                    Element oldFolderElement=(Element)ulm.getElementById(nodeId);
                    Node parent=oldFolderElement.getParentNode();

                    // move children
                    Vector children=new Vector();
                    for(Node n=oldFolderElement.getFirstChild(); n!=null;n=n.getNextSibling()) {
                        children.add(n);
                    }

                    for(int i=0;i<children.size();i++) {
                        newFolderElement.appendChild((Node)children.get(i));
                    }

                    // replace the actual node
                    parent.removeChild(oldFolderElement);
                    parent.insertBefore(newFolderElement,nextSibling);
                    // register new child instead
                    newFolderElement.setIdAttribute("ID", true);
                     newFolderElement.setAttribute("ID",node.getId());

                    // inform the listeners
                    LayoutEvent ev=new LayoutEvent(this,node);
                    for(Iterator i=listeners.iterator();i.hasNext();) {
                        LayoutEventListener lel=(LayoutEventListener)i.next();
                        lel.folderUpdated(ev);
                    }
                }
            }
            markLayoutDirty();
            this.updateCacheKey();

            return true;
        } else {
            return false;
        }
    }


    public boolean canAddNode(IUserLayoutNodeDescription node, String parentId, String nextSiblingId) throws PortalException {
        return this.canAddNode(node,this.getNode(parentId),nextSiblingId);
    }

    protected boolean canAddNode(IUserLayoutNodeDescription node,IUserLayoutNodeDescription parent, String nextSiblingId) throws PortalException {
        // make sure sibling exists and is a child of nodeId
        if(nextSiblingId!=null) {
            IUserLayoutNodeDescription sibling=getNode(nextSiblingId);
            if(sibling==null) {
                throw new PortalException("Unable to find a sibling node with id=\""+nextSiblingId+"\"");
            }
            if(!parent.getId().equals(getParentId(nextSiblingId))) {
                throw new PortalException("Given sibling (\""+nextSiblingId+"\") is not a child of a given parentId (\""+parent.getId()+"\")");
            }
        }

        return (parent!=null && parent instanceof IUserLayoutFolderDescription && !parent.isImmutable());
    }

    public boolean canMoveNode(String nodeId, String parentId,String nextSiblingId) throws PortalException {
        return this.canMoveNode(this.getNode(nodeId),this.getNode(parentId),nextSiblingId);
    }

    protected boolean canMoveNode(IUserLayoutNodeDescription node,IUserLayoutNodeDescription parent, String nextSiblingId) throws PortalException {
        // is the current parent immutable ?
        IUserLayoutNodeDescription currentParent=getNode(getParentId(node.getId()));
        if(currentParent==null) {
            throw new PortalException("Unable to determine a parent node for node with id=\""+node.getId()+"\"");
        }
        return (!currentParent.isImmutable() && canAddNode(node,parent,nextSiblingId));
    }

    public boolean canDeleteNode(String nodeId) throws PortalException {
        return canDeleteNode(this.getNode(nodeId));
    }

    protected boolean canDeleteNode(IUserLayoutNodeDescription node) throws PortalException {
        return !(node==null || node.isUnremovable());
    }

    public boolean canUpdateNode(String nodeId) throws PortalException {
        return canUpdateNode(this.getNode(nodeId));
    }

    public boolean canUpdateNode(IUserLayoutNodeDescription node) {
        return !(node==null || node.isImmutable());
    }

    public void markAddTargets(IUserLayoutNodeDescription node) throws PortalException {
        if(node!=null) {
            this.markingMode=ADD_COMMAND;
        } else {
            clearMarkings();
        }
    }

    public void markMoveTargets(String nodeId) throws PortalException {
        if(nodeId!=null) {
            this.markingMode=MOVE_COMMAND;
            this.markingNode=nodeId;
        } else {
            clearMarkings();
        }
    }

    public String getParentId(String nodeId) throws PortalException {
        Document ulm=this.getUserLayoutDOM();
        Element nelement=(Element)ulm.getElementById(nodeId);
        if(nelement!=null) {
            Node parent=nelement.getParentNode();
            if(parent!=null) {
                if(parent.getNodeType()!=Node.ELEMENT_NODE) {
                    throw new PortalException("Node with id=\""+nodeId+"\" is attached to something other then an element node.");
                } else {
                    Element e=(Element) parent;
                    return e.getAttribute("ID");
                }
            } else {
                return null;
            }
        } else {
            throw new PortalException("Node with id=\""+nodeId+"\" doesn't exist.");
        }
    }

    public String getNextSiblingId(String nodeId) throws PortalException {
        Document ulm=this.getUserLayoutDOM();
        Element nelement=(Element)ulm.getElementById(nodeId);
        if(nelement!=null) {
            Node nsibling=nelement.getNextSibling();
            // scroll to the next element node
            while(nsibling!=null && nsibling.getNodeType()!=Node.ELEMENT_NODE){
                nsibling=nsibling.getNextSibling();
            }
            if(nsibling!=null) {
                Element e=(Element) nsibling;
                return e.getAttribute("ID");
            } else {
                return null;
            }
        } else {
            throw new PortalException("Node with id=\""+nodeId+"\" doesn't exist.");
        }
    }

    public String getPreviousSiblingId(String nodeId) throws PortalException {
        Document ulm=this.getUserLayoutDOM();
        Element nelement=(Element)ulm.getElementById(nodeId);
        if(nelement!=null) {
            Node nsibling=nelement.getPreviousSibling();
            // scroll to the next element node
            while(nsibling!=null && nsibling.getNodeType()!=Node.ELEMENT_NODE){
                nsibling=nsibling.getNextSibling();
            }
            if(nsibling!=null) {
                Element e=(Element) nsibling;
                return e.getAttribute("ID");
            } else {
                return null;
            }
        } else {
            throw new PortalException("Node with id=\""+nodeId+"\" doesn't exist.");
        }
    }

    public Enumeration getChildIds(String nodeId) throws PortalException {
        Vector v=new Vector();
        IUserLayoutNodeDescription node=getNode(nodeId);
        if(node instanceof IUserLayoutFolderDescription) {
            Document ulm=this.getUserLayoutDOM();
            Element felement=(Element)ulm.getElementById(nodeId);
            for(Node n=felement.getFirstChild(); n!=null;n=n.getNextSibling()) {
                if(n.getNodeType()==Node.ELEMENT_NODE) {
                    Element e=(Element)n;
                    if(e.getAttribute("ID")!=null) {
                        v.add(e.getAttribute("ID"));
                    }
                }
            }
        }
        return v.elements();
    }

    public String getCacheKey() {
        if(markingMode==null) {
            return this.cacheKey;
        } else {
            if(markingNode!=null) {
                return this.cacheKey+this.markingMode+this.markingNode;
            } else {
                return this.cacheKey+this.markingMode;
            }
        }
    }

    /**
     * This is outright cheating ! We're supposed to analyze the user layout tree
     * and return a key that corresponds uniqly to the composition and the sturcture of the tree.
     * Here we just return a different key wheneever anything changes. So if one was to move a
     * node back and forth, the key would always never (almost) come back to the original value,
     * even though the changes to the user layout are cyclic.
     *
     */
    private void updateCacheKey() {
        this.cacheKey=generateNewCacheKey();
    }

    /**
     * The message digest wrapper is optimized for string processing
     */
    private static final class MessageDigestWrapper {
        private final MessageDigest md_;
        private byte [] ba_;
        
        private static final ThreadLocal perThreadByteArray = new ThreadLocal() {
            protected Object initialValue() {
                return (new byte[256]);
            }
        };
        
        public MessageDigestWrapper(final String algo) throws NoSuchAlgorithmException {
            md_ = MessageDigest.getInstance(algo);
            // to reduce the number of objects created, we use a thread
            // local. Note that we resize if necessary.
            ba_ = (byte[]) perThreadByteArray.get();
        }

        public final void update(final String data) {
            if (data == null) {
                return;
            }

            // resize the cached byte array if necessary
            final int len = data.length();
            if (ba_.length < len) {
                ba_ = new byte[len];
                perThreadByteArray.set(ba_);
            }

            // we use a deprecated method for speed
            data.getBytes(0, len, ba_, 0);
            md_.update(ba_, 0, len);
        }
        
        public final byte [] digest() {
            return (md_.digest());
        }
    }
    
    /**
     * Given a message digest, inspect the node and update the digest with the
     * node name/value and any attribute names/values
     */
    private static final void visit(MessageDigestWrapper md, Node node) {
        int type = node.getNodeType();
        switch (type) {
        case Node.DOCUMENT_NODE:
            break;

        case Node.ELEMENT_NODE:
            md.update(node.getNodeName());
            NamedNodeMap nnm = node.getAttributes();
            if (nnm != null) {
                int len = nnm.getLength();
                Attr attr;
                for (int i = 0; i < len; i++) {
                    attr = (Attr) nnm.item(i);
                    md.update(attr.getNodeName());
                    md.update(attr.getNodeValue());
                }
            }
            break;

        case Node.ENTITY_REFERENCE_NODE:
            md.update(node.getNodeName());
            break;

        case Node.CDATA_SECTION_NODE:
            md.update(node.getNodeValue());
            break;

        case Node.TEXT_NODE:
            md.update(node.getNodeValue());
            break;

        case Node.PROCESSING_INSTRUCTION_NODE:
            md.update(node.getNodeName());
            String data = node.getNodeValue();
            if (data != null && data.length() > 0) {
                md.update(data);
            }
            break;

        }// end of switch
    }// end of visit

    /**
     * Walk through a DOM (non-recursively) and update the message
     * digest with element node names/values and attribute names/values
     */
    private static final void updateDigest(MessageDigestWrapper md, Node node) {
        Node currentNode = node;

        while (currentNode != null) {
            visit(md, currentNode); // pre order

            // Move down to first child
            Node nextNode = currentNode.getFirstChild();
            if (nextNode != null) {
                currentNode = nextNode;
                continue;
            }

            // No child nodes, so walk tree
            while (currentNode != null) {
                // visit(md, currentNode); // post order

                // Move to sibling if possible.
                nextNode = currentNode.getNextSibling();
                if (nextNode != null) {
                    currentNode = nextNode;
                    break;
                }

                // Move up
                if (currentNode == node) {
                    currentNode = null;
                } else {
                    currentNode = currentNode.getParentNode();
                }
            }
        }
    }

    /**
     * The goal of this method is to generate a unique key
     * based upon a given layout. A users layout is represented
     * by the getUserLayoutDOM() result. We then walk through
     * the DOM, calculating an MD5 hash. This then becomes the
     * key to represent a layout. Two users, with the same layout
     * should result in the same cache key.
     */
    private String generateNewCacheKey() {
        String cacheKey = null;
        long startTime = System.currentTimeMillis();
        try {          
            MessageDigestWrapper md = new MessageDigestWrapper("MD5");
            updateDigest(md, this.getUserLayoutDOM());
            byte[] digest = md.digest();
            StringBuffer sb = new StringBuffer(32);
            for (int i=0; i<digest.length; i++) {
                if (digest[i] < 0x10 && digest[i] >= 0x0) {
                    // add a leading 0
                    sb.append("0");
                }
                sb.append(Integer.toHexString((0xff & digest[i])));
            }
            cacheKey = sb.toString();
        } catch (Exception e) {
          log.error("SimpleUserLayoutManager::generateNewCacheKey() : unable to generate new cache key, using default", e);
          cacheKey = Long.toString(rnd.nextLong());
        }

        if (log.isDebugEnabled()) {
            log.debug("SimpleUserLayoutManager::generateNewCacheKey() : generating new cache key took " + (System.currentTimeMillis()-startTime) + "ms");
        }
        return cacheKey;
    }

    
    public int getLayoutId() {
        return profile.getLayoutId();
    }

    /**
     * Returns a subscription id given a functional name.
     *
     * @param fname  the functional name to lookup.
     * @return a <code>String</code> subscription id.
     */
    public String getSubscribeId(String fname) throws PortalException {
        try {
            String expression = "//channel[@fname=\'"+fname+"\']";
            XPathFactory fac = XPathFactory.newInstance();
            XPath xpath = fac.newXPath();
            Element fnameNode = (Element) xpath.evaluate(expression, 
                    this.getUserLayoutDOM(), XPathConstants.NODE);
            if(fnameNode!=null) {
                return fnameNode.getAttribute("ID");
            } else {
                return null;
            }
        } catch (XPathExpressionException e) {
            log.error( "SimpleUserLayoutManager::getSubcribeId() : " +
                    "encountered exception while trying to identify subscribe channel id for the fname=\""+fname+"\" : ", e);
            return null;
        }
    }

    public boolean addLayoutEventListener(LayoutEventListener l) {
        return listeners.add(l);
    }
    public boolean removeLayoutEventListener(LayoutEventListener l) {
        return listeners.remove(l);
    }

    /**
     * A factory method to create an empty <code>IUserLayoutNodeDescription</code> instance
     *
     * @param nodeType a node type value
     * @return an <code>IUserLayoutNodeDescription</code> instance
     * @exception PortalException if the error occurs.
     */
    public IUserLayoutNodeDescription createNodeDescription( int nodeType ) throws PortalException {
            switch ( nodeType ) {
              case IUserLayoutNodeDescription.FOLDER:
                return new UserLayoutFolderDescription();
              case IUserLayoutNodeDescription.CHANNEL:
                return new UserLayoutChannelDescription();
              default:
                return null;
            }
    }

    protected boolean isLayoutDirty() { return dirtyState; }
    private void markLayoutDirty() { dirtyState=true; }
    private void clearDirtyFlag() { dirtyState=false; }
}
