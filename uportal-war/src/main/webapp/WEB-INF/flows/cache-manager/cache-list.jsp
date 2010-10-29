<%--

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

--%>

<%@ include file="/WEB-INF/jsp/include.jsp" %>

<portlet:actionURL var="flushAllUrl">
    <portlet:param name="execution" value="${flowExecutionKey}" />
    <portlet:param name="_eventId" value="flush-all"/>
</portlet:actionURL>

<!-- Portlet -->
<div class="fl-widget portlet cache-manager" role="section">

  <!-- Portlet Title -->
  <div class="fl-widget-titlebar portlet-title" role="sectionhead">
    <h2 role="heading"><spring:message code="cache-list.title"/></h2>
  </div> <!-- end: portlet-title -->
  
  <!-- Portlet Toolbar -->
  <div class="fl-col-flex2 portlet-toolbar" role="toolbar">
    <div class="fl-col">
        <ul>
            <li><a href="${flushAllUrl}"><span><spring:message code="cache-list.emptyAllButton" /></span></a></li>
      </ul>
    </div>
  </div> <!-- end: portlet-toolbar -->

  <!-- Portlet Body -->
  <div class="fl-widget-content portlet-body" role="main">
    <!-- Portlet Section -->
    <div class="portlet-section" role="region">
      <h3 class="portlet-section-header" role="heading">
       <spring:message code="cache-list.listHeading"/>
      </h3>
      
      <div class="portlet-section-body">
        <p class="portlet-section-note" role="note"><spring:message code="cache-list.listDescription"/></p>
      
        <table class="cache-table">
            <thead>
                <tr>
                    <th><spring:message code="cache-list.table-header.name"/></th>
                    <th><spring:message code="cache-list.table-header.used"/></th>
                    <th><spring:message code="cache-list.table-header.effectiveness"/></th>
                    <th><spring:message code="cache-list.table-header.flush"/></th>
                </tr>
            </thead>
            <c:forEach items="${statisticsMap}" var="statisticsEntry">
                <tr class="cache-member">
                    <td class="cache-name">
                        <portlet:actionURL var="viewStatsUrl">
                            <portlet:param name="cacheName" value="${statisticsEntry.key}"/>
                            <portlet:param name="execution" value="${flowExecutionKey}" />
                            <portlet:param name="_eventId" value="view-statistics"/>
                        </portlet:actionURL>
                        <a href="${viewStatsUrl}">${fn:escapeXml(statisticsEntry.key)}</a>
                    </td>
                    <td class="cache-used">
                        <span><fmt:formatNumber value="${statisticsEntry.value.usage}" pattern="00%" /> </span> 
                        <small>(${statisticsEntry.value.size} / ${statisticsEntry.value.maxSize})</small>
                    </td>
                    <td class="cache-effectiveness">
                        <span><fmt:formatNumber value="${statisticsEntry.value.effectiveness}" pattern="00%" /> </span>
                        <small>(<c:out value="${fn:escapeXml(statisticsEntry.value.hits)}"/> <spring:message code="cache-list.hits"/>, <c:out value="${fn:escapeXml(statisticsEntry.value.misses)}"/> <spring:message code="cache-list.misses"/>)</small>
                    </td>
                    <td class="cache-flush">
                        <portlet:actionURL var="flushUrl">
                          <portlet:param name="cacheName" value="${statisticsEntry.key}"/>
                          <portlet:param name="_eventId" value="flush"/>
                          <portlet:param name="execution" value="${flowExecutionKey}" />
                        </portlet:actionURL>
                        <a href="${flushUrl}"><spring:message code="cache-list.flush-link"/></a>
                    </td>
                </tr>
            </c:forEach>
        </table>
      </div>
    </div>
  </div>
  
</div>