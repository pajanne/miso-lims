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

package uk.ac.bbsrc.tgac.miso.sqlstore;

import com.eaglegenomics.simlims.core.SecurityProfile;
import com.googlecode.ehcache.annotations.Cacheable;
import com.googlecode.ehcache.annotations.KeyGenerator;
import com.googlecode.ehcache.annotations.Property;
import com.googlecode.ehcache.annotations.TriggersRemove;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.bbsrc.tgac.miso.core.data.*;
import uk.ac.bbsrc.tgac.miso.core.data.type.PlatformType;
import uk.ac.bbsrc.tgac.miso.core.factory.DataObjectFactory;
import uk.ac.bbsrc.tgac.miso.core.store.PartitionStore;
import uk.ac.bbsrc.tgac.miso.core.store.RunStore;
import uk.ac.bbsrc.tgac.miso.core.store.SequencerPartitionContainerStore;
import uk.ac.bbsrc.tgac.miso.core.store.Store;

import javax.persistence.CascadeType;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * uk.ac.bbsrc.tgac.miso.sqlstore
 * <p/>
 * Info
 *
 * @author Rob Davey
 * @since 0.1.6
 */
public class SQLSequencerPartitionContainerDAO implements SequencerPartitionContainerStore {
  private static final String SEQUENCER_PARTITION_CONTAINER_SELECT =
          "SELECT containerId, platformType, identificationBarcode, locationBarcode, validationBarcode, securityProfile_profileId FROM SequencerPartitionContainer";

  private static final String SEQUENCER_PARTITION_CONTAINER_SELECT_BY_ID =
          SEQUENCER_PARTITION_CONTAINER_SELECT + " WHERE containerId=?";

  private static final String SEQUENCER_PARTITION_CONTAINER_SELECT_BY_PARTITION_ID  =
          "SELECT s.containerId, s.platformType, s.identificationBarcode, s.locationBarcode, s.validationBarcode, s.securityProfile_profileId " +
          "FROM SequencerPartitionContainer s, SequencerPartitionContainer_Partition sp " +
          "WHERE s.containerId=sp.container_containerId " +
          "AND sp.partitions_partitionId=?";

  private static final String SEQUENCER_PARTITION_CONTAINER_SELECT_BY_RELATED_RUN =
          "SELECT DISTINCT f.containerId, f.platformType, f.identificationBarcode, f.locationBarcode, f.validationBarcode, f.securityProfile_profileId " +
          "FROM SequencerPartitionContainer f, Run_SequencerPartitionContainer rf " +
          "WHERE f.containerId=rf.containers_containerId " +
          "AND rf.run_runId=?";

  private static final String SEQUENCER_PARTITION_CONTAINER_SELECT_BY_IDENTIFICATION_BARCODE =
          SEQUENCER_PARTITION_CONTAINER_SELECT + " WHERE identificationBarcode=? ORDER BY containerId DESC";

  public static final String SEQUENCER_PARTITION_CONTAINER_PARTITION_DELETE_BY_SEQUENCER_PARTITION_CONTAINER_ID =
          "DELETE FROM SequencerPartitionContainer_Partition " +
          "WHERE container_containerId=:container_containerId";

  public static final String RUN_SEQUENCER_PARTITION_CONTAINER_DELETE_BY_SEQUENCER_PARTITION_CONTAINER_ID =
          "DELETE FROM Run_SequencerPartitionContainer " +
          "WHERE Run_runId=:Run_runId "+
          "AND containers_containerId=:containers_containerId";

  public static final String SEQUENCER_PARTITION_CONTAINER_UPDATE =
          "UPDATE SequencerPartitionContainer " +
          "SET platformType=:platformType, identificationBarcode=:identificationBarcode, locationBarcode=:locationBarcode, validationBarcode=:validationBarcode, securityProfile_profileId:=securityProfile_profileId " +
          "WHERE containerId=:containerId";

  protected static final Logger log = LoggerFactory.getLogger(SQLSequencerPartitionContainerDAO.class);

  private PartitionStore partitionDAO;
  private RunStore runDAO;
  private Store<SecurityProfile> securityProfileDAO;
  private JdbcTemplate template;
  private CascadeType cascadeType;

  @Autowired
  private CacheManager cacheManager;

  public void setCacheManager(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @Autowired
  private DataObjectFactory dataObjectFactory;

  public void setDataObjectFactory(DataObjectFactory dataObjectFactory) {
    this.dataObjectFactory = dataObjectFactory;
  }

  public void setPartitionDAO(PartitionStore partitionDAO) {
    this.partitionDAO = partitionDAO;
  }

  public void setRunDAO(RunStore runDAO) {
    this.runDAO = runDAO;
  }

  public Store<SecurityProfile> getSecurityProfileDAO() {
    return securityProfileDAO;
  }

  public void setSecurityProfileDAO(Store<SecurityProfile> securityProfileDAO) {
    this.securityProfileDAO = securityProfileDAO;
  }

  public JdbcTemplate getJdbcTemplate() {
    return template;
  }

  public void setJdbcTemplate(JdbcTemplate template) {
    this.template = template;
  }

  public void setCascadeType(CascadeType cascadeType) {
    this.cascadeType = cascadeType;
  }

  @Override
  @Cacheable(cacheName = "containerCache",
                  keyGenerator = @KeyGenerator(
                          name = "HashCodeCacheKeyGenerator",
                          properties = {
                                  @Property(name = "includeMethod", value = "false"),
                                  @Property(name = "includeParameterTypes", value = "false")
                          }
                  )
  )
  public SequencerPartitionContainer<SequencerPoolPartition> get(long sequencerPartitionContainerId) throws IOException {
    List eResults = template.query(SEQUENCER_PARTITION_CONTAINER_SELECT_BY_ID, new Object[]{sequencerPartitionContainerId}, new SequencerPartitionContainerMapper<SequencerPartitionContainer<SequencerPoolPartition>>());
    SequencerPartitionContainer<SequencerPoolPartition> f = eResults.size() > 0 ? (SequencerPartitionContainer<SequencerPoolPartition>) eResults.get(0) : null;
    fillInRun(f);
    return f;
  }

  public SequencerPartitionContainer<SequencerPoolPartition> lazyGet(long sequencerPartitionContainerId) throws IOException {
    List eResults = template.query(SEQUENCER_PARTITION_CONTAINER_SELECT_BY_ID, new Object[]{sequencerPartitionContainerId}, new LazySequencerPartitionContainerMapper<SequencerPartitionContainer<SequencerPoolPartition>>());
    SequencerPartitionContainer<SequencerPoolPartition> f = eResults.size() > 0 ? (SequencerPartitionContainer<SequencerPoolPartition>) eResults.get(0) : null;
    //TODO - this seems to fuck everything up
    //fillInRun(f);
    return f;
  }

  @Override
  @Cacheable(cacheName="containerListCache",
      keyGenerator = @KeyGenerator(
              name = "HashCodeCacheKeyGenerator",
              properties = {
                      @Property(name="includeMethod", value="false"),
                      @Property(name="includeParameterTypes", value="false")
              }
      )
  )
  public Collection<SequencerPartitionContainer<SequencerPoolPartition>> listAll() throws IOException {
    Collection<SequencerPartitionContainer<SequencerPoolPartition>> lp = template.query(SEQUENCER_PARTITION_CONTAINER_SELECT, new LazySequencerPartitionContainerMapper<SequencerPartitionContainer<SequencerPoolPartition>>());
    for (SequencerPartitionContainer<SequencerPoolPartition> f : lp) {
      fillInRun(f);
    }
    return lp;
  }

  @Override
  public List<SequencerPartitionContainer<SequencerPoolPartition>> listSequencerPartitionContainersByBarcode(String barcode) throws IOException {
    List<SequencerPartitionContainer<SequencerPoolPartition>> lp =  template.query(SEQUENCER_PARTITION_CONTAINER_SELECT_BY_IDENTIFICATION_BARCODE, new Object[]{barcode}, new LazySequencerPartitionContainerMapper<SequencerPartitionContainer<SequencerPoolPartition>>());
    for (SequencerPartitionContainer<SequencerPoolPartition> f : lp) {
      fillInRun(f);
    }
    return lp;
  }

  @Override
  public List<SequencerPartitionContainer<SequencerPoolPartition>> listAllSequencerPartitionContainersByRunId(long runId) throws IOException {
    List<SequencerPartitionContainer<SequencerPoolPartition>> lp =  template.query(SEQUENCER_PARTITION_CONTAINER_SELECT_BY_RELATED_RUN, new Object[]{runId}, new LazySequencerPartitionContainerMapper<SequencerPartitionContainer<SequencerPoolPartition>>());
    for (SequencerPartitionContainer<SequencerPoolPartition> f : lp) {
      fillInRun(f, runId);
    }
    return lp;
  }

  @Override
  public Collection<? extends SequencerPoolPartition> listPartitionsByContainerId(long sequencerPartitionContainerId) throws IOException {
    return partitionDAO.listBySequencerPartitionContainerId(sequencerPartitionContainerId);
  }

  @Override
  public SequencerPartitionContainer<SequencerPoolPartition> getSequencerPartitionContainerByPartitionId(long partitionId) throws IOException {
    List eResults = template.query(SEQUENCER_PARTITION_CONTAINER_SELECT_BY_PARTITION_ID, new Object[]{partitionId}, new LazySequencerPartitionContainerMapper<SequencerPartitionContainer<SequencerPoolPartition>>());
    SequencerPartitionContainer<SequencerPoolPartition> f = eResults.size() > 0 ? (SequencerPartitionContainer<SequencerPoolPartition>) eResults.get(0) : null;
    fillInRun(f);
    return f;
  }

  private void fillInRun(SequencerPartitionContainer<SequencerPoolPartition> container) throws IOException {
    container.setRun(runDAO.getLatestRunIdRunBySequencerPartitionContainerId(container.getContainerId()));
  }

  private void fillInRun(SequencerPartitionContainer<SequencerPoolPartition> container, long runId) throws IOException {
    Run r = runDAO.lazyGet(runId);
    container.setRun(r);
  }

  private void purgeListCache(SequencerPartitionContainer<SequencerPoolPartition> s, boolean replace) {
    Cache cache = cacheManager.getCache("containerListCache");
    if (cache.getKeys().size() > 0) {
      Object cachekey = cache.getKeys().get(0);
      List<SequencerPartitionContainer<SequencerPoolPartition>> c = (List<SequencerPartitionContainer<SequencerPoolPartition>>)cache.get(cachekey).getValue();
      if (c.remove(s)) {
        if (replace) {
          c.add(s);
        }
      }
      else {
        c.add(s);
      }
      cache.put(new Element(cachekey, c));
    }
  }

  private void purgeListCache(SequencerPartitionContainer<SequencerPoolPartition> s) {
    purgeListCache(s, true);
  }

  @Override
  @Transactional(readOnly = false, rollbackFor = IOException.class)
  @TriggersRemove(cacheName = "containerCache",
                  keyGenerator = @KeyGenerator(
                          name = "HashCodeCacheKeyGenerator",
                          properties = {
                                  @Property(name = "includeMethod", value = "false"),
                                  @Property(name = "includeParameterTypes", value = "false")
                          }
                  )
  )
  public long save(SequencerPartitionContainer<SequencerPoolPartition> sequencerPartitionContainer) throws IOException {
    Long securityProfileId = sequencerPartitionContainer.getSecurityProfile().getProfileId();
    if (securityProfileId == null || (this.cascadeType != null)) { // && this.cascadeType.equals(CascadeType.PERSIST))) {
      securityProfileId = securityProfileDAO.save(sequencerPartitionContainer.getSecurityProfile());
    }

    MapSqlParameterSource params = new MapSqlParameterSource();

    params.addValue("securityProfile_profileId", securityProfileId);
    params.addValue("identificationBarcode", sequencerPartitionContainer.getIdentificationBarcode());
    params.addValue("locationBarcode", sequencerPartitionContainer.getLocationBarcode());
    params.addValue("validationBarcode", sequencerPartitionContainer.getValidationBarcode());

    if (sequencerPartitionContainer.getPlatformType() != null) {
      params.addValue("platformType", sequencerPartitionContainer.getPlatformType().getKey());
    }

    if (sequencerPartitionContainer.getContainerId() == AbstractSequencerPartitionContainer.UNSAVED_ID) {
      SimpleJdbcInsert insert = new SimpleJdbcInsert(template)
              .withTableName("SequencerPartitionContainer")
              .usingGeneratedKeyColumns("containerId");
      Number newId = insert.executeAndReturnKey(params);
      sequencerPartitionContainer.setContainerId(newId.longValue());
    }
    else {
      params.addValue("containerId", sequencerPartitionContainer.getContainerId());
      NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);
      namedTemplate.update(SEQUENCER_PARTITION_CONTAINER_UPDATE, params);
    }

    MapSqlParameterSource delparams = new MapSqlParameterSource();
    delparams.addValue("container_containerId", sequencerPartitionContainer.getContainerId());
    NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);
    namedTemplate.update(SEQUENCER_PARTITION_CONTAINER_PARTITION_DELETE_BY_SEQUENCER_PARTITION_CONTAINER_ID, delparams);

    if (sequencerPartitionContainer.getPartitions() != null && !sequencerPartitionContainer.getPartitions().isEmpty()) {
      SimpleJdbcInsert eInsert = new SimpleJdbcInsert(template)
              .withTableName("SequencerPartitionContainer_Partition");

      for (SequencerPoolPartition l : sequencerPartitionContainer.getPartitions()) {
        l.setSecurityProfile(sequencerPartitionContainer.getSecurityProfile());
        long partitionId = partitionDAO.save(l);

        MapSqlParameterSource flParams = new MapSqlParameterSource();
        flParams.addValue("container_containerId", sequencerPartitionContainer.getContainerId())
                .addValue("partitions_partitionId", partitionId);
        try {
          eInsert.execute(flParams);
        }
        catch (DuplicateKeyException dke) {
          log.warn("This Container/Partition combination already exists - not inserting: " + dke.getMessage());
        }
      }
    }

    if (this.cascadeType != null) {
      purgeListCache(sequencerPartitionContainer);
    }

    return sequencerPartitionContainer.getContainerId();
  }

  public class LazySequencerPartitionContainerMapper<T extends SequencerPartitionContainer<SequencerPoolPartition>> implements RowMapper<T> {
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
      SequencerPartitionContainer<SequencerPoolPartition> s = null;
      try {
        s = dataObjectFactory.getSequencerPartitionContainer();
        s.setContainerId(rs.getLong("containerId"));
        List<SequencerPoolPartition> partitions = new ArrayList<SequencerPoolPartition>(partitionDAO.listBySequencerPartitionContainerId(rs.getLong("containerId")));
        for (SequencerPoolPartition part : partitions) {
          part.setSequencerPartitionContainer(s);
        }
        s.setPartitions((partitions));

        if ((rs.getString("platformType") == null || "".equals(rs.getString("platformType"))) && s.getRun() != null) {
          s.setPlatformType(s.getRun().getPlatformType());
        }
        else {
          s.setPlatformType(PlatformType.get(rs.getString("platformType")));
        }

        s.setIdentificationBarcode(rs.getString("identificationBarcode"));
        s.setLocationBarcode(rs.getString("locationBarcode"));
        s.setValidationBarcode(rs.getString("validationBarcode"));
        s.setSecurityProfile(securityProfileDAO.get(rs.getLong("securityProfile_profileId")));
      }
      catch (IOException e1) {
        e1.printStackTrace();
      }
      return (T)s;
    }
  }

  public class SequencerPartitionContainerMapper<T extends SequencerPartitionContainer<SequencerPoolPartition>> implements RowMapper<T> {
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
      SequencerPartitionContainer<SequencerPoolPartition> s = null;
      try {
        s = dataObjectFactory.getSequencerPartitionContainer();
        s.setContainerId(rs.getLong("containerId"));
        Collection<SequencerPoolPartition> partitions = partitionDAO.listBySequencerPartitionContainerId(rs.getLong("containerId"));
        s.setPartitions(new ArrayList<SequencerPoolPartition>(partitions));

        if ((rs.getString("platformType") == null || "".equals(rs.getString("platformType"))) && s.getRun() != null) {
          s.setPlatformType(s.getRun().getPlatformType());
        }
        else {
          s.setPlatformType(PlatformType.get(rs.getString("platformType")));
        }

        s.setIdentificationBarcode(rs.getString("identificationBarcode"));
        s.setLocationBarcode(rs.getString("locationBarcode"));
        s.setValidationBarcode(rs.getString("validationBarcode"));
        s.setSecurityProfile(securityProfileDAO.get(rs.getLong("securityProfile_profileId")));
      }
      catch (IOException e1) {
        e1.printStackTrace();
      }
      return (T)s;
    }
  }
}
