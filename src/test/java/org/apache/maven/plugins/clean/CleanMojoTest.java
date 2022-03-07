package org.apache.maven.plugins.clean;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the clean mojo.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
@MojoTest
public class CleanMojoTest
{

    /**
     * Tests the simple removal of directories
     *
     * @throws Exception in case of an error.
     */
    @Test
    @InjectMojo( goal = "clean", pom = "classpath:/unit/basic-clean-test/plugin-pom.xml" )
    public void testBasicClean( CleanMojo mojo )
        throws Exception
    {
        assertNotNull( mojo );

        mojo.execute();

        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/" + "basic-clean-test/buildDirectory" ),
                     "Directory exists" );
        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/basic-clean-test/" + "buildOutputDirectory" ),
                    "Directory exists");
        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/basic-clean-test/" + "buildTestDirectory" ),
                    "Directory exists");
    }

    /**
     * Tests the removal of files and nested directories
     *
     * @throws Exception in case of an error.
     */
    @Test
    @InjectMojo( goal = "clean", pom = "classpath:/unit/nested-clean-test/plugin-pom.xml" )
    public void testCleanNestedStructure( CleanMojo mojo )
        throws Exception
    {
        assertNotNull( mojo );

        mojo.execute();

        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/nested-clean-test/target" ) );
        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/nested-clean-test/target/classes" ) );
        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/nested-clean-test/target/test-classes" ) );
    }

    /**
     * Tests that no exception is thrown when all internal variables are empty and that it doesn't
     * just remove whats there
     *
     * @throws Exception in case of an error.
     */
    @Test
    @InjectMojo( goal = "clean", pom = "classpath:/unit/basic-clean-test/plugin-pom.xml", empty = true )
    public void testCleanEmptyDirectories( CleanMojo mojo )
        throws Exception
    {
        assertNotNull( mojo );

        mojo.execute();

        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/empty-clean-test/testDirectoryStructure" ) );
        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/empty-clean-test/" + "testDirectoryStructure/file.txt" ) );
        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/empty-clean-test/" + "testDirectoryStructure/outputDirectory" ) );
        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/empty-clean-test/" + "testDirectoryStructure/outputDirectory/file.txt" ) );
    }

    /**
     * Tests the removal of files using fileset
     *
     * @throws Exception in case of an error.
     */
    @Test
    @InjectMojo( goal = "clean", pom = "classpath:/unit/fileset-clean-test/plugin-pom.xml", empty = true )
    public void testFilesetsClean( CleanMojo mojo )
        throws Exception
    {
        assertNotNull( mojo );

        mojo.execute();

        // fileset 1
        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/fileset-clean-test/target" ) );
        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/classes" ) );
        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/test-classes" ) );
        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/subdir" ) );
        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/classes/file.txt" ) );
        assertTrue( checkEmpty( getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/classes" ) );
        assertFalse( checkEmpty( getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/subdir" ) );
        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/fileset-clean-test/target/subdir/file.txt" ) );

        // fileset 2
        assertTrue( checkExists( getBasedir() + "/target/test-classes/unit/fileset-clean-test/"
            + "buildOutputDirectory" ) );
        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/fileset-clean-test/"
            + "buildOutputDirectory/file.txt" ) );
    }

    /**
     * Tests the removal of a directory as file
     *
     * @throws Exception in case of an error.
     */
    @Test
    @InjectMojo( goal = "clean", pom = "classpath:/unit/invalid-directory-test/plugin-pom.xml" )
    public void testCleanInvalidDirectory( CleanMojo mojo )
        throws Exception
    {
        assertNotNull( mojo );
        assertThrows( MojoException.class, mojo::execute, "Should fail to delete a file treated as a directory" );
    }

    /**
     * Tests the removal of a missing directory
     *
     * @throws Exception in case of an error.
     */
    @Test
    @InjectMojo( goal = "clean", pom = "classpath:/unit/missing-directory-test/plugin-pom.xml" )
    public void testMissingDirectory( CleanMojo mojo )
        throws Exception
    {
        assertNotNull( mojo );
        mojo.execute();
        assertFalse( checkExists( getBasedir() + "/target/test-classes/unit/missing-directory-test/does-not-exist" ) );
    }

    /**
     * Test the removal of a locked file on Windows systems.
     * <p>
     * Note: Unix systems doesn't lock any files.
     * </p>
     *
     * @throws Exception in case of an error.
     */
    @Test
    @EnabledOnOs( OS.WINDOWS )
    @InjectMojo( goal = "clean", pom = "classpath:/unit/locked-file-test/plugin-pom.xml" )
    public void testCleanLockedFile( CleanMojo mojo )
        throws Exception
    {
        assertNotNull( mojo );
        File f = new File( getBasedir(), "target/test-classes/unit/locked-file-test/buildDirectory/file.txt" );
        try ( FileChannel channel = new RandomAccessFile( f, "rw" ).getChannel();
              FileLock ignored = channel.lock() )
        {
            assertThrows( MojoException.class, mojo::execute, "Should fail to delete a file that is locked" );
        }
    }

    /**
     * Test the removal of a locked file on Windows systems.
     * <p>
     * Note: Unix systems doesn't lock any files.
     * </p>
     *
     * @throws Exception in case of an error.
     */
    @Test
    @EnabledOnOs( OS.WINDOWS )
    @InjectMojo( goal = "clean", pom = "classpath:/unit/locked-file-test/plugin-pom.xml" )
    public void testCleanLockedFileWithNoError( CleanMojo mojo )
        throws Exception
    {

        assertNotNull( mojo );
        setVariableValueToObject( mojo, "failOnError", Boolean.FALSE );

        File f = new File( getBasedir(), "target/test-classes/unit/locked-file-test/buildDirectory/file.txt" );
        try ( FileChannel channel = new RandomAccessFile( f, "rw" ).getChannel();
              FileLock ignored = channel.lock())
        {
            assertThrows( MojoException.class, mojo::execute, "Should display a warning when deleting a file that is locked" );
        }
    }

    /**
     * @param dir a dir or a file
     * @return true if a file/dir exists, false otherwise
     */
    private boolean checkExists( String dir )
    {
        return new File( new File( dir ).getAbsolutePath() ).exists();
    }

    /**
     * @param dir a directory
     * @return true if a dir is empty, false otherwise
     */
    private boolean checkEmpty( String dir )
    {
        File[] files = new File( dir ).listFiles();
        return files == null || files.length == 0;
    }
}
