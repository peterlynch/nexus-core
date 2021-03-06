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
package org.sonatype.nexus.proxy;

/**
 * Generic remote storage exception thrown by given storage implementation (more special than generic IOExceptions), and
 * so. Denotes a (probably) unrecoverable, serious system and/or IO error that needs some Core action to manage it.
 * 
 * @author cstamas
 */
public class RemoteStorageException
    extends StorageException
{
    private static final long serialVersionUID = 6487865845745424470L;

    public RemoteStorageException( String msg )
    {
        super( msg );
    }

    public RemoteStorageException( String msg, Throwable cause )
    {
        super( msg, cause );
    }

    public RemoteStorageException( Throwable cause )
    {
        super( cause.getMessage(), cause );
    }
}
