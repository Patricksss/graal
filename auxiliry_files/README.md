# callTracer-graalVM

InputLists contains the ordering obtained from querying the graph database and the input for GraalVM compiler.

call_graph_EdgeLists contains the call graphs obtained from the CallTracer tool.

BashCommands listst all bash commands nescesary to run the benchmarks and tools.

CallTracer files are for building and testing the call graph profiling tool.

OptimisedCallTarget implements changes to the compiler such that the InputLists are used.

AutomatedBenchmarking was used to automaticly run the toool and perform all banchmarks.

Visualise is used to create all graphs and data for the report.
