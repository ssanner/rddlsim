# !/usr/bin/env bash

CGROUPS_ROOT=/sys/fs/cgroup/
IPPC_CPUACCT_CGROUP=$CGROUPS_ROOT/cpuacct/ippc
IPPC_MEMORY_CGROUP=$CGROUPS_ROOT/memory/ippc


if [ "$#" -ne 1 ]; then
    echo "Usage: ./run_cgroup.sh /path/to/rddl/domains"
    exit
fi
BENCHMARK_DIR=$1


### ATTENTION!!!!
#
# In the Slurm, you need to change the USER_GROUP
# variable to "infai".
# Usually, you can run in you local machine using
# the group as your username.
#
###
USER_GROUP=$USER

source cgroup_utils

if [ ! -d "$IPPC_CGROUP" ]; then
    sudo cgcreate -a $USER:$USER_GROUP -t $USER:$USER_GROUP -g cpuacct:ippc
fi


MEMORY_GROUP=$(create_group_task memory  /ippc/limited_client_$$) # Only for the client
echo "Script created the memory group $MEMORY_GROUP for the client."

CPUACCT_GROUP=$(create_group_task cpuacct /ippc/limited_$$) # Shared by the client and the server
echo "Script created the time group $CPUACCT_GROUP for the client and the server."

################################
### START THE SERVER AND WAIT
# Important: change the call but keep the parameter
# $CPUACCT_GROUP. Because this is how the Server
# checks the remaining time for the client
##
## REMARK: there is a sleep after the cgexec.
################################
set -e # Not sure yet if we should use it.
(
    cgexec -g cpuacct:$CPUACCT_GROUP --sticky ./run rddl.competition.Server $BENCHMARK_DIR 2323 100 1 1 60 ./ 1 $CPUACCT_GROUP \
	   > server.out 2> server.err
)&
SERVER_PID=$!
sleep 5

echo "Server created." 

sleep 2
echo "Killing server just to finish the script correctly."
kill -KILL $SERVER_PID && sleep 5
wait $SERVER_PID

### How to run your planner:
#
#   (cgexec -g cpuacct:$CPUACCT_GROUP -g memory:$MEMORY_GROUP --sticky ./path/to/planner [ARGUMENTS]) &
#
### If you are running already with a Singularity image of a planner:
#
#   (cgexec -g cpuacct:$CPUACCT_GROUP -g memory:$MEMORY_GROUP --sticky singularity run -C -H $RUNDIR planner.img INSTANCE port) &
#
###

echo "Remove is right now not working"
exit
# Remove is right now not working
remove_cgroup cpuacct $CPUACCT_GROUP
remove_cgroup memory $MEMORY_GROUP

echo "Cleaning up remaining the cgroups"
