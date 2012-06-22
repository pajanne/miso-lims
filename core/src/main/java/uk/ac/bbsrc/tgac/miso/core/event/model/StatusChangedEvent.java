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

package uk.ac.bbsrc.tgac.miso.core.event.model;

import uk.ac.bbsrc.tgac.miso.core.data.Status;
import uk.ac.bbsrc.tgac.miso.core.event.Event;
import uk.ac.bbsrc.tgac.miso.core.event.type.MisoEventType;

/**
 * uk.ac.bbsrc.tgac.miso.core.event
 * <p/>
 * Info
 *
 * @author Rob Davey
 * @date 26/09/11
 * @since 0.1.2
 */
public class StatusChangedEvent<T> implements Event {
  private T o;
  private Status currentStatus;

  public StatusChangedEvent(T o, Status s) {
    this.o = o;
    this.currentStatus = s;
  }

  public T getEventObject() {
    return o;
  }

  public Status getStatus() {
    return currentStatus;
  }

  @Override
  public String getEventMessage() {
    return "Status changed: " + currentStatus.getHealth();
  }

  @Override
  public MisoEventType getEventType() {
    return MisoEventType.STATUS_CHANGED_EVENT;
  }
}