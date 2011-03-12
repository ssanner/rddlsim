#!/bin/sh

#############################################################################
# This file generates test instances for the MDP and POMDP boolean test
# competition of the IPPC 2011.
#
# Author: Scott Sanner (ssanner [at] gmail.com)
#############################################################################

# Executables must run from rddsim/
cd ../..
echo `pwd`

#############################################################################
# Generate SysAdmin MDP instances
#############################################################################
echo Generating SysAdmin MDPs
./run rddl.competition.generators.SysAdminMDPGen files/test_comp/rddl sysadmin_inst_mdp__1 10 2 0.05 30 1.0
./run rddl.competition.generators.SysAdminMDPGen files/test_comp/rddl sysadmin_inst_mdp__2 10 3 0.05 30 1.0
./run rddl.competition.generators.SysAdminMDPGen files/test_comp/rddl sysadmin_inst_mdp__3 20 2 0.05 30 1.0
./run rddl.competition.generators.SysAdminMDPGen files/test_comp/rddl sysadmin_inst_mdp__4 20 3 0.05 30 1.0
./run rddl.competition.generators.SysAdminMDPGen files/test_comp/rddl sysadmin_inst_mdp__5 30 2 0.05 30 1.0
./run rddl.competition.generators.SysAdminMDPGen files/test_comp/rddl sysadmin_inst_mdp__6 30 3 0.05 30 1.0
./run rddl.competition.generators.SysAdminMDPGen files/test_comp/rddl sysadmin_inst_mdp__7 40 2 0.05 30 1.0
./run rddl.competition.generators.SysAdminMDPGen files/test_comp/rddl sysadmin_inst_mdp__8 40 3 0.05 30 1.0
./run rddl.competition.generators.SysAdminMDPGen files/test_comp/rddl sysadmin_inst_mdp__9 50 2 0.05 30 1.0
./run rddl.competition.generators.SysAdminMDPGen files/test_comp/rddl sysadmin_inst_mdp__10 50 3 0.05 30 1.0

#############################################################################
# Generate SysAdmin POMDP instances
#############################################################################
echo Generating SysAdmin POMDPs
./run rddl.competition.generators.SysAdminPOMDPGen files/test_comp/rddl sysadmin_inst_pomdp__1 10 2 0.05 30 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/test_comp/rddl sysadmin_inst_pomdp__2 10 3 0.05 30 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/test_comp/rddl sysadmin_inst_pomdp__3 20 2 0.05 30 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/test_comp/rddl sysadmin_inst_pomdp__4 20 3 0.05 30 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/test_comp/rddl sysadmin_inst_pomdp__5 30 2 0.05 30 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/test_comp/rddl sysadmin_inst_pomdp__6 30 3 0.05 30 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/test_comp/rddl sysadmin_inst_pomdp__7 40 2 0.05 30 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/test_comp/rddl sysadmin_inst_pomdp__8 40 3 0.05 30 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/test_comp/rddl sysadmin_inst_pomdp__9 50 2 0.05 30 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/test_comp/rddl sysadmin_inst_pomdp__10 50 3 0.05 30 1.0

#############################################################################
# Generate elevator MDP instances
#############################################################################
echo Generating Elevator MDPs
./run rddl.competition.generators.ElevatorMDPGen files/test_comp/rddl elevator_inst_mdp__1 1 3 0.5 0.1 100 1.0
./run rddl.competition.generators.ElevatorMDPGen files/test_comp/rddl elevator_inst_mdp__2 2 3 0.5 0.3 100 1.0
./run rddl.competition.generators.ElevatorMDPGen files/test_comp/rddl elevator_inst_mdp__3 2 3 0.7 0.5 100 1.0
./run rddl.competition.generators.ElevatorMDPGen files/test_comp/rddl elevator_inst_mdp__4 1 4 0.5 0.1 100 1.0
./run rddl.competition.generators.ElevatorMDPGen files/test_comp/rddl elevator_inst_mdp__5 2 4 0.5 0.3 100 1.0
./run rddl.competition.generators.ElevatorMDPGen files/test_comp/rddl elevator_inst_mdp__6 2 4 0.7 0.5 100 1.0
./run rddl.competition.generators.ElevatorMDPGen files/test_comp/rddl elevator_inst_mdp__7 1 5 0.5 0.1 100 1.0
./run rddl.competition.generators.ElevatorMDPGen files/test_comp/rddl elevator_inst_mdp__8 2 5 0.5 0.3 100 1.0
./run rddl.competition.generators.ElevatorMDPGen files/test_comp/rddl elevator_inst_mdp__9 2 5 0.7 0.5 100 1.0
./run rddl.competition.generators.ElevatorMDPGen files/test_comp/rddl elevator_inst_mdp__10 1 6 0.5 0.1 100 1.0

#############################################################################
# Generate elevator POMDP instances
#############################################################################
echo Generating Elevator POMDPs
./run rddl.competition.generators.ElevatorPOMDPGen files/test_comp/rddl elevator_inst_pomdp__1 1 3 0.5 0.1 100 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/test_comp/rddl elevator_inst_pomdp__2 2 3 0.5 0.3 100 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/test_comp/rddl elevator_inst_pomdp__3 2 3 0.7 0.5 100 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/test_comp/rddl elevator_inst_pomdp__4 1 4 0.5 0.1 100 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/test_comp/rddl elevator_inst_pomdp__5 2 4 0.5 0.3 100 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/test_comp/rddl elevator_inst_pomdp__6 2 4 0.7 0.5 100 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/test_comp/rddl elevator_inst_pomdp__7 1 5 0.5 0.1 100 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/test_comp/rddl elevator_inst_pomdp__8 2 5 0.5 0.3 100 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/test_comp/rddl elevator_inst_pomdp__9 2 5 0.7 0.5 100 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/test_comp/rddl elevator_inst_pomdp__10 1 6 0.5 0.1 100 1.0

#############################################################################
# Generate game of life MDP instances
#############################################################################
echo Generating Game of Life MDPs
./run rddl.competition.generators.GameOfLifeMDPGen files/test_comp/rddl game_of_life_inst_mdp__1 3 3 0.01 0.01 0.5 30 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/test_comp/rddl game_of_life_inst_mdp__2 3 3 0.10 0.15 0.5 30 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/test_comp/rddl game_of_life_inst_mdp__3 3 3 0.20 0.30 0.5 30 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/test_comp/rddl game_of_life_inst_mdp__4 4 4 0.01 0.01 0.5 30 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/test_comp/rddl game_of_life_inst_mdp__5 4 4 0.10 0.15 0.5 30 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/test_comp/rddl game_of_life_inst_mdp__6 4 4 0.20 0.30 0.5 30 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/test_comp/rddl game_of_life_inst_mdp__7 5 5 0.01 0.01 0.5 30 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/test_comp/rddl game_of_life_inst_mdp__8 5 5 0.10 0.15 0.5 30 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/test_comp/rddl game_of_life_inst_mdp__9 5 5 0.20 0.30 0.5 30 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/test_comp/rddl game_of_life_inst_mdp__10 6 6 0.10 0.15 0.5 30 1.0

#############################################################################
# Generate game of life POMDP instances
#############################################################################
echo Generating Game of Life POMDPs
./run rddl.competition.generators.GameOfLifePOMDPGen files/test_comp/rddl game_of_life_inst_pomdp__1 3 3 0.01 0.01 0.5 30 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/test_comp/rddl game_of_life_inst_pomdp__2 3 3 0.10 0.15 0.5 30 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/test_comp/rddl game_of_life_inst_pomdp__3 3 3 0.20 0.30 0.5 30 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/test_comp/rddl game_of_life_inst_pomdp__4 4 4 0.01 0.01 0.5 30 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/test_comp/rddl game_of_life_inst_pomdp__5 4 4 0.10 0.15 0.5 30 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/test_comp/rddl game_of_life_inst_pomdp__6 4 4 0.20 0.30 0.5 30 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/test_comp/rddl game_of_life_inst_pomdp__7 5 5 0.01 0.01 0.5 30 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/test_comp/rddl game_of_life_inst_pomdp__8 5 5 0.10 0.15 0.5 30 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/test_comp/rddl game_of_life_inst_pomdp__9 5 5 0.20 0.30 0.5 30 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/test_comp/rddl game_of_life_inst_pomdp__10 6 6 0.10 0.15 0.5 30 1.0

#############################################################################
# Generate traffic MDP instances
#############################################################################
echo Generating traffic MDPs
./run rddl.competition.generators.TrafficMDPGen files/test_comp/rddl traffic_inst_mdp__1 2 0.15 0.4 100 1.0
./run rddl.competition.generators.TrafficMDPGen files/test_comp/rddl traffic_inst_mdp__2 2 0.3  0.5 100 1.0
./run rddl.competition.generators.TrafficMDPGen files/test_comp/rddl traffic_inst_mdp__3 3 0.15 0.4 100 1.0
./run rddl.competition.generators.TrafficMDPGen files/test_comp/rddl traffic_inst_mdp__4 3 0.3  0.5 100 1.0
./run rddl.competition.generators.TrafficMDPGen files/test_comp/rddl traffic_inst_mdp__5 4 0.15 0.4 100 1.0
./run rddl.competition.generators.TrafficMDPGen files/test_comp/rddl traffic_inst_mdp__6 4 0.3  0.5 100 1.0
./run rddl.competition.generators.TrafficMDPGen files/test_comp/rddl traffic_inst_mdp__7 5 0.15 0.4 100 1.0
./run rddl.competition.generators.TrafficMDPGen files/test_comp/rddl traffic_inst_mdp__8 5 0.3  0.5 100 1.0
./run rddl.competition.generators.TrafficMDPGen files/test_comp/rddl traffic_inst_mdp__9 6 0.15 0.4 100 1.0
./run rddl.competition.generators.TrafficMDPGen files/test_comp/rddl traffic_inst_mdp__10 6 0.3  0.5 100 1.0

#############################################################################
# Generate traffic POMDP instances
#############################################################################
echo Generating traffic POMDPs
./run rddl.competition.generators.TrafficPOMDPGen files/test_comp/rddl traffic_inst_pomdp__1 2 0.15 0.4 100 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/test_comp/rddl traffic_inst_pomdp__2 2 0.3  0.5 100 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/test_comp/rddl traffic_inst_pomdp__3 3 0.15 0.4 100 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/test_comp/rddl traffic_inst_pomdp__4 3 0.3  0.5 100 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/test_comp/rddl traffic_inst_pomdp__5 4 0.15 0.4 100 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/test_comp/rddl traffic_inst_pomdp__6 4 0.3  0.5 100 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/test_comp/rddl traffic_inst_pomdp__7 5 0.15 0.4 100 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/test_comp/rddl traffic_inst_pomdp__8 5 0.3  0.5 100 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/test_comp/rddl traffic_inst_pomdp__9 6 0.15 0.4 100 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/test_comp/rddl traffic_inst_pomdp__10 6 0.3  0.5 100 1.0

#############################################################################
# Generate RDDL Prefix, (PO-)PPDDL, SPUDD, and Symbolic Perseus translations
#############################################################################

./run rddl.translate.RDDL2Prefix files/test_comp/rddl files/test_comp/rddl_prefix
./run rddl.translate.RDDL2Format files/test_comp/rddl files/test_comp/spudd_sperseus spudd_sperseus
./run rddl.translate.RDDL2Format files/test_comp/rddl files/test_comp/ppddl ppddl

#############################################################################
# Generate Instance Lists
#############################################################################
DIR="files/test_comp/rddl"
MDP_FILE="files/test_comp/mdp_instance_list.txt"

if [[ -f ${MDP_FILE} ]]; then  
        rm -f ${MDP_FILE} # if the file exists, remove it
fi
echo
echo MDP instances exported to ${MDP_FILE}
for f in "${DIR}"/*inst_mdp*.rddl
do
  echo ${f} | sed 's#^.*/##' | sed 's#\.rddl##'
  echo ${f} | sed 's#^.*/##' | sed 's#\.rddl##' >> ${MDP_FILE}
done

POMDP_FILE="files/test_comp/pomdp_instance_list.txt"

if [[ -f ${POMDP_FILE} ]]; then  
        rm -f ${POMDP_FILE} # if the file exists, remove it
fi
echo
echo POMDP instances exported to ${POMDP_FILE}
for f in "${DIR}"/*inst_pomdp*.rddl
do
  echo ${f} | sed 's#^.*/##' | sed 's#\.rddl##'
  echo ${f} | sed 's#^.*/##' | sed 's#\.rddl##' >> ${POMDP_FILE}
done

