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
package org.sonatype.nexus.tasks;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.feeds.FeedRecorder;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.scheduling.AbstractNexusTask;
import org.sonatype.scheduling.SchedulerTask;

/**
 * Delete repository folders
 * 
 * @author Juven Xu
 */
@Component( role = SchedulerTask.class, hint = "DeleteRepositoryFoldersTask", instantiationStrategy = "per-lookup" )
public class DeleteRepositoryFoldersTask
    extends AbstractNexusTask<Object>
{
    private Repository repository;

    private boolean deleteForever = false;

    public Repository getRepository()
    {
        return repository;
    }

    public void setRepository( Repository repository )
    {
        this.repository = repository;
    }

    public boolean isDeleteForever()
    {
        return deleteForever;
    }

    public void setDeleteForever( boolean deleteForever )
    {
        this.deleteForever = deleteForever;
    }

    @Override
    public boolean isExposed()
    {
        return false;
    }

    @Override
    protected Object doRun()
        throws Exception
    {
        if ( repository != null )
        {
            getNexus().deleteRepositoryFolders( repository, deleteForever );
        }
        return null;
    }

    @Override
    protected String getAction()
    {
        return FeedRecorder.SYSTEM_REMOVE_REPO_FOLDER_ACTION;
    }

    @Override
    protected String getMessage()
    {
        if ( repository != null )
        {
            return "Deleting folders with repository ID: " + repository.getId();
        }
        return null;
    }

}
