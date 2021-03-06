<%--
  ~ Copyright (c) 2012. The Genome Analysis Centre, Norwich, UK
  ~ MISO project contacts: Robert Davey, Mario Caccamo @ TGAC
  ~ **********************************************************************
  ~
  ~ This file is part of MISO.
  ~
  ~ MISO is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ MISO is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with MISO.  If not, see <http://www.gnu.org/licenses/>.
  ~
  ~ **********************************************************************
  --%>

<%--
  Created by IntelliJ IDEA.
  User: davey
  Date: 12/01/12
  Time: 12:07
 --%>
<%@ include file="../header.jsp" %>
<script src="<c:url value='/scripts/sequencer_partition_container_ajax.js?ts=${timestamp.time}'/>" type="text/javascript"></script>
<script src="<c:url value='/scripts/sequencer_partition_container_validation.js?ts=${timestamp.time}'/>" type="text/javascript"></script>

<div id="maincontent">
<div id="contentcolumn">
<form:form method="POST" commandName="container" autocomplete="off" onsubmit="return validate_container(this);">
<sessionConversation:insertSessionConversationId attributeName="container"/>
<h1>
    <c:choose>
        <c:when test="${not empty container.containerId}">Edit</c:when>
        <c:otherwise>Create</c:otherwise>
    </c:choose> Sequencer Partition Container
    <button type="submit" class="fg-button ui-state-default ui-corner-all">Save</button>
</h1>

<table class="in">
  <tr>
    <td class="h">Container ID:</td>
    <td>
      <c:choose>
        <c:when test="${not empty container.containerId}"><input type='hidden' id='containerId' name='containerId' value='${container.containerId}'/>${container.containerId}</c:when>
        <c:otherwise><i>Unsaved</i></c:otherwise>
      </c:choose>
    </td>
  </tr>

  <tr>
    <td>Platform:</td>
    <td>
      <c:choose>
        <c:when test="${not empty container.containerId and not empty container.platformType}">
          <div id="platformTypesDiv">${container.platformType.key}</div>
        </c:when>
        <c:otherwise>
          <div id="platformTypesDiv"></div>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
<c:if test="${empty container.containerId}">
  <tr>
    <td>Sequencer:</td>
    <td id="sequencerReferenceSelect">
      <i>Please choose a platform above...</i>
    </td>
  </tr>
</c:if>
</table>

<table width="100%">
  <tbody>
  <tr>
    <td width="50%" valign="top">
      <h2>Container Parameters</h2>
      <div id="containerPartitions">
      <c:if test="${not empty container.containerId}">
        <div class="note ui-corner-all">
          <c:if test="${multiplexed and not empty container.identificationBarcode}">
            <ul class="sddm">
              <li><a
                      onmouseover="mopen('containermenu')"
                      onmouseout="mclosetime()">Options <span style="float:right"
                                                              class="ui-icon ui-icon-triangle-1-s"></span></a>

                <div class="run" id="containermenu"
                     onmouseover="mcancelclosetime()"
                     onmouseout="mclosetime()">
                  <a href="javascript:void(0);"
                     onclick="generateCasava17DemultiplexCSV(${container.containerId});">Demultiplex
                    CSV (pre-1.8)</a>
                  <a href="javascript:void(0);"
                     onclick="generateCasava18DemultiplexCSV(${container.containerId});">Demultiplex
                    CSV (1.8+)</a>
                </div>
              </li>
            </ul>
          </c:if>
          <div style="clear:both"></div>
          <table class="in">
            <tr>
              <c:choose>
                <c:when test="${empty container.identificationBarcode}">
                  <td>ID Barcode:</td>
                  <td>
                    <button onclick="lookupContainer(this);" type="button" class="right-button ui-state-default ui-corner-all">Lookup</button>
                    <div style="overflow: hidden">
                      <form:input path="identificationBarcode"/>
                    </div>
                  </td>
                </c:when>
                <c:otherwise>
                  <td>ID Barcode:</td>
                  <td>
                    <span id="idBarcode">${container.identificationBarcode}</span>
                    <c:if test="${(container.securityProfile.owner.loginName eq SPRING_SECURITY_CONTEXT.authentication.principal.username)
                                                    or fn:contains(SPRING_SECURITY_CONTEXT.authentication.principal.authorities,'ROLE_ADMIN')}">
                      <a href="javascript:void(0);"
                         onclick="editContainerIdBarcode(jQuery('#idBarcode'), 0)">
                        <span class="fg-button ui-icon ui-icon-pencil"></span>
                      </a>
                    </c:if>
                  </td>
                </c:otherwise>
              </c:choose>
            </tr>
            <tr>
              <c:choose>
                <c:when test="${empty container.locationBarcode}">
                  <td>Location:</td>
                  <td><form:input path="locationBarcode"/></td>
                </c:when>
                <c:otherwise>
                  <td>Location:</td>
                  <td>
                    <span id="locationBarcode">${container.locationBarcode}</span>
                    <c:if test="${(container.securityProfile.owner.loginName eq SPRING_SECURITY_CONTEXT.authentication.principal.username)
                                                    or fn:contains(SPRING_SECURITY_CONTEXT.authentication.principal.authorities,'ROLE_ADMIN')}">
                      <a href="javascript:void(0);"
                         onclick="editContainerLocationBarcode(jQuery('#locationBarcode'), 0)">
                        <span class="fg-button ui-icon ui-icon-pencil"></span>
                      </a>
                    </c:if>
                  </td>
                </c:otherwise>
              </c:choose>
            </tr>
            <tr>
              <c:choose>
                <c:when test="${empty container.validationBarcode}">
                  <td>Validation:</td>
                  <td><form:input path="validationBarcode"/></td>
                </c:when>
                <c:otherwise>
                  <td>Validation:</td>
                  <td>
                    <span id="validationBarcode">${container.validationBarcode}</span>
                    <c:if test="${(container.securityProfile.owner.loginName eq SPRING_SECURITY_CONTEXT.authentication.principal.username)
                                                    or fn:contains(SPRING_SECURITY_CONTEXT.authentication.principal.authorities,'ROLE_ADMIN')}">
                      <a href="javascript:void(0);"
                         onclick="editContainerValidationBarcode(jQuery('#validationBarcode'), 0)">
                        <span class="fg-button ui-icon ui-icon-pencil"></span>
                      </a>
                    </c:if>
                  </td>
                </c:otherwise>
              </c:choose>
            </tr>
            <%--
            <tr>
              <td>Paired: ${container.paired}</td>
            </tr>
            --%>
          </table>
          <div id='partitionErrorDiv'> </div>
          <div id="partitionDiv">
            <i class="italicInfo">Click in a partition box to beep/type in barcodes, or double click a pool on the right to sequentially add pools to the container</i>
            <table class="in">
              <th>Partition No.</th>
              <th>Pool</th>
              <c:forEach items="${container.partitions}" var="partition" varStatus="partitionCount">
                <tr>
                  <td>${partition.partitionNumber}</td>
                  <td width="90%">
                    <c:choose>
                      <c:when test="${not empty partition.pool}">
                        <ul partition="${partitionCount.index}" bind="partitions[${partitionCount.index}].pool" class="runPartitionDroppable">
                        <div class="dashboard">
                          <a href='<c:url value="/miso/pool/${fn:toLowerCase(container.platformType.key)}/${partition.pool.poolId}"/>'>
                              ${partition.pool.name}
                            (${partition.pool.creationDate})
                          </a><br/>
                            <span style="font-size:8pt">
                              <c:choose>
                                <c:when test="${not empty partition.pool.experiments}">

                                  <i><c:forEach items="${partition.pool.experiments}" var="experiment">
                                    ${experiment.study.project.alias} (${experiment.name}: ${fn:length(partition.pool.dilutions)} dilutions)<br/>
                                  </c:forEach>
                                  </i>
                                  <input type="hidden"
                                         name="partitions[${partitionCount.index}].pool"
                                         id="pId${partitionCount.index}"
                                         value="${partition.pool.poolId}"/>
                                </c:when>
                                <c:otherwise>
                                  <i>No experiment linked to this pool</i>
                                </c:otherwise>
                              </c:choose>
                            </span>
                        </div>
                        </ul>
                      </c:when>
                      <c:otherwise>
                        <div id="p_div_${partitionCount.index}"
                             class="elementListDroppableDiv">
                          <ul class='runPartitionDroppable' bind='partitions[${partitionCount.index}].pool' partition='${partitionCount.index}' ondblclick='populatePartition(this);'/></ul>
                        </div>
                      </c:otherwise>
                    </c:choose>
                  </td>
                </tr>
              </c:forEach>
            </table>
          </div>
        </div>
      </c:if>
      </div>
    </td>
  </tr>
  </tbody>
</table>
</form:form>
</div>
</div>

<script type="text/javascript">
  <c:if test="${empty container.containerId or empty container.platformType}">
    jQuery(document).ready(function() {
      populatePlatformTypes();
    });
  </c:if>
</script>

<%@ include file="adminsub.jsp" %>

<%@ include file="../footer.jsp" %>