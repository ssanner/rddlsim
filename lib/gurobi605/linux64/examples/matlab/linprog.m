function [x, fval, exitflag] = linprog(f, A, b, Aeq, beq, lb, ub)
%LINPROG A linear programming example using the Gurobi MATLAB interface
%
%   This example is based on the linprog interface defined in the
%   MATLAB Optimization Toolbox. The Optimization Toolbox
%   is a registered trademark of The MathWorks, Inc.
%
%   x = LINPROG(f,A,b) solves the linear programming problem:
%
%   minimize     f'*x
%   subject to   A*x <= b
%
%
%   x = LINPROG(f,A,b,Aeq,beq) solves the problem:
%
%   minimize     f'*x
%   subject to     A*x <= b,
%                Aeq*x == beq.
%
%   x = LINPROG(f,A,b,Aeq,beq,lb,ub) solves the problem:
%
%   minimize     f'*x
%   subject to     A*x <= b,
%                Aeq*x == beq,
%          lb <=     x <= ub.
%
%   You can set lb(j) = -inf, if x(j) has no lower bound,
%   and ub(j) = inf, if x(j) has no upper bound.
%
%   [x, fval] = LINPROG(f, A, b) returns the objective value
%   at the solution. That is, fval = f'*x.
%
%   [x, fval, exitflag] = LINPROG(f, A, b) returns an exitflag
%   containing the status of the optimization. The values for
%   exitflag and corresponding status codes are:
%      1 - OPTIMAL,
%      0 - ITERATION_LIMIT,
%     -2 - INFEASIBLE,
%     -3 - UNBOUNDED.
%

if nargin < 3
    error('linprog(f, A, b)')
end

if nargin > 7
    error('linprog(f, A, b, Aeq, beq, lb, ub)');
end

if ~isempty(A)
    n = size(A, 2);
elseif nargin > 4 && ~isempty(Aeq)
    n = size(Aeq, 2);
else
    error('No linear constraints specified')
end

if ~issparse(A)
    A = sparse(A);
end

if nargin > 3 && ~issparse(Aeq)
    Aeq = sparse(Aeq);
end


model.obj = f;

if nargin < 4
    model.A = A;
    model.rhs = b;
    model.sense = '<';
else
    model.A = [A; Aeq];
    model.rhs = [b; beq];
    model.sense = [repmat('<', size(A,1), 1); repmat('=', size(Aeq,1), 1)];
end

if nargin < 6
    model.lb = -inf(n,1);
else
    model.lb = lb;
end

if nargin == 7
   model.ub = ub;
end

params.outputflag = 0;
result = gurobi(model, params);


if strcmp(result.status, 'OPTIMAL')
    exitflag = 1;
elseif strcmp(result.status, 'ITERATION_LIMIT')
    exitflag = 0;
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
elseif strcmp(result.status, 'INFEASIBLE')
    exitflag = -2;
elseif strcmp(result.status, 'UNBOUNDED')
    exitflag = -3;
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
