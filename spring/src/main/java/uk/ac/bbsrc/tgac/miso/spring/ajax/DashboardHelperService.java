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

import uk.ac.bbsrc.tgac.miso.core.data.Project;
import com.eaglegenomics.simlims.core.User;
import com.eaglegenomics.simlims.core.manager.SecurityManager;
import net.sf.json.JSONObject;
import net.sourceforge.fluxion.ajax.Ajaxified;
import net.sourceforge.fluxion.ajax.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.InetOrgPerson;
import uk.ac.bbsrc.tgac.miso.core.data.*;
import uk.ac.bbsrc.tgac.miso.core.data.impl.UserImpl;
import uk.ac.bbsrc.tgac.miso.core.event.Alert;
import uk.ac.bbsrc.tgac.miso.core.event.type.AlertLevel;
import uk.ac.bbsrc.tgac.miso.core.manager.RequestManager;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * uk.ac.bbsrc.tgac.miso.miso.spring.ajax
 * <p/>
 * Info
 *
 * @author Xingdong Bian
 * @author Rob Davey
 * @since 0.0.2
 */
@Ajaxified
public class DashboardHelperService {
  protected static final Logger log = LoggerFactory.getLogger(DashboardHelperService.class);
  @Autowired
  private SecurityManager securityManager;
  @Autowired
  private RequestManager requestManager;

  public JSONObject checkUser(HttpSession session, JSONObject json) {
    String username = json.getString("username");
    if (username != null && !username.equals("")) {
      if (SecurityContextHolder.getContext().getAuthentication().getName().equals(username)) {
        try {
          User user = securityManager.getUserByLoginName(username);
          if (user == null) {
            //user is authed, but doesn't exist in the LIMS DB. Save that user!
            User u = new UserImpl();
            Object o = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (o instanceof UserDetails) {
              UserDetails details = (UserDetails) o;
              u.setLoginName(details.getUsername());
              u.setFullName(details.getUsername());
              u.setPassword(details.getPassword());
              u.setActive(true);

              if (details.getAuthorities().contains(new GrantedAuthorityImpl("ROLE_ADMIN"))) {
                u.setAdmin(true);
              }

              if (details.getAuthorities().contains(new GrantedAuthorityImpl("ROLE_INTERNAL"))) {
                u.setInternal(true);
                u.setRoles(new String[]{"ROLE_INTERNAL"});
              }
              else if (details.getAuthorities().contains(new GrantedAuthorityImpl("ROLE_EXTERNAL"))) {
                u.setExternal(true);
                u.setRoles(new String[]{"ROLE_EXTERNAL"});
              }
              else {
                log.warn("Unrecognised roles");
              }

              if (details instanceof InetOrgPerson) {
                u.setFullName(((InetOrgPerson) details).getDisplayName());
                u.setEmail(((InetOrgPerson) details).getMail());
              }

              securityManager.saveUser(u);
            }
            else {
              return JSONUtils.SimpleJSONError("The UserDetailsService specified in the Spring config cannot support mapping of usernames to UserDetails objects");
            }
          }
          else {
            //the user isn't null, but LDAP is "newer" than the LIMS, i.e. LDAP pass changed but LIMS still the same
            Object o = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (o instanceof UserDetails) {
              UserDetails details = (UserDetails) o;
              if (!user.getPassword().equals(details.getPassword())) {
                user.setPassword(details.getPassword());
                securityManager.saveUser(user);
              }
            }
          }
          return null;
        }
        catch (IOException e) {
          e.printStackTrace();
          return JSONUtils.SimpleJSONError("Something went wrong trying to get user information from the database: " + e.getMessage());
        }
      }
      else {
        return JSONUtils.SimpleJSONError("Cannot check LIMS user database table if you are not authenticated as that user!");
      }
    }
    return JSONUtils.SimpleJSONError("Please supply a valid username to check");
  }

  public JSONObject searchProject(HttpSession session, JSONObject json) {
    String searchStr = (String) json.get("str");
    try {
      List<Project> projects = new ArrayList<Project>();
      StringBuilder b = new StringBuilder();
      if (searchStr != null && !searchStr.equals("")) {
        projects = new ArrayList<Project>(requestManager.listAllProjectsBySearch(searchStr));
      }
      else {
        projects = new ArrayList<Project>(requestManager.listAllProjects());
      }

      if (projects.size() > 0) {
        Collections.sort(projects);
        for (Project p : projects) {
          b.append("<a class=\"dashboardresult\" href=\"/miso/project/" + p.getProjectId() + "\"><div  onMouseOver=\"this.className=&#39dashboardhighlight&#39\" onMouseOut=\"this.className=&#39dashboard&#39\" class=\"dashboard\">");
          b.append("Name: <b>" + p.getName() + "</b><br/>");
          b.append("Alias: <b>" + p.getAlias() + "</b><br/>");
          b.append("</div></a>");
        }
      }
      else {
        b.append("No matches");
      }
      return JSONUtils.JSONObjectResponse("html", b.toString());

    }
    catch (IOException e) {
      log.debug("Failed", e);
      return JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
  }

  public JSONObject searchStudy(HttpSession session, JSONObject json) {
    String searchStr = (String) json.get("str");
    try {
      List<Study> studies = new ArrayList<Study>();
      StringBuilder b = new StringBuilder();
      if (searchStr != null && !searchStr.equals("")) {
        studies = new ArrayList<Study>(requestManager.listAllStudiesBySearch(searchStr));
      }
      else {
        studies = new ArrayList<Study>(requestManager.listAllStudies());
      }

      if (studies.size() > 0) {
        Collections.sort(studies);
        for (Study s : studies) {
          b.append("<a class=\"dashboardresult\" href=\"/miso/study/" + s.getStudyId() + "\"><div  onMouseOver=\"this.className=&#39dashboardhighlight&#39\" onMouseOut=\"this.className=&#39dashboard&#39\" class=\"dashboard\">");
          b.append("Name: <b>" + s.getName() + "</b><br/>");
          b.append("Alias: <b>" + s.getAlias() + "</b><br/>");
          b.append("</div></a>");
        }
      }
      else {
        b.append("No matches");
      }
      return JSONUtils.JSONObjectResponse("html", b.toString());
    }
    catch (IOException e) {
      log.debug("Failed", e);
      return JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
  }

  public JSONObject searchExperiment(HttpSession session, JSONObject json) {
    String searchStr = (String) json.get("str");
    try {
      List<Experiment> experiments = new ArrayList<Experiment>();
      StringBuilder b = new StringBuilder();
      if (searchStr != null && !searchStr.equals("")) {
        experiments = new ArrayList<Experiment>(requestManager.listAllExperimentsBySearch(searchStr));
      }
      else {
        experiments = new ArrayList<Experiment>(requestManager.listAllExperiments());
      }

      if (experiments.size() > 0) {
        Collections.sort(experiments);
        for (Experiment e : experiments) {
          b.append("<a class=\"dashboardresult\" href=\"/miso/experiment/" + e.getExperimentId() + "\"><div  onMouseOver=\"this.className=&#39dashboardhighlight&#39\" onMouseOut=\"this.className=&#39dashboard&#39\" class=\"dashboard\">");
          b.append("Name: <b>" + e.getName() + "</b><br/>");
          b.append("Alias: <b>" + e.getAlias() + "</b><br/>");
          b.append("</div></a>");
        }
      }
      else {
        b.append("No matches");
      }
      return JSONUtils.JSONObjectResponse("html", b.toString());
    }
    catch (IOException e) {
      log.debug("Failed", e);
      return JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
  }

  public JSONObject searchRun(HttpSession session, JSONObject json) {
    String searchStr = (String) json.get("str");
    try {
      List<Run> runs = new ArrayList<Run>();
      StringBuilder b = new StringBuilder();
      if (searchStr != null && !searchStr.equals("")) {
        runs = new ArrayList<Run>(requestManager.listAllRunsBySearch(searchStr));
      }
      else {
        runs = new ArrayList<Run>(requestManager.listAllRuns());
      }

      if (runs.size() > 0) {
        Collections.sort(runs);
        for (Run r : runs) {
          b.append("<a class=\"dashboardresult\" href=\"/miso/run/" + r.getRunId() + "\"><div  onMouseOver=\"this.className=&#39dashboardhighlight&#39\" onMouseOut=\"this.className=&#39dashboard&#39\" class=\"dashboard\">");
          b.append("Name: <b>" + r.getName() + "</b><br/>");
          b.append("Alias: <b>" + r.getAlias() + "</b><br/>");
          b.append("</div></a>");
        }
      }
      else {
        b.append("No matches");
      }
      return JSONUtils.JSONObjectResponse("html", b.toString());
    }
    catch (IOException e) {
      log.debug("Failed", e);
      return JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
  }

  public JSONObject searchLibrary(HttpSession session, JSONObject json) {
    String searchStr = (String) json.get("str");
    try {
      List<Library> libraries = new ArrayList<Library>();
      StringBuilder b = new StringBuilder();
      if (searchStr != null && !searchStr.equals("")) {
        libraries = new ArrayList<Library>(requestManager.listAllLibrariesBySearch(searchStr));
      }
      else {
        libraries = new ArrayList<Library>(requestManager.listAllLibraries());
      }

      if (libraries.size() > 0) {
        Collections.sort(libraries);
        for (Library l : libraries) {
          b.append("<a class=\"dashboardresult\" href=\"/miso/library/" + l.getLibraryId() + "\"><div  onMouseOver=\"this.className=&#39dashboardhighlight&#39\" onMouseOut=\"this.className=&#39dashboard&#39\" class=\"dashboard\">");
          b.append("Name: <b>" + l.getName() + "</b><br/>");
          b.append("Alias: <b>" + l.getAlias() + "</b><br/>");
          b.append("</div></a>");
        }
      }
      else {
        b.append("No matches");
      }
      return JSONUtils.JSONObjectResponse("html", b.toString());
    }
    catch (IOException e) {
      log.debug("Failed", e);
      return JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
  }

  public JSONObject searchSample(HttpSession session, JSONObject json) {
    String searchStr = (String) json.get("str");
    try {
      List<Sample> samples = new ArrayList<Sample>();
      StringBuilder b = new StringBuilder();
      if (searchStr != null && !searchStr.equals("")) {
        samples = new ArrayList<Sample>(requestManager.listAllSamplesBySearch(searchStr));
      }
      else {
        samples = new ArrayList<Sample>(requestManager.listAllSamples());
      }

      if (samples.size() > 0) {
        Collections.sort(samples);
        for (Sample s : samples) {
          b.append("<a class=\"dashboardresult\" href=\"/miso/sample/" + s.getSampleId() + "\"><div  onMouseOver=\"this.className=&#39dashboardhighlight&#39\" onMouseOut=\"this.className=&#39dashboard&#39\" class=\"dashboard\">");
          b.append("Name: <b>" + s.getName() + "</b><br/>");
          b.append("Alias: <b>" + s.getAlias() + "</b><br/>");
          b.append("</div></a>");
        }
      }
      else {
        b.append("No matches");
      }
      return JSONUtils.JSONObjectResponse("html", b.toString());
    }
    catch (IOException e) {
      log.debug("Failed", e);
      return JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
  }

  public JSONObject checkAlerts(HttpSession session, JSONObject json) {
    JSONObject response = new JSONObject();
    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      if (!requestManager.listUnreadAlertsByUserId(user.getUserId()).isEmpty()) {
        response.put("newAlerts", true);
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
    return response;
  }

  public JSONObject getAlerts(HttpSession session, JSONObject json) {
    StringBuilder b = new StringBuilder();

    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      List<Alert> alerts;
      if (json.has("showReadAlerts") && json.getBoolean("showReadAlerts")) {
        alerts = new ArrayList<Alert>(requestManager.listAlertsByUserId(user.getUserId()));
      }
      else {
        alerts = new ArrayList<Alert>(requestManager.listUnreadAlertsByUserId(user.getUserId()));
      }
      Collections.sort(alerts);
      for (Alert a : alerts) {
        if (a.getAlertLevel().equals(AlertLevel.CRITICAL) || a.getAlertLevel().equals(AlertLevel.HIGH)) {
          b.append("<div alertId='" + a.getAlertId() + "' class=\"dashboard error\">");
        }
        else {
          b.append("<div alertId='" + a.getAlertId() + "' class=\"dashboard\">");
        }

        b.append(a.getAlertDate() + " <b>" + a.getAlertTitle() + "</b><br/>");
        b.append(a.getAlertText() + "<br/>");
        if (!a.getAlertRead()) {
          b.append("<span onclick='confirmAlertRead(this);' class='float-right ui-icon ui-icon-circle-close'></span>");
        }
        b.append("</div>");
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }

    return JSONUtils.JSONObjectResponse("html", b.toString());
  }

  public JSONObject getSystemAlerts(HttpSession session, JSONObject json) {
    StringBuilder b = new StringBuilder();

    long limit = 20;
    if (json.has("limit")) limit = json.getLong("limit");

    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      if (user.isAdmin()) {
        List<Alert> alerts = new ArrayList<Alert>(requestManager.listAlertsByUserId(0L, limit));
        Collections.sort(alerts);
        Collections.reverse(alerts);
        for (Alert a : alerts) {
          if (a.getAlertLevel().equals(AlertLevel.CRITICAL) || a.getAlertLevel().equals(AlertLevel.HIGH)) {
            b.append("<div alertId='" + a.getAlertId() + "' class=\"dashboard error\">");
          }
          else {
            b.append("<div alertId='" + a.getAlertId() + "' class=\"dashboard\">");
          }

          b.append(a.getAlertDate() + " <b>" + a.getAlertTitle() + "</b><br/>");
          b.append(a.getAlertText() + "<br/>");
          b.append("</div>");
        }
      }
      else {
        JSONUtils.SimpleJSONError("Failed: You do not have access to view system level alerts");
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }

    return JSONUtils.JSONObjectResponse("html", b.toString());
  }

  public JSONObject setAlertAsRead(HttpSession session, JSONObject json) {
    Long alertId = json.getLong("alertId");
    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      Alert a = requestManager.getAlertById(alertId);
      if (a.getAlertUser().equals(user)) {
        a.setAlertRead(true);
        requestManager.saveAlert(a);
      }
      else {
        JSONUtils.SimpleJSONError("You do not have the rights to set this alert as read");
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
    return JSONUtils.SimpleJSONResponse("ok");
  }

  public JSONObject setAllAlertsAsRead(HttpSession session, JSONObject json) {

    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      List<Alert> alerts = new ArrayList<Alert>(requestManager.listUnreadAlertsByUserId(user.getUserId()));
      for (Alert a : alerts) {
        if (a.getAlertUser().equals(user)) {
          a.setAlertRead(true);
          requestManager.saveAlert(a);
        }
        else {
          JSONUtils.SimpleJSONError("You do not have the rights to set this alert as read");
        }
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      JSONUtils.SimpleJSONError("Failed: " + e.getMessage());
    }
    return JSONUtils.SimpleJSONResponse("ok");
  }

  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  public void setRequestManager(RequestManager requestManager) {
    this.requestManager = requestManager;
  }
}
