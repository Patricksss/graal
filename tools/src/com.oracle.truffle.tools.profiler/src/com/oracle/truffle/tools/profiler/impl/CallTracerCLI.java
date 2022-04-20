package com.oracle.truffle.tools.profiler.impl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;
import org.graalvm.options.OptionType;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.tools.profiler.CallTracer;
import com.oracle.truffle.tools.utils.json.JSONArray;
import com.oracle.truffle.tools.utils.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;

import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.nodes.RootNode;

@Option.Group(CallTracerInstrument.ID)
class CallTracerCLI extends ProfilerCLI {

    enum Output {
        HISTOGRAM,
        JSON,
        CSV,
    }

    static final OptionType<Output> CLI_OUTPUT_TYPE = new OptionType<>("Output",
            new Function<String, Output>() {
                @Override
                public Output apply(String s) {
                    try {
                        return Output.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Output can be: histogram or json");
                    }
                }
            });

    @Option(name = "", help = "Enable the CPU tracer (default: false).", category = OptionCategory.USER, stability = OptionStability.STABLE) //
    static final OptionKey<Boolean> ENABLED = new OptionKey<>(false);

    @Option(name = "TraceArguments", help = "Capture and use arguments as key when tracing (default:false).", category = OptionCategory.USER, stability = OptionStability.STABLE) //
    static final OptionKey<Boolean> TRACE_ARGUMENTS = new OptionKey<>(false);

    @Option(name = "TraceResults", help = "Capture and use returns as key when tracing (default:false).", category = OptionCategory.USER, stability = OptionStability.STABLE) //
    static final OptionKey<Boolean> TRACE_RESULTS = new OptionKey<>(false);

    @Option(name = "TraceObjectInstances", help = "Capture and use Object instances as key when tracing, only in javascript (default:false).", category = OptionCategory.USER, stability = OptionStability.STABLE) //
    static final OptionKey<Boolean> TRACE_OBJECTS = new OptionKey<>(false);

    @Option(name = "TraceSpecifiedSource", help = "Capture and use specified source locations instead of general source root as key when tracing (default:false).", category = OptionCategory.USER, stability = OptionStability.STABLE) //
    static final OptionKey<Boolean> TRACE_LOCATION = new OptionKey<>(false);

    @Option(name = "TraceText", help = "Add function text to output (default:false).", category = OptionCategory.USER, stability = OptionStability.STABLE) //
    static final OptionKey<Boolean> TRACE_TEXT = new OptionKey<>(false);

    @Option(name = "TraceTime", help = "Add excecution time of each call (default:false).", category = OptionCategory.USER, stability = OptionStability.STABLE) //
    static final OptionKey<Boolean> TRACE_TIME = new OptionKey<>(false);

    @Option(name = "TraceInternal", help = "Trace internal elements (default:false).", category = OptionCategory.INTERNAL) //
    static final OptionKey<Boolean> TRACE_INTERNAL = new OptionKey<>(false);

    @Option(name = "FilterRootName", help = "Wildcard filter for program roots. (eg. Math.*) (default: no filter).", usageSyntax = "<filter>", category = OptionCategory.USER, stability = OptionStability.STABLE) //
    static final OptionKey<WildcardFilter> FILTER_ROOT = new OptionKey<>(WildcardFilter.DEFAULT, WildcardFilter.WILDCARD_FILTER_TYPE);

    @Option(name = "FilterFile", help = "Wildcard filter for source file paths. (eg. *program*.sl) (default: no filter).", usageSyntax = "<filter>", category = OptionCategory.USER, stability = OptionStability.STABLE) //
    static final OptionKey<WildcardFilter> FILTER_FILE = new OptionKey<>(WildcardFilter.DEFAULT, WildcardFilter.WILDCARD_FILTER_TYPE);

    @Option(name = "FilterMimeType", help = "Only profile languages with mime-type. (eg. +, default:no filter).", category = OptionCategory.USER, stability = OptionStability.STABLE) //
    static final OptionKey<String> FILTER_MIME_TYPE = new OptionKey<>("");

    @Option(name = "FilterLanguage", help = "Only profile languages with given ID. (eg. js, default:no filter).", category = OptionCategory.USER, stability = OptionStability.STABLE) //
    static final OptionKey<String> FILTER_LANGUAGE = new OptionKey<>("");

    @Option(name = "Output", help = "Print a 'histogram' or 'json' as output (default:HISTOGRAM).", category = OptionCategory.USER, stability = OptionStability.STABLE) //
    static final OptionKey<Output> OUTPUT = new OptionKey<>(Output.HISTOGRAM, CLI_OUTPUT_TYPE);

    @Option(name = "OutputFile", help = "Save output to the given file. Output is printed to output stream by default.", category = OptionCategory.USER, stability = OptionStability.STABLE) //
    static final OptionKey<String> OUTPUT_FILE = new OptionKey<>("");

    public static void handleOutput(TruffleInstrument.Env env, CallTracer tracer) {
        PrintStream out = chooseOutputStream(env, OUTPUT_FILE);
        switch (env.getOptions().get(OUTPUT)) {
            case HISTOGRAM:
                printTracerHistogram(out, tracer);
                break;
            case JSON:
                printTracerJson(out, tracer);
                break;
            case CSV:
                printCSV(out, tracer);
                break;
        }
    }

    private static void printTracerJson(PrintStream out, CallTracer tracer) {
        JSONObject output = new JSONObject();
        output.put("tool", CallTracerInstrument.ID);
        output.put("tool", CallTracerInstrument.ID);
        output.put("version", CallTracerInstrument.VERSION);
        Map<Integer, Integer> callMapCount = new ConcurrentHashMap<>(tracer.getCallMapCount());
        Map<Integer, CallTracer.Key> callMapKey = new ConcurrentHashMap<>(tracer.getCallMapKey());
        Map<Integer, ArrayList<Long>> callMapTimes = new ConcurrentHashMap<>(tracer.getCallMapTimes());
        JSONArray profile = new JSONArray();

        for (Map.Entry<Integer, Integer> entry : callMapCount.entrySet()) {
            JSONObject newEntry = new JSONObject();
            Integer mapKey = entry.getKey();
            newEntry.put("source", callMapKey.get(mapKey).sourceRootNode);
            newEntry.put("target", callMapKey.get(mapKey).targetRootNode);
            String nameSource = callMapKey.get(mapKey).sourceSourceSection.getSource().getName();
            int startLineSource = callMapKey.get(mapKey).sourceSourceSection.getStartLine();
            int endLineSource = callMapKey.get(mapKey).sourceSourceSection.getEndLine();
            int startColumnSource = callMapKey.get(mapKey).sourceSourceSection.getStartColumn();
            int endColumnSource = callMapKey.get(mapKey).sourceSourceSection.getEndColumn();
            String sourceLocation = nameSource + "~" + startLineSource + "-" + endLineSource + ":" + startColumnSource + "-" + endColumnSource;
            //System.out.println(sourceLocation);
            String nameTarget = callMapKey.get(mapKey).targetSourceSection.getSource().getName();
            int startLineTarget = callMapKey.get(mapKey).targetSourceSection.getStartLine();
            int endLineTarget = callMapKey.get(mapKey).targetSourceSection.getEndLine();
            int startColumnTarget = callMapKey.get(mapKey).targetSourceSection.getStartColumn();
            int endColumnTarget = callMapKey.get(mapKey).targetSourceSection.getEndColumn();
            String targetLocation = nameTarget + "~" + startLineTarget + "-" + endLineTarget + ":" + startColumnTarget + "-" + endColumnTarget;

            newEntry.put("source_location", sourceLocation);
            newEntry.put("target_location", targetLocation);
            newEntry.put("count", entry.getValue());
            //String sourceText = callMapKey.get(mapKey).sourceSourceSection.toString().replace(";","");
            //String targetText = callMapKey.get(mapKey).targetSourceSection.toString().replace(";","");
            //ArrayList<Long> excecutionTime = callMapTimes.get(mapKey);

            if (tracer.usingArguments()){
                newEntry.put("arguments", callMapKey.get(mapKey).arguments);
            }
            if (tracer.usingResults()){
                newEntry.put("result", callMapKey.get(mapKey).result.toString());
            }
            if (tracer.usingObjectInstances()){
                newEntry.put("object-instance", callMapKey.get(mapKey).objectInstanceKey.toString());
            }
            if (tracer.usingSpecificSourceLocations()){
                newEntry.put("call_location", getShortDescription(callMapKey.get(mapKey).specifiedSourceLoc));
            }
            profile.put(newEntry);
        }

        output.put("profile", profile);
        out.println(output.toString());
    }

    private static void printCSV(PrintStream out, CallTracer tracer) {
        Map<Integer, Integer> callMapCount = new ConcurrentHashMap<>(tracer.getCallMapCount());
        Map<Integer, CallTracer.Key> callMapKey = new ConcurrentHashMap<>(tracer.getCallMapKey());
        Map<Integer, ArrayList<Long>> callMapTimes = new ConcurrentHashMap<>(tracer.getCallMapTimes());
        String file = ("SourceLocation,TargetLocation,Count,rootSize,targetSize,sourceNode,targetNode,");

        if (tracer.usingText()){
            file = file + "SourceText;TargetText;";
        }
        if (tracer.usingTime()){
            file = file + "Time;";
        }
        if (tracer.usingArguments()){
            file = file + "Arguments;";
        }
        if (tracer.usingResults()){
            file = file + "Return;";
        }
        if (tracer.usingObjectInstances()){
            file = file + "ObjectInstance;";
        }
        if (tracer.usingSpecificSourceLocations()){
            file = file + "CallLocation,";
        }
        out.print(file + "\n");
        for (Map.Entry<Integer, Integer> entry : callMapCount.entrySet()) {
            Integer mapKey = entry.getKey();
            Integer rootSize = callMapKey.get(mapKey).rootSize;
            Integer targetSize = callMapKey.get(mapKey).targetSize;
            String sourceNode = callMapKey.get(mapKey).sourceRootNode.toString().replace(",","h");
            String targetNode = callMapKey.get(mapKey).targetRootNode.toString().replace(",","h");
            String nameSource = callMapKey.get(mapKey).sourceSourceSection.getSource().getName().replace(",","");
	        String nameTarget = callMapKey.get(mapKey).targetSourceSection.getSource().getName().replace(",","");
            int startLineSource = callMapKey.get(mapKey).sourceSourceSection.getStartLine();
            int endLineSource = callMapKey.get(mapKey).sourceSourceSection.getEndLine();
            int startColumnSource = callMapKey.get(mapKey).sourceSourceSection.getStartColumn();
            int endColumnSource = callMapKey.get(mapKey).sourceSourceSection.getEndColumn();
            String sourceLocation = nameSource + "~" + startLineSource + "-" + endLineSource + ":" + startColumnSource + "-" + endColumnSource;
            //System.out.println(sourceLocation);
            int startLineTarget = callMapKey.get(mapKey).targetSourceSection.getStartLine();
            int endLineTarget = callMapKey.get(mapKey).targetSourceSection.getEndLine();
            int startColumnTarget = callMapKey.get(mapKey).targetSourceSection.getStartColumn();
            int endColumnTarget = callMapKey.get(mapKey).targetSourceSection.getEndColumn();
            String targetLocation = nameTarget + "~" + startLineTarget + "-" + endLineTarget + ":" + startColumnTarget + "-" + endColumnTarget;
            //System.out.println(targetLocation);
            Integer count = entry.getValue();
            String temp = (sourceLocation + "," + targetLocation + "," + count + "," + rootSize + "," + targetSize + "," + sourceNode + "," + targetNode + ",");

            if (tracer.usingText()){
           	String sourceText = callMapKey.get(mapKey).sourceSourceSection.toString().replace(";","");
            	String targetText = callMapKey.get(mapKey).targetSourceSection.toString().replace(";","");
                temp = temp + sourceText + ";" + targetText + ";";
            }
            if (tracer.usingTime()){
            	ArrayList<Long> excecutionTime = callMapTimes.get(mapKey);
                temp = temp + excecutionTime + ";";
            }
            if (tracer.usingArguments()){
                ArrayList<String> printArguments = callMapKey.get(mapKey).arguments;
                temp = temp + printArguments + ";";
            }
            if (tracer.usingResults()){
                String returnResults = callMapKey.get(mapKey).result.toString();
                temp = temp + returnResults + ";";
            }
            if (tracer.usingObjectInstances()){
                String objectInstance = callMapKey.get(mapKey).objectInstanceKey.toString();
                temp = temp + objectInstance + ";";
            }
            if (tracer.usingSpecificSourceLocations()){
                String specificSourceLocation = getShortDescription(callMapKey.get(mapKey).specifiedSourceLoc);
                temp = temp + specificSourceLocation + ",";
            }
            out.print(temp + "\n");
        }
    }


    static void printTracerHistogram(PrintStream out, CallTracer tracer) {
        //private CallTracerTest tracertest;
        Map<Integer, Integer> callMapCount = new ConcurrentHashMap<>(tracer.getCallMapCount());
        Map<Integer, CallTracer.Key> callMapKey = new ConcurrentHashMap<>(tracer.getCallMapKey());

        String format2 = " %60s | %5s | %20s | %s";
        String title2 = String.format(format2, "Name", "count","sourceloc","targetloc");

        String sep2 = repeat("-", title2.length());
        long totalCount2 = 0;
        for (Map.Entry<Integer, Integer> entry : callMapCount.entrySet()) {
            totalCount2 += entry.getValue();
        }
        final long finalTotalCount2 = totalCount2;

        out.println(sep2);
        out.println(String.format("Tracing Histogram. Counted a total of %d element executions.", finalTotalCount2));
        out.println("Total Count: Number of times the element was executed and percentage of total executions.");
        out.println(sep2);

        out.println(title2);
        out.println(sep2);
        callMapCount.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEach(entry -> {
            Integer mapKey = entry.getKey();
            String targetKey = callMapKey.get(mapKey).targetRootNode.toString();
            RootNode sourceKey = callMapKey.get(mapKey).sourceRootNode;
            String callNode = (sourceKey + " ---> " + targetKey);

            String sourceLocation = getShortDescription(callMapKey.get(mapKey).sourceSourceSection);
            String targetLocation = getShortDescription(callMapKey.get(mapKey).targetSourceSection);
            String totalSection = sourceLocation;
            String total = String.format("%d %5.1f%%", entry.getValue(), (double) entry.getValue() * 100 / finalTotalCount2);
            Object returnResults = callMapKey.get(mapKey).result;//.toString().substring(callMapKey.get(mapKey).result.toString().length());
            ArrayList<String> printArguments = new ArrayList<>();
            if (callMapKey.get(mapKey).arguments != null) {
                for (int i=0; i<callMapKey.get(mapKey).arguments.size(); i++){
                    if (i>-1){
                        printArguments.add(callMapKey.get(mapKey).arguments.get(i).toString());//.toString().substring(callMapKey.get(mapKey).arguments.get(i).toString().length()));
                    }
                }
            }
            String total2 =" ";// String.format("%d %5.1f%%", entry.getValue(), (double) entry.getValue() * 100 / finalTotalCount);
            out.println(String.format(format2, callNode ,total,totalSection,targetLocation));
        });
        out.println(sep2);
    }

    private static int computeNameLength(Collection<CallTracer.Payload> payloads, int limit) {
        int maxLength = 6;
        for (CallTracer.Payload payload : payloads) {
            int rootNameLength = 0;
            maxLength = Math.max(rootNameLength + 2, maxLength);
            maxLength = Math.min(maxLength, limit);
        }
        return maxLength;
    }
}
