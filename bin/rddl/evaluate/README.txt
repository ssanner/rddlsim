General Information on RDDLSim Evaluation
=========================================

To evaluate log files from RDDLSim, there are two steps.

(1) Generate a file of min/max scores per instance for normalization.

      rddl.evaluate.MinMaxEval [--time-limit TIME] [--num-trials NUM] <directory of RDDL .log files>
    
    for example, the commands used to evaluate different competitions:
    
      IPPC 2011: rddl.evaluate.MinMaxEval --num-trials 30 FinalComp2011/MDP/
      IPPC 2014: rddl.evaluate.MinMaxEval --time-limit 1080000 --num-trials 30 FinalComp2014/MDP/

Simply run rddl.evaluate.MinMaxEval with the sole argument containing
the directory of all log files to be processed including the Random 
and NOOP policies.  Produces file "min_max_norm_constants.txt".

[NOTE: For *comparison* to the IPPC 2011 normalized scores, this file
 are already provided, so you should download them from the IPPC 
 website and skip this step.]

(2) Process all log files and produce raw and normalized scores aggregated
    by instance, domain, and overall.

      rddl.evaluate.FinalEval [--time-limit TIME] [--num-trials NUM] <directory of RDDL .log files>
    
    for example, the commands used to evaluate different competitions:
    
      IPPC 2011: rddl.evaluate.FinalEval --num-trials 30 FinalComp/MDP/
      IPPC 2014: rddl.evaluate.FinalEval --time-limit 1080000 --num-trials 30 FinalComp/MDP/
    
Simply run rddl.evaluate.FinalEval with the sole argument containing
the directory of all log files to be processed.  This directory
must contain the file "min_max_norm_constants.txt", either downloaded
or generated in step (1) above.  Produces file "all_results.txt".

===

Ignoring specific client names:

You can also place an optional list of client-names to ignore
in a file 'IGNORE_CLIENT_LIST.txt' (with one client name per line)
in the same directory as the log files. 

===

Time limits (new in 2014):

To enforce the time limit of 1080000 ms for 30 trials, prefix 
the above commands in (1) and (2) with "--enforce-time-limit".

===

Log file archives from past competitions:

Please see the respective competition websites... they should
link to an archive of these files. 