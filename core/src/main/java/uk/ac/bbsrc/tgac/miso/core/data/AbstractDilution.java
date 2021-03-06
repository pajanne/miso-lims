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

package uk.ac.bbsrc.tgac.miso.core.data;

import com.eaglegenomics.simlims.core.SecurityProfile;
import com.eaglegenomics.simlims.core.User;
import uk.ac.bbsrc.tgac.miso.core.security.SecurableByProfile;

import javax.persistence.*;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Skeleton implementation of a Dilution
 *
 * @author Rob Davey
 * @since 0.0.2
 */
public abstract class AbstractDilution implements Dilution, Comparable {
  public static final Long UNSAVED_ID = null;

  @OneToOne(cascade = CascadeType.ALL)
  private SecurityProfile securityProfile;
  
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long dilutionId = AbstractDilution.UNSAVED_ID;
  private String name;
  private Date creationDate;
  private Double concentration;
  private String identificationBarcode;
  private String dilutionUserName;

  public Long getDilutionId() {
    return this.dilutionId;
  }

  public void setDilutionId(Long dilutionId) {
    this.dilutionId = dilutionId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDilutionCreator() {
    return dilutionUserName;
  }

  public void setDilutionCreator(String dilutionUserName) {
    this.dilutionUserName = dilutionUserName;
  }

  public Date getCreationDate() {
    return this.creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public Double getConcentration() {
    return this.concentration;
  }

  public void setConcentration(Double concentration) {
    this.concentration = concentration;
  }

  public String getIdentificationBarcode() {
    return this.identificationBarcode;
  }

  public void setIdentificationBarcode(String identificationBarcode) {
    this.identificationBarcode = identificationBarcode;
  }

  public String getLabelText() {
    return getLibrary().getAlias();
  }

  @Override
  public Collection<? extends Dilution> getInternalPoolableElements() {
    Set<Dilution> dil = new HashSet<Dilution>();
    dil.add(this);
    return dil;
  }

  public SecurityProfile getSecurityProfile() {
    return securityProfile;
  }

  public void setSecurityProfile(SecurityProfile profile) {
    this.securityProfile = profile;
  }

  public void inheritPermissions(SecurableByProfile parent) throws SecurityException {
    setSecurityProfile(parent.getSecurityProfile());
  }

  public boolean userCanRead(User user) {
    return securityProfile.userCanRead(user);
  }

  public boolean userCanWrite(User user) {
    return securityProfile.userCanWrite(user);
  }

  public boolean isDeletable() {
    return getDilutionId() != AbstractDilution.UNSAVED_ID;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getDilutionId());
    sb.append(" : ");
    sb.append(getName());
    sb.append(" : ");
    sb.append(getCreationDate());
    sb.append(" : ");
    sb.append(getConcentration());
    return sb.toString();
  }

  /**
   * Equivalency is based on getProjectId() if set, otherwise on name,
   * description and creation date.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (obj == this)
      return true;
    if (!(obj instanceof TagBarcode))
      return false;
    Dilution them = (Dilution) obj;
    // If not saved, then compare resolved actual objects. Otherwise
    // just compare IDs.
    if (getDilutionId() == AbstractDilution.UNSAVED_ID
        || them.getDilutionId() == AbstractDilution.UNSAVED_ID) {
      return getName().equals(them.getName()) &&
             getDilutionCreator().equals(them.getDilutionCreator()) &&
             getConcentration().equals(them.getConcentration());
    }
    else {
      return getDilutionId().longValue() == them.getDilutionId().longValue();
    }
  }

  @Override
  public int hashCode() {
    if (getDilutionId() != AbstractDilution.UNSAVED_ID) {
      return getDilutionId().intValue();
    }
    else {
      final int PRIME = 37;
      int hashcode = 1;
      if (getName() != null) hashcode = PRIME * hashcode + getName().hashCode();
      if (getDilutionCreator() != null) hashcode = PRIME * hashcode + getDilutionCreator().hashCode();
      if (getConcentration() != null) hashcode = PRIME * hashcode + getConcentration().hashCode();
      return hashcode;
    }
  }

  @Override
  public int compareTo(Object o) {
    Dilution t = (Dilution)o;
    if (getDilutionId() < t.getDilutionId()) return -1;
    if (getDilutionId() > t.getDilutionId()) return 1;
    return 0;
  }
}
