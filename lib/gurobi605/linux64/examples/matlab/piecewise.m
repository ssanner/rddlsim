% Copyright 2014, Gurobi Optimization, Inc.

% This example considers the following separable, convex problem:
%
%   minimize    f(x) - y + g(z)
%   subject to  x + 2 y + 3 z <= 4
%               x +   y       >= 1
%               x,    y,    z <= 1
%
% where f(u) = exp(-u) and g(u) = 2 u^2 - 4 u, for all real u. It
% formulates and solves a simpler LP model by approximating f and
% g with piecewise-linear functions. Then it transforms the model
% into a MIP by negating the approximation for f, which corresponds
% to a non-convex piecewise-linear function, and solves it again.

names = {'x'; 'y'; 'z'};

try
    clear model;
    model.A = sparse([1 2 3; 1 1 0]);
    model.obj = [0; -1; 0];
    model.rhs = [4; 1];
    model.sense = '<>';
    model.vtype = 'C';
    model.lb = [0; 0; 0];
    model.ub = [1; 1; 1];
    model.varnames = names;

    % Compute f and g on 101 points in [0,1]
    u = linspace(0.0, 1.0, 101);
    f = exp(-u);
    g = 2*u.^2 - 4*u;

    % Set piecewise linear objective f(x)
    model.pwlobj(1).var = 1;
    model.pwlobj(1).x   = u;
    model.pwlobj(1).y   = f;

    % Set piecewise linear objective g(z)
    model.pwlobj(2).var   = 3;
    model.pwlobj(2).x     = u;
    model.pwlobj(2).y     = g;

    % Optimize model as LP
    result = gurobi(model);

    disp(result);

    for v=1:length(names)
        fprintf('%s %d\n', names{v}, result.x(v));
    end

    fprintf('Obj: %e\n', result.objval);

    % Negate piecewise-linear objective function for x
    f = -f;
    model.pwlobj(1).y = f;

    gurobi_write(model, 'pwl.lp')

    % Optimize model as a MIP
    result = gurobi(model);

    disp(result);

    for v=1:length(names)
        fprintf('%s %d\n', names{v}, result.x(v));
    end

    fprintf('Obj: %e\n', result.objval);

catch gurobiError
     fprintf('Error reported\n');
end
