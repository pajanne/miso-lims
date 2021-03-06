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

import com.eaglegenomics.simlims.core.manager.SecurityManager;
import com.eaglegenomics.simlims.core.User;
import org.springframework.security.core.context.SecurityContextHolder;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sourceforge.fluxion.ajax.Ajaxified;
import net.sourceforge.fluxion.ajax.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.bbsrc.tgac.miso.core.data.*;
import uk.ac.bbsrc.tgac.miso.core.data.impl.LibraryDilution;
import uk.ac.bbsrc.tgac.miso.core.data.impl.PoolImpl;
import uk.ac.bbsrc.tgac.miso.core.data.impl.StudyImpl;
import uk.ac.bbsrc.tgac.miso.core.data.type.PlatformType;
import uk.ac.bbsrc.tgac.miso.core.exception.MalformedDilutionException;
import uk.ac.bbsrc.tgac.miso.core.factory.DataObjectFactory;
import uk.ac.bbsrc.tgac.miso.core.manager.RequestManager;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: bianx
 * Date: 18-Aug-2011
 * Time: 16:44:32
 * To change this template use File | Settings | File Templates.
 */
@Ajaxified
public class PoolWizardControllerHelperService {
  protected static final Logger log = LoggerFactory.getLogger(PoolControllerHelperService.class);
  @Autowired
  private SecurityManager securityManager;
  @Autowired
  private RequestManager requestManager;
  @Autowired
  private DataObjectFactory dataObjectFactory;

  public JSONObject addPool(HttpSession session, JSONObject json) {
    JSONObject response = new JSONObject();
    String alias = json.getString("alias");
    Double concentration = json.getDouble("concentration");
    PlatformType platformType = PlatformType.get(json.getString("platformType"));

    StringBuilder sb = new StringBuilder();
    List<Long> ids = new ArrayList<Long>();
    JSONArray a = JSONArray.fromObject(json.get("dilutions"));
    for (JSONObject j : (Iterable<JSONObject>) a) {
      ids.add(j.getLong("dilutionId"));
    }

    if (ids.size() > 0 && platformType != null && concentration != null) {
      try {
        List<Dilution> dils = new ArrayList<Dilution>();
        for (Long id : ids) {
          dils.add(requestManager.getDilutionByIdAndPlatform(id, platformType));
        }

        boolean barcodeCollision = false;
        if (dils.size() > 1) {
          for (Dilution d1 : dils) {
            if (d1 != null) {
              for (Dilution d2 : dils) {
                if (d2 != null && !d1.equals(d2)) {
                  if (d1.getLibrary().getTagBarcode() != null && d2.getLibrary().getTagBarcode() != null) {
                    if (d1.getLibrary().getTagBarcode().equals(d2.getLibrary().getTagBarcode())) {
                      barcodeCollision = true;
                    }
                  }
                }
              }
            }
          }
        }

        if (!barcodeCollision) {
	      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
          Pool pool;
          //TODO special type of pool for LibraryDilutions that will go on to be emPCRed as a whole
          if (dils.get(0) instanceof LibraryDilution &&
              (platformType.equals(PlatformType.SOLID) || platformType.equals(PlatformType.LS454))) {
            pool = dataObjectFactory.getEmPCRPool(platformType, user);
          }
          else {
            pool = dataObjectFactory.getPoolOfType(platformType, user);
          }

          if (alias != null) {
            pool.setAlias(alias);
          }

          pool.setCreationDate(new Date());
          pool.setConcentration(concentration);
          pool.setPlatformType(platformType);
          pool.setReadyToRun(true);

          for (Dilution d : dils) {
            try {
              pool.addPoolableElement(d);
            }
            catch (MalformedDilutionException dle) {
              log.debug("Failed", dle);
              return JSONUtils.SimpleJSONError("Failed: " + dle.getMessage());
            }
          }

          requestManager.savePool(pool);

          sb.append("<a  class='dashboardresult' href='/miso/pool/"+pool.getPlatformType().getKey().toLowerCase()+"/" + pool.getPoolId() + "' target='_blank'><div  onmouseover=\"this.className='dashboardhighlight ui-corner-all'\" onmouseout=\"this.className='dashboard ui-corner-all'\"  class='dashboard ui-corner-all' >");
          sb.append("Pool ID: <b>" + pool.getPoolId() + "</b><br/>");
          sb.append("Pool Name: <b>" + pool.getName() + "</b><br/>");
          sb.append("Platform Type: <b>" + pool.getPlatformType().name() + "</b><br/>");
          sb.append("Dilutions: <ul class='bullets'>");
          for (Dilution dl : (Collection<? extends Dilution>) pool.getDilutions()) {
            sb.append("<li>" + dl.getName() + " (<a href='/miso/library/"+dl.getLibrary().getLibraryId()+"'>" + dl.getLibrary().getAlias() + "</a>)</li>");
          }
          sb.append("</ul></div></a>");
        }
        else {
          throw new IOException("Tag barcode collision. Two or more selection dilutions have the same tag barcode and therefore cannot be pooled together.");          
        }
      }
      catch (IOException e) {
        log.debug("Failed", e);
        return JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
      }
    }
    else {
      sb.append("<br/>No dilution available to save.");
    }
    
    response.put("html", sb.toString());
    return response;
  }

  public JSONObject addStudy(HttpSession session, JSONObject json) {
    String studyType = null;
    Long projectId = json.getLong("projectId");
    String studyDescription = null;

    StringBuilder sb = new StringBuilder();

    JSONArray a = JSONArray.fromObject(json.get("form"));
    for (JSONObject j : (Iterable<JSONObject>) a) {

      if (j.getString("name").equals("studyDescription")) {
        studyDescription = j.getString("value");
      }
      else if (j.getString("name").equals("studyType")) {
        studyType = j.getString("value");
      }
    }
    try {
      Project p = requestManager.getProjectById(projectId);
      Study s = new StudyImpl();
      s.setProject(p);
      s.setAlias(p.getAlias());
      s.setDescription(studyDescription);
      s.setSecurityProfile(p.getSecurityProfile());
      s.setStudyType(studyType);

      requestManager.saveStudy(s);

      sb.append("<a  class=\"dashboardresult\" href='/miso/study/" + s.getStudyId() + "' target='_blank'><div onmouseover=\"this.className='dashboardhighlight ui-corner-all'\" onmouseout=\"this.className='dashboard ui-corner-all'\"  class='dashboard ui-corner-all' >New Study Added:<br/>");
      sb.append("Study ID: " + s.getStudyId() + "<br/>");
      sb.append("Study Name: <b>" + s.getName() + "</b><br/>");
      sb.append("Study Alias: <b>" + s.getAlias() + "</b><br/>");
      sb.append("Study Description: <b>" + s.getDescription() + "</b></div></a><br/><hr/><br/>");
    }
    catch (IOException e) {
      log.debug("Failed", e);
      return JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
    return JSONUtils.JSONObjectResponse("html", sb.toString());
  }

  public JSONObject populateDilutions(HttpSession session, JSONObject json) {
    Long projectId = json.getLong("projectId");
    PlatformType platformType = PlatformType.get(json.getString("platformType"));
    try {
      StringBuilder b = new StringBuilder();

      JSONArray a = new JSONArray();
      List<Dilution> dls = new ArrayList<Dilution>(requestManager.listAllDilutionsByProjectAndPlatform(projectId, platformType));
      Collections.sort(dls);
      for (Dilution dl : dls) {
        if (dl.getLibrary().getQcPassed()) {
          //b.append("<tr id='"+dl.getDilutionId()+"'><td class='rowSelect'><input class='chkbox' type='checkbox' name='ids' value='" + dl.getDilutionId() + "'/></td>");
          String barcode = "";
          if (dl.getLibrary().getTagBarcode() != null) barcode = dl.getLibrary().getTagBarcode().getName();

          b.append("<tr id='"+dl.getDilutionId()+"'><td class='rowSelect'></td>");
          b.append("<td>" + dl.getName() +"</td>");
          b.append("<td>");
          b.append(barcode);
          b.append("</td>");
          b.append("</tr>");

          a.add(JSONObject.fromObject("{'id':"+dl.getDilutionId()+",'name':'"+dl.getName()+"','description':'"+dl.getLibrary().getDescription()+"','library':'"+dl.getLibrary().getAlias()+"','libraryBarcode':'"+barcode+"'}"));
        }
      }

      JSONObject j = new JSONObject();
      j.put("dilutions", a);
      return JSONUtils.JSONObjectResponse(j);

    }
    catch (IOException e) {
      log.debug("Failed", e);
      return JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
  }

  public void setSecurityManager(com.eaglegenomics.simlims.core.manager.SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  public void setRequestManager(uk.ac.bbsrc.tgac.miso.core.manager.RequestManager requestManager) {
    this.requestManager = requestManager;
  }

  public void setDataObjectFactory(DataObjectFactory dataObjectFactory) {
    this.dataObjectFactory = dataObjectFactory;
  }
}
