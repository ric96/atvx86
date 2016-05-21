/*
 * Copyright (C) 2011 The Android-x86 Open Source Project
 *
 * by Chih-Wei Huang <cwhuang@linux.org.tw>
 *
 * Licensed under GPLv2 or later
 *
 */

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#define	IO_SHIFT	0x68
#define	IO_VALUE	0x62

void usage(int invalid)
{
	if (invalid)
		fprintf(stderr, "Invalid parameter\n");
	fprintf(stderr, "Usage : io_switch 0x?? 0x??\n");
	exit(-1);
}

int main(int argc, char *argv[])
{
	int fd, addr;
	char val;

	if (argc < 3)
		usage(0);

	if (sscanf(argv[1], "0x%x", &addr) <= 0)
		usage(1);

	if (sscanf(argv[2], "0x%hhx", &val) <= 0)
		usage(1);

	printf("Writing 0x%x : 0x%hhx\n", addr, val);

	fd = open("/dev/port", O_WRONLY);
	if (fd < 0) {
		fprintf(stderr, "Open file failed\n");
	} else {
		val += IO_VALUE;
		lseek(fd, addr + IO_SHIFT, SEEK_SET);
		write(fd, &val, 1);
		close(fd);
	}

	return (fd < 0);
}
