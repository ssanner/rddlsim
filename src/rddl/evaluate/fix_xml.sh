#!/bin/sh
#
# Shell script for converting xml log files to proper XML.
# Note: the Java evaluation code already does this automatically.
#
echo Cleaning xml files in $1

# Remove repeated header lines
/usr/bin/perl -pi.bak -e 's/\Q<?xml version="1.0" encoding="UTF-8"?>\E//g' $1/*.log

# Add back in single header and make sure there is a root node
for file in $1/*.log
do
  echo Processing $file
  cp $file "${file}.tmp"
  echo '<?xml version="1.0" encoding="UTF-8"?><root>' | cat - "${file}.tmp" > $file
  echo '</root>' | cat >> $file
  rm "${file}.tmp"
done
