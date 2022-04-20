# This is a sample Python script.

# Press Shift+F10 to execute it or replace it with your code.
# Press Double Shift to search everywhere for classes, files, tool windows, actions, and settings.
#
#todo: Use python to build everything?

from neo4j import GraphDatabase
import time
import subprocess
import re
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import numpy as np
import math

def tempInput():
    uri = "neo4j://localhost:7687"
    driver = GraphDatabase.driver(uri)
    ##start database
    ##Needs java 11
    command = "cd " + \
              "&& cd " + path + "/neo4j-community-4.3.7/bin " + \
              "&& export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.14.1.1-2.el8_5.x86_64 " + \
              "&& export PATH=$JAVA_HOME/bin:$PATH" + \
              "&& ./neo4j start"
    subprocess.call(command
                    , shell=True
                    )

    with driver.session() as session:
        session.write_transaction(tempInput2)
    ##stop database
    command = "cd " + \
              "&& cd " + path + "/neo4j-community-4.3.7/bin " + \
              "&& ./neo4j stop"
    subprocess.call(command
                    , shell=True
                    )
def tempInput2(tx):
    # resultLength = tx.run("MATCH (a) "
    #                       "RETURN a ")
    # totalFunctions = int(20 / 100 * len([record for record in resultLength]))

    # query = "MATCH (a)-[r:Call]->() " + \
    #         "RETURN SUM(r.Count) as out, r.SourceLocation as id, a.inc as count, AVG(r.rootSize) as size;"
    # name = "foo"
    # result = tx.run(query)
    # dataList = [record for record in result]
    # order = []
    # count = []
    # id = []
    # for record in dataList:
    #     order.append(record["out"]*(1/math.sqrt(record["size"])))
    #     count.append(record["count"])
    #     id.append(record['id'])
    # frame1 = pd.DataFrame(data = [order,count,id],index = ["order","count", "id"])

    # query = "MATCH ()-[r:Call]->(b) " + \
    #         "WITH SUM(r.Count) as out, r.TargetLocation as id, AVG(r.rootSize) as size, a.inc as count, SUM(r.Count)/a.inc as ratio " + \
    #         "RETURN out, id, size, count, out/count as ratio " + \
    #         "ORDER BY count DESC " + \
    #         "LIMIT $totalFunctions;"


    # query = "MATCH (a)-[r:Call]->() " + \
    #         "WITH SUM(r.Count) as out, r.TargetLocation as id, AVG(r.rootSize) as size, a.inc as count, SUM(r.Count)/a.inc as ratio " + \
    #         "WHERE ratio>5 AND out>1000 " + \
    #         "RETURN out, id, size, count, ratio;"
    # result = tx.run(query)
    # dataList = [record for record in result]
    # order = []
    # count = []
    # id = []
    # for record in dataList:
    #     ##square
    #     #order.append(record["count"] * (1 / (record["size"]*record["size"])))
    #     ##xlogx
    #     ##order.append(record["count"] * (1 / (record["size"]*math.log(record["size"]))))
    #     ##sqrt
    #     #order.append(record["count"] * (1 / math.sqrt(record["size"])))
    #     ##noSqrt
    #     #order.append(record["count"] * (1 / record["size"]))
    #     ##noSize
    #     order.append(record["out"])
    #     count.append(record["count"])
    #     id.append(record['id'])
    # frame1 = pd.DataFrame(data=[order, count, id], index=["order", "count", "id"]).T


    name = "foo"
    query = "MATCH ()-[r:Call]->(b) " + \
            "WITH SUM(r.Count) as count, r.TargetLocation as id, r.targetNode as TargetName, AVG(r.targetSize) as size " + \
            "RETURN count,id,TargetName,size; "
    result = tx.run(query)
    dataList = [record for record in result]
    order = []
    count = []
    id = []
    idName = []
    for record in dataList:
        ##square
        if (types == "square"):
            order.append(record["count"] * (1 / (record["size"]*record["size"])))
        ##xlogx
        elif (types == "xSqrt"):
            order.append(record["count"] * (1 / (record["size"]*math.sqrt(record["size"]))))
        ##sqrt
        elif (types == "sqrt"):
            order.append(record["count"] * (1 / math.sqrt(record["size"])))
        ##noSqrt
        elif (types == "linear"):
            order.append(record["count"] * (1 / record["size"]))
        ##noSize
        else:
            order.append(record["count"])
        count.append(record["count"])
        id.append(record['id'])
        idName.append(record['TargetName'])

    frame2 = pd.DataFrame(data = [order,count,id,idName],index = ["order","count", "id","idName"])
    frame = frame2.sort_values(by=["order"], axis=1,ascending=False).T.reset_index(drop=True)

    #frame = pd.concat([frame1,frame2],axis=0,ignore_index=True)
    #print(frame)
    #frame = frame.drop_duplicates(subset=['id']).T
    nodeCounts = []
    upd = 1 / (speed*3)
    k = 0
    increases = np.linspace(0, 0.1, len(dataList))
    for x in range(len(dataList)):
        #print(frame['order'][x])
        # print(nodeCounts)
        #print(frame[x])
        if frame['count'][x] is None:
            print("empty")
        else:
            #print(round(frame['count'][x]))
            nodeCounts.append(str(round(frame['count'][x]*k)+1) + "@@" + str(frame['id'][x]) + str(frame['idName'][x]))
        k=k+upd
    textname = name + ".csv"
    textfile = open(textname, "a")
    for element in nodeCounts:
        textfile.write(element + "\n")
    textfile.close()
    ## create out for every node
    name = "inlined"
    query = " MATCH (a) WHERE NOT (a)-[]->() AND a.distinct < " + str(maxInc+1) + " AND a.size < "+str(maxSize)+" RETURN DISTINCT a.id, a.inc,a.location ORDER BY a.inc DESC LIMIT " + str(numberOfInlines) +"; "
    #query = " MATCH (a) WHERE a.distinct < 2 AND a.size < 30 RETURN DISTINCT a.id, a.inc,a.location ORDER BY a.inc DESC LIMIT 50; "
    result = tx.run(query)
    dataList = [record for record in result]
    print(dataList)
    count = []
    id = []
    idName = []
    for record in dataList:
        count.append(record["a.inc"])
        id.append(record['a.location'])
        idName.append(record['a.id'])
    frame = pd.DataFrame(data=[count, id, idName], index=["count", "id", "idName"]).T
    nodeCounts = []
    increases = np.linspace(0, 0.1, len(dataList))
    for x in range(len(dataList)):
        # print(frame['order'][x])
        # print(nodeCounts)
        # print(frame[x])
        if frame['count'][x] is None:
            print("empty")
        else:
            # print(round(frame['count'][x]))
            nodeCounts.append(
                str(0) + "@@" + str(frame['id'][x]) + str(frame['idName'][x]))
    textname = name + ".csv"
    textfile = open(textname, "a")
    for element in nodeCounts:
        textfile.write(element + "\n")
    textfile.close()


def CreateInput(tx, topFunctions,name,query):
    resultLength = tx.run("MATCH (a) "
                    "RETURN a ")
    totalFunctions = int(topFunctions/100 * len([record for record in resultLength]))
    print(totalFunctions)
    result = tx.run(query, totalFunctions=totalFunctions)

    ##print(type(result))
    dataList = [record for record in result]
    nodeCounts = []
    for record in dataList:
        # print(nodeCounts)
        if record["count"] is None:
            nodeCounts.append(str(0) + "@@" + str(record['id']))
        else:
            nodeCounts.append(str(record["count"]) + "@@" + str(record['id']))
    textname = name + ".csv"
    textfile = open(textname, "a")
    for element in nodeCounts:
        textfile.write(element + "\n")
    textfile.close()

def runQuery(tx, topFunctions,name,query):
    tx.run(query)

def runDatabaseQuery(queryType,topFunctions,name,query):
    uri = "neo4j://localhost:7687"
    driver = GraphDatabase.driver(uri)
    ##start database
    command = "cd " + \
              "&& cd " + path + "/neo4j-community-4.3.7/bin " + \
              "&& export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.14.1.1-2.el8_5.x86_64 " + \
              "&& export PATH=$JAVA_HOME/bin:$PATH" + \
              "&& ./neo4j start"
    subprocess.call(command
                    , shell=True
                    )
    with driver.session() as session:
        session.write_transaction(queryType,topFunctions,name,query)

    ##stop database
    command = "cd " + \
              "&& cd " + path + "/neo4j-community-4.3.7/bin " + \
              "&& ./neo4j stop"
    subprocess.call(command
                    , shell=True
                    )

def RunBenchmarks(testName,benchname,numberofBenchmarks):
    ##5.43
    ##build graalvm
    ##needs java 17
    buildCommand = 'cd ' + \
              '&& cd Graalvm ' + \
              '&& export PATH=$PWD/mx:$PATH ' + \
              '&& cd graaljs/graal-js ' + \
              '&& export JAVA_HOME=/usr/java/jdk-17.0.1 ' + \
              '&& mx --dy /compiler build'
    subprocess.call(buildCommand, shell=True)
    for x in range(numberofBenchmarks):
        command = 'sudo cset shield -c 0,1,2 ' + \
                  '&& cd ' + \
                  '&& cd Graalvm '+ \
                  '&& export PATH=$PWD/mx:$PATH ' + \
                  '&& cd graaljs/graal-js ' + \
                  '&& export JAVA_HOME=/usr/java/jdk-17.0.1 ' + \
                  '&& su -c \"cset shield -e mx -- --dy /compiler js --experimental-options --engine.TraversingCompilationQueue=false --engine.CompilerThreads=1 --engine.Inlining=true --engine.TraceInlining --engine.TraceCompilationDetails ' + \
                  '--engine.CompilationStatistics --log.file=' + benchname + "/" + testName + '/results_' + str(x) + '.txt benchmarks/' + benchname + '.js\"'
        test_out = subprocess.check_output([command]
                               ,shell=True,input=SUpass)
        output_str = test_out.decode()
        timer_file = open(path + '/graaljs/graal-js/' + benchname + "/" + testName + '/' + str(x) +'.txt', "w")
        for element in output_str.split('\n'):
            timer_file.write(element + "\n")
        timer_file.close()
        ##Cleanup

        cleanupCommand = 'sudo cset shield --reset'
        subprocess.call(cleanupCommand, shell=True)

def changeRuns(numberOfRuns,benchname):
    current_text = path + '/graaljs/graal-js/benchmarks/' + benchname + '.js'
    textfile = open(current_text, "r")
    replacement = ""
    for line in textfile:
        if re.search("minSamples: (.*),", str(line)) is not None:
            typescript = float(re.search("minSamples: (.*),", str(line)).group(1))
            print(int(typescript))
            changes = line.replace(str(int(typescript)), str(numberOfRuns))
            replacement = replacement + changes
            print(changes)
        else:
            replacement = replacement + line
    textfile.close()
    fout = open(path + '/graaljs/graal-js/benchmarks/' + benchname + '.js', "w")
    fout.write(replacement)
    fout.close()

def getCallGraph(benchname):
    buildCommand = 'cd ' + \
              '&& cd Graalvm ' + \
              '&& export PATH=$PWD/mx:$PATH ' + \
              '&& cd graaljs/graal-js ' + \
              '&& export JAVA_HOME=/usr/java/jdk-17.0.1 ' + \
              '&& mx --dy /tools build ' + \
              '&& mx --dy /tools js --vm.Xms4g --experimental-options --calltracer --calltracer.TraceSpecifiedSource --calltracer.TraceInternal=true --calltracer.Output=CSV ' + \
              '--calltracer.OutputFile=callGraphs/' + benchname + '.csv ' + 'benchmarks/' + benchname + '.js'
    subprocess.call(buildCommand, shell=True)
    command = "cd && cd " + path + "/graaljs/graal-js/callGraphs && cp " + benchname + '.csv ' + path + "/neo4j-community-4.3.7/import"
    subprocess.call(command
                    , shell=True
                    )

##Excecute in seperate terminal before testing
# sudo bash -c "echo 0 > /proc/sys/kernel/randomize_va_space"
# sudo bash -c "echo off > /sys/devices/system/cpu/smt/control"
##Inits
path = "/home/patricksss/Graalvm"
##pass needed for cpushield
SUpass = b"7053"

##order testing (sqrt, noSqrt, noSize, xSqrt, square)
type = ["linear","sqrt"]

##Inlining Stuff
maxSizeList = [30,60]
maxIncList = [1]
numberOfInlinesList = [0,100]
##force or non force

#inputSpeed
inputSpeed = [50]
testType = "fifo_internals_warmup_inlineAndCompile"

##Leave empty
name = " "
topFunctions = 1337
##Increase speed by not resetting/rebuilding database and not creating new callgraph
quickBench = False
numberofBenchmarks = 3

testlist = [
"acorn",
]

#"typescript",
#"babylon",
for benchname in testlist:

    if not quickBench:
        # #queryType = CreateInput
        #
        # #remove previous ddatabase
        query = "MATCH (n) DETACH DELETE n;"
        queryType = runQuery
        runDatabaseQuery(queryType,topFunctions,name,query)
        #
        # #set runs to 1 for callgraph
        #numberOfRuns = 3
        #changeRuns(numberOfRuns, benchname)
        #
        # #create callgraph and put in import
        #getCallGraph(benchname)


        ##put callgraph into database
        query = " LOAD CSV WITH HEADERS FROM \'file:///" + benchname + ".csv\' AS row FIELDTERMINATOR \',\'" + \
                " MERGE (a:Node {id:row.sourceNode,location:row.SourceLocation})" + \
                " MERGE (b:Node {id:row.targetNode,location:row.TargetLocation})" + \
                " CREATE (a)-[r:Call]->(b)" + \
                " SET r = row," + \
                " r.Count = toInteger(row.Count)," + \
                " r.rootSize = toInteger(row.rootSize)," + \
                " r.targetSize = toInteger(row.targetSize)," + \
                " r.sourceNode = row.sourceNode," + \
                " r.targetNode = row.targetNode," + \
                " r.SourceLocation = row.SourceLocation," + \
                " r.TargetLocation = row.TargetLocation," + \
                " r.CallLocation = row.CallLocation;"
        queryType = runQuery
        runDatabaseQuery(queryType,topFunctions,name,query)

        ## create inc for every node
        query = " MATCH ()-[r:Call]->(b:Node)" + \
                " WITH SUM(r.Count) as sum,b, AVG(r.targetSize) as size " + \
                " SET b.inc = sum, " \
                "     b.size = size;"
        queryType = runQuery
        runDatabaseQuery(queryType,topFunctions,name,query)

        ## create inc for every node
        query = " MATCH ()-[r:Call]->(b:Node)" + \
                " WITH b, count(DISTINCT r.CallLocation) as distinct " + \
                " SET b.distinct = distinct;"
        queryType = runQuery
        runDatabaseQuery(queryType,topFunctions,name,query)
    #make dir to store results

    #testName = "50_nosqrt_fifo_internals_regex_warmup_truffle_2"

    for types in type:
        for speed in inputSpeed:
            for maxSize in maxSizeList:
                for maxInc in maxIncList:
                    for numberOfInlines in numberOfInlinesList:
                        testName = str(speed) + "_" + str(types) + "_" + str(testType) + "_top" + str(numberOfInlines) + "_size" + str(maxSize) +"_inc" + str(maxInc)
                        command = "cd && cd " + path + "/graaljs/graal-js && mkdir " + benchname
                        subprocess.call(command, shell=True)
                        command = "cd && cd " + path + "/graaljs/graal-js/" + benchname + " && mkdir " + testName
                        subprocess.call(command, shell=True)


                        tempInput()

                        ##move created foo.csv to graaljs folder and remove previous
                        command = "cp foo.csv " + path + "/graaljs/graal-js/" + benchname + "/" + testName
                        subprocess.call(command, shell=True)
                        command = "cd && cd " + path + "/graaljs/graal-js && rm foo.csv"
                        subprocess.call(command, shell=True)
                        command = "mv foo.csv " + path + "/graaljs/graal-js"
                        subprocess.call(command, shell=True)

                        ##create inline list from database
                        #
                        command = "cp inlined.csv " + path + "/graaljs/graal-js/" + benchname + "/" + testName
                        subprocess.call(command, shell=True)
                        command = "cd && cd " + path + "/graaljs/graal-js && rm inlined.csv"
                        subprocess.call(command, shell=True)
                        command = "mv inlined.csv " + path + "/graaljs/graal-js"
                        subprocess.call(command, shell=True)

                        ##run benchmarks
                        numberOfRuns = 500
                        changeRuns(numberOfRuns, benchname)
                        RunBenchmarks(testName,benchname,numberofBenchmarks)


    #
    # ##create foo list from database inc
    # query = "MATCH ()-[r:Call]->(b) " + \
    #         "RETURN SUM(r.Count) as count, r.TargetLocation as id " + \
    #         "ORDER BY count DESC " + \
    #         "LIMIT $totalFunctions;"
    # name = "foo"
    # topFunctions = 70
    # queryType = CreateInput
    # runDatabaseQuery(queryType,topFunctions,name,query)
    #
    # #create foo list from database out
    # query = "MATCH a(a)-[r:Call]->() " + \
    #         "RETURN SUM(r.Count) as inc, r.SourceLocation as id, a.inc as count " + \
    #         "ORDER BY inc DESC " + \
    #         "LIMIT $totalFunctions;"
    # name = "foo"
    # topFunctions = 40
    # queryType = CreateInput
    # runDatabaseQuery(queryType,topFunctions,name,query)

    # create foo list from database inline
    # query = "MATCH (a)-[r:Call]->(b) " + \
    #         "WITH SUM(r.Count) as count,b.location as id, count(DISTINCT r.CallLocation) as distinct " + \
    #         "WHERE distinct = 1 " + \
    #         "RETURN count, id " + \
    #         "ORDER BY count DESC " + \
    #         "LIMIT $totalFunctions;"
    # name = "foo"
    # topFunctions = 50
    # queryType = CreateInput
    # runDatabaseQuery(queryType,topFunctions,name,query)