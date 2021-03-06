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
import com.googlecode.ehcache.annotations.KeyGenerator;
import com.googlecode.ehcache.annotations.Property;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.bbsrc.tgac.miso.core.data.impl.LibraryDilution;
import uk.ac.bbsrc.tgac.miso.core.store.DilutionStore;
import uk.ac.bbsrc.tgac.miso.core.store.EmPCRStore;
import uk.ac.bbsrc.tgac.miso.core.store.Store;
import com.googlecode.ehcache.annotations.Cacheable;
import com.googlecode.ehcache.annotations.TriggersRemove;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.bbsrc.tgac.miso.sqlstore.util.DbUtils;
import uk.ac.bbsrc.tgac.miso.core.data.impl.emPCR;
import uk.ac.bbsrc.tgac.miso.core.factory.DataObjectFactory;

import javax.persistence.CascadeType;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * uk.ac.bbsrc.tgac.miso.sqlstore
 * <p/>
 * Info
 *
 * @author Rob Davey
 * @since 0.0.2
 */
public class SQLEmPCRDAO implements EmPCRStore {

  public static String EMPCR_SELECT =
          "SELECT pcrId, concentration, pcrUserName, creationDate, name, dilution_dilutionId, securityProfile_profileId " +
          "FROM emPCR";

  public static final String EMPCR_SELECT_BY_PCR_ID =
          EMPCR_SELECT + " WHERE pcrId=?";

  public static final String EMPCR_SELECT_BY_RELATED_DILUTION =
          EMPCR_SELECT + " WHERE dilution_dilutionId=?";

  public static String EMPCR_SELECT_BY_PROJECT =
          "SELECT e.* FROM Project p " +
          "INNER JOIN Sample sa ON sa.project_projectId = p.projectId " +
          "INNER JOIN Library li ON li.sample_sampleId = sa.sampleId " +
          "INNER JOIN LibraryDilution ld ON ld.library_libraryId = li.libraryId " +
          "INNER JOIN emPCR e ON e.dilution_dilutionId = ld.dilutionId " +
          "WHERE p.projectId=?";  

  public static final String EMPCR_UPDATE =
          "UPDATE emPCR " +
          "SET concentration=:concentration, pcrUserName=:pcrUserName, creationDate=:creationDate, name=:name, dilution_dilutionId=:dilution_dilutionId, securityProfile_profileId=:securityProfile_profileId " +
          "WHERE pcrId=:pcrId";

  public static final String EMPCR_DELETE =
          "DELETE FROM emPCR WHERE pcrId=:pcrId";

  protected static final Logger log = LoggerFactory.getLogger(SQLEmPCRDAO.class);

  private JdbcTemplate template;
  private DilutionStore dilutionDAO;
  private CascadeType cascadeType;
  private Store<SecurityProfile> securityProfileDAO;

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

  public JdbcTemplate getJdbcTemplate() {
    return template;
  }

  public void setJdbcTemplate(JdbcTemplate template) {
    this.template = template;
  }

  public void setDilutionDAO(DilutionStore dilutionDAO) {
    this.dilutionDAO = dilutionDAO;
  }  

  public void setCascadeType(CascadeType cascadeType) {
    this.cascadeType = cascadeType;
  }

  public Store<SecurityProfile> getSecurityProfileDAO() {
    return securityProfileDAO;
  }

  public void setSecurityProfileDAO(Store<SecurityProfile> securityProfileDAO) {
    this.securityProfileDAO = securityProfileDAO;
  }

  @Transactional(readOnly = false, rollbackFor = IOException.class)
  @TriggersRemove(cacheName="empcrCache",
                  keyGenerator = @KeyGenerator(
                          name = "HashCodeCacheKeyGenerator",
                          properties = {
                                  @Property(name = "includeMethod", value = "false"),
                                  @Property(name = "includeParameterTypes", value = "false")
                          }
                  )
  )
  public long save(emPCR pcr) throws IOException {
    Long securityProfileId = pcr.getSecurityProfile().getProfileId();
    if (securityProfileId == null || (this.cascadeType != null)) { // && this.cascadeType.equals(CascadeType.PERSIST))) {
      securityProfileId = securityProfileDAO.save(pcr.getSecurityProfile());
    }

    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("concentration", pcr.getConcentration())
          .addValue("creationDate", pcr.getCreationDate())
          .addValue("pcrUserName", pcr.getPcrCreator())
          .addValue("dilution_dilutionId", pcr.getLibraryDilution().getDilutionId())
          .addValue("securityProfile_profileId", securityProfileId);

    if (pcr.getPcrId() == emPCR.UNSAVED_ID) {
      SimpleJdbcInsert insert = new SimpleJdbcInsert(template)
                              .withTableName("emPCR")
                              .usingGeneratedKeyColumns("pcrId");
      String name = "EMP"+ DbUtils.getAutoIncrement(template, "emPCR");
      params.addValue("name", name);
      Number newId = insert.executeAndReturnKey(params);
      pcr.setPcrId(newId.longValue());
      pcr.setName(name);
    }
    else {
      params.addValue("pcrId", pcr.getPcrId())
              .addValue("name", pcr.getName());
      NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);
      namedTemplate.update(EMPCR_UPDATE, params);
    }

    if (this.cascadeType != null) {
      LibraryDilution ld = pcr.getLibraryDilution();
      if (this.cascadeType.equals(CascadeType.PERSIST)) {
        if (ld != null) dilutionDAO.save(ld);
      }
      else if (this.cascadeType.equals(CascadeType.REMOVE)) {
        if (ld != null) {
          Cache pc = cacheManager.getCache("libraryDilutionCache");
          pc.remove(DbUtils.hashCodeCacheKeyFor(ld.getDilutionId()));
        }
      }
    }

    return pcr.getPcrId();
  }

  @Cacheable(cacheName="empcrCache",
                  keyGenerator = @KeyGenerator(
                          name = "HashCodeCacheKeyGenerator",
                          properties = {
                                  @Property(name = "includeMethod", value = "false"),
                                  @Property(name = "includeParameterTypes", value = "false")
                          }
                  )
  )
  public emPCR get(long pcrId) throws IOException {
    List eResults = template.query(EMPCR_SELECT_BY_PCR_ID, new Object[]{pcrId}, new EmPCRMapper());
    emPCR e = eResults.size() > 0 ? (emPCR) eResults.get(0) : null;
    return e;
  }

  public emPCR lazyGet(long pcrId) throws IOException {
    List eResults = template.query(EMPCR_SELECT_BY_PCR_ID, new Object[]{pcrId}, new LazyEmPCRMapper());
    emPCR e = eResults.size() > 0 ? (emPCR) eResults.get(0) : null;
    return e;
  }

  public Collection<emPCR> listAllByProjectId(long projectId) throws IOException {
    return template.query(EMPCR_SELECT_BY_PROJECT, new Object[]{projectId}, new EmPCRMapper());
  }

  public Collection<emPCR> listAll() throws IOException {
    return template.query(EMPCR_SELECT, new LazyEmPCRMapper());
  }

  public Collection<emPCR> listAllByDilutionId(long dilutionId) throws IOException {
    return template.query(EMPCR_SELECT_BY_RELATED_DILUTION, new Object[]{dilutionId}, new EmPCRMapper());
  }

  @Transactional(readOnly = false, rollbackFor = IOException.class)
  @TriggersRemove(
          cacheName="empcrCache",
          keyGenerator = @KeyGenerator (
              name = "HashCodeCacheKeyGenerator",
              properties = {
                      @Property(name="includeMethod", value="false"),
                      @Property(name="includeParameterTypes", value="false")
              }
          )
  )
  public boolean remove(emPCR e) throws IOException {
    NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);
    if (e.isDeletable() &&
           (namedTemplate.update(EMPCR_DELETE,
                            new MapSqlParameterSource().addValue("pcrId", e.getPcrId())) == 1)) {
      LibraryDilution ld = e.getLibraryDilution();
      if (this.cascadeType.equals(CascadeType.PERSIST)) {
        if (ld != null) dilutionDAO.save(ld);
      }
      else if (this.cascadeType.equals(CascadeType.REMOVE)) {
        if (ld != null) {
          Cache pc = cacheManager.getCache("emPCRDilutionCache");
          pc.remove(DbUtils.hashCodeCacheKeyFor(ld.getDilutionId()));
        }
      }
      return true;
    }
    return false;
  }

  public class EmPCRMapper implements RowMapper<emPCR> {
    public emPCR mapRow(ResultSet rs, int rowNum) throws SQLException {
      emPCR pcr = dataObjectFactory.getEmPCR();
      pcr.setPcrId(rs.getLong("pcrId"));
      pcr.setConcentration(rs.getDouble("concentration"));
      pcr.setName(rs.getString("name"));
      pcr.setCreationDate(rs.getDate("creationDate"));
      pcr.setPcrCreator(rs.getString("pcrUserName"));

      try {
        pcr.setSecurityProfile(securityProfileDAO.get(rs.getLong("securityProfile_profileId")));
        pcr.setLibraryDilution(dilutionDAO.getLibraryDilutionById(rs.getLong("dilution_dilutionId")));
        pcr.setEmPcrDilutions(dilutionDAO.listAllByEmPCRId(rs.getLong("pcrId")));
      }
      catch (IOException e1) {
        e1.printStackTrace();
      }
      return pcr;
    }
  }

  public class LazyEmPCRMapper implements RowMapper<emPCR> {
    public emPCR mapRow(ResultSet rs, int rowNum) throws SQLException {
      emPCR pcr = dataObjectFactory.getEmPCR();
      pcr.setPcrId(rs.getLong("pcrId"));
      pcr.setConcentration(rs.getDouble("concentration"));
      pcr.setName(rs.getString("name"));
      pcr.setCreationDate(rs.getDate("creationDate"));
      pcr.setPcrCreator(rs.getString("pcrUserName"));

      try {
        pcr.setSecurityProfile(securityProfileDAO.get(rs.getLong("securityProfile_profileId")));
        pcr.setLibraryDilution(dilutionDAO.getLibraryDilutionById(rs.getLong("dilution_dilutionId")));
      }
      catch (IOException e1) {
        e1.printStackTrace();
      }
      return pcr;
    }
  }
}
