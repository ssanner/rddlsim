#---------------------------------------------------------------------------
CC	= g++

FLAGS	= -DUNIX
MFLAG   =
ICFLAGS = -g
CFLAGS  = $(ICFLAGS) $(MFLAG) $(XCFLAGS)

SRC	= cclient.cc getopt1.c  getopt.c rddlclient.cc  strxml.cc
OBJ	= cclient.o getopt1.o getopt.o rddlclient.o strxml.o

cclient.o: cclient.cc
	$(CC) $(CFLAGS) -I. -c cclient.cc 

getopt1.o: getopt1.c
	$(CC) $(CFLAGS) -I. -c getopt1.c

getopt.o: getopt.c
	$(CC) $(CFLAGS) -I. -c getopt.c

rddlclient.o: rddlclient.cc
	$(CC) $(CFLAGS) -I. -c rddlclient.cc

strxml.o: strxml.cc
	$(CC) $(CFLAGS) -I. -c strxml.cc

all: cclient.o getopt1.o getopt.o rddlclient.o strxml.o
	$(CC) -o client $(OBJ)
