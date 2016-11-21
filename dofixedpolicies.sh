#!/bin/sh

./run rddl.sim.DomainExplorer -R files/Reservoir/Reservoir_4/Reservoir.rddl -P rddl.policy.domain.reservoir.FixReservoirPolicy -I is1 -V rddl.viz.ValueVectorDisplay -K 30 >logs/Reservoir_4_log.txt


./run rddl.sim.DomainExplorer -R files/Reservoir/Reservoir_3/Reservoir.rddl -P rddl.policy.domain.reservoir.FixReservoirPolicy -I is1 -V rddl.viz.ValueVectorDisplay -K 30 >logs/Reservoir_3_log.txt


./run rddl.sim.DomainExplorer -R files/HVAC/ROOM_6/HVAC_VAV.rddl2 -P rddl.policy.domain.HVAC.HVACPolicy_VAV_det -I inst_hvac_vav_fix -V rddl.viz.ValueVectorDisplay -K 30  >logs/HVAC_6_log.txt


./run rddl.sim.DomainExplorer -R files/HVAC/ROOM_3/HVAC_VAV.rddl2 -P rddl.policy.domain.HVAC.HVACPolicy_VAV_det -I inst_hvac_vav_fix -V rddl.viz.ValueVectorDisplay -K 30 >logs/HVAC_3_log.txt


./run rddl.sim.DomainExplorer -R files/Navigation/8x8/Navigation_Radius.rddl -P rddl.policy.domain.navigation.StochasticNavigationPolicy -I is1 -V rddl.viz.ValueVectorDisplay -K 30 >logs/Navigation_8x8_log.txt


./run rddl.sim.DomainExplorer -R files/Navigation/10x10/Navigation_Radius.rddl -P rddl.policy.domain.navigation.StochasticNavigationPolicy -I is1 -V rddl.viz.ValueVectorDisplay -K 30 >logs/Navigation_10x10_log.txt
