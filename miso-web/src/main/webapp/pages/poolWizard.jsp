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
<script type="text/javascript" src="<c:url value='/scripts/jquery/js/jquery.breadcrumbs.popup.js'/>"></script>
<script src="<c:url value='/scripts/datatables_utils.js?ts=${timestamp.time}'/>" type="text/javascript"></script>
<script src="<c:url value='/scripts/jquery/datatables/jquery.dataTables.min.js'/>" type="text/javascript"></script>
<link rel="stylesheet" href="<c:url value='/scripts/jquery/datatables/datatable.css'/>" type="text/css">

<div id="maincontent">
  <div id="contentcolumn">
    <h1>Create Pool Wizard</h1>
    <div class="breadcrumbs">
      <ul>
        <li><a href='<c:url value="/"/>'>Home</a></li>
        <li>
          <div class="breadcrumbsbubbleInfo">
            <div class="trigger">
              <a href='<c:url value="/miso/project/${project.projectId}"/>'>${project.name}</a>
            </div>
            <div class="breadcrumbspopup">${project.alias}</div>
          </div>
        </li>
      </ul>
    </div>
    <br/><br/>

    <div id="studyTriggerDiv">
      <input id="newStudyTrigger" type="radio" onchange="jQuery('#newStudyForm').slideToggle();"/> Create a new Study
      <%-- <input id="selectStudyTrigger" type="radio" onchange="jQuery('#selectStudyForm').slideToggle();"/> Select an existing Study --%>
    </div>
    <div id="newStudyForm" style="display:none;">
      <button onClick="addStudy('newStudy');"
              class="fg-button ui-state-default ui-corner-all">Save Study
      </button>
      <form id="newStudy" method="POST" autocomplete="off">
        <table>
          <tbody>
          <tr>
            <td>Study Type:</td>
            <td>
              <select name="studyType">${studyTypes}</select>
            </td>
          </tr>
          <tr>
            <td>Study Description:</td>
            <td>
              <input type="text" id="studyDescription" name="studyDescription"/>
            </td>
          </tr>
          </tbody>
        </table>
      </form>
      <br/>
    </div>
      <%--
    <div id="selectStudyForm" style="display:none;">
        <form:select id="studies" path="study">
          <form:option value="" label="Select study..."/>
          <form:options items="${existingStudies}" itemLabel="alias" itemValue="studyId"/>
        </form:select>
    </div>
      --%>
    <hr/>
    <br/>
    This system will create <b>ONE</b> pool for each of the selected dilutions below:
    <br/>

    <table>
      <tbody>
      <tr>
        <td>Platform Type:</td>
        <td>
          <select id="platformType" name="platformType" onchange="selectDilutionsByPlatform();">${platforms}</select>
        </td>
      </tr>
      <tr>
        <td>Pool Alias:</td>
        <td>
          <input type="text" id="alias" name="alias"/><br/>
        </td>
      </tr>
      <tr>
        <td>Pool Concentration:</td>
        <td>
          <input type="text" id="concentration" name="concentration"/><br/>
        </td>
      </tr>
      <tr>
        <td>Remove selected dilutions?</td>
        <td>
          <input id="removeDilutions" type="checkbox"/>
        </td>
      </tr>
      </tbody>
    </table>
    
    <table width="100%">
      <tbody>
      <tr>
        <td width="50%" valign="top">
          <div class="simplebox ui-corner-all">
            <h2>Available Dilutions</h2>
            <button id="createPoolButton" onClick="createNewPool();"
                    class="fg-button ui-state-default ui-corner-all">Create New Pool
            </button>
            <table id="dlTable" class="display">
              <thead>
              <tr>
                <th>Select <span sel="none" header="select" class="ui-icon ui-icon-arrowstop-1-s" style="float:right"
                                 onclick="toggleSelectAll('#dlTable', this);"></span>
                </th>
                <th>Dilution ID</th>
                <th>Dilution Name</th>
                <th>Parent Library</th>
                <th>Description</th>
                <th>Parent Library Barcode</th>
              </tr>
              </thead>
              <tbody id="dilutions">
              </tbody>
            </table>
          </div>
        </td>
        <td width="50%" valign="top">
          <div class="simplebox ui-corner-all">
            <h2>Created Pools</h2>
            <div id="poolResult"></div>
          </div>
        </td>
      </tr>
      </tbody>
    </table>
    </div>
</div>

<script type="text/javascript">
  var headers = ['rowsel','dilutionId','dilutionName','library','description','libraryBarcode'];

  function addStudy(form) {
    if (jQuery('#studyDescription').val() == "") {
      alert('You have not entered a study description');
    }
    else {
      Fluxion.doAjax(
              'poolWizardControllerHelperService',
              'addStudy',
      {'form':jQuery('#' + form).serializeArray(), 'projectId':${project.projectId}, 'url':ajaxurl},
      {'doOnSuccess':function(json) {
        jQuery('#newStudyForm').html(json.html);
        jQuery('#studyTriggerDiv').html("");
      }
      });
    }
  }

  function selectDilutionsByPlatform() {
    Fluxion.doAjax(
            'poolWizardControllerHelperService',
            'populateDilutions',
    {'platformType':jQuery('#platformType').val(), 'projectId':${project.projectId}, 'url':ajaxurl},
    {'doOnSuccess':function(json) {
      var table = jQuery('#dlTable').dataTable();
      table.fnClearTable();

      jQuery.each(json.dilutions, function(index, value) {
        var a = table.fnAddData(["",value.id, value.name, value.library, value.description, value.libraryBarcode]);
        var nTr = table.fnSettings().aoData[a[0]].nTr;
        jQuery(nTr.cells[0]).addClass("rowSelect");
      });

      jQuery('#dlTable .rowSelect').click(function() {
        if (jQuery(this).parent().hasClass('row_selected')) {
          jQuery(this).parent().removeClass('row_selected');
        }
        else {
          jQuery(this).parent().addClass('row_selected');
        }
      });
    }
    });
  }

  function createNewPool() {
    if (jQuery('#concentration').val() == null || jQuery('#concentration').val() == "") {
      alert('You have not enterted a concentration for the new pool');
    }
    else {
      jQuery('#createPoolButton').attr('disabled', 'disabled');
      jQuery('#createPoolButton').html("Processing...");

      var table = jQuery('#dlTable').dataTable();
      var nodes = fnGetSelected(table);
      var arr = [];
      for (var i = 0; i < nodes.length; i++) {
        var obj = {};
        for (var j = 1; j < (nodes[i].cells.length); j++) {
            obj[headers[j]] = jQuery(nodes[i].cells[j]).text();
        }
        arr.push(JSON.stringify(obj));
      }

      Fluxion.doAjax(
              'poolWizardControllerHelperService',
              'addPool',
      {'dilutions':"[" + arr.join(',') + "]",
       'platformType':jQuery('#platformType').val(),
       'alias':jQuery('#alias').val(),
       'concentration':jQuery('#concentration').val(),
       'url':ajaxurl },
      {'doOnSuccess':function(json) {
        jQuery('#poolResult').append(json.html);
        if (jQuery("#removeDilutions").attr('checked')) {
          for (var i = 0; i < nodes.length; i++) {
            table.fnDeleteRow(nodes[i]);
          }
        }
        jQuery('#concentration').val("");
        jQuery('#alias').val("");
        jQuery('#createPoolButton').removeAttr('disabled');
        jQuery('#createPoolButton').html("Create New Pool");
      },
      'doOnError':function(json) {
        alert(json.error);
        jQuery('#createPoolButton').removeAttr('disabled');
        jQuery('#createPoolButton').html("Create New Pool");
      }
      });
    }
  }

  function fnGetSelected(datatable) {
    var aReturn = new Array();
    var aTrs = datatable.fnGetNodes();
    for (var i = 0; i < aTrs.length; i++) {
      if (jQuery(aTrs[i]).hasClass('row_selected')) {
        aReturn.push(aTrs[i]);
      }
    }
    return aReturn;
  }

  jQuery(document).ready(function() {
    Fluxion.doAjax(
            'poolWizardControllerHelperService',
            'populateDilutions',
    {'platformType':"Illumina", 'projectId':${project.projectId}, 'url':ajaxurl},
    {'doOnSuccess':function(json) {
      var table = jQuery('#dlTable').dataTable({
        "aoColumnDefs": [
          {
            "bUseRendered": false,
            "aTargets": [ 0 ]
          }
        ],
        "bPaginate": false,
        "bInfo": true,
        "bJQueryUI": true,
        "bAutoWidth": true,
        "bSort": false,
        "bFilter": true,
        "sDom": '<<"toolbar">f>r<t>ip>'
      });

      jQuery.each(json.dilutions, function(index, value) {
        var a = table.fnAddData(["",value.id, value.name, value.library, value.description, value.libraryBarcode]);
        var nTr = table.fnSettings().aoData[a[0]].nTr;
        jQuery(nTr.cells[0]).addClass("rowSelect");
      });

      jQuery('#dlTable .rowSelect').click(function() {
        if (jQuery(this).parent().hasClass('row_selected')) {
          jQuery(this).parent().removeClass('row_selected');
        }
        else {
          jQuery(this).parent().addClass('row_selected');
        }
      });
    }
    });
  });
</script>

<%@ include file="adminsub.jsp" %>

<%@ include file="../footer.jsp" %>