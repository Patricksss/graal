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
package com.oracle.truffle.tools.profiler.impl;

import java.io.PrintStream;

import org.graalvm.options.OptionDescriptors;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Instrument;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.tools.profiler.CallTracer;

/**
 * The {@linkplain TruffleInstrument instrument} for the CPU tracer.
 *
 * @since 0.30
 */
@TruffleInstrument.Registration(id = CallTracerInstrument.ID, name = "CPU Tracer", version = CallTracerInstrument.VERSION, services = {CallTracer.class})
public class CallTracerInstrument extends TruffleInstrument {

    /**
     * Default constructor.
     *
     * @since 0.30
     */
    public CallTracerInstrument() {
    }

    /**
     * A string used to identify the tracer, i.e. as the name of the tool.
     *
     * @since 0.30
     */
    public static final String ID = "calltracer";

    static final String VERSION = "0.3.0";
    private boolean enabled;
    private CallTracer tracer;
    private static ProfilerToolFactory<CallTracer> factory;

    /**
     * Sets the factory which instantiates the {@link CallTracer}.
     *
     * @param factory the factory which instantiates the {@link CallTracer}.
     * @since 0.30
     */
    public static void setFactory(ProfilerToolFactory<CallTracer> factory) {
        if (factory == null || !factory.getClass().getName().startsWith("com.oracle.truffle.tools.profiler")) {
            throw new IllegalArgumentException("Wrong factory: " + factory);
        }
        CallTracerInstrument.factory = factory;
    }

    static {
        // Be sure that the factory is initialized:
        try {
            Class.forName(CallTracer.class.getName(), true, CallTracer.class.getClassLoader());
        } catch (ClassNotFoundException ex) {
            // Can not happen
            throw new AssertionError();
        }
    }

    /**
     * Does a lookup in the runtime instruments of the engine and returns an instance of the
     * {@link CallTracer}.
     *
     * @since 0.33
     */
    public static CallTracer getTracer(Engine engine) {
        Instrument instrument = engine.getInstruments().get(ID);
        if (instrument == null) {
            throw new IllegalStateException("Tracer is not installed.");
        }
        return instrument.lookup(CallTracer.class);
    }

    /**
     * Called to create the Instrument.
     *
     * @param env environment information for the instrument
     * @since 0.30
     */
    @Override
    protected void onCreate(Env env) {

        tracer = factory.create(env);
        enabled = env.getOptions().get(CallTracerCLI.ENABLED);
        OptionValues options = env.getOptions();
         if (enabled) {
            try {
                tracer.setFilter(getSourceSectionFilter(env));
                tracer.setArguments(options.get(CallTracerCLI.TRACE_ARGUMENTS));
                tracer.setResults(options.get(CallTracerCLI.TRACE_RESULTS));
                tracer.setObjectInstances(options.get(CallTracerCLI.TRACE_OBJECTS));
                tracer.setSpecificSourceLocations(options.get(CallTracerCLI.TRACE_LOCATION));
                tracer.setText(options.get(CallTracerCLI.TRACE_TEXT));
                tracer.setTime(options.get(CallTracerCLI.TRACE_TIME));
            } catch (IllegalArgumentException e) {
                new PrintStream(env.err()).println(ID + " error: " + e.getMessage());
                enabled = false;
                tracer.setCollecting(false);
                env.registerService(tracer);
                return;
            }
            tracer.setCollecting(true);
        }
        env.registerService(tracer);
    }

    /**First, it dmx -defines SourceSectionFilter. This filter is a declarative definition of the parts of
     * the source code we are interested in. In our example, we care about all nodes that are considered expressions,
     * and we do not care about internal language parts.
     */

     private static SourceSectionFilter getSourceSectionFilter(Env env) {
        final boolean internals = env.getOptions().get(CallTracerCLI.TRACE_INTERNAL);
        final WildcardFilter filterRootName = env.getOptions().get(CPUTracerCLI.FILTER_ROOT);
        final WildcardFilter filterFile = env.getOptions().get(CPUTracerCLI.FILTER_FILE);
        final String filterMimeType = env.getOptions().get(CallTracerCLI.FILTER_MIME_TYPE);
        final String filterLanguage = env.getOptions().get(CallTracerCLI.FILTER_LANGUAGE);
        return CallTracerCLI.buildFilter(true, false, false, internals, filterRootName, filterFile, filterMimeType, filterLanguage);
    }

    /**
     * @return A list of the options provided by the {@link CallTracer}.
     * @since 0.30
     */
    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return new CallTracerCLIOptionDescriptors();
    }

    /**
     * Called when the Instrument is to be disposed.
     *
     * @param env environment information for the instrument
     * @since 0.30
     */
    @Override
    protected void onDispose(Env env) {
        if (enabled) {
            CallTracerCLI.handleOutput(env, tracer);
            tracer.close();
        }
    }
}
