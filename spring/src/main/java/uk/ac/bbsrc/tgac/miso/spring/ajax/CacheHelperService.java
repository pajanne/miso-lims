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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sourceforge.fluxion.ajax.Ajaxified;
import net.sourceforge.fluxion.ajax.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.bbsrc.tgac.miso.core.data.Library;
import uk.ac.bbsrc.tgac.miso.core.data.Pool;
import uk.ac.bbsrc.tgac.miso.core.data.Sample;
import uk.ac.bbsrc.tgac.miso.core.data.impl.LibraryDilution;
import uk.ac.bbsrc.tgac.miso.core.data.impl.emPCRDilution;
import uk.ac.bbsrc.tgac.miso.core.event.manager.PoolAlertManager;
import uk.ac.bbsrc.tgac.miso.core.event.manager.ProjectAlertManager;
import uk.ac.bbsrc.tgac.miso.core.event.manager.RunAlertManager;
import uk.ac.bbsrc.tgac.miso.core.manager.RequestManager;
import uk.ac.bbsrc.tgac.miso.sqlstore.util.DbUtils;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

/**
 * uk.ac.bbsrc.tgac.miso.spring.ajax
 * <p/>
 * Info
 *
 * @author Rob Davey
 * @since 0.0.2
 */
@Ajaxified
public class CacheHelperService {
  protected static final Logger log = LoggerFactory.getLogger(CacheHelperService.class);

  @Autowired
  private CacheManager cacheManager;

  public void setCacheManager(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @Autowired
  private RequestManager requestManager;

  public void setRequestManager(RequestManager requestManager) {
    this.requestManager = requestManager;
  }

  @Autowired
  private ProjectAlertManager projectAlertManager;

  public void setProjectAlertManager(ProjectAlertManager projectAlertManager) {
    this.projectAlertManager = projectAlertManager;
  }

  @Autowired
  private RunAlertManager runAlertManager;

  public void setRunAlertManager(RunAlertManager runAlertManager) {
    this.runAlertManager = runAlertManager;
  }

  @Autowired
  private PoolAlertManager poolAlertManager;

  public void setPoolAlertManager(PoolAlertManager poolAlertManager) {
    this.poolAlertManager = poolAlertManager;
  }

  public JSONObject flushAllCaches(HttpSession session, JSONObject json) {
    DbUtils.flushAllCaches(cacheManager);
    log.info("Caches flushed!");
    return JSONUtils.JSONObjectResponse("html", jQueryDialogFactory.okDialog("Cache Administration", "Caches flushed successfully!"));
  }

  public JSONObject viewCacheStats(HttpSession session, JSONObject json) {
    Map<String, Object> response = new HashMap<String, Object>();
    List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
    Collections.sort(cacheNames);
    JSONArray caches = new JSONArray();
    for (String s : cacheNames) {
      Cache c = cacheManager.getCache(s);
      JSONObject j = new JSONObject();
      j.put("name",s);
      j.put("size",c.getSize());
      j.put("hits", c.getLiveCacheStatistics().getCacheHitCount());
      j.put("searchtimes", c.getLiveCacheStatistics().getAverageGetTimeMillis() + " ("+c.getLiveCacheStatistics().getMaxGetTimeMillis()+")");
      caches.add(j);
    }
    response.put("caches", caches);
    return JSONUtils.JSONObjectResponse(response);
  }

  public JSONObject regenerateAllBarcodes(HttpSession session, JSONObject json) {
    try {
      for (Sample s : requestManager.listAllSamples()) {
        if (s.getIdentificationBarcode() == null || "".equals(s.getIdentificationBarcode())) {
          requestManager.saveSample(s);
        }
      }

      for (LibraryDilution ld : requestManager.listAllLibraryDilutions()) {
        if (ld.getIdentificationBarcode() == null || "".equals(ld.getIdentificationBarcode())) {
          requestManager.saveLibraryDilution(ld);
        }
      }

      for (emPCRDilution ed : requestManager.listAllEmPcrDilutions()) {
        if (ed.getIdentificationBarcode() == null || "".equals(ed.getIdentificationBarcode())) {
          requestManager.saveEmPCRDilution(ed);
        }
      }

      for (Library l : requestManager.listAllLibraries()) {
        if (l.getIdentificationBarcode() == null || "".equals(l.getIdentificationBarcode())) {
          requestManager.saveLibrary(l);
        }
      }

      for (Pool p : requestManager.listAllPools()) {
        if (p.getIdentificationBarcode() == null || "".equals(p.getIdentificationBarcode())) {
          requestManager.savePool(p);
        }
      }

      //requestManager.listAllFlowcells();      
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.JSONObjectResponse("html", jQueryDialogFactory.errorDialog("Cache Administration", "Barcode regeneration failed!:\n\n" + e.getMessage()));
    }

    DbUtils.flushAllCaches(cacheManager);
    log.info("Barcodes regenerated!");
    return JSONUtils.JSONObjectResponse("html", jQueryDialogFactory.okDialog("Cache Administration", "Barcodes regenerated successfully!"));
  }

  public JSONObject reindexAlertManagers(HttpSession session, JSONObject json) {
    try {
      log.info("Alert managers reindexing...");
      if (projectAlertManager != null) projectAlertManager.indexify();
      if (runAlertManager != null) runAlertManager.indexify();
      if (poolAlertManager != null) poolAlertManager.indexify();
    }
    catch (IOException e) {
      e.printStackTrace();
      return JSONUtils.JSONObjectResponse("html", jQueryDialogFactory.okDialog("Cache Administration", "Unable to reindex alert managers: " + e.getMessage()));
    }
    return JSONUtils.JSONObjectResponse("html", jQueryDialogFactory.okDialog("Cache Administration", "Alert Managers reindexing!"));
  }
}
