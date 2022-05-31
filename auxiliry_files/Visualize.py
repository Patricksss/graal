import re
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import numpy as np
import os, os.path
import math

def visualizeResults(i,benchname):
    testName = i
    ##save experiment outcome
    DIR = homeDirectory + benchname + "/" + testName + '/'
    numberofBenchmarks = int(math.floor((len(([name for name in os.listdir(DIR) if os.path.isfile(os.path.join(DIR, name))])))/2)-1)
    current_text = homeDirectory + benchname + "/" + testName + '/0.txt'
    textfile = open(current_text, "r").readlines()
    numberOfRuns = 0
    for line in textfile:
        if re.search("cycle in (.*) sec", str(line)) is not None:
            numberOfRuns = numberOfRuns + 1

    speedDev = np.zeros((numberOfRuns,numberofBenchmarks))
    speedCombined = np.zeros((numberOfRuns,numberofBenchmarks))
    meanSpeed = []
    topSpeed = []
    compilations = []
    inlines = []
    queueRemaining = []
    tiers = []
    histTime = [[]*numberofBenchmarks]
    origin = []
    histTimeFinal = []
    for x in range(numberofBenchmarks):
        compileName = []
        queuesizeLive = []
        queuetime = []
        tempTiers = []
        histTimeTemp = []
        current_text = homeDirectory + benchname + "/" + testName + '/' + str(x) + '.txt'
        textfile = open(current_text, "r").readlines()
        run = 0
        prev = 0.0
        #Loop through file with compilation times
        for line in textfile:
            if re.search("cycle in (.*) sec",line) is not None and run < numberOfRuns:
                speedDev[run, x] = 1/float(re.search("cycle in (.*) sec", str(line)).group(1))
                prev = prev +  float(re.search("cycle in (.*) sec", str(line)).group(1))
                speedCombined[run, x] = prev
                run = run + 1

            #if re.search("typescript:  (.*) runs/sec \[average over all", str(line)) is not None:
            #    meanSpeed.append(float(re.search("typescript:  (.*) runs/sec \[average over all", str(line)).group(1)))
            #if re.search("typescript:  (.*) runs/sec \[average over last",str(line)) is not None:
            #    topSpeed.append(float(re.search("typescript:  (.*) runs/sec \[average over last", str(line)).group(1)))

        current_text = homeDirectory + benchname + "/" + testName + '/' + 'results_' + str(x) + '.txt'
        textfile = open(current_text, "r").readlines()
        # Loop through file with compilation details
        for line in textfile:
            if re.search("Queue: Size(.*)", str(line)) is not None:
                queuesizeLive.append(float(re.search("Queue: Size (.*)Change", str(line)).group(1)))
                queuetime.append(float(re.search("Timestamp (.*)\|Src", str(line)).group(1)))
            if re.search(" Compilations (.*)", str(line)) is not None:
                compilations.append(float(re.search(": (.*)", str(line)).group(1)))
            if re.search(" opt done (.*)", str(line)) is not None:
                if re.search("\|Tier (.*)\|Time ", str(line)) is not None:
                    tempTiers.append(int(re.search("\|Tier (.*)\|Time ", str(line)).group(1)))
                if re.search("Timestamp (.*)\|Src", str(line)) is not None:
                    histTimeTemp.append(float(re.search("Timestamp (.*)\|Src", str(line)).group(1)))
                if re.search("\|Src (.*):", str(line)) is not None:
                    origin.append(re.search("\|Src (.*):", str(line)).group(1))
                    if (x == 0):
                        tempReg = re.search("  (.*)\|Tier", str(line)).group(1)[9:]
                compileName.append((tempReg).strip())
                        # if re.search("<split(.*)", str(tempReg)) is not None:
                        #     compileName.append((re.search("(.*)<split", str(tempReg)).group(1)).strip())
                        # else:
                        #     compileName.append((tempReg).strip())
            #if re.search(" Queues (.*)", str(line)) is not None:
            #    queue = float(re.search(": (.*)", str(line)).group(1))
            #if re.search(" Code size (.*)", str(line)) is not None:
            #    splits = float(re.search(", sum=(.*), min=", str(line)).group(1))
            if re.search("Remaining Compilation Queue (.*)", str(line)) is not None:
                queueRemaining.append(float(re.search(": (.*)", str(line)).group(1)))
            #if re.search("Polymorphic(.*)", str(line)) is not None:
            #    poly = float(re.search(", sum=(.*), min=", str(line)).group(1).strip())
            #if re.search("Megamorphic(.*)", str(line)) is not None:
            #    poly = poly + float(re.search(", sum=(.*), min=", str(line)).group(1).strip())
            #if re.search("Truffle loops(.*)", str(line)) is not None:
            #    loops = float(re.search(", sum=(.*), min=", str(line)).group(1).strip())
            #if re.search(" Inlined(.*) ", str(line)) is not None:
            #    inlines.append(float(re.search(", sum=(.*), min=", str(line)).group(1).strip()))
        #Get tier level of compilation
        j = 0
        for s in tempTiers:
            if s == 1:
                j=j+1
        tiers.append(j/len(tempTiers))

        firstCompilation = histTimeTemp[0]
        for y in range(len(histTimeTemp)):
            histTimeFinal.append(histTimeTemp[y]-firstCompilation)

        #plot size of compilation queue
        # if x==0:
        #     firsttime = queuetime[0]
        #     for y in range(len(queuetime)):
        #         queuetime[y] -= firsttime
        #     plt.plot(queuetime, queuesizeLive, label=testName, color='r')
        #     plt.legend()
        #     plt.show()
        ##histogram of copilation speed
        #firsttime = histTimeTemp[x][0]
        #for y in range(histTimeTemp[x]):
    avgCombined = np.average(speedCombined,axis = 1)
    avgCombined_coinf_500 = 1.96 * np.std(speedCombined[499]) / np.sqrt(numberofBenchmarks)
    #confidence intervals
    cis = []
    ##mean speed per
    averageSpeed = np.average(speedDev,axis = 1)
    x_coord = []
    for y in range(numberOfRuns):
        x_coord.append(y)
        cis.append(1.96 * np.std(speedDev[y])/np.sqrt(numberofBenchmarks))
    return [x_coord,cis,averageSpeed,compilations,inlines,queueRemaining,queuesizeLive,queuetime,tiers,origin,compileName,avgCombined,histTimeFinal,avgCombined_coinf_500,speedCombined]

#smoothening function for visualisation
def moving_avg(x,n):
    mv =  np.convolve(x,np.ones(n)/n,mode='valid')
    return np.concatenate(([np.NaN for k in range(n-1)],mv))

def multiple(name,graphName):
    benchname = name + "_results"
    path = "/home/patrick/Pictures/"
    testName = "1__standard_top0_size0_inc0"
    data = visualizeResults(testName, benchname)
    testName2 = "50_noSize_noPriority_top0_size0_inc0"
    data2 = visualizeResults(testName2, benchname)
    testName3 = "50_sqrt_noPriority_top0_size0_inc0"
    data3 = visualizeResults(testName3, benchname)
    testName4 = "50_linear_noPriority_top0_size0_inc0"
    data4 = visualizeResults(testName4, benchname)


    return [data,data2,data3,data4]
def single(name,graphName):
    benchname = name + "_results"

    testName = "1__standard_top0_size0_inc0"
    data = visualizeResults(testName, benchname)
    testName2 = "50_noSize_noPriority_top0_size0_inc0"
    data2 = visualizeResults(testName2, benchname)
    testName3 = "50_sqrt_noPriority_top0_size0_inc0"
    data3 = visualizeResults(testName3, benchname)
    testName4 = "50_linear_noPriority_top0_size0_inc0"
    data4 = visualizeResults(testName4, benchname)

    smooth = 1
    ##speed per excecution
    x_coord = data[11][:500]
    averageSpeed = data[2][:500]
    cis = data[1][:500]
    ##smoothening
    averageSpeed = moving_avg(data[2][:500],smooth)
    cis = moving_avg(data[1][:500],smooth)
    plt.plot(x_coord, averageSpeed, label="Default compilation",color = 'b')
    plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'b')
    x_coord = data2[11]
    averageSpeed = data2[2]
    cis = data2[1]
    ##smoothening
    averageSpeed = moving_avg(data2[2],smooth)
    cis = moving_avg(data2[1],smooth)
    plt.plot(x_coord, averageSpeed, label="Linear size penalty",color = 'r')
    plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'r')
    x_coord = data3[11]
    averageSpeed = data3[2]
    cis = data3[1]
    ##smoothening
    averageSpeed = moving_avg(data3[2],smooth)
    cis = moving_avg(data3[1],smooth)
    plt.plot(x_coord, averageSpeed, label="Square root size penalty",color = 'y')
    plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'g')
    x_coord = data4[11]
    averageSpeed = data4[2]
    cis = data4[1]
    ##smoothening
    averageSpeed = moving_avg(data4[2],smooth)
    cis = moving_avg(data4[1],smooth)
    plt.plot(x_coord, averageSpeed, label="No size penalty",color = 'y')
    plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'y')
    #plt.title(graphName +" benchmark 50 iterations")
    plt.xlabel('Time in seconds')
    plt.ylabel('Number of iterations/second')
    plt.legend()
    plt.savefig(path + "benchmark 500 iterations "+name +'.png', bbox_inches='tight')
    plt.show()

    # zoomed image
    zoomStart = 0
    zoom = 50

    x_coord = data[11][zoomStart:zoom]
    averageSpeed = data[2][zoomStart:zoom]
    cis = data[1][zoomStart:zoom]
    plt.plot(x_coord, averageSpeed, label="Default compilation",color = 'b')
    plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'b')
    x_coord = data2[11][zoomStart:zoom]
    averageSpeed = data2[2][zoomStart:zoom]
    cis = data2[1][zoomStart:zoom]
    plt.plot(x_coord, averageSpeed, label="Linear size penalty",color = 'r')
    plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'r')
    x_coord = data3[11][zoomStart:zoom]
    averageSpeed = data3[2][zoomStart:zoom]
    cis = data3[1][zoomStart:zoom]
    plt.plot(x_coord, averageSpeed, label="Square root size penalty",color = 'g')
    plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'g')
    x_coord = data4[11][zoomStart:zoom]
    averageSpeed = data4[2][zoomStart:zoom]
    cis = data4[1][zoomStart:zoom]
    #plt.title(graphName +" benchmark 50 iterations")
    plt.plot(x_coord, averageSpeed, label="No size penalty",color = 'y')
    plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'y')
    #plt.title(graphName +" benchmark 50 iterations")
    plt.xlabel('Time in seconds')
    plt.ylabel('Number of iterations/second')
    plt.legend()
    plt.savefig(path + "benchmark 50 iterations "+name +'.png', bbox_inches='tight')
    plt.show()

    ##compilations
    top = data[3]
    top2 = data2[3]
    top3 = data3[3]
    top4 = data4[3]
    dataTotal3 = [top,top2,top3,top4]
    fig, ax = plt.subplots()
    #ax.title.set_text('Total number of compilations for '+graphName)
    ax.boxplot(dataTotal3,labels = ["Default","Linear","Square root","No penalty"])
    plt.ylabel('Number of compilations')
    plt.savefig(path + "Total number of compilations "+name +'.png', bbox_inches='tight')
    plt.show()
    #
    # #inlines
    # top = data[4]
    # top2 = data2[4]
    # dataTotal3 = [top,top2]
    # fig, ax = plt.subplots()
    # ax.title.set_text('Inlines')
    # ax.boxplot(dataTotal3,labels = [testName,testName2])
    # plt.show()
    #
    # ##remaining queue
    # top = data[5]
    # top2 = data2[5]
    # dataTotal3 = [top,top2]
    # fig, ax = plt.subplots()
    # ax.title.set_text('Queue remaining')
    # ax.boxplot(dataTotal3,labels = [testName,testName2])
    # plt.show()

    ##print list of compile diference
    # print(set(data3[10])-set(data[10]))
    # print(set(data3[10]))

    #compilation tiers (1 vs 2)
    top = data[8]
    top2 = data2[8]
    top3 = data3[8]
    top4 = data4[8]
    dataTotal3 = [top,top2,top3,top4]
    fig, ax = plt.subplots()
    #ax.title.set_text('Percentage tier 1 compilations for '+graphName)
    ax.boxplot(dataTotal3,labels = ["Default","Linear","Square root","No penalty"])
    plt.ylabel('Percentage')
    plt.savefig(path + "Percentage tier 1 compilations " +name+'.png', bbox_inches='tight')

    plt.show()


    ##compile locations (builtin vs javascript vs other)
    top = data[9]
    topTemp = []
    for x in top:
        if x == name + '.js':
            topTemp.append(graphName)
        elif(x == '<builtin>'):
            topTemp.append(x)
        else:
            topTemp.append('other')
    top2 = data2[9]
    topTemp2 = []
    for x in top2:
        if x == name + '.js':
            topTemp2.append(graphName)
        elif(x == '<builtin>'):
            topTemp2.append(x)
        else:
            topTemp2.append('other')
    top3 = data3[9]
    topTemp3 = []
    for x in top3:
        if x == name + '.js':
            topTemp3.append(graphName)
        elif(x == '<builtin>'):
            topTemp3.append(x)
        else:
            topTemp3.append('other')
    top4 = data4[9]
    topTemp4 = []
    for x in top4:
        if x == name + '.js':
            topTemp4.append(graphName)
        elif(x == '<builtin>'):
            topTemp4.append(x)
        else:
            topTemp4.append('other')

    dataTotal3 = [topTemp,topTemp2,topTemp3,topTemp4]
    fig, ax = plt.subplots()
    #ax.title.set_text('Source location of compiled functions for '+graphName)
    plt.hist(dataTotal3, weights=[len(top)*[0.05],len(top2)*[0.05],len(top3)*[0.05],len(top4)*[0.05]],label=["Default compilation","Linear size penalty","Square root size penalty","No size penalty"])
    #plt.legend(loc='upper right')
    plt.legend()
    plt.xlabel('Origin of compiled function')
    plt.ylabel('Average number of compiled functions per iteration')
    plt.savefig(path + "Source location of compiled functions " +name+'.png', bbox_inches='tight')

    plt.show()

    combined1 = data[11]
    combined2 = data2[11]
    combined3 = data3[11]
    combined4 = data4[11]

    print(data[13])

    plt.figure(figsize=(8,6))
    plt.errorbar(x=data[11][499], y=0, xerr=data[13], color="r", capsize=20,elinewidth=3,capthick=3,marker="s", markersize=5)
    plt.errorbar(x=data2[11][499], y=1, xerr=data2[13], color="r", capsize=20,elinewidth=3,capthick=3,marker="s", markersize=5)
    plt.errorbar(x=data3[11][499], y=2, xerr=data3[13], color="r", capsize=20,elinewidth=3,capthick=3,marker="s", markersize=5)
    plt.errorbar(x=data4[11][499], y=3, xerr=data4[13], color="r", capsize=20,elinewidth=3,capthick=3,marker="s", markersize=5)
    plt.eventplot([combined1,combined2,combined3,combined4], orientation="horizontal", linelengths=0.8, linewidths=0.5)
    plt.yticks(ticks = [0,1,2,3],labels = ["Default","Linear","Square root","No penalty"])
    plt.xlabel('Time in seconds')
    #plt.title('Average time completion of each iteration for '+graphName)
    plt.savefig(path + "Average time finish for each iteration " +name+'.png', bbox_inches='tight')
    plt.show()

    #norm = [float(i)/max(raw) for i in raw]
    #[float(i)/max(data[12]) for i in data[12]]
    for x in range(len(data[12])):
        data[12][x] = data[12][x]/1000000000
    for x in range(len(data2[12])):
        data2[12][x] = data2[12][x]/1000000000
    for x in range(len(data3[12])):
        data3[12][x] = data3[12][x]/1000000000
    for x in range(len(data4[12])):
        data4[12][x] = data4[12][x]/1000000000

    top = data[12]
    top2 = data2[12]
    top3 = data3[12]
    top4 = data4[12]

    plt.figure(figsize=(8,6))
    plt.hist([top,top2,top3,top4], weights=[len(top)*[0.05],len(top2)*[0.05],len(top3)*[0.05],len(top4)*[0.05]],bins=10, label =["Default compilation","Linear size penalty","Square root size penalty","No size penalty"])
    plt.legend()
    plt.xlabel('Time in seconds')
    plt.ylabel('Average number of compilations')
    plt.savefig(path + "Source location of compiled functions " +name+'.png', bbox_inches='tight')
    plt.show()

def ActivateMultiple():
    for i,item in enumerate(drawList):
        dataFinal[i] = multiple(item,nameList[i])
    endSpeed50=[[],[],[]]
    endSpeed500=[[],[],[]]
    # endSpeed50=[[]]
    # endSpeed500=[[]]
    for i,item in enumerate(dataFinal):
        minSpeed50 = item[0][11][9]
        minSpeed500 = item[0][11][499]
        standard_percent_50 = item[0][14][9]
        standard_percent_500 = item[0][14][499]
        numberOfCompileListStandard = item[0][8]
        for j in range(len(endSpeed50)):
            print("new")
            print(round(((minSpeed50/item[j+1][11][9])-1)*100,1))
            print(round((1.96*np.std((standard_percent_50/item[j+1][14][9])-1) / np.sqrt(len(standard_percent_50))*100),1))
            # print(round(((minSpeed500/item[j+1][11][499])-1)*100,1))
            # print(round((1.96 * np.std(standard_percent_500 / item[j + 1][14][499] - 1) / np.sqrt(len(standard_percent_500))*100),1))
            endSpeed50[j].append(((minSpeed50/item[j+1][11][9])-1)*100)
            endSpeed500[j].append(((minSpeed500/item[j+1][11][499])-1)*100)
    FinalResults50 = []
    FinalResults500 = []
    for j in range(len(endSpeed50)):
        print("final")
        print(np.average(endSpeed50[j]))
        print(1.96 * np.std(endSpeed50[j]) / np.sqrt(len(endSpeed50[j])))
        #print(np.average(endSpeed500[j]))
        #print(1.96 * np.std(endSpeed500[j]) / np.sqrt(len(endSpeed500[j])))

    name = "total_results"
    path = "/home/patrick/Pictures/new2/"

    fig, ax = plt.subplots()
    ax.boxplot(endSpeed50,labels = ["Linear","Square root","No penalty"])
    plt.ylabel("Percentage versus default strategy.")
    plt.savefig(path + name+'.png', bbox_inches='tight')
    plt.show()

    fig, ax = plt.subplots()
    ax.boxplot(endSpeed500,labels = ["Linear","Square root","No penalty"])
    plt.ylabel("Percentage versus default strategy.")
    plt.savefig(path + name+'.png', bbox_inches='tight')
    plt.show()

##setup
homeDirectory = '/home/patrick/Graalvm/graaljs/graal-js/'

drawList = ["prettier","terser","chai","espree","typescript","acorn","babel-minify","babylon","buble","esprima","jshint","source-map","prepack","postcss"]
nameList = ["prettier","terser","chai","espree","typescript","acorn","babel-minify","babylon","buble","esprima","jshint","source-map","prepack","postcss"]

path = "/home/patrick/Pictures/new/"

dataFinal = [[]*4]*len(drawList)
#dataFinal = [[]*2]*len(drawList)

# single(name,graphName)
ActivateMultiple()