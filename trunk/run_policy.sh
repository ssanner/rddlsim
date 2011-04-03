#!/bin/sh
# Arg 1: File instance list
# Arg 2: RDDL file directory
# Arg 3: Client name
# Arg 4: Client policy name
# Arg 5: Server port
exec<$1
while read instance
do
  ./run rddl.competition.Client $2 localhost $3 $4 $5 123456 $instance
done
