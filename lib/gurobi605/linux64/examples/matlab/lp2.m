% Copyright 2014, Gurobi Optimization, Inc.
%
% Formulate a simple linear program, solve it, and then solve it
% again using the optimal basis.

clear model;
model.A = sparse([1 3 4; 8 2 3]);
model.obj = [1 2 3];
model.rhs = [4 7];
model.sense = ['>' '>'];

% First solve requires a few simplex iterations
result = gurobi(model)

model.vbasis = result.vbasis;
model.cbasis = result.cbasis;

% Second solve - start from an optimal basis, so no iterations

result = gurobi(model)
