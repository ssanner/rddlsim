#!/bin/sh

# Arg 1: File instance list
# Arg 2: RDDL file directory
# Arg 3: Client name
# Arg 4: Client policy name
# Arg 5: Server port

# Read in instance list as stdin
exec<$1

# Executables must run from rddsim/
cd ../..

while read instance
do
  ./run rddl.competition.Client $2 localhost $3 $4 $5 123456 $instance
done
