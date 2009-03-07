/**
 * Copyright (c) 2000-2009, Jasig, Inc.
 * See license distributed with this file and available online at
 * https://www.ja-sig.org/svn/jasig-parent/tags/rel-10/license-header.txt
 */
package org.jasig.portal.layout.dlm;

import java.util.List;

import org.jasig.portal.spring.PortalApplicationContextLocator;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

/**
 * Implementation of {@link ConfigurationLoader} that loads 
 * {@link FragmentDefinition} objects from the database using JPA/Hibernate.  
 * You can enable this feature by setting the <code>ConfigurationLoader.impl</code> 
 * property in dlm.xml.
 * 
 * @author awills
 */
public class RDBMConfigurationLoader extends ConfigurationLoader {
    
    // Instance Members.
    private IFragmentDefinitionDao fragmentDao = null;

    @Override
    public FragmentDefinition[] getFragments() {
        List<FragmentDefinition> frags = fragmentDao.getAllFragments();
        FragmentDefinition[] rslt = new FragmentDefinition[frags.size()];
        for (int i=0; i < frags.size(); i++) {
            FragmentDefinition fd = frags.get(i);
            fd.setIndex(i);
            rslt[i] = fd;
        }
        return rslt;
    }

    @Override
    public synchronized void init(Document doc) {

        // Do this stuff only once...
        if (this.fragmentDao == null) {
            // There's nothing in the dlm.xml file we care about in this Java 
            // class, but we can use this opportunity to get a reference to 
            // the ApplicationContext...
            ApplicationContext ctx = PortalApplicationContextLocator.getApplicationContext();
            // Also resolve the 'fragmentDefinitionDao' bean...
            fragmentDao = (IFragmentDefinitionDao) ctx.getBean("fragmentDefinitionDao");
        }
        
    }

}
