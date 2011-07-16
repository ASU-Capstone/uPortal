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

package org.jasig.portal.layout;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.jasig.portal.layout.om.ILayoutAttributeDescriptor;
import org.jasig.portal.layout.om.IOutputPropertyDescriptor;
import org.jasig.portal.layout.om.IStylesheetData;
import org.jasig.portal.layout.om.IStylesheetData.Scope;
import org.jasig.portal.layout.om.IStylesheetDescriptor;
import org.jasig.portal.layout.om.IStylesheetParameterDescriptor;
import org.jasig.portal.layout.om.IStylesheetUserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Implementation of stylesheet user preferences that composites data from all of the {@link Scope}s for read
 * operations and stores the data in the correct scope based on the configuration in the corresponding
 * {@link IStylesheetDescriptor}. Deals with lazy initialization of stored objects only when data is being set.
 * 
 * If persistentReadOnly is true the {@link Scope#PERSISTENT} {@link IStylesheetUserPreferences} will only ever be
 * read from, all persistent writes will go to the {@link Scope#SESSION} {@link IStylesheetUserPreferences} 
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
class CompositeStylesheetUserPreferences implements IStylesheetUserPreferences {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final IStylesheetDescriptor stylesheetDescriptor;
    private final Map<Scope, IStylesheetUserPreferences> stylesheetUserPreferences;
    private final boolean persistentReadOnly;
    private final IStylesheetUserPreferences distributedStylesheetUserPreferences;
    
    public CompositeStylesheetUserPreferences(
            IStylesheetDescriptor stylesheetDescriptor, 
            Map<Scope, IStylesheetUserPreferences> stylesheetUserPreferences,
            IStylesheetUserPreferences distributedStylesheetUserPreferences,
            boolean readOnlyPersistent) {
        
        this.stylesheetDescriptor = stylesheetDescriptor;
        this.stylesheetUserPreferences = stylesheetUserPreferences;
        this.distributedStylesheetUserPreferences = distributedStylesheetUserPreferences;
        this.persistentReadOnly = readOnlyPersistent;
    }
    
    protected IStylesheetDescriptor getStylesheetDescriptor() {
        return this.stylesheetDescriptor;
    }
    
    protected Map<Scope, IStylesheetUserPreferences> getComponentPreferences() {
        return this.stylesheetUserPreferences;
    }
    
    @Override
    public long getId() {
        final IStylesheetUserPreferences stylesheetUserPreferences = this.getStylesheetUserPreferences(Scope.PERSISTENT, false);
        if (stylesheetUserPreferences != null) {
            return stylesheetUserPreferences.getId();
        }
        
        return -1;
    }
    
    @Override
    public long getStylesheetDescriptorId() {
        return this.stylesheetDescriptor.getId();
    }
    
    @Override
    public void setStylesheetUserPreferences(IStylesheetUserPreferences stylesheetUserPreferences) {
        throw new UnsupportedOperationException("Setting all preferences on a composite prefernces object is not supported");
    }

    @Override
    public String getOutputProperty(String name) {
        final IOutputPropertyDescriptor outputPropertyDescriptor = this.stylesheetDescriptor.getOutputPropertyDescriptor(name);
        if (outputPropertyDescriptor == null) {
            logger.warn("Attempted to get output property {} but no such output property is defined in stylesheet descriptor {}. null will be returned", new Object[] {name, this.stylesheetDescriptor.getName()});
            return null;
        }
        
        final Scope scope = outputPropertyDescriptor.getScope();
        final IStylesheetUserPreferences stylesheetUserPreferences = this.getStylesheetUserPreferences(scope, false);
        if (stylesheetUserPreferences != null) {
            final String outputProperty = stylesheetUserPreferences.getOutputProperty(name);
            if (outputProperty != null) {
                return outputProperty;
            }
        }
        
        return outputPropertyDescriptor.getDefaultValue();
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.layout.om.IStylesheetUserPreferences#setOutputProperty(java.lang.String, java.lang.String)
     */
    @Override
    public String setOutputProperty(String name, String value) {
        final IOutputPropertyDescriptor outputPropertyDescriptor = this.stylesheetDescriptor.getOutputPropertyDescriptor(name);
        if (outputPropertyDescriptor == null) {
            logger.warn("Attempted to set output property {}={} but no such output property is defined in stylesheet descriptor {}. It will be ignored", new Object[] {name, value, this.stylesheetDescriptor.getName()});
            return null;
        }
        
        final Scope scope = this.getWriteScope(outputPropertyDescriptor);
        final IStylesheetUserPreferences stylesheetUserPreferences = this.getStylesheetUserPreferences(scope, true);
        
        final String defaultValue = outputPropertyDescriptor.getDefaultValue();
        if (this.compareValues(value, defaultValue)) {
            return stylesheetUserPreferences.removeOutputProperty(name);
        }
        
        return stylesheetUserPreferences.setOutputProperty(name, value);
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.layout.om.IStylesheetUserPreferences#removeOutputProperty(java.lang.String)
     */
    @Override
    public String removeOutputProperty(String name) {
        final IOutputPropertyDescriptor outputPropertyDescriptor = this.stylesheetDescriptor.getOutputPropertyDescriptor(name);
        if (outputPropertyDescriptor == null) {
            logger.warn("Attempted to remove output property {} but no such output property is defined in stylesheet descriptor {}. It will be ignored", new Object[] {name, this.stylesheetDescriptor.getName()});
            return null;
        }
        
        final Scope scope = this.getWriteScope(outputPropertyDescriptor);
        final IStylesheetUserPreferences stylesheetUserPreferences = this.getStylesheetUserPreferences(scope, false);
        if (stylesheetUserPreferences == null) {
            return null;
        }
        
        return stylesheetUserPreferences.removeOutputProperty(name);
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.layout.om.IStylesheetUserPreferences#getOutputProperties()
     */
    @Override
    public Properties getOutputProperties() {
        return this.buildCompositeMap(
            new Function<Object, Properties>() {
                @Override
                public Properties apply(Object input) {
                    return new Properties();
                }
            },
            new PreferenceValueFunction<Properties>() {
                @Override
                public Properties getPreferences(IStylesheetUserPreferences preferences) {
                    return preferences.getOutputProperties();
                }
            },
            stylesheetDescriptor.getOutputPropertyDescriptors());
    }

    @Override
    public void clearOutputProperties() {
        this.clearComponentPreferences(new Function<IStylesheetUserPreferences, Object>() {
            @Override
            public Object apply(IStylesheetUserPreferences componentPreferences) {
                componentPreferences.clearOutputProperties();
                return null;
            }
        });
    }

    @Override
    public String getStylesheetParameter(String name) {
        final IStylesheetParameterDescriptor stylesheetParameterDescriptor = this.stylesheetDescriptor.getStylesheetParameterDescriptor(name);
        if (stylesheetParameterDescriptor == null) {
            logger.warn("Attempted to get stylesheet parameter {} but no such stylesheet parameter is defined in stylesheet descriptor {}. null will be returned", new Object[] {name, this.stylesheetDescriptor.getName()});
            return null;
        }
        
        final Scope scope = stylesheetParameterDescriptor.getScope();
        final IStylesheetUserPreferences stylesheetUserPreferences = this.getStylesheetUserPreferences(scope, false);
        if (stylesheetUserPreferences != null) {
            final String stylesheetParameter = stylesheetUserPreferences.getStylesheetParameter(name);
            if (stylesheetParameter != null) {
                return stylesheetParameter;
            }
        }
        
        return stylesheetParameterDescriptor.getDefaultValue();
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.layout.om.IStylesheetUserPreferences#setStylesheetParameter(java.lang.String, java.lang.String)
     */
    @Override
    public String setStylesheetParameter(String name, String value) {
        final IStylesheetParameterDescriptor stylesheetParameterDescriptor = this.stylesheetDescriptor.getStylesheetParameterDescriptor(name);
        if (stylesheetParameterDescriptor == null) {
            logger.warn("Attempted to set stylesheet parameter {}={} but no such stylesheet parameter is defined in stylesheet descriptor {}. It will be ignored", new Object[] {name, value, this.stylesheetDescriptor.getName()});
            return null;
        }
        
        final Scope scope = this.getWriteScope(stylesheetParameterDescriptor);
        final IStylesheetUserPreferences stylesheetUserPreferences = this.getStylesheetUserPreferences(scope, true);
        
        final String defaultValue = stylesheetParameterDescriptor.getDefaultValue();
        if (this.compareValues(value, defaultValue)) {
            return stylesheetUserPreferences.removeStylesheetParameter(name);
        }

        return stylesheetUserPreferences.setStylesheetParameter(name, value);
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.layout.om.IStylesheetUserPreferences#removeStylesheetParameter(java.lang.String)
     */
    @Override
    public String removeStylesheetParameter(String name) {
        final IStylesheetParameterDescriptor stylesheetParameterDescriptor = this.stylesheetDescriptor.getStylesheetParameterDescriptor(name);
        if (stylesheetParameterDescriptor == null) {
            logger.warn("Attempted to remove stylesheet parameter {} but no such stylesheet parameter is defined in stylesheet descriptor {}. It will be ignored", new Object[] {name, this.stylesheetDescriptor.getName()});
            return null;
        }
        
        final Scope scope = this.getWriteScope(stylesheetParameterDescriptor);
        final IStylesheetUserPreferences stylesheetUserPreferences = this.getStylesheetUserPreferences(scope, false);
        if (stylesheetUserPreferences == null) {
            return null;
        }
        
        return stylesheetUserPreferences.removeStylesheetParameter(name);
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.layout.om.IStylesheetUserPreferences#getStylesheetParameters()
     */
    @Override
    public Map<String, String> getStylesheetParameters() {
        return this.buildCompositeMap(
            new Function<Object, Map<String, String>>() {
                @Override
                public Map<String, String> apply(Object input) {
                    return new LinkedHashMap<String, String>();
                }
            },
            new PreferenceValueFunction<Map<String, String>>() {
                @Override
                public Map<String, String> getPreferences(IStylesheetUserPreferences preferences) {
                    return preferences.getStylesheetParameters();
                }
            },
            stylesheetDescriptor.getStylesheetParameterDescriptors());
    }

    @Override
    public void clearStylesheetParameters() {
        this.clearComponentPreferences(new Function<IStylesheetUserPreferences, Object>() {
            @Override
            public Object apply(IStylesheetUserPreferences componentPreferences) {
                componentPreferences.clearStylesheetParameters();
                return null;
            }
        });
    }

    @Override
    public String getLayoutAttribute(String nodeId, String name) {
        final ILayoutAttributeDescriptor layoutAttributeDescriptor = this.stylesheetDescriptor.getLayoutAttributeDescriptor(name);
        if (layoutAttributeDescriptor == null) {
            logger.warn("Attempted to get layout attribute {} for ID=\"{}\" but no such stylesheet parameter is defined in stylesheet descriptor {}. Null will be returned", new Object[] {name, nodeId, this.stylesheetDescriptor.getName()});
            return null;
        }

        final Scope scope = layoutAttributeDescriptor.getScope();
        final IStylesheetUserPreferences stylesheetUserPreferences = this.getStylesheetUserPreferences(scope, false);
        if (stylesheetUserPreferences != null) {
            final String layoutAttribute = stylesheetUserPreferences.getLayoutAttribute(nodeId, name);
            if (layoutAttribute != null) {
                return layoutAttribute;
            }
        }
        
        if (this.distributedStylesheetUserPreferences != null) {
            final String layoutAttribute = this.distributedStylesheetUserPreferences.getLayoutAttribute(nodeId, name);
            if (layoutAttribute != null) {
                return layoutAttribute;
            }
        }
        
        return layoutAttributeDescriptor.getDefaultValue();
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.layout.om.IStylesheetUserPreferences#setLayoutAttribute(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public String setLayoutAttribute(String nodeId, String name, String value) {
        final ILayoutAttributeDescriptor layoutAttributeDescriptor = this.stylesheetDescriptor.getLayoutAttributeDescriptor(name);
        if (layoutAttributeDescriptor == null) {
            logger.warn("Attempted to set layout attribute {}={} on node with ID=\"{}\" but no such stylesheet parameter is defined in stylesheet descriptor {}. It will be ignored.", new Object[] {name, value, nodeId, this.stylesheetDescriptor.getName()});
            return null;
        }

        final Scope scope = this.getWriteScope(layoutAttributeDescriptor);
        final IStylesheetUserPreferences stylesheetUserPreferences = this.getStylesheetUserPreferences(scope, true);
        
        final String defaultValue = layoutAttributeDescriptor.getDefaultValue();
        if (this.compareValues(value, defaultValue)) {
            return stylesheetUserPreferences.removeLayoutAttribute(nodeId, name);
        }

        return stylesheetUserPreferences.setLayoutAttribute(nodeId, name, value);
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.layout.om.IStylesheetUserPreferences#removeLayoutAttribute(java.lang.String, java.lang.String)
     */
    @Override
    public String removeLayoutAttribute(String nodeId, String name) {
        final ILayoutAttributeDescriptor layoutAttributeDescriptor = this.stylesheetDescriptor.getLayoutAttributeDescriptor(name);
        if (layoutAttributeDescriptor == null) {
            logger.warn("Attempted to remove layout attribute {} for ID=\"{}\" but no such stylesheet parameter is defined in stylesheet descriptor {}. It will be ignored.", new Object[] {name, nodeId, this.stylesheetDescriptor.getName()});
            return null;
        }
        
        final Scope scope = this.getWriteScope(layoutAttributeDescriptor);
        final IStylesheetUserPreferences stylesheetUserPreferences = this.getStylesheetUserPreferences(scope, false);
        if (stylesheetUserPreferences != null) {
            final String layoutAttribute = stylesheetUserPreferences.removeLayoutAttribute(nodeId, name);
            if (layoutAttribute != null) {
                return layoutAttribute;
            }
        }
        
        if (this.distributedStylesheetUserPreferences != null) {
            return this.distributedStylesheetUserPreferences.removeLayoutAttribute(nodeId, name);
        }
        
        return null;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.layout.om.IStylesheetUserPreferences#getLayoutAttributes(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getLayoutAttributes(final String nodeId) {
        final Map<String, String> distributedLayoutAttributes;
        if (distributedStylesheetUserPreferences != null) {
            distributedLayoutAttributes = distributedStylesheetUserPreferences.getLayoutAttributes(nodeId);
        }
        else {
            distributedLayoutAttributes = Collections.emptyMap();
        }
        
        return this.buildCompositeMap(
            new Function<Object, Map<String, String>>() {
                @Override
                public Map<String, String> apply(Object input) {
                    return new LinkedHashMap<String, String>();
                }
            },
            new PreferenceValueFunction<Map<String, String>>() {
                @Override
                public Map<String, String> getPreferences(IStylesheetUserPreferences preferences) {
                    return preferences.getLayoutAttributes(nodeId);
                }
            },
            stylesheetDescriptor.getLayoutAttributeDescriptors(),
            distributedLayoutAttributes);
    }
    
    /* (non-Javadoc)
     * 
     * Not the most efficient impl, but reduces duplicate code.
     * 
     * @see org.jasig.portal.layout.om.IStylesheetUserPreferences#getAllLayoutAttributes()
     */
    @Override
    public Map<String, Map<String, String>> getAllLayoutAttributes() {
        final LinkedHashMap<String, Map<String, String>> compositeMap = new LinkedHashMap<String, Map<String, String>>();
        
        this.addLayoutAttributes(compositeMap, distributedStylesheetUserPreferences);
        
        for (final Scope scope : Scope.values()) {
            final IStylesheetUserPreferences stylesheetUserPreferences = this.getStylesheetUserPreferences(scope, false);
            this.addLayoutAttributes(compositeMap, stylesheetUserPreferences);
        }
        
        return compositeMap;
    }

    /**
     * Adds all layout attributes from the specified preferences into the composite map (as long as they don't already exist).
     * Uses {@link #getLayoutAttributes(String)}
     */
    protected void addLayoutAttributes(final Map<String, Map<String, String>> compositeMap, final IStylesheetUserPreferences stylesheetUserPreferences) {
        if (stylesheetUserPreferences == null) {
            return;
        }
        
        final Map<String, Map<String, String>> allLayoutAttributes = stylesheetUserPreferences.getAllLayoutAttributes();
        
        for (final String nodeId : allLayoutAttributes.keySet()) {
            if (compositeMap.containsKey(nodeId)) {
                continue;
            }
            
            final Map<String, String> layoutAttributes = this.getLayoutAttributes(nodeId);
            compositeMap.put(nodeId, layoutAttributes);
        }
    }

    @Override
    public void clearLayoutAttributes(final String nodeId) {
        this.clearComponentPreferences(new Function<IStylesheetUserPreferences, Object>() {
            @Override
            public Object apply(IStylesheetUserPreferences componentPreferences) {
                componentPreferences.clearLayoutAttributes(nodeId);
                return null;
            }
        });
    }

    @Override
    public void clearAllLayoutAttributes() {
        this.clearComponentPreferences(new Function<IStylesheetUserPreferences, Object>() {
            @Override
            public Object apply(IStylesheetUserPreferences componentPreferences) {
                componentPreferences.clearAllLayoutAttributes();
                return null;
            }
        });
    }

    /**
     * The scope to use for the descriptor, handles persistentReadOnly logic
     */
    protected Scope getWriteScope(final IStylesheetData descriptor) {
        Scope scope = descriptor.getScope();
        if (this.persistentReadOnly && Scope.PERSISTENT == scope) {
            scope = Scope.SESSION;
        }
        return scope;
    }

    /**
     * Get the stylesheet preferences for the specified scope
     */
    protected IStylesheetUserPreferences getStylesheetUserPreferences(Scope scope, boolean create) {
        IStylesheetUserPreferences stylesheetUserPreferences = this.stylesheetUserPreferences.get(scope);
        if (stylesheetUserPreferences == null) {
            stylesheetUserPreferences = new StylesheetUserPreferencesImpl(this.stylesheetDescriptor.getId());
            this.stylesheetUserPreferences.put(scope, stylesheetUserPreferences);
        }
        return stylesheetUserPreferences;
    }

    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected <M extends Map, F> M buildCompositeMap(Function<F, M> mapMaker, PreferenceValueFunction<M> preferenceValueFunction, Collection<? extends IStylesheetData> defaults, M... extraDataMaps) {
        final M compositeMap = mapMaker.apply(null);
        
        //Add data from the stylesheet descriptor
        for (final IStylesheetData defaultData : defaults) {
            final String defaultValue = defaultData.getDefaultValue();
            final String name = defaultData.getName();
            compositeMap.put(name, defaultValue);
        }
        
        //Add any pre-existing data to the composite map
        for (final M existingValue : extraDataMaps) {
            compositeMap.putAll(existingValue);
        }
        
        //Iterate through scopes adding data from each stylesheet user prefs objects
        for (final Scope scope : Scope.values()) {
            final IStylesheetUserPreferences stylesheetUserPreferences = this.getStylesheetUserPreferences(scope, false);
            if (stylesheetUserPreferences != null) {
                final Map preferences = preferenceValueFunction.getPreferences(stylesheetUserPreferences);
                compositeMap.putAll(preferences);
            }
        }
        
        return compositeMap;
    }
    
    protected void clearComponentPreferences(Function<IStylesheetUserPreferences, Object> clearPreferences) {
        for (final Entry<Scope, IStylesheetUserPreferences> entry : this.stylesheetUserPreferences.entrySet()) {
            final Scope scope = entry.getKey();
            if (this.persistentReadOnly && Scope.PERSISTENT == scope) {
                //Skip clearing persistent preferences if they are marked read-only
                continue;
            }
            
            final IStylesheetUserPreferences componentPreferences = entry.getValue();
            clearPreferences.apply(componentPreferences);
        }
    }
    
    protected boolean compareValues(String value, String defaultValue) {
        return value == defaultValue || (value != null && value.equals(defaultValue));
    }
    
    private interface PreferenceValueFunction<V> {
        public V getPreferences(IStylesheetUserPreferences preferences);
    }
}
