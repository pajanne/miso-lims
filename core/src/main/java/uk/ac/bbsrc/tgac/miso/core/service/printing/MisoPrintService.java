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

package uk.ac.bbsrc.tgac.miso.core.service.printing;

import net.sourceforge.fluxion.spi.Spi;
import uk.ac.bbsrc.tgac.miso.core.data.Barcodable;
import uk.ac.bbsrc.tgac.miso.core.factory.barcode.BarcodeLabelFactory;
import uk.ac.bbsrc.tgac.miso.core.service.printing.context.PrintContext;

import java.io.File;
import java.io.IOException;

/**
 * uk.ac.bbsrc.tgac.miso.core.service.printing
 * <p/>
 * Info
 *
 * @author Rob Davey
 * @date 30-Jun-2011
 * @since 0.0.3
 */
@Spi
public interface MisoPrintService<T, C extends PrintContext<T>> {
  long getServiceId();
  void setServiceId(long serviceId);
  String getName();
  void setName(String name);
  boolean isEnabled();
  void setEnabled(boolean enabled);
  C getPrintContext();
  void setPrintContext(C pc);
  public BarcodeLabelFactory<T> getBarcodeLabelFactory();
  public void setBarcodeLabelFactory(BarcodeLabelFactory<T> barcodeLabelFactory);
  boolean print(T content) throws IOException;
  void setPrintServiceFor(Class<? extends Barcodable> c);
  Class<? extends Barcodable> getPrintServiceFor();
  public T getLabelFor(Barcodable b);
}
