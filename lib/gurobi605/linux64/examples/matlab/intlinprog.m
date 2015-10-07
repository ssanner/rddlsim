function [x, fval, exitflag] = intlinprog(f, intcon, A, b, Aeq, beq, lb, ub)
%INTLINPROG A mixed integer linear programming example using the
%   Gurobi MATLAB interface
%
%   This example is based on the intlinprog interface defined in the
%   MATLAB Optimization Toolbox. The Optimization Toolbox
%   is a registered trademark of The MathWorks, Inc.
%
%   x = INTLINPROG(f,intcon,A,b) solves the problem:
%
%   minimize     f'*x
%   subject to   A*x <= b
%                x(j) integer, when j is in the vector
%                intcon of integer constraints
%
%   x = INTLINPROG(f,intcon,A,b,Aeq,beq) solves the problem:
%
%   minimize     f'*x
%   subject to     A*x <= b,
%                Aeq*x == beq
%                x(j) integer, where j is in the vector
%                intcon of integer constraints
%
%   x = INTLINPROG(f,intcon,A,b,Aeq,beq,lb,ub) solves the problem:
%
%   minimize     f'*x
%   subject to     A*x <= b,
%                Aeq*x == beq,
%          lb <=     x <= ub.
%                x(j) integer, where j is in the vector
%                intcon of integer constraints
%
%   You can set lb(j) = -inf, if x(j) has no lower bound,
%   and ub(j) = inf, if x(j) has no upper bound.
%
%   [x, fval] = INTLINPROG(f, intcon, A, b) returns the objective value
%   at the solution. That is, fval = f'*x.
%
%   [x, fval, exitflag] = INTLINPROG(f, intcon, A, b) returns an exitflag
%   containing the status of the optimization. The values for
%   exitflag and corresponding status codes are:
%    2 - Solver stopped prematurely. Integer feasible point found.
%    1 - Optimal solution found.
%    0 - Solver stopped prematurely. No integer feasible point found.
%   -2 - No feasible point found.
%   -3 - Problem is unbounded.

if nargin < 4
    error('intlinprog(f, intcon, A, b)')
end

if nargin > 8
    error('intlinprog(f, intcon, A, b, Aeq, beq, lb, ub)');
end

if ~isempty(A)
    n = size(A, 2);
elseif nargin > 5 && ~isempty(Aeq)
    n = size(Aeq, 2);
else
    error('No linear constraints specified')
end

if ~issparse(A)
    A = sparse(A);
end

if nargin > 4 && ~issparse(Aeq)
    Aeq = sparse(Aeq);
end

model.obj = f;
model.vtype = repmat('C', n, 1);
model.vtype(intcon) = 'I';

if nargin < 5
    model.A = A;
    model.rhs = b;
    model.sense = '<';
else
    model.A = [A; Aeq];
    model.rhs = [b; beq];
    model.sense = [repmat('<', size(A,1), 1); repmat('=', size(Aeq,1), 1)];
end

if nargin < 7
    model.lb = -inf(n,1);
else
    model.lb = lb;
end

if nargin == 8
   model.ub = ub;
end

params.outputflag = 1;
result = gurobi(model, params);


if strcmp(result.status, 'OPTIMAL')
    exitflag = 1;
elseif strcmp(result.status, 'INTERRUPTED')
    if isfield(result, 'x')
        exitflag = 2;
    else
        exitflag = 0;
    end
elseif strcmp(result.status, 'INF_OR_UNBD')
    params.dualreductions = 0;
    result = gurobi(model, params);
    if strcmp(result.status, 'INFEASIBLE')
        exitflag = -2;
    elseif strcmp(result.status, 'UNBOUNDED')
        exitflag = -3;
    else
        exitflag = nan;
    end
else
    exitflag = nan;
end


if isfield(result, 'x')
    x = result.x;
else
    x = nan(n,1);
end

if isfield(result, 'objval')
    fval = result.objval;
else
    fval = nan;
end


