# !/usr/bin/env bash

CGROUPS_ROOT=/sys/fs/cgroup/
IPPC_CPUACCT_CGROUP=$CGROUPS_ROOT/cpuacct/ippc
IPPC_MEMORY_CGROUP=$CGROUPS_ROOT/memory/ippc


if [ "$#" -ne 4 ]; then
    echo "Usage: ./run_cgroup.sh /path/to/rddl/domains [MEMORY LIMIT] [TIME LIMIT] [PORT]"
    echo "MEMORY LIMIT: memory limit for the clients in megabytes (without MB or M)"
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


set_memory_limit $MEMORY_GROUP "$MEMORY_LIMIT"M # set cgroups to 8GB
echo "Setting memory limit to $MEMORY_LIMIT megabytes"
echo "Setting memory limit to $TIME_LIMIT seconds"

################################
### START THE SERVER AND WAIT
# Important: change the call but keep the parameter
# $CPUACCT_GROUP. Because this is how the Server
# checks the remaining time for the client
##
## REMARK: there is a sleep after the cgexec.
################################

(
    cgexec -g cpuacct:$CPUACCT_GROUP --sticky ./run rddl.competition.Server $BENCHMARK_DIR $PORT 100 1 1 $TIME_LIMIT ./ 1 $CPUACCT_GROUP \
	   > server.out 2> server.err
)&
SERVER_PID=$!
sleep 25

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
INSTANCE=recon_demo_inst_mdp__1
(
    cgexec -g cpuacct:$CPUACCT_GROUP -g memory:$MEMORY_GROUP --sticky singularity run -C -H $RUNDIR planner.img $INSTANCE $PORT \
	   > client.out 2> client.err
    exit $?
)&
#
###
CLIENT_PID=$!
echo "Runnin the client planner." 
wait $CLIENT_PID
CLIENT_EXIT_CODE=$?
echo "Exit code of the planner: $CLIENT_EXIT_CODE"

echo "Remove is right now not working"
#exit
# Remove is right now not working
sleep 10
remove_cgroup cpuacct $CPUACCT_GROUP
remove_cgroup memory $MEMORY_GROUP

echo "Cleaning up remaining the cgroups"
