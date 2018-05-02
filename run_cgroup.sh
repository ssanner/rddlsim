# !/usr/bin/env bash

CGROUPS_ROOT=/sys/fs/cgroup/
IPPC_CPUACCT_CGROUP=$CGROUPS_ROOT/cpuacct/ippc
IPPC_MEMORY_CGROUP=$CGROUPS_ROOT/memory/ippc


if [ "$#" -ne 4 ]; then
    echo "Usage: ./run_cgroup.sh /path/to/rddl/domains [MEMORY LIMIT] [TIME LIMIT] [PORT]"
    echo "MEMORY LIMIT: memory limit for the clients in kilobytes (without KB or K)"
    echo "TIME LIMIT: total time limit"
    echo "PORT: connection port"
    exit
fi
BENCHMARK_DIR=$1
shift
MEMORY_LIMIT=$1
shift
TIME_LIMIT=$1
shift
PORT=$1
shift
PLANNER_NAME=$1
shift


### ATTENTION!!!!
#
# In the Slurm, you need to change the USER_GROUP
# variable to "infai".
# Usually, you can run in you local machine using
# the group as your username.
#
###
USER_GROUP=$USER


set -e # Not sure yet if we should use it here.
source "${BASH_SOURCE%/*}/cgroup_utils"

if [ ! -d "$IPPC_CGROUP" ]; then
    sudo cgcreate -a $USER:$USER_GROUP -t $USER:$USER_GROUP -g cpuacct:ippc
fi


MEMORY_GROUP=$(create_group_task memory  /ippc/limited_client_$$) # Only for the client
echo "Script created the memory group $MEMORY_GROUP for the client."

CPUACCT_GROUP=$(create_group_task cpuacct /ippc/limited_$$) # Shared by the client and the server
echo "Script created the time group $CPUACCT_GROUP for the client and the server."


### SETTING UP THE MEMORY.
# The "excess" parameter is an additional to guarantee the cgroup to work fine
# with singularity images.
ULIMIT_MEMORY_LIMIT=$MEMORY_LIMIT
CGROUP_MEMORY_LIMIT=$(($ULIMIT_MEMORY_LIMIT + 128000))
MONITOR_MEMORY_LIMIT=$ULIMIT_MEMORY_LIMIT

set_memory_limit $MEMORY_GROUP "$MEMORY_LIMIT"K 
echo "Setting memory limit to $MEMORY_LIMIT megabytes"
echo "Setting memory limit to $TIME_LIMIT seconds"


### SETTING UP THE TIME
# Give an extra time limit to allow graceful exit
KILL_WAIT=2


################################
### START THE SERVER AND WAIT
# Important: change the call but keep the parameter
# $CPUACCT_GROUP. Because this is how the Server
# checks the remaining time for the client
##
## REMARK: there is a sleep after the cgexec.
################################

set +e
(
    #cgexec -g cpuacct:$CPUACCT_GROUP --sticky ./run rddl.competition.Server $BENCHMARK_DIR $PORT 100 1 1 $TIME_LIMIT ./ 1 1 $CPUACCT_GROUP > server.out 2> server.err
    cgexec -g cpuacct:$CPUACCT_GROUP --sticky ./run rddl.competition.Server $BENCHMARK_DIR $PORT 100 1 1 $TIME_LIMIT ./ 1 0 $PLANNER_NAME > server.out 2> server.err
)&
SERVER_PID=$!
sleep 3

echo "Server created."

# sleep 2
# echo "Killing server just to finish the script correctly."
# kill -KILL $SERVER_PID && sleep 5

### How to run your planner:
#
#   (cgexec -g cpuacct:$CPUACCT_GROUP -g memory:$MEMORY_GROUP --sticky ./path/to/planner [ARGUMENTS]) &
#
### If you are running already with a Singularity image of a planner, use the next lines.
### Notice that we have to set up a "RUNDIR" variable with an absolute path. In this example,
### we are using the same one. We are also using a naive INSTANCE.
#
RUNDIR="$(pwd)"
INSTANCE=crossing_traffic_demo_inst_mdp__1
(
    # Limiting the "extra" time limit
    ulimit -S -t $(($TIME_LIMIT + $KILL_WAIT)) #$TIME_LIMIT
    ulimit -H -t $(($TIME_LIMIT + $KILL_WAIT))
    # # Limiting the "extra" memory limit
    ulimit -c 0
    ulimit -v $ULIMIT_MEMORY_LIMIT
    cgexec -g cpuacct:$CPUACCT_GROUP -g memory:$MEMORY_GROUP --sticky singularity run -C -H $RUNDIR planner.img $INSTANCE $PORT \
	   > client.out 2> client.err
    exit $?
)&
CLIENT_PID=$!
sleep 5
echo "Running the client planner." 
monitor_client $CLIENT_PID $(($MONITOR_MEMORY_LIMIT * 1024)) $MEMORY_GROUP
CLIENT_EXIT_CODE=$?
echo "Exit code of the planner: $CLIENT_EXIT_CODE"

print_resource_usage $CPUACCT_GROUP $MEMORY_GROUP

sleep 4

set -e
remove_cgroup cpuacct $CPUACCT_GROUP
remove_cgroup memory $MEMORY_GROUP

echo "Cleaning up remaining the cgroups"
