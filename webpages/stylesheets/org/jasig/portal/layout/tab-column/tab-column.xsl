<?xml version='1.0' encoding='utf-8' ?><xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:param name="activeTab">t01</xsl:param>
<xsl:param name="userLayoutRoot">root</xsl:param>

<!-- document fragment template. See structure stylesheet for more comments -->
<xsl:template match="layout_fragment">
   <layout_fragment>
    <xsl:call-template name="tabList"/>
    <content>
      <xsl:apply-templates/>
    </content>
   </layout_fragment>    
</xsl:template>

<xsl:template match="layout">
  <layout>
  
    <header>
      <xsl:for-each select="child::folder[attribute::type='header']">
	      <xsl:copy-of select=".//channel"/>
      </xsl:for-each>
    </header>
    
    <xsl:call-template name="tabList"/>

    <content>
      <xsl:choose>
        <xsl:when test="$userLayoutRoot = 'root'">
          <xsl:apply-templates select="folder[attribute::type='regular']"/>
        </xsl:when>
        <xsl:otherwise>
          <column>
            <xsl:apply-templates select="//*[@ID = $userLayoutRoot]"/>
          </column>
        </xsl:otherwise>
      </xsl:choose>
    </content>

    <footer>
      <xsl:for-each select="child::folder[attribute::type='footer']">
	      <xsl:copy-of select=".//channel"/>
      </xsl:for-each>
    </footer>
    
  </layout>    
</xsl:template>

<xsl:template name="tabList">
  <navigation>
    <xsl:for-each select="/layout/folder[attribute::type='regular']">
      <tab>
        <xsl:attribute name="ID"><xsl:value-of select="@ID"/></xsl:attribute>
      	<xsl:choose>
      	  <xsl:when test="$activeTab=@ID">
      	    <xsl:attribute name="activeTab">true</xsl:attribute>
      	  </xsl:when>
      	  <xsl:otherwise>
      	    <xsl:attribute name="activeTab">false</xsl:attribute>
      	  </xsl:otherwise>
      	</xsl:choose>
        <xsl:attribute name="priority"><xsl:value-of select="@priority"/></xsl:attribute>
        <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
      </tab>
    </xsl:for-each>
  </navigation>
</xsl:template>

<xsl:template match="folder">
  <xsl:if test="$activeTab=@ID">
    <xsl:if test="child::folder">
      <xsl:for-each select="folder">
        <column>
          <xsl:attribute name="ID"><xsl:value-of select="@ID"/></xsl:attribute>
          <xsl:attribute name="priority"><xsl:value-of select="@priority"/></xsl:attribute>
          <xsl:attribute name="width"><xsl:value-of select="@width"/></xsl:attribute>
          <xsl:apply-templates/>
        </column>
      </xsl:for-each>
    </xsl:if>
    <xsl:if test="child::channel">
      <column>
        <xsl:apply-templates/>
      </column>
    </xsl:if>
  </xsl:if>
</xsl:template>

<xsl:template match="channel">
  <xsl:copy-of select="."/>
</xsl:template>

<xsl:template match="parameter">
  <xsl:copy-of select="."/>
</xsl:template>

</xsl:stylesheet>
