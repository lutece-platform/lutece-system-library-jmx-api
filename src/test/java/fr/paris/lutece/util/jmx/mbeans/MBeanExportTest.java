/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.util.jmx.mbeans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import javax.management.Descriptor;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exports MBeans through the library contracts the way plugin-jmx does, on a private MBean server.
 */
public class MBeanExportTest
{
    private MBeanServer _server;

    /**
     * Standard MBean interface of the exported test bean.
     */
    public interface CounterMBean
    {
        /**
         * Returns the counter value.
         *
         * @return the value
         */
        int getValue( );
    }

    /**
     * Standard MBean exported by {@link CounterExporter}.
     */
    public static class Counter implements CounterMBean
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public int getValue( )
        {
            return 42;
        }
    }

    /**
     * MBeanExporter naming its bean under the Lutece domain.
     */
    private static class CounterExporter implements MBeanExporter
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public Object getMBean( )
        {
            return new Counter( );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMBeanName( )
        {
            return MBEAN_ROOT_NAME + "type=Counter";
        }
    }

    /**
     * Managed resource exposing a version attribute.
     */
    public static class VersionResource implements ManagedResource
    {
        private final String _strName;
        private final String _strVersion;

        /**
         * Builds a resource.
         *
         * @param strName
         *            the resource name
         * @param strVersion
         *            the version
         */
        public VersionResource( String strName, String strVersion )
        {
            _strName = strName;
            _strVersion = strVersion;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName( )
        {
            return ResourceExporter.MBEAN_ROOT_NAME + "type=Plugin,name=" + _strName;
        }

        /**
         * Returns the version.
         *
         * @return the version
         */
        public String getVersion( )
        {
            return _strVersion;
        }
    }

    /**
     * ResourceExporter describing {@link VersionResource} with a model MBean info.
     */
    private static class VersionExporter implements ResourceExporter
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public Iterable<ManagedResource> getMBeans( )
        {
            return List.of( new VersionResource( "alpha", "1.0.0" ), new VersionResource( "beta", "2.0.0" ) );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ModelMBeanInfo getMBeanInfo( )
        {
            Descriptor descriptor = new DescriptorSupport( new String [ ] {
                    "name=Version", "descriptorType=attribute", "getMethod=getVersion"
            } );
            ModelMBeanAttributeInfo [ ] attributes = {
                    new ModelMBeanAttributeInfo( "Version", "java.lang.String", "Version", true, false, false, descriptor )
            };
            ModelMBeanOperationInfo [ ] operations = {
                    new ModelMBeanOperationInfo( "getVersion", "getter", null, "java.lang.String", ModelMBeanOperationInfo.INFO )
            };

            return new ModelMBeanInfoSupport( VersionResource.class.getName( ), "Version", attributes, null, operations, null );
        }
    }

    /**
     * Creates a private MBean server.
     */
    @BeforeEach
    public void setUp( )
    {
        _server = MBeanServerFactory.newMBeanServer( );
    }

    /**
     * The root name is the Lutece JMX domain, the same through both exporter interfaces.
     *
     * @throws Exception
     *             if the name is not a valid object name
     */
    @Test
    public void testRootNameIsTheLuteceDomain( ) throws Exception
    {
        assertEquals( "Lutece:", MBeanConstants.MBEAN_ROOT_NAME );
        assertEquals( MBeanConstants.MBEAN_ROOT_NAME, MBeanExporter.MBEAN_ROOT_NAME );
        assertEquals( MBeanConstants.MBEAN_ROOT_NAME, ResourceExporter.MBEAN_ROOT_NAME );
        assertEquals( "Lutece", new ObjectName( MBeanConstants.MBEAN_ROOT_NAME + "type=Test" ).getDomain( ) );
    }

    /**
     * A standard MBean given by an MBeanExporter registers under its name and answers its attribute.
     *
     * @throws Exception
     *             if the registration fails
     */
    @Test
    public void testMBeanExporterRegistersItsBean( ) throws Exception
    {
        MBeanExporter exporter = new CounterExporter( );
        ObjectName name = new ObjectName( exporter.getMBeanName( ) );

        _server.registerMBean( exporter.getMBean( ), name );

        assertEquals( 42, _server.getAttribute( name, "Value" ) );
    }

    /**
     * Every resource of a ResourceExporter registers as a model MBean and answers its own attribute value.
     *
     * @throws Exception
     *             if the registration fails
     */
    @Test
    public void testResourceExporterRegistersEveryResource( ) throws Exception
    {
        ResourceExporter exporter = new VersionExporter( );

        for ( ManagedResource resource : exporter.getMBeans( ) )
        {
            RequiredModelMBean mbean = new RequiredModelMBean( exporter.getMBeanInfo( ) );
            mbean.setManagedResource( resource, "objectReference" );
            _server.registerMBean( mbean, new ObjectName( resource.getName( ) ) );
        }

        Set<ObjectName> names = _server.queryNames( new ObjectName( MBeanConstants.MBEAN_ROOT_NAME + "type=Plugin,*" ), null );
        assertEquals( 2, names.size( ) );
        assertEquals( "1.0.0", _server.getAttribute( new ObjectName( "Lutece:type=Plugin,name=alpha" ), "Version" ) );
        assertEquals( "2.0.0", _server.getAttribute( new ObjectName( "Lutece:type=Plugin,name=beta" ), "Version" ) );
        assertTrue( _server.isRegistered( new ObjectName( "Lutece:type=Plugin,name=alpha" ) ) );
    }
}
