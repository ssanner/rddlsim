%GUROBI_WRITE  Write a model to a file
%   GUROBI_WRITE(MODEL, FILENAME) writes the optimization model
%   stored in the MODEL struct to FILENAME.
%
%   The MODEL struct must contain a valid Gurobi model. See the
%   documentation of the GUROBI function for a description of
%   this struct's required fields and values.
%
%   Note that the type of the file is encoded in the file name
%   suffix. Valid suffixes are .mps, .rew, .lp, .rlp. The files can be
%   compressed, so additional suffixes of .gz, .bz2, .zip, or .7z are
%   accepted.
%
% Examples:
%
%   model.A          = sparse([1 2 3; 1 1 0]);
%   model.obj        = [1 1 2];
%   model.modelsense = 'max';
%   model.rhs        = [4; 1];
%   model.sense      = '<>';
%
%   gurobi_write(model, 'mymodel.mps');
%   gurobi_write(model, 'mymodel.lp');
%   gurobi_write(model, 'mymodel.mps.bz2');
%
% Copyright 2013, Gurobi Optimization, Inc.
%
% See also GUROBI, GUROBI_READ, STRUCT
