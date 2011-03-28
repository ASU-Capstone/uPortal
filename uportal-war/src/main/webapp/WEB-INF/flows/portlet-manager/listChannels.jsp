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
    
<!-- START: VALUES BEING PASSED FROM BACKEND -->
<portlet:actionURL var="queryUrl">
  <portlet:param name="execution" value="${flowExecutionKey}" />
</portlet:actionURL>

<c:set var="n"><portlet:namespace/></c:set>

<portlet:actionURL var="newPortletUrl">
  <portlet:param name="execution" value="${flowExecutionKey}" />
  <portlet:param name="_eventId" value="createPortlet"/>
</portlet:actionURL>
<portlet:actionURL var="popularPortletsUrl">
  <portlet:param name="execution" value="${flowExecutionKey}" />
  <portlet:param name="_eventId" value="popularPortlets"/>
</portlet:actionURL>
<portlet:actionURL var="editPortletUrl">
  <portlet:param name="execution" value="${flowExecutionKey}" />
  <portlet:param name="_eventId" value="editPortlet"/>
  <portlet:param name="chanId" value="PORTLETID"/>
</portlet:actionURL>
<portlet:actionURL var="removePortletUrl">
  <portlet:param name="execution" value="${flowExecutionKey}" />
  <portlet:param name="_eventId" value="removePortlet"/>
  <portlet:param name="chanId" value="PORTLETID"/>
</portlet:actionURL>
<!-- END: VALUES BEING PASSED FROM BACKEND -->

<!--
PORTLET DEVELOPMENT STANDARDS AND GUIDELINES
| For the standards and guidelines that govern
| the user interface of this portlet
| including HTML, CSS, JavaScript, accessibilty,
| naming conventions, 3rd Party libraries
| (like jQuery and the Fluid Skinning System)
| and more, refer to:
| http://www.ja-sig.org/wiki/x/cQ
-->
    
<!-- Portlet -->
<div class="fl-widget portlet" role="section">
	<form id="${n}portletSelectionForm">
  
  <!-- Portlet Title -->
  <div class="fl-widget-titlebar portlet-title" role="sectionhead">
  	<h2 role="heading"><spring:message code="listChannels.title"/></h2>
  </div> <!-- end: portlet-title -->
  
  <!-- Portlet Toolbar -->
  <div class="fl-col-flex2 portlet-toolbar" role="toolbar">
    <div class="fl-col">
      <ul>
        <li><a href="${ newPortletUrl }" title="<spring:message code="listChannels.newPortletButton"/>"><span><spring:message code="listChannels.newPortletButton"/></span></a></li>
        <li><a href="${ popularPortletsUrl }" title="<spring:message code="listChannels.popularPortletsButton"/>"><span><spring:message code="listChannels.popularPortletsButton"/></span></a></li>
      </ul>
    </div>
    <div class="fl-col fl-text-align-right">
  		<input id="${n}channelSearch"/>
      <input type="submit" value="<spring:message code="listChannels.searchSubmitButton"/>"/>
    </div>
  </div> <!-- end: portlet-toolbar -->
        
	<!-- Portlet Body -->
  <div class="fl-widget-content portlet-body" role="main">
  
  	<!-- Portlet Messages -->
  	<div class="portlet-msg-info" role="status" id="${n}loadingMessage">
    	<h3>Loading Information</h3>
    	<p>Please wait while the system finishes loading the portlet registry.</p>
    </div> <!-- end: portlet-msg -->
    
    <!-- Portlet Section -->
    <div id="${n}channelAddingTabs" class="portlet-section fl-pager" role="region"> 
      <h3 class="portlet-section-header" role="heading">
        <spring:message code="listChannels.portletListHeading"/>
      </h3>
      
      <!-- Portlet Section Options -->
      <div class="fl-col-flex2 portlet-section-options">
        <div class="fl-col view-filter">
          <label for="${n}categorySelectMenu"><spring:message code="listChannels.categoryFilterLabel"/></label>
          <select id="${n}categorySelectMenu">
            <option value=""><spring:message code="listChannels.categoryFilterAllCategories"/></option>
          </select>
        </div>
        <div class="fl-col view-pager flc-pager-top">
          <ul id="pager-top" class="fl-pager-ui">
            <li class="flc-pager-previous"><a href="#">&lt; <spring:message code="listChannels.pagerPrevious"/></a></li>
            <li>
              <ul class="fl-pager-links flc-pager-links" style="margin:0; display:inline">
                <li class="flc-pager-pageLink"><a href="javascript:;">1</a></li>
                <li class="flc-pager-pageLink-disabled">2</li>
                <li class="flc-pager-pageLink"><a href="javascript:;">3</a></li>
              </ul>
            </li>
            <li class="flc-pager-next"><a href="#"><spring:message code="listChannels.pagerNext"/> &gt;</a></li>
            <li>
              <span class="flc-pager-summary"><spring:message code="listChannels.pagerPerPagePrefix"/></span>
              <span> <select class="pager-page-size flc-pager-page-size">
              <option value="5">5</option>
              <option value="10">10</option>
              <option value="20">20</option>
              <option value="50">50</option>
              </select></span> <spring:message code="listChannels.pagerPerPageSuffix"/>
            </li>
          </ul>
        </div>
      </div> <!-- end: portlet-section-options -->

      <div class="portlet-section-body">

        <table id="${n}categoriesTable1" summary="" xmlns:rsf="http://ponder.org.uk" style="width:100%;">
          <thead>
            <tr rsf:id="header:">
              <th id="${n}portletName" class="flc-pager-sort-header"><a rsf:id="name" title="Click to sort" href="javascript:;"><spring:message code="listChannels.portletTableNameHeading"/></a></th>
              <th id="${n}portletType" class="flc-pager-sort-header"><a rsf:id="type" title="Click to sort" href="javascript:;"><spring:message code="listChannels.portletTableTypeHeading"/></a></th>
              <th id="${n}portletState" class="flc-pager-sort-header"><a rsf:id="state" title="Click to sort" href="javascript:;"><spring:message code="listChannels.portletTableStateHeading"/></a></th>
              <th id="${n}portletEditLink" rsf:id="editLink"><spring:message code="listChannels.portletTableEditHeading"/></th>
              <th id="${n}portletDeleteLink" rsf:id="deleteLink"><spring:message code="listChannels.portletTableDeleteHeading"/></th>
            </tr>
          </thead>
          <tbody id="${n}categoriesBody">
            <tr rsf:id="row:">
              <td headers="${n}portletName"><span rsf:id="name"></span></td>
              <td headers="${n}portletType" rsf:id="type"></td>
              <td headers="${n}portletState" rsf:id="state" style="text-transform:capitalize"></td>
              <td headers="${n}portletEditLink"><a href="" rsf:id="editLink"></a></td>
              <td headers="${n}portletDeleteLink"><a href="" rsf:id="deleteLink"></a></td>
            </tr>
          </tbody>
        </table>
      
      </div>  
    </div> <!-- end: portlet-section -->
    
  </div> <!-- end: portlet-body -->
  </form>

</div> <!-- end: portlet -->
    	
	<script type="text/javascript">
	 up.jQuery(function() {
        var $ = up.jQuery;
        var channelBrowser;
        var editUrl = "${ editPortletUrl }";
        var removeUrl = "${ removePortletUrl }";
        var pager;

        var channelTypes = { };
        <c:forEach items="${channelTypes}" var="type">channelTypes[${type.id}] = '${type.name}';</c:forEach>

        var searchChannels = function() {
            var searchTerm = $("#${n}channelSearch").val();
            var categoryId = $("#${n}categorySelectMenu").val();
            var channels;
            if (searchTerm == null || searchTerm == "") {
                channels = channelBrowser.getChannelsForCategory(categoryId);
            } else {
                channels = channelBrowser.searchChannels(searchTerm, categoryId);
            }
            $(channels).each(function() {
                this.type = channelTypes[this.typeId]; 
            });
            return channels;
        }

        var updateTable = function() {
            var newChannels = searchChannels();
            var newModel = up.fluid.copy(pager.model);
            newModel.totalRange = newChannels.length;
            newModel.pageIndex = 0;
            newModel.pageCount = Math.max(1, Math.floor((newModel.totalRange - 1)/ newModel.pageSize) + 1);
            up.fluid.clear(pager.options.dataModel);
            up.fluid.model.copyModel(pager.options.dataModel, newChannels);
            pager.permutation = undefined;
            pager.events.onModelChange.fire(newModel, pager.model, pager);
            up.fluid.model.copyModel(pager.model, newModel)
        }

        $(document).ready(function() {
            channelBrowser = $.channelbrowser({
                channelXmlUrl: 'mvc/channelList?xml=true&type=manage',
                onDataLoad: function(categories) {
                    var categorySelect = $("#${n}categorySelectMenu");
                    $(categories).each(function(i, val) {
                        categorySelect.get(0).options[i+1] = new Option(this.name, this.id);
                    });
                    $("#${n}loadingMessage").css("display", "none");

                    var selectorPrefix = "${ n }categoriesTable";

                    var options = {
                        dataModel: searchChannels(),
                        columnDefs: [
	                        { key: "name", valuebinding: "*.name", sortable: true },
                            { key: "type", valuebinding: "*.type", sortable: true },
                            { key: "state", valuebinding: "*.state", sortable: true },
                            { key: "editLink", valuebinding: "*.id",
                                components: {
                                    target: editUrl.replace("PORTLETID", '${"${*.id}"}'),
                                    linktext: "<spring:message code="listChannels.editLink"/>"
                                    }
                                },
                            { key: "deleteLink", valuebinding: "*.id",
                                components: {
                                    target: removeUrl.replace("PORTLETID", '${"${*.id}"}'),
                                    linktext: "<spring:message code="listChannels.deleteLink"/>"
                                    }
                                }
                        ],
                        bodyRenderer: {
                          type: "fluid.pager.selfRender",
                          options: {
                              selectors: {
                                 root: "#${n}categoriesTable1"
                              },
                              row: "row:"
                            }
                            
                        },
                        pagerBar: {type: "fluid.pager.pagerBar", options: {
                          pageList: {type: "fluid.pager.renderedPageList",
                            options: { 
                              linkBody: "a"
                            }
                          }
                        }}
                    };
                    pager = up.fluid.pager("#${n}channelAddingTabs", options);
                    categorySelect.change(updateTable);
                    $("#${n}portletSelectionForm").submit(updateTable);
                    $("#${n}channelSearch").keyup(updateTable);
                }
            });
            
        });
   	  });
    </script>
