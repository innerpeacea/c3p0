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

import java.util.*;
import com.mchange.v2.coalesce.*;
import com.mchange.v2.log.*;
import com.mchange.v2.c3p0.impl.C3P0ImplUtils;
import com.mchange.v2.c3p0.impl.IdentityTokenized;
import com.mchange.v2.c3p0.subst.C3P0Substitutions;

public final class C3P0Registry
{
    //MT: thread-safe
    final static MLogger logger = MLog.getLogger( C3P0Registry.class );

    //MT: protected by class' lock
    static boolean banner_printed = false;

    //MT: thread-safe, immutable
    private static CoalesceChecker CC = new CoalesceChecker()
	{
	    public boolean checkCoalesce( Object a, Object b )
	    {
		IdentityTokenized aa = (IdentityTokenized) a;
		IdentityTokenized bb = (IdentityTokenized) b;

		String ta = aa.getIdentityToken();
		String tb = bb.getIdentityToken();

		if (ta == null || tb == null)
		    throw new NullPointerException( "[c3p0 bug] An IdentityTokenized object has no identity token set?!?! " + (ta == null ? ta : tb) );
		else
		    return ta.equals(tb);
	    }

	    public int coalesceHash( Object a )
	    { 
		String t = ((IdentityTokenized) a).getIdentityToken();
		return (t != null ? t.hashCode() : 0); 
	    }
	};

    //MT: protected by its own lock
    //a strong, synchronized coalescer
    private static Coalescer idtCoalescer = CoalescerFactory.createCoalescer(CC, false , true);

    //MT: protected by class' lock
    private static HashSet topLevelPooledDataSources = new HashSet();

    private static synchronized void banner()
    {
	if (! banner_printed )
	    {
		if (logger.isLoggable( MLevel.INFO ) )
		    logger.info("Initializing c3p0-" + C3P0Substitutions.VERSION + " [built " + C3P0Substitutions.TIMESTAMP + 
				"; debug? " + C3P0Substitutions.DEBUG + 
				"; trace: " + C3P0Substitutions.TRACE 
				+']');
		banner_printed = true;
	    }
    }

    private static synchronized void addToTopLevelPooledDataSources(IdentityTokenized idt)
    {
	if (idt instanceof PoolBackedDataSource)
	    {
		if (((PoolBackedDataSource) idt).owner() == null)
		    topLevelPooledDataSources.add( idt );
	    }
	else if (idt instanceof PooledDataSource)
	    { topLevelPooledDataSources.add( idt ); }
    }

    static void register(IdentityTokenized idt)
    {
	banner();

	if (idt.getIdentityToken() == null)
	    throw new RuntimeException("[c3p0 issue] The identityToken of a registered object should be set prior to registration.");
	if (idtCoalescer.coalesce(idt) != idt)
	    throw new RuntimeException("[c3p0 bug] Only brand new IdentityTokenized's, with their identities just set, should be registered!!!");
// 	System.err.println("[c3p0-registry] registered " + idt.getClass().getName() + 
// 			   "; natural identity: " + C3P0ImplUtils.identityToken( idt ) +
// 			   "; set identity: " + idt.getIdentityToken());

	addToTopLevelPooledDataSources(idt);
    }

    public static Set getPooledDataSources()
    { return (Set) topLevelPooledDataSources.clone(); }

    public static Object coalesce( IdentityTokenized idt )
    { 
	Object out = idtCoalescer.coalesce( idt ); 
// 	System.err.println("[c3p0-registry] coalesced " + idt.getClass().getName() + 
// 			   "; natural identity: " + C3P0ImplUtils.identityToken( idt ) +
// 			   "; set identity: " + idt.getIdentityToken());
// 	//System.err.println(idt);
// 	System.err.println("[c3p0-registry] output item " + idt.getClass().getName() + 
// 			   "; natural identity: " + C3P0ImplUtils.identityToken( out ) +
// 			   "; set identity: " + ((IdentityTokenized) out).getIdentityToken());
// 	//System.err.println(out);
	return out;
    }
}