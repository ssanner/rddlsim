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
./run rddl.competition.generators.SysAdminMDPGen files/final_comp/rddl sysadmin_inst_mdp__1 10 2 0.05 40 1.0
./run rddl.competition.generators.SysAdminMDPGen files/final_comp/rddl sysadmin_inst_mdp__2 10 3 0.05 40 1.0
./run rddl.competition.generators.SysAdminMDPGen files/final_comp/rddl sysadmin_inst_mdp__3 20 2 0.04 40 1.0
./run rddl.competition.generators.SysAdminMDPGen files/final_comp/rddl sysadmin_inst_mdp__4 20 3 0.04 40 1.0
./run rddl.competition.generators.SysAdminMDPGen files/final_comp/rddl sysadmin_inst_mdp__5 30 2 0.03 40 1.0
./run rddl.competition.generators.SysAdminMDPGen files/final_comp/rddl sysadmin_inst_mdp__6 30 3 0.03 40 1.0
./run rddl.competition.generators.SysAdminMDPGen files/final_comp/rddl sysadmin_inst_mdp__7 40 2 0.02 40 1.0
./run rddl.competition.generators.SysAdminMDPGen files/final_comp/rddl sysadmin_inst_mdp__8 40 3 0.02 40 1.0
./run rddl.competition.generators.SysAdminMDPGen files/final_comp/rddl sysadmin_inst_mdp__9 50 2 0.01 40 1.0
./run rddl.competition.generators.SysAdminMDPGen files/final_comp/rddl sysadmin_inst_mdp__10 50 3 0.01 40 1.0

#############################################################################
# Generate SysAdmin POMDP instances
#############################################################################
echo Generating SysAdmin POMDPs
./run rddl.competition.generators.SysAdminPOMDPGen files/final_comp/rddl sysadmin_inst_pomdp__1 10 2 0.02 40 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/final_comp/rddl sysadmin_inst_pomdp__2 10 3 0.02 40 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/final_comp/rddl sysadmin_inst_pomdp__3 20 2 0.015 40 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/final_comp/rddl sysadmin_inst_pomdp__4 20 3 0.015 40 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/final_comp/rddl sysadmin_inst_pomdp__5 30 2 0.01 40 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/final_comp/rddl sysadmin_inst_pomdp__6 30 3 0.01 40 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/final_comp/rddl sysadmin_inst_pomdp__7 40 2 0.005 40 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/final_comp/rddl sysadmin_inst_pomdp__8 40 3 0.005 40 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/final_comp/rddl sysadmin_inst_pomdp__9 50 2 0.002 40 1.0
./run rddl.competition.generators.SysAdminPOMDPGen files/final_comp/rddl sysadmin_inst_pomdp__10 50 3 0.002 40 1.0

#############################################################################
# Generate elevator MDP instances
#############################################################################
echo Generating Elevator MDPs
./run rddl.competition.generators.ElevatorMDPGen files/final_comp/rddl elevators_inst_mdp__1 1 3 0.5 0.1 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp/rddl elevators_inst_mdp__2 2 3 0.5 0.2 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp/rddl elevators_inst_mdp__3 2 3 0.7 0.3 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp/rddl elevators_inst_mdp__4 1 4 0.5 0.1 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp/rddl elevators_inst_mdp__5 2 4 0.5 0.2 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp/rddl elevators_inst_mdp__6 2 4 0.6 0.3 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp/rddl elevators_inst_mdp__7 1 5 0.4 0.1 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp/rddl elevators_inst_mdp__8 2 5 0.4 0.2 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp/rddl elevators_inst_mdp__9 2 5 0.5 0.3 40 1.0
./run rddl.competition.generators.ElevatorMDPGen files/final_comp/rddl elevators_inst_mdp__10 1 6 0.3 0.1 40 1.0

#############################################################################
# Generate elevator POMDP instances
#############################################################################
echo Generating Elevator POMDPs
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp/rddl elevators_inst_pomdp__1 1 3 0.3 0.1 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp/rddl elevators_inst_pomdp__2 2 3 0.3 0.2 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp/rddl elevators_inst_pomdp__3 2 3 0.5 0.3 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp/rddl elevators_inst_pomdp__4 1 4 0.3 0.1 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp/rddl elevators_inst_pomdp__5 2 4 0.3 0.2 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp/rddl elevators_inst_pomdp__6 2 4 0.4 0.2 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp/rddl elevators_inst_pomdp__7 1 5 0.2 0.1 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp/rddl elevators_inst_pomdp__8 2 5 0.2 0.1 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp/rddl elevators_inst_pomdp__9 2 5 0.3 0.2 40 1.0
./run rddl.competition.generators.ElevatorPOMDPGen files/final_comp/rddl elevators_inst_pomdp__10 1 6 0.2 0.1 40 1.0

#############################################################################
# Generate game of life MDP instances
#############################################################################
echo Generating Game of Life MDPs
./run rddl.competition.generators.GameOfLifeMDPGen files/final_comp/rddl game_of_life_inst_mdp__1 3 3 0.01 0.05 0.5 40 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/final_comp/rddl game_of_life_inst_mdp__2 3 3 0.05 0.10 0.5 40 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/final_comp/rddl game_of_life_inst_mdp__3 3 3 0.10 0.15 0.5 40 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/final_comp/rddl game_of_life_inst_mdp__4 4 4 0.01 0.05 0.5 40 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/final_comp/rddl game_of_life_inst_mdp__5 4 4 0.05 0.10 0.5 40 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/final_comp/rddl game_of_life_inst_mdp__6 4 4 0.10 0.15 0.5 40 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/final_comp/rddl game_of_life_inst_mdp__7 5 5 0.01 0.05 0.5 40 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/final_comp/rddl game_of_life_inst_mdp__8 5 5 0.03 0.07 0.5 40 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/final_comp/rddl game_of_life_inst_mdp__9 5 5 0.05 0.09 0.5 40 1.0
./run rddl.competition.generators.GameOfLifeMDPGen files/final_comp/rddl game_of_life_inst_mdp__10 10 3 0.01 0.03 0.5 40 1.0

#############################################################################
# Generate game of life POMDP instances
#############################################################################
echo Generating Game of Life POMDPs
./run rddl.competition.generators.GameOfLifePOMDPGen files/final_comp/rddl game_of_life_inst_pomdp__1 3 3 0.01 0.05 0.5 40 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/final_comp/rddl game_of_life_inst_pomdp__2 3 3 0.05 0.10 0.5 40 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/final_comp/rddl game_of_life_inst_pomdp__3 3 3 0.10 0.15 0.5 40 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/final_comp/rddl game_of_life_inst_pomdp__4 4 4 0.01 0.05 0.5 40 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/final_comp/rddl game_of_life_inst_pomdp__5 4 4 0.05 0.10 0.5 40 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/final_comp/rddl game_of_life_inst_pomdp__6 4 4 0.10 0.15 0.5 40 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/final_comp/rddl game_of_life_inst_pomdp__7 5 5 0.01 0.05 0.5 40 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/final_comp/rddl game_of_life_inst_pomdp__8 5 5 0.03 0.07 0.5 40 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/final_comp/rddl game_of_life_inst_pomdp__9 5 5 0.05 0.09 0.5 40 1.0
./run rddl.competition.generators.GameOfLifePOMDPGen files/final_comp/rddl game_of_life_inst_pomdp__10 10 3 0.01 0.03 0.5 40 1.0

#############################################################################
# Generate traffic MDP instances
#############################################################################
echo Generating Traffic MDPs
./run rddl.competition.generators.TrafficMDPGen files/final_comp/rddl traffic_inst_mdp__1 2 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp/rddl traffic_inst_mdp__2 2 0.1 0.5 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp/rddl traffic_inst_mdp__3 3 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp/rddl traffic_inst_mdp__4 3 0.1 0.5 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp/rddl traffic_inst_mdp__5 4 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp/rddl traffic_inst_mdp__6 4 0.1 0.5 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp/rddl traffic_inst_mdp__7 5 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp/rddl traffic_inst_mdp__8 5 0.1 0.5 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp/rddl traffic_inst_mdp__9 6 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficMDPGen files/final_comp/rddl traffic_inst_mdp__10 6 0.1 0.5 40 1.0

#############################################################################
# Generate traffic POMDP instances
#############################################################################
echo Generating Traffic POMDPs
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp/rddl traffic_inst_pomdp__1 2 0.1 0.2 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp/rddl traffic_inst_pomdp__2 2 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp/rddl traffic_inst_pomdp__3 3 0.1 0.2 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp/rddl traffic_inst_pomdp__4 3 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp/rddl traffic_inst_pomdp__5 4 0.1 0.2 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp/rddl traffic_inst_pomdp__6 4 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp/rddl traffic_inst_pomdp__7 5 0.1 0.2 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp/rddl traffic_inst_pomdp__8 5 0.1 0.3 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp/rddl traffic_inst_pomdp__9 6 0.1 0.2 40 1.0
./run rddl.competition.generators.TrafficPOMDPGen files/final_comp/rddl traffic_inst_pomdp__10 6 0.1 0.3 40 1.0

#############################################################################
# Generate crossing traffic MDP instances
#############################################################################
echo Generating Crossing Traffic MDPs
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp/rddl crossing_traffic_inst_mdp__1 3 3 0.3 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp/rddl crossing_traffic_inst_mdp__2 3 3 0.6 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp/rddl crossing_traffic_inst_mdp__3 4 4 0.3 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp/rddl crossing_traffic_inst_mdp__4 4 4 0.6 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp/rddl crossing_traffic_inst_mdp__5 5 5 0.2 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp/rddl crossing_traffic_inst_mdp__6 5 5 0.4 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp/rddl crossing_traffic_inst_mdp__7 6 6 0.2 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp/rddl crossing_traffic_inst_mdp__8 6 6 0.4 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp/rddl crossing_traffic_inst_mdp__9 7 7 0.1 40 1.0
./run rddl.competition.generators.CrossingTrafficMDPGen files/final_comp/rddl crossing_traffic_inst_mdp__10 7 7 0.3 40 1.0

#############################################################################
# Generate crossing traffic POMDP instances
#############################################################################
echo Generating Crossing Traffic POMDPs
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp/rddl crossing_traffic_inst_pomdp__1 3 3 0.2 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp/rddl crossing_traffic_inst_pomdp__2 3 3 0.4 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp/rddl crossing_traffic_inst_pomdp__3 4 4 0.2 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp/rddl crossing_traffic_inst_pomdp__4 4 4 0.4 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp/rddl crossing_traffic_inst_pomdp__5 5 5 0.1 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp/rddl crossing_traffic_inst_pomdp__6 5 5 0.3 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp/rddl crossing_traffic_inst_pomdp__7 6 6 0.1 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp/rddl crossing_traffic_inst_pomdp__8 6 6 0.3 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp/rddl crossing_traffic_inst_pomdp__9 7 7 0.1 40 1.0
./run rddl.competition.generators.CrossingTrafficPOMDPGen files/final_comp/rddl crossing_traffic_inst_pomdp__10 7 7 0.2 40 1.0

#############################################################################
# Generate recon MDP instances
#############################################################################
echo Generating Recon MDPs
./run rddl.competition.generators.ReconMDPGen files/final_comp/rddl recon_inst_mdp__1 2 4 0.4 0.25 0.7 40 1.0
./run rddl.competition.generators.ReconMDPGen files/final_comp/rddl recon_inst_mdp__2 2 4 0.6 0.25 0.7 40 1.0
./run rddl.competition.generators.ReconMDPGen files/final_comp/rddl recon_inst_mdp__3 3 5 0.4 0.15 0.7 40 1.0
./run rddl.competition.generators.ReconMDPGen files/final_comp/rddl recon_inst_mdp__4 3 5 0.6 0.15 0.7 40 1.0
./run rddl.competition.generators.ReconMDPGen files/final_comp/rddl recon_inst_mdp__5 4 6 0.4 0.15 0.7 40 1.0
./run rddl.competition.generators.ReconMDPGen files/final_comp/rddl recon_inst_mdp__6 4 6 0.5 0.15 0.7 40 1.0
./run rddl.competition.generators.ReconMDPGen files/final_comp/rddl recon_inst_mdp__7 4 6 0.6 0.15 0.7 40 1.0
./run rddl.competition.generators.ReconMDPGen files/final_comp/rddl recon_inst_mdp__8 5 7 0.4 0.15 0.7 40 1.0
./run rddl.competition.generators.ReconMDPGen files/final_comp/rddl recon_inst_mdp__9 5 7 0.5 0.15 0.7 40 1.0
./run rddl.competition.generators.ReconMDPGen files/final_comp/rddl recon_inst_mdp__10 5 7 0.6 0.15 0.45 40 1.0

#############################################################################
# Generate recon POMDP instances
#############################################################################
echo Generating Recon POMDPs
./run rddl.competition.generators.ReconPOMDPGen files/final_comp/rddl recon_inst_pomdp__1 2 4 0.4 0.25 0.7 40 1.0 0.10
./run rddl.competition.generators.ReconPOMDPGen files/final_comp/rddl recon_inst_pomdp__2 2 4 0.6 0.25 0.7 40 1.0 0.25
./run rddl.competition.generators.ReconPOMDPGen files/final_comp/rddl recon_inst_pomdp__3 3 5 0.4 0.15 0.7 40 1.0 0.10
./run rddl.competition.generators.ReconPOMDPGen files/final_comp/rddl recon_inst_pomdp__4 3 5 0.6 0.15 0.7 40 1.0 0.25
./run rddl.competition.generators.ReconPOMDPGen files/final_comp/rddl recon_inst_pomdp__5 4 6 0.4 0.15 0.7 40 1.0 0.10
./run rddl.competition.generators.ReconPOMDPGen files/final_comp/rddl recon_inst_pomdp__6 4 6 0.5 0.15 0.7 40 1.0 0.15
./run rddl.competition.generators.ReconPOMDPGen files/final_comp/rddl recon_inst_pomdp__7 4 6 0.6 0.15 0.7 40 1.0 0.25
./run rddl.competition.generators.ReconPOMDPGen files/final_comp/rddl recon_inst_pomdp__8 5 7 0.4 0.15 0.7 40 1.0 0.10
./run rddl.competition.generators.ReconPOMDPGen files/final_comp/rddl recon_inst_pomdp__9 5 7 0.5 0.15 0.7 40 1.0 0.15
./run rddl.competition.generators.ReconPOMDPGen files/final_comp/rddl recon_inst_pomdp__10 5 7 0.6 0.15 0.45 40 1.0 0.25

#############################################################################
# Generate skill teaching MDP instances
#############################################################################
echo Generating Skill Teaching MDPs
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp/rddl skill_teaching_inst_mdp__1 2 2 2 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp/rddl skill_teaching_inst_mdp__2 2 2 3 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp/rddl skill_teaching_inst_mdp__3 4 4 2 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp/rddl skill_teaching_inst_mdp__4 4 4 3 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp/rddl skill_teaching_inst_mdp__5 6 6 2 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp/rddl skill_teaching_inst_mdp__6 6 6 3 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp/rddl skill_teaching_inst_mdp__7 7 7 2 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp/rddl skill_teaching_inst_mdp__8 7 7 3 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp/rddl skill_teaching_inst_mdp__9 8 8 2 0.8 0.75 40 1.0
./run rddl.competition.generators.SkillTeachingMDPGen files/final_comp/rddl skill_teaching_inst_mdp__10 8 8 3 0.8 0.75 40 1.0

#############################################################################
# Generate skill teaching POMDP instances
#############################################################################
echo Generating Skill Teaching POMDPs
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp/rddl skill_teaching_inst_pomdp__1 2 2 2 0.8 0.75 40 1.0 0.10
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp/rddl skill_teaching_inst_pomdp__2 2 2 3 0.8 0.75 40 1.0 0.15
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp/rddl skill_teaching_inst_pomdp__3 4 4 2 0.8 0.75 40 1.0 0.10
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp/rddl skill_teaching_inst_pomdp__4 4 4 3 0.8 0.75 40 1.0 0.15
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp/rddl skill_teaching_inst_pomdp__5 6 6 2 0.8 0.75 40 1.0 0.10
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp/rddl skill_teaching_inst_pomdp__6 6 6 3 0.8 0.75 40 1.0 0.15
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp/rddl skill_teaching_inst_pomdp__7 7 7 2 0.8 0.75 40 1.0 0.10
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp/rddl skill_teaching_inst_pomdp__8 7 7 3 0.8 0.75 40 1.0 0.15
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp/rddl skill_teaching_inst_pomdp__9 8 8 2 0.8 0.75 40 1.0 0.10
./run rddl.competition.generators.SkillTeachingPOMDPGen files/final_comp/rddl skill_teaching_inst_pomdp__10 8 8 3 0.8 0.75 40 1.0 0.15

#############################################################################
# Generate navigation MDP instances
#############################################################################
echo Generating Navigation MDPs
./run rddl.competition.generators.NavigationMDPGen files/final_comp/rddl navigation_inst_mdp__1 4 3 obfuscate 40 1.0
./run rddl.competition.generators.NavigationMDPGen files/final_comp/rddl navigation_inst_mdp__2 5 3 obfuscate 40 1.0
./run rddl.competition.generators.NavigationMDPGen files/final_comp/rddl navigation_inst_mdp__3 5 4 obfuscate 40 1.0
./run rddl.competition.generators.NavigationMDPGen files/final_comp/rddl navigation_inst_mdp__4 5 6 obfuscate 40 1.0
./run rddl.competition.generators.NavigationMDPGen files/final_comp/rddl navigation_inst_mdp__5 10 3 obfuscate 40 1.0
./run rddl.competition.generators.NavigationMDPGen files/final_comp/rddl navigation_inst_mdp__6 10 4 obfuscate 40 1.0
./run rddl.competition.generators.NavigationMDPGen files/final_comp/rddl navigation_inst_mdp__7 10 5 obfuscate 40 1.0
./run rddl.competition.generators.NavigationMDPGen files/final_comp/rddl navigation_inst_mdp__8 20 3 obfuscate 40 1.0
./run rddl.competition.generators.NavigationMDPGen files/final_comp/rddl navigation_inst_mdp__9 20 4 obfuscate 40 1.0
./run rddl.competition.generators.NavigationMDPGen files/final_comp/rddl navigation_inst_mdp__10 20 5 obfuscate 40 1.0

#############################################################################
# Generate navigation POMDP instances
#############################################################################
echo Generating Navigation POMDPs
./run rddl.competition.generators.NavigationPOMDPGen files/final_comp/rddl navigation_inst_pomdp__1 4 3 obfuscate 40 1.0
./run rddl.competition.generators.NavigationPOMDPGen files/final_comp/rddl navigation_inst_pomdp__2 5 3 obfuscate 40 1.0
./run rddl.competition.generators.NavigationPOMDPGen files/final_comp/rddl navigation_inst_pomdp__3 5 4 obfuscate 40 1.0
./run rddl.competition.generators.NavigationPOMDPGen files/final_comp/rddl navigation_inst_pomdp__4 5 6 obfuscate 40 1.0
./run rddl.competition.generators.NavigationPOMDPGen files/final_comp/rddl navigation_inst_pomdp__5 10 3 obfuscate 40 1.0
./run rddl.competition.generators.NavigationPOMDPGen files/final_comp/rddl navigation_inst_pomdp__6 10 4 obfuscate 40 1.0
./run rddl.competition.generators.NavigationPOMDPGen files/final_comp/rddl navigation_inst_pomdp__7 10 5 obfuscate 40 1.0
./run rddl.competition.generators.NavigationPOMDPGen files/final_comp/rddl navigation_inst_pomdp__8 20 3 obfuscate 40 1.0
./run rddl.competition.generators.NavigationPOMDPGen files/final_comp/rddl navigation_inst_pomdp__9 20 4 obfuscate 40 1.0
./run rddl.competition.generators.NavigationPOMDPGen files/final_comp/rddl navigation_inst_pomdp__10 20 5 obfuscate 40 1.0

#############################################################################
# Generate RDDL Prefix, (PO-)PPDDL, SPUDD, and Symbolic Perseus translations
#############################################################################

./run rddl.translate.RDDL2Prefix files/final_comp/rddl files/final_comp/rddl_prefix
./run rddl.translate.RDDL2Format files/final_comp/rddl files/final_comp/spudd_sperseus spudd_sperseus
./run rddl.translate.RDDL2Format files/final_comp/rddl files/final_comp/ppddl ppddl

#############################################################################
# Generate Instance Lists
#############################################################################
DIR="files/final_comp/rddl"
MDP_FILE="files/final_comp/mdp_instance_list.txt"

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

POMDP_FILE="files/final_comp/pomdp_instance_list.txt"

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

