/* Copyright 2007 The JA-SIG Collaborative.  All rights reserved.
*  See license distributed with this file and
*  available online at http://www.uportal.org/license.html
*/

package org.jasig.portal.tools.chanpub;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.dom4j.Document;
import org.dom4j.Element;

/**
 * Used by the import-channel Cernunnos script
 */
public final class UrlChannelPublisher {

    public static void publishChannel(Element m) {

        try {

            m.remove(m.attribute("script"));
            Document d = m.getDocument();
            d.addDocType("channel-definition", null, "channelDefinition.dtd");

            String xml = d.asXML();
            InputStream inpt = new ByteArrayInputStream(xml.getBytes());
            IChannelPublisher pub = ChannelPublisher.getCommandLineInstance();
            pub.publishChannel(inpt);

        } catch (Throwable t) {
            String msg = "Error publishing the channel definition.";
            throw new RuntimeException(msg, t);
        }

    }

}