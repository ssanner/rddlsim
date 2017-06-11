#!/bin/csh
#
# use current working directory for input and output - defaults is 
# to use the users home directory
#$ -cwd 
#
# name this job
#$ -N JOB_NAME
#
# send stdout and stderror to this file
#$ -o JOB_NAME.out
#$ -e JOB_NAME.err
#$ -j y
#
# select queue - if needed 
#$ -hard
#$ -l h=compute-*,m_mem_free=12G
#
# compute-1*|compute-2*|compute-3*|compute-4*|compute-5*
hostname
date
free -g
#limit memoryuse 9388608 
#limit vmemoryuse 9388608 
limit

set dom = DOMAIN_FILENAME
set inst = INSTANCE_FILENAME
set dataFile = DATA_FILENAME_CSV

set numFolds = NUMBER_OF_FOLDS
set trainingFold = TRAIN_FOLD_INDEX
set testingFold = TESTING_FOLD_INDEX

set outFile = OUTPUT_CSV_FILENAME
set nround = NMBER_OF_ROUNDS
set randomizeTest = TESTING_PHASE_RANDOM

set timeOutMins = TIMEOUT_MINS
set job_title = JOB_NAME
set lookahead = STEPS_LOOKAHEAD
set numFutures = NUM_FUTURES
set rootPolicy = HOP_CONSTRAINT
set futureStrat = FUTURE_STRATEGY
set viz = VISUALIZER 

setenv LD_LIBRARY_PATH /scratch/cluster-share/gurobi652/linux64/lib
set path = ( $path /scratch/cluster-share/gurobi652/linux64/bin )
setenv GRB_LICENSE_FILE /nfs/cluster-fserv/cluster-share/gurobi652/linux64/gurobi.lic
#setenv GRB_LICENSE_FILE /sciratch/rddlsim/lib/gurobi66/linux64/gurobi.lic
#setenv LD_LIBRARY_PATH /scratch/rddlsim/lib/gurobi605/linux64/lib
#set path = ($path /scratch/rddlsim/lib/gurobi605/linux64/bin)

rm -f /tmp/$job_title\_tmp/*
rmdir -f /tmp/$job_title\_tmp

mkdir /tmp/$job_title\_tmp
cp -f ./dist/FittedEmergencyDomainHOP.jar /tmp/$job_title\_tmp/FittedEmergencyDomainHOP.jar 

cp -f $dom /tmp/$job_title\_tmp/$job_title\_dom.rddl
cp -f $inst /tmp/$job_title\_tmp/$job_title\_inst.rddl
cp -f $dataFile /tmp/$job_title\_tmp/$job_title\_data.csv

cd /tmp/$job_title\_tmp

echo " solving $dom $inst"

free -g

time /usr/local/common64/jdk1.8/bin/java -jar -Xmx4g -ea FittedEmergencyDomainHOP.jar $job_title\_dom.rddl $job_title\_inst.rddl $lookahead $timeOutMins $viz $numFutures $futureStrat $rootPolicy $job_title\_data.csv $numFolds $trainingFold $testingFold $outFile $nround $randomizeTest  >! $job_title.log  

free -g
date

cp -f $job_title.log ~/rddlsim/log/
cp -f ~/rddlsim/JOB_NAME.out ~/rddlsim/log/
cp -f ~/rddlsim/JOB_NAME.err ~/rddlsim/log/
cp -f $outFile ~/rddlsim/log/

rm -f /tmp/$job_title\_tmp/*
rmdir -f /tmp/$job_title\_tmp 

#ps aux | grep -i RDDLServer | cut -f 2 -d ' ' | head -n 1 | xargs kill -9

echo "HOP PLanning done $dom $inst"

limit memoryuse unlimited 
limit vmemoryuse unlimited 
