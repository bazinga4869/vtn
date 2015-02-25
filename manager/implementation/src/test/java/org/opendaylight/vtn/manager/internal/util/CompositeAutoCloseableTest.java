/*
 * Copyright (c) 2015 NEC Corporation
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.vtn.manager.internal.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.mockito.Mockito;

import org.slf4j.Logger;

import org.opendaylight.vtn.manager.internal.TestBase;

/**
 * JUnit test for {@link CompositeAutoCloseable}.
 */
public class CompositeAutoCloseableTest extends TestBase {
    /**
     * An implementation of {@link AutoCloseable} that counts the number
     * of {@link #close()} calls.
     */
    private static class CloseCounter implements AutoCloseable {
        /**
         * The number of {@link #close()} calls.
         */
        private final AtomicInteger  closed = new AtomicInteger();

        /**
         * Close this instance.
         */
        @Override
        public void close() {
            closed.getAndIncrement();
        }

        /**
         * Return the number of {@link #close()} calls.
         *
         * @return  The number of {@link #close()} calls.
         */
        private int getClosedCount() {
            return closed.get();
        }
    }

    /**
     * An implementation of {@link AutoCloseable} that always throws the
     * specified exception.
     */
    private static class BadCloseable extends CloseCounter {
        /**
         * An exception to be thrown by {@link #close()}.
         */
        private final RuntimeException  exception;

        /**
         * Construct a new instance.
         *
         * @param e  An exception to be thrown by {@link #close()}.
         */
        private BadCloseable(RuntimeException e) {
            exception = e;
        }

        /**
         * Close this instance.
         */
        @Override
        public void close() {
            super.close();
            throw exception;
        }
    }

    /**
     * Test case for {@link CompositeAutoCloseable} methods.
     */
    @Test
    public void test() {
        Logger logger = Mockito.mock(Logger.class);
        CompositeAutoCloseable cc = new CompositeAutoCloseable(logger);
        assertEquals(false, cc.isClosed());

        List<CloseCounter> counters = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CloseCounter counter = new CloseCounter();
            counters.add(counter);
            for (int j = 0; j < 5; j++) {
                cc.add(counter);
            }
        }

        IllegalStateException e = new IllegalStateException();
        BadCloseable bad = new BadCloseable(e);
        counters.add(bad);
        for (int i = 0; i < 5; i++) {
            cc.add(bad);
        }
        assertEquals(false, cc.isClosed());

        cc.close();
        assertEquals(true, cc.isClosed());
        for (CloseCounter counter: counters) {
            assertEquals(1, counter.getClosedCount());
        }

        Mockito.verify(logger).error("Failed to close instance: " + bad, e);
        Mockito.verify(logger, Mockito.never()).warn(Mockito.anyString());
        Mockito.verify(logger, Mockito.never()).debug(Mockito.anyString());
        Mockito.verify(logger, Mockito.never()).trace(Mockito.anyString());
        Mockito.reset(logger);

        // Ensure that CompositeAutoCloseable does nothing if it is closed.
        List<CloseCounter> ignored = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CloseCounter counter = new CloseCounter();
            ignored.add(counter);
            cc.add(counter);

            BadCloseable bc = new BadCloseable(new IllegalArgumentException());
            ignored.add(bc);
            cc.add(bc);

            cc.close();
        }

        for (CloseCounter counter: counters) {
            assertEquals(1, counter.getClosedCount());
        }
        for (CloseCounter counter: ignored) {
            assertEquals(0, counter.getClosedCount());
        }

        Mockito.verifyZeroInteractions(logger);
    }
}
