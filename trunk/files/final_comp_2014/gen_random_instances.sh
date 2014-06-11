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
rm -rf files/final_comp_2014/rddl/*inst*

#############################################################################
# Generate triangle tireworld MDP instances
#############################################################################
echo Generating Triangle Tireworld MDPs
./run rddl.competition.generators.TriangleTireworldMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_mdp__1  src/ppddl/tt/p01.pddl 0.40 40 1.0
./run rddl.competition.generators.TriangleTireworldMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_mdp__2  src/ppddl/tt/p01.pddl 0.499  40 1.0
./run rddl.competition.generators.TriangleTireworldMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_mdp__3  src/ppddl/tt/p02.pddl 0.35  40 1.0
./run rddl.competition.generators.TriangleTireworldMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_mdp__4  src/ppddl/tt/p02.pddl 0.45  40 1.0
./run rddl.competition.generators.TriangleTireworldMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_mdp__5  src/ppddl/tt/p03.pddl 0.30  40 1.0
./run rddl.competition.generators.TriangleTireworldMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_mdp__6  src/ppddl/tt/p03.pddl 0.40  40 1.0
./run rddl.competition.generators.TriangleTireworldMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_mdp__7  src/ppddl/tt/p04.pddl 0.25  40 1.0
./run rddl.competition.generators.TriangleTireworldMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_mdp__8  src/ppddl/tt/p04.pddl 0.35  40 1.0
./run rddl.competition.generators.TriangleTireworldMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_mdp__9  src/ppddl/tt/p05.pddl 0.20  40 1.0
./run rddl.competition.generators.TriangleTireworldMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_mdp__10 src/ppddl/tt/p05.pddl 0.30  40 1.0

#############################################################################
# Generate triangle tireworld POMDP instances
#############################################################################
echo Generating Triangle Tireworld POMDPs
./run rddl.competition.generators.TriangleTireworldPOMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_pomdp__1  src/ppddl/tt/p01.pddl 0.40 40 1.0
./run rddl.competition.generators.TriangleTireworldPOMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_pomdp__2  src/ppddl/tt/p01.pddl 0.499  40 1.0
./run rddl.competition.generators.TriangleTireworldPOMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_pomdp__3  src/ppddl/tt/p02.pddl 0.35  40 1.0
./run rddl.competition.generators.TriangleTireworldPOMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_pomdp__4  src/ppddl/tt/p02.pddl 0.45  40 1.0
./run rddl.competition.generators.TriangleTireworldPOMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_pomdp__5  src/ppddl/tt/p03.pddl 0.30  40 1.0
./run rddl.competition.generators.TriangleTireworldPOMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_pomdp__6  src/ppddl/tt/p03.pddl 0.40  40 1.0
./run rddl.competition.generators.TriangleTireworldPOMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_pomdp__7  src/ppddl/tt/p04.pddl 0.25  40 1.0
./run rddl.competition.generators.TriangleTireworldPOMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_pomdp__8  src/ppddl/tt/p04.pddl 0.35  40 1.0
./run rddl.competition.generators.TriangleTireworldPOMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_pomdp__9  src/ppddl/tt/p05.pddl 0.20  40 1.0
./run rddl.competition.generators.TriangleTireworldPOMDPGen files/final_comp_2014/rddl triangle_tireworld_inst_pomdp__10 src/ppddl/tt/p05.pddl 0.30  40 1.0

#############################################################################
# Generate tamarisk MDP instances
#############################################################################
echo Generating Tamarisk MDPs
./run rddl.competition.generators.TamariskMDPGen files/final_comp_2014/rddl tamarisk_inst_mdp__1  4 2 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskMDPGen files/final_comp_2014/rddl tamarisk_inst_mdp__2  4 3 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskMDPGen files/final_comp_2014/rddl tamarisk_inst_mdp__3  5 2 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskMDPGen files/final_comp_2014/rddl tamarisk_inst_mdp__4  5 3 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskMDPGen files/final_comp_2014/rddl tamarisk_inst_mdp__5  6 2 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskMDPGen files/final_comp_2014/rddl tamarisk_inst_mdp__6  6 3 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskMDPGen files/final_comp_2014/rddl tamarisk_inst_mdp__7  7 2 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskMDPGen files/final_comp_2014/rddl tamarisk_inst_mdp__8  7 3 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskMDPGen files/final_comp_2014/rddl tamarisk_inst_mdp__9  8 2 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskMDPGen files/final_comp_2014/rddl tamarisk_inst_mdp__10 8 3 0.3 0.2 40 1.0

#############################################################################
# Generate tamarisk POMDP instances
#############################################################################
echo Generating Tamarisk POMDPs
./run rddl.competition.generators.TamariskPOMDPGen files/final_comp_2014/rddl tamarisk_inst_pomdp__1  4 2 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskPOMDPGen files/final_comp_2014/rddl tamarisk_inst_pomdp__2  4 3 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskPOMDPGen files/final_comp_2014/rddl tamarisk_inst_pomdp__3  5 2 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskPOMDPGen files/final_comp_2014/rddl tamarisk_inst_pomdp__4  5 3 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskPOMDPGen files/final_comp_2014/rddl tamarisk_inst_pomdp__5  6 2 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskPOMDPGen files/final_comp_2014/rddl tamarisk_inst_pomdp__6  6 3 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskPOMDPGen files/final_comp_2014/rddl tamarisk_inst_pomdp__7  7 2 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskPOMDPGen files/final_comp_2014/rddl tamarisk_inst_pomdp__8  7 3 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskPOMDPGen files/final_comp_2014/rddl tamarisk_inst_pomdp__9  8 2 0.3 0.2 40 1.0
./run rddl.competition.generators.TamariskPOMDPGen files/final_comp_2014/rddl tamarisk_inst_pomdp__10 8 3 0.3 0.2 40 1.0

#############################################################################
# Generate academic advising MDP instances
#############################################################################
echo Generating Academic Advising MDPs
./run rddl.competition.generators.AcademicAdvisingMDPGen files/final_comp_2014/rddl academic_advising_inst_mdp__1  5 2 2 0.5 0.5 1 40 1.0
./run rddl.competition.generators.AcademicAdvisingMDPGen files/final_comp_2014/rddl academic_advising_inst_mdp__2  5 2 3 0.5 0.5 2 40 1.0
./run rddl.competition.generators.AcademicAdvisingMDPGen files/final_comp_2014/rddl academic_advising_inst_mdp__3  5 3 2 0.5 0.5 1 40 1.0
./run rddl.competition.generators.AcademicAdvisingMDPGen files/final_comp_2014/rddl academic_advising_inst_mdp__4  5 3 3 0.5 0.5 2 40 1.0
./run rddl.competition.generators.AcademicAdvisingMDPGen files/final_comp_2014/rddl academic_advising_inst_mdp__5  5 4 2 0.5 0.4 1 40 1.0
./run rddl.competition.generators.AcademicAdvisingMDPGen files/final_comp_2014/rddl academic_advising_inst_mdp__6  5 4 3 0.5 0.4 2 40 1.0
./run rddl.competition.generators.AcademicAdvisingMDPGen files/final_comp_2014/rddl academic_advising_inst_mdp__7  5 5 2 0.5 0.4 1 40 1.0
./run rddl.competition.generators.AcademicAdvisingMDPGen files/final_comp_2014/rddl academic_advising_inst_mdp__8  5 5 3 0.5 0.4 2 40 1.0
./run rddl.competition.generators.AcademicAdvisingMDPGen files/final_comp_2014/rddl academic_advising_inst_mdp__9  5 6 2 0.5 0.3 1 40 1.0
./run rddl.competition.generators.AcademicAdvisingMDPGen files/final_comp_2014/rddl academic_advising_inst_mdp__10 5 6 3 0.5 0.3 2 40 1.0

#############################################################################
# Generate academic advising POMDP instances
#############################################################################
echo Generating Academic Advising POMDPs
./run rddl.competition.generators.AcademicAdvisingPOMDPGen files/final_comp_2014/rddl academic_advising_inst_pomdp__1  5 2 2 0.5 0.5 1 40 1.0
./run rddl.competition.generators.AcademicAdvisingPOMDPGen files/final_comp_2014/rddl academic_advising_inst_pomdp__2  5 2 3 0.5 0.5 2 40 1.0
./run rddl.competition.generators.AcademicAdvisingPOMDPGen files/final_comp_2014/rddl academic_advising_inst_pomdp__3  5 3 2 0.5 0.5 1 40 1.0
./run rddl.competition.generators.AcademicAdvisingPOMDPGen files/final_comp_2014/rddl academic_advising_inst_pomdp__4  5 3 3 0.5 0.5 2 40 1.0
./run rddl.competition.generators.AcademicAdvisingPOMDPGen files/final_comp_2014/rddl academic_advising_inst_pomdp__5  5 4 2 0.5 0.4 1 40 1.0
./run rddl.competition.generators.AcademicAdvisingPOMDPGen files/final_comp_2014/rddl academic_advising_inst_pomdp__6  5 4 3 0.5 0.4 2 40 1.0
./run rddl.competition.generators.AcademicAdvisingPOMDPGen files/final_comp_2014/rddl academic_advising_inst_pomdp__7  5 5 2 0.5 0.4 1 40 1.0
./run rddl.competition.generators.AcademicAdvisingPOMDPGen files/final_comp_2014/rddl academic_advising_inst_pomdp__8  5 5 3 0.5 0.4 2 40 1.0
./run rddl.competition.generators.AcademicAdvisingPOMDPGen files/final_comp_2014/rddl academic_advising_inst_pomdp__9  5 6 2 0.5 0.3 1 40 1.0
./run rddl.competition.generators.AcademicAdvisingPOMDPGen files/final_comp_2014/rddl academic_advising_inst_pomdp__10 5 6 3 0.5 0.3 2 40 1.0

#############################################################################
# Generate wildfire MDP instances
#############################################################################
echo Generating Wildfire MDPs
./run rddl.competition.generators.WildfireMDPGen files/final_comp_2014/rddl wildfire_inst_mdp__1   3 3 3  0.1 0.2 40 1.0
./run rddl.competition.generators.WildfireMDPGen files/final_comp_2014/rddl wildfire_inst_mdp__2   3 3 5  0.1 0.2 40 1.0
./run rddl.competition.generators.WildfireMDPGen files/final_comp_2014/rddl wildfire_inst_mdp__3   4 4 4  0.1 0.1 40 1.0
./run rddl.competition.generators.WildfireMDPGen files/final_comp_2014/rddl wildfire_inst_mdp__4   4 4 8  0.1 0.1 40 1.0
./run rddl.competition.generators.WildfireMDPGen files/final_comp_2014/rddl wildfire_inst_mdp__5   5 5 5  0.1 0.1 40 1.0
./run rddl.competition.generators.WildfireMDPGen files/final_comp_2014/rddl wildfire_inst_mdp__6   5 5 10 0.1 0.1 40 1.0
./run rddl.competition.generators.WildfireMDPGen files/final_comp_2014/rddl wildfire_inst_mdp__7  10 3 5  0.1 0.1 40 1.0
./run rddl.competition.generators.WildfireMDPGen files/final_comp_2014/rddl wildfire_inst_mdp__8  10 3 10 0.1 0.1 40 1.0
./run rddl.competition.generators.WildfireMDPGen files/final_comp_2014/rddl wildfire_inst_mdp__9   9 4 5  0.1 0.1 40 1.0
./run rddl.competition.generators.WildfireMDPGen files/final_comp_2014/rddl wildfire_inst_mdp__10  9 4 10 0.1 0.1 40 1.0

#############################################################################
# Generate wildfire POMDP instances
#############################################################################
echo Generating Wildfire POMDPs
./run rddl.competition.generators.WildfirePOMDPGen files/final_comp_2014/rddl wildfire_inst_pomdp__1   3 3 3  0.1 0.2 40 1.0
./run rddl.competition.generators.WildfirePOMDPGen files/final_comp_2014/rddl wildfire_inst_pomdp__2   3 3 5  0.1 0.2 40 1.0
./run rddl.competition.generators.WildfirePOMDPGen files/final_comp_2014/rddl wildfire_inst_pomdp__3   4 4 4  0.1 0.1 40 1.0
./run rddl.competition.generators.WildfirePOMDPGen files/final_comp_2014/rddl wildfire_inst_pomdp__4   4 4 8  0.1 0.1 40 1.0
./run rddl.competition.generators.WildfirePOMDPGen files/final_comp_2014/rddl wildfire_inst_pomdp__5   5 5 5  0.1 0.1 40 1.0
./run rddl.competition.generators.WildfirePOMDPGen files/final_comp_2014/rddl wildfire_inst_pomdp__6   5 5 10 0.1 0.1 40 1.0
./run rddl.competition.generators.WildfirePOMDPGen files/final_comp_2014/rddl wildfire_inst_pomdp__7  10 3 5  0.1 0.1 40 1.0
./run rddl.competition.generators.WildfirePOMDPGen files/final_comp_2014/rddl wildfire_inst_pomdp__8  10 3 10 0.1 0.1 40 1.0
./run rddl.competition.generators.WildfirePOMDPGen files/final_comp_2014/rddl wildfire_inst_pomdp__9   9 4 5  0.1 0.1 40 1.0
./run rddl.competition.generators.WildfirePOMDPGen files/final_comp_2014/rddl wildfire_inst_pomdp__10  9 4 10 0.1 0.1 40 1.0

#############################################################################
# Generate elevator MDP instances
#############################################################################
echo Generating Elevator MDPs
./run rddl.competition.generators.ElevatorMDPGen files/final_comp_2014/rddl elevators_inst_mdp__1 1 3 0.5 0.1 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp_2014/rddl elevators_inst_mdp__2 2 3 0.5 0.2 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp_2014/rddl elevators_inst_mdp__3 2 3 0.7 0.3 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp_2014/rddl elevators_inst_mdp__4 1 4 0.5 0.1 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp_2014/rddl elevators_inst_mdp__5 2 4 0.5 0.2 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp_2014/rddl elevators_inst_mdp__6 2 4 0.6 0.3 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp_2014/rddl elevators_inst_mdp__7 1 5 0.4 0.1 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp_2014/rddl elevators_inst_mdp__8 2 5 0.4 0.2 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp_2014/rddl elevators_inst_mdp__9 2 5 0.5 0.3 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp_2014/rddl elevators_inst_mdp__10 1 6 0.3 0.1 40 1.0

#############################################################################
# Generate elevator POMDP instances
#############################################################################
echo Generating Elevator POMDPs
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp_2014/rddl elevators_inst_pomdp__1 1 3 0.3 0.1 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp_2014/rddl elevators_inst_pomdp__2 2 3 0.3 0.2 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp_2014/rddl elevators_inst_pomdp__3 2 3 0.5 0.3 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp_2014/rddl elevators_inst_pomdp__4 1 4 0.3 0.1 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp_2014/rddl elevators_inst_pomdp__5 2 4 0.3 0.2 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp_2014/rddl elevators_inst_pomdp__6 2 4 0.4 0.2 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp_2014/rddl elevators_inst_pomdp__7 1 5 0.2 0.1 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp_2014/rddl elevators_inst_pomdp__8 2 5 0.2 0.1 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp_2014/rddl elevators_inst_pomdp__9 2 5 0.3 0.2 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp_2014/rddl elevators_inst_pomdp__10 1 6 0.2 0.1 40 1.0

#############################################################################
# Generate traffic MDP instances
#############################################################################
echo Generating Traffic MDPs
./run rddl.competition.generators.TrafficMDPGen files/final_comp_2014/rddl traffic_inst_mdp__1 2 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp_2014/rddl traffic_inst_mdp__2 2 0.1 0.5 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp_2014/rddl traffic_inst_mdp__3 3 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp_2014/rddl traffic_inst_mdp__4 3 0.1 0.5 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp_2014/rddl traffic_inst_mdp__5 4 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp_2014/rddl traffic_inst_mdp__6 4 0.1 0.5 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp_2014/rddl traffic_inst_mdp__7 5 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp_2014/rddl traffic_inst_mdp__8 5 0.1 0.5 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp_2014/rddl traffic_inst_mdp__9 6 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp_2014/rddl traffic_inst_mdp__10 6 0.1 0.5 40 1.0

#############################################################################
# Generate traffic POMDP instances
#############################################################################
echo Generating Traffic POMDPs
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp_2014/rddl traffic_inst_pomdp__1 2 0.1 0.2 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp_2014/rddl traffic_inst_pomdp__2 2 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp_2014/rddl traffic_inst_pomdp__3 3 0.1 0.2 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp_2014/rddl traffic_inst_pomdp__4 3 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp_2014/rddl traffic_inst_pomdp__5 4 0.1 0.2 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp_2014/rddl traffic_inst_pomdp__6 4 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp_2014/rddl traffic_inst_pomdp__7 5 0.1 0.2 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp_2014/rddl traffic_inst_pomdp__8 5 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp_2014/rddl traffic_inst_pomdp__9 6 0.1 0.2 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp_2014/rddl traffic_inst_pomdp__10 6 0.1 0.3 40 1.0

#############################################################################
# Generate crossing traffic MDP instances
#############################################################################
echo Generating Crossing Traffic MDPs
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp_2014/rddl crossing_traffic_inst_mdp__1 3 3 0.3 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp_2014/rddl crossing_traffic_inst_mdp__2 3 3 0.6 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp_2014/rddl crossing_traffic_inst_mdp__3 4 4 0.3 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp_2014/rddl crossing_traffic_inst_mdp__4 4 4 0.6 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp_2014/rddl crossing_traffic_inst_mdp__5 5 5 0.2 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp_2014/rddl crossing_traffic_inst_mdp__6 5 5 0.4 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp_2014/rddl crossing_traffic_inst_mdp__7 6 6 0.2 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp_2014/rddl crossing_traffic_inst_mdp__8 6 6 0.4 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp_2014/rddl crossing_traffic_inst_mdp__9 7 7 0.1 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp_2014/rddl crossing_traffic_inst_mdp__10 7 7 0.3 40 1.0

#############################################################################
# Generate crossing traffic POMDP instances
#############################################################################
echo Generating Crossing Traffic POMDPs
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp_2014/rddl crossing_traffic_inst_pomdp__1 3 3 0.2 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp_2014/rddl crossing_traffic_inst_pomdp__2 3 3 0.4 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp_2014/rddl crossing_traffic_inst_pomdp__3 4 4 0.2 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp_2014/rddl crossing_traffic_inst_pomdp__4 4 4 0.4 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp_2014/rddl crossing_traffic_inst_pomdp__5 5 5 0.1 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp_2014/rddl crossing_traffic_inst_pomdp__6 5 5 0.3 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp_2014/rddl crossing_traffic_inst_pomdp__7 6 6 0.1 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp_2014/rddl crossing_traffic_inst_pomdp__8 6 6 0.3 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp_2014/rddl crossing_traffic_inst_pomdp__9 7 7 0.1 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp_2014/rddl crossing_traffic_inst_pomdp__10 7 7 0.2 40 1.0

#############################################################################
# Generate skill teaching MDP instances
#############################################################################
echo Generating Skill Teaching MDPs
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp_2014/rddl skill_teaching_inst_mdp__1 2 2 2 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp_2014/rddl skill_teaching_inst_mdp__2 2 2 3 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp_2014/rddl skill_teaching_inst_mdp__3 4 4 2 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp_2014/rddl skill_teaching_inst_mdp__4 4 4 3 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp_2014/rddl skill_teaching_inst_mdp__5 6 6 2 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp_2014/rddl skill_teaching_inst_mdp__6 6 6 3 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp_2014/rddl skill_teaching_inst_mdp__7 7 7 2 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp_2014/rddl skill_teaching_inst_mdp__8 7 7 3 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp_2014/rddl skill_teaching_inst_mdp__9 8 8 2 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp_2014/rddl skill_teaching_inst_mdp__10 8 8 3 0.8 0.75 40 1.0

#############################################################################
# Generate skill teaching POMDP instances
#############################################################################
echo Generating Skill Teaching POMDPs
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp_2014/rddl skill_teaching_inst_pomdp__1 2 2 2 0.8 0.75 40 1.0 0.10
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp_2014/rddl skill_teaching_inst_pomdp__2 2 2 3 0.8 0.75 40 1.0 0.15
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp_2014/rddl skill_teaching_inst_pomdp__3 4 4 2 0.8 0.75 40 1.0 0.10
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp_2014/rddl skill_teaching_inst_pomdp__4 4 4 3 0.8 0.75 40 1.0 0.15
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp_2014/rddl skill_teaching_inst_pomdp__5 6 6 2 0.8 0.75 40 1.0 0.10
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp_2014/rddl skill_teaching_inst_pomdp__6 6 6 3 0.8 0.75 40 1.0 0.15
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp_2014/rddl skill_teaching_inst_pomdp__7 7 7 2 0.8 0.75 40 1.0 0.10
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp_2014/rddl skill_teaching_inst_pomdp__8 7 7 3 0.8 0.75 40 1.0 0.15
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp_2014/rddl skill_teaching_inst_pomdp__9 8 8 2 0.8 0.75 40 1.0 0.10
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp_2014/rddl skill_teaching_inst_pomdp__10 8 8 3 0.8 0.75 40 1.0 0.15





