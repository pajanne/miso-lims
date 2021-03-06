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
import org.w3c.dom.Document;
import uk.ac.bbsrc.tgac.miso.core.data.visitor.SubmittableVisitor;
import uk.ac.bbsrc.tgac.miso.core.exception.MalformedExperimentException;
import uk.ac.bbsrc.tgac.miso.core.security.SecurableByProfile;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;

/**
 * Skeleton implementation of a Study
 *
 * @author Rob Davey
 * @since 0.0.2
 */
@Entity
@Table(name = "`Study`")
public abstract class AbstractStudy implements Study {
  public static final Long UNSAVED_ID = null;

  private static final long serialVersionUID = 1L;

  @ManyToOne(cascade = CascadeType.ALL)
  private Project project = null;

  @OneToMany(targetEntity = AbstractExperiment.class, cascade = CascadeType.ALL)
  private Collection<Experiment> experiments = new HashSet<Experiment>();

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long studyId = AbstractStudy.UNSAVED_ID;

  @Transient
  public Document submissionDocument;

  @OneToOne(cascade = CascadeType.ALL)
  private SecurityProfile securityProfile = null;

  @Column(name = "name", nullable = false)
  private String name;
  @Column(name = "description", nullable = false)
  private String description;
  @Column(name = "accession")
  private String accession;
  @Column(name = "studyType")
  private String studyType;
  @Column(name = "alias")
  private String alias;

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public Long getStudyId() {
    return studyId;
  }

  public void setStudyId(Long studyId) {
    this.studyId = studyId;
  }

  public String getAccession() {
    return accession;
  }

  public void setAccession(String accession) {
    this.accession = accession;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public String getStudyType() {
    return studyType;
  }

  public void setStudyType(String studyType) {
    this.studyType = studyType;
  }

  public void addExperiment(Experiment e) throws MalformedExperimentException {
    //do experiment validation

    //propagate security profiles down the hierarchy
    e.setSecurityProfile(this.securityProfile);

    //add
    this.experiments.add(e);
  }

  public Collection<Experiment> getExperiments() {
    return experiments;
  }

  public void setExperiments(Collection<Experiment> experiments) {
    this.experiments = experiments;
  }

  public boolean isDeletable() {
    return getStudyId() != AbstractStudy.UNSAVED_ID &&
           getExperiments().isEmpty();    
  }

  public abstract void buildSubmission();

  public SecurityProfile getSecurityProfile() {
    return securityProfile;
  }

  public void setSecurityProfile(SecurityProfile securityProfile) {
    this.securityProfile = securityProfile;
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
    if (!(obj instanceof Study))
      return false;
    Study them = (Study) obj;
    // If not saved, then compare resolved actual objects. Otherwise
    // just compare IDs.
    if (getStudyId() == AbstractStudy.UNSAVED_ID
        || them.getStudyId() == AbstractStudy.UNSAVED_ID) {
      if (getName() != null && them.getName() != null) {
        return getName().equals(them.getName());
      }
      else {
        return getAlias().equals(them.getAlias());
      }
    }
    else {
      return this.getStudyId().longValue() == them.getStudyId().longValue();
    }
  }

  @Override
  public int hashCode() {
    if (this.getStudyId() != AbstractStudy.UNSAVED_ID) {
      return this.getStudyId().intValue();
    }
    else {
      final int PRIME = 37;
      int hashcode = 1;
      if (getName() != null) hashcode = PRIME * hashcode + getName().hashCode();
      if (getAlias() != null) hashcode = PRIME * hashcode + getAlias().hashCode();
      //if (getDescription() != null) hashcode = 37 * hashcode + getDescription().hashCode();
      return hashcode;
    }
  }

  @Override
  public int compareTo(Object o) {
    Study t = (Study)o;
    if (getStudyId() < t.getStudyId()) return -1;
    if (getStudyId() > t.getStudyId()) return 1;
    return 0;
  }
}
