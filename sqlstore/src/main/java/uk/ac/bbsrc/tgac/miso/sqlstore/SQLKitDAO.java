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

import com.eaglegenomics.simlims.core.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import uk.ac.bbsrc.tgac.miso.core.data.AbstractKit;
import uk.ac.bbsrc.tgac.miso.core.data.Kit;
import uk.ac.bbsrc.tgac.miso.core.data.Platform;
import uk.ac.bbsrc.tgac.miso.core.data.impl.kit.*;
import uk.ac.bbsrc.tgac.miso.core.data.type.KitType;
import uk.ac.bbsrc.tgac.miso.core.data.type.PlatformType;
import uk.ac.bbsrc.tgac.miso.core.factory.DataObjectFactory;
import uk.ac.bbsrc.tgac.miso.core.store.ExperimentStore;
import uk.ac.bbsrc.tgac.miso.core.store.KitStore;
import uk.ac.bbsrc.tgac.miso.core.store.NoteStore;

import javax.persistence.CascadeType;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * uk.ac.bbsrc.tgac.miso.sqlstore
 * <p/>
 * Info
 *
 * @author Rob Davey
 * @since 0.0.2
 */
public class SQLKitDAO implements KitStore {
  public static final String KITS_SELECT =
          "SELECT kitId, identificationBarcode, locationBarcode, lotNumber, kitDate, kitDescriptorId " +
          "FROM Kit";

  public static final String KIT_SELECT_BY_ID =
          KITS_SELECT + " WHERE kitId = ?";

  public static final String KIT_SELECT_BY_BARCODE =
          KITS_SELECT + " WHERE identificationBarcode = ?";

  public static final String KIT_SELECT_BY_LOT_NUMBER =
          KITS_SELECT + " WHERE lotNumber = ?";  

  public static final String KITS_SELECT_BY_TYPE =
          "SELECT k.kitId, k.identificationBarcode, k.locationBarcode, k.lotNumber, k.kitDate, k.kitDescriptorId, ek.experiments_experimentId " +
          "FROM Kit k, Experiment_Kit ek " +
          "WHERE ek.kits_kitId=k.kitId " +
          "AND ek.experiments_experimentId=?";

  public static final String KITS_SELECT_BY_MANUFACTURER =
          "SELECT k.kitId, k.identificationBarcode, k.locationBarcode, k.lotNumber, k.kitDate, k.kitDescriptorId, ek.experiments_experimentId " +
          "FROM Kit k, Experiment_Kit ek " +
          "WHERE ek.kits_kitId=k.kitId " +
          "AND ek.experiments_experimentId=?";

  public static final String KITS_SELECT_BY_RELATED_EXPERIMENT =
          "SELECT k.kitId, k.identificationBarcode, k.locationBarcode, k.lotNumber, k.kitDate, k.kitDescriptorId, ek.experiments_experimentId " +
          "FROM Kit k, Experiment_Kit ek " +
          "WHERE ek.kits_kitId=k.kitId " +
          "AND ek.experiments_experimentId=?";

  public static final String KITS_SELECT_BY_RELATED_LIBRARY =
          "SELECT k.kitId, k.identificationBarcode, k.locationBarcode, k.lotNumber, k.kitDate, k.kitDescriptorId, ek.experiments_experimentId " +
          "FROM Kit k, Library_Kit lk " +
          "WHERE lk.kits_kitId=k.kitId " +
          "AND lk.libraries_libraryId=?";

  public static final String KIT_UPDATE =
          "UPDATE Kit " +
          "SET identificationBarcode=:identificationBarcode, locationBarcode=:locationBarcode, lotNumber=:lotNumber, kitDate=:kitDate, kitDescriptorId=:kitDescriptorId " +
          "WHERE kitId=:kitId";

  public static final String KIT_DESCRIPTORS_SELECT =
          "SELECT kitDescriptorId, name, version, manufacturer, partNumber, stockLevel, kitType, platformType " +
          "FROM KitDescriptor";

  public static final String KIT_DESCRIPTOR_SELECT_BY_ID =
          KIT_DESCRIPTORS_SELECT + " WHERE kitDescriptorId=?";

  public static final String KIT_DESCRIPTOR_SELECT_BY_PART_NUMBER =
          KIT_DESCRIPTORS_SELECT + " WHERE partNumber=?";  

  public static final String KIT_DESCRIPTORS_SELECT_BY_TYPE =
          KIT_DESCRIPTORS_SELECT + " WHERE kitType = ?";

  public static final String KIT_DESCRIPTORS_SELECT_BY_PLATFORM =
          KIT_DESCRIPTORS_SELECT + " WHERE platformType = ?";

  public static final String KIT_DESCRIPTOR_UPDATE =
          "UPDATE KitDescriptor " +
          "SET name=:name, version=:version, manufacturer=:manufacturer, partNumber=:partNumber, stockLevel=:stockLevel, kitType=:kitType, platformType=:platformType " +
          "WHERE kitDescriptorId=:kitDescriptorId";  

  protected static final Logger log = LoggerFactory.getLogger(SQLKitDAO.class);
  private JdbcTemplate template;
  private NoteStore noteDAO;
  private CascadeType cascadeType;

  @Autowired
  private DataObjectFactory dataObjectFactory;

  public void setDataObjectFactory(DataObjectFactory dataObjectFactory) {
    this.dataObjectFactory = dataObjectFactory;
  }

  public void setNoteDAO(NoteStore noteDAO) {
    this.noteDAO = noteDAO;
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

  public Kit get(long id) throws IOException {
    List eResults = template.query(KIT_SELECT_BY_ID, new Object[]{id}, new KitMapper());
    return eResults.size() > 0 ? (Kit) eResults.get(0) : null;
  }

  public Kit getKitByIdentificationBarcode(String barcode) throws IOException {
    List eResults = template.query(KIT_SELECT_BY_BARCODE, new Object[]{barcode}, new KitMapper());
    return eResults.size() > 0 ? (Kit) eResults.get(0) : null;
  }

  public Kit getKitByLotNumber(String lotNumber) throws IOException {
    List eResults = template.query(KIT_SELECT_BY_LOT_NUMBER, new Object[]{lotNumber}, new KitMapper());
    return eResults.size() > 0 ? (Kit) eResults.get(0) : null;
  }

  public Collection<Kit> listAll() throws IOException {
    return template.query(KITS_SELECT, new KitMapper());
  }

  public List<Kit> listByExperiment(long experimentId) throws IOException {
    return template.query(KITS_SELECT_BY_RELATED_EXPERIMENT, new Object[]{experimentId}, new KitMapper());
  }

  public List<Kit> listByLibrary(long libraryId) throws IOException {
    return template.query(KITS_SELECT_BY_RELATED_LIBRARY, new Object[]{libraryId}, new KitMapper());
  }

  public List<Kit> listByManufacturer(String manufacturerName) throws IOException {
    return template.query(KITS_SELECT_BY_MANUFACTURER, new Object[]{manufacturerName}, new KitMapper());
  }

  public List<Kit> listKitsByType(KitType kitType) throws IOException {
    return template.query(KITS_SELECT_BY_TYPE, new Object[]{kitType.getKey()}, new KitMapper());
  }

  public long save(Kit kit) throws IOException {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("identificationBarcode", kit.getIdentificationBarcode())
            .addValue("locationBarcode", kit.getLocationBarcode())
            .addValue("lotNumber", kit.getLotNumber())
            .addValue("kitDate", kit.getKitDate())
            .addValue("kitDescriptorId", kit.getKitDescriptor().getKitDescriptorId());

    if (kit.getKitId() == AbstractKit.UNSAVED_ID) {
      SimpleJdbcInsert insert = new SimpleJdbcInsert(template)
                            .withTableName("Kit")
                            .usingGeneratedKeyColumns("kitId");
      Number newId = insert.executeAndReturnKey(params);
      kit.setKitId(newId.longValue());
    }
    else {
      params.addValue("kitId", kit.getKitId());
      NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);
      namedTemplate.update(KIT_UPDATE, params);
    }

    if (this.cascadeType != null && this.cascadeType.equals(CascadeType.PERSIST)) {
      if (!kit.getNotes().isEmpty()) {
        for (Note n : kit.getNotes()) {
          noteDAO.saveKitNote(kit, n);
        }
      }
    }
    return kit.getKitId();
  }

  public class LazyKitMapper implements RowMapper<Kit> {
    public Kit mapRow(ResultSet rs, int rowNum) throws SQLException {
      Kit kit = null;

      try {
        KitDescriptor kd = getKitDescriptorById(rs.getInt("kitDescriptorId"));
        KitType kitType = kd.getKitType();

        if (kitType.equals(KitType.CLUSTERING)) {
          kit = new ClusterKit();
        }
        else if (kitType.equals(KitType.EMPCR)) {
          kit = new EmPcrKit();
        }
        else if (kitType.equals(KitType.LIBRARY)) {
          kit = new LibraryKit();
        }
        else if (kitType.equals(KitType.MULTIPLEXING)) {
          kit = new MultiplexingKit();
        }
        else if (kitType.equals(KitType.SEQUENCING)) {
          kit = new SequencingKit();
        }
        else {
          throw new SQLException("Unsupported KitType: "+kitType.getKey());
        }

        kit.setKitId(rs.getLong("kitId"));
        kit.setIdentificationBarcode(rs.getString("identificationBarcode"));
        kit.setLocationBarcode(rs.getString("locationBarcode"));
        kit.setLotNumber(rs.getString("lotNumber"));
        kit.setKitDate(rs.getDate("kitDate"));
        kit.setKitDescriptor(kd);
        kit.setNotes(noteDAO.listByKit(rs.getLong("kitId")));
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      return kit;
    }
  }

  public class KitMapper implements RowMapper<Kit> {
    public Kit mapRow(ResultSet rs, int rowNum) throws SQLException {
      Kit kit = null;

      try {
        KitDescriptor kd = getKitDescriptorById(rs.getInt("kitDescriptorId"));
        KitType kitType = kd.getKitType();

        if (kitType.equals(KitType.CLUSTERING)) {
          kit = new ClusterKit();
        }
        else if (kitType.equals(KitType.EMPCR)) {
          kit = new EmPcrKit();
        }
        else if (kitType.equals(KitType.LIBRARY)) {
          kit = new LibraryKit();
        }
        else if (kitType.equals(KitType.MULTIPLEXING)) {
          kit = new MultiplexingKit();
        }
        else if (kitType.equals(KitType.SEQUENCING)) {
          kit = new SequencingKit();
        }
        else {
          throw new SQLException("Unsupported KitType: "+kitType.getKey());
        }

        kit.setKitId(rs.getLong("kitId"));
        kit.setIdentificationBarcode(rs.getString("identificationBarcode"));
        kit.setLocationBarcode(rs.getString("locationBarcode"));
        kit.setLotNumber(rs.getString("lotNumber"));
        kit.setKitDate(rs.getDate("kitDate"));
        kit.setKitDescriptor(kd);
        kit.setNotes(noteDAO.listByKit(rs.getLong("kitId")));
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      return kit;
    }
  }

  public KitDescriptor getKitDescriptorById(long id) throws IOException {
    List eResults = template.query(KIT_DESCRIPTOR_SELECT_BY_ID, new Object[]{id}, new KitDescriptorMapper());
    return eResults.size() > 0 ? (KitDescriptor) eResults.get(0) : null;
  }

  public KitDescriptor getKitDescriptorByPartNumber(String partNumber) throws IOException {
    List eResults = template.query(KIT_DESCRIPTOR_SELECT_BY_PART_NUMBER, new Object[]{partNumber}, new KitDescriptorMapper());
    return eResults.size() > 0 ? (KitDescriptor) eResults.get(0) : null;
  }

  public List<KitDescriptor> listAllKitDescriptors() throws IOException {
    return template.query(KIT_DESCRIPTORS_SELECT, new KitDescriptorMapper());
  }
  
  public List<KitDescriptor> listKitDescriptorsByType(KitType kitType) throws IOException {
    return template.query(KIT_DESCRIPTORS_SELECT_BY_TYPE, new Object[]{kitType.getKey()}, new KitDescriptorMapper());
  }

  public List<KitDescriptor> listKitDescriptorsByPlatform(PlatformType platformType) throws IOException {
    return template.query(KIT_DESCRIPTORS_SELECT_BY_PLATFORM, new Object[]{platformType}, new KitDescriptorMapper());
  }
  
  public long saveKitDescriptor(KitDescriptor kd) throws IOException {
    log.info("Saving " + kd.toString() + " : " + kd.getKitType() + " : " + kd.getPlatformType());
    MapSqlParameterSource params = new MapSqlParameterSource();

    params.addValue("name", kd.getName())
            .addValue("version", kd.getVersion())
            .addValue("manufacturer", kd.getManufacturer())
            .addValue("partNumber", kd.getPartNumber())
            .addValue("stockLevel", kd.getStockLevel())
            .addValue("kitType", kd.getKitType().getKey())
            .addValue("platformType", kd.getPlatformType().getKey());

    if (kd.getKitDescriptorId() == KitDescriptor.UNSAVED_ID) {
      SimpleJdbcInsert insert = new SimpleJdbcInsert(template)
                            .withTableName("KitDescriptor")
                            .usingGeneratedKeyColumns("kitDescriptorId");
      Number newId = insert.executeAndReturnKey(params);
      kd.setKitDescriptorId(newId.longValue());
    }
    else {
      params.addValue("kitDescriptorId", kd.getKitDescriptorId());
      NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);
      namedTemplate.update(KIT_DESCRIPTOR_UPDATE, params);
    }

    return kd.getKitDescriptorId();
  }

  public class KitDescriptorMapper implements RowMapper<KitDescriptor> {
    public KitDescriptor mapRow(ResultSet rs, int rowNum) throws SQLException {
      KitDescriptor kd = new KitDescriptor();
      kd.setKitDescriptorId(rs.getLong("kitDescriptorId"));
      kd.setName(rs.getString("name"));
      kd.setVersion(rs.getDouble("version"));
      kd.setManufacturer(rs.getString("manufacturer"));
      kd.setPartNumber(rs.getString("partNumber"));
      kd.setStockLevel(rs.getInt("stockLevel"));
      
      kd.setKitType(KitType.get(rs.getString("kitType")));

      log.info("Set kit type for descriptor " + kd.getKitDescriptorId() + " to " + kd.getKitType());

      kd.setPlatformType(PlatformType.get(rs.getString("platformType")));
      
      log.info("Set platform type for descriptor " + kd.getKitDescriptorId() + " to " + kd.getPlatformType());
      return kd;
    }
  }
}
