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

<%@ include file="../header.jsp" %>
<script src="<c:url value='/scripts/datatables_utils.js?ts=${timestamp.time}'/>" type="text/javascript"></script>
<script src="<c:url value='/scripts/jquery/datatables/jquery.dataTables.min.js'/>" type="text/javascript"></script>
<script src="<c:url value='/scripts/jquery/editable/jquery.jeditable.mini.js'/>" type="text/javascript"></script>
<script src="<c:url value='/scripts/jquery/editable/jquery.jeditable.datepicker.js'/>" type="text/javascript"></script>
<script src="<c:url value='/scripts/jquery/editable/jquery.jeditable.checkbox.js'/>" type="text/javascript"></script>

<link rel="stylesheet" href="<c:url value='/scripts/jquery/datatables/datatable.css'/>" type="text/css">
<link rel="stylesheet" href="<c:url value='/styles/progress.css'/>" type="text/css">

<form:form method="POST" commandName="project" autocomplete="off" onsubmit="return validate_project(this);">
<sessionConversation:insertSessionConversationId attributeName="project"/>
<h1><c:choose><c:when
        test="${not empty project.projectId}">Edit</c:when><c:otherwise>Create</c:otherwise></c:choose>
  Project
  <button type="submit" class="fg-button ui-state-default ui-corner-all">Save</button>
</h1>
<div class="sectionDivider" onclick="toggleLeftInfo(jQuery('#note_arrowclick'), 'notediv');">Quick Help
  <div id="note_arrowclick" class="toggleLeft"></div>
</div>
<div id="notediv" class="note" style="display:none;">A Project contains information about a set of Studies that may
  comprise many different Samples, Experiments and Runs. Samples are attached to Projects as they are often
  processed into Dilutions, which are then Pooled and sequenced.<br/>Projects also have Overviews, which hold
  information about a Project proposal.
</div>

<c:if test="${not empty project.projectId}">
  <div id="trafdiv" class="ui-corner-all" onclick="location.href='#';">
    <div id="pro${project.projectId}traf"></div>
    <script type="text/javascript">
      jQuery(document).ready(function() {
        editProjectTrafficLight(${project.projectId});

        jQuery("#trafdiv").colorbox({width:"90%", inline:true, href:"#trafpanel"});
      });
    </script>
  </div>
  <div style='display:none'>
    <div id="trafpanel">
      <div id="trafresultgraph">
        <div id="chart"></div>
      </div>
    </div>
  </div>


</c:if>

<h2>Project Information</h2>
<table class="in">
  <tr>
    <td class="h">Project ID:</td>
    <td>
      <c:choose>
        <c:when test="${not empty project.projectId}">${project.projectId}</c:when>
        <c:otherwise><i>Unsaved</i></c:otherwise>
      </c:choose>
    </td>
  </tr>
  <tr>
    <td class="h">Name:</td>
    <td>
      <c:choose>
        <c:when test="${not empty project.projectId}">${project.name}</c:when>
        <c:otherwise><i>Unsaved</i></c:otherwise>
      </c:choose>
    </td>
  </tr>
  <tr>
    <td class="h">Creation date:</td>
    <td><fmt:formatDate value="${project.creationDate}"/></td>
  </tr>
  <tr>
    <td class="h">Alias:</td>
    <td><form:input path="alias" maxlength="${maxLengths['alias']}" class="validateable"/><span id="aliascounter" class="counter"></span>
    </td>
  </tr>
  <tr>
    <td class="h">Description:</td>
    <td><form:input path="description" maxlength="${maxLengths['description']}" class="validateable"/><span id="descriptioncounter"
                                                                                       class="counter"></span></td>
  </tr>
  <tr>
    <td>Progress:</td>
    <td>
      <c:choose>
        <c:when test="${(project.securityProfile.owner.loginName eq SPRING_SECURITY_CONTEXT.authentication.principal.username)
                        or fn:contains(SPRING_SECURITY_CONTEXT.authentication.principal.authorities,'ROLE_ADMIN')}">
          <form:radiobuttons id="progress" path="progress"/>
        </c:when>
        <c:otherwise>
          ${project.progress}
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
</table>
<div id="printServiceSelectDialog" title="Select a Printer"></div>


<div id="projectoverviews">
  <c:if test="${not empty project.projectId}">
    <a class="add" href="javascript:void(0);" onclick="showProjectOverviewDialog(${project.projectId});">Add
      Overview</a><br/>
  </c:if>
  <c:choose>
    <c:when test="${not empty project.overviews}">
      <c:forEach items="${project.overviews}" var="overview" varStatus="ov">
        <div id="overviewdiv${overview.overviewId}" class="ui-corner-all simplebox">
        <ul class="sddm" style="margin: 0px 8px 0 0;">
          <li><a
                  onmouseover="mopen('overviewMenu${overview.overviewId}')"
                  onmouseout="mclosetime()">Options <span style="float:right"
                                                          class="ui-icon ui-icon-triangle-1-s"></span></a>

            <div id="overviewMenu${overview.overviewId}"
                 onmouseover="mcancelclosetime()"
                 onmouseout="mclosetime()">
              <c:choose>
                <c:when test="${not empty overviewMap[overview.overviewId]}">
                  <a href='javascript:void(0);' onclick="unwatchOverview(${overview.overviewId});">Stop watching</a>
                </c:when>
                <c:otherwise>
                  <a href='javascript:void(0);' onclick="watchOverview(${overview.overviewId});">Watch</a>
                </c:otherwise>
              </c:choose>
            </div>
          </li>
          </ul>
          <table class="list" id="overview">
            <thead>
            <tr>
              <c:if test="${(project.securityProfile.owner.loginName eq SPRING_SECURITY_CONTEXT.authentication.principal.username)
                                        or fn:contains(SPRING_SECURITY_CONTEXT.authentication.principal.authorities,'ROLE_ADMIN')}">
                <th>Lock/Unlock</th>
              </c:if>
              <th>Principal Investigator</th>
              <th>Start Date</th>
              <th>End Date</th>
              <th># Proposed Samples</th>
              <th># QC Passed Samples</th>
              <th width="40%">Notes</th>
            </tr>
            </thead>
            <tbody>
            <tr onMouseOver="this.className='highlightrow'" onMouseOut="this.className='normalrow'">
              <c:if test="${(project.securityProfile.owner.loginName eq SPRING_SECURITY_CONTEXT.authentication.principal.username)
                                      or fn:contains(SPRING_SECURITY_CONTEXT.authentication.principal.authorities,'ROLE_ADMIN')}">
                <c:choose>
                  <c:when test="${overview.locked}">
                    <td style="text-align:center;"><a href="javascript:void(0);"
                                                      onclick="unlockProjectOverview(${overview.overviewId})"><img
                            style="border:0;" alt="Unlock" title="Unlock this overview"
                            src="<c:url value='/styles/images/lock_closed.png'/>"/></a></td>
                  </c:when>
                  <c:otherwise>
                    <td style="text-align:center;"><a href="javascript:void(0);"
                                                      onclick="lockProjectOverview(${overview.overviewId})"><img
                            style="border:0;" alt="Lock" title="Lock this overview"
                            src="<c:url value='/styles/images/lock_open.png'/>"/></a></td>
                  </c:otherwise>
                </c:choose>
              </c:if>

              <td>${overview.principalInvestigator}</td>
              <td>
                <c:choose>
                  <c:when test="${overview.locked eq false and ((project.securityProfile.owner.loginName eq SPRING_SECURITY_CONTEXT.authentication.principal.username)
                                      or fn:contains(SPRING_SECURITY_CONTEXT.authentication.principal.authorities,'ROLE_ADMIN'))}">
                    <form:input path="overviews['${ov.count-1}'].startDate" id="startdatepicker"/>
                    <script type="text/javascript">
                      addDatePicker("startdatepicker");
                    </script>
                  </c:when>
                  <c:otherwise>
                    ${overview.startDate}
                  </c:otherwise>
                </c:choose>
              </td>
              <td>
                <c:choose>
                  <c:when test="${overview.locked eq false and ((project.securityProfile.owner.loginName eq SPRING_SECURITY_CONTEXT.authentication.principal.username)
                                      or fn:contains(SPRING_SECURITY_CONTEXT.authentication.principal.authorities,'ROLE_ADMIN'))}">
                    <form:input path="overviews['${ov.count-1}'].endDate" id="enddatepicker"/>
                    <script type="text/javascript">
                      addDatePicker("enddatepicker");
                    </script>
                  </c:when>
                  <c:otherwise>
                    ${overview.endDate}
                  </c:otherwise>
                </c:choose>
              </td>
              <td>
                <c:choose>
                  <c:when test="${overview.locked eq false and ((project.securityProfile.owner.loginName eq SPRING_SECURITY_CONTEXT.authentication.principal.username)
                                      or fn:contains(SPRING_SECURITY_CONTEXT.authentication.principal.authorities,'ROLE_ADMIN'))}">
                    <form:input path="overviews['${ov.count-1}'].numProposedSamples" id="numProposedSamples${ov.count-1}"/>
                  </c:when>
                  <c:otherwise>
                    ${overview.numProposedSamples}
                  </c:otherwise>
                </c:choose>
              </td>
              <td>
                  ${fn:length(overview.qcPassedSamples)} / ${overview.numProposedSamples}
                <div id="progressbar${overview.overviewId}"></div>
                <script type="text/javascript">
                  jQuery("#progressbar${overview.overviewId}").progressbar({ value: ${fn:length(overview.qcPassedSamples) / overview.numProposedSamples * 100} });
                </script>
              </td>
              <td>
                <c:if test="${not overview.locked}">
                  <a onclick="showProjectOverviewNoteDialog(${overview.overviewId});"
                     href="javascript:void(0);" class="add">Add Note</a><br/>
                </c:if>
                <c:forEach items="${overview.notes}" var="note" varStatus="n">
                  <div class="exppreview" id="overview-notes-${n.count}">
                    <b>${note.creationDate}</b>: ${note.text}
                                          <span class="float-right"
                                                style="font-weight:bold; color:#C0C0C0;">${note.owner.loginName}</span>
                  </div>
                </c:forEach>
              </td>
            </tr>
            </tbody>
          </table>
          <ol id="progress">
            <li class="sample-qc-step">
              <c:choose>
              <c:when test="${overview.allSampleQcPassed and overview.libraryPreparationComplete}">
                <div class="left mid-progress-done">
              </c:when>
              <c:when test="${overview.allSampleQcPassed}">
                <div class="left-progress-done">
              </c:when>
              <c:otherwise>
                <div class="left">
              </c:otherwise>
              </c:choose>
                <span>Sample QCs</span>
                <form:checkbox value="${overview.allSampleQcPassed}" path="overviews[${ov.count-1}].allSampleQcPassed"/>
              </div>
            </li>

            <li class="lib-prep-step">
              <c:choose>
              <c:when test="${overview.libraryPreparationComplete and overview.allLibrariesQcPassed}">
                <div class="mid-progress-done">
              </c:when>
              <c:when test="${overview.libraryPreparationComplete}">
                <div class="left-progress-done">
              </c:when>
              <c:otherwise>
                <div>
              </c:otherwise>
              </c:choose>
                <span>Libraries prepared</span>
                <form:checkbox value="${overview.libraryPreparationComplete}" path="overviews[${ov.count-1}].libraryPreparationComplete"/>
              </div>
            </li>

            <li class="lib-qc-step">
              <c:choose>
              <c:when test="${overview.allLibrariesQcPassed and overview.allPoolsConstructed}">
                <div class="mid-progress-done">
              </c:when>
              <c:when test="${overview.allLibrariesQcPassed}">
                <div class="left-progress-done">
              </c:when>
              <c:otherwise>
                <div>
              </c:otherwise>
              </c:choose>
                <span>Library QCs</span>
                <form:checkbox value="${overview.allLibrariesQcPassed}" path="overviews[${ov.count-1}].allLibrariesQcPassed"/>
              </div>
            </li>

            <li class="pools-step">
              <c:choose>
              <c:when test="${overview.allPoolsConstructed and overview.allRunsCompleted}">
                <div class="mid-progress-done">
              </c:when>
              <c:when test="${overview.allPoolsConstructed}">
                <div class="left-progress-done">
              </c:when>
              <c:otherwise>
                <div>
              </c:otherwise>
              </c:choose>
                <span>Pools Constructed</span>
                <form:checkbox value="${overview.allPoolsConstructed}" path="overviews[${ov.count-1}].allPoolsConstructed"/>
              </div>
            </li>

            <li class="runs-step">
              <c:choose>
              <c:when test="${overview.allRunsCompleted and overview.primaryAnalysisCompleted}">
                <div class="mid-progress-done">
              </c:when>
              <c:when test="${overview.allRunsCompleted}">
                <div class="left-progress-done">
              </c:when>
              <c:otherwise>
                <div>
              </c:otherwise>
              </c:choose>
                <span>Runs Completed</span>
                <form:checkbox value="${overview.allRunsCompleted}" path="overviews[${ov.count-1}].allRunsCompleted"/>
              </div>
            </li>

            <li class="primary-analysis-step">
              <c:choose>
              <c:when test="${overview.primaryAnalysisCompleted}">
                <div class="right mid-progress-done">
              </c:when>
              <c:otherwise>
                <div class="right">
              </c:otherwise>
              </c:choose>
                <span>Primary Analysis</span>
                <form:checkbox value="${overview.primaryAnalysisCompleted}" path="overviews[${ov.count-1}].primaryAnalysisCompleted"/>
              </div>
            </li>
          </ol>
          <p style="clear:both"/>
        </div>

      </c:forEach>
    </c:when>
  </c:choose>
</div>

<div class="sectionDivider" onclick="toggleLeftInfo(jQuery('#issues_arrowclick'), 'issuesdiv');">Tracked Issues
  <div id="issues_arrowclick" class="toggleLeft"></div>
</div>
<div id="issuesdiv" class="note" style="display:none;">
  <c:choose>
    <c:when test="${not empty project.projectId}">
      To link issues to this project please enter your issue keys here, separated by a single comma, e.g. FOO-1,FOO-2,FOO-3:<br/>
      <input type="text" id="previewKeys" name="previewKeys"/>
      <button type="button" class="br-button ui-state-default ui-corner-all" onclick="previewIssueKeys();">Preview
        Issues
      </button>
      <br/>
    </c:when>
    <c:otherwise>
      To import a project from an issue tracker, please enter an Issue Key to form the basis of this project.
      Enter a SINGLE key, e.g. FOO-1, and click Import to link this project to an external issue.<br/>
      <input type="text" id="previewKey" name="previewKey"/>
      <button type="button" class="br-button ui-state-default ui-corner-all" onclick="importProjectFromIssue();">
        Import
      </button>
    </c:otherwise>
  </c:choose>
  <div id="issues"></div>
</div>

<%@ include file="permissions.jsp" %>
<c:if test="${empty project.projectId}">
  <script type="text/javascript">
    jQuery(document).ready(function() {
      //show import pane by default if project is unsaved
      jQuery("#issuesdiv").attr("style", "");
      jQuery("#issues_arrowclick").removeClass("toggleLeft").addClass("toggleLeftDown");

      //show permissions pane by default if project is unsaved
      jQuery("#permissions").attr("style", "");
      jQuery("#permissions_arrowclick").removeClass("toggleLeft").addClass("toggleLeftDown");
    });
  </script>
</c:if>
</form:form>

<c:choose>
<c:when test="${not empty project.projectId}">
<div id="simplebox">
  <div class="sectionDivider" onclick="toggleLeftInfo(jQuery('#upload_arrowclick'), 'uploaddiv');">Project Files
    <div id="upload_arrowclick" class="toggleLeft"></div>
  </div>
  <div id="uploaddiv" class="simplebox" style="display:none;">
    <table class="in">
      <tr>
        <td>
          <form method='post'
                id='ajax_upload_form'
                action="<c:url value="/miso/upload/project"/>"
                enctype="multipart/form-data"
                target="target_upload"
                onsubmit="fileUploadProgress('ajax_upload_form', 'statusdiv', processingOverlay);">
            <input type="hidden" name="projectId" value="${project.projectId}"/>
            <input type="file" name="file"/>
            <button type="submit" class="br-button ui-state-default ui-corner-all">Upload</button>
          </form>
          <iframe id='target_upload' name='target_upload' src='' style='display: none'></iframe>
          <div id="statusdiv"></div>
        </td>
      </tr>
    </table>
  </div>

  <div id="projectfiles">
    <c:forEach items="${projectFiles}" var="file">
      <a href="<c:url value='/miso/download/project/${project.projectId}/${file.key}'/>">
        <a class="listbox" href="<c:url value='/miso/download/project/${project.projectId}/${file.key}'/>">
          <div onMouseOver="this.className='boxlistboxhighlight'" onMouseOut="this.className='boxlistbox'"
               class="boxlistbox">
              ${file.value}
          </div>
        </a>
      </a>
    </c:forEach>
  </div>
</div>
<br/>

<div class="sectionDivider"
     onclick="toggleLeftInfo(jQuery('#samples_arrowclick'), 'samplesdiv');">${fn:length(project.samples)} Samples
  <div id="samples_arrowclick" class="toggleLeft"></div>
</div>
<div id="samplesdiv" style="display:none;">
  <h1>${fn:length(project.samples)} Samples</h1>
  <ul class="sddm">
    <li><a
            onmouseover="mopen('samplemenu')"
            onmouseout="mclosetime()">Options <span style="float:right"
                                                    class="ui-icon ui-icon-triangle-1-s"></span></a>

      <div id="samplemenu"
           onmouseover="mcancelclosetime()"
           onmouseout="mclosetime()">
        <a href='<c:url value="/miso/sample/new/${project.projectId}"/>'>Add Samples</a>
        <c:if test="${not empty project.samples}">
          <a href='javascript:void(0);' onclick='bulkSampleQcTable();'>QC Samples</a>
          <a href='<c:url value="/miso/library/new/${project.samples[0].sampleId}#tab-2"/>'>Add
            Libraries</a>
          <a href="javascript:void(0);" onclick="selectSampleBarcodesToPrint('#sample_table');">Print Barcodes ...</a>
          <a href="javascript:void(0);" onclick="generateSampleDeliveryForm(${project.projectId});">Get
            Delivery Form</a>
          <a href="javascript:void(0);" onclick="uploadSampleDeliveryForm(${project.projectId});">Import
            Delivery Form</a>
          <a href='<c:url value="/miso/sample/receipt"/>'>Receive Samples</a>
        </c:if>
      </div>
    </li>
  </ul>

    <span style="clear:both">
      <div id="deliveryformdiv" class="simplebox" style="display:none;">
        <table class="in">
          <tr>
            <td>
              <form method='post'
                    id='deliveryform_upload_form'
                    action="<c:url value="/miso/upload/project/sample-delivery-form"/>"
                    enctype="multipart/form-data"
                    target="deliveryform_target_upload"
                    onsubmit="fileUploadProgress('deliveryform_upload_form', 'deliveryform_statusdiv', deliveryFormUploadSuccess);">
                <input type="hidden" name="projectId" value="${project.projectId}"/>
                <input type="file" name="file"/>
                <button type="submit" class="br-button ui-state-default ui-corner-all">Upload</button>
                <button type="button" class="br-button ui-state-default ui-corner-all"
                        onclick="cancelSampleDeliveryFormUpload();">Cancel
                </button>
              </form>
              <iframe id='deliveryform_target_upload' name='deliveryform_target_upload' src=''
                      style='display: none'></iframe>
              <div id="deliveryform_statusdiv"></div>
            </td>
          </tr>
        </table>
      </div>

      <table class="list" id="sample_table">
        <thead>
        <tr>
          <th>Sample Name</th>
          <th>Sample Alias</th>
          <th>Sample Description</th>
          <th>Type</th>
          <th>Received Date </th>
          <th>QC Passed</th>
          <th class="fit">Edit</th>
          <sec:authorize access="hasRole('ROLE_ADMIN')">
            <th class="fit">DELETE</th>
          </sec:authorize>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${project.samples}" var="sample">
          <tr sampleId="${sample.sampleId}" onMouseOver="this.className='highlightrow'"
              onMouseOut="this.className='normalrow'">
            <td><b>${sample.name}</b></td>
            <td>${sample.alias}</td>
            <td>${sample.description}</td>
            <td>${sample.sampleType}</td>
            <td>${sample.receivedDate}</td>
            <td>${sample.qcPassed}</td>
            <td class="misoicon"
                onclick="window.location.href='<c:url value="/miso/sample/${sample.sampleId}"/>'"><span class="ui-icon ui-icon-pencil"/></td>
            <sec:authorize access="hasRole('ROLE_ADMIN')">
              <td class="misoicon" onclick="deleteSample(${sample.sampleId}, pageReload);"><span class="ui-icon ui-icon-trash"/></td>
            </sec:authorize>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </span>
</div>

<div class="sectionDivider"
     onclick="toggleLeftInfo(jQuery('#libraries_arrowclick'), 'librariesdiv');">${fn:length(projectLibraries)} Libraries
  <div id="libraries_arrowclick" class="toggleLeft"></div>
</div>
<div id="librariesdiv" style="display:none;">
  <a name="library"></a>

  <h1>${fn:length(projectLibraries)} Libraries</h1>
  <ul class="sddm">
    <li><a
            onmouseover="mopen('librarymenu')"
            onmouseout="mclosetime()">Options <span style="float:right" class="ui-icon ui-icon-triangle-1-s"></span></a>

      <div id="librarymenu"
           onmouseover="mcancelclosetime()"
           onmouseout="mclosetime()">

        <c:if test="${not empty project.samples}">
          <a href='<c:url value="/miso/library/new/${project.samples[0].sampleId}#tab-2"/>'>Add Libraries</a>
        </c:if>

        <c:if test="${not empty projectLibraries}">
          <a href='javascript:void(0);' onclick='bulkLibraryQcTable();' class="add">QC these
            Libraries</a>
          <a href='javascript:void(0);' onclick='bulkLibraryDilutionTable();' class="add">Add Library Dilutions</a>
          <a href="javascript:void(0);" onclick="selectLibraryBarcodesToPrint('#library_table');">Print Barcodes ...</a>
        </c:if>
      </div>
    </li>
  </ul>
    <span style="clear:both">
      <table class="list" id="library_table">
        <thead>
        <tr>
          <th>Library Name</th>
          <th>Library Alias</th>
          <th>Library Description</th>
          <th>Library Type</th>
          <th>Library Platform</th>
          <th>Tag Barcode</th>
          <th>Insert Size</th>
          <th>QC Passed</th>
          <th class="fit">Edit</th>
          <sec:authorize access="hasRole('ROLE_ADMIN')">
            <th class="fit">DELETE</th>
          </sec:authorize>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${projectLibraries}" var="library">
          <tr libraryId="${library.libraryId}" onMouseOver="this.className='highlightrow'"
              onMouseOut="this.className='normalrow'">
            <td><b>${library.name}</b></td>
            <td>${library.alias}</td>
            <td>${library.description}</td>
            <td>${library.libraryType.description}</td>
            <td>${library.platformName}</td>
            <td><c:if test="${not empty library.tagBarcode}">${library.tagBarcode.name} (${library.tagBarcode.sequence})</c:if></td>
            <td><c:forEach var="qc" items="${library.libraryQCs}" end="0">${qc.insertSize}</c:forEach></td>
            <td>${library.qcPassed}</td>
            <td class="misoicon"
                onclick="window.location.href='<c:url value="/miso/library/${library.libraryId}"/>'"><span class="ui-icon ui-icon-pencil"/></td>
            <sec:authorize access="hasRole('ROLE_ADMIN')">
              <td class="misoicon" onclick="deleteLibrary(${library.libraryId}, pageReload);"><span class="ui-icon ui-icon-trash"/></td>
            </sec:authorize>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </span>
  <script type="text/javascript">
    <%--
    jQuery(document).ready(function() {
        jQuery("#library_table").tablesorter({
            headers: {
                4: {
                    sorter: false
                }
            }
        });
    });
    --%>
  </script>
</div>

<div class="sectionDivider"
     onclick="toggleLeftInfo(jQuery('#librarydils_arrowclick'), 'librarydilsdiv');">${fn:length(projectLibraryDilutions)}
  Library Dilutions
  <div id="librarydils_arrowclick" class="toggleLeft"></div>
</div>
<div id="librarydilsdiv" style="display:none;">
  <a name="librarydil"></a>

  <h1>${fn:length(projectLibraryDilutions)} Library Dilutions</h1>
  <ul class="sddm">
    <li><a
            onmouseover="mopen('librarydilsmenu')"
            onmouseout="mclosetime()">Options <span style="float:right" class="ui-icon ui-icon-triangle-1-s"></span></a>

      <div id="librarydilsmenu"
           onmouseover="mcancelclosetime()"
           onmouseout="mclosetime()">
        <c:if test="${not empty projectLibraryDilutions}">
          <c:if test="${existsAnyEmPcrLibrary}">
            <a href='javascript:void(0);' onclick='bulkEmPcrTable();' class="add">Add EmPCRs</a>
          </c:if>
          <a href="javascript:void(0);" onclick="selectLibraryDilutionBarcodesToPrint('#librarydils_table');">Print Barcodes ...</a>
          <a href='<c:url value="/miso/poolwizard/new/${project.projectId}"/>'>Create Pools</a>
        </c:if>
      </div>
    </li>
  </ul>
    <span style="clear:both">
      <table class="list" id="librarydils_table">
        <thead>
        <tr>
          <th>Dilution Name</th>
          <th>Parent Library</th>
          <th>Dilution Creator</th>
          <th>Dilution Creation Date</th>
          <th>Dilution Platform</th>
          <th>Dilution Concentration</th>
          <th class="fit">Edit</th>
          <sec:authorize access="hasRole('ROLE_ADMIN')">
            <th class="fit">DELETE</th>
          </sec:authorize>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${projectLibraryDilutions}" var="dil">
          <tr dilutionId="${dil.dilutionId}" onMouseOver="this.className='highlightrow'"
              onMouseOut="this.className='normalrow'">
            <td><b>${dil.name}</b></td>
            <td>${dil.library.alias}<c:if test="${not empty dil.library.tagBarcode}"> (${dil.library.tagBarcode.name})</c:if></td>
            <td>${dil.dilutionCreator}</td>
            <td>${dil.creationDate}</td>
            <td>${dil.library.platformName}</td>
            <td>${dil.concentration}</td>
            <td class="misoicon"
                onclick="window.location.href='<c:url value="/miso/library/${dil.library.libraryId}"/>'"><span class="ui-icon ui-icon-pencil"/></td>
            <sec:authorize access="hasRole('ROLE_ADMIN')">
              <td class="misoicon" onclick="deleteLibraryDilution(${dil.dilutionId}, pageReload);"><span class="ui-icon ui-icon-trash"/></td>
            </sec:authorize>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </span>
</div>

<div class="sectionDivider"
     onclick="toggleLeftInfo(jQuery('#pools_arrowclick'), 'poolsdiv');">${fn:length(projectPools)}
  Pools
  <div id="pools_arrowclick" class="toggleLeft"></div>
</div>
<div id="poolsdiv" style="display:none;">
  <a name="pool"></a>

  <h1>${fn:length(projectPools)} Pools</h1>
  <ul class="sddm">
    <li><a
            onmouseover="mopen('poolsmenu')"
            onmouseout="mclosetime()">Options <span style="float:right" class="ui-icon ui-icon-triangle-1-s"></span></a>

      <div id="poolsmenu"
           onmouseover="mcancelclosetime()"
           onmouseout="mclosetime()">
        <c:if test="${not empty projectPools}">
          <c:if test="${existsAnyEmPcrLibrary}">
            <a href='javascript:void(0);' onclick="addPoolEmPCR('#pools_table');" class="add">Add Pool EmPCR</a>
          </c:if>
          <a href="javascript:void(0);" onclick="selectPoolBarcodesToPrint('#pools_table');">Print Barcodes ...</a>
        </c:if>
      </div>
    </li>
  </ul>
    <span style="clear:both">
      <table class="list" id="pools_table">
        <thead>
        <tr>
          <th>Pool Name</th>
          <th>Pool Alias</th>
          <th>Pool Platform</th>
          <th>Pool Creation Date</th>
          <th>Pool Concentration</th>
          <th class="fit">Edit</th>
          <sec:authorize access="hasRole('ROLE_ADMIN')">
            <th class="fit">DELETE</th>
          </sec:authorize>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${projectPools}" var="pool">
          <tr poolId="${pool.poolId}" onMouseOver="this.className='highlightrow'"
              onMouseOut="this.className='normalrow'">
            <td><b>${pool.name}</b></td>
            <td>${pool.alias}</td>
            <td>${pool.platformType.key}</td>
            <td>${pool.creationDate}</td>
            <td>${pool.concentration}</td>
            <td class="misoicon"
                onclick="window.location.href='<c:url value="/miso/pool/${fn:toLowerCase(pool.platformType.key)}/${pool.poolId}"/>'"><span class="ui-icon ui-icon-pencil"/></td>
            <sec:authorize access="hasRole('ROLE_ADMIN')">
              <td class="misoicon" onclick="deletePool(${pool.poolId}, pageReload);"><span class="ui-icon ui-icon-trash"/></td>
            </sec:authorize>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </span>
</div>

<%--
  TODO - only show these options if some of the libraries have the right platform!
   At the moment you can create emPCRs and EmPcrDilutions for Illumina libraries!
--%>
<div class="sectionDivider"
     onclick="toggleLeftInfo(jQuery('#empcrs_arrowclick'), 'empcrsdiv');">${fn:length(projectEmPcrs)} EmPCRs
  <div id="empcrs_arrowclick" class="toggleLeft"></div>
</div>
<div id="empcrsdiv" style="display:none;">
  <a name="empcr"></a>

  <h1>${fn:length(projectEmPcrs)} EmPCRs</h1>
  <ul class="sddm">
    <li><a
            onmouseover="mopen('empcrsmenu')"
            onmouseout="mclosetime()">Options <span style="float:right" class="ui-icon ui-icon-triangle-1-s"></span></a>

      <div id="empcrsmenu"
           onmouseover="mcancelclosetime()"
           onmouseout="mclosetime()">
        <c:if test="${not empty projectEmPcrs}">
          <a href='javascript:void(0);' onclick='bulkEmPcrDilutionTable();' class="add">Add EmPCR Dilutions</a>
        </c:if>
      </div>
    </li>
  </ul>
    <span style="clear:both">
      <table class="list" id="empcrs_table">
        <thead>
        <tr>
          <th>EmPCR Name</th>
          <th>Library Dilution</th>
          <th>EmPCR Creator</th>
          <th>EmPCR Creation Date</th>
          <th>EmPCR Concentration</th>
          <th class="fit">Edit</th>
          <sec:authorize access="hasRole('ROLE_ADMIN')">
            <th class="fit">DELETE</th>
          </sec:authorize>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${projectEmPcrs}" var="pcr">
          <tr pcrId="${pcr.pcrId}" onMouseOver="this.className='highlightrow'"
              onMouseOut="this.className='normalrow'">
            <td><b>${pcr.name}</b></td>
            <td>${pcr.libraryDilution.name}</td>
            <td>${pcr.pcrCreator}</td>
            <td>${pcr.creationDate}</td>
            <td>${pcr.concentration}</td>
            <td class="misoicon"
                onclick="window.location.href='<c:url value="/miso/library/${pcr.libraryDilution.library.libraryId}"/>'"><span class="ui-icon ui-icon-pencil"/></td>
            <sec:authorize access="hasRole('ROLE_ADMIN')">
              <td class="misoicon" onclick="deleteEmPCR(${pcr.pcrId}, pageReload);"><span class="ui-icon ui-icon-trash"/></td>
            </sec:authorize>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </span>
  <script type="text/javascript">
    jQuery(document).ready(function() {
      jQuery("#librarydils_table").tablesorter({
        headers: {
          5: {
             sorter: false
          }
        }
      });
    });
  </script>
</div>

<div class="sectionDivider"
     onclick="toggleLeftInfo(jQuery('#empcrdils_arrowclick'), 'empcrdilsdiv');">${fn:length(projectEmPcrDilutions)}
  EmPCR Dilutions
  <div id="empcrdils_arrowclick" class="toggleLeft"></div>
</div>
<div id="empcrdilsdiv" style="display:none;">
  <a name="empcrdil"></a>

  <h1>${fn:length(projectEmPcrDilutions)} EmPCR Dilutions</h1>
  <ul class="sddm">
    <li><a
            onmouseover="mopen('empcrdilsmenu')"
            onmouseout="mclosetime()">Options <span style="float:right" class="ui-icon ui-icon-triangle-1-s"></span></a>

      <div id="empcrdilsmenu"
           onmouseover="mcancelclosetime()"
           onmouseout="mclosetime()">
        <c:if test="${not empty projectEmPcrDilutions}">
          <a href='<c:url value="/miso/poolwizard/new/${project.projectId}"/>'>Create Pools</a>
        </c:if>
      </div>
    </li>
  </ul>
    <span style="clear:both">
      <table class="list" id="empcrdils_table">
        <thead>
        <tr>
          <th>Dilution Name</th>
          <th>Dilution Creator</th>
          <th>Dilution Creation Date</th>
          <th>Dilution Concentration</th>
          <th class="fit">Edit</th>
          <sec:authorize access="hasRole('ROLE_ADMIN')">
            <th class="fit">DELETE</th>
          </sec:authorize>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${projectEmPcrDilutions}" var="dil">
          <tr dilutionId="${dil.dilutionId}" onMouseOver="this.className='highlightrow'"
              onMouseOut="this.className='normalrow'">
            <td><b>${dil.name}</b></td>
            <td>${dil.dilutionCreator}</td>
            <td>${dil.creationDate}</td>
            <td>${dil.concentration}</td>
            <td class="misoicon"
                onclick="window.location.href='<c:url value="/miso/library/${dil.library.libraryId}"/>'"><span class="ui-icon ui-icon-pencil"/></td>
            <sec:authorize access="hasRole('ROLE_ADMIN')">
              <td class="misoicon" onclick="deleteEmPCRDilution(${dil.dilutionId}, pageReload);"><span class="ui-icon ui-icon-trash"/></td>
            </sec:authorize>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </span>
  <script type="text/javascript">
    jQuery(document).ready(function() {
      jQuery("#librarydils_table").tablesorter({
         headers: {
           4: {
             sorter: false
           }
         }
       });
    });
  </script>
</div>

<div class="sectionDivider"
     onclick="toggleLeftInfo(jQuery('#studies_arrowclick'), 'studiesdiv');">${fn:length(project.studies)} Studies
  <div id="studies_arrowclick" class="toggleLeft"></div>
</div>
<div id="studiesdiv" style="display:none;">
  <h1>${fn:length(project.studies)} Studies</h1>
  <ul class="sddm">
    <li><a
            onmouseover="mopen('studymenu')"
            onmouseout="mclosetime()">Options <span style="float:right" class="ui-icon ui-icon-triangle-1-s"></span></a>

      <div id="studymenu"
           onmouseover="mcancelclosetime()"
           onmouseout="mclosetime()">
        <a href='<c:url value="/miso/study/new/${project.projectId}"/> '>Add new Study</a>
        <a href='<c:url value="/miso/experimentwizard/new/${project.projectId}"/> '>Create Experiments</a>
        <a href='<c:url value="/miso/poolwizard/new/${project.projectId}"/> '>Create Pools</a>
      </div>
    </li>
  </ul>
    <span style="clear:both">
      <table class="list" id="study_table">
        <thead>
        <tr>
          <th>Study Name</th>
          <th>Study Alias</th>
          <th class="fit">Edit</th>
          <sec:authorize access="hasRole('ROLE_ADMIN')">
            <th class="fit">DELETE</th>
          </sec:authorize>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${project.studies}" var="study">
          <tr studyId="${study.studyId}" onMouseOver="this.className='highlightrow'"
              onMouseOut="this.className='normalrow'">
            <td><b>${study.name}</b></td>
            <td>${study.alias}</td>
            <td class="misoicon"
                onclick="window.location.href='<c:url value="/miso/study/${study.studyId}"/>'"><span class="ui-icon ui-icon-pencil"/></td>
            <sec:authorize access="hasRole('ROLE_ADMIN')">
              <td class="misoicon" onclick="deleteStudy(${study.studyId}, pageReload);"><span class="ui-icon ui-icon-trash"/></td>
            </sec:authorize>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </span>
</div>

<div class="sectionDivider" onclick="toggleLeftInfo(jQuery('#runs_arrowclick'), 'runsdiv');">${fn:length(projectRuns)}
  Runs
  <div id="runs_arrowclick" class="toggleLeft"></div>
</div>
<div id="runsdiv" style="display:none;">
  <h1>${fn:length(projectRuns)} Runs</h1>

  <table class="list" id="run_table">
    <thead>
    <tr>
      <th>Run Name</th>
      <th>Run Alias</th>
      <th>Lanes/Chambers</th>
      <th class="fit">Edit</th>
      <sec:authorize access="hasRole('ROLE_ADMIN')">
        <th class="fit">DELETE</th>
      </sec:authorize>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${projectRuns}" var="run" varStatus="runCount">
      <tr runId="${run.runId}" onMouseOver="this.className='highlightrow'"
          onMouseOut="this.className='normalrow'">
        <td><b>${run.name}</b></td>
        <td>${run.alias}</td>
        <td>
          <c:forEach items="${run.sequencerPartitionContainers}" var="container" varStatus="fCount">
            <table class="containerSummary">
              <tr>
                <c:forEach items="${container.partitions}" var="partition">
                  <td id="partition${runCount.count}_${fCount.count}_${partition.partitionNumber}"
                      class="smallbox">${partition.partitionNumber}</td>
                  <c:forEach items="${partition.pool.experiments}" var="experiment">
                    <c:if test="${experiment.study.project.projectId eq project.projectId}">
                      <script type="text/javascript">
                        jQuery(document).ready(function() {
                          jQuery('#partition${runCount.count}_${fCount.count}_${partition.partitionNumber}').addClass("partitionOccupied");
                        });
                      </script>
                    </c:if>
                  </c:forEach>
                </c:forEach>
              </tr>
            </table>
            <c:if test="${fn:length(run.sequencerPartitionContainers) > 1}">
              <br/>
            </c:if>
          </c:forEach>
        </td>
        <td class="misoicon" onclick="window.location.href='<c:url value="/miso/run/${run.runId}"/>'"><span class="ui-icon ui-icon-pencil"/></td>
        <sec:authorize access="hasRole('ROLE_ADMIN')">
          <td class="misoicon" onclick="deleteRun(${run.runId}, pageReload);"><span class="ui-icon ui-icon-trash"/></td>
        </sec:authorize>
      </tr>
    </c:forEach>
    </tbody>
  </table>
</div>
</c:when>
</c:choose>

<div id="addProjectOverviewDialog" title="Create new Overview"></div>
<div id="addProjectOverviewNoteDialog" title="Create new Note"></div>

<script type="text/javascript">
jQuery(document).ready(function() {
  //remove any overlays
  jQuery.colorbox.remove();

  jQuery('#alias')
          .simplyCountable({
                             counter: '#aliascounter',
                             countType: 'characters',
                             maxCount: ${maxLengths['alias']},
                             countDirection: 'down'
                           });

  jQuery('#description')
          .simplyCountable({
                             counter: '#descriptioncounter',
                             countType: 'characters',
                             maxCount: ${maxLengths['description']},
                             countDirection: 'down'
                           });

  <c:if test="${not empty project.projectId}">
    getProjectIssues(${project.projectId});
  </c:if>
});

<c:if test="${not empty project.samples}">
function bulkSampleQcTable() {
  if (!jQuery('#sample_table').hasClass("display")) {
    jQuery('#sample_table').addClass("display");

    //remove edit and delete header and column
    jQuery('#sample_table tr:first th:gt(5)').remove();

    var headers = ['rowsel',
                   'name',
                   'alias',
                   'description',
                   'sampleType',
                   'receivedDate',
                   'qcPassed',
                   'qcDate',
                   'qcType',
                   'results'];

    jQuery('#sample_table').find("tr").each(function() {
      //remove rows where the sample QC has already passed
      if (jQuery(this).find("td:eq(5)").html() == "true") {
        jQuery(this).remove();
      }
      else {
        jQuery(this).removeAttr("onmouseover").removeAttr("onmouseout");
        jQuery(this).find("td:gt(5)").remove();
        jQuery(this).find("td:eq(5)").addClass("passedCheck");
      }
    });

    //headers
    jQuery("#sample_table tr:first").prepend("<th>Select <span sel='none' header='select' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='toggleSelectAll(\"#sample_table\", this);'></span></th>");
    jQuery('#sample_table tr:first th:eq(6)').html("QC Passed <span header='qcPassed' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='fillDown(\"#sample_table\", this);'></span>");
    jQuery("#sample_table tr:first").append("<th>QC Date <span header='qcDate' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='fillDown(\"#sample_table\", this);'></span></th>");
    jQuery("#sample_table tr:first").append("<th>QC Method <span header='qcType' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='fillDown(\"#sample_table\", this);'></span></th>");
    jQuery("#sample_table tr:first").append("<th>Results</th>");

    //columns
    jQuery("#sample_table tr:gt(0)").prepend("<td class='rowSelect'></td>");
    jQuery("#sample_table tr:gt(0)").append("<td class='dateSelect'></td>");
    jQuery("#sample_table tr:gt(0)").append("<td class='typeSelect'></td>");
    jQuery("#sample_table tr:gt(0)").append("<td class='defaultEditable'></td>");

    var datatable = jQuery('#sample_table').dataTable({
    "aoColumnDefs": [
      {
        "bUseRendered": false,
        "aTargets": [ 0 ]
      }
    ],
    "bPaginate": false,
    "bInfo": false,
    "bJQueryUI": true,
    "bAutoWidth": true,
    "bSort": false,
    "bFilter": false,
    "sDom": '<<"toolbar">f>r<t>ip>'
    });

    jQuery('#sample_table').find("tr:gt(0)").each(function() {
      for (var i = 0; i < this.cells.length; i++) {
        jQuery(this.cells[i]).attr("name", headers[i]);
      }
    });

    jQuery('#sample_table .rowSelect').click(function() {
      if (jQuery(this).parent().hasClass('row_selected'))
        jQuery(this).parent().removeClass('row_selected');
      else
        jQuery(this).parent().addClass('row_selected');
    });

    //jQuery("div.toolbar").parent().addClass("fg-toolbar ui-toolbar ui-widget-header ui-corner-tl ui-corner-tr ui-helper-clearfix");
    jQuery("div.toolbar").html("<button id=\"bulkSampleQcButton\" onclick=\"saveBulkSampleQc();\" class=\"fg-button ui-state-default ui-corner-all\">Save QCs</button>");
    jQuery("div.toolbar").append("<button onclick=\"pageReload();\" class=\"fg-button ui-state-default ui-corner-all\">Cancel</button>");
    jQuery("div.toolbar").removeClass("toolbar");

    jQuery('#sample_table .defaultEditable').editable(
      function(value, settings) {
        return value;
      },
      {
        callback: function(sValue, y) {
          var aPos = datatable.fnGetPosition(this);
          datatable.fnUpdate(sValue, aPos[0], aPos[1]);
        },
        submitdata: function (value, settings) {
          return {
            "row_id": this.parentNode.getAttribute('id'),
            "column": datatable.fnGetPosition(this)[2]
          };
        },
        onblur: 'submit',
        placeholder : '',
        height: '14px'
      }
    );

    jQuery(".typeSelect").editable(
      function(value, settings) {
        return value;
      },
      {
        data : '{${sampleQcTypesString}}',
        type : 'select',
        onblur: 'submit',
        placeholder : '',
        style : 'inherit',
        callback: function(sValue, y) {
          var aPos = datatable.fnGetPosition(this);
          datatable.fnUpdate(sValue, aPos[0], aPos[1]);
        },
        submitdata : function(value, settings) {
          return {
            "row_id": this.parentNode.getAttribute('id'),
            "column": datatable.fnGetPosition(this)[2]
          };
        }
      }
    );

    jQuery(".dateSelect").editable(
      function(value, settings) {
        return value;
      },
      {
        type: 'datepicker',
        width: '100px',
        onblur: 'submit',
        placeholder : '',
        style : 'inherit',
        datepicker: {
          dateFormat: 'dd/mm/yy',
          showButtonPanel: true,
          maxDate:0
        },
        callback: function(sValue, y) {
          var aPos = datatable.fnGetPosition(this);
          datatable.fnUpdate(sValue, aPos[0], aPos[1]);
        },
        submitdata : function(value, settings) {
          return {
            "row_id": this.parentNode.getAttribute('id'),
            "column": datatable.fnGetPosition(this)[2]
          };
        }
      }
    );

    jQuery(".passedCheck").editable(
      function(value, settings) {
        return value;
      },
      {
        type : 'checkbox',
        onblur: 'submit',
        placeholder : '',
        style : 'inherit',
        callback: function(sValue, y) {
          var aPos = datatable.fnGetPosition(this);
          datatable.fnUpdate(sValue, aPos[0], aPos[1]);
        },
        submitdata : function(value, settings) {
          return {
            "row_id": this.parentNode.getAttribute('id'),
            "column": datatable.fnGetPosition(this)[2]
          };
        }
      }
    );
  }
}

function generateSampleDeliveryForm(projectId) {
  if (!jQuery('#sample_table').hasClass("display")) {
    jQuery('#sample_table').addClass("display");

    //remove edit header and column
    jQuery('#sample_table tr:first th:gt(4)').remove();

    var headers = ['rowsel',
                   'name',
                   'alias',
                   'description',
                   'sampleType',
                   'qcPassed'];

    jQuery('#sample_table').find("tr").each(function() {
      jQuery(this).removeAttr("onmouseover").removeAttr("onmouseout");
      jQuery(this).find("td:gt(4)").remove();
    });

    //headers
    jQuery("#sample_table tr:first").prepend("<th>Select <span sel='none' header='select' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='toggleSelectAll(\"#sample_table\", this);'></span></th>");
    jQuery("#sample_table tr:gt(0)").prepend("<td class='rowSelect'></td>");

    var datatable = jQuery('#sample_table')
            .dataTable({
                         "aoColumnDefs": [
                           {
                             "bUseRendered": false,
                             "aTargets": [ 0 ]
                           }
                         ],
                         "bPaginate": false,
                         "bInfo": false,
                         "bJQueryUI": true,
                         "bAutoWidth": true,
                         "bSort": false,
                         "bFilter": false,
                         "sDom": '<<"toolbar">f>r<t>ip>'
                       });

    jQuery('#sample_table').find("tr:gt(0)").each(function() {
      for (var i = 0; i < this.cells.length; i++) {
        jQuery(this.cells[i]).attr("name", headers[i]);
      }
    });

    jQuery('#sample_table .rowSelect').click(function() {
      if (jQuery(this).parent().hasClass('row_selected'))
        jQuery(this).parent().removeClass('row_selected');
      else
        jQuery(this).parent().addClass('row_selected');
    });

    jQuery("div.toolbar").html("<button onclick=\"processSampleDeliveryForm(${project.projectId});\" class=\"fg-button ui-state-default ui-corner-all\">Generate Form</button>");
  }
}
</c:if>

<c:if test="${not empty projectLibraries}">
function bulkLibraryQcTable() {
  if (!jQuery('#library_table').hasClass("display")) {
    jQuery('#library_table').addClass("display");

    //remove edit header and column
    jQuery('#library_table tr:first th:gt(7)').remove();
    jQuery('#library_table tr:first th:eq(5)').remove();

    var libraryheaders = ['rowsel',
                          'name',
                          'alias',
                          'description',
                          'libraryType',
                          'platform',
                          //'tagBarcode',
                          'insertSize',
                          'qcPassed',
                          'qcDate',
                          'qcType',
                          'results'];

    jQuery('#library_table').find("tr").each(function() {
      if (jQuery(this).find("td:eq(7)").html() == "true") {
        jQuery(this).remove();
      }
      else {
        jQuery(this).removeAttr("onmouseover").removeAttr("onmouseout");
        jQuery(this).find("td:gt(7)").remove();
        jQuery(this).find("td:eq(5)").remove();
        jQuery(this).find("td:eq(7)").addClass("passedCheck");
      }
    });

    //headers
    jQuery("#library_table tr:first").prepend("<th>Select <span sel='none' header='select' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='toggleSelectAll(\"#library_table\", this);'></span></th>");
    jQuery('#library_table tr:first th:eq(7)').html("QC Passed <span header='qcPassed' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='fillDown(\"#library_table\", this);'></span>");
    jQuery("#library_table tr:first").append("<th>QC Date <span header='qcDate' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='fillDown(\"#library_table\", this);'></span></th>");
    jQuery("#library_table tr:first").append("<th>QC Method <span header='qcType' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='fillDown(\"#library_table\", this);'></span></th>");
    <%--jQuery("#library_table tr:first").append("<th>Insert Size</th>");--%>
    jQuery("#library_table tr:first").append("<th>Concentration</th>");

    //columns
    jQuery("#library_table tr:gt(0)").prepend("<td class='rowSelect'></td>");
    jQuery("#library_table tr:gt(0)").find("td:eq(6)").addClass("defaultEditable");
    jQuery("#library_table tr:gt(0)").find("td:eq(7)").addClass("passedCheck");
    jQuery("#library_table tr:gt(0)").append("<td class='dateSelect'></td>");
    jQuery("#library_table tr:gt(0)").append("<td class='typeSelect'></td>");
    jQuery("#library_table tr:gt(0)").append("<td class='defaultEditable'></td>");

    var datatable = jQuery('#library_table')
            .dataTable({
                         "aoColumnDefs": [
                           {
                             "bUseRendered": false,
                             "aTargets": [ 0 ]
                           }
                         ],
                         "aoColumns": [
                           {"bSortable": false},
                           null,
                           null,
                           null,
                           null,
                           null,
                           null,
                           {"bSortable": false},
                           {"bSortable": false},
                           {"bSortable": false},
                           {"bSortable": false}
                         ],
                         "bPaginate": false,
                         "bInfo": false,
                         "bJQueryUI": true,
                         "bAutoWidth": true,
                         "bSort": true,
                         "bFilter": true,
                         "sDom": '<<"toolbar">f>r<t>ip>'
                       });

    jQuery('#library_table').find("tr:gt(0)").each(function() {
      for (var i = 0; i < this.cells.length; i++) {
        jQuery(this.cells[i]).attr("name", libraryheaders[i]);
      }
    });

    jQuery('#library_table .rowSelect').click(function() {
      if (jQuery(this).parent().hasClass('row_selected'))
        jQuery(this).parent().removeClass('row_selected');
      else
        jQuery(this).parent().addClass('row_selected');
    });

    //jQuery("div.toolbar").parent().addClass("fg-toolbar ui-toolbar ui-widget-header ui-corner-tl ui-corner-tr ui-helper-clearfix");
    jQuery("div.toolbar").html("<button id=\"bulkLibraryQcButton\" onclick=\"saveBulkLibraryQc();\" class=\"fg-button ui-state-default ui-corner-all\">Save QCs</button>");
    jQuery("div.toolbar").append("<button onclick=\"pageReload();\" class=\"fg-button ui-state-default ui-corner-all\">Cancel</button>");
    jQuery("div.toolbar").removeClass("toolbar");

    jQuery('#library_table .defaultEditable')
            .editable(function(value, settings) {
                        return value;
                      },
                      {
                        callback: function(sValue, y) {
                          var aPos = datatable.fnGetPosition(this);
                          datatable.fnUpdate(sValue, aPos[0], aPos[1]);
                        },
                        submitdata: function (value, settings) {
                          return {
                            "row_id": this.parentNode.getAttribute('id'),
                            "column": datatable.fnGetPosition(this)[2]
                          };
                        },
                        onblur: 'submit',
                        placeholder : '',
                        height: '14px'
                      });

    jQuery("#library_table .typeSelect")
            .editable(function(value, settings) {
                        return value;
                      },
                      {
                        data : '{${libraryQcTypesString}}',
                        type : 'select',
                        onblur: 'submit',
                        placeholder : '',
                        style : 'inherit',
                        callback: function(sValue, y) {
                          var aPos = datatable.fnGetPosition(this);
                          datatable.fnUpdate(sValue, aPos[0], aPos[1]);
                        },
                        submitdata : function(value, settings) {
                          return {
                            "row_id": this.parentNode.getAttribute('id'),
                            "column": datatable.fnGetPosition(this)[2]
                          };
                        }
                      });

    jQuery("#library_table .dateSelect")
            .editable(function(value, settings) {
                        return value;
                      },
                      {
                        type: 'datepicker',
                        width: '100px',
                        onblur: 'submit',
                        placeholder : '',
                        style : 'inherit',
                        datepicker: {
                          dateFormat: 'dd/mm/yy',
                          showButtonPanel: true,
                          maxDate:0
                        },
                        callback: function(sValue, y) {
                          var aPos = datatable.fnGetPosition(this);
                          datatable.fnUpdate(sValue, aPos[0], aPos[1]);
                        },
                        submitdata : function(value, settings) {
                          return {
                            "row_id": this.parentNode.getAttribute('id'),
                            "column": datatable.fnGetPosition(this)[2]
                          };
                        }
                      });

    jQuery("#library_table .passedCheck")
            .editable(function(value, settings) {
                        return value;
                      },
                      {
                        type : 'checkbox',
                        onblur: 'submit',
                        placeholder : '',
                        style : 'inherit',
                        callback: function(sValue, y) {
                          var aPos = datatable.fnGetPosition(this);
                          datatable.fnUpdate(sValue, aPos[0], aPos[1]);
                        },
                        submitdata : function(value, settings) {
                          return {
                            "row_id": this.parentNode.getAttribute('id'),
                            "column": datatable.fnGetPosition(this)[2]
                          };
                        }
                      });
  }
}

function bulkLibraryDilutionTable() {
  if (!jQuery('#library_table').hasClass("display")) {
    jQuery('#library_table').addClass("display");

    //remove edit header and column
    jQuery('#library_table tr:first th:gt(7)').remove();
    jQuery('#library_table tr:first th:eq(5)').remove();

    var dilutionheaders = ['rowsel',
                           'name',
                           'alias',
                           'description',
                           'libraryType',
                           'platform',
                           'insertSize',
                           'qcPassed',
                           'dilutionDate',
                           'results'];

    jQuery('#library_table').find("tr").each(function() {
      if (jQuery(this).find("td:eq(7)").html() == "false") {
        jQuery(this).remove();
      }
      else {
        jQuery(this).removeAttr("onmouseover").removeAttr("onmouseout");
        jQuery(this).find("td:gt(7)").remove();
        jQuery(this).find("td:eq(5)").remove();
      }
    });

    //headers
    jQuery("#library_table tr:first").prepend("<th>Select <span sel='none' header='select' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='toggleSelectAll(\"#library_table\", this);'></span></th>");
    jQuery("#library_table tr:first").append("<th>Dilution Date <span header='dilutionDate' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='fillDown(\"#library_table\", this);'></span></th>");
    jQuery("#library_table tr:first").append("<th>Concentration <span header='qcType' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='fillDown(\"#library_table\", this);'></span></th>");

    //columns
    jQuery("#library_table tr:gt(0)").prepend("<td class='rowSelect'></td>");
    jQuery("#library_table tr:gt(0)").append("<td class='dateSelect'></td>");
    jQuery("#library_table tr:gt(0)").append("<td class='defaultEditable'></td>");

    var datatable = jQuery('#library_table')
            .dataTable({
                         "aoColumnDefs": [
                           {
                             "bUseRendered": false,
                             "aTargets": [ 0 ]
                           }
                         ],
                         "aoColumns": [
                           {"bSortable": false},
                           null,
                           null,
                           null,
                           null,
                           {"bSortable": false},
                           {"bSortable": false},
                           {"bSortable": false},
                           {"bSortable": false},
                           {"bSortable": false}
                         ],
                         "bPaginate": false,
                         "bInfo": false,
                         "bJQueryUI": true,
                         "bAutoWidth": true,
                         "bSort": false,
                         "bFilter": false,
                         "sDom": '<<"toolbar">f>r<t>ip>'
                       });

    jQuery('#library_table').find("tr:gt(0)").each(function() {
      for (var i = 0; i < this.cells.length; i++) {
        jQuery(this.cells[i]).attr("name", dilutionheaders[i]);
      }
    });

    jQuery('#library_table .rowSelect').click(function() {
      if (jQuery(this).parent().hasClass('row_selected'))
        jQuery(this).parent().removeClass('row_selected');
      else
        jQuery(this).parent().addClass('row_selected');
    });

    jQuery("div.toolbar").html("<button id=\"bulkLibraryDilutionButton\" onclick=\"saveBulkLibraryDilutions();\" class=\"fg-button ui-state-default ui-corner-all\">Save Dilutions</button>");
    jQuery("div.toolbar").append("<button onclick=\"pageReload();\" class=\"fg-button ui-state-default ui-corner-all\">Cancel</button>");
    jQuery("div.toolbar").removeClass("toolbar");

    jQuery('#library_table .defaultEditable')
            .editable(function(value, settings) {
                        return value;
                      },
                      {
                        callback: function(sValue, y) {
                          var aPos = datatable.fnGetPosition(this);
                          datatable.fnUpdate(sValue, aPos[0], aPos[1]);
                        },
                        submitdata: function (value, settings) {
                          return {
                            "row_id": this.parentNode.getAttribute('id'),
                            "column": datatable.fnGetPosition(this)[2]
                          };
                        },
                        onblur: 'submit',
                        placeholder : '',
                        height: '14px'
                      });

    jQuery("#library_table .dateSelect")
            .editable(function(value, settings) {
                        return value;
                      },
                      {
                        type: 'datepicker',
                        width: '100px',
                        onblur: 'submit',
                        placeholder : '',
                        style : 'inherit',
                        datepicker: {
                          dateFormat: 'dd/mm/yy',
                          showButtonPanel: true,
                          maxDate:0
                        },
                        callback: function(sValue, y) {
                          var aPos = datatable.fnGetPosition(this);
                          datatable.fnUpdate(sValue, aPos[0], aPos[1]);
                        },
                        submitdata : function(value, settings) {
                          return {
                            "row_id": this.parentNode.getAttribute('id'),
                            "column": datatable.fnGetPosition(this)[2]
                          };
                        }
                      });
  }
}
</c:if>

<c:if test="${existsAnyEmPcrLibrary and not empty projectLibraryDilutions}">
function bulkEmPcrTable() {
  if (!jQuery('#librarydils_table').hasClass("display")) {
    jQuery('#librarydils_table').addClass("display");
    //remove edit header and column
    jQuery('#librarydils_table tr:first th:gt(4)').remove();

    var dilutionheaders = ['rowsel',
                           'dilName',
                           'dilCreator',
                           'dilDate',
                           'libPlatform',
                           'dilConc',
                           'pcrDate',
                           'results'];

    jQuery('#librarydils_table').find("tr").each(function() {
      jQuery(this).removeAttr("onmouseover").removeAttr("onmouseout");
      jQuery(this).find("td:gt(4)").remove();
    });

    //headers
    jQuery("#librarydils_table tr:first").prepend("<th>Select <span sel='none' header='select' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='toggleSelectAll(\"#librarydils_table\", this);'></span></th>");
    jQuery("#librarydils_table tr:first").append("<th>PCR Date <span header='pcrDate' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='fillDown(\"#librarydils_table\", this);'></span></th>");
    jQuery("#librarydils_table tr:first").append("<th>Concentration</th>");

    //columns
    jQuery("#librarydils_table tr:gt(0)").prepend("<td class='rowSelect'></td>");
    jQuery("#librarydils_table tr:gt(0)").append("<td class='dateSelect'></td>");
    jQuery("#librarydils_table tr:gt(0)").append("<td class='defaultEditable'></td>");

    var datatable = jQuery('#librarydils_table')
            .dataTable({
                         "aoColumnDefs": [
                           {
                             "bUseRendered": false,
                             "aTargets": [ 0 ]
                           }
                         ],
                         "aoColumns": [
                           {"bSortable": false},
                           null,
                           null,
                           null,
                           null,
                           null,
                           {"bSortable": false},
                           {"bSortable": false}
                         ],
                         "bPaginate": false,
                         "bInfo": false,
                         "bJQueryUI": true,
                         "bAutoWidth": true,
                         "bSort": false,
                         "bFilter": false,
                         "sDom": '<<"toolbar">f>r<t>ip>'
                       });

    jQuery('#librarydils_table').find("tr:gt(0)").each(function() {
      for (var i = 0; i < this.cells.length; i++) {
        jQuery(this.cells[i]).attr("name", dilutionheaders[i]);
      }

      var platform = jQuery(this.cells[4]).text();
      if (platform === "Illumina") {
        datatable.fnDeleteRow(this);
      }
    });

    jQuery('#librarydils_table .rowSelect').click(function() {
      if (jQuery(this).parent().hasClass('row_selected'))
        jQuery(this).parent().removeClass('row_selected');
      else
        jQuery(this).parent().addClass('row_selected');
    });

    jQuery("div.toolbar").html("<button id=\"bulkEmPcrButton\" onclick=\"saveBulkEmPcrs();\" class=\"fg-button ui-state-default ui-corner-all\">Save EmPCRs</button>");
    jQuery("div.toolbar").append("<button onclick=\"pageReload();\" class=\"fg-button ui-state-default ui-corner-all\">Cancel</button>");
    jQuery("div.toolbar").removeClass("toolbar");

    jQuery('#librarydils_table .defaultEditable')
            .editable(function(value, settings) {
                        return value;
                      },
                      {
                        callback: function(sValue, y) {
                          var aPos = datatable.fnGetPosition(this);
                          datatable.fnUpdate(sValue, aPos[0], aPos[1]);
                        },
                        submitdata: function (value, settings) {
                          return {
                            "row_id": this.parentNode.getAttribute('id'),
                            "column": datatable.fnGetPosition(this)[2]
                          };
                        },
                        onblur: 'submit',
                        placeholder : '',
                        height: '14px'
                      });

    jQuery("#librarydils_table .dateSelect")
            .editable(function(value, settings) {
                        return value;
                      },
                      {
                        type: 'datepicker',
                        width: '100px',
                        onblur: 'submit',
                        placeholder : '',
                        style : 'inherit',
                        datepicker: {
                          dateFormat: 'dd/mm/yy',
                          showButtonPanel: true,
                          maxDate:0
                        },
                        callback: function(sValue, y) {
                          var aPos = datatable.fnGetPosition(this);
                          datatable.fnUpdate(sValue, aPos[0], aPos[1]);
                        },
                        submitdata : function(value, settings) {
                          return {
                            "row_id": this.parentNode.getAttribute('id'),
                            "column": datatable.fnGetPosition(this)[2]
                          };
                        }
                      });
  }
}
</c:if>

<c:if test="${not empty projectEmPcrs}">
function bulkEmPcrDilutionTable() {
  if (!jQuery('#empcrs_table').hasClass("display")) {
    jQuery('#empcrs_table').addClass("display");
    //remove edit header and column
    jQuery('#empcrs_table tr:first th:gt(4)').remove();

    var dilutionheaders = ['rowsel',
                           'pcrName',
                           'libDilName',
                           'pcrCreator',
                           'pcrDate',
                           'pcrConc',
                           'pcrDilutionDate',
                           'results'];

    jQuery('#empcrs_table').find("tr").each(function() {
      jQuery(this).removeAttr("onmouseover").removeAttr("onmouseout");
      jQuery(this).find("td:gt(4)").remove();
    });

    //headers
    jQuery("#empcrs_table tr:first").prepend("<th>Select <span sel='none' header='select' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='toggleSelectAll(\"#empcrs_table\", this);'></span></th>");
    jQuery("#empcrs_table tr:first").append("<th>Dilution Date <span header='pcrDilutionDate' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='fillDown(\"#empcrs_table\", this);'></span></th>");
    jQuery("#empcrs_table tr:first").append("<th>Concentration <span header='qcType' class='ui-icon ui-icon-arrowstop-1-s' style='float:right' onclick='fillDown(\"#empcrs_table\", this);'></span></th>");

    //columns
    jQuery("#empcrs_table tr:gt(0)").prepend("<td class='rowSelect'></td>");
    jQuery("#empcrs_table tr:gt(0)").append("<td class='dateSelect'></td>");
    jQuery("#empcrs_table tr:gt(0)").append("<td class='defaultEditable'></td>");

    var datatable = jQuery('#empcrs_table')
            .dataTable({
                         "aoColumnDefs": [
                           {
                             "bUseRendered": false,
                             "aTargets": [ 0 ]
                           }
                         ],
                         "aoColumns": [
                           {"bSortable": false},
                           null,
                           null,
                           null,
                           null,
                           null,
                           {"bSortable": false},
                           {"bSortable": false}
                         ],
                         "bPaginate": false,
                         "bInfo": false,
                         "bJQueryUI": true,
                         "bAutoWidth": true,
                         "bSort": false,
                         "bFilter": false,
                         "sDom": '<<"toolbar">f>r<t>ip>'
                       });

    jQuery('#empcrs_table').find("tr:gt(0)").each(function() {
      for (var i = 0; i < this.cells.length; i++) {
        jQuery(this.cells[i]).attr("name", dilutionheaders[i]);
      }
    });

    jQuery('#empcrs_table .rowSelect').click(function() {
      if (jQuery(this).parent().hasClass('row_selected'))
        jQuery(this).parent().removeClass('row_selected');
      else
        jQuery(this).parent().addClass('row_selected');
    });

    jQuery("div.toolbar").html("<button id=\"bulkEmPcrDilutionButton\" onclick=\"saveBulkEmPcrDilutions();\" class=\"fg-button ui-state-default ui-corner-all\">Save Dilutions</button>");
    jQuery("div.toolbar").append("<button onclick=\"pageReload();\" class=\"fg-button ui-state-default ui-corner-all\">Cancel</button>");
    jQuery("div.toolbar").removeClass("toolbar");

    jQuery('#empcrs_table .defaultEditable')
            .editable(function(value, settings) {
                        return value;
                      },
                      {
                        callback: function(sValue, y) {
                          var aPos = datatable.fnGetPosition(this);
                          datatable.fnUpdate(sValue, aPos[0], aPos[1]);
                        },
                        submitdata: function (value, settings) {
                          return {
                            "row_id": this.parentNode.getAttribute('id'),
                            "column": datatable.fnGetPosition(this)[2]
                          };
                        },
                        onblur: 'submit',
                        placeholder : '',
                        height: '14px'
                      });

    jQuery("#empcrs_table .dateSelect")
            .editable(function(value, settings) {
                        return value;
                      },
                      {
                        type: 'datepicker',
                        width: '100px',
                        onblur: 'submit',
                        placeholder : '',
                        style : 'inherit',
                        datepicker: {
                          dateFormat: 'dd/mm/yy',
                          showButtonPanel: true,
                          maxDate:0
                        },
                        callback: function(sValue, y) {
                          var aPos = datatable.fnGetPosition(this);
                          datatable.fnUpdate(sValue, aPos[0], aPos[1]);
                        },
                        submitdata : function(value, settings) {
                          return {
                            "row_id": this.parentNode.getAttribute('id'),
                            "column": datatable.fnGetPosition(this)[2]
                          };
                        }
                      });
  }
}
</c:if>

<c:if test="${not empty project.projectId}">
  var duration = 500, i = 0, root;
  var r = 960 / 2;

  var tree = d3.layout.tree()
          .size([360, r - 120])
          .separation(function(a, b) {
                        return (a.parent == b.parent ? 1 : 2) / a.depth;
                      });

  var diagonal = d3.svg.diagonal.radial()
          .projection(function(d) {
                        return [d.y, d.x / 180 * Math.PI];
                      });
  var vis = d3.select("#chart").append("svg:svg")
          .attr("width", r * 2)
          .attr("height", r * 2 - 150)
          .append("svg:g")
          .attr("transform", "translate(" + r + "," + r + ")");

  d3.json("/miso/d3graph/project/" + ${project.projectId}, function(json) {
    json.x0 = 800;
    json.y0 = 0;
    update(root = json);
  });

  function update(source) {

    // Compute the new tree layout.
    var nodes = tree.nodes(root);

    // Update the links…
    var link = vis.selectAll("path.link")
            .data(tree.links(nodes), function(d) {
                    return d.target.id;
                  });

    // Enter any new links at the parent's previous position.
    link.enter().append("svg:path")

            .attr("class", "link")
            .attr("d", diagonal)
            .transition()
            .duration(duration)
            .attr("d", diagonal);

    // Transition links to their new position.
    link.transition()
            .duration(duration)
            .attr("d", diagonal);

    // Transition exiting nodes to the parent's new position.
    link.exit().transition()
            .duration(duration)
            .attr("d", diagonal)
            .remove();

    var node = vis.selectAll("circle.node")
            .data(nodes);

    // Enter any new nodes at the parent's previous position.
    node.enter().append("svg:circle")
            .attr("dx", function(d) {
                    return source.x0;
                  })
            .attr("dy", function(d) {
                    return source.y0;
                  })
            .attr("r", 4)

            .attr("class", "node")
            .style("fill", function(d) {
                     return d._children ? "lightsteelblue" : "#fff";
                   })
            .style("stroke", function(d) {
                           return d.color == 0 ? "red"
                                   : d.color == 1 ? "lightgreen"
                                   : d.color == 2 ? "gray" :
                                     "steelblue";
                       })
            .style("stroke-width", "1px")
            .attr("transform", function(d) {
                    return "rotate (" + (source.x0 - 90) + ")translate(" + source.y0 + ")";
                  })
            .style("stroke", function(d) {
                     return d.color == 0 ? "red"
                             : d.color == 1 ? "lightgreen"
                             : d.color == 2 ? "gray" :
                               "steelblue";
                   })

            .on("click", click)
            .append("svg:title")
            .text(function(d) {
                    return d.description;
                  })
            .transition()
            .duration(duration)
            .attr("dx", function(d) {
                    return d.x;
                  })
            .attr("dy", function(d) {
                    return d.y;
                  })
            .attr("transform", function(d) {
                    return "rotate (" + (d.x - 90) + ")translate(" + d.y + ")";
                  });

    // Transition nodes to their new position.
    node.transition()
            .duration(duration)
            .attr("dx", function(d) {
                    return d.x;
                  })
            .attr("dy", function(d) {
                    return d.y;
                  })
            .style("fill", function(d) {
                     return d._children ? "lightsteelblue" : "#fff";
                   })
            .style("stroke", function(d) {
                     return d.color == 0 ? "red"
                             : d.color == 1 ? "lightgreen"
                             : d.color == 2 ? "gray" :
                               "steelblue";
                   })
            .attr("transform", function(d) {
                    return "rotate (" + (d.x - 90) + ")translate(" + d.y + ")";
                  });


    // Transition exiting nodes to the parent's new position.
    node.exit().transition()
            .duration(duration)

            .attr("dx", function(d) {
                    return source.x;
                  })
            .attr("dy", function(d) {
                    return source.y;
                  })
            .remove();


    // Update the texts…

    var text = vis.selectAll("text")
            .data(nodes, function(d) {
                    return d.name;
                  });

    text.enter().append("svg:text")
            .attr("class", "node1")
            .attr("dx", "8")
            .attr("dy", ".31em")
            .attr("transform", function(d) {
                    return "rotate (" + (d.x - 90) + ")translate(" + d.y + ")";
                  })
            .text(function(d) {
                    return d.name;
                  })
            .attr("text-anchor", "start")
            .transition()
            .duration(duration)
            .attr("transform", function(d) {
                    return "rotate (" + (d.x - 90) + ")translate(" + d.y + ")";
                  })
            .attr("dx", "8")
            .attr("dy", ".31em");

    text.transition()
            .duration(duration)
            .attr("class", "node")
            .attr("dx", "8")
            .attr("dy", ".31em")
            .attr("transform", function(d) {
                    return "rotate (" + (d.x - 90) + ")translate(" + d.y + ")";
                  });


    // Transition exiting nodes to the parent's new position.
    text.exit().transition()
            .duration(duration)
            .attr("dx", "8")
            .attr("dy", "31em")
            .remove();


    // Stash the old positions for transition.
    nodes.forEach(function(d) {
      d.x0 = d.x;
      d.y0 = d.y;
    });
  }

  // Toggle children on click.
  function click(d) {
    if (d.children) {
      d._children = d.children;
      d.children = null;
    }
    else {
      d.children = d._children;
      d._children = null;
    }
    update(d);
  }

  d3.select(self.frameElement).style("height", "1000px");
</c:if>

</script>

<%@ include file="../footer.jsp" %>