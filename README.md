#### Java RDDLSim (this repo)

Implements a Java parser, simulator, and client/server evaluation architecture for the relational dynamic influence diagram language (RDDL) -- pronounced "riddle". RDDL is intended to compactly support the representation of a wide range of relational MDPs and POMDPs and support the efficient simulation of these domains. 

When you checkout the Java code, the first file you'll want to look at is [INSTALL.txt](https://github.com/ssanner/rddlsim/blob/master/INSTALL.txt)... this includes everything you need to start simulating, visualizing, and translating domains in < 5 minutes!

#### Python RDDLGym (external repo)

Please see [PyRDDLGym](https://github.com/pyrddlgym-project) -- **consider migrating to the Python version unless you have a Java requirement**.  Java planners can be interfaced to PyRDDLGym (see how this is done for [PROST](https://github.com/pyrddlgym-project/pyRDDLGym-prost)).

#### International Probabilistic Planning Competitions (IPPCs)

* [IPPC 2011 (Discrete)](http://users.cecs.anu.edu.au/~ssanner/IPPC_2011/)
* [IPPC 2014 (Discrete)](https://ssanner.github.io/IPPC_2014/)
* [IPPC 2015 (Continuous)](http://users.cecs.anu.edu.au/~ssanner/IPPC_2014/) -- cancelled 
* [IPPC 2018 (Discrete)](https://ipc2018-probabilistic.bitbucket.io/)
* [IPPC 2023 (Discrete and Continuous)](https://ataitler.github.io/IPPC2023/)

#### RDDL Resources and Tutorials:

* [RDDL Language Guide](http://users.cecs.anu.edu.au/~ssanner/IPPC_2011/RDDL.pdf)

  Please cite as

```
   @unpublished{Sanner:RDDL,
      author = "Scott Sanner",
      title = "Relational Dynamic Influence Diagram Language (RDDL): Language Description",
      note = "http://users.cecs.anu.edu.au/~ssanner/IPPC_2011/RDDL.pdf",
      year = 2010}
```

* [RDDL Tutorial Slides](http://users.rsise.anu.edu.au/~ssanner/Papers/RDDL_Tutorial_ICAPS_2014.pdf)

* [RDDL Wildfire Tutorial](https://ataitler.github.io/IPPC2023/pyrddlgym_rddl_tutorial.html) -- a step-by-step guide to building the Wildfire domain, exercises, and using the [PROST](https://github.com/prost-planner/prost) planner with it in PyRDDLGym.

* [RDDL + PyRDDLGym Tutorial](https://pyrddlgym-project.github.io/AAAI24-lab) -- RDDL language overview followed by tutorial exercises and Google Colab examples in Python for PyRDDLGym.

