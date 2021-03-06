/*
 * MindmapsDB - A Distributed Semantic Database
 * Copyright (C) 2016  Mindmaps Research Ltd
 *
 * MindmapsDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MindmapsDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MindmapsDB. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package io.mindmaps.factory;

import io.mindmaps.MindmapsTransaction;
import io.mindmaps.core.MindmapsGraph;
import io.mindmaps.core.implementation.MindmapsTransactionImpl;
import io.mindmaps.core.implementation.exception.MindmapsValidationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class MindmapsTitanGraphTest {
    private static final String TEST_CONFIG = "../conf/mindmaps-test.properties";
    private static final String TEST_NAME = "mindmapstest";
    private static final String TEST_URI = "localhost";
    private MindmapsGraph mindmapsGraph;

    @Before
    public void setup(){
        mindmapsGraph = new MindmapsTitanGraphFactory().getGraph(TEST_NAME, TEST_URI, TEST_CONFIG);
    }

    @After
    public void cleanup(){
        MindmapsGraph mg = new MindmapsTitanGraphFactory().getGraph(TEST_NAME, TEST_URI, TEST_CONFIG);
        mg.clear();
    }

    @Test
    public void testMultithreading(){
        Set<Future> futures = new HashSet<>();
        ExecutorService pool = Executors.newFixedThreadPool(10);

        for(int i = 0; i < 100; i ++){
            futures.add(pool.submit(() -> addEntityType(mindmapsGraph)));
        }

        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        MindmapsTransactionImpl transaction = (MindmapsTransactionImpl) mindmapsGraph.getTransaction();
        assertEquals(108, transaction.getTinkerTraversal().V().toList().size());
    }
    private void addEntityType(MindmapsGraph mindmapsGraph){
        MindmapsTransaction mindmapsTransaction = mindmapsGraph.getTransaction();
        mindmapsTransaction.putEntityType(UUID.randomUUID().toString());
        try {
            mindmapsTransaction.commit();
        } catch (MindmapsValidationException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testSingletonTitanTransaction() throws ExecutionException, InterruptedException {
        MindmapsTransaction transaction = mindmapsGraph.getTransaction();
        MindmapsTransaction transaction2 = mindmapsGraph.getTransaction();
        final MindmapsTransaction[] transaction3 = new MindmapsTransaction[1];

        assertEquals(transaction, transaction2);

        ExecutorService pool = Executors.newSingleThreadExecutor();
        pool.submit(() -> transaction3[0] = mindmapsGraph.getTransaction()).get();

        assertNotNull(transaction3[0]);
        assertNotEquals(transaction, transaction3[0]);
    }

    @Test
    public void testTestThreadLocal(){
        ExecutorService pool = Executors.newFixedThreadPool(10);
        Set<Future> futures = new HashSet<>();
        MindmapsTransactionImpl transcation = (MindmapsTransactionImpl) mindmapsGraph.getTransaction();
        transcation.putEntityType(UUID.randomUUID().toString());
        assertEquals(9, transcation.getTinkerTraversal().V().toList().size());

        for(int i = 0; i < 100; i ++){
            futures.add(pool.submit(() -> {
                MindmapsTransaction innerTranscation = mindmapsGraph.getTransaction();
                innerTranscation.putEntityType(UUID.randomUUID().toString());
            }));
        }

        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException ignored) {

            }
        });

        assertEquals(9, transcation.getTinkerTraversal().V().toList().size());
    }
}