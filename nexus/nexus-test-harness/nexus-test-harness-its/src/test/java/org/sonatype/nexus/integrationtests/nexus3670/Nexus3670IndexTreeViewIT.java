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
package org.sonatype.nexus.integrationtests.nexus3670;

import static org.sonatype.nexus.integrationtests.ITGroups.INDEX;

import java.util.Collections;
import java.util.Set;

import org.sonatype.nexus.index.treeview.TreeNode;
import org.sonatype.nexus.index.treeview.TreeNode.Type;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.indextreeview.IndexBrowserTreeNode;
import org.sonatype.nexus.rest.indextreeview.IndexBrowserTreeViewResponseDTO;
import org.sonatype.nexus.rest.model.SearchNGResponse;
import org.sonatype.nexus.test.utils.EventInspectorsUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test Index tree view.
 */
public class Nexus3670IndexTreeViewIT
    extends AbstractNexusIntegrationTest
{
    @Override
    protected void runOnce()
        throws Exception
    {
        super.runOnce();

        // just making sure all tasks are finished
        TaskScheduleUtil.waitForAllTasksToStop();
        
        new EventInspectorsUtil( this ).waitForCalmPeriod();

        // this is just a "preflight", that all is there what we want, not a real test

        // groupId
        SearchNGResponse results = getSearchMessageUtil().searchNGFor( "nexus3670" );
        Assert.assertEquals( 7, results.getData().size() );
        // repoId
        Assert.assertEquals( results.getData().get( 0 ).getArtifactHits().get( 0 ).getRepositoryId(),
                             REPO_TEST_HARNESS_REPO, "Where got it deployed?" );
    }

    @Test(groups = INDEX)
    public void testTreeWithoutHint()
        throws Exception
    {
        IndexBrowserTreeViewResponseDTO response =
            getSearchMessageUtil().indexBrowserTreeView( REPO_TEST_HARNESS_REPO, "/" );

        Assert.assertEquals( response.getData().getChildren().size(), 1, "There is one \"nexus3670\" group!" );

        // this is the G node of the "nexus3670" groupId (note: on G nodes, only the path is filled, but not the GAV!)
        IndexBrowserTreeNode node = (IndexBrowserTreeNode) response.getData().getChildren().get( 0 );

        // check path (note leading and trailing slashes!)
        Assert.assertEquals( node.getPath(), "/nexus3670/", "The path does not correspond to group!" );

        // get one level deeper
        // but this path is also Group ID, hence response will contain whole tree!
        response = getSearchMessageUtil().indexBrowserTreeView( REPO_TEST_HARNESS_REPO, node.getPath() );

        Assert.assertEquals( response.getData().getChildren().size(), 4,
                             "There are four \"nexus3670\" artifacts in a group!" );

        // this is group node
        node = (IndexBrowserTreeNode) response.getData().getChildren().get( 0 );

        Assert.assertEquals( node.getChildren().size(), 3,
                             "There is three versions of \"nexus3670:known-artifact-a\" artifact!" );

        // get one child (V)
        node = (IndexBrowserTreeNode) node.getChildren().get( 0 );

        // check path (note leading and trailing slashes!)
        Assert.assertEquals( node.getType(), TreeNode.Type.V, "The path should be V node" );
    }

    @Test(groups = INDEX)
    public void testTreeWithHint()
        throws Exception
    {
        // here, we will omit the path (it is "root" anyway), but by giving G AND A hints, we actually end up with
        // complete subtree
        IndexBrowserTreeViewResponseDTO response =
            getSearchMessageUtil().indexBrowserTreeView( REPO_TEST_HARNESS_REPO, "nexus3670", "known-artifact-a" );

        int artifactCount = countNodes( response.getData(), Collections.singleton( TreeNode.Type.artifact ) );

        Assert.assertEquals( artifactCount, 3, "Total of 3 distinct artifacts here!" );
    }

    protected int countNodes( IndexBrowserTreeNode node, Set<Type> types )
    {
        int result = types.contains( node.getType() ) ? 1 : 0;

        if ( !node.isLeaf() )
        {
            for ( TreeNode child : node.getChildren() )
            {
                result = result + countNodes( (IndexBrowserTreeNode) child, types );
            }
        }

        return result;
    }
}
