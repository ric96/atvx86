CC=gcc
CFLAGS=-c -Wall -O2
SOURCES=tablet-mode.c
OBJECTS=$(SOURCES:.c=.o)
EXECUTABLE=tablet-mode

all: $(SOURCES) $(EXECUTABLE)

$(EXECUTABLE): $(OBJECTS)
	$(CC) $(LDFLAGS) $(OBJECTS) -o $@

.c.o:
	$(CC) $(CFLAGS) $< -o $@

clean:
	rm $(OBJECTS) $(EXECUTABLE)
