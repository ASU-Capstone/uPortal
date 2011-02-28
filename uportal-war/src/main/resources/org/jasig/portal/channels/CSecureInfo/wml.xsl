<?xml version="1.0" encoding="utf-8"?>
<!--

    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.

-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" indent="yes"/>
  <xsl:param name="baseActionURL">baseActionURL_false</xsl:param>
  <xsl:variable name="baseMediaURL">media/org/jasig/portal/channels/CSecureInfo/</xsl:variable>

  <xsl:template match="secure">

    <p align="left"><b>Channel ID:</b></p>
    <p align="right"><xsl:value-of select="channel/id"/></p>

    <p align="left"><b>Attention:</b></p>
    <p align="right">
	This channel must be rendered using a secure protocol (i.e. https).
    </p>

  </xsl:template>

</xsl:stylesheet>
