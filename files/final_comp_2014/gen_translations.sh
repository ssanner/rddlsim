#!/bin/sh

#############################################################################
# Generate RDDL Prefix, (PO-)PPDDL, SPUDD, and Symbolic Perseus translations
#
# Author: Scott Sanner (ssanner [at] gmail.com)
#############################################################################

# Executables must run from rddsim/
cd ../..
echo `pwd`

rm -rf files/final_comp_2014/rddl_domains/*
rm -rf files/final_comp_2014/rddl_prefix/*
rm -rf files/final_comp_2014/spudd_sperseus/*
rm -rf files/final_comp_2014/ppddl/*

cp -r files/final_comp_2014/rddl/* files/final_comp_2014/rddl_domains/
rm -rf files/final_comp_2014/rddl_domains/*inst*

./run rddl.translate.RDDL2Prefix files/final_comp_2014/rddl files/final_comp_2014/rddl_prefix
./run rddl.translate.RDDL2Format files/final_comp_2014/rddl files/final_comp_2014/spudd_sperseus spudd_sperseus
./run rddl.translate.RDDL2Format files/final_comp_2014/rddl files/final_comp_2014/ppddl ppddl

