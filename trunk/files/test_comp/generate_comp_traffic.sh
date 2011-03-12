#!/bin/sh

# Executables must run from rddsim/
cd ../..
echo `pwd`

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

