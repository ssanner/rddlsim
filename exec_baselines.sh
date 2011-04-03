#!/bin/sh
./run_policy.sh files/test_comp/mdp_instance_list.txt files/test_comp/rddl RandomBoolPolicy rddl.policy.RandomBoolPolicy 2320
./run_policy.sh files/test_comp/mdp_instance_list.txt files/test_comp/rddl NoopPolicy rddl.policy.NoopPolicy 2320
./run_policy.sh files/test_comp/pomdp_instance_list.txt files/test_comp/rddl RandomBoolPolicy rddl.policy.RandomBoolPolicy 2320
./run_policy.sh files/test_comp/pomdp_instance_list.txt files/test_comp/rddl NoopPolicy rddl.policy.NoopPolicy 2320

