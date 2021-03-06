/*
 * Copyright (c) 2012. The Genome Analysis Centre, Norwich, UK
 * MISO project contacts: Robert Davey, Mario Caccamo @ TGAC
 * *********************************************************************
 *
 * This file is part of MISO.
 *
 * MISO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MISO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MISO.  If not, see <http://www.gnu.org/licenses/>.
 *
 * *********************************************************************
 */

function changePlatformName(input) {
  var platform = jQuery(input).val();
  Fluxion.doAjax(
          'libraryControllerHelperService',
          'changePlatformName',
  {'platform':platform, 'url':ajaxurl},
  {'doOnSuccess':processPlatformChange}
  );
}

var processPlatformChange = function(json) {
  jQuery('#libraryTypes').html(json.libraryTypes);
  jQuery('#tagBarcodes').html(json.tagBarcodes);
};

function insertLibraryQCRow(libraryId, includeId) {
  if (!jQuery('#libraryQcTable').attr("qcInProgress")) {
    jQuery('#libraryQcTable').attr("qcInProgress", "true");

    $('libraryQcTable').insertRow(1);
    //QCId  QCed By  	QC Date  	Method  	Results

    if (includeId) {
      var column1=$('libraryQcTable').rows[1].insertCell(-1);
      column1.innerHTML="<input type='hidden' id='libraryId' name='libraryId' value='"+libraryId+"'/>";
    }
    var column2=$('libraryQcTable').rows[1].insertCell(-1);
    column2.innerHTML="<input id='libraryQcUser' name='libraryQcUser' type='hidden' value='"+$('currentUser').innerHTML+"'/>"+$('currentUser').innerHTML;
    var column3=$('libraryQcTable').rows[1].insertCell(-1);
    column3.innerHTML="<input id='libraryQcDate' name='libraryQcDate' type='text'/>";
    var column4=$('libraryQcTable').rows[1].insertCell(-1);
    column4.innerHTML="<select id='libraryQcType' name='libraryQcType' onchange='changeLibraryQcUnits(this);'/>";
    var column5=$('libraryQcTable').rows[1].insertCell(-1);
    column5.innerHTML="<input id='libraryQcResults' name='libraryQcResults' type='text'/><span id='units'/>";
    var column6=$('libraryQcTable').rows[1].insertCell(-1);
    column6.innerHTML="<input id='libraryQcInsertSize' name='libraryQcInsertSize' type='text'/> bp";
    var column7=$('libraryQcTable').rows[1].insertCell(-1);
    column7.innerHTML="<a href='javascript:void(0);' onclick='addLibraryQC(\"addQcForm\");'/>Add</a>";

    addMaxDatePicker("libraryQcDate", 0);

    Fluxion.doAjax(
            'libraryControllerHelperService',
            'getLibraryQcTypes',
    {'url':ajaxurl},
    {'doOnSuccess':function(json) {
        jQuery('#libraryQcType').html(json.types);
        jQuery('#units').html(jQuery('#libraryQcType option:first').attr("units"));
      }
    }
    );
  }
  else {
    alert("Cannot add another QC when one is already in progress.")
  }
}

function changeLibraryQcUnits(input) {
  jQuery('#units').html(jQuery('#libraryQcType').find(":selected").attr("units"));
}

function addLibraryQC(form) {
    var f = $(form);
    var tindex = f.libraryQcType.selectedIndex;
    Fluxion.doAjax(
            'libraryControllerHelperService',
            'addLibraryQC',
    {
        'libraryId':f.libraryId.value,
        'qcCreator':f.libraryQcUser.value,
        'qcDate':f.libraryQcDate.value,
        'qcType':f.libraryQcType.options[tindex].value,
        'results':f.libraryQcResults.value,
        'insertSize':f.libraryQcInsertSize.value,
        'url':ajaxurl},
    {'updateElement':'libraryQcTable',
     'doOnSuccess':function(json) {
       jQuery('#libraryQcTable').removeAttr("qcInProgress");
     }    
    }
    );
}

function changeLibraryQCRow(qcId, libraryId) {
  Fluxion.doAjax(
    'libraryControllerHelperService',
    'changeLibraryQCRow',
    {
      'libraryId':libraryId,
      'qcId':qcId,
      'url':ajaxurl
    },
    {'doOnSuccess':function(json) {
      jQuery('#result' + qcId).html(json.results);
      jQuery('#insert' + qcId).html(json.insertSize);
      jQuery('#edit' + qcId).html(json.edit);
    }
    }
  );
}

function editLibraryQC(qcId, libraryId) {
  Fluxion.doAjax(
    'libraryControllerHelperService',
    'editLibraryQC',
    {
      'libraryId':libraryId,
      'qcId':qcId,
      'result':jQuery('#results' + qcId).val(),
      'insertSize':jQuery('#insertSize' + qcId).val(),
      'url':ajaxurl
    },
    {'doOnSuccess':pageReload
    }
  );
}

function insertLibraryDilutionRow(libraryId) {
  if (!jQuery('#libraryDilutionTable').attr("dilutionInProgress")) {
    jQuery('#libraryDilutionTable').attr("dilutionInProgress", "true");

    $('libraryDilutionTable').insertRow(1);
    //dilutionId    Done By   Dilution Date Barcode Results

  //
    //var column1=$('libraryDilutionTable').rows[1].insertCell(-1);
    //column1.innerHTML="<input type='hidden' id='libraryId' name='libraryId' value='"+libraryId+"'/>";
    var column1=$('libraryDilutionTable').rows[1].insertCell(-1);
    column1.innerHTML="<input id='name' name='name' type='hidden' value='Unsaved '/>Unsaved";
    var column2=$('libraryDilutionTable').rows[1].insertCell(-1);
    column2.innerHTML="<input id='libraryDilutionCreator' name='libraryDilutionCreator' type='hidden' value='"+$('currentUser').innerHTML+"'/>"+$('currentUser').innerHTML;
    var column3=$('libraryDilutionTable').rows[1].insertCell(-1);
    column3.innerHTML="<input id='libraryDilutionDate' name='libraryDilutionDate' type='text'/>";
    //var column5=$('libraryDilutionTable').rows[1].insertCell(-1);
    //column5.innerHTML="<input id='libraryDilutionBarcode' name='libraryDilutionBarcode' type='text'/>";
    var column6=$('libraryDilutionTable').rows[1].insertCell(-1);
    column6.innerHTML="<input id='libraryDilutionResults' name='libraryDilutionResults' type='text'/>";
    var column7=$('libraryDilutionTable').rows[1].insertCell(-1);
    column7.innerHTML="<i>Generated on save</i>";
    var column8=$('libraryDilutionTable').rows[1].insertCell(-1);
    column8.innerHTML="<a href='javascript:void(0);' onclick='addLibraryDilution(\"addDilutionForm\");'/>Add</a>";

    addMaxDatePicker("libraryDilutionDate", 0);
  }
  else {
    alert("Cannot add another dilution when one is already in progress.")
  }
}

function addLibraryDilution(form) {
  var f = $(form);
  Fluxion.doAjax(
    'libraryControllerHelperService',
    'addLibraryDilution',
  {
    'libraryId':f.libraryId.value,
    'dilutionCreator':f.libraryDilutionCreator.value,
    'dilutionDate':f.libraryDilutionDate.value,
    //'locationBarcode':f.libraryDilutionBarcode.value,
    'results':f.libraryDilutionResults.value,
    'url':ajaxurl},
  {'updateElement':'libraryDilutionTable',
   'doOnSuccess':function(json) {
     jQuery('#libraryDilutionTable').removeAttr("dilutionInProgress");
   }
  }
  );
}

function changeLibraryDilutionRow(dilutionId) {
    Fluxion.doAjax(
            'libraryControllerHelperService',
            'changeLibraryDilutionRow',
            {
              'dilutionId':dilutionId,
              'url':ajaxurl
            },
            {'doOnSuccess':function(json) {
              jQuery('#results' + dilutionId).html(json.results);
              jQuery('#edit' + dilutionId).html(json.edit);
            }
            }
    );
}

function editLibraryDilution(dilutionId) {
  Fluxion.doAjax(
          'libraryControllerHelperService',
          'editLibraryDilution',
          {
            'dilutionId':dilutionId,
            'result':jQuery('#' + dilutionId).val(),
            'url':ajaxurl
          },
          {'doOnSuccess':pageReload
          }
  );
}

function insertEmPcrRow(dilutionId) {
  if (!jQuery('#emPcrTable').attr("pcrInProgress")) {
    jQuery('#emPcrTable').attr("pcrInProgress", "true");

    $('emPcrTable').insertRow(1);

    //var column1=$('emPcrTable').rows[1].insertCell(-1);
    //column1.innerHTML="<input type='hidden' id='dilutionId' name='dilutionId' value='"+dilutionId+"'/>";
    var column2=$('emPcrTable').rows[1].insertCell(-1);
    column2.innerHTML=""+dilutionId+" <input type='hidden' id='dilutionId' name='dilutionId' value='"+dilutionId+"'/>";  
    var column3=$('emPcrTable').rows[1].insertCell(-1);
    column3.innerHTML="<input id='emPcrCreator' name='emPcrCreator' type='hidden' value='"+$('currentUser').innerHTML+"'/>"+$('currentUser').innerHTML;
    var column4=$('emPcrTable').rows[1].insertCell(-1);
    column4.innerHTML="<input id='emPcrDate' name='emPcrDate' type='text'/>";
    var column5=$('emPcrTable').rows[1].insertCell(-1);
    column5.innerHTML="<input id='emPcrResults' name='emPcrResults' type='text'/>";
    var column6=$('emPcrTable').rows[1].insertCell(-1);
    column6.innerHTML="<a href='javascript:void(0);' onclick='addEmPcr(\"addEmPcrForm\");'/>Add</a>";

    addMaxDatePicker("emPcrDate", 0);
  }
  else {
    alert("Cannot add another emPCR when one is already in progress.")
  }
}

function addEmPcr(form) {
  var f = $(form);
  Fluxion.doAjax(
    'libraryControllerHelperService',
    'addEmPcr',
  {
    'dilutionId':f.dilutionId.value,
    'pcrCreator':f.emPcrCreator.value,
    'pcrDate':f.emPcrDate.value,
    'results':f.emPcrResults.value,
    'url':ajaxurl},
  {'updateElement':'emPcrTable',
   'doOnSuccess':function(json) {
     jQuery('#emPcrTable').removeAttr("pcrInProgress");
   }
  }
  );
}

function insertEmPcrDilutionRow(emPcrId) {
  if (!jQuery('#emPcrDilutionTable').attr("dilutionInProgress")) {
    jQuery('#emPcrDilutionTable').attr("dilutionInProgress", "true");

    $('emPcrDilutionTable').insertRow(1);

    //var column1=$('emPcrDilutionTable').rows[1].insertCell(-1);
    //column1.innerHTML="<input type='hidden' id='emPcrId' name='emPcrId' value='"+emPcrId+"'/>";
    var column2=$('emPcrDilutionTable').rows[1].insertCell(-1);
    column2.innerHTML=""+emPcrId+" <input type='hidden' id='emPcrId' name='emPcrId' value='"+emPcrId+"'/>";
    var column3=$('emPcrDilutionTable').rows[1].insertCell(-1);
    column3.innerHTML="<input id='emPcrDilutionCreator' name='emPcrDilutionCreator' type='hidden' value='"+$('currentUser').innerHTML+"'/>"+$('currentUser').innerHTML;
    var column4=$('emPcrDilutionTable').rows[1].insertCell(-1);
    column4.innerHTML="<input id='emPcrDilutionDate' name='emPcrDilutionDate' type='text'/>";
    //var column5=$('emPcrDilutionTable').rows[1].insertCell(-1);
    //column5.innerHTML="<input id='emPcrDilutionBarcode' name='emPcrDilutionBarcode' type='text'/>";
    var column6=$('emPcrDilutionTable').rows[1].insertCell(-1);
    column6.innerHTML="<input id='emPcrDilutionResults' name='emPcrDilutionResults' type='text'/>";
    var column7=$('emPcrDilutionTable').rows[1].insertCell(-1);
    column7.innerHTML="<a href='javascript:void(0);' onclick='addEmPcrDilution(\"addEmPcrDilutionForm\");'/>Add</a>";

    addMaxDatePicker("emPcrDilutionDate", 0);
  }
  else {
    alert("Cannot add another dilution when one is already in progress.")
  }
}

function addEmPcrDilution(form) {
  var f = $(form);
  Fluxion.doAjax(
    'libraryControllerHelperService',
    'addEmPcrDilution',
  {
    'pcrId':f.emPcrId.value,
    'pcrDilutionCreator':f.emPcrDilutionCreator.value,
    'pcrDilutionDate':f.emPcrDilutionDate.value,
    //'pcrDilutionBarcode':f.emPcrDilutionBarcode.value,
    'results':f.emPcrDilutionResults.value,
    'url':ajaxurl},
  {'updateElement':'emPcrDilutionTable',
   'doOnSuccess':function(json) {
     jQuery('#emPcrDilutionTable').removeAttr("dilutionInProgress");
   }
  }
  );
}

function printLibraryBarcodes() {
  var libraries = [];
  for (var i = 0; i < arguments.length; i++) {
    libraries[i] = {'libraryId':arguments[i]};
  }

  Fluxion.doAjax(
    'printerControllerHelperService',
    'listAvailableServices',
    {
      'serviceClass':'uk.ac.bbsrc.tgac.miso.core.data.Library',
      'url':ajaxurl
    },
    {
      'doOnSuccess':function (json) {
        jQuery('#printServiceSelectDialog')
                .html("<form>" +
                      "<fieldset class='dialog'>" +
                      "<select name='serviceSelect' id='serviceSelect' class='ui-widget-content ui-corner-all'>" +
                      json.services +
                      "</select></fieldset></form>");

        jQuery(function() {
          jQuery('#printServiceSelectDialog').dialog({
            autoOpen: false,
            width: 400,
            modal: true,
            resizable: false,
            buttons: {
              "Print": function() {
                Fluxion.doAjax(
                  'libraryControllerHelperService',
                  'printLibraryBarcodes',
                  {
                    'serviceName':jQuery('#serviceSelect').val(),
                    'libraries':libraries,
                    'url':ajaxurl
                  },
                  {
                    'doOnSuccess':function (json) {
                      alert(json.response);
                    }
                  }
                );
                jQuery(this).dialog('close');
              },
              "Cancel": function() {
                jQuery(this).dialog('close');
              }
            }
          });
        });
        jQuery('#printServiceSelectDialog').dialog('open');
      },
      'doOnError':function (json) { alert(json.error); }
    }
  );
}

function printDilutionBarcode(dilutionId, platform) {
  var dilutions = [];
  dilutions[0] = {'dilutionId':dilutionId};

  Fluxion.doAjax(
    'printerControllerHelperService',
    'listAvailableServices',
    {
      'serviceClass':'uk.ac.bbsrc.tgac.miso.core.data.Dilution',
      'url':ajaxurl
    },
    {
      'doOnSuccess':function (json) {
        jQuery('#printServiceSelectDialog')
                .html("<form>" +
                      "<fieldset class='dialog'>" +
                      "<select name='serviceSelect' id='serviceSelect' class='ui-widget-content ui-corner-all'>" +
                      json.services +
                      "</select></fieldset></form>");

        jQuery(function() {
          jQuery('#printServiceSelectDialog').dialog({
            autoOpen: false,
            width: 400,
            modal: true,
            resizable: false,
            buttons: {
              "Print": function() {
                Fluxion.doAjax(
                  'libraryControllerHelperService',
                  'printLibraryDilutionBarcodes',
                  {
                    'serviceName':jQuery('#serviceSelect').val(),
                    'dilutions':dilutions,
                    'platform':platform,
                    'url':ajaxurl
                  },
                  {
                    'doOnSuccess':function (json) {
                      alert(json.response);
                    }
                  }
                );
                jQuery(this).dialog('close');
              },
              "Cancel": function() {
                jQuery(this).dialog('close');
              }
            }
          });
        });
        jQuery('#printServiceSelectDialog').dialog('open');
      },
      'doOnError':function (json) { alert(json.error); }
    }
  );
}

function showLibraryNoteDialog(libraryId) {
  jQuery('#addLibraryNoteDialog')
          .html("<form>" +
                "<fieldset class='dialog'>" +
                "<label for='internalOnly'>Internal Only?</label>" +
                "<input type='checkbox' checked='checked' name='internalOnly' id='internalOnly' class='text ui-widget-content ui-corner-all' />" +
                "<br/>" +
                "<label for='notetext'>Text</label>" +
                "<input type='text' name='notetext' id='notetext' class='text ui-widget-content ui-corner-all' />" +
                "</fieldset></form>");

  jQuery(function() {
    jQuery('#addLibraryNoteDialog').dialog({
      autoOpen: false,
      width: 400,
      modal: true,
      resizable: false,
      buttons: {
        "Add Note": function() {
          addLibraryNote(libraryId, jQuery('#internalOnly').val(), jQuery('#notetext').val());
          jQuery(this).dialog('close');
        },
        "Cancel": function() {
          jQuery(this).dialog('close');
        }
      }
    });
  });
  jQuery('#addLibraryNoteDialog').dialog('open');
}

var addLibraryNote = function(libraryId, internalOnly, text) {
  Fluxion.doAjax(
    'libraryControllerHelperService',
    'addLibraryNote',
    {'libraryId':libraryId, 'internalOnly':internalOnly, 'text':text, 'url':ajaxurl},
    {'doOnSuccess':pageReload}
  );
};

function showLibraryLocationChangeDialog(libraryId) {
  jQuery('#changeLibraryLocationDialog')
          .html("<form>" +
                "<fieldset class='dialog'>" +
                "<label for='notetext'>New Location:</label>" +
                "<input type='text' name='locationBarcodeInput' id='locationBarcodeInput' class='text ui-widget-content ui-corner-all'/>" +
                "</fieldset></form>");

  jQuery(function() {
    jQuery('#changeLibraryLocationDialog').dialog({
      autoOpen: false,
      width: 400,
      modal: true,
      resizable: false,
      buttons: {
        "Save": function() {
          changeLibraryLocation(libraryId, jQuery('#locationBarcodeInput').val());
          jQuery(this).dialog('close');
        },
        "Cancel": function() {
          jQuery(this).dialog('close');
        }
      }
    });
  });
  jQuery('#changeLibraryLocationDialog').dialog('open');
}

var changeLibraryLocation = function(libraryId, barcode) {
  Fluxion.doAjax(
    'libraryControllerHelperService',
    'changeLibraryLocation',
    {'libraryId':libraryId, 'locationBarcode':barcode, 'url':ajaxurl},
    {'doOnSuccess':pageReload}
  );
};

function deleteLibrary(libraryId, successfunc) {
  if (confirm("Are you sure you really want to delete LIB"+libraryId+"? This operation is permanent!")) {
    Fluxion.doAjax(
      'libraryControllerHelperService',
      'deleteLibrary',
      {'libraryId':libraryId, 'url':ajaxurl},
      {'doOnSuccess':function(json) {
        successfunc();
      }
    });
  }
}

function deleteLibraryDilution(libraryDilutionId, successfunc) {
  if (confirm("Are you sure you really want to delete LDI"+libraryDilutionId+"? This operation is permanent!")) {
    Fluxion.doAjax(
      'libraryControllerHelperService',
      'deleteLibraryDilution',
      {'libraryDilutionId':libraryDilutionId, 'url':ajaxurl},
      {'doOnSuccess':function(json) {
        successfunc();
      }
    });
  }
}

function deleteEmPCR(empcrId, successfunc) {
  if (confirm("Are you sure you really want to delete EmPCR "+empcrId+"? This operation is permanent!")) {
    Fluxion.doAjax(
      'libraryControllerHelperService',
      'deleteEmPCR',
      {'empcrId':empcrId, 'url':ajaxurl},
      {'doOnSuccess':function(json) {
        successfunc();
      }
    });
  }
}

function deleteEmPCRDilution(empcrDilutionId, successfunc) {
  if (confirm("Are you sure you really want to delete EmPCRDilution"+empcrDilutionId+"? This operation is permanent!")) {
    Fluxion.doAjax(
      'libraryControllerHelperService',
      'deleteEmPCRDilution',
      {'empcrDilutionId':empcrDilutionId, 'url':ajaxurl},
      {'doOnSuccess':function(json) {
        successfunc();
      }
    });
  }
}