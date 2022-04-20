import re
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import numpy as np
import os, os.path

def visualizeResults(i,benchname):
    testName = i
    ##save experiment outcome
    DIR = homeDirectory + benchname + "/" + testName + '/'
    numberofBenchmarks = int((len(([name for name in os.listdir(DIR) if os.path.isfile(os.path.join(DIR, name))]))-2)/2)
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
    for x in range(numberofBenchmarks):
        compileName = []
        queuesizeLive = []
        queuetime = []
        tempTiers = []
        histTimeTemp =[]
        current_text = homeDirectory + benchname + "/" + testName + '/' + str(x) + '.txt'
        textfile = open(current_text, "r").readlines()
        run = 0
        prev = 0.0
        #Loop through file with compilation times
        for line in textfile:
            if re.search("cycle in (.*) sec",line) is not None and run < numberOfRuns:
                speedDev[run, x] = 1/float(re.search("cycle in (.*) sec", str(line)).group(1))
                prev = prev + float(re.search("cycle in (.*) sec", str(line)).group(1))
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
                    if (x == 0):
                        origin.append(re.search("\|Src (.*):", str(line)).group(1))
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

        #plot size of compilation queue
        if x==0:
            firsttime = queuetime[0]
            for y in range(len(queuetime)):
                queuetime[y] -= firsttime
            plt.plot(queuetime, queuesizeLive, label=testName, color='r')
            plt.legend()
            plt.show()
        ##histogram of copilation speed
        #firsttime = histTimeTemp[x][0]
        #for y in range(histTimeTemp[x]):

        if x==0:
            plt.hist(histTimeTemp, bins=40,label=testName)
            plt.legend()
            plt.show()
    avgCombined = np.average(speedCombined,axis = 1)

    #confidence intervals
    cis = []
    ##mean speed per
    averageSpeed = np.average(speedDev,axis = 1)
    x_coord = []
    for y in range(numberOfRuns):
        x_coord.append(y)
        cis.append(1.96 * np.std(speedDev[y])/np.sqrt(numberofBenchmarks))
    return [x_coord,cis,averageSpeed,compilations,inlines,queueRemaining,queuesizeLive,queuetime,tiers,origin,compileName,avgCombined]

#smoothening function for visualisation
def moving_avg(x,n):
    mv =  np.convolve(x,np.ones(n)/n,mode='valid')
    return np.concatenate(([np.NaN for k in range(n-1)],mv))

##setup
homeDirectory = '/home/patricksss/Graalvm/graaljs/graal-js/'
benchname = "acorn"

testName = "standard"
data = visualizeResults(testName,benchname)
testName2 = "50_linear_fifo_internals_warmup_inlineAndCompile_top0_size30_inc1"
data2 = visualizeResults(testName2,benchname)
testName3 = "50_sqrt_fifo_internals_warmup_inlineAndCompile_top100_size30_inc1"
data3 = visualizeResults(testName3,benchname)
testName4 = "50_sqrt_fifo_internals_warmup_inlineAndCompile_top100_size60_inc1"
data4 = visualizeResults(testName4,benchname)
smooth = 20

##speed per excecution
x_coord = data[11][:500]
averageSpeed = data[2][:500]
cis = data[1][:500]
##smoothening
averageSpeed = moving_avg(data[2][:500],smooth)
cis = moving_avg(data[1][:500],smooth)
plt.plot(x_coord, averageSpeed, label=testName,color = 'b')
plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'b')
x_coord = data2[11]
averageSpeed = data2[2]
cis = data2[1]
##smoothening
averageSpeed = moving_avg(data2[2],smooth)
cis = moving_avg(data2[1],smooth)
plt.plot(x_coord, averageSpeed, label=testName2,color = 'r')
plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'r')
x_coord = data3[11]
averageSpeed = data3[2]
cis = data3[1]
##smoothening
averageSpeed = moving_avg(data3[2],smooth)
cis = moving_avg(data3[1],smooth)
plt.title(benchname)
plt.plot(x_coord, averageSpeed, label=testName3,color = 'g')
plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'g')
x_coord = data4[11]
averageSpeed = data4[2]
cis = data4[1]
##smoothening
averageSpeed = moving_avg(data4[2],smooth)
cis = moving_avg(data4[1],smooth)
plt.title(benchname)
plt.plot(x_coord, averageSpeed, label=testName4,color = 'y')
plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'y')
plt.legend()
plt.show()

# zoomed image
zoomStart = 0
zoom = 10

x_coord = data[11][zoomStart:zoom]
averageSpeed = data[2][zoomStart:zoom]
cis = data[1][zoomStart:zoom]
plt.plot(x_coord, averageSpeed, label=testName,color = 'b')
plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'b')
x_coord = data2[11][zoomStart:zoom]
averageSpeed = data2[2][zoomStart:zoom]
cis = data2[1][zoomStart:zoom]
plt.plot(x_coord, averageSpeed, label=testName2,color = 'r')
plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'r')
x_coord = data3[11][zoomStart:zoom]
averageSpeed = data3[2][zoomStart:zoom]
cis = data3[1][zoomStart:zoom]
plt.title(benchname)
plt.plot(x_coord, averageSpeed, label=testName3,color = 'g')
plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'g')
x_coord = data4[11][zoomStart:zoom]
averageSpeed = data4[2][zoomStart:zoom]
cis = data4[1][zoomStart:zoom]
plt.title(benchname)
plt.plot(x_coord, averageSpeed, label=testName4,color = 'y')
plt.fill_between(x_coord, (averageSpeed - cis), (averageSpeed + cis), alpha=.1,color = 'y')
plt.legend()
plt.show()

# ##compilations
# top = data[3]
# top2 = data2[3]
# dataTotal3 = [top,top2]
# fig, ax = plt.subplots()
# ax.title.set_text('Compilations')
# ax.boxplot(dataTotal3,labels = [testName,testName2])
# plt.show()
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
print(set(data3[10])-set(data[10]))
print(set(data3[10]))
#compilation tiers (1 vs 2)
top = data[8]
top2 = data2[8]
top3 = data3[8]
dataTotal3 = [top,top2,top3]
fig, ax = plt.subplots()
ax.title.set_text('Percentage tier 1 compilations')
ax.boxplot(dataTotal3,labels = [testName,testName2,testName3])
plt.show()

##compile locations (builtin vs javascript vs other)
top = data[9]
topTemp = []
for x in top:
    if x == benchname + '.js':
        topTemp.append(x)
    elif(x == '<builtin>'):
        topTemp.append(x)
    else:
        topTemp.append('other')
top2 = data2[9]
topTemp2 = []
for x in top2:
    if x == benchname + '.js':
        topTemp2.append(x)
    elif(x == '<builtin>'):
        topTemp2.append(x)
    else:
        topTemp2.append('other')
top3 = data3[9]
topTemp3 = []
for x in top3:
    if x == benchname + '.js':
        topTemp3.append(x)
    elif(x == '<builtin>'):
        topTemp3.append(x)
    else:
        topTemp3.append('other')
top4 = data4[9]
topTemp4 = []
for x in top4:
    if x == benchname + '.js':
        topTemp4.append(x)
    elif(x == '<builtin>'):
        topTemp4.append(x)
    else:
        topTemp4.append('other')

dataTotal3 = [topTemp,topTemp2,topTemp3,topTemp4]
fig, ax = plt.subplots()
ax.title.set_text('Source location of compiled functions')
plt.hist(dataTotal3, label=[testName,testName2,testName3,testName4])
plt.legend(loc='upper right')
plt.show()

combined1 = data[11][:500]
combined2 = data2[11][:500]
combined3 = data3[11][:500]
combined4 = data4[11][:500]

fig, ax = plt.subplots()
ax.eventplot([combined1,combined2,combined3,combined4], orientation="horizontal", linelengths=0.8, linewidths=0.5)
# ax.set(xlim=(0, 8), xticks=np.arange(1, 8),
#       ylim=(0, 8), yticks=np.arange(1, 8))
plt.show()
