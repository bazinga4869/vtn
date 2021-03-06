/*
 * Copyright (c) 2015 NEC Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.vtn.manager.internal.util.inventory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Test;

import org.opendaylight.vtn.manager.internal.TestBase;

import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.impl.inventory.rev150209.vtn.node.info.VtnPort;

/**
 * JUnit test for {@link NodePortFilter}
 */
public class NodePortFilterTest extends TestBase {
    /**
     * Test case for {@link NodePortFilter#accept(SalPort, VtnPort)}
     */
    @Test
    public void testAccept() {
        long nodeNumber = 2L;
        SalNode target = new SalNode(nodeNumber);
        NodePortFilter filter = new NodePortFilter(target);
        VtnPort vport = mock(VtnPort.class);

        for (long dpid = 1L; dpid < 10L; dpid++) {
            for (long port = 1L; port <= 5L; port++) {
                SalPort sport = new SalPort(dpid, port);
                assertEquals(dpid == nodeNumber, filter.accept(sport, vport));
            }
        }

        assertEquals(false, filter.accept(null, vport));
        verifyZeroInteractions(vport);
    }

    /**
     * Test case for {@link NodePortFilter#toString()}.
     */
    @Test
    public void testToString() {
        for (long dpid = 1L; dpid < 10L; dpid++) {
            SalNode snode = new SalNode(dpid);
            NodePortFilter filter = new NodePortFilter(snode);
            String expected = "port-filter[node=openflow:" + dpid + "]";
            assertEquals(expected, filter.toString());
        }
    }
}
