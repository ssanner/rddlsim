%GUROBI_READ  Read a model from a file
%   MODEL = GUROBI_READ(FILENAME) reads the optimization model
%   stored in FILENAME into the MODEL struct.
%
%   Note that the type of the file is encoded in the file name
%   suffix. Valid suffixes are .mps, .rew, .lp, .rlp, or .ilp.
%   The files can be compressed, so additional suffixes of .gz,
%   .bz2, .zip, or .7z are accepted. The file name may contain
%   * or ? wildcards. No file is read when no wildcard match is
%   found. If more than one match is found, this routine will
%   attempt to read the first matching file.
%
%   The GUROBI_READ function returns a struct MODEL containing
%   multiple named fields. See the documentation of the GUROBI
%   function for a description of these fields and their contents.
%
% Examples:
%
%     model = gurobi_read('etamacro.mps');
%     result = gurobi(model);
%
%     model = gurobi_read('afiro.lp');
%     result = gurobi(model);
%
% Copyright 2013, Gurobi Optimization, Inc.
%
% See also GUROBI, GUROBI_WRITE, STRUCT
