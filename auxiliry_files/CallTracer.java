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
package com.oracle.truffle.tools.profiler;

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.ArrayList;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.StandardTags.RootTag;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.tools.profiler.impl.CallTracerInstrument;
import com.oracle.truffle.tools.profiler.impl.ProfilerToolFactory;

import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.NodeUtil;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.TruffleRuntime;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.RootCallTarget;
import java.util.Iterator;
import com.oracle.truffle.api.nodes.*;

/**
 * Implementation of a tracing based profiler for {@linkplain com.oracle.truffle.api.TruffleLanguage
 * Truffle languages} built on top of the {@linkplain TruffleInstrument Truffle instrumentation
 * framework}.
 * <p>
 * The tracer counts how many times each of the elements of interest (e.g. functions, statements,
 * etc.) are executed.
 * <p>
 * Usage example: {@codesnippet CallTracerSnippets#example}
 *
 *
 env.getInstrumenter().attachContextsListener(new ContextsListener() {

@Override
public void onContextCreated(TruffleContext context) {
synchronized (CPUSampler.this) {
activeContexts.put(context, new MutableSamplerData());
}
}


 * @since 0.30
 */

public final class CallTracer implements Closeable {

    CallTracer(Env env) {
        this.env = env;
    }

    private static final SourceSectionFilter DEFAULT_FILTER = SourceSectionFilter.newBuilder().tagIs(RootTag.class).build();

    private final Env env;

    private boolean closed = false;

    private boolean collecting = false;

    private SourceSectionFilter filter = null;

    private EventBinding<?> activeBinding;

    private Integer mapKey;

    private final Map<Integer, Payload> payloadMap = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> callMapCount = new ConcurrentHashMap<>();
    private static final Map<Integer, Key> callMapKey = new ConcurrentHashMap<>();
    private static final Map<Integer, ArrayList<Long>> callMapTimes = new ConcurrentHashMap<>();

    private static Long initialTime;
    private static ArrayList<String> previousArguments = new ArrayList<>();
    private static Payload sourcePayload;
    private static Payload targetPayload;
    private static ArrayList<Payload> targetPayloadList = new ArrayList<>();
    private static Object objectInstance;

    private static Boolean firstIteration = true;
    //These are used as options.
    private static Boolean useArgumentsFlag = false;
    private static Boolean useResultsFlag = false;
    private static Boolean useSpecificSourceLocationsFlag = false;
    private static Boolean useTexts = false;
    private static Boolean useTimesFlag = false;
    //Javascript only
    private static Boolean useObjectInstancesFlag = false;
    private static final Long warmup = 0l;
    //private static final Long warmup = 4000000000l;


    public synchronized boolean usingArguments() {
        return useArgumentsFlag;
    }
    public synchronized boolean usingResults() {
        return useResultsFlag;
    }
    public synchronized boolean usingObjectInstances() {
        return useObjectInstancesFlag;
    }
    public synchronized boolean usingSpecificSourceLocations() {
        return useSpecificSourceLocationsFlag;
    }
    public synchronized boolean usingText() {
        return useTexts;
    }
    public synchronized boolean usingTime() {
        return useTimesFlag;
    }

    public synchronized void setArguments(boolean useArguments) {
        useArgumentsFlag = useArguments;
    }
    public synchronized void setResults(boolean useResults) {
        useResultsFlag = useResults;
    }
    public synchronized void setObjectInstances(boolean useObjectInstances) {
        useObjectInstancesFlag = useObjectInstances;
    }
    public synchronized void setSpecificSourceLocations(boolean useSpecificSourceLocations) {
        useSpecificSourceLocationsFlag = useSpecificSourceLocations;
    }
    public synchronized void setText(boolean useText) {
        useTexts = useText;
    }
    public synchronized void setTime(boolean useTime) {
        useTimesFlag = useTime;
    }

    /**
     * Finds {@link CallTracer} associated with given engine.
     *
     * @param engine the engine to find debugger for
     * @return an instance of associated {@link CallTracer}
     * @since 19.0
     */
    public static CallTracer find(Engine engine) {
        return CallTracerInstrument.getTracer(engine);
    }

    /**
     * Controls whether the tracer is collecting data or not.
     *
     * @param collecting the new state of the tracer.
     * @since 0.30
     */
    public synchronized void setCollecting(boolean collecting) {
        if (closed) {
            throw new ProfilerException("CallTracer is already closed.");
        }
        if (this.collecting != collecting) {
            this.collecting = collecting;
            resetTracer();
        }
    }

    /**
     * @return whether or not the tracer is currently collecting data.
     * @since 0.30
     */
    public synchronized boolean isCollecting() {
        return collecting;
    }

    /**
     * Sets the {@link SourceSectionFilter filter} for the tracer. This allows the tracer to trace
     * only parts of the executed source code.
     *
     * @param filter The filter describing which part of the source code to trace
     * @since 0.30
     */
    public synchronized void setFilter(SourceSectionFilter filter) {
        verifyConfigAllowed();
        this.filter = filter;
    }

    /**
     * @return The filter describing which part of the source code to sample
     * @since 0.30
     */
    public synchronized SourceSectionFilter getFilter() {
        return filter;
    }

    /**
     * @return All the payloads the tracer has gathered as an unmodifiable collection
     * @since 0.30
     */
    public Collection<Payload> getPayloads() {
        return Collections.unmodifiableCollection(payloadMap.values());
    }
    /**
     * Erases all the data gathered by the tracer.
     *
     * @since 0.30
     */
    public void clearData() {
        payloadMap.clear();
        callMapCount.clear();
        callMapKey.clear();
        callMapTimes.clear();
    }

    private Payload getCounter(EventContext context) {
        SourceSection sourceSection = context.getInstrumentedSourceSection();
        RootNode rootNode = context.getInstrumentedNode().getRootNode();
        if (rootNode != null && sourceSection != null) {
            mapKey = 31 * rootNode.hashCode() * sourceSection.hashCode();
        }
        else {
            mapKey = 31 * rootNode.hashCode();
        }
        assert sourceSection != null : context;
        return payloadMap.computeIfAbsent(mapKey, new Function<Integer, Payload>() {
            @Override
            public Payload apply(Integer mapKeyInt) {
                return new Payload(new StackTraceEntry(CallTracer.this.env.getInstrumenter(), context, 0, true),context);
            }
        });
    }

    private synchronized void verifyConfigAllowed() {
        assert Thread.holdsLock(this);
        if (closed) {
            throw new ProfilerException("CallTracer is already closed.");
        } else if (collecting) {
            throw new ProfilerException("Cannot change tracer configuration while collecting. Call setCollecting(false) to disable collection first.");
        }
    }

    private synchronized void resetTracer() {
        assert Thread.holdsLock(this);
        if (activeBinding != null) {
            activeBinding.dispose();
            activeBinding = null;
        }
        if (!collecting || closed) {
            return;
        }

        SourceSectionFilter f = this.filter;
        if (f == null) {
            //changed
            f = SourceSectionFilter.newBuilder().tagIs(RootTag.class).build();
        }
        this.activeBinding = env.getInstrumenter().attachExecutionEventFactory(f, new ExecutionEventNodeFactory() {
            @Override
            public ExecutionEventNode create(EventContext context) {
                return new CounterNode(getCounter(context));
            }
        });
    }

    /**
     * Closes the tracer for fuhrer use, deleting all the gathered data.
     *
     * @since 0.30
     */
    @Override
    public synchronized void close() {
        closed = true;
        clearData();
    }

    /**
     * Holds data on how many times a section of source code was executed. Differentiates between
     * compiled and interpreted executions.
     *
     * @since 0.30
     */
    public static final class Payload {

        private final StackTraceEntry location;
        //instrumentedNode
        private final EventContext context;
        private long countInterpreted;
        private long countCompiled;
        private long countBoth;

        Payload(StackTraceEntry location,EventContext context) {
            this.location = location;
            this.context = context;
        }

        /**
         * @return Node object of the instrumented node.
         */
        public Node getNode() {
            return context.getInstrumentedNode();
        }
        public Object getObject() {
            return context.getNodeObject();
        }
        /**
         * @return The name of the root this counter is associated with.
         */
        public RootNode getRootName() {
            return context.getInstrumentedNode().getRootNode();
        }
        /**
         * Returns a set tags a stack location marked with. Common tags are {@link RootTag root},
         * {@link StatementTag statement} and {@link ExpressionTag expression}. Whether statement or
         * expression stack trace entries appear depends on the configured
         * {@link CallTracer#setFilter(com.oracle.truffle.api.instrumentation.SourceSectionFilter)
         * filter}.
         *
         * @since 0.30
         */
        public Set<Class<?>> getTags() {
            return location.getTags();
        }

        /**
         * @return The source section for which this {@link Payload} is counting executions
         * @since 0.30
         */
        public SourceSection getSourceSection() {
            return location.getSourceSection();
        }

        /**
         * @return The number of times the associated source sections was executed as compiled code
         * @since 0.30
         */
        public long getCountCompiled() {
            return countCompiled;
        }

        /**
         * @return The number of times the associated source sections was interpreted
         * @since 0.30
         */
        public long getCountInterpreted() {
            return countInterpreted;
        }

        public long getCountBoth() {
            return countBoth;
        }

        /**
         * @return The total number of times the associated source sections was executed
         * @since 0.30 class com.oracle.truffle.api.instrumentation.StandardTags$RootTag"
         */
        public long getCount() {
            return countCompiled + countInterpreted;
        }
    }

    /**
     * @return Hashmap which counts on how often source/target pair is called.
     */
    public Map<Integer, Integer> getCallMapCount() {
        return callMapCount;
    }

    /**
     * @return Hashmap with all information used to create the key.
     */
    public Map<Integer, Key> getCallMapKey() {
        return callMapKey;
    }

    /**
     * @return Hashmap that stores all return times of a source/target call.
     */
    public Map<Integer, ArrayList<Long>> getCallMapTimes() {
        return callMapTimes;
    }

    /**
     * @return Call information that is used to construct a key for previous 3 hashmaps.
     */

    public static class Key {
        public ArrayList<String> arguments;
        public SourceSection sourceSourceSection;
        public SourceSection targetSourceSection;
        public RootNode sourceRootNode;
        public RootNode targetRootNode;
        public Object result;
        public Object objectInstanceKey;
        public SourceSection specifiedSourceLoc;
        public Integer rootSize;
        public Integer targetSize;

        //standard hashfunction gave wrong results
        @Override
        public int hashCode() {
            int hash = 31;

            if (sourceSourceSection != null) {
                hash = 7*hash + sourceSourceSection.hashCode();
            }
            if (targetSourceSection != null) {
                hash = 7*hash + targetSourceSection.hashCode();
            }
            if (sourceRootNode != null) {
                hash = 7*hash + sourceRootNode.hashCode();
            }
            if (targetRootNode != null) {
                hash = 7*hash + targetRootNode.hashCode();
            }
            if (result != null) {
                hash = 7*hash + result.hashCode();
            }
            if (objectInstanceKey != null) {
                hash = 7*hash + objectInstanceKey.hashCode();
            }
            if (arguments != null) {
                hash = 7*hash + arguments.hashCode();
            }
            if (specifiedSourceLoc != null) {
                hash = 7*hash + specifiedSourceLoc.hashCode();
            }
            return hash;
        }
        }

    private static class CounterNode extends ExecutionEventNode {

        private final Payload payload;
        private boolean continueIterate;

        CounterNode(Payload payload) {
            this.payload = payload;
        }

        @Override
        protected void onReturnValue(VirtualFrame frame, Object result) {
            ArrayList<String> arguments = new ArrayList<>();
            Object objectInstance = new Object();

            //Cannot have null values in key. And init time gathering (first call is gathered at time 0).
            if (firstIteration) {
                initialTime = System.nanoTime();
                firstIteration=false;
            }

            if ((System.nanoTime() - initialTime) > warmup) {
                //Get most information from the source function
                if (callMapKey.isEmpty()) {
                    System.out.println("Data gathering started" + (System.nanoTime() - initialTime));
                }
                //Correct results only in javascript

                Key newKey = new Key();
                continueIterate = true;
                Truffle.getRuntime().iterateFrames(frame2 -> {
                    Node node = frame2.getCallNode();
                    if (node != null) {
                        newKey.sourceRootNode = node.getRootNode();
                        newKey.targetRootNode = payload.getNode().getRootNode();
                        newKey.sourceSourceSection = node.getRootNode().getEncapsulatingSourceSection();
                        newKey.targetSourceSection = payload.getNode().getRootNode().getSourceSection();
                        newKey.rootSize = NodeUtil.countNodes(node.getRootNode());
                        newKey.targetSize = NodeUtil.countNodes(payload.getNode().getRootNode());

                        if (useArgumentsFlag){
                            if (frame.getArguments() != null) {
                                for (Object i : frame.getArguments()) {
                                    arguments.add("\"" + i.getClass().toString() + "\"");
                                }
                                newKey.arguments = arguments;
                            }
                        }
                        if (useResultsFlag){
                            newKey.result = result.getClass().toString();
                        }
                        if (useObjectInstancesFlag){
                            if (frame.getArguments() != null) {
                                newKey.objectInstanceKey = frame.getArguments()[0];
                            }
                        }
                        if (useSpecificSourceLocationsFlag){
                            newKey.specifiedSourceLoc = node.getEncapsulatingSourceSection();
                        }
                        if (useTimesFlag){
                            callMapTimes.computeIfAbsent(newKey.hashCode(), k -> new ArrayList<>()).add(System.nanoTime() - initialTime);
                        }
                        callMapKey.putIfAbsent(newKey.hashCode(), newKey);
                        callMapCount.merge(newKey.hashCode(), 1, (a, b) -> a + b);

                        continueIterate = false;

                    }
                    if (continueIterate) {
                        return null;
                    } else {
                        return "stop";
                    }
                });
            }
        }
    }

    static {
        CallTracerInstrument.setFactory(new ProfilerToolFactory<CallTracer>() {
            @Override
            public CallTracer create(Env env) {
                return new CallTracer(env);
            }
        });
    }
}
