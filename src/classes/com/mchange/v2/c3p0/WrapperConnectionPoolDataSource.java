/*
 * Distributed as part of c3p0 v.0.9.0.2
 *
 * Copyright (C) 2005 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.v2.c3p0;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.PropertyChangeListener;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.sql.*;
import javax.sql.*;
import com.mchange.v2.c3p0.impl.*;
import com.mchange.v2.log.*;

public final class WrapperConnectionPoolDataSource extends WrapperConnectionPoolDataSourceBase implements ConnectionPoolDataSource
{
    final static MLogger logger = MLog.getLogger( WrapperConnectionPoolDataSource.class );

    //MT: protected by this' lock
    ConnectionTester connectionTester = C3P0ImplUtils.defaultConnectionTester();

    {
	VetoableChangeListener setConnectionTesterListener = new VetoableChangeListener()
	    {
		// always called within synchronized mutators of the parent class... needn't explicitly sync here
		public void vetoableChange( PropertyChangeEvent evt ) throws PropertyVetoException
		{
		    Object val = evt.getNewValue();
		    try
			{
			    if ( "connectionTesterClassName".equals( evt.getPropertyName() ) )
				recreateConnectionTester( (String) val );
			}
		    catch ( Exception e )
			{
			    //e.printStackTrace();
			    if ( logger.isLoggable( MLevel.WARNING ) )
				 logger.log( MLevel.WARNING, "Failed to create ConnectionTester of class " + val, e );

			    throw new PropertyVetoException("Could not instantiate connection tester class with name '" + val + "'.", evt);
			}
		}
	    };
	this.addVetoableChangeListener( setConnectionTesterListener );

	C3P0Registry.register( this );
    }

    //implementation of javax.sql.ConnectionPoolDataSource
    public synchronized PooledConnection getPooledConnection()
	throws SQLException
    { 
	Connection conn = getNestedDataSource().getConnection();
	if ( this.isUsesTraditionalReflectiveProxies() )
	    {
		//return new C3P0PooledConnection( new com.mchange.v2.c3p0.test.CloseReportingConnection( conn ), 
		return new C3P0PooledConnection( conn, 
						 connectionTester,
						 this.isAutoCommitOnClose(), 
						 this.isForceIgnoreUnresolvedTransactions() ); 
	    }
	else
	    {
		return new NewPooledConnection( conn, 
						connectionTester,
						this.isAutoCommitOnClose(), 
						this.isForceIgnoreUnresolvedTransactions() ); 
	    }
    } 
 
    public synchronized PooledConnection getPooledConnection(String user, String password)
	throws SQLException
    { 
	Connection conn = getNestedDataSource().getConnection(user, password);
	if ( this.isUsesTraditionalReflectiveProxies() )
	    {
		//return new C3P0PooledConnection( new com.mchange.v2.c3p0.test.CloseReportingConnection( conn ), 
		return new C3P0PooledConnection( conn,
						 connectionTester,
						 this.isAutoCommitOnClose(), 
						 this.isForceIgnoreUnresolvedTransactions() ); 
	    }
	else
	    {
		return new NewPooledConnection( conn, 
						connectionTester,
						this.isAutoCommitOnClose(), 
						this.isForceIgnoreUnresolvedTransactions() ); 
	    }
    }
 
    public synchronized PrintWriter getLogWriter()
	throws SQLException
    { return getNestedDataSource().getLogWriter(); }

    public synchronized void setLogWriter(PrintWriter out)
	throws SQLException
    { getNestedDataSource().setLogWriter( out ); }

    public synchronized void setLoginTimeout(int seconds)
	throws SQLException
    { getNestedDataSource().setLoginTimeout( seconds ); }

    public synchronized int getLoginTimeout()
	throws SQLException
    { return getNestedDataSource().getLoginTimeout(); }

    //"virtual properties"
    public synchronized String getUser()
    { 
	try { return C3P0ImplUtils.findAuth( this.getNestedDataSource() ).getUser(); }
	catch (SQLException e)
	    {
		//e.printStackTrace();
		if ( logger.isLoggable( MLevel.WARNING ) )
		    logger.log( MLevel.WARNING, 
				"An Exception occurred while trying to find the 'user' property from our nested DataSource." +
				" Defaulting to no specified username.", e );
		return null; 
	    }
    }

    public synchronized String getPassword()
    { 
	try { return C3P0ImplUtils.findAuth( this.getNestedDataSource() ).getPassword(); }
	catch (SQLException e)
	    { 
		//e.printStackTrace();
		if ( logger.isLoggable( MLevel.WARNING ) )
		    logger.log( MLevel.WARNING, "An Exception occurred while trying to find the 'password' property from our nested DataSource." + 
				" Defaulting to no specified password.", e );
		return null; 
	    }
    }

    //other code
    private synchronized void recreateConnectionTester(String className) throws Exception
    {
	if (className != null)
	    {
		ConnectionTester ct = (ConnectionTester) Class.forName( className ).newInstance();
		this.connectionTester = ct;
	    }
	else
	    this.connectionTester = C3P0ImplUtils.defaultConnectionTester();
    }
}
