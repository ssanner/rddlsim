% Copyright 2014, Gurobi Optimization, Inc.
%
% This example formulates and solves the following simple LP model:
% maximize
%       x + 2 y + 3 z
% subject to
%       x +   y        <= 1
%             y +   z  <= 1
%
clear model;
model.A = sparse([1 1 0; 0 1 1]);
model.obj = [1 2 3];
model.modelsense = 'Max';
model.rhs = [1 1];
model.sense = [ '<' '<'];

result = gurobi(model)

disp(result.objval);
disp(result.x);

% Alterantive representation of A - as sparse triplet matrix
i = [1; 1; 2; 2];
j = [1; 2; 2; 3];
x = [1; 1; 1; 1];
model.A = sparse(i, j, x, 2, 3);

clear params;
params.method = 2;
params.timelimit = 100;

result = gurobi(model, params);

disp(result.objval);
disp(result.x)
