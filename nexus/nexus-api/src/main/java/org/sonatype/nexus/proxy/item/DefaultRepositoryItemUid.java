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
package org.sonatype.nexus.proxy.item;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * The Class RepositoryItemUid. This class represents unique and constant label of all items/files originating from a
 * Repository, thus backed by some storage (eg. Filesystem).
 */
public class DefaultRepositoryItemUid
    implements RepositoryItemUid
{
    private static enum LockStep
    {
        READ( true ), WRITE( true ), READ_WRITE_UPGRADE( true ), READ_AS_BEFORE( false ), WRITE_AS_BEFORE( false );

        private final boolean mustAct;

        private LockStep( boolean mustAct )
        {
            this.mustAct = mustAct;
        }

        public boolean isMustAct()
        {
            return mustAct;
        }

        public boolean isReadLockLastLocked()
        {
            return READ.equals( this ) || READ_AS_BEFORE.equals( this );
        }

        public LockStep getFollowingLockstep( Action action )
        {
            // sanity check, "action" and "this" must be aligned:
            // reading actions may be with both locks (because WRITE will be not downgraded)
            // writing actions may be with WRITE_* and upgrade steps only
            if ( !action.isReadAction() )
            {
                // write action can be together with LockSteps: WRITE, WRITE_AS_BEFORE, READ_WRITE_UPGRADE
                switch ( this )
                {
                    case WRITE:
                    case WRITE_AS_BEFORE:
                    case READ_WRITE_UPGRADE:
                        break;

                    default:
                        throw new IllegalStateException( "Illegal combination of action \"" + action.toString()
                            + "\" with lockstep " + this.toString() );
                }
            }

            // return the appropriate "same as" step
            switch ( this )
            {
                case READ:
                case READ_AS_BEFORE:

                    return READ_AS_BEFORE;

                case WRITE:
                case WRITE_AS_BEFORE:
                case READ_WRITE_UPGRADE:

                    return WRITE_AS_BEFORE;

                default:
                    throw new IllegalStateException( "Unknown lockstep " + this.toString() );
            }
        }
    }

    private static final ThreadLocal<Map<String, Stack<LockStep>>> threadCtx =
        new ThreadLocal<Map<String, Stack<LockStep>>>()
        {
            @Override
            protected synchronized Map<String, Stack<LockStep>> initialValue()
            {
                return new HashMap<String, Stack<LockStep>>();
            };
        };

    private final RepositoryItemUidFactory factory;

    private final ReentrantReadWriteLock contentLock;

    private final ReentrantReadWriteLock attributesLock;

    /** The repository. */
    private final Repository repository;

    /** The path. */
    private final String path;

    protected DefaultRepositoryItemUid( RepositoryItemUidFactory factory, Repository repository, String path )
    {
        super();

        this.factory = factory;

        this.contentLock = new ReentrantReadWriteLock();

        this.attributesLock = new ReentrantReadWriteLock();

        this.repository = repository;

        this.path = path;
    }

    public RepositoryItemUidFactory getRepositoryItemUidFactory()
    {
        return factory;
    }

    public Repository getRepository()
    {
        return repository;
    }

    public String getPath()
    {
        return path;
    }

    public void lock( Action action )
    {
        doLock( action, getLockKey(), contentLock );
    }

    public void unlock()
    {
        doUnlock( null, getLockKey(), contentLock );
    }

    public void lockAttributes( Action action )
    {
        doLock( action, getAttributeLockKey(), attributesLock );
    }

    public void unlockAttributes()
    {
        doUnlock( null, getAttributeLockKey(), attributesLock );
    }

    /**
     * toString() will return a "string representation" of this UID in form of repoId + ":" + path
     */
    @Override
    public String toString()
    {
        return getRepository().getId() + ":" + getPath();
    }

    public String toDebugString()
    {
        return getRepository().getId() + ":" + getPath() + " (" + super.toString() + ")";
    }

    // ==

    protected LockStep getLastStep( String lockKey )
    {
        Map<String, Stack<LockStep>> threadMap = threadCtx.get();

        if ( !threadMap.containsKey( lockKey ) )
        {
            return null;
        }
        else
        {
            try
            {
                return threadMap.get( lockKey ).peek();
            }
            catch ( EmptyStackException e )
            {
                return null;
            }
        }
    }

    protected void putLastStep( String lockKey, LockStep lock )
    {
        Map<String, Stack<LockStep>> threadMap = threadCtx.get();

        if ( lock != null )
        {
            if ( !threadMap.containsKey( lockKey ) )
            {
                threadMap.put( lockKey, new Stack<LockStep>() );
            }

            threadMap.get( lockKey ).push( lock );
        }
        else
        {
            Stack<LockStep> stack = threadMap.get( lockKey );

            stack.pop();

            // cleanup if stack is empty
            if ( stack.isEmpty() )
            {
                threadMap.remove( lockKey );
            }
        }
    }

    protected void doLock( Action action, String lockKey, ReentrantReadWriteLock rwLock )
    {
        // we always go from "weaker" to "stronger" lock (read is shared, while write is exclusive lock)
        // because of Nexus nature (wrong nature?), the calls are heavily boxed, hence a thread once acquired write
        // may re-lock with read action. In this case, we keep the write lock, since it is "stronger"
        // The proper downgrade of locks happens in unlock method, while unraveling the stack of locking steps.
        LockStep step = getLastStep( lockKey );

        if ( step != null && step.isReadLockLastLocked() && !action.isReadAction() )
        {
            // we need lock upgrade (r->w)
            // java5+ does not supports this direction, do it "risky" way
            try
            {
                getActionLock( rwLock, true ).unlock();
            }
            catch ( IllegalMonitorStateException e )
            {
                // increasing the details level
                IllegalMonitorStateException ie =
                    new IllegalMonitorStateException( "Unable to upgrade lock for: '" + lockKey + "' on "
                        + this.toString() + " caused by: " + e.getMessage() );
                ie.initCause( e );
                throw ie;
            }

            getActionLock( rwLock, false ).lock();

            step = LockStep.READ_WRITE_UPGRADE;
        }
        else if ( step == null )
        {
            // just lock it, this is first timer
            getActionLock( rwLock, action.isReadAction() ).lock();

            step = action.isReadAction() ? LockStep.READ : LockStep.WRITE;
        }
        else
        {
            // not a first timer (we already have some lock) and no lock upgrade needed, hence we have the proper lock
            // already, just do nothing but note the fact in Stack

            // just DO NOT lock it, we already own the needed lock, and upgrade/dowgrade
            // becomes unmaneagable if we have reentrant locks!
            //
            // example code (the call tree actually, this code may be in multiple, even recursive calls):
            //
            // lock(read);
            // ...
            // lock(read);
            // ...
            // lock(write); <- This call will stumble and lockup on itself, see above about upgrade
            // ...
            // ...
            // unlock();
            // ...
            // unlock();
            // ...
            // unlock();

            step = step.getFollowingLockstep( action );
        }

        putLastStep( lockKey, step );
    }

    protected void doUnlock( Action action, String lockKey, ReentrantReadWriteLock rwLock )
    {
        LockStep step = getLastStep( lockKey );

        if ( step == null )
        {
            // this is error here
            throw new IllegalMonitorStateException( "UID \"" + toString()
                + "\" was tried to be unlocked but had no step-history..." );
        }

        if ( step.isMustAct() )
        {
            if ( LockStep.READ.equals( step ) )
            {
                getActionLock( rwLock, true ).unlock();
            }
            else if ( LockStep.WRITE.equals( step ) )
            {
                getActionLock( rwLock, false ).unlock();
            }
            else if ( LockStep.READ_WRITE_UPGRADE.equals( step ) )
            {
                // now we need to downgrade (w->r)
                // java5+ supports this direction, do it by the "book"
                getActionLock( rwLock, true ).lock();

                getActionLock( rwLock, false ).unlock();
            }
        }

        putLastStep( lockKey, null );
    }

    private Lock getActionLock( ReadWriteLock rwLock, boolean isReadAction )
    {
        if ( isReadAction )
        {
            return rwLock.readLock();
        }
        else
        {
            return rwLock.writeLock();
        }
    }

    private String getLockKey()
    {
        return toString() + " : itemlock";
    }

    private String getAttributeLockKey()
    {
        return toString() + " : attrlock";
    }

    public boolean isHidden()
    {
        // paths that start with a . in any directory (or filename)
        // are considered hidden.
        // This check will catch (for example):
        // .metadata
        // /.meta/something.jar
        // /something/else/.hidden/something.jar
        if ( getPath() != null && ( getPath().indexOf( "/." ) > -1 || getPath().startsWith( "." ) ) )
        {
            return true;
        }

        return false;
    }

    // for Debug/tests vvv

    protected ReentrantReadWriteLock getContentLock()
    {
        return contentLock;
    }

    protected ReentrantReadWriteLock getAttributesLock()
    {
        return attributesLock;
    }

    // for Debug/tests ^^^

}