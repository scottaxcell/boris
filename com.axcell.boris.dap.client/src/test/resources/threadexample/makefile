CC = g++
CFLAGS = -g -pthread
RM = rm -f

default: all

all: threadexample

threadexample: threadexample.cpp
	$(CC) $(CFLAGS) -o threadexample threadexample.cpp

clean:
	$(RM) threadexample
