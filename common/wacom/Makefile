CC=gcc
CFLAGS=-c -Wall -O2
#LDFLAGS=-lncurses
SOURCES=wactablet.c wacserial.c wacusb.c wacom-input.c
OBJECTS=$(SOURCES:.c=.o)
EXECUTABLE=wacom-input

all: $(SOURCES) $(EXECUTABLE)

$(EXECUTABLE): $(OBJECTS)
	$(CC) $(LDFLAGS) $(OBJECTS) -o $@

.c.o:
	$(CC) $(CFLAGS) $< -o $@

clean:
	rm $(OBJECTS) $(EXECUTABLE)
