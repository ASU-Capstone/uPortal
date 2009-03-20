<?xml version='1.0' encoding='utf-8' ?>
<!--

    Copyright (c) 2000-2009, Jasig, Inc.
    See license distributed with this file and available online at
    https://www.ja-sig.org/svn/jasig-parent/tags/rel-10/license-header.txt

-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="html" indent="no"/>
    
    <!-- ========== VARIABLES & PARAMETERS ========== -->
    <xsl:param name="baseActionURL">default</xsl:param>
    <xsl:param name="unauthenticated">true</xsl:param>
    <xsl:param name="locale">en_US</xsl:param>
    <xsl:param name="mediaPath" select="'media/org/jasig/portal/channels/CLogin'"/>
    <xsl:param name="portalInstitutionName">JA-SIG</xsl:param>
    <xsl:param name="portalName">uPortal</xsl:param>
    <xsl:param name="forgotLoginUrl">http://www.uportal.org/</xsl:param>
    <xsl:param name="contactAdminUrl">http://www.uportal.org/</xsl:param>
    <xsl:param name="casLoginUrl"></xsl:param>
    <xsl:param name="casNewUserUrl"></xsl:param>
  <!-- ========== VARIABLES & PARAMETERS ========== -->
    
    
    <!-- Match on root element then check if the user is NOT authenticated. -->
    <xsl:template match="/">
    
        <xsl:if test="$unauthenticated='true'">
	        <xsl:apply-templates/>
        </xsl:if>
        <xsl:if test="$unauthenticated='false'">
          <div id="portalWelcome">
            <div id="portalWelcomeInner">
              <p>Welcome <xsl:value-of select="//login-status/full-name"/>. <span class="logout-label"><a href="Logout" title="Sign out">Sign out</a></span>
              </p>
            </div>
          </div>
        </xsl:if>
        
    </xsl:template>
    
    
    <!-- If user is not authenticated insert login form. -->
    <xsl:template match="login-status">
    
      <div id="portalLogin" class="fl-widget">
        <div class="fl-widget-inner">
        	<div class="fl-widget-titlebar">
        		<h2>Sign In</h2>
          </div>
          <xsl:choose>
            <!-- CAS Login link -->
            <xsl:when test="$casLoginUrl!= ''">
              <div id="portalCASLogin" class="fl-widget-content">
                <a id="portalCASLoginLink" href="{$casLoginUrl}" title="Sign In">
                  <span>Sign In <span class="via-cas">with CAS</span></span>
                </a>
                <p>New user? <a id="portalCASLoginNewLink" href="{$casNewUserUrl}" title="New User">Start here</a>.</p>
              </div>
            </xsl:when>
            <!-- Username/password login form -->
            <xsl:otherwise>
              <div id="portalLoginStandard" class="fl-widget-content">
                <xsl:apply-templates/>
                <form id="portalLoginForm" action="Login" method="post">
                  <input type="hidden" name="action" value="login"/>
                  <label for="userName">Username:</label>
                  <input type="text" name="userName" size="15" value="{failure/@attemptedUserName}"/>
                  <label for="password">Password:</label>
                  <input type="password" name="password" size="15"/>
                  <input type="submit" value="Sign In" name="Login" id="portalLoginButton" class="portlet-form-button"/>
                </form>
                <p><a id="portalLoginForgot" href="{$forgotLoginUrl}">Forgot your username or password?</a></p>
              </div>
            </xsl:otherwise>
        	</xsl:choose>
        
        </div>
      </div>
      
    </xsl:template>
    
    
    <!-- If user login fails present error message box. -->
    <xsl:template match="failure">
    
    	<div id="portalLoginMessage" class="portlet-msg-alert">
        <h2>Important Message</h2>
        <p>The username and password you entered do not match any accounts on record. Please make sure that you have correctly entered the username associated with your <xsl:value-of select="$portalName"/> account.</p>
        <p><a id="portalLoginErrorForgot" href="{$forgotLoginUrl}">Forgot your username or password?</a></p>
      </div>
        
    </xsl:template>
    
    
    <!-- If user login encounters an error present error message box. -->
    <xsl:template match="error">
    
      <div id="portalLoginMessage" class="portlet-msg-error">
        <h2>Important Message</h2>
        <p><xsl:value-of select="$portalName"/> is unable to complete your login request at this time. It is possible the system is down for maintenance or other reasons. Please try again later. If this problem persists, contact <a href="{$contactAdminUrl}"><xsl:value-of select="$portalInstitutionName"/></a></p>
      </div>
        
    </xsl:template>
    
    
    <!-- Error message box. -->
    <xsl:template name="message">
    
       <xsl:param name="messageString"/>
       <div id="portalLoginMessage">
					<h2>Important Message</h2>
          <xsl:value-of select="$messageString"/>
       </div>
       
    </xsl:template>
    
    
</xsl:stylesheet>
