# callTracer-graalVM
Instrument to create a call graph for projects run on GraalVM and use these call graphs to optimise GraalVM.

Graaljs commit version (Apr 14): c6fc92e7367004b1b952ce21da7c8a13f3f436a1

Mx commit version (Apr 9): 65c6727a0080515696c25934f3da02ef7a1aa43a

Install GraalJS and build: https://github.com/oracle/graaljs/blob/master/docs/Building.md

Install Graal tools: https://github.com/oracle/graal/tree/master/tools

All changes to the main repo can be found in https://github.com/Patricksss/graal/tree/master/auxiliry_files

## Use query results to improve GraalVM performance

Taking as example the TypeScript benchmark.

Benchmark can be found under: `https://github.com/Patricksss/graal/tree/master/auxiliry_files/benchmarks`.

Example input files under: `https://github.com/Patricksss/graal/tree/master/auxiliry_files/InputLists/typescript_inputs`.

Now we can run the benchmark with an improved compiler strategy with:

`mx --dy /compiler js --experimental-options --vm.DcallTarget.useGraph=true --vm.DcallTarget.inputCompile="input.csv" --engine.TraversingCompilationQueue=false engine.PriorityQueue=false --engine.CompilerThreads=1 --engine.Inlining=true typescript.js`.

## Use calltracer tool to create call graph of JavaScript program.
Run the following command to get call graph of TypeScript compiler in csv format.

```mx --dy /tools js --vm.Xms4g --calltracer --calltracer.Output=CSV --calltracer.OutputFile=output.csv typescript.js```

Additional information such as arguments and object instances can be enabled by adding options: `--calltracer.TraceArguments`

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
Example Cypher queries are in: `https://github.com/Patricksss/graal/blob/master/auxiliry_files/Queries.cypher`


[![GraalVM](.github/assets/logo_320x64.svg)][website]

[![GraalVM downloads][badge-dl]][downloads] [![GraalVM docs][badge-docs]][docs] [![GraalVM on Slack][badge-slack]][slack] [![GraalVM on Twitter][badge-twitter]][twitter] [![GraalVM on YouTube][badge-yt]][youtube]  [![GraalVM Gate][badge-gate]][gate] [![License][badge-license]](#license)

GraalVM is a high-performance JDK distribution designed to accelerate the execution of applications written in Java and other JVM languages along with support for JavaScript, Ruby, Python, and a number of other popular languages.

The project website at [https://www.graalvm.org/][website] describes how to [get started][getting-started], how to [stay connected][community], and how to [contribute][contributors].

## Documentation

Please refer to the [GraalVM website for documentation][docs]. You can find most of the documentation sources in the [`docs/`](docs/) directory in the same hierarchy as displayed on the website. Additional documentation including developer instructions for individual components can be found in corresponding `docs/` sub-directories. The documentation for the Truffle framework, for example, is in [`truffle/docs/`](truffle/docs/). This also applies to languages, tools, and other components maintained in [related repositories](#related-repositories).

## Get Support

* Open a [GitHub issue][issues] for bug reports, questions, or requests for enhancements.
* Join the [GraalVM Slack][slack] to connect with the community and the GraalVM team.
* Report a security vulnerability according to the [Reporting Vulnerabilities guide][reporting-vulnerabilities].

## Repository Structure

This source repository is the main repository for GraalVM and includes the following components:

Directory | Description
------------ | -------------
[`.github/`](.github/) | Configuration files for GitHub issues, workflows, ….
[`compiler/`](compiler/) | [Graal compiler][reference-compiler], a modern, versatile compiler written in Java.
[`espresso/`](espresso/) | [Espresso][java-on-truffle], a meta-circular Java bytecode interpreter for the GraalVM.
[`java-benchmarks/`](java-benchmarks/) | Java benchmarks.
[`regex/`](regex/) | TRegex, a regular expression engine for other GraalVM languages.
[`sdk/`](sdk/) | [GraalVM SDK][graalvm-sdk], long-term supported APIs of GraalVM.
[`substratevm/`](substratevm/) | Framework for ahead-of-time (AOT) compilation with [Native Image][native-image].
[`sulong/`](sulong/) | [Sulong][reference-sulong], an engine for running LLVM bitcode on GraalVM.
[`tools/`](tools/) | Tools for GraalVM languages implemented with the instrumentation framework.
[`truffle/`](truffle/) | GraalVM's [language implementation framework][truffle] for creating languages and tools.
[`vm/`](vm/) | Components for building GraalVM distributions.
[`wasm/`](wasm/) | [GraalWasm][reference-graalwasm], an engine for running WebAssembly programs on GraalVM.

## Related Repositories

GraalVM provides additional languages, tools, and other components developed in related repositories. These are:

Name         | Description
------------ | -------------
[FastR] | Implementation of the R language.
[GraalJS] | Implementation of JavaScript and Node.js.
[GraalPython] | Implementation of the Python language.
[GraalVM Demos][graalvm-demos] | Several example applications illustrating GraalVM capabilities.
[Native Build Tools][native-build-tools] | Build tool plugins for GraalVM Native Image.
[SimpleLanguage] | A simple example language built with the Truffle framework.
[SimpleTool] | A simple example tool built with the Truffle framework. 
[TruffleRuby] | Implementation of the Ruby language.
[VS Code Extensions][vscode-extensions] | VS Code extensions for GraalVM.

## License

GraalVM Community Edition is open source and distributed under [version 2 of the GNU General Public License with the “Classpath” Exception](LICENSE), which are the same terms as for Java. The licenses of the individual GraalVM components are generally derivative of the license of a particular language (see the table below). GraalVM Community is free to use for any purpose - no strings attached.

Component(s) | License
------------ | -------------
[Espresso](espresso/LICENSE) | GPL 2
[GraalVM Compiler](compiler/LICENSE.md), [SubstrateVM](substratevm/LICENSE), [Tools](tools/LICENSE), [VM](vm/LICENSE_GRAALVM_CE) | GPL 2 with Classpath Exception
[GraalVM SDK](sdk/LICENSE.md), [GraalWasm](wasm/LICENSE), [Truffle Framework](truffle/LICENSE.md), [TRegex](regex/LICENSE.md) | Universal Permissive License
[Sulong](sulong/LICENSE) | 3-clause BSD


[badge-dl]: https://img.shields.io/badge/download-latest-blue
[badge-docs]: https://img.shields.io/badge/docs-read-green
[badge-gate]: https://github.com/oracle/graal/actions/workflows/main.yml/badge.svg
[badge-license]: https://img.shields.io/badge/license-GPLv2+CE-green
[badge-slack]: https://img.shields.io/badge/Slack-join-active?logo=slack
[badge-twitter]: https://img.shields.io/badge/Twitter-@graalvm-active?logo=twitter
[badge-yt]: https://img.shields.io/badge/YouTube-subscribe-active?logo=youtube
[community]: https://www.graalvm.org/community/
[contributors]: https://www.graalvm.org/community/contributors/
[docs]: https://www.graalvm.org/docs/introduction/
[downloads]: https://www.graalvm.org/downloads/
[fastr]: https://github.com/oracle/fastr
[gate]: https://github.com/oracle/graal/actions/workflows/main.yml
[getting-started]: https://www.graalvm.org/docs/getting-started/
[graaljs]: https://github.com/oracle/graaljs
[graalpython]: https://github.com/oracle/graalpython
[graalvm-demos]: https://github.com/graalvm/graalvm-demos
[graalvm-sdk]: https://www.graalvm.org/sdk/javadoc/
[issues]: https://github.com/oracle/graal/issues
[java-on-truffle]: https://www.graalvm.org/reference-manual/java-on-truffle/
[native-build-tools]: https://github.com/graalvm/native-build-tools
[native-image]: https://www.graalvm.org/reference-manual/native-image/
[reference-compiler]: https://www.graalvm.org/reference-manual/compiler/
[reference-graalwasm]: https://www.graalvm.org/reference-manual/wasm/
[reference-sulong]: https://www.graalvm.org/reference-manual/llvm/
[reporting-vulnerabilities]: https://www.oracle.com/corporate/security-practices/assurance/vulnerability/reporting.html
[simplelanguage]: https://github.com/graalvm/simplelanguage
[simpletool]: https://github.com/graalvm/simpletool
[slack]: https://www.graalvm.org/slack-invitation/
[truffle]: https://www.graalvm.org/graalvm-as-a-platform/language-implementation-framework/
[truffleruby]: https://github.com/oracle/truffleruby
[twitter]: https://twitter.com/graalvm
[vscode-extensions]: https://github.com/graalvm/vscode-extensions
[website]: https://www.graalvm.org/
[youtube]: https://www.youtube.com/graalvm
