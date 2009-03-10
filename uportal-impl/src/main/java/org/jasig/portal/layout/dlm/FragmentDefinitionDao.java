/**
 * Copyright (c) 2000-2009, Jasig, Inc.
 * See license distributed with this file and available online at
 * https://www.ja-sig.org/svn/jasig-parent/tags/rel-10/license-header.txt
 */
package org.jasig.portal.layout.dlm;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.lang.Validate;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class FragmentDefinitionDao implements IFragmentDefinitionDao {

    // Static Members
    private static final String GET_ALL_FRAGMENTS = "SELECT x FROM FragmentDefinition x ORDER BY x.precedence DESC";
    private static final String FIND_FRAGMENT_BY_NAME = "SELECT x FROM FragmentDefinition x WHERE x.name = :name";

    // Instance Members.
    private EntityManager entityManager;

    /**
     * @param entityManager the entityManager to set
     */
    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    @SuppressWarnings("unchecked")
    public List<FragmentDefinition> getAllFragments() {

        final Query query = this.entityManager.createQuery(GET_ALL_FRAGMENTS);
        query.setHint("org.hibernate.cacheable", true);
        final List<FragmentDefinition> rslt = query.getResultList();
        return rslt;
        
    }

    @SuppressWarnings("unchecked")
    public FragmentDefinition getFragmentDefinition(String name) {
        
        final Query query = this.entityManager.createQuery(FIND_FRAGMENT_BY_NAME);
        query.setParameter("name", name);
        query.setHint("org.hibernate.cacheable", true);
        query.setMaxResults(1);
        
        final List<FragmentDefinition> list = query.getResultList();
        final FragmentDefinition rslt = (FragmentDefinition) DataAccessUtils.uniqueResult(list);
        return rslt;
        
    }

    @Transactional
    public void updateFragmentDefinition(FragmentDefinition fd) {
        
        Validate.notNull(fd, "FragmentDefinition can not be null");
        this.entityManager.merge(fd);
        
    }

    @Transactional
    public void removeFragmentDefinition(FragmentDefinition fd) {
        
        Validate.notNull(fd, "FragmentDefinition can not be null");
        this.entityManager.remove(fd);
        
    }

}
