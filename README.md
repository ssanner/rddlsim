#### RDDLSim

Implements a parser, simulator, and client/server evaluation architecture for the relational dynamic influence diagram language (RDDL) -- pronounced "riddle". RDDL is intended to compactly support the representation of a wide range of relational MDPs and POMDPs and support the efficient simulation of these domains. RDDL is used in a number of past and present **International Probabilistic Planning Competitions (IPPCs)**:

* [IPPC 2011 (Discrete)](http://users.cecs.anu.edu.au/~ssanner/IPPC_2011/)
* [IPPC 2014 (Discrete)](https://ssanner.github.io/IPPC_2014/)
* [IPPC 2015 (Continuous)](http://users.cecs.anu.edu.au/~ssanner/IPPC_2014/) -- cancelled 
* [IPPC 2018 (Discrete and Continuous)](https://ipc2018-probabilistic.bitbucket.io/)

#### RDDL Resources:

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

* [RDDL Tutorial Website](https://sites.google.com/site/rddltutorial/) -- a step-by-step guide to building the Wildfire domain, simulating it in RDDLSim, and using the [PROST](https://bitbucket.org/tkeller/prost/wiki/Home) planner with it.

When you checkout the code, the first file you'll want to look at is [INSTALL.txt](https://github.com/ssanner/rddlsim/blob/master/INSTALL.txt)... this includes everything you need to start simulating, visualizing, and translating domains in < 5 minutes!

If you want to use an MDP or POMDP planner in conjunction with RDDL, please check out planner releases at the above IPPC competition web pages... all support the RDDL Client/Server protocol.
