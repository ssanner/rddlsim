#---------------------------------------------------------------------------
CC	= g++

FLAGS	= -DUNIX
MFLAG   =
ICFLAGS = -g
CFLAGS  = $(ICFLAGS) $(MFLAG) $(XCFLAGS)

SRC	= rddlclient.cc strxml.cc
OBJ	= rddlclient.o strxml.o

rddlclient.o: rddlclient.cc
	$(CC) $(CFLAGS) -I. -c rddlclient.cc

strxml.o: strxml.cc
	$(CC) $(CFLAGS) -I. -c strxml.cc

all: rddlclient.o strxml.o
	$(CC) -o rddlclient $(OBJ)
