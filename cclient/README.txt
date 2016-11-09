cclient -- A sample C++ client for the RDDLSim simulator, modified
           from the the MDPSim client by Hakan Younes.

Copyright 2003-2005 Carnegie Mellon University and Rutgers University
Copyright 2007 Hakan Younes
Copyright 2011 Sungwook Yoon, Scott Sanner (modified for RDDLSim)


Compilation
===========

This client is intended as a sample C client that should work with
the Java-based RDDL Server.  

To compile the code, just use the command

   make all

If all goes well then an executable named 'client' (UNIX/Linux) or
'client.exe' (Windows/Cygwin) should be produced.

(If all does not go well... good luck!  Sorry, but we cannot help
 everyone configure these files for their system / compiler.)


Testing
=======

To test the code, run the RDDL server from the rddlsim/ directory:

   ./run rddl.competition.Server files/boolean/rddl/

followed by a client instance, e.g.:

   ./rddlclient     glm1 (UNIX/Linux)
   ./rddlclient.exe glm1 (Windows/Cygwin)

The command line currently only takes one argument which is the
instance name.  The rddlclient currently has a hardcoded action
selection and thus only works with SysAdmin (e.g., instances
glm1, glm2, glp1, glp2).


Modifying for Custom Use
========================

You need only modify 'rddlclient.cc'.

In short, your client has to read the problem state / observations and
return an appropriate action.  Both of these points in the main
communication loop are marked with TODO in 'rddlclient.cc', namely the
methods 'showState(...)' and 'sendAction(...)'.

* For reading the state, you will obviously need to convert the string
  representation into your planner's own internal state or observation
  representation.

* For actions, you can send a noop, which looks like this:

   <actions/>

or equivalently this

   <actions></actions>

Obviously you will need to send non-noop actions.  For most of the
IPPC 2011 competition, concurrency is not being used.  This means that
you can simply send a single action name and associated arguments with
a value of true for the action you wish to execute.  For example, you
will see PPDDL and SPUDD / Symbolic Perseus single action names like
the following in the Game of Life domain:

   set__x1_y1

where the action name is separated from the arguments by a double
underscore "__" and the arguments are separated by a single underscore
"_".  To send such an action to the RDDL server as XML you would send:

   <actions>
       <action>
           <action-name>set</action-name>
           <action-arg>x1</action-arg>
           <action-arg>y1</action-arg>
           <action-value>true</action-value>
       </action>
   </actions>

For concurrent domains like traffic, you will see PPDDL and 
SPUDD / Symbolic Perseus *joint* action names like:

  advance__ia2a4___advance__ia2a8 

This is actually two actions separated by a triple underscore
"___".  To send such an action to the RDDL server, simply separate
the joint action into singleton actions and send the following XML:

   <actions>
       <action>
           <action-name>advance</action-name>
           <action-arg>ia2a4</action-arg>
           <action-value>true</action-value>
       </action>
       <action>
           <action-name>advance</action-name>
           <action-arg>ia2a8</action-arg>
           <action-value>true</action-value>
       </action>
   </actions>

* For reading an instance file, note that we do not provide a C/C++
  parser for any of the formats (RDDL, PPDDL, SPUDD / Symbolic Perseus).
  Competitors need to write their own parsers or use existing C/C++
  parsers, e.g., MDPSim for PPDDL: http://www.tempastic.org/mdpsim/
