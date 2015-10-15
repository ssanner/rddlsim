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
#$ -l m_mem_free=6G
#$ -l h=compute-0*|compute-1*|compute-2*|compute-3*|compute-4*|compute-5*
set dom = DOMAIN_FILENAME
set inst = INSTANCE_FILENAME
set timeOutMins = TIMEOUT_MINS
set job_title = JOB_NAME
set lookahead = STEPS_LOOKAHEAD
set numFutures = NUM_FUTURES
set rootPolicy = HOP_CONSTRAINT
set futureStrat = FUTURE_STRATEGY
set instanceName = `cat $inst | grep "instance " | cut -f 2 -d ' ' | cut -f 1 -d ' '`
set viz = VISUALIZER 

setenv LD_LIBRARY_PATH /scratch/cluster-share/gurobi604/linux64/lib
set path = ( $path /scratch/cluster-share/gurobi604/linux64/bin )
setenv GRB_LICENSE_FILE /nfs/cluster-fserv/cluster-share/gurobi604/linux64/license/gurobi.lic
#setenv GRB_LICENSE_FILE /sciratch/rddlsim/lib/gurobi66/linux64/gurobi.lic
#setenv LD_LIBRARY_PATH /scratch/rddlsim/lib/gurobi605/linux64/lib
#set path = ($path /scratch/rddlsim/lib/gurobi605/linux64/bin)

set files_dir = ./files/mdp_hybrid/
set portno = SERVER_PORT_NUMBER
set nrounds = SERVER_NUM_ROUNDS
set seed = SERVER_RANDOM_SEED

rm -f /tmp/$job_title\_tmp/*
rmdir -f /tmp/$job_title\_tmp

mkdir /tmp/$job_title\_tmp
cp -f ./dist/RDDLServer.jar /tmp/$job_title\_tmp/RDDLServer.jar
cp -f ./dist/HOPTranslateRDDLClient.jar /tmp/$job_title\_tmp/HOPTranslateRDDLClient.jar 

cp -f $dom /tmp/$job_title\_tmp/$job_title\_dom
cp -f $inst /tmp/$job_title\_tmp/$job_title\_inst

hostname
date
limit

echo " solving $dom $inst"

time /usr/local/common64/jdk1.8/bin/java -jar -Xmx1g -Xms1g -ea /tmp/$job_title\_tmp/RDDLServer.jar $files_dir $portno $nrounds $seed >! /tmp/$job_title\_tmp/$job_title\_server.log & 
set server_pid = `echo $!`  
echo "server pid $server_pid"
 
sleep 10

time /usr/local/common64/jdk1.8/bin/java -jar -Xmx4g -Xms1g -ea /tmp/$job_title\_tmp/HOPTranslateRDDLClient.jar $files_dir localhost grb_$job_title rddl.det.mip.HOPTranslate $portno $seed $instanceName $dom $inst $lookahead $timeOutMins $viz $numFutures $futureStrat $rootPolicy >! /tmp/$job_title\_tmp/$job_title\_client.log  

echo "killing pid $server_pid"
kill -9 $server_pid 

cp -f /tmp/$job_title\_tmp/$job_title*.log ./log/
cp -f JOB_NAME.out ./log/
cp -f JOB_NAME.err ./log/
cp -f reservoir_levels_last.viz log/$job_title\_levels.viz 
cp -f reservoir_rain_last.viz log/$job_title\_rain.viz 

rm -f /tmp/$job_title\_tmp/*
rmdir -f /tmp/$job_title\_tmp

#ps aux | grep -i RDDLServer | cut -f 2 -d ' ' | head -n 1 | xargs kill -9

echo "HOP PLanning done $dom $inst"

