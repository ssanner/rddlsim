#!/bin/sh

./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy elevators_inst_mdp__9 rddl.viz.ElevatorDisplay
./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy elevators_inst_pomdp__9 rddl.viz.ElevatorDisplay

./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy game_of_life_inst_mdp__10 rddl.viz.GameOfLifeScreenDisplay
./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy game_of_life_inst_pomdp__10 rddl.viz.GameOfLifeScreenDisplay

./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy sysadmin_inst_mdp__10 rddl.viz.SysAdminScreenDisplay
./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy sysadmin_inst_pomdp__10 rddl.viz.SysAdminScreenDisplay

./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy traffic_inst_mdp__10 rddl.viz.TrafficDisplay
./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy traffic_inst_pomdp__10 rddl.viz.TrafficDisplay

./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy crossing_traffic_inst_mdp__10 rddl.viz.CrossingTrafficDisplay
./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy crossing_traffic_inst_pomdp__10 rddl.viz.CrossingTrafficDisplay

./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy recon_inst_mdp__10
./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy recon_inst_pomdp__10

./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy skill_teaching_inst_mdp__10
./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy skill_teaching_inst_pomdp__10

./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy navigation_inst_mdp__10 rddl.viz.NavigationDisplay
./run rddl.sim.Simulator files/final_comp/rddl rddl.policy.RandomBoolPolicy navigation_inst_pomdp__10 rddl.viz.NavigationDisplay
