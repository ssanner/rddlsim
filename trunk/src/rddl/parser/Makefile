#================================================================
# A Makefile for the RDDL parser.
#
# This directory should be src/rddl/parser and should contain
# - rddl.lex
# - rddl.cup
#
# The parent directory src/rddl should contain
# - RDDL.java
#
# Compiled class files are expected in bin/, but this can be
# changed below to src/ (search-replace) if class files are 
# desired here. 
#
# Author: Scott Sanner (ssanner [at] gmail.com)
# $Revision: 1.1 $
#================================================================

#Cygwin(;) or Linux/UNIX(:) separator?
SEP	= ;
#SEP 	= :

# environment settings:
JAVAC   = javac -classpath "../../../bin$(SEP)../../../lib/jlex.jar$(SEP)../../../lib/java_cup.jar"
JAVADOC = javadoc
JAVA    = java -classpath "../../../bin$(SEP)../../../lib/jlex.jar$(SEP)../../../lib/java_cup.jar"

# project specifics:
CLASSES      = \
	Yylex.class sym.class parser.class ParseException.class

# list of subpackages:
PACKAGES     = parser

# Explain to make how compiling Java code works...
.SUFFIXES :     .class .java
.java.class :
	$(JAVAC) $<

# some other useful macros we can derive...
SRCS = $(CLASSES:.class=.java)
DOCS = $(CLASSES:.class=.html)

# Typical make targets:
all: $(PACKAGES) $(CLASSES) 

docs: ALWAYS_BUILD
	-mkdir docs; \
	$(JAVADOC) -author -d docs $(SRCS) $(PACKAGES)

lex: ALWAYS_BUILD
	$(JAVA) jlex.Main rddl.lex; \
	mv rddl.lex.java Yylex.java; \
	rm ../../../bin/rddl/parser/*.class;

parser: ALWAYS_BUILD
	$(JAVA) java_cup.Main -expect 0 rddl.cup;

clean:
	-rm ../../../bin/rddl/parser/*.class *~

os:
	-echo OSTYPE $$OSTYPE; \
	echo SEP "$(SEP)";

wc: ALWAYS_BUILD
	wc `find . -name "*.java"`

# Packages:


# This is a dummy target that can be used to ensure that a target's
# dependencies are never satisfied.  This is useful because by default, make
# treats the presence of a directory with the same name as the target as an
# up-to-date target.
ALWAYS_BUILD:
