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

package uk.ac.bbsrc.tgac.miso.core.service.submission;

import uk.ac.bbsrc.tgac.miso.core.data.SequencerPoolPartition;
import uk.ac.bbsrc.tgac.miso.core.data.impl.LibraryDilution;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: collesa
 * Date: 04/04/12
 * Time: 15:15
 * To change this template use File | Settings | File Templates.
 */
public class FakeFilepathGenerator implements FilePathGenerator{
    //returns a HashSet containing 3 files on the local drive.
    @Override
    public File generateFilePath(SequencerPoolPartition partition, LibraryDilution libraryDilution) {
        File datafile = new File("/storage/miso/datafiles/datafile1.dat");
        return datafile;
    }

    @Override
    public Set<File> generateFilePaths(SequencerPoolPartition partition) {

        Set<File> dataFiles = new HashSet<File>();
        for(int i=1;i<=3;i++){
            File datafile = new File("/storage/miso/datafiles/datafile"+i+".dat");
            dataFiles.add(datafile);
        }
        return dataFiles;
    }
}
