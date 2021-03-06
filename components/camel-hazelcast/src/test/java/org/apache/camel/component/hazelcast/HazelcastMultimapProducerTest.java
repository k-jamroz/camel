/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.hazelcast;

import java.util.Arrays;
import java.util.Collection;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class HazelcastMultimapProducerTest extends HazelcastCamelTestSupport {

    @Mock
    private MultiMap<Object, Object> map;

    @Override
    protected void trainHazelcastInstance(HazelcastInstance hazelcastInstance) {
        when(hazelcastInstance.getMultiMap("bar")).thenReturn(map);
    }

    @Override
    protected void verifyHazelcastInstance(HazelcastInstance hazelcastInstance) {
        verify(hazelcastInstance, atLeastOnce()).getMultiMap("bar");
    }

    @AfterEach
    public void verifyMapMock() {
        verifyNoMoreInteractions(map);
    }

    @Test
    public void testWithInvalidOperation() {
        assertThrows(CamelExecutionException.class,
                () -> template.sendBodyAndHeader("direct:putInvalid", "my-foo", HazelcastConstants.OBJECT_ID, "4711"));
    }

    @Test
    public void testPut() throws InterruptedException {
        template.sendBodyAndHeader("direct:put", "my-foo", HazelcastConstants.OBJECT_ID, "4711");
        verify(map).put("4711", "my-foo");
    }

    @Test
    public void testPutWithOperationName() throws InterruptedException {
        template.sendBodyAndHeader("direct:putWithOperationName", "my-foo", HazelcastConstants.OBJECT_ID, "4711");
        verify(map).put("4711", "my-foo");
    }

    @Test
    public void testPutWithOperationNumber() throws InterruptedException {
        template.sendBodyAndHeader("direct:putWithOperationNumber", "my-foo", HazelcastConstants.OBJECT_ID, "4711");
        verify(map).put("4711", "my-foo");
    }

    @Test
    public void testRemoveValue() {
        template.sendBodyAndHeader("direct:removeValue", "my-foo", HazelcastConstants.OBJECT_ID, "4711");
        verify(map).remove("4711", "my-foo");
    }

    @Test
    public void testGet() {
        when(map.get("4711")).thenReturn(Arrays.<Object> asList("my-foo"));
        template.sendBodyAndHeader("direct:get", null, HazelcastConstants.OBJECT_ID, "4711");
        verify(map).get("4711");
        Collection<?> body = consumer.receiveBody("seda:out", 5000, Collection.class);
        assertTrue(body.contains("my-foo"));
    }

    @Test
    public void testDelete() {
        template.sendBodyAndHeader("direct:delete", null, HazelcastConstants.OBJECT_ID, 4711);
        verify(map).remove(4711);
    }

    @Test
    public void testClear() {
        template.sendBody("direct:clear", "test");
        verify(map).clear();
    }

    @Test
    public void testValueCount() {
        template.sendBodyAndHeader("direct:valueCount", "test", HazelcastConstants.OBJECT_ID, "4711");
        verify(map).valueCount("4711");
    }

    @Test
    public void testContainsKey() {
        when(map.containsKey("testOk")).thenReturn(true);
        when(map.containsKey("testKo")).thenReturn(false);
        template.sendBodyAndHeader("direct:containsKey", null, HazelcastConstants.OBJECT_ID, "testOk");
        Boolean body = consumer.receiveBody("seda:out", 5000, Boolean.class);
        verify(map).containsKey("testOk");
        assertEquals(true, body);
        template.sendBodyAndHeader("direct:containsKey", null, HazelcastConstants.OBJECT_ID, "testKo");
        body = consumer.receiveBody("seda:out", 5000, Boolean.class);
        verify(map).containsKey("testKo");
        assertEquals(false, body);
    }

    @Test
    public void testContainsValue() {
        when(map.containsValue("testOk")).thenReturn(true);
        when(map.containsValue("testKo")).thenReturn(false);
        template.sendBody("direct:containsValue", "testOk");
        Boolean body = consumer.receiveBody("seda:out", 5000, Boolean.class);
        verify(map).containsValue("testOk");
        assertEquals(true, body);
        template.sendBody("direct:containsValue", "testKo");
        body = consumer.receiveBody("seda:out", 5000, Boolean.class);
        verify(map).containsValue("testKo");
        assertEquals(false, body);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("direct:putInvalid").setHeader(HazelcastConstants.OPERATION, constant("bogus"))
                        .to(String.format("hazelcast-%sbar", HazelcastConstants.MULTIMAP_PREFIX));

                from("direct:put").setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.PUT))
                        .to(String.format("hazelcast-%sbar", HazelcastConstants.MULTIMAP_PREFIX));

                from("direct:removeValue").setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.REMOVE_VALUE))
                        .to(
                                String.format("hazelcast-%sbar", HazelcastConstants.MULTIMAP_PREFIX));

                from("direct:get").setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.GET))
                        .to(String.format("hazelcast-%sbar", HazelcastConstants.MULTIMAP_PREFIX))
                        .to("seda:out");

                from("direct:delete").setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.DELETE))
                        .to(String.format("hazelcast-%sbar", HazelcastConstants.MULTIMAP_PREFIX));

                from("direct:clear").setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.CLEAR))
                        .to(String.format("hazelcast-%sbar", HazelcastConstants.MULTIMAP_PREFIX));

                from("direct:valueCount").setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.VALUE_COUNT))
                        .to(String.format("hazelcast-%sbar", HazelcastConstants.MULTIMAP_PREFIX));

                from("direct:containsKey").setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.CONTAINS_KEY))
                        .to(String.format("hazelcast-%sbar", HazelcastConstants.MULTIMAP_PREFIX))
                        .to("seda:out");

                from("direct:containsValue")
                        .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.CONTAINS_VALUE))
                        .to(String.format("hazelcast-%sbar", HazelcastConstants.MULTIMAP_PREFIX))
                        .to("seda:out");

                from("direct:putWithOperationNumber").toF("hazelcast-%sbar?operation=%s", HazelcastConstants.MULTIMAP_PREFIX,
                        HazelcastOperation.PUT);
                from("direct:putWithOperationName").toF("hazelcast-%sbar?operation=PUT", HazelcastConstants.MULTIMAP_PREFIX);
            }
        };
    }

}
