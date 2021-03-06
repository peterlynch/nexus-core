/**
 * Sonatype Nexus (TM) Open Source Version.
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://nexus.sonatype.org/dev/attributions.html
 * This program is licensed to you under Version 3 only of the GNU General Public License as published by the Free Software Foundation.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License Version 3 for more details.
 * You should have received a copy of the GNU General Public License Version 3 along with this program.
 * If not, see http://www.gnu.org/licenses/.
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc.
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */
package org.sonatype.nexus.integrationtests.nexus634;

import java.io.File;
import java.util.Collection;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test SnapshotRemoverTask to remove old artifacts but keep updated artifacts
 * @author marvin
 */
public class Nexus634KeepTwoSnapshotsIT
    extends AbstractSnapshotRemoverIT
{

    @Test
    public void keepTwoSnapshots()
        throws Exception
    {

        // This is THE important part
        runSnapshotRemover( "repo_nexus-test-harness-snapshot-repo", 2, 0, true );

        Collection<File> jars = listFiles( artifactFolder, new String[] { "jar" }, false );
        Assert.assertEquals( jars.size(), 2, "SnapshotRemoverTask should remove only old artifacts" );
    }

}
