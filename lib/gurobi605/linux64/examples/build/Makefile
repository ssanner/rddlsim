# Copyright 2014, Gurobi Optimization, Inc.

PLATFORM = linux64
INC      = ../../include/
CC       = gcc
CPP      = g++
CARGS    = -m64 -g
CLIB     = -L../../lib/ -lgurobi60
CPPLIB   = -L../../lib/ -lgurobi_c++ -lgurobi60
JSRC     = ../java
CLASSDIR = -classpath ../../lib/gurobi.jar:.
JFLAG    = -d . $(CLASSDIR)

all: callback_c dense_c diet_c facility_c feasopt_c fixanddive_c \
     lp_c lpmethod_c lpmod_c mip1_c mip2_c params_c piecewise_c qcp_c\
     qp_c sensitivity_c sos_c sudoku_c tsp_c tune_c \
     workforce1_c workforce2_c workforce3_c workforce4_c \
     callback_c++ dense_c++ diet_c++ facility_c++ feasopt_c++ fixanddive_c++ \
     lp_c++ lpmethod_c++ lpmod_c++ mip1_c++ mip2_c++ params_c++ piecewise_c++ \
     qcp_c++ qp_c++ sensitivity_c++ sos_c++ sudoku_c++ tsp_c++ tune_c++ \
     workforce1_c++ workforce2_c++ workforce3_c++ workforce4_c++ \
     Callback.class Dense.class Diet.class Facility.class Feasopt.class \
     Fixanddive.class Lp.class Lpmethod.class Lpmod.class Mip1.class \
     Mip2.class Params.class Piecewise.class Qcp.class Qp.class \
     Sensitivity.class Sos.class Sudoku.class Tsp.class Tune.class \
     Workforce1.class Workforce2.class Workforce3.class Workforce4.class

run: run_c run_c++ run_java run_python

run_c: run_callback_c run_dense_c run_diet_c run_facility_c run_feasopt_c \
       run_fixanddive_c run_lp_c run_lpmethod_c run_lpmod_c run_mip1_c \
       run_mip2_c run_params_c run_piecewise_c run_qcp_c run_qp_c \
       run_sensitivity_c run_sos_c run_sudoku_c run_tsp_c run_tune_c \
       run_workforce1_c run_workforce2_c run_workforce3_c run_workforce4_c

run_c++: run_callback_c++ run_dense_c++ run_diet_c++ run_facility_c++ \
         run_feasopt_c++ run_fixanddive_c++ run_lp_c++ run_lpmethod_c++ \
         run_lpmod_c++ run_mip1_c++ run_mip2_c++ run_params_c++ \
         run_piecewise_c++ run_qcp_c++ run_qp_c++ run_sensitivity_c++ \
         run_sos_c++ run_sudoku_c++ run_tsp_c++ run_tune_c++ \
         run_workforce1_c++ run_workforce2_c++ run_workforce3_c++ \
         run_workforce4_c++

run_java: Callback Dense Diet Facility Feasopt Fixanddive Lp Lpmethod Lpmod \
          Mip1 Mip2 Params Piecewise Qcp Qp Sensitivity Sos Sudoku Tsp Tune \
          Workforce1 Workforce2 Workforce3 Workforce4

run_python: run_callback_python run_dense_python run_diet_python \
        run_diet2_python run_diet3_python run_diet4_python \
        run_facility_python run_feasopt_python run_fixanddive_python \
        run_lp_python run_lpmethod_python run_lpmod_python run_mip1_python \
        run_mip2_python run_netflow_python run_params_python \
        run_piecewise_python run_qcp_python run_qp_python \
        run_sensitivity_python run_sos_python run_sudoku_python \
        run_tsp_python run_tune_python run_workforce1_python \
        run_workforce2_python run_workforce3_python run_workforce4_python


callback_c: ../c/callback_c.c
	$(CC) $(CARGS) -o callback_c ../c/callback_c.c -I$(INC) $(CLIB) -lpthread -lm
dense_c: ../c/dense_c.c
	$(CC) $(CARGS) -o dense_c ../c/dense_c.c -I$(INC) $(CLIB) -lpthread -lm
diet_c: ../c/diet_c.c
	$(CC) $(CARGS) -o diet_c ../c/diet_c.c -I$(INC) $(CLIB) -lpthread -lm
facility_c: ../c/facility_c.c
	$(CC) $(CARGS) -o facility_c ../c/facility_c.c -I$(INC) $(CLIB) -lpthread -lm
feasopt_c: ../c/feasopt_c.c
	$(CC) $(CARGS) -o feasopt_c ../c/feasopt_c.c -I$(INC) $(CLIB) -lpthread -lm
fixanddive_c: ../c/fixanddive_c.c
	$(CC) $(CARGS) -o fixanddive_c ../c/fixanddive_c.c -I$(INC) $(CLIB) -lpthread -lm
lp_c: ../c/lp_c.c
	$(CC) $(CARGS) -o lp_c ../c/lp_c.c -I$(INC) $(CLIB) -lpthread -lm
lpmethod_c: ../c/lpmethod_c.c
	$(CC) $(CARGS) -o lpmethod_c ../c/lpmethod_c.c -I$(INC) $(CLIB) -lpthread -lm
lpmod_c: ../c/lpmod_c.c
	$(CC) $(CARGS) -o lpmod_c ../c/lpmod_c.c -I$(INC) $(CLIB) -lpthread -lm
mip1_c: ../c/mip1_c.c
	$(CC) $(CARGS) -o mip1_c ../c/mip1_c.c -I$(INC) $(CLIB) -lpthread -lm
mip2_c: ../c/mip2_c.c
	$(CC) $(CARGS) -o mip2_c ../c/mip2_c.c -I$(INC) $(CLIB) -lpthread -lm
params_c: ../c/params_c.c
	$(CC) $(CARGS) -o params_c ../c/params_c.c -I$(INC) $(CLIB) -lpthread -lm
piecewise_c: ../c/piecewise_c.c
	$(CC) $(CARGS) -o piecewise_c ../c/piecewise_c.c -I$(INC) $(CLIB) -lpthread -lm
qcp_c: ../c/qcp_c.c
	$(CC) $(CARGS) -o qcp_c ../c/qcp_c.c -I$(INC) $(CLIB) -lpthread -lm
qp_c: ../c/qp_c.c
	$(CC) $(CARGS) -o qp_c ../c/qp_c.c -I$(INC) $(CLIB) -lpthread -lm
sensitivity_c: ../c/sensitivity_c.c
	$(CC) $(CARGS) -o sensitivity_c ../c/sensitivity_c.c -I$(INC) $(CLIB) -lpthread -lm
sos_c: ../c/sos_c.c
	$(CC) $(CARGS) -o sos_c ../c/sos_c.c -I$(INC) $(CLIB) -lpthread -lm
sudoku_c: ../c/sudoku_c.c
	$(CC) $(CARGS) -o sudoku_c ../c/sudoku_c.c -I$(INC) $(CLIB) -lpthread -lm
tsp_c: ../c/tsp_c.c
	$(CC) $(CARGS) -o tsp_c ../c/tsp_c.c -I$(INC) $(CLIB) -lpthread -lm
tune_c: ../c/tune_c.c
	$(CC) $(CARGS) -o tune_c ../c/tune_c.c -I$(INC) $(CLIB) -lpthread -lm
workforce1_c: ../c/workforce1_c.c
	$(CC) $(CARGS) -o workforce1_c ../c/workforce1_c.c -I$(INC) $(CLIB) -lpthread -lm
workforce2_c: ../c/workforce2_c.c
	$(CC) $(CARGS) -o workforce2_c ../c/workforce2_c.c -I$(INC) $(CLIB) -lpthread -lm
workforce3_c: ../c/workforce3_c.c
	$(CC) $(CARGS) -o workforce3_c ../c/workforce3_c.c -I$(INC) $(CLIB) -lpthread -lm
workforce4_c: ../c/workforce4_c.c
	$(CC) $(CARGS) -o workforce4_c ../c/workforce4_c.c -I$(INC) $(CLIB) -lpthread -lm

callback_c++: ../c++/callback_c++.cpp
	$(CPP) $(CARGS) -o callback_c++ ../c++/callback_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
dense_c++: ../c++/dense_c++.cpp
	$(CPP) $(CARGS) -o dense_c++ ../c++/dense_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
diet_c++: ../c++/diet_c++.cpp
	$(CPP) $(CARGS) -o diet_c++ ../c++/diet_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
facility_c++: ../c++/facility_c++.cpp
	$(CPP) $(CARGS) -o facility_c++ ../c++/facility_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
feasopt_c++: ../c++/feasopt_c++.cpp
	$(CPP) $(CARGS) -o feasopt_c++ ../c++/feasopt_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
fixanddive_c++: ../c++/fixanddive_c++.cpp
	$(CPP) $(CARGS) -o fixanddive_c++ ../c++/fixanddive_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
lp_c++: ../c++/lp_c++.cpp
	$(CPP) $(CARGS) -o lp_c++ ../c++/lp_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
lpmethod_c++: ../c++/lpmethod_c++.cpp
	$(CPP) $(CARGS) -o lpmethod_c++ ../c++/lpmethod_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
lpmod_c++: ../c++/lpmod_c++.cpp
	$(CPP) $(CARGS) -o lpmod_c++ ../c++/lpmod_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
mip1_c++: ../c++/mip1_c++.cpp
	$(CPP) $(CARGS) -o mip1_c++ ../c++/mip1_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
mip2_c++: ../c++/mip2_c++.cpp
	$(CPP) $(CARGS) -o mip2_c++ ../c++/mip2_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
params_c++: ../c++/params_c++.cpp
	$(CPP) $(CARGS) -o params_c++ ../c++/params_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
piecewise_c++: ../c++/piecewise_c++.cpp
	$(CPP) $(CARGS) -o piecewise_c++ ../c++/piecewise_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
qcp_c++: ../c++/qcp_c++.cpp
	$(CPP) $(CARGS) -o qcp_c++ ../c++/qcp_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
qp_c++: ../c++/qp_c++.cpp
	$(CPP) $(CARGS) -o qp_c++ ../c++/qp_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
sensitivity_c++: ../c++/sensitivity_c++.cpp
	$(CPP) $(CARGS) -o sensitivity_c++ ../c++/sensitivity_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
sos_c++: ../c++/sos_c++.cpp
	$(CPP) $(CARGS) -o sos_c++ ../c++/sos_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
sudoku_c++: ../c++/sudoku_c++.cpp
	$(CPP) $(CARGS) -o sudoku_c++ ../c++/sudoku_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
tsp_c++: ../c++/tsp_c++.cpp
	$(CPP) $(CARGS) -o tsp_c++ ../c++/tsp_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
tune_c++: ../c++/tune_c++.cpp
	$(CPP) $(CARGS) -o tune_c++ ../c++/tune_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
workforce1_c++: ../c++/workforce1_c++.cpp
	$(CPP) $(CARGS) -o workforce1_c++ ../c++/workforce1_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
workforce2_c++: ../c++/workforce2_c++.cpp
	$(CPP) $(CARGS) -o workforce2_c++ ../c++/workforce2_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
workforce3_c++: ../c++/workforce3_c++.cpp
	$(CPP) $(CARGS) -o workforce3_c++ ../c++/workforce3_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm
workforce4_c++: ../c++/workforce4_c++.cpp
	$(CPP) $(CARGS) -o workforce4_c++ ../c++/workforce4_c++.cpp -I$(INC) $(CPPLIB) -lpthread -lm

Callback.class: $(JSRC)/Callback.java
	javac $(JFLAG) $(JSRC)/Callback.java
Dense.class: $(JSRC)/Dense.java
	javac $(JFLAG) $(JSRC)/Dense.java
Diet.class: $(JSRC)/Diet.java
	javac $(JFLAG) $(JSRC)/Diet.java
Facility.class: $(JSRC)/Facility.java
	javac $(JFLAG) $(JSRC)/Facility.java
Feasopt.class: $(JSRC)/Feasopt.java
	javac $(JFLAG) $(JSRC)/Feasopt.java
Fixanddive.class: $(JSRC)/Fixanddive.java
	javac $(JFLAG) $(JSRC)/Fixanddive.java
Lp.class: $(JSRC)/Lp.java
	javac $(JFLAG) $(JSRC)/Lp.java
Lpmethod.class: $(JSRC)/Lpmethod.java
	javac $(JFLAG) $(JSRC)/Lpmethod.java
Lpmod.class: $(JSRC)/Lpmod.java
	javac $(JFLAG) $(JSRC)/Lpmod.java
Mip1.class: $(JSRC)/Mip1.java
	javac $(JFLAG) $(JSRC)/Mip1.java
Mip2.class: $(JSRC)/Mip2.java
	javac $(JFLAG) $(JSRC)/Mip2.java
Params.class: $(JSRC)/Params.java
	javac $(JFLAG) $(JSRC)/Params.java
Piecewise.class: $(JSRC)/Piecewise.java
	javac $(JFLAG) $(JSRC)/Piecewise.java
Qcp.class: $(JSRC)/Qcp.java
	javac $(JFLAG) $(JSRC)/Qcp.java
Qp.class: $(JSRC)/Qp.java
	javac $(JFLAG) $(JSRC)/Qp.java
Sensitivity.class: $(JSRC)/Sensitivity.java
	javac $(JFLAG) $(JSRC)/Sensitivity.java
Sos.class: $(JSRC)/Sos.java
	javac $(JFLAG) $(JSRC)/Sos.java
Sudoku.class: $(JSRC)/Sudoku.java
	javac $(JFLAG) $(JSRC)/Sudoku.java
Tsp.class: $(JSRC)/Tsp.java
	javac $(JFLAG) $(JSRC)/Tsp.java
Tune.class: $(JSRC)/Tune.java
	javac $(JFLAG) $(JSRC)/Tune.java
Workforce1.class: $(JSRC)/Workforce1.java
	javac $(JFLAG) $(JSRC)/Workforce1.java
Workforce2.class: $(JSRC)/Workforce2.java
	javac $(JFLAG) $(JSRC)/Workforce2.java
Workforce3.class: $(JSRC)/Workforce3.java
	javac $(JFLAG) $(JSRC)/Workforce3.java
Workforce4.class: $(JSRC)/Workforce4.java
	javac $(JFLAG) $(JSRC)/Workforce4.java

run_callback_c: callback_c
	./callback_c ../data/p0033.mps
run_dense_c: dense_c
	./dense_c
run_diet_c: diet_c
	./diet_c
run_facility_c: facility_c
	./facility_c
run_feasopt_c: feasopt_c
	./feasopt_c ../data/klein1.mps
run_fixanddive_c: fixanddive_c
	./fixanddive_c ../data/stein9.mps
run_lp_c: lp_c
	./lp_c ../data/klein1.mps
run_lpmethod_c: lpmethod_c
	./lpmethod_c ../data/afiro.mps
run_lpmod_c: lpmod_c
	./lpmod_c ../data/afiro.mps
run_mip1_c: mip1_c
	./mip1_c
run_mip2_c: mip2_c
	./mip2_c ../data/stein9.mps
run_params_c: params_c
	./params_c ../data/misc07.mps
run_piecewise_c: piecewise_c
	./piecewise_c
run_qcp_c: qcp_c
	./qcp_c
run_qp_c: qp_c
	./qp_c
run_sensitivity_c: sensitivity_c
	./sensitivity_c ../data/p0033.lp
run_sos_c: sos_c
	./sos_c
run_sudoku_c: sudoku_c
	./sudoku_c ../data/sudoku1
run_tsp_c: tsp_c
	./tsp_c 50
run_tune_c: tune_c
	./tune_c ../data/p0033.mps
run_workforce1_c: workforce1_c
	./workforce1_c
run_workforce2_c: workforce2_c
	./workforce2_c
run_workforce3_c: workforce3_c
	./workforce3_c
run_workforce4_c: workforce4_c
	./workforce4_c

run_callback_c++: callback_c++
	./callback_c++ ../data/p0033.mps
run_dense_c++: dense_c++
	./dense_c++
run_diet_c++: diet_c++
	./diet_c++
run_facility_c++: facility_c++
	./facility_c++
run_feasopt_c++: feasopt_c++
	./feasopt_c++ ../data/klein1.mps
run_fixanddive_c++: fixanddive_c++
	./fixanddive_c++ ../data/stein9.mps
run_lp_c++: lp_c++
	./lp_c++ ../data/klein1.mps
run_lpmethod_c++: lpmethod_c++
	./lpmethod_c++ ../data/afiro.mps
run_lpmod_c++: lpmod_c++
	./lpmod_c++ ../data/afiro.mps
run_mip1_c++: mip1_c++
	./mip1_c++
run_mip2_c++: mip2_c++
	./mip2_c++ ../data/stein9.mps
run_params_c++: params_c++
	./params_c++ ../data/misc07.mps
run_piecewise_c++: piecewise_c++
	./piecewise_c++
run_qcp_c++: qcp_c++
	./qcp_c++
run_qp_c++: qp_c++
	./qp_c++
run_sensitivity_c++: sensitivity_c++
	./sensitivity_c++ ../data/p0033.lp
run_sos_c++: sos_c++
	./sos_c++
run_sudoku_c++: sudoku_c++
	./sudoku_c++ < ../data/sudoku1
run_tsp_c++: tsp_c++
	./tsp_c++ 50
run_tune_c++: tune_c++
	./tune_c++ ../data/p0033.mps
run_workforce1_c++: workforce1_c++
	./workforce1_c++
run_workforce2_c++: workforce2_c++
	./workforce2_c++
run_workforce3_c++: workforce3_c++
	./workforce3_c++
run_workforce4_c++: workforce4_c++
	./workforce4_c++

Callback: Callback.class
	java $(CLASSDIR): Callback ../data/p0033.mps
Dense: Dense.class
	java $(CLASSDIR): Dense
Diet: Diet.class
	java $(CLASSDIR): Diet
Facility: Facility.class
	java $(CLASSDIR): Facility
Feasopt: Feasopt.class
	java $(CLASSDIR): Feasopt ../data/klein1.mps
Fixanddive: Fixanddive.class
	java $(CLASSDIR): Fixanddive ../data/stein9.mps
Lp: Lp.class
	java $(CLASSDIR): Lp ../data/klein1.mps
Lpmethod: Lpmethod.class
	java $(CLASSDIR): Lpmethod ../data/afiro.mps
Lpmod: Lpmod.class
	java $(CLASSDIR): Lpmod ../data/afiro.mps
Mip1: Mip1.class
	java $(CLASSDIR): Mip1
Mip2: Mip2.class
	java $(CLASSDIR): Mip2 ../data/stein9.mps
Params: Params.class
	java $(CLASSDIR): Params ../data/misc07.mps
Piecewise: Piecewise.class
	java $(CLASSDIR): Piecewise
Qcp: Qcp.class
	java $(CLASSDIR): Qcp
Qp: Qp.class
	java $(CLASSDIR): Qp
Sensitivity: Sensitivity.class
	java $(CLASSDIR): Sensitivity ../data/p0033.lp
Sos: Sos.class
	java $(CLASSDIR): Sos
Sudoku: Sudoku.class
	java $(CLASSDIR): Sudoku ../data/sudoku1
Tsp: Tsp.class
	java $(CLASSDIR): Tsp 50
Tune: Tune.class
	java $(CLASSDIR): Tune ../data/p0033.mps
Workforce1: Workforce1.class
	java $(CLASSDIR): Workforce1
Workforce2: Workforce2.class
	java $(CLASSDIR): Workforce2
Workforce3: Workforce3.class
	java $(CLASSDIR): Workforce3
Workforce4: Workforce4.class
	java $(CLASSDIR): Workforce4

run_callback_python:
	gurobi.sh ../python/callback.py ../data/p0033.mps
run_dense_python:
	gurobi.sh ../python/dense.py
run_diet_python:
	gurobi.sh ../python/diet.py
run_diet2_python:
	gurobi.sh ../python/diet2.py
run_diet3_python:
	gurobi.sh ../python/diet3.py
run_diet4_python:
	gurobi.sh ../python/diet4.py
run_facility_python:
	gurobi.sh ../python/facility.py
run_feasopt_python:
	gurobi.sh ../python/feasopt.py ../data/klein1.mps
run_fixanddive_python:
	gurobi.sh ../python/fixanddive.py ../data/stein9.mps
run_lp_python:
	gurobi.sh ../python/lp.py ../data/klein1.mps
run_lpmethod_python:
	gurobi.sh ../python/lpmethod.py ../data/afiro.mps
run_lpmod_python:
	gurobi.sh ../python/lpmod.py ../data/afiro.mps
run_mip1_python:
	gurobi.sh ../python/mip1.py
run_mip2_python:
	gurobi.sh ../python/mip2.py ../data/stein9.mps
run_netflow_python:
	gurobi.sh ../python/netflow.py
run_params_python:
	gurobi.sh ../python/params.py ../data/misc07.mps
run_piecewise_python:
	gurobi.sh ../python/piecewise.py
run_qcp_python:
	gurobi.sh ../python/qcp.py
run_qp_python:
	gurobi.sh ../python/qp.py
run_sensitivity_python:
	gurobi.sh ../python/sensitivity.py ../data/p0033.lp
run_sos_python:
	gurobi.sh ../python/sos.py
run_sudoku_python:
	gurobi.sh ../python/sudoku.py ../data/sudoku1
run_tsp_python:
	gurobi.sh ../python/tsp.py 50
run_tune_python:
	gurobi.sh ../python/tune.py ../data/p0033.mps
run_workforce1_python:
	gurobi.sh ../python/workforce1.py
run_workforce2_python:
	gurobi.sh ../python/workforce2.py
run_workforce3_python:
	gurobi.sh ../python/workforce3.py
run_workforce4_python:
	gurobi.sh ../python/workforce4.py

clean:
	rm -rf *.o *_c *_c++ *.class *.log *.rlp *.lp *.bas *.ilp
