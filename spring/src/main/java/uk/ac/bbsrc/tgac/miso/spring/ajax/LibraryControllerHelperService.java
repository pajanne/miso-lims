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

package uk.ac.bbsrc.tgac.miso.spring.ajax;

import com.eaglegenomics.simlims.core.Note;
import com.eaglegenomics.simlims.core.SecurityProfile;
import com.eaglegenomics.simlims.core.User;
import com.eaglegenomics.simlims.core.manager.SecurityManager;
import com.opensymphony.util.FileUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sourceforge.fluxion.ajax.Ajaxified;
import net.sourceforge.fluxion.ajax.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.ac.bbsrc.tgac.miso.core.data.*;
import uk.ac.bbsrc.tgac.miso.core.data.impl.LibraryDilution;
import uk.ac.bbsrc.tgac.miso.core.data.impl.LibraryImpl;
import uk.ac.bbsrc.tgac.miso.core.data.impl.emPCR;
import uk.ac.bbsrc.tgac.miso.core.data.impl.emPCRDilution;
import uk.ac.bbsrc.tgac.miso.core.data.type.LibraryType;
import uk.ac.bbsrc.tgac.miso.core.data.type.PlatformType;
import uk.ac.bbsrc.tgac.miso.core.data.type.QcType;
import uk.ac.bbsrc.tgac.miso.core.exception.MalformedSampleQcException;
import uk.ac.bbsrc.tgac.miso.core.exception.MisoPrintException;
import uk.ac.bbsrc.tgac.miso.core.factory.DataObjectFactory;
import uk.ac.bbsrc.tgac.miso.core.factory.barcode.BarcodeFactory;
import uk.ac.bbsrc.tgac.miso.core.factory.barcode.MisoJscriptFactory;
import uk.ac.bbsrc.tgac.miso.core.manager.FilesManager;
import uk.ac.bbsrc.tgac.miso.core.manager.MisoFilesManager;
import uk.ac.bbsrc.tgac.miso.core.manager.PrintManager;
import uk.ac.bbsrc.tgac.miso.core.manager.RequestManager;
import uk.ac.bbsrc.tgac.miso.core.service.printing.MisoPrintService;
import uk.ac.bbsrc.tgac.miso.core.service.printing.context.PrintContext;
import uk.ac.bbsrc.tgac.miso.core.util.AliasComparator;
import uk.ac.bbsrc.tgac.miso.core.util.LimsUtils;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpSession;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * uk.ac.bbsrc.tgac.miso.spring.ajax
 * <p/>
 * Info
 *
 * @author Rob Davey
 * @since 0.0.2
 */
@Ajaxified
public class LibraryControllerHelperService {
  protected static final Logger log = LoggerFactory.getLogger(LibraryControllerHelperService.class);
  @Autowired
  private SecurityManager securityManager;
  @Autowired
  private RequestManager requestManager;
  @Autowired
  private DataObjectFactory dataObjectFactory;
  @Autowired
  private BarcodeFactory barcodeFactory;
  @Autowired
  private MisoFilesManager misoFileManager;
  @Autowired
  private PrintManager<MisoPrintService, Queue<?>> printManager;

  public JSONObject addLibraryNote(HttpSession session, JSONObject json) {
    Long libraryId = json.getLong("libraryId");
    String internalOnly = json.getString("internalOnly");
    String text = json.getString("text");

    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      Library library = requestManager.getLibraryById(libraryId);
      Note note = new Note();
      internalOnly = internalOnly.equals("on") ? "true" : "false";
      note.setInternalOnly(Boolean.parseBoolean(internalOnly));
      note.setText(text);
      note.setOwner(user);
      note.setCreationDate(new Date());
      library.getNotes().add(note);
      requestManager.saveLibraryNote(library, note);
      requestManager.saveLibrary(library);
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError(e.getMessage());
    }

    return JSONUtils.SimpleJSONResponse("Note saved successfully");
  }

  public JSONObject getLibraryBarcode(HttpSession session, JSONObject json) {
    Long libraryId = json.getLong("libraryId");
    File temploc = new File(session.getServletContext().getRealPath("/")+"temp/");
    try {
      Library library = requestManager.getLibraryById(libraryId);
      barcodeFactory.setPointPixels(1.5f);
      barcodeFactory.setBitmapResolution(600);
      RenderedImage bi = barcodeFactory.generateSquareDataMatrix(library, 400);
      if (bi != null) {
        File tempimage = misoFileManager.generateTemporaryFile("barcode-", ".png", temploc);
        if (ImageIO.write(bi, "png", tempimage)) {
          return JSONUtils.JSONObjectResponse("img", tempimage.getName());
        }
        return JSONUtils.SimpleJSONError("Writing temp image file failed.");
      }
      else {
        return JSONUtils.SimpleJSONError("Library has no parseable barcode");
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError(e.getMessage() + ": Cannot seem to generate temp file for barcode");
    }
  }

  public JSONObject printLibraryBarcodes(HttpSession session, JSONObject json) {
    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());

      String serviceName = null;
      if (json.has("serviceName")) { serviceName = json.getString("serviceName"); }

      MisoPrintService<File, PrintContext<File>> mps = null;
      if (serviceName == null) {
        Collection<MisoPrintService> services = printManager.listPrintServicesByBarcodeableClass(Library.class);
        if (services.size() == 1) {
          mps = services.iterator().next();
        }
        else {
          return JSONUtils.SimpleJSONError("No serviceName specified, but more than one available service able to print this barcode type.");
        }
      }
      else {
        mps = printManager.getPrintService(serviceName);
      }

      Queue<File> thingsToPrint = new LinkedList<File>();

      JSONArray ls = JSONArray.fromObject(json.getString("libraries"));
      for (JSONObject l : (Iterable<JSONObject>)ls) {
        try {
          Long libraryId = l.getLong("libraryId");
          Library library = requestManager.getLibraryById(libraryId);
          //autosave the barcode if none has been previously generated
          if (library.getIdentificationBarcode() == null || "".equals(library.getIdentificationBarcode())) {
            requestManager.saveLibrary(library);
          }

          File f = mps.getLabelFor(library);
          if (f!=null) thingsToPrint.add(f);
        }
        catch (IOException e) {
          e.printStackTrace();
          return JSONUtils.SimpleJSONError("Error printing barcodes: " + e.getMessage());
        }
      }
      PrintJob pj = printManager.print(thingsToPrint, mps.getName(), user);
      return JSONUtils.SimpleJSONResponse("Job "+pj.getJobId()+" : Barcodes printed.");
    }
    catch (MisoPrintException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    }
  }

  public JSONObject printLibraryDilutionBarcodes(HttpSession session, JSONObject json) {
    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());

      String serviceName = null;
      if (json.has("serviceName")) { serviceName = json.getString("serviceName"); }

      MisoPrintService<File, PrintContext<File>> mps = null;
      if (serviceName == null) {
        Collection<MisoPrintService> services = printManager.listPrintServicesByBarcodeableClass(LibraryDilution.class);
        if (services.size() == 1) {
          mps = services.iterator().next();
        }
        else {
          return JSONUtils.SimpleJSONError("No serviceName specified, but more than one available service able to print this barcode type.");
        }
      }
      else {
        mps = printManager.getPrintService(serviceName);
      }

      Queue<File> thingsToPrint = new LinkedList<File>();

      JSONArray ls = JSONArray.fromObject(json.getString("dilutions"));
      for (JSONObject l : (Iterable<JSONObject>)ls) {
        try {
          Long dilutionId = l.getLong("dilutionId");
          String platform = l.getString("platform");
          Dilution dilution = requestManager.getDilutionByIdAndPlatform(dilutionId, PlatformType.get(platform));
          //autosave the barcode if none has been previously generated
          if (dilution.getIdentificationBarcode() == null || "".equals(dilution.getIdentificationBarcode())) {
            requestManager.saveDilution(dilution);
          }
          File f = mps.getLabelFor(dilution);
          if (f!=null) thingsToPrint.add(f);
          thingsToPrint.add(f);
        }
        catch (IOException e) {
          e.printStackTrace();
          return JSONUtils.SimpleJSONError("Error printing barcodes: " + e.getMessage());
        }
      }
      PrintJob pj = printManager.print(thingsToPrint, mps.getName(), user);
      return JSONUtils.SimpleJSONResponse("Job "+pj.getJobId()+" : Barcodes printed.");
    }
    catch (MisoPrintException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError("Failed to print barcodes: " + e.getMessage());
    }
  }

  public JSONObject changeLibraryLocation(HttpSession session, JSONObject json) {
    Long libraryId = json.getLong("libraryId");
    String locationBarcode = json.getString("locationBarcode");

    try {
      String newLocation = LimsUtils.lookupLocation(locationBarcode);
      if (newLocation != null) {
        User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
        Library library = requestManager.getLibraryById(libraryId);
        String oldLocation = library.getLocationBarcode();
        library.setLocationBarcode(newLocation);

        Note note = new Note();
        note.setInternalOnly(true);
        note.setText("Location changed from " + oldLocation + " to " + newLocation + " by " + user.getLoginName() + " on " + new Date());
        note.setOwner(user);
        note.setCreationDate(new Date());
        library.getNotes().add(note);
        requestManager.saveLibraryNote(library, note);
        requestManager.saveLibrary(library);
      }
      else {
        return JSONUtils.SimpleJSONError("New location barcode not recognised");
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError(e.getMessage());
    }

    return JSONUtils.SimpleJSONResponse("Note saved successfully");
  }

  public JSONObject getLibraryDilutionBarcode(HttpSession session, JSONObject json) {
    Long dilutionId = json.getLong("dilutionId");
    File temploc = new File(session.getServletContext().getRealPath("/")+"temp/");
    try {
      LibraryDilution dil = requestManager.getLibraryDilutionById(dilutionId);
      barcodeFactory.setPointPixels(1.5f);
      barcodeFactory.setBitmapResolution(600);
      RenderedImage bi = barcodeFactory.generateSquareDataMatrix(dil, 400);
      if (bi != null) {
        File tempimage = misoFileManager.generateTemporaryFile("barcode-", ".png", temploc);
        if (ImageIO.write(bi, "png", tempimage)) {
          return JSONUtils.JSONObjectResponse("img", tempimage.getName());
        }
        return JSONUtils.SimpleJSONError("Writing temp image file failed.");
      }
      else {
        return JSONUtils.SimpleJSONError("Dilution has no parseable barcode");
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError(e.getMessage() + ": Cannot seem to generate temp file for barcode");
    }
  }

  public JSONObject bulkSaveLibraries(HttpSession session, JSONObject json) {
    if (json.has("libraries")) {
      try {
        Project p = requestManager.getProjectById(json.getLong("projectId"));

        //objectify the stringified JSONArray
        JSONArray a = JSONArray.fromObject(json.get("libraries"));
        Set<Library> saveSet = new HashSet<Library>();

        for (JSONObject j : (Iterable<JSONObject>) a) {
          try {
            SecurityProfile sp = null;
            Sample sample = null;
            String libAlias = null;
            String sampleAlias = j.getString("parentSample");
            String regex = "([A-z0-9]+)_S([A-z0-9]+)_(.*)";
            Pattern pat = Pattern.compile(regex);

            for (Sample s : p.getSamples()) {
              if (s.getAlias().equals(sampleAlias)) {
                sp = s.getSecurityProfile();
                sample = s;
                
                Matcher mat = pat.matcher(s.getAlias());
                if (mat.matches()) {
                  //convert the sample alias automatically to a library alias
                  int numLibs = requestManager.listAllLibrariesBySampleId(s.getSampleId()).size();
                  libAlias = mat.group(1) + "_" + "L" + mat.group(2) + "-"+(numLibs+1)+"_" + mat.group(3);
                }
              }
            }

            if (sample != null && libAlias != null) {
              String descr = j.getString("description");
              String platform = j.getString("platform");
              String type = j.getString("libraryType");
              String selectionType = j.getString("selectionType");
              String strategyType = j.getString("strategyType");
              String locationBarcode = j.getString("locationBarcode");

              Library library = new LibraryImpl();
              library.setSecurityProfile(sp);
              library.setSample(sample);
              library.setAlias(libAlias);
              library.setDescription(descr);
              library.setPlatformName(platform);
              library.setCreationDate(new Date());
              library.setLocationBarcode(locationBarcode);
              library.setQcPassed(false);
              library.setLibraryType(requestManager.getLibraryTypeByDescription(type));
              library.setLibrarySelectionType(requestManager.getLibrarySelectionTypeByName(selectionType));
              library.setLibraryStrategyType(requestManager.getLibraryStrategyTypeByName(strategyType));

              boolean paired = false;
              if (!"".equals(j.getString("paired"))) {
                paired = j.getBoolean("paired");
              }
              library.setPaired(paired);

              if (!"".equals(j.getString("tagBarcode"))) {
                int tagBarcode = j.getInt("tagBarcode");
                library.setTagBarcode(requestManager.getTagBarcodeById(tagBarcode));
              }

              saveSet.add(library);
            }
            else {
              throw new IOException("Could not process a selected Sample to generate Libraries. Please check that all selected samples' aliases conform to the <PI initials>_S<Sample Number>_<Species> naming convention.");
            }
          }
          catch (IOException e) {
            e.printStackTrace();
            return JSONUtils.SimpleJSONError("Cannot save Library generated from " + j.getString("parentSample") + ": " + e.getMessage());
          }
          catch (JSONException e) {
            e.printStackTrace();
            JSONUtils.SimpleJSONError("Cannot save Library. Something cannot be retrieved from the bulk input table: " + e.getMessage());
          }
        }

        /*
        Set<Library> complement = LimsUtils.relativeComplementByProperty(
                Library.class,
                "getAlias",
                saveSet,
                new HashSet(requestManager.listAllLibrariesByProjectId(json.getLong("projectId"))));
                        */
        List<Library> sortedList = new ArrayList<Library>(saveSet);
        Collections.sort(sortedList, new AliasComparator(Library.class));
        for (Library library : sortedList) {
          //log.info("Saving bulk Library: " + library.toString());
          requestManager.saveLibrary(library);
        }
        
        return JSONUtils.SimpleJSONResponse("All libraries saved successfully");
      }
      catch (Exception e) {
        e.printStackTrace();
        return JSONUtils.SimpleJSONError("Cannot retrieve parent project with ID " + json.getLong("projectId"));
      }
    }
    else {
      return JSONUtils.SimpleJSONError("No libraries specified");
    }
  }

  public JSONObject changePlatformName(HttpSession session, JSONObject json) {
    try {
      if (json.has("platform") && !json.get("platform").equals("")) {
        String platform = json.getString("platform");
        Map<String, Object> map = new HashMap<String, Object>();

        StringBuilder libsb = new StringBuilder();
        List<LibraryType> types = new ArrayList<LibraryType>(requestManager.listLibraryTypesByPlatform(platform));
        Collections.sort(types);
        for (LibraryType s : types) {
          libsb.append("<option value='" + s.getLibraryTypeId() + "'>"+s.getDescription()+"</option>");
        }

        StringBuilder tagsb = new StringBuilder();
        List<TagBarcode> barcodes = new ArrayList<TagBarcode>(requestManager.listAllTagBarcodesByPlatform(platform));
        Collections.sort(barcodes);
        tagsb.append("<option value=''>No Barcode</option>");
        for (TagBarcode tb : barcodes) {
          tagsb.append("<option value='" + tb.getTagBarcodeId() + "'>"+tb.getName()+" ("+tb.getSequence()+")</option>");
        }

        map.put("libraryTypes", libsb.toString());
        map.put("tagBarcodes", tagsb.toString());
        
        return JSONUtils.JSONObjectResponse(map);
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      log.debug("Failed to retrieve library types given platform type: ", e);
      return JSONUtils.SimpleJSONError("Failed to retrieve library types given platform type: " + e.getMessage());      
    }
    return JSONUtils.SimpleJSONError("Cannot resolve LibraryType from selected Platform");
  }

  public JSONObject getLibraryQcTypes(HttpSession session, JSONObject json) {
    try {
      StringBuilder sb = new StringBuilder();
      Collection<QcType> types = requestManager.listAllLibraryQcTypes();
      for (QcType s : types) {
        sb.append("<option units='"+s.getUnits()+"' value='" + s.getQcTypeId() + "'>"+s.getName()+"</option>");
      }
      Map<String, Object> map = new HashMap<String, Object>();
      map.put("types", sb.toString());
      return JSONUtils.JSONObjectResponse(map);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return JSONUtils.SimpleJSONError("Cannot list all Library QC Types");
  }

  public JSONObject addLibraryQC(HttpSession session, JSONObject json) {
    try {
      for (Object key : json.keySet()) {
        if (json.get(key) == null || json.get(key).equals("")) {
          String k = (String)key;
          return JSONUtils.SimpleJSONError("Please enter a value for '" +k+ "'");
        }
      }
      if (json.has("libraryId") && !json.get("libraryId").equals("")) {
        Long libraryId = Long.parseLong(json.getString("libraryId"));
        Library library = requestManager.getLibraryById(libraryId);
        LibraryQC newQc = dataObjectFactory.getLibraryQC();
        if (json.has("qcPassed") && json.getString("qcPassed").equals("true")) {
          library.setQcPassed(true);
        }
        newQc.setQcCreator(json.getString("qcCreator"));
        newQc.setQcDate(new SimpleDateFormat("dd/MM/yyyy").parse(json.getString("qcDate")));
        newQc.setQcType(requestManager.getLibraryQcTypeById(json.getLong("qcType")));
        newQc.setResults(Double.parseDouble(json.getString("results")));
        newQc.setInsertSize(Integer.parseInt(json.getString("insertSize")));
        library.addQc(newQc);
        requestManager.saveLibraryQC(newQc);

        StringBuilder sb = new StringBuilder();
        //sb.append("<tr><th>ID</th><th>QCed By</th><th>QC Date</th><th>Method</th><th>Results</th><th>Insert Size</th></tr>");
        sb.append("<tr><th>QCed By</th><th>QC Date</th><th>Method</th><th>Results</th><th>Insert Size</th></tr>");
        for (LibraryQC qc : library.getLibraryQCs()) {
          sb.append("<tr>");
          //sb.append("<td>"+qc.getQcId()+"</td>");
          sb.append("<td>"+qc.getQcCreator()+"</td>");
          sb.append("<td>"+qc.getQcDate()+"</td>");
          sb.append("<td>"+qc.getQcType().getName()+"</td>");
          sb.append("<td>"+qc.getResults()+" "+ qc.getQcType().getUnits() +"</td>");
          sb.append("<td>"+qc.getInsertSize()+" bp</td>");
          sb.append("</tr>");
        }
        return JSONUtils.SimpleJSONResponse(sb.toString());
      }
    }
    catch (Exception e) {
      log.debug("Failed to add Library QC to this Library: ", e);
      return JSONUtils.SimpleJSONError("Failed to add Library QC to this Library: " + e.getMessage());
    }
    return JSONUtils.SimpleJSONError("Cannot add LibraryQC");
  }

  public JSONObject bulkAddLibraryQCs(HttpSession session, JSONObject json) {
    try {
      JSONArray qcs = JSONArray.fromObject(json.getString("qcs"));
      //validate
      boolean ok = true;
      for (JSONObject qc : (Iterable<JSONObject>)qcs) {
        String qcPassed = qc.getString("qcPassed");
        String qcType = qc.getString("qcType");
        String results = qc.getString("results");
        String qcCreator = qc.getString("qcCreator");
        String qcDate = qc.getString("qcDate");
        String insertSize = qc.getString("insertSize");

        if (qcPassed == null || qcPassed.equals("") ||
            qcType == null || qcType.equals("") ||
            results == null || results.equals("") ||
            qcCreator == null || qcCreator.equals("") ||
            qcDate == null || qcDate.equals("") ||
            insertSize == null || insertSize.equals("")) {
          ok = false;
        }
      }

      //persist
      if (ok) {
        Map<String, Object> map = new HashMap<String, Object>();
        JSONArray a = new JSONArray();
        for (JSONObject qc : (Iterable<JSONObject>)qcs) {
          JSONObject j = addLibraryQC(session, qc);
          j.put("libraryId", qc.getString("libraryId"));
          a.add(j);
        }
        map.put("saved", a);
        return JSONUtils.JSONObjectResponse(map);
      }
      else {
        log.error("Failed to add Library QC to this Library: one of the required fields of the selected QCs is missing or invalid");
        return JSONUtils.SimpleJSONError("Failed to add Library QC to this Library: one of the required fields of the selected QCs is missing or invalid");
      }
    }
    catch (Exception e) {
      log.error("Failed to add Library QC to this Library: ", e);
      return JSONUtils.SimpleJSONError("Failed to add Library QC to this Library: " + e.getMessage());
    }
  }

  public JSONObject addLibraryDilution(HttpSession session, JSONObject json) {
    try {
      for (Object key : json.keySet()) {
        if (json.get(key) == null || json.get(key).equals("")) {
          String k = (String)key;
          return JSONUtils.SimpleJSONError("Please enter a value for '" +k+ "'");
        }
      }
      if (json.has("libraryId") && !json.get("libraryId").equals("")) {
        Long libraryId = Long.parseLong(json.getString("libraryId"));
        Library library = requestManager.getLibraryById(libraryId);
        LibraryDilution newDilution = dataObjectFactory.getLibraryDilution();
        newDilution.setSecurityProfile(library.getSecurityProfile());
        newDilution.setDilutionCreator(json.getString("dilutionCreator"));
        newDilution.setCreationDate(new SimpleDateFormat("dd/MM/yyyy").parse(json.getString("dilutionDate")));
        //newDilution.setLocationBarcode(json.getString("locationBarcode"));
        newDilution.setConcentration(Double.parseDouble(json.getString("results")));
        library.addDilution(newDilution);
        requestManager.saveLibraryDilution(newDilution);

        StringBuilder sb = new StringBuilder();
        sb.append("<tr>");
        //sb.append("<th>ID</th><th>Done By</th><th>Date</th><th>Barcode</th><th>Results</th>");
        sb.append("<th>LD Name</th><th>Done By</th><th>Date</th><th>Results</th><th>ID barcode</th>");
        if (!library.getPlatformName().equals("Illumina")) {
          sb.append("<th>Add emPCR</th>");
        }
        sb.append("</tr>");

        File temploc = new File(session.getServletContext().getRealPath("/")+"temp/");
        for (LibraryDilution dil : library.getLibraryDilutions()) {
          sb.append("<tr>");
          sb.append("<td>"+dil.getName()+"</td>");
          sb.append("<td>"+dil.getDilutionCreator()+"</td>");
          sb.append("<td>"+dil.getCreationDate()+"</td>");
          sb.append("<td>"+dil.getConcentration()+" "+ dil.getUnits()+"</td>");
          sb.append("<td>");

          try {
            barcodeFactory.setPointPixels(1.5f);
            barcodeFactory.setBitmapResolution(600);
            RenderedImage bi = barcodeFactory.generateSquareDataMatrix(dil, 400);
            if (bi != null) {
              File tempimage = misoFileManager.generateTemporaryFile("barcode-", ".png", temploc);
              if (ImageIO.write(bi, "png", tempimage)) {
                sb.append("<img style='border:0;' src='/temp/"+tempimage.getName()+"'/>");
              }
            }
          }
          catch (IOException e) {
            e.printStackTrace();
          }
          sb.append("</td>");

          if (!library.getPlatformName().equals("Illumina")) {
            sb.append("<td><a href='javascript:void(0);' onclick='insertEmPcrRow("+dil.getDilutionId()+");'>Add emPCR</a></td>");
          }
          else {
            sb.append("<td><a href='/miso/pool/"+library.getPlatformName().toLowerCase()+"/new/'>Construct New Pool</a></td>");
          }

          sb.append("</tr>");
        }
        return JSONUtils.SimpleJSONResponse(sb.toString());
      }
    }
    catch (Exception e) {
      log.debug("Failed to add Library Dilution to this Library: ", e);
      return JSONUtils.SimpleJSONError("Failed to add Library Dilution to this Library: " + e.getMessage());
    }
    return JSONUtils.SimpleJSONError("Cannot add LibraryDilution");
  }

  public JSONObject bulkAddLibraryDilutions(HttpSession session, JSONObject json) {
    try {
      JSONArray dilutions = JSONArray.fromObject(json.getString("dilutions"));
      //validate
      boolean ok = true;
      for (JSONObject dil : (Iterable<JSONObject>)dilutions) {
        String results = dil.getString("results");
        String dilutionCreator = dil.getString("dilutionCreator");
        String dilutionDate = dil.getString("dilutionDate");

        if (results == null || results.equals("") ||
            dilutionCreator == null || dilutionCreator.equals("") ||
            dilutionDate == null || dilutionDate.equals("")) {
          ok = false;
        }
      }

      //persist
      if (ok) {
        Map<String, Object> map = new HashMap<String, Object>();
        JSONArray a = new JSONArray();
        for (JSONObject dil : (Iterable<JSONObject>)dilutions) {
          JSONObject j = addLibraryDilution(session, dil);
          j.put("libraryId", dil.getString("libraryId"));
          a.add(j);
        }
        map.put("saved", a);
        return JSONUtils.JSONObjectResponse(map);
      }
      else {
        log.error("Failed to add Library Dilutions to this Library: one of the required fields of the selected Library Dilutions is missing or invalid");
        return JSONUtils.SimpleJSONError("Failed to add Library Dilutions to this Library: one of the required fields of the selected Library Dilutions is missing or invalid");
      }
    }
    catch (Exception e) {
      log.error("Failed to add Library Dilutions to this Library: ", e);
      return JSONUtils.SimpleJSONError("Failed to add Library Dilutions to this Library: " + e.getMessage());
    }
  }

  public JSONObject changeLibraryDilutionRow(HttpSession session, JSONObject json) {
    try {
      JSONObject response = new JSONObject();
      Long dilutionId = Long.parseLong(json.getString("dilutionId"));
      LibraryDilution dilution = requestManager.getLibraryDilutionById(dilutionId);
      response.put("results", "<input type='text' id='" + dilutionId + "' value='" + dilution.getConcentration() + "'/>");
      response.put("edit", "<a href='javascript:void(0);' onclick='editLibraryDilution(\"" + dilutionId + "\");'>Save</a>");
      return response;
    }
    catch (Exception e) {
      log.error("Failed to display Library Dilution of this Library: ", e);
      return JSONUtils.SimpleJSONError("Failed to display Library Dilution of this sample: " + e.getMessage());
    }
  }

  public JSONObject editLibraryDilution(HttpSession session, JSONObject json) {
    try {
      if (json.has("dilutionId") && !json.get("dilutionId").equals("")) {
        Long dilutionId = Long.parseLong(json.getString("dilutionId"));
        LibraryDilution dilution = requestManager.getLibraryDilutionById(dilutionId);
        dilution.setConcentration(Double.parseDouble(json.getString("result")));
        requestManager.saveLibraryDilution(dilution);
        return JSONUtils.SimpleJSONResponse("OK");
      }
    }
    catch (Exception e) {
      log.error("Failed to edit Library Dilution of this Library: ", e);
      return JSONUtils.SimpleJSONError("Failed to edit Library Dilution of this Library: " + e.getMessage());
    }
    return JSONUtils.SimpleJSONError("Cannot add LibraryDilution");
  }

  public JSONObject addEmPcr(HttpSession session, JSONObject json) {
    try {
      for (Object key : json.keySet()) {
        if (json.get(key) == null || json.get(key).equals("")) {
          String k = (String)key;
          return JSONUtils.SimpleJSONError("Please enter a value for '" +k+ "'");
        }
      }
      if (json.has("dilutionId") && !json.get("dilutionId").equals("")) {
        Long dilutionId = Long.parseLong(json.getString("dilutionId"));
        LibraryDilution dilution = requestManager.getLibraryDilutionById(dilutionId);
        emPCR pcr = dataObjectFactory.getEmPCR();
        pcr.setSecurityProfile(dilution.getSecurityProfile());
        pcr.setPcrCreator(json.getString("pcrCreator"));
        pcr.setCreationDate(new SimpleDateFormat("dd/MM/yyyy").parse(json.getString("pcrDate")));
        pcr.setConcentration(Double.parseDouble(json.getString("results")));
        pcr.setLibraryDilution(dilution);
        requestManager.saveEmPCR(pcr);

        StringBuilder sb = new StringBuilder();
        sb.append("<tr>");
        sb.append("<th>ID</th><th>Done By</th><th>Date</th><th>Results</th>");
        sb.append("<th>Add emPCR Dilution</th>");
        sb.append("</tr>");
        for (emPCR p : requestManager.listAllEmPCRsByDilutionId(dilutionId)) {
          sb.append("<tr>");
          sb.append("<td>"+p.getPcrId()+"</td>");
          sb.append("<td>"+p.getPcrCreator()+"</td>");
          sb.append("<td>"+p.getCreationDate()+"</td>");
          sb.append("<td>"+p.getConcentration()+" "+ p.getUnits()+"</td>");
          sb.append("<td><a href='javascript:void(0);' onclick='insertEmPcrDilutionRow("+p.getPcrId()+");'>Add emPCR Dilution</a></td>");
          sb.append("</tr>");
        }
        return JSONUtils.SimpleJSONResponse(sb.toString());
      }
      else {
        log.debug("Failed to add emPCR to this LibraryDilution: No parent Dilution ID found");
        return JSONUtils.SimpleJSONError("Failed to add emPCR to this LibraryDilution: No parent Dilution ID found");        
      }
    }
    catch (Exception e) {
      log.debug("Failed to add emPCR to this LibraryDilution: ", e);
      return JSONUtils.SimpleJSONError("Failed to add emPCR to this LibraryDilution: " + e.getMessage());
    }
  }

  public JSONObject addEmPcrDilution(HttpSession session, JSONObject json) {
    try {
      for (Object key : json.keySet()) {
        if (json.get(key) == null || json.get(key).equals("")) {
          String k = (String)key;
          return JSONUtils.SimpleJSONError("Please enter a value for '" +k+ "'");
        }
      }
      if (json.has("pcrId") && !json.get("pcrId").equals("")) {
        Long pcrId = Long.parseLong(json.getString("pcrId"));
        emPCR pcr = requestManager.getEmPcrById(pcrId);
        emPCRDilution newDilution = dataObjectFactory.getEmPCRDilution();
        newDilution.setSecurityProfile(pcr.getSecurityProfile());
        newDilution.setDilutionCreator(json.getString("pcrDilutionCreator"));
        newDilution.setCreationDate(new SimpleDateFormat("dd/MM/yyyy").parse(json.getString("pcrDilutionDate")));
        newDilution.setConcentration(Double.parseDouble(json.getString("results")));
        newDilution.setEmPCR(pcr);
        requestManager.saveEmPCRDilution(newDilution);

        StringBuilder sb = new StringBuilder();
        sb.append("<tr>");
        //sb.append("<th>ID</th><th>Done By</th><th>Date</th><th>Barcode</th><th>Results</th>");
        sb.append("<th>ID</th><th>Done By</th><th>Date</th><th>Results</th><th>ID Barcode</th>");
        sb.append("</tr>");
        
        File temploc = new File(session.getServletContext().getRealPath("/")+"temp/");
        for (emPCRDilution dil : requestManager.listAllEmPcrDilutionsByEmPcrId(pcrId)) {
          sb.append("<tr>");
          sb.append("<td>"+dil.getDilutionId()+"</td>");
          sb.append("<td>"+dil.getDilutionCreator()+"</td>");
          sb.append("<td>"+dil.getCreationDate()+"</td>");
          sb.append("<td>"+dil.getConcentration()+" "+ dil.getUnits()+"</td>");

          sb.append("<td>");
          try {
            barcodeFactory.setPointPixels(1.5f);
            barcodeFactory.setBitmapResolution(600);
            RenderedImage bi = barcodeFactory.generateSquareDataMatrix(dil, 400);
            if (bi != null) {
              File tempimage = misoFileManager.generateTemporaryFile("barcode-", ".png", temploc);
              if (ImageIO.write(bi, "png", tempimage)) {
                sb.append("<img style='border:0;' src='/temp/"+tempimage.getName()+"'/>");
              }
            }
          }
          catch (IOException e) {
            e.printStackTrace();
          }
          sb.append("</td>");

          sb.append("<td><a href='/miso/pool/"+pcr.getLibraryDilution().getLibrary().getPlatformName().toLowerCase()+"/new/'>Construct New Pool</a></td>");
          sb.append("</tr>");
        }
        return JSONUtils.SimpleJSONResponse(sb.toString());
      }
    }
    catch (Exception e) {
      log.debug("Failed to add EmPCRDilution to this EmPCR: ", e);
      return JSONUtils.SimpleJSONError("Failed to add EmPCRDilution to this EmPCR: " + e.getMessage());
    }
    return JSONUtils.SimpleJSONError("Cannot add EmPCRDilution");
  }

  public JSONObject bulkAddEmPcrs(HttpSession session, JSONObject json) {
    try {
      JSONArray pcrs = JSONArray.fromObject(json.getString("pcrs"));
      //validate
      boolean ok = true;
      for (JSONObject pcr : (Iterable<JSONObject>)pcrs) {
        String pcrCreator = pcr.getString("pcrCreator");
        String pcrDate = pcr.getString("pcrDate");
        String concentration = pcr.getString("results");

        if (concentration == null || concentration.equals("") ||
            pcrCreator == null || pcrCreator.equals("") ||
            pcrDate == null || pcrDate.equals("")) {
          ok = false;
        }
      }

      //persist
      if (ok) {
        Map<String, Object> map = new HashMap<String, Object>();
        JSONArray a = new JSONArray();
        for (JSONObject pcr : (Iterable<JSONObject>)pcrs) {
          JSONObject j = addEmPcr(session, pcr);
          j.put("dilutionId", pcr.getString("dilutionId"));
          a.add(j);
        }
        map.put("saved", a);
        return JSONUtils.JSONObjectResponse(map);
      }
      else {
        log.error("Failed to add EmPCRs to this Library Dilution: one of the required fields of the selected EmPCR is missing or invalid");
        return JSONUtils.SimpleJSONError("Failed to add EmPCRs to this Library Dilution: one of the required fields of the EmPCR is missing or invalid");
      }
    }
    catch (Exception e) {
      log.error("Failed to add EmPCRs to this Library Dilution: ", e);
      return JSONUtils.SimpleJSONError("Failed to add EmPCRs to this Library Dilution: " + e.getMessage());
    }
  }

  public JSONObject bulkAddEmPcrDilutions(HttpSession session, JSONObject json) {
    try {
      JSONArray dilutions = JSONArray.fromObject(json.getString("dilutions"));
      //validate
      boolean ok = true;
      for (JSONObject dil : (Iterable<JSONObject>)dilutions) {
        String dilutionCreator = dil.getString("pcrDilutionCreator");
        String dilutionDate = dil.getString("pcrDilutionDate");
        String concentration = dil.getString("results");

        if (concentration == null || concentration.equals("") ||
            dilutionCreator == null || dilutionCreator.equals("") ||
            dilutionDate == null || dilutionDate.equals("")) {
          ok = false;
        }
      }

      //persist
      if (ok) {
        log.info("EmPCR Dilutions OK, saving...");
        Map<String, Object> map = new HashMap<String, Object>();
        JSONArray a = new JSONArray();
        for (JSONObject dil : (Iterable<JSONObject>)dilutions) {
          JSONObject j = addEmPcrDilution(session, dil);
          j.put("pcrId", dil.getString("pcrId"));
          a.add(j);
        }
        map.put("saved", a);
        return JSONUtils.JSONObjectResponse(map);
      }
      else {
        log.error("Failed to add EmPCR Dilutions to this EmPCR: one of the required fields of the selected EmPCR Dilutions is missing or invalid");
        return JSONUtils.SimpleJSONError("Failed to add EmPCR Dilutions to this EmPCR: one of the required fields of the selected EmPCR Dilutions is missing or invalid");
      }
    }
    catch (Exception e) {
      log.error("Failed to add EmPCR Dilutions to this EmPCR: ", e);
      return JSONUtils.SimpleJSONError("Failed to add EmPCR Dilutions to this EmPCR: " + e.getMessage());
    }
  }


  public JSONObject changeLibraryQCRow(HttpSession session, JSONObject json) {
    try {
      JSONObject response = new JSONObject();
      Long qcId = Long.parseLong(json.getString("qcId"));
      LibraryQC libraryQc = requestManager.getLibraryQCById(qcId);
      Long libraryId = Long.parseLong(json.getString("libraryId"));

      response.put("results", "<input type='text' id='results" + qcId + "' value='" + libraryQc.getResults() + "'/>");
      response.put("insertSize", "<input type='text' id='insertSize" + qcId + "' value='" + libraryQc.getInsertSize() + "'/>");
      response.put("edit", "<a href='javascript:void(0);' onclick='editLibraryQC(\"" + qcId + "\",\"" + libraryId + "\");'>Save</a>");
      return response;
    }
    catch (Exception e) {
      log.error("Failed to display library QC of this library: ", e);
      return JSONUtils.SimpleJSONError("Failed to display library QC of this library: " + e.getMessage());
    }
  }

  public JSONObject editLibraryQC(HttpSession session, JSONObject json) {
    try {
      if (json.has("qcId") && !json.get("qcId").equals("")) {
        Long qcId = Long.parseLong(json.getString("qcId"));
        LibraryQC libraryQc = requestManager.getLibraryQCById(qcId);

        libraryQc.setResults(Double.parseDouble(json.getString("result")));
        libraryQc.setInsertSize(Integer.parseInt(json.getString("insertSize")));
        requestManager.saveLibraryQC(libraryQc);

        }
        return JSONUtils.SimpleJSONResponse("done");
      }
    catch (Exception e) {
      log.error("Failed to add library QC to this library: ", e);
      return JSONUtils.SimpleJSONError("Failed to add library QC to this library: " + e.getMessage());
    }
  }

  public JSONObject deleteLibrary(HttpSession session, JSONObject json) {
    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      if (user.isAdmin()) {
        if (json.has("libraryId")) {
          Long libraryId = json.getLong("libraryId");
          requestManager.deleteLibrary(requestManager.getLibraryById(libraryId));
          return JSONUtils.SimpleJSONResponse("Library deleted");
        }
        else {
          return JSONUtils.SimpleJSONError("No library specified to delete.");
        }
      }
      else {
        return JSONUtils.SimpleJSONError("Only admins can delete objects.");
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError("Error getting currently logged in user.");
    }
  }

  public JSONObject deleteLibraryDilution(HttpSession session, JSONObject json) {
    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      if (user.isAdmin()) {
        if (json.has("libraryDilutionId")) {
          Long libraryDilutionId = json.getLong("libraryDilutionId");
          requestManager.deleteDilution(requestManager.getLibraryDilutionById(libraryDilutionId));
          return JSONUtils.SimpleJSONResponse("LibraryDilution deleted");
        }
        else {
          return JSONUtils.SimpleJSONError("No lirbary dilution specified to delete.");
        }
      }
      else {
        return JSONUtils.SimpleJSONError("Only admins can delete objects.");
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError("Error getting currently logged in user.");
    }
  }

  public JSONObject deleteEmPCR(HttpSession session, JSONObject json) {
    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      if (user.isAdmin()) {
        if (json.has("empcrId")) {
          Long empcrId = json.getLong("empcrId");
          requestManager.deleteEmPCR(requestManager.getEmPcrById(empcrId));
          return JSONUtils.SimpleJSONResponse("EmPCR deleted");
        }
        else {
          return JSONUtils.SimpleJSONError("No EmPCR specified to delete.");
        }
      }
      else {
        return JSONUtils.SimpleJSONError("Only admins can delete objects.");
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError("Error getting currently logged in user.");
    }
  }

  public JSONObject deleteEmPCRDilution(HttpSession session, JSONObject json) {
    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      if (user.isAdmin()) {
        if (json.has("deleteEmPCRDilution")) {
          Long deleteEmPCRDilution = json.getLong("deleteEmPCRDilution");
          requestManager.deleteDilution(requestManager.getEmPcrDilutionById(deleteEmPCRDilution));
          return JSONUtils.SimpleJSONResponse("EmPCRDilution deleted");
        }
        else {
          return JSONUtils.SimpleJSONError("No EmPCR dilution specified to delete.");
        }
      }
      else {
        return JSONUtils.SimpleJSONError("Only admins can delete objects.");
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.SimpleJSONError("Error getting currently logged in user.");
    }
  }

  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  public void setRequestManager(RequestManager requestManager) {
    this.requestManager = requestManager;
  }

  public void setDataObjectFactory(DataObjectFactory dataObjectFactory) {
    this.dataObjectFactory = dataObjectFactory;
  }

  public void setBarcodeFactory(BarcodeFactory barcodeFactory) {
    this.barcodeFactory = barcodeFactory;
  }

  public void setMisoFileManager(MisoFilesManager misoFileManager) {
    this.misoFileManager = misoFileManager;
  }
  
  public void setPrintManager(PrintManager<MisoPrintService, Queue<?>> printManager) {
    this.printManager = printManager;
  }
}