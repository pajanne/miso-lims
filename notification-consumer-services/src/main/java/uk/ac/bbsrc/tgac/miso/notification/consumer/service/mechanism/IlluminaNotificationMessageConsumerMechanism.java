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

package uk.ac.bbsrc.tgac.miso.notification.consumer.service.mechanism;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.util.Assert;
import uk.ac.bbsrc.tgac.miso.core.data.*;
import uk.ac.bbsrc.tgac.miso.core.data.impl.RunImpl;
import uk.ac.bbsrc.tgac.miso.core.data.impl.SequencerPartitionContainerImpl;
import uk.ac.bbsrc.tgac.miso.core.data.impl.illumina.IlluminaRun;
import uk.ac.bbsrc.tgac.miso.core.data.impl.illumina.IlluminaStatus;
import uk.ac.bbsrc.tgac.miso.core.data.type.HealthType;
import uk.ac.bbsrc.tgac.miso.core.data.type.PlatformType;
import uk.ac.bbsrc.tgac.miso.core.exception.InterrogationException;
import uk.ac.bbsrc.tgac.miso.core.manager.RequestManager;
import uk.ac.bbsrc.tgac.miso.core.service.integration.mechanism.NotificationMessageConsumerMechanism;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * uk.ac.bbsrc.tgac.miso.core.service.integration.mechanism.impl
 * <p/>
 * Info
 *
 * @author Rob Davey
 * @date 03/02/12
 * @since 0.1.5
 */
public class IlluminaNotificationMessageConsumerMechanism implements NotificationMessageConsumerMechanism<Message<Map<String, List<String>>>, Set<Run>> {
  protected static final Logger log = LoggerFactory.getLogger(IlluminaNotificationMessageConsumerMechanism.class);

  public boolean attemptRunPopulation = true;

  public void setAttemptRunPopulation(boolean attemptRunPopulation) {
    this.attemptRunPopulation = attemptRunPopulation;
  }

  private final String runDirRegex = "[\\d]+_([A-z0-9])+_([\\d])+_([A-z0-9_\\-])*";
  private final Pattern p = Pattern.compile(runDirRegex);
  private final DateFormat logDateFormat = new SimpleDateFormat("MM'/'dd'/'yyyy','HH:mm:ss");
  private final DateFormat anotherLogDateFormat = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH:mm:ss");
  private final DateFormat illuminaRunFolderDateFormat = new SimpleDateFormat("yyMMdd");

  @Override
  public Set<Run> consume(Message<Map<String, List<String>>> message) throws InterrogationException {
    RequestManager requestManager = message.getHeaders().get("handler", RequestManager.class);
    Assert.notNull(requestManager, "Cannot consume MISO notification messages without a RequestManager.");
    Map<String, List<String>> statuses = message.getPayload();
    Set<Run> output = new HashSet<Run>();
    for (String key : statuses.keySet()) {
      HealthType ht = HealthType.valueOf(key);
      JSONArray runs = (JSONArray) JSONArray.fromObject(statuses.get(key)).get(0);
      Map<String, Run> map = processRunJSON(ht, runs, requestManager);
      for (Run r : map.values()) {
        output.add(r);
      }
    }
    return output;
  }

  private Map<String, Run> processRunJSON(HealthType ht, JSONArray runs, RequestManager requestManager) {
    Map<String, Run> updatedRuns = new HashMap<String, Run>();

    StringBuilder sb = new StringBuilder();

    for (JSONObject run : (Iterable<JSONObject>) runs) {
      String runName = run.getString("runName");
      sb.append("Processing " + runName);
      log.debug("Processing " + runName);
      Status is = new IlluminaStatus();
      is.setRunName(runName);
      Run r = null;

      Matcher m = p.matcher(runName);
      if (m.matches()) {
        try {
          r = requestManager.getRunByAlias(runName);
        }
        catch(IOException ioe) {
          log.warn("Cannot find run by the alias "+runName+". This usually means the run hasn't been previously imported. If attemptRunPopulation is false, processing will not take place for this run!");
        }
      }

      try {
        if (attemptRunPopulation) {
          if (r == null) {
            log.debug("Saving new run and status: " + runName);
            if (!run.has("status")) {
              //probably MiSeq
              r = new IlluminaRun();
              r.setPlatformRunId(Integer.parseInt(m.group(2)));
              r.setAlias(runName);
              r.setFilePath(runName);
              r.setDescription(m.group(3));
              r.setPairedEnd(false);
              is.setHealth(ht);
              r.setStatus(is);
            }
            else {
              String xml = run.getString("status");
              is = new IlluminaStatus(xml);
              r = new IlluminaRun(xml);
              is.setHealth(ht);
              r.getStatus().setHealth(ht);
            }

            if (run.has("fullPath")) {
              r.setFilePath(run.getString("fullPath"));
            }

            SequencerReference sr = null;
            if (run.has("sequencerName")) {
              sr = requestManager.getSequencerReferenceByName(run.getString("sequencerName"));
              r.getStatus().setInstrumentName(run.getString("sequencerName"));
              r.setSequencerReference(sr);
            }
            if (r.getSequencerReference() == null) {
              sr = requestManager.getSequencerReferenceByName(m.group(1));
              r.setSequencerReference(sr);
            }
            if (r.getSequencerReference() == null) {
              sr = requestManager.getSequencerReferenceByName(r.getStatus().getInstrumentName());
              r.setSequencerReference(sr);
            }

            if (r.getSequencerReference() != null) {
              log.debug("Setting sequencer reference: " + sr.getName());
            }
            else {
              log.error("Cannot save " + is.getRunName() + ": no sequencer reference available.");
            }

            if (run.has("startDate")) {
              try {
                if (run.get("startDate") != null && !run.getString("startDate").equals("null") && !"".equals(run.getString("startDate"))) {
                  log.debug("Updating start date:" + run.getString("startDate"));
                  r.getStatus().setStartDate(illuminaRunFolderDateFormat.parse(run.getString("startDate")));
                }
              }
              catch (ParseException e) {
                log.error(e.getMessage());
                e.printStackTrace();
              }
            }

            if (run.has("completionDate")) {
              try {
                if (run.get("completionDate") != null && !run.getString("completionDate").equals("null") && !"".equals(run.getString("completionDate"))) {
                  log.debug("Updating completion date:" + run.getString("completionDate"));
                  r.getStatus().setCompletionDate(logDateFormat.parse(run.getString("completionDate")));
                }
              }
              catch (ParseException e) {
                log.error(e.getMessage());
                e.printStackTrace();
              }
            }
          }
          else {
            log.debug("Updating existing run and status: " + is.getRunName());

            r.setPlatformType(PlatformType.ILLUMINA);

            if (r.getStatus() != null && run.has("status")) {
              r.getStatus().setHealth(ht);
              r.getStatus().setXml(run.getString("status"));
            }
            else {
              if (run.has("status")) {
                is.setXml(run.getString("status"));
                is.setHealth(ht);
                r.setStatus(is);
              }
            }

            if (r.getSequencerReference() == null) {
              SequencerReference sr = null;
              if (run.has("sequencerName")) {
                sr = requestManager.getSequencerReferenceByName(run.getString("sequencerName"));
                r.getStatus().setInstrumentName(run.getString("sequencerName"));
                r.setSequencerReference(sr);
              }
              if (r.getSequencerReference() == null) {
                sr = requestManager.getSequencerReferenceByName(m.group(1));
                r.setSequencerReference(sr);
              }
              if (r.getSequencerReference() == null) {
                sr = requestManager.getSequencerReferenceByName(r.getStatus().getInstrumentName());
                r.setSequencerReference(sr);
              }
            }

            if (run.has("startDate")) {
              try {
                if (run.get("startDate") != null && !run.getString("startDate").equals("null") && !"".equals(run.getString("startDate"))) {
                  log.debug("Updating start date:" + run.getString("startDate"));
                  r.getStatus().setStartDate(illuminaRunFolderDateFormat.parse(run.getString("startDate")));
                }
              }
              catch (ParseException e) {
                log.error(runName + ": "+ e.getMessage());
                e.printStackTrace();
              }
            }

            if (run.has("completionDate")) {
              if (run.get("completionDate") != null && !run.getString("completionDate").equals("null") && !"".equals(run.getString("completionDate"))) {
                log.debug("Updating completion date:" + run.getString("completionDate"));
                try {
                  r.getStatus().setCompletionDate(logDateFormat.parse(run.getString("completionDate")));
                }
                catch (ParseException e) {
                  log.error(runName + ": "+ e.getMessage());
                  try {
                    r.getStatus().setCompletionDate(anotherLogDateFormat.parse(run.getString("completionDate")));
                  }
                  catch (ParseException e1) {
                    log.error(runName + ": "+ e1.getMessage());
                    e1.printStackTrace();
                  }
                }
              }
              else {
                if (!r.getStatus().getHealth().equals(HealthType.Completed) &&
                    !r.getStatus().getHealth().equals(HealthType.Failed) &&
                    !r.getStatus().getHealth().equals(HealthType.Stopped)) {
                  r.getStatus().setCompletionDate(null);
                }
              }
            }

            //update path if changed
            if (run.has("fullPath") && !"".equals(run.getString("fullPath")) && r.getFilePath() != null && !"".equals(r.getFilePath())) {
              if (!run.getString("fullPath").equals(r.getFilePath())) {
                log.debug("Updating run file path:" + r.getFilePath() + " -> " + run.getString("fullPath"));
                r.setFilePath(run.getString("fullPath"));
              }
            }
          }

          Collection<SequencerPartitionContainer<SequencerPoolPartition>> fs = ((IlluminaRun) r).getSequencerPartitionContainers();
          if (fs.isEmpty()) {
            if (run.has("containerId") && !"".equals(run.getString("containerId"))) {
              Collection<SequencerPartitionContainer<SequencerPoolPartition>> pfs = requestManager.listSequencerPartitionContainersByBarcode(run.getString("containerId"));
              if (!pfs.isEmpty()) {
                if (pfs.size() == 1) {
                  SequencerPartitionContainer<SequencerPoolPartition> lf = new ArrayList<SequencerPartitionContainer<SequencerPoolPartition>>(pfs).get(0);
                  if (lf.getSecurityProfile() != null && r.getSecurityProfile() == null) {
                    r.setSecurityProfile(lf.getSecurityProfile());
                  }
                  if (lf.getPlatformType() == null && r.getPlatformType() != null) {
                    lf.setPlatformType(r.getPlatformType());
                  }
                  ((RunImpl)r).addSequencerPartitionContainer(lf);
                }
                else {
                  //more than one flowcell hit to this barcode
                  log.warn(r.getAlias() + ":: More than one partition container has this barcode. Cannot automatically link to a pre-existing barcode.");
                }
              }
              else {
                SequencerPartitionContainer<SequencerPoolPartition> f = new SequencerPartitionContainerImpl();
                f.setSecurityProfile(r.getSecurityProfile());
                if (f.getPlatformType() == null && r.getPlatformType() != null) {
                  f.setPlatformType(r.getPlatformType());
                }

                if (r.getSequencerReference().getPlatform().getInstrumentModel().contains("MiSeq")) {
                  f.setPartitionLimit(1);
                }
                f.initEmptyPartitions();
                f.setIdentificationBarcode(run.getString("containerId"));
                ((RunImpl)r).addSequencerPartitionContainer(f);
              }
            }
          }
          else {
            SequencerPartitionContainer<SequencerPoolPartition> f = fs.iterator().next();
            f.setSecurityProfile(r.getSecurityProfile());
            if (f.getPlatformType() == null && r.getPlatformType() != null) {
              f.setPlatformType(r.getPlatformType());
            }

            if (f.getPartitions().isEmpty()) {
              log.info("No partitions found for run " + r.getName() + " (container "+f.getContainerId()+")");
              if (r.getSequencerReference().getPlatform().getInstrumentModel().contains("MiSeq")) {
                f.setPartitionLimit(1);
              }
              f.initEmptyPartitions();
            }

            if (f.getIdentificationBarcode() == null || "".equals(f.getIdentificationBarcode())) {
              if (run.has("containerId") && !"".equals(run.getString("containerId"))) {
                f.setIdentificationBarcode(run.getString("containerId"));
                requestManager.saveSequencerPartitionContainer(f);
              }
            }
          }

          long runId = requestManager.saveRun(r);
          r.setRunId(runId);
          updatedRuns.put(r.getAlias(), r);
        }
        else {
          log.warn("\\_ Run not saved. Saving status: " + is.getRunName());
          requestManager.saveStatus(is);
        }
      }
      catch(IOException ioe) {
        log.error(ioe.getMessage());
        ioe.printStackTrace();
      }
      sb.append("...done\n");
    }
    log.info(sb.toString());
    return updatedRuns;
  }
}
