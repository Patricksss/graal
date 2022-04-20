/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.tools.profiler.test;
import static com.oracle.truffle.api.TruffleLanguage.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import org.graalvm.polyglot.Source;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.tools.profiler.CallTracer;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;

public class CallTracerTest extends AbstractProfilerTest {

    private CallTracer tracer;

    protected static final SourceSectionFilter Both = SourceSectionFilter.
            newBuilder().sourceIs(s -> !s.isInternal()).
            tagIs(StandardTags.CallTag.class,StandardTags.RootTag.class).build();
    
    protected final Source jsSource = makeSource(
            "function sum(x){\n" +
                    "\treturn x;\n" +
                    "}\n" +
                    "\n" +
                    "function calc(x){\n" +
                    "\tfor (var i=0;i<10;i++) {sum(x)};\n" +
                    "}\n" +
                    "\n" +
                    "function add(){\n" +
                    "\treturn 5;\n" +
                    "}\n" +
                    "\n" +
                    "function output(){\n" +
                    "\tvar self = this;\n" +
                    "\tthis.output = function(){\n" +
                    "\t}\n" +
                    "}\n" +
                    "\n" +
                    "function main(){\n" +
                    "\tcalc(\"hello\");\n" +
                    "\tcalc(5);\n" +
                    "\toutput1 = new output();\n" +
                    "\toutput2 = new output();\n" +
                    "\tfor (var i=0;i<20;i++) {add()};\n" +
                    "\tsum();\n" +
                    "\toutput1.output();\n" +
                    "\toutput2.output();\n" +
                    "}\n" +
                    "\n" +
                    "main();\n"
    );

    protected final Source jsRecursiveSource = makeSource(
            "function sum(x){\n"
            + "    return x;\n"
            + "}\n"
            + "function calc(x,y){\n"
            + "    if (y>5)\n"
            + "        return;\n"
            + "    for (y=y;y<5;y++) {calc(x)};\n"
            + "    for (var i=0;i<10;i++) {sum(x)};\n"
            + "}\n"
            + "function main(){\n"
            + "    calc(\"hello\",0);\n"
            + "    calc(5,0);\n"
            + "    }\n"
            + "main();"
    );
    
    protected Source makeSource(String s) {
        return Source.newBuilder("js", s, "test").buildLiteral();
    }

    @Before
    public void setupTracer() {
        tracer = CallTracer.find(context.getEngine());
        Assert.assertNotNull(tracer);
    }

    @Test
    public void testCollecting() {

        Assert.assertFalse(tracer.isCollecting());

        tracer.setCollecting(true);

        Assert.assertEquals(0, tracer.getPayloads().size());
        Assert.assertTrue(tracer.isCollecting());

        eval(jsSource);

        Assert.assertNotEquals(0, tracer.getPayloads().size());
        Assert.assertTrue(tracer.isCollecting());

        tracer.setCollecting(false);

        Assert.assertFalse(tracer.isCollecting());

        tracer.clearData();

        Assert.assertEquals(0, tracer.getPayloads().size());
    }

    //CallCount
    //CallArgumentCount
    //CallResultCount
    //CallObjectCount
    //CallLocationCount
    //CallCountAll
    @Test
    public void testRecursiveCorrectCallCount() {
        final Map<String, Long> expectedCountMap = new HashMap<>();
        expectedCountMap.put("main,calc", 2L);
        expectedCountMap.put("calc,sum", 120L);
        expectedCountMap.put("calc,calc", 10L);
        expectedCountMap.put(":program,main", 1L);

        synchronized (tracer) {
            tracer.setFilter(Both);
        }
        executeAndCheckRootNameCounters(jsRecursiveSource, expectedCountMap,false,false,false,false,true);
    }
    
    @Test
    public void testRecursiveArguments() {
        final Map<String, Long> expectedCountMap = new HashMap<>();
        expectedCountMap.put("main,calc,\"class java.lang.Integer\",\"class java.lang.Integer\"", 1L);
        expectedCountMap.put("main,calc,\"class java.lang.String\",\"class java.lang.Integer\"", 1L);
        expectedCountMap.put("calc,sum,\"class java.lang.String\"", 60L);
        expectedCountMap.put("calc,sum,\"class java.lang.Integer\"", 60L);
        expectedCountMap.put("calc,calc,\"class java.lang.Integer\"", 5L);
        expectedCountMap.put("calc,calc,\"class java.lang.String\"", 5L);
        expectedCountMap.put(":program,main", 1L);

        synchronized (tracer) {
            tracer.setFilter(Both);
        }
        executeAndCheckRootNameCounters(jsRecursiveSource, expectedCountMap,true,false,false,false,true);
    }
    
    @Test
    public void testCorrectCallCount() {
        final Map<String, Long> expectedCountMap = new HashMap<>();
        expectedCountMap.put("main,calc", 2L);
        expectedCountMap.put("calc,sum", 20L);
        expectedCountMap.put("main,add", 20L);
        expectedCountMap.put("main,output", 2L);
        expectedCountMap.put("main,this.output", 2L);
        expectedCountMap.put("main,sum", 1L);
        expectedCountMap.put(":program,main", 1L);

        synchronized (tracer) {
            tracer.setFilter(Both);
        }
        executeAndCheckRootNameCounters(jsSource, expectedCountMap,false,false,false,false,false);
    }

    @Test
    public void testCorrectArgumentCount() {
        final Map<String, Long> expectedCountMap = new HashMap<>();
        expectedCountMap.put("main,calc,\"class java.lang.Integer\"", 1L);
        expectedCountMap.put("main,calc,\"class java.lang.String\"", 1L);
        expectedCountMap.put("calc,sum,\"class java.lang.String\"", 10L);
        expectedCountMap.put("calc,sum,\"class java.lang.Integer\"", 10L);
        expectedCountMap.put("main,add", 20L);
        expectedCountMap.put("main,output", 2L);
        expectedCountMap.put("main,this.output", 2L);
        expectedCountMap.put("main,sum", 1L);
        expectedCountMap.put(":program,main", 1L);

        synchronized (tracer) {
            tracer.setFilter(Both);
        }
        executeAndCheckRootNameCounters(jsSource, expectedCountMap,true,false,false,false,false);
    }

    @Test
    public void testCorrectResultCount() {
        final Map<String, Long> expectedCountMap = new HashMap<>();
        expectedCountMap.put("main,calc,class com.oracle.truffle.js.runtime.objects.Nullish", 2L);
        expectedCountMap.put("calc,sum,class java.lang.String", 10L);
        expectedCountMap.put("calc,sum,class java.lang.Integer", 10L);
        expectedCountMap.put("main,add,class java.lang.Integer", 20L);
        expectedCountMap.put("main,output,class com.oracle.truffle.js.runtime.objects.JSOrdinaryObject$DefaultLayout", 2L);
        expectedCountMap.put("main,this.output,class com.oracle.truffle.js.runtime.objects.Nullish", 2L);
        expectedCountMap.put("main,sum,class com.oracle.truffle.js.runtime.objects.Nullish", 1L);
        expectedCountMap.put(":program,main,class com.oracle.truffle.js.runtime.objects.Nullish", 1L);

        synchronized (tracer) {
            tracer.setFilter(Both);
        }
        executeAndCheckRootNameCounters(jsSource, expectedCountMap,false,false,true,false,false);
    }

    @Test
    public void testCorrectLocationCount() {
        final Map<String, Long> expectedCountMap = new HashMap<>();
        expectedCountMap.put("main,calc,20", 1L);
        expectedCountMap.put("main,calc,21", 1L);
        expectedCountMap.put("calc,sum,6", 20L);
        expectedCountMap.put("main,add,24", 20L);
        expectedCountMap.put("main,output,22", 1L);
        expectedCountMap.put("main,output,23", 1L);
        expectedCountMap.put("main,this.output,26", 1L);
        expectedCountMap.put("main,this.output,27", 1L);
        expectedCountMap.put("main,sum,25", 1L);
        expectedCountMap.put(":program,main,30", 1L);

        synchronized (tracer) {
            tracer.setFilter(Both);
        }
        executeAndCheckRootNameCounters(jsSource, expectedCountMap,false,true,false,false,false);
    }

    @Test
    public void testCorrectObjectInstanceCount() {
        final Map<String, Long> expectedCountMap = new HashMap<>();
        expectedCountMap.put("main,calc", 2L);
        expectedCountMap.put("calc,sum", 20L);
        expectedCountMap.put("main,add", 20L);
        expectedCountMap.put("main,output,1", 1L);
        expectedCountMap.put("main,output,2", 1L);
        expectedCountMap.put("main,this.output,1", 1L);
        expectedCountMap.put("main,this.output,2", 1L);
        expectedCountMap.put("main,sum", 1L);
        expectedCountMap.put(":program,main", 1L);

        synchronized (tracer) {
            tracer.setFilter(Both);
        }

        tracer.clearData();
        tracer.setArguments(false);
        tracer.setResults(false);
        tracer.setObjectInstances(true);
        tracer.setSpecificSourceLocations(false);

        tracer.setCollecting(true);
        eval(jsSource);

        Map<Integer, Integer> callMapCount = new HashMap<>(tracer.getCallMapCount());
        Map<Integer, CallTracer.Key> callMapKey = new HashMap<>(tracer.getCallMapKey());

        Assert.assertEquals(
                "Total number of counters does not match after one excecution",
                expectedCountMap.size(), callMapCount.size());
    }
    

    private void executeAndCheckRootNameCounters(Source recursiveSource,
                                                 Map<String, Long> expectedCountMap,
                                                 Boolean argumentss, Boolean location, Boolean result, Boolean object, Boolean recursive) {
        final int longExecutionCount = 100;

        tracer.clearData();
        tracer.setArguments(argumentss);
        tracer.setResults(result);
        tracer.setObjectInstances(object);
        tracer.setSpecificSourceLocations(location);

        tracer.setCollecting(true);
        
        if (recursive) {
        	eval(jsRecursiveSource);
        }
        else {
        	eval(jsSource);
        }

        Map<Integer, Integer> callMapCount = new HashMap<>(tracer.getCallMapCount());
        Map<Integer, CallTracer.Key> callMapKey = new HashMap<>(tracer.getCallMapKey());

        Assert.assertEquals(
                "Total number of counters does not match after one excecution",
                expectedCountMap.size(), callMapCount.size());

        for (Map.Entry<Integer, Integer> entry : callMapCount.entrySet()) {
            Integer mapKey = entry.getKey();
            String targetKey = callMapKey.get(mapKey).targetRootNode.toString();
            String sourceKey = callMapKey.get(mapKey).sourceRootNode.toString();
            String key = sourceKey + "," + targetKey;
            if (result) {
                String returnResults = callMapKey.get(mapKey).result.toString();
                key = key + "," + returnResults;
            }
            if (object) {
                String objectInstance = callMapKey.get(mapKey).objectInstanceKey.toString();
            }
            if (location) {
                Integer specificSourceLocation = callMapKey.get(mapKey).specifiedSourceLoc.getEndLine();
                key = key + "," + specificSourceLocation.toString();
            }
            if (argumentss){
                ArrayList<String> printArguments = callMapKey.get(mapKey).arguments;
                if (printArguments.size() < 3){}
                else {
                    for (int i=2; i<printArguments.size(); i++){
                        key = key + "," + printArguments.get(i);
                    }
                }
            }
            final long expectedCount = expectedCountMap.get(key);
            final long count = entry.getValue();
            Assert.assertEquals(sourceKey + "," + targetKey + " count not correct",
                    expectedCount, count);
        }

        for (int i = 1; i < longExecutionCount; i++) {
            if (recursive) {
            	eval(jsRecursiveSource);
            }
            else {
            	eval(jsSource);
            }
        }

        Map<Integer, Integer> callMapCount2 = new HashMap<>(tracer.getCallMapCount());

        for (Map.Entry<Integer, Integer> entry : callMapCount2.entrySet()) {
            Integer mapKey = entry.getKey();
            String targetKey = callMapKey.get(mapKey).targetRootNode.toString();
            String sourceKey = callMapKey.get(mapKey).sourceRootNode.toString();
            String key = sourceKey + "," + targetKey;

            if (result) {
                String returnResults = callMapKey.get(mapKey).result.toString();
                key = key + "," + returnResults;
            }
            if (object) {
                String objectInstance = callMapKey.get(mapKey).objectInstanceKey.toString();
            }
            if (location) {
                Integer specificSourceLocation = callMapKey.get(mapKey).specifiedSourceLoc.getEndLine();
                key = key + "," + specificSourceLocation.toString();
            }
            if (argumentss){
                ArrayList<String> printArguments = callMapKey.get(mapKey).arguments;
                if (printArguments.size() < 3){}
                else {
                    for (int i=2; i<printArguments.size(); i++){
                        key = key + "," + printArguments.get(i);
                    }
                }
            }
            final long expectedCount = expectedCountMap.get(key)*longExecutionCount;
            final long count = entry.getValue();
            Assert.assertEquals(sourceKey + "," + targetKey + " count not correct",
                    expectedCount, count);
        }


    }
}