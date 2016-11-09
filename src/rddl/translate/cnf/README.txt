cnf Package Description
=========================================
rddl.translate.cnf

rddlsim/src/rddl/translate/cnf provides the following files:
CNF.java------------------| Class of conjunctive normal form, support translation from rddl to SAT.
			  | Some code of POMDP was programmed, but not be tested.
CNFClause.java------------| Class of Clause
			  | Some code of POMDP was programmed, but not be tested.
Literal.java--------------| Class of Literal
			  | Some code of POMDP was programmed, but not be tested.
ManualWorkPolicy.java-----| Implementation of rddl.Policy, need choose which action to execute in each timestep. 
			  | Support console I/O both in linux terminal and Eclipse window.
SolutionReader.java-------| Read solution from output of RSAT, need input CNF file of the problem too.
TestElevatorPolicy.java---| Implementation of rddl.Policy. For each tiemstep, 
			  | check current state--->
			  | update starting state of cnf instance--->
			  | if necessary, try to find a new plan and replace the old one--->
			  | get next action of current plan.

As a part of rddlsim, this package can be compiled by:
    compile     For Windows/Cygwin(not tested) and UNIX/Linux systems
    compile.bat For the Windows CMD prompt(not tested)

For running from a terminal, please follow the instruction of rddlsim/INSTALL.txt.


Basic Usage
===================

For running the SolutionReader:
./run cnf.SolutionReader cnf-file-name solution-file-name
./run cnf.SolutionReader cnf-file-name solution-file-name output-dir

For translate *.rddl to *.cnf:
./run rddl.translate.cnf.CNF RDDL-file/directory output-dir

For running the simulator for elevator benchmark:

Make sure all paths list below are valid(or Modify the source code in TestElevatorPolicy.java)
WROOT/test_input/elevators_mdp_testinput3.rddl
WROOT/result/
WROOT/rsat_2.02_cost_simple/rsat


./run rddl.sim.Simulator RDDL-file cnf.TestElevatorPolicy instance-name rddl.viz.ElevatorDisplay


