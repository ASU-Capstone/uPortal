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

package org.jasig.portal.io.xml;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import org.apache.commons.lang.Validate;

/**
 * Describes the type and version of a portal data XML file.
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public class PortalDataKey {
    /**
     * The XML Attribute on the root element that contains the cernunnos script
     * that denotes the file version. Used for data files from 3.2 and earlier.
     */
    public static final QName SCRIPT_ATTRIBUTE_NAME = new QName("script");
    /**
     * The version of the data file, used for data files form 4.0 and later.
     */
    public static final QName VERSION_ATTRIBUTE_NAME = new QName("version");
    
    private QName name;
    private String script;
    private String version;
    
    public PortalDataKey(StartElement startElement) {
        this.name = startElement.getName();
        this.script = getAttributeValue(startElement, SCRIPT_ATTRIBUTE_NAME);
        this.version = getAttributeValue(startElement, VERSION_ATTRIBUTE_NAME);
    }

    public PortalDataKey(QName name, String script, String version) {
        Validate.notNull(name);
        this.name = name;
        this.script = script;
        this.version = version;
    }

    protected String getAttributeValue(StartElement startElement, QName name) {
        final Attribute versionAttr = startElement.getAttributeByName(name);
        if (versionAttr != null) {
            return versionAttr.getValue();
        }

        return null;
    }

    public QName getName() {
        return this.name;
    }

    public String getScript() {
        return this.script;
    }

    public String getVersion() {
        return this.version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.script == null) ? 0 : this.script.hashCode());
        result = prime * result + ((this.version == null) ? 0 : this.version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PortalDataKey other = (PortalDataKey) obj;
        if (this.name == null) {
            if (other.name != null)
                return false;
        }
        else if (!this.name.equals(other.name))
            return false;
        if (this.script == null) {
            if (other.script != null)
                return false;
        }
        else if (!this.script.equals(other.script))
            return false;
        if (this.version == null) {
            if (other.version != null)
                return false;
        }
        else if (!this.version.equals(other.version))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("<");
        builder.append(this.name);
        if (this.script != null) {
            builder.append(" script=\"").append(this.script).append("\"");
        }
        if (this.version != null) {
            builder.append(" version=\"").append(this.version).append("\"");
        }
        builder.append(">");
        
        return builder.toString();
    }
}
