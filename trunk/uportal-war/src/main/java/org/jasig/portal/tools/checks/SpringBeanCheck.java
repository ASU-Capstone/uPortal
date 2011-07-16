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

package org.jasig.portal.tools.checks;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Check that a particular named Spring bean is defined.
 *
 * @version $Revision$ $Date$
 * @since uPortal 2.5
 */
public class SpringBeanCheck extends BaseCheck implements ApplicationContextAware {

    private final String beanName;
    private final String requiredBeanTypeClassName;
    private ApplicationContext applicationContext;

    private CheckResult nullBeanNameResult = CheckResult
            .createFailure("This check is fatally misconfigured. It hasn't been told the name of the Spring bean for which it should be checking.",
                    "Fix the check configuration (probably a List named 'checks' declared in checksContext.xml).");

    public SpringBeanCheck(String beanName, String requiredBeanTypeClassName) {
        this.beanName = beanName;
        this.requiredBeanTypeClassName = requiredBeanTypeClassName;
        
        if (this.beanName == null) {
            this.setDescription("Fatally misconfigured check doesn't do anything because it hasn't been given a bean name to check.");
        }
        else if (this.requiredBeanTypeClassName == null) {
            this.setDescription("Checks for the presence of the Spring bean named [" + this.beanName + "].");
        }
        else {
            this.setDescription("Checks for the presence of the Spring bean named [" + this.beanName + "] and that it is an instance of class [" + this.requiredBeanTypeClassName + "]");
        }
    }

    public SpringBeanCheck(String beanName) {
        this(beanName, null);
    }

    /* (non-Javadoc)
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    
    /* (non-Javadoc)
     * @see org.jasig.portal.tools.checks.BaseCheck#doCheckInternal()
     */
    @Override
    protected CheckResult doCheckInternal() {
        if (this.beanName == null) {
            return this.nullBeanNameResult;
        }

        // either verify that the bean exists (and is of the desired type if specified)
        // or translate the resulting RuntimeException into an explanatory
        // CheckResult.
        try {
            if (this.requiredBeanTypeClassName == null) {
                this.applicationContext.getBean(this.beanName);
                return CheckResult.createSuccess("Bean with name [" + this.beanName + "] was present.");
            }

            this.applicationContext.getBean(this.beanName, Class.forName(this.requiredBeanTypeClassName));
            return CheckResult.createSuccess("Bean with name [" + this.beanName + "] was present and of class ["
                    + this.requiredBeanTypeClassName + "]");
        }
        catch (NoSuchBeanDefinitionException nsbde) {
            String remediationAdvice;
            if (this.requiredBeanTypeClassName == null) {
                remediationAdvice = "Declare a singleton bean of name [" + this.beanName
                        + "] in a Spring bean definition file in /properties/contexts/.";
            }
            else {
                remediationAdvice = "Declare a singleton bean of name [" + this.beanName + "] and of type ["
                        + this.requiredBeanTypeClassName
                        + "] in a Spring bean definition file mapped in /properties/contexts/.";
            }
            return CheckResult.createFailure("There is no bean named [" + this.beanName
                    + "] in uPortal's Spring bean definition file(s) (e.g., /properties/contexts/*.xml).",
                    remediationAdvice);
        }
        catch (BeanNotOfRequiredTypeException bnfe) {
            return CheckResult.createFailure("The bean named [" + this.beanName
                    + "] defined in uPotal's Spring bean definitions (/properties/contexts/*.xml), is not of type ["
                    + this.requiredBeanTypeClassName + "]", "Fix the declaration of bean [" + this.beanName
                    + "] to be of the required type.");

        }
        catch (BeansException be) {
            log.error("Error instantiating " + this.beanName, be);
            return CheckResult.createFailure("There was an error instantiating the bean [" + this.beanName
                    + "] required for configuration of configuration checking.", be.getMessage());
        }
        catch (ClassNotFoundException cnfe) {
            return CheckResult
                    .createFailure("We can't check for the presence of the bean [" + this.beanName
                            + "] because the desired class [" + this.requiredBeanTypeClassName
                            + "] itself could not be found.",
                            "Either fix the configuration of this check so that it's checking for the right class name, or place the required class on the classpath.");
        }

    }
}
