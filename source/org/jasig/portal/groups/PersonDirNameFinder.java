/* Copyright 2002, 2005 The JA-SIG Collaborative.  All rights reserved.
*  See license distributed with this file and
*  available online at http://www.uportal.org/license.html
*/


package  org.jasig.portal.groups;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.jasig.portal.services.PersonDirectory;
import org.jasig.portal.utils.SoftHashMap;


/**
 * PersonDirectory implementation of <code>IEntityNameFinder</code> for <code>IPersons</code>.
 * @author Alex Vigdor
 * @version $Revision$
 */
public class PersonDirNameFinder
        implements IEntityNameFinder {
    // Singleton instance:
    private static IEntityNameFinder singleton;
    private static PersonDirectory pd;
    // Our cache of entity names:
    private final SoftHashMap names;

    /**
     * ReferenceIPersonNameFinder constructor comment.
     */
    private PersonDirNameFinder () throws SQLException
    {
        super();
        pd = PersonDirectory.instance();
        names = new SoftHashMap();
    }


    public String getName (String key) throws Exception {
        String name = (String) this.names.get(key);
        
        if (name == null && key !=null) {
            // cached name not found, get name from underlying DAO.
            name = primGetName(key);
            // cache the name
            this.names.put(key, name);
        }
        return  name;
    }

    /**
     * Given an array of keys, returns the names of the entities.  If a key
     * is not found, its name will be null.
     * @param keys java.lang.String[]
     */
    public java.util.Map getNames (java.lang.String[] keys) throws Exception {
        Map selectedNames = new HashMap();
        for (int i = 0; i < keys.length; i++) {
            String name = getName(keys[i]);
            selectedNames.put(keys[i], name);
        }
        return  selectedNames;
    }

    /**
     * Returns the entity type for this <code>IEntityFinder</code>.
     * @return java.lang.Class
     */
    public Class getType () {
        return  org.jasig.portal.security.IPerson.class;
    }

    /**
     * put your documentation comment here
     * @param key
     * @return
     * @exception java.sql.SQLException
     */
    private String primGetName (String key) throws java.sql.SQLException {
        String name = key;
        Hashtable userInfo = pd.getUserDirectoryInformation(name);
        Object displayName = userInfo.get("displayName");
        String displayNameStr = "";
        if (displayName != null)
        {
            if (displayName instanceof java.util.List)
            {
                List displayNameList = (List) displayName;
                if (! displayNameList.isEmpty() )
                    { displayNameStr = (String)displayNameList.get(0); } 
            }
            else displayNameStr = (String)displayName;
        
            if (! displayNameStr.trim().equals("")) 
                { name = displayNameStr; }
        }
        return  name;
    }


    /**
     * @return IEntityNameFinder
     */
    public static synchronized IEntityNameFinder singleton () throws SQLException {
        if (singleton == null) {
            singleton = new PersonDirNameFinder();
        }
        return  singleton;
    }

    /**
     * Returns a String that represents the value of this object.
     * @return a string representation of the receiver
     */
    public String toString () {
        return  "IEntityNameFinder for " + getType().getName();
    }
}



