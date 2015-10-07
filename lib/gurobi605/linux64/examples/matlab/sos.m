% Copyright 2014, Gurobi Optimization, Inc.

% This example creates a very simple Special Ordered Set (SOS)
% model. The model consists of 3 continuous variables, no linear
% constraints, and a pair of SOS constraints of type 1.

try
    clear model;
    model.ub = [1 1 2];
    model.obj = [2 1 1];
    model.modelsense = 'Max';
    model.A = sparse(1,3);
    model.rhs = 0;
    model.sense = '=';

    % Add first SOS: x1 = 0 or x2 = 0
    model.sos(1).type   = 1;
    model.sos(1).index  = [1 2];
    model.sos(1).weight = [1 2];

    % Add second SOS: x1 = 0 or x3 = 0
    model.sos(2).type   = 1;
    model.sos(2).index  = [1 3];
    model.sos(2).weight = [1 2];

    % Write model to file
    gurobi_write(model, 'sos.lp');

    result = gurobi(model);

    for i=1:3
        fprintf('x%d %e\n', i, result.x(i))
    end

    fprintf('Obj: %e\n', result.objval);

catch gurobiError
    fprintf('Encountered an error\n')
end
