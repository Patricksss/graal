# callTracer-graalVM
Instrument to create a call graph for projects run on graalvm.


Graal commit version (feb 12): 92aec74112376782179ebc40d86978a5e9609496

Graaljs commit version (feb 12): 4c9ec6e13510460079a398abc1eccadad5e5879d
## How to install and run

Install GraalJS and build: https://github.com/oracle/graaljs/blob/master/docs/Building.md

Install Graal tools: https://github.com/oracle/graal/tree/master/tools

Place `callTracer.java`,
`CallTracerCLI.java`,
`CallTracerInstrument.java` into tools directory:

`~/graalvm/graal/tools/src/com.oracle.truffle.tools.profiler/src/com/oracle/truffle/tools/profiler`

place `callTracerTest` into test directory:

`~/graalvm/graal/tools/src/com.oracle.truffle.tools.profiler.test/src/com/oracle/truffle/tools/profiler/test`

Build new instrument and run tests:

```
cd graalvm/graal/tools
mx --dy /graal-js build
mx unittest --dy /graal-js --suite tools --verbose
```

Go to GraalJS repository:

```
cd graalvm/graaljs/graal-js
```
Run the following command to get call graph of Typescript compiler in csv format.

```
mx --dy /tools js --vm.Xms4g --calltracer --calltracer.Output=CSV --calltracer.OutputFile=output.csv typescript.js
```
Other options such as arguments and object instances can be enabled by adding options: `--calltracer.TraceArguments`

All options can be found by `mx --dy /tools --help:tools`.

## Running queries on output CSV

Install Neo4j Community Server: `Neo4j 4.3.7 (tar)`: https://neo4j.com/download-center/#community

Place CSV of interest in `neo4j-community-4.3.7/import` directory.

Enter bin directory:

```
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.13.0.8-4.el8_5.x86_64
export PATH=$JAVA_HOME/bin:$PATH
./neo4j start
./cypher-shell
```

Now we can run Cypher queries. Cypher queries are included in: https://github.com/Patricksss/callTracer-graalVM/blob/main/Queries.cypher

### Optional settings required for some options/queries

From `neo4j-community-4.3.7/labs` directory move `apoc-4.3.0.3-core.jar` to `neo4j-community-4.3.7/plugins` directory.

Enter `neo4j-community-4.3.7/conf` directory and edit `neo4j.conf`:

Add to file: `dbms.import.csv.buffer_size=33554432`

Add to file: `apoc.export.file.enabled=true`

change `dbms.jvm.additional=-Djdk.nio.maxCachedBufferSize=` to: `dbms.jvm.additional=-Djdk.nio.maxCachedBufferSize=2097152`

## Use query results to optimise compiler
Search for `foo.csv` and `inline.csv` in `OptimizedCallTarget.java` and change paths.

Take `OptimizedCallTarget.java` and replace it in `~/Graalvm/graal/compiler/src/org.graalvm.compiler.truffle.runtime/src/org/graalvm/compiler/truffle/runtime`.

Place `foo.csv` and `inline.csv` in `~/Graalvm/graaljs/graal-js`.

Build the updated compiler: `mx --dy /compiler build`.

And we can run single threaded benchmarks with: 
`mx --dy /compiler js --experimental-options --engine.CompilerThreads=1 --engine.TraceCompilation typescript.js`.

## Use updatedBench.py to automaticly run Benchmarks

Place `neo4j-community-4.3.7` folder into `Graalvm` directory.

Change `path` and `SUpass` (needed to run cpuset).
