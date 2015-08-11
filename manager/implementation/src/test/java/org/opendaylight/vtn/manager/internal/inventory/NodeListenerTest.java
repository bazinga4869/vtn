/*
 * Copyright (c) 2015 NEC Corporation
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.vtn.manager.internal.inventory;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import org.junit.Before;
import org.junit.Test;

import org.opendaylight.vtn.manager.internal.TxQueue;
import org.opendaylight.vtn.manager.internal.TxTask;
import org.opendaylight.vtn.manager.internal.util.ChangedData;
import org.opendaylight.vtn.manager.internal.util.IdentifiedData;
import org.opendaylight.vtn.manager.internal.util.inventory.SalNode;

import org.opendaylight.vtn.manager.internal.TestBase;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.opendaylight.yang.gen.v1.urn.opendaylight.vtn.types.rev150209.VtnUpdateType;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;

/**
 * JUnit test for {@link NodeListener}.
 */
public class NodeListenerTest extends TestBase {
    /**
     * Mock-up of {@link TxQueue}.
     */
    private TxQueue  txQueue;

    /**
     * Mock-up of {@link DataBroker}.
     */
    private DataBroker  dataBroker;

    /**
     * Mock-up of {@link ListenerRegistration}.
     */
    private ListenerRegistration  registration;

    /**
     * A {@link NodeListener} instance for test.
     */
    private NodeListener  nodeListener;

    /**
     * Set up test environment.
     */
    @Before
    public void setUp() {
        dataBroker = mock(DataBroker.class);
        txQueue = mock(TxQueue.class);
        registration = mock(ListenerRegistration.class);

        when(dataBroker.registerDataChangeListener(
                 any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                 any(NodeListener.class), any(DataChangeScope.class))).
            thenReturn(registration);
        nodeListener = new NodeListener(txQueue, dataBroker);
    }

    /**
     * Test case for {@link NodeListener#NodeListener(TxQueue, DataBroker)}.
     */
    @Test
    public void testConstructor() {
        LogicalDatastoreType oper = LogicalDatastoreType.OPERATIONAL;
        DataChangeScope scope = DataChangeScope.SUBTREE;
        InstanceIdentifier<FlowCapableNode> path = getPath();
        verify(dataBroker).
            registerDataChangeListener(oper, path, nodeListener, scope);
        verifyZeroInteractions(registration);
        verify(txQueue).postFirst(any(TxTask.class));
        verify(txQueue, never()).post(any(TxTask.class));
        assertEquals(FlowCapableNode.class, nodeListener.getTargetType());
    }

    /**
     * Test case for {@link NodeListener#enterEvent(AsyncDataChangeEvent)}.
     */
    @Test
    public void testEnterEvent() {
        AsyncDataChangeEvent ev = null;
        NodeUpdateTask task = nodeListener.enterEvent(ev);
        assertTrue(task instanceof NodeUpdateTask);
        NodeUpdateTask task1 = nodeListener.enterEvent(ev);
        assertTrue(task1 instanceof NodeUpdateTask);
        assertNotSame(task, task1);
    }

    /**
     * Test case for {@link NodeListener#exitEvent(NodeUpdateTask)}.
     */
    @Test
    public void testExitEvent() {
        reset(txQueue);
        Logger logger = mock(Logger.class);
        NodeUpdateTask task = new NodeUpdateTask(logger);
        nodeListener.exitEvent(task);
        verifyZeroInteractions(txQueue);

        SalNode snode = new SalNode(123L);
        InstanceIdentifier<FlowCapableNode> path = InstanceIdentifier.
            builder(Nodes.class).
            child(Node.class, snode.getNodeKey()).
            augmentation(FlowCapableNode.class).
            build();
        task.addUpdated(path, snode);
        nodeListener.exitEvent(task);
        verify(txQueue, never()).postFirst(any(TxTask.class));
        verify(txQueue).post(task);
        verifyZeroInteractions(logger);
    }

    /**
     * Test case for {@link NodeListener#onCreated(NodeUpdateTask,IdentifiedData)}.
     */
    @Test
    public void testOnCreated() {
        reset(txQueue);
        FlowCapableNode fcn = mock(FlowCapableNode.class);

        Logger logger = mock(Logger.class);
        NodeUpdateTask task = new NodeUpdateTask(logger);
        assertEquals(false, task.hasUpdates());

        // In case of unsupported node.
        NodeId unsupported = new NodeId("unknown:1");
        InstanceIdentifier<FlowCapableNode> path = InstanceIdentifier.
            builder(Nodes.class).
            child(Node.class, new NodeKey(unsupported)).
            augmentation(FlowCapableNode.class).
            build();
        IdentifiedData<FlowCapableNode> data = new IdentifiedData<>(path, fcn);
        nodeListener.onCreated(task, data);
        assertEquals(false, task.hasUpdates());

        // In case of OpenFlow node.
        SalNode[] nodes = {
            new SalNode(1L),
            new SalNode(-1L),
            new SalNode(12345L),
        };
        Map<SalNode, InstanceIdentifier<FlowCapableNode>> updated =
            new HashMap<>();
        for (SalNode snode: nodes) {
            path = InstanceIdentifier.builder(Nodes.class).
                child(Node.class, snode.getNodeKey()).
                augmentation(FlowCapableNode.class).
                build();
            assertEquals(null, updated.put(snode, path));
            data = new IdentifiedData<>(path, fcn);
            nodeListener.onCreated(task, data);
            assertEquals(true, task.hasUpdates());
        }

        verifyZeroInteractions(txQueue);
        assertEquals(nodes.length, updated.size());
        assertEquals(updated, task.getUpdatedMap());
    }

    /**
     * Test case for {@link NodeListener#onUpdated(NodeUpdateTask,ChangedData)}.
     */
    @Test
    public void testOnUpdated() {
        reset(txQueue);
        FlowCapableNode fcn1 = mock(FlowCapableNode.class);
        FlowCapableNode fcn2 = mock(FlowCapableNode.class);
        SalNode snode = new SalNode(12345L);
        InstanceIdentifier<FlowCapableNode> path = InstanceIdentifier.
            builder(Nodes.class).
            child(Node.class, snode.getNodeKey()).
            augmentation(FlowCapableNode.class).
            build();
        ChangedData<FlowCapableNode> data =
            new ChangedData<>(path, fcn1, fcn2);

        Logger logger = mock(Logger.class);
        NodeUpdateTask task = new NodeUpdateTask(logger);
        assertEquals(false, task.hasUpdates());

        try {
            nodeListener.onUpdated(task, data);
            unexpected();
        } catch (IllegalStateException e) {
        }

        assertEquals(false, task.hasUpdates());
        verifyZeroInteractions(txQueue);
    }

    /**
     * Test case for {@link NodeListener#onRemoved(NodeUpdateTask,IdentifiedData)}.
     */
    @Test
    public void testOnRemoved() {
        reset(txQueue);
        FlowCapableNode fcn = mock(FlowCapableNode.class);

        Logger logger = mock(Logger.class);
        NodeUpdateTask task = new NodeUpdateTask(logger);
        assertEquals(false, task.hasUpdates());

        // In case of unsupported node.
        NodeId unsupported = new NodeId("unknown:1");
        InstanceIdentifier<FlowCapableNode> path = InstanceIdentifier.
            builder(Nodes.class).
            child(Node.class, new NodeKey(unsupported)).
            augmentation(FlowCapableNode.class).
            build();
        IdentifiedData<FlowCapableNode> data = new IdentifiedData<>(path, fcn);
        nodeListener.onRemoved(task, data);
        assertEquals(false, task.hasUpdates());

        // In case of OpenFlow node.
        SalNode[] nodes = {
            new SalNode(1L),
            new SalNode(-1L),
            new SalNode(12345L),
        };
        Map<SalNode, InstanceIdentifier<FlowCapableNode>> updated =
            new HashMap<>();
        for (SalNode snode: nodes) {
            path = InstanceIdentifier.builder(Nodes.class).
                child(Node.class, snode.getNodeKey()).
                augmentation(FlowCapableNode.class).
                build();
            assertEquals(null, updated.put(snode, path));
            data = new IdentifiedData<>(path, fcn);
            nodeListener.onRemoved(task, data);
            assertEquals(true, task.hasUpdates());
        }

        verifyZeroInteractions(txQueue);
        assertEquals(nodes.length, updated.size());
        assertEquals(updated, task.getUpdatedMap());
    }

    /**
     * Test case for {@link NodeListener#getWildcardPath()}.
     */
    @Test
    public void testGetWildcardPath() {
        assertEquals(getPath(), nodeListener.getWildcardPath());
    }

    /**
     * Test case for {@link NodeListener#getRequiredEvents()}.
     */
    @Test
    public void testGetRequiredEvents() {
        Set<VtnUpdateType> events = nodeListener.getRequiredEvents();
        assertEquals(2, events.size());
        assertEquals(true, events.contains(VtnUpdateType.CREATED));
        assertEquals(true, events.contains(VtnUpdateType.REMOVED));
    }

    /**
     * Test case for {@link NodeListener#getLogger()}.
     */
    @Test
    public void testGetLogger() {
        Logger logger = nodeListener.getLogger();
        assertEquals(NodeListener.class.getName(), logger.getName());
    }

    /**
     * Test case for {@link NodeListener#close()}.
     */
    @Test
    public void testClose() {
        verifyZeroInteractions(registration);
        nodeListener.close();
        verify(registration).close();
    }

    /**
     * Return a wildcard path to the MD-SAL data model to listen.
     *
     * @return  A wildcard path to the MD-SAL data model to listen.
     */
    private InstanceIdentifier<FlowCapableNode> getPath() {
        return InstanceIdentifier.builder(Nodes.class).
            child(Node.class).
            augmentation(FlowCapableNode.class).
            build();
    }
}