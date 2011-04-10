General Information on RDDLSim Evaluation
=========================================

To evaluate log files from RDDLSim, there are two steps.

(1) Generate a file of min/max scores per instance for normalization.

Simply run rddl.evaluate.MinMaxEval with the sole argument containing
the directory of all log files to be processed including the Random 
and NOOP policies.

[NOTE: For comparison to the IPPC 2011 normalized scores, these files
 are already provided, so you should download them from the IPPC 
 website and skip this step.]
 
(2) Process all log files and produce raw and normalized scores aggregated
    by instance, domain, and overall.
    
Simply run rddl.evaluate.FinalEval with the sole argument containing
the directory of all log files to be processed.  This directory
must contain the file "min_max_norm_constants.txt", either downloaded
or generated in step (1) above.
