package org.sonatype.nexus.integrationtests.nexus1329;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.test.utils.FileTestingUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public abstract class Nexus1329AbstractCheckOnlyOneMirror
    extends AbstractMirrorIT
{
    /**
     * Nexus should try only the first mirror server
     */
    @Test
    public void checkSingleMirror()
        throws Exception
    {
        beforeCheck();
        
        File content = getTestFile( "basic" );

        server.addServer( "repository", content );
        List<String> mirror1Urls = server.addServer( "mirror1", HttpServletResponse.SC_NOT_FOUND );
        List<String> mirror2Urls = server.addServer( "mirror2", HttpServletResponse.SC_NOT_FOUND );

        server.start();

        Gav gav =
            new Gav( "nexus1329", "sample", "1.0.0", null, "xml", null, null, null, false, false, null, false, null );

        File artifactFile = this.downloadArtifactFromRepository( REPO, gav, "./target/downloads/nexus1329" );

        File originalFile = this.getTestFile( "basic/nexus1329/sample/1.0.0/sample-1.0.0.xml" );
        Assert.assertTrue( FileTestingUtils.compareFileSHA1s( originalFile, artifactFile ) );

        Assert.assertTrue( mirror1Urls.size() > 0, "Nexus should access first mirror " + mirror1Urls );
        Assert.assertTrue( mirror2Urls.isEmpty(), "Nexus should not access second mirror " + mirror2Urls );
    }
    
    protected void beforeCheck()
        throws Exception
    {
    }
}
