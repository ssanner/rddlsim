#!/bin/sh
./run_policy.sh mdp_instance_list.txt files/final_comp_2014/rddl RandomBoolPolicy rddl.policy.RandomBoolPolicy 2320
./run_policy.sh mdp_instance_list.txt files/final_comp_2014/rddl NoopPolicy rddl.policy.NoopPolicy 2320
./run_policy.sh pomdp_instance_list.txt files/final_comp_2014/rddl RandomBoolPolicy rddl.policy.RandomBoolPolicy 2320
./run_policy.sh pomdp_instance_list.txt files/final_comp_2014/rddl NoopPolicy rddl.policy.NoopPolicy 2320

