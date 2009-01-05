package org.sonatype.nexus.proxy.repository;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.feeds.FeedRecorder;
import org.sonatype.nexus.feeds.NexusArtifactEvent;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.RemoteAccessDeniedException;
import org.sonatype.nexus.proxy.RemoteAccessException;
import org.sonatype.nexus.proxy.RemoteAuthenticationNeededException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.events.RepositoryEventProxyModeChanged;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCache;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

/**
 * Adds the proxying capability to a simple repository. The proxying will happen only if reposiory has remote storage!
 * So, this implementation is used in both "simple" repository cases: hosted and proxy, but in 1st case there is no
 * remote storage.
 * 
 * @author cstamas
 */
public abstract class AbstractProxyRepository
    extends AbstractRepository
    implements ProxyRepository
{
    /** The time while we do NOT check an already known remote status: 5 mins */
    protected static final long REMOTE_STATUS_RETAIN_TIME = 5L * 60L * 1000L;

    protected static final int DOWNLOAD_RETRY_COUNT = 2;

    private static final ExecutorService exec = Executors.newCachedThreadPool();

    /**
     * Feed recorder.
     */
    @Requirement
    private FeedRecorder feedRecorder;

    /** The proxy mode */
    private volatile ProxyMode proxyMode = ProxyMode.ALLOW;

    /** The proxy remote status */
    private volatile RemoteStatus remoteStatus = RemoteStatus.UNKNOWN;

    /** The repo status check mode */
    private volatile RepositoryStatusCheckMode repositoryStatusCheckMode = RepositoryStatusCheckMode.NEVER;

    /** Last time remote status was updated */
    private volatile long remoteStatusUpdated = 0;

    /** The remote storage. */
    private RemoteRepositoryStorage remoteStorage;

    /** The remote url. */
    private String remoteUrl;

    /** Remote storage context to store connection configs. */
    private RemoteStorageContext remoteStorageContext;

    /**
     * The item max age.
     */
    private int itemMaxAge = 24 * 60;

    protected void resetRemoteStatus()
    {
        remoteStatusUpdated = 0;
    }

    protected boolean isRemoteStorageReachable()
        throws StorageException,
            RemoteAuthenticationNeededException,
            RemoteAccessDeniedException
    {
        try
        {
            // TODO: include context? from where?
            return getRemoteStorage().isReachable( this, null );
        }
        catch ( RemoteAccessException ex )
        {
            getLogger().warn(
                "RemoteStorage of repository " + getId()
                    + " throws RemoteAccessException. Please set up authorization information for repository ID='"
                    + this.getId()
                    + "'. Setting ProxyMode of this repository to BlockedAuto. MANUAL INTERVENTION NEEDED.",
                ex );

            autoBlockProxying( ex );

            return false;
        }
    }

    /** Is checking in progress? */
    private volatile boolean _remoteStatusChecking = false;

    public RemoteStatus getRemoteStatus( boolean forceCheck )
    {
        // if the last known status is old, simply reset it
        if ( forceCheck || System.currentTimeMillis() - remoteStatusUpdated > REMOTE_STATUS_RETAIN_TIME )
        {
            remoteStatus = RemoteStatus.UNKNOWN;
        }

        if ( getProxyMode() != null && getProxyMode().shouldCheckRemoteStatus()
            && RemoteStatus.UNKNOWN.equals( remoteStatus ) && !_remoteStatusChecking )
        {
            // check for thread and go check it
            _remoteStatusChecking = true;

            exec.submit( new Callable<Object>()
            {
                public Object call()
                    throws Exception
                {
                    try
                    {
                        try
                        {
                            if ( isRemoteStorageReachable() )
                            {
                                setRemoteStatus( RemoteStatus.AVAILABLE, null );
                            }
                            else
                            {
                                setRemoteStatus( RemoteStatus.UNAVAILABLE, new ItemNotFoundException( "/" ) );
                            }
                        }
                        catch ( StorageException e )
                        {
                            setRemoteStatus( RemoteStatus.UNAVAILABLE, e );
                        }
                    }
                    finally
                    {
                        _remoteStatusChecking = false;
                    }

                    return null;
                }
            } );
        }
        else if ( getProxyMode() != null && !getProxyMode().shouldCheckRemoteStatus()
            && RemoteStatus.UNKNOWN.equals( remoteStatus ) && !_remoteStatusChecking )
        {
            setRemoteStatus( RemoteStatus.UNAVAILABLE, null );

            _remoteStatusChecking = false;
        }

        return remoteStatus;
    }

    private void setRemoteStatus( RemoteStatus remoteStatus, Throwable cause )
    {
        this.remoteStatus = remoteStatus;

        if ( RemoteStatus.AVAILABLE.equals( remoteStatus ) )
        {
            this.remoteStatusUpdated = System.currentTimeMillis();

            if ( getProxyMode() != null && getProxyMode().shouldAutoUnblock() )
            {
                setProxyMode( ProxyMode.ALLOW, true, cause );
            }
        }
        /*
         * AUTO_BLOCK temporarily disabled else if ( RemoteStatus.UNAVAILABLE.equals( remoteStatus ) ) {
         * this.remoteStatusUpdated = System.currentTimeMillis(); if ( getProxyMode() != null &&
         * getProxyMode().shouldProxy() ) { setProxyMode( ProxyMode.BLOCKED_AUTO, true, cause ); } }
         */
    }

    public ProxyMode getProxyMode()
    {
        if ( getRepositoryKind().isFacetAvailable( ProxyRepository.class ) )
        {
            return proxyMode;
        }
        else
        {
            return null;
        }
    }

    protected void setProxyMode( ProxyMode proxyMode, boolean sendNotification, Throwable cause )
    {
        ProxyMode oldProxyMode = this.proxyMode;

        this.proxyMode = proxyMode;

        // if this is proxy
        // and was !shouldProxy() and the new is shouldProxy()
        if ( this.proxyMode != null && this.proxyMode.shouldProxy() && !oldProxyMode.shouldProxy() )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "We have a !shouldProxy() -> shouldProxy() transition, purging NFC" );
            }

            getNotFoundCache().purge();

            resetRemoteStatus();
        }

        if ( sendNotification && !proxyMode.equals( oldProxyMode ) )
        {
            notifyProximityEventListeners( new RepositoryEventProxyModeChanged( this, oldProxyMode, proxyMode, cause ) );
        }
    }

    public void setProxyMode( ProxyMode proxyMode )
    {
        setProxyMode( proxyMode, true, null );
    }

    protected void autoBlockProxying( Throwable cause )
    {
        setRemoteStatus( RemoteStatus.UNAVAILABLE, cause );
    }

    public RepositoryStatusCheckMode getRepositoryStatusCheckMode()
    {
        return repositoryStatusCheckMode;
    }

    public void setRepositoryStatusCheckMode( RepositoryStatusCheckMode mode )
    {
        this.repositoryStatusCheckMode = mode;
    }

    public String getRemoteUrl()
    {
        return remoteUrl;
    }

    public void setRemoteUrl( String remoteUrl )
    {
        if ( remoteUrl == null )
        {
            this.remoteUrl = null;
        }
        else
        {
            String trstr = remoteUrl.trim();

            if ( !trstr.endsWith( RepositoryItemUid.PATH_SEPARATOR ) )
            {
                this.remoteUrl = trstr;
            }
            else
            {
                this.remoteUrl = trstr.substring( 0, trstr.length() - 1 );
            }
        }
    }

    public RemoteStorageContext getRemoteStorageContext()
    {
        return remoteStorageContext;
    }

    public void setRemoteStorageContext( RemoteStorageContext remoteStorageContext )
    {
        this.remoteStorageContext = remoteStorageContext;

        if ( getProxyMode() != null && getProxyMode().shouldAutoUnblock() )
        {
            // perm changes? retry if autoBlocked
            setProxyMode( ProxyMode.ALLOW );
        }
    }

    /**
     * Gets the item max age in (in minutes).
     * 
     * @return the item max age in (in minutes)
     */
    public int getItemMaxAge()
    {
        return itemMaxAge;
    }

    /**
     * Sets the item max age in (in minutes).
     * 
     * @param itemMaxAgeInSeconds the new item max age in (in minutes).
     */
    public void setItemMaxAge( int itemMaxAge )
    {
        this.itemMaxAge = itemMaxAge;
    }

    public RemoteRepositoryStorage getRemoteStorage()
    {
        return remoteStorage;
    }

    public void setRemoteStorage( RemoteRepositoryStorage remoteStorage )
    {
        this.remoteStorage = remoteStorage;

        setAllowWrite( remoteStorage == null );
    }

    protected AbstractStorageItem doCacheItem( AbstractStorageItem item )
        throws StorageException
    {
        if ( Boolean.TRUE.equals( item.getItemContext().get( CTX_TRANSITIVE_ITEM ) ) )
        {
            return item;
        }

        AbstractStorageItem result = null;

        try
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug(
                    "Caching item " + item.getRepositoryItemUid().toString() + " in local storage of repository." );
            }

            getLocalStorage().storeItem( this, item.getItemContext(), item );

            removeFromNotFoundCache( item.getRepositoryItemUid().getPath() );

            result = getLocalStorage()
                .retrieveItem( this, item.getItemContext(), item.getRepositoryItemUid().getPath() );

            notifyProximityEventListeners( new RepositoryItemEventCache( this, result ) );

            result.getItemContext().putAll( item.getItemContext() );
        }
        catch ( ItemNotFoundException ex )
        {
            // this is a nonsense, we just stored it!
            result = item;
        }
        catch ( UnsupportedStorageOperationException ex )
        {
            getLogger().warn( "LocalStorage does not handle STORE operation, not caching remote fetched item.", ex );

            result = item;
        }

        return result;
    }

    protected StorageItem doRetrieveItem( RepositoryItemUid uid, Map<String, Object> context )
        throws IllegalOperationException,
            ItemNotFoundException,
            StorageException
    {
        AbstractStorageItem item = null;
        AbstractStorageItem remoteItem = null;
        boolean localOnly = context != null && context.containsKey( ResourceStoreRequest.CTX_LOCAL_ONLY_FLAG )
            && Boolean.TRUE.equals( context.get( ResourceStoreRequest.CTX_LOCAL_ONLY_FLAG ) );

        AbstractStorageItem localItem = (AbstractStorageItem) super.doRetrieveItem( uid, context );

        boolean shouldProxy = true;

        for ( RequestProcessor processor : getRequestProcessors() )
        {
            shouldProxy = shouldProxy && processor.shouldProxy( this, uid, context );
        }

        if ( getProxyMode() != null && getProxyMode().shouldProxy() && !localOnly && shouldProxy )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "ProxyMode is " + getProxyMode().toString() );
            }

            // we are able to go remote
            if ( localItem == null || isOld( localItem ) )
            {
                // we should go remote coz we have no local copy or it is old
                try
                {
                    boolean shouldGetRemote = false;

                    if ( localItem != null )
                    {
                        if ( getLogger().isDebugEnabled() )
                        {
                            getLogger().debug(
                                "Item " + uid.toString() + " is old, checking for newer file on remote then local: "
                                    + new Date( localItem.getModified() ) );
                        }

                        // check is the remote newer than the local one
                        try
                        {
                            shouldGetRemote = getRemoteStorage().containsItem(
                                localItem.getModified(),
                                this,
                                context,
                                uid.getPath() );

                            if ( !shouldGetRemote )
                            {
                                markItemRemotelyChecked( uid, context );
                            }

                            if ( getLogger().isDebugEnabled() )
                            {
                                getLogger().debug(
                                    "Newer version of item " + uid.toString() + " is found on remote storage." );
                            }
                        }
                        catch ( RemoteAccessException ex )
                        {
                            getLogger()
                                .warn(
                                    "RemoteStorage of repository "
                                        + getId()
                                        + " throws RemoteAccessException. Please set up authorization information for repository ID='"
                                        + this.getId()
                                        + "'. Setting ProxyMode of this repository to BlockedAuto. MANUAL INTERVENTION NEEDED.",
                                    ex );

                            autoBlockProxying( ex );

                            // do not go remote, but we did not mark it as "remote checked" also.
                            // let the user do proper setup and probably it will try again
                            shouldGetRemote = false;
                        }
                    }
                    else
                    {
                        // we have no local copy of it, try to get it unconditionally
                        shouldGetRemote = true;
                    }

                    if ( shouldGetRemote )
                    {
                        // this will GET it unconditionally
                        try
                        {
                            ContentValidationResult result = null;

                            for ( int retry = 0; retry < DOWNLOAD_RETRY_COUNT; retry++ )
                            {
                                remoteItem = doRetrieveRemoteItem( uid, context );

                                remoteItem = doCacheItem( remoteItem );

                                result = doValidateRemoteItemContent( remoteItem, context );

                                if ( result == null || result.isContentValid() )
                                {
                                    break;
                                }
                            }

                            if ( result != null )
                            {
                                // send validation error/warning events
                                for ( NexusArtifactEvent event : result.getEvents() )
                                {
                                    feedRecorder.addNexusArtifactEvent( event );
                                }

                                if ( !result.isContentValid() )
                                {
                                    if ( getLogger().isDebugEnabled() )
                                    {
                                        getLogger().debug(
                                            "Item " + uid.toString() + " failed content integrity validation." );
                                    }

                                    getLocalStorage().retrieveItem( this, context, uid.getPath() );

                                    throw new ItemNotFoundException( uid );
                                }
                            }

                            if ( getLogger().isDebugEnabled() )
                            {
                                getLogger().debug( "Item " + uid.toString() + " found in remote storage." );
                            }
                        }
                        catch ( RemoteAccessException e )
                        {
                            remoteItem = null;
                        }
                        catch ( StorageException e )
                        {
                            remoteItem = null;

                            // cleanup if any remnant is here
                            try
                            {
                                deleteItem( uid, context );
                            }
                            catch ( ItemNotFoundException ex1 )
                            {
                                // ignore
                            }
                            catch ( UnsupportedStorageOperationException ex2 )
                            {
                                // will not happen
                            }
                        }
                    }
                    else
                    {
                        remoteItem = null;
                    }
                }
                catch ( ItemNotFoundException ex )
                {
                    if ( getLogger().isDebugEnabled() )
                    {
                        getLogger().debug( "Item " + uid.toString() + " not found in remote storage." );
                    }

                    remoteItem = null;
                }
            }

            if ( localItem == null && remoteItem == null )
            {
                // we dont have neither one, NotFoundException
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger()
                        .debug(
                            "Item "
                                + uid.toString()
                                + " does not exist in local storage neither in remote storage, throwing ItemNotFoundException." );
                }

                throw new ItemNotFoundException( uid );
            }
            else if ( localItem != null && remoteItem == null )
            {
                // simple: we have local but not remote (coz we are offline or coz it is not newer)
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug(
                        "Item " + uid.toString() + " does exist in local storage and is fresh, returning local one." );
                }

                item = localItem;
            }
            else
            {
                // the fact that remoteItem != null means we _have_ to return that one
                // OR: we had no local copy
                // OR: remoteItem is for sure newer (look above)
                item = remoteItem;
            }

        }
        else
        {
            // we cannot go remote
            if ( localItem != null )
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug(
                        "Item " + uid.toString() + " does exist locally and cannot go remote, returning local one." );
                }

                item = localItem;
            }
            else
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug(
                        "Item " + uid.toString()
                            + " does not exist locally and cannot go remote, throwing ItemNotFoundException." );
                }

                throw new ItemNotFoundException( uid );
            }
        }

        return item;
    }

    protected void markItemRemotelyChecked( RepositoryItemUid uid, Map<String, Object> context )
        throws StorageException,
            ItemNotFoundException
    {
        // remote file unchanged, touch the local one to renew it's Age
        getLocalStorage().touchItemRemoteChecked( this, context, uid.getPath() );
    }

    /**
     * Validates integrity of content of <code>item</code>.
     */
    protected ContentValidationResult doValidateRemoteItemContent( AbstractStorageItem item, Map<String, Object> context )
        throws RemoteAccessException,
            StorageException
    {
        return new ContentValidationResult();
    }

    protected AbstractStorageItem doRetrieveRemoteItem( RepositoryItemUid uid, Map<String, Object> context )
        throws ItemNotFoundException,
            RemoteAccessException,
            StorageException
    {
        AbstractStorageItem result = null;

        try
        {
            result = getRemoteStorage().retrieveItem( this, context, uid.getPath() );

            result.getItemContext().putAll( context );
        }
        catch ( RemoteAccessException ex )
        {
            getLogger().warn(
                "RemoteStorage of repository " + getId()
                    + " throws RemoteAccessException. Please set up authorization information for repository ID='"
                    + this.getId()
                    + "'. Setting ProxyMode of this repository to BlockedAuto. MANUAL INTERVENTION NEEDED.",
                ex );

            autoBlockProxying( ex );

            throw ex;
        }
        catch ( StorageException ex )
        {
            getLogger()
                .warn(
                    "RemoteStorage of repository "
                        + getId()
                        + " throws StorageException. Are we online? Is storage properly set up? Setting ProxyMode of this repository to BlockedAuto. MANUAL INTERVENTION NEEDED.",
                    ex );

            autoBlockProxying( ex );

            throw ex;
        }

        return result;
    }

    /**
     * Checks if item is old with "default" maxAge.
     * 
     * @param item the item
     * @return true, if is old
     */
    protected boolean isOld( StorageItem item )
    {
        return isOld( getItemMaxAge(), item );
    }

    /**
     * Checks if item is old with given maxAge.
     * 
     * @param maxAge
     * @param item
     * @return
     */
    protected boolean isOld( int maxAge, StorageItem item )
    {
        // if item is manually expired, true
        if ( item.isExpired() )
        {
            return true;
        }

        // a directory is not "aged"
        if ( StorageCollectionItem.class.isAssignableFrom( item.getClass() ) )
        {
            return false;
        }

        // if repo is non-expirable, false
        if ( maxAge < 1 )
        {
            return false;
        }
        // else check age
        else
        {
            return ( ( System.currentTimeMillis() - item.getRemoteChecked() ) > ( (long) maxAge * 60L * 1000L ) );
        }
    }

}