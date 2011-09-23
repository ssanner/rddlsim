Please see Trf_Documentation.pdf for the report detailing the traffic
models in this directory.

To run the traffic simulator/visualizer/policies with these models,
use the following command:

./run rddl.sim.Simulator files/rddl/examples/traffic_cont_ctm/ <Policy> <Instance> rddl.viz.trfctm.TrfDisplay

Possible generic instances are:
- is_simple
- is_roundabout
- is_intersection
- is_grid

Possible instances for 3x3 grid traffic with high/medium/low 
eastbound_westbound traffic configurations are below:
- is_trf_grid_h_h
- is_trf_grid_h_m
- is_trf_grid_h_l
- is_trf_grid_m_h
- is_trf_grid_m_m
- is_trf_grid_m_l
- is_trf_grid_l_h
- is_trf_grid_l_m
- is_trf_grid_l_l

Possible generic policies that work with all models are:
- rddl.policy.RandomEnumPolicy

Possibly policies that work only with the 3x3 is_trf_grid_?_? are
- rddl.policy.trfctm.TrfTestPolicy
- rddl.policy.trfctm.TrfRandomPolicy
- rddl.policy.trfctm.TrfFixedTimePolicy

So for example, one can run
./run rddl.sim.Simulator files/rddl/examples/traffic_cont_ctm/ rddl.policy.RandomEnumPolicy is_roundabout rddl.viz.trfctm.TrfDisplay
./run rddl.sim.Simulator files/rddl/examples/traffic_cont_ctm/ rddl.policy.RandomEnumPolicy is_intersection rddl.viz.trfctm.TrfDisplay
./run rddl.sim.Simulator files/rddl/examples/traffic_cont_ctm/ rddl.policy.trfctm.TrfRandomPolicy is_trf_grid_h_l rddl.viz.trfctm.TrfDisplay
./run rddl.sim.Simulator files/rddl/examples/traffic_cont_ctm/ rddl.policy.trfctm.TrfFixedTimePolicy is_trf_grid_l_h rddl.viz.trfctm.TrfDisplay
