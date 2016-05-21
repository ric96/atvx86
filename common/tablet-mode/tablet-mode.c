/**
 * Convert SW_TABLET_MODE events to SW_LID events for Android
 *
 * Copyright 2012 The Android-x86 Open Source Project
 *
 * Author: Stefan Seidel <android@stefanseidel.info>
 *
 * Licensed under GPLv2 or later
 *
 **/

#define LOG_TAG "tablet-mode"

#include <sys/stat.h>
#include <poll.h>
#include <fcntl.h>
#include <errno.h>
#include <dirent.h>
#include <string.h>
#include <cutils/log.h>
#include <linux/input.h>
#include <linux/uinput.h>

/* we must use this kernel-compatible implementation */
#define BITS_PER_LONG (sizeof(long) * 8)
#define NBITS(x) ((((x)-1)/BITS_PER_LONG)+1)
#define OFF(x)  ((x)%BITS_PER_LONG)
#define BIT(x)  (1UL<<OFF(x))
#define LONG(x) ((x)/BITS_PER_LONG)
#define test_bit(bit, array)    ((array[LONG(bit)] >> OFF(bit)) & 1)

int openfd(void)
{
	int fd;
	const char *dirname = "/dev/input";
	DIR *dir;
	if ((dir = opendir(dirname))) {
		struct dirent *de;
		unsigned long caps[NBITS(SW_TABLET_MODE+1)];
		while ((de = readdir(dir))) {
			if (de->d_name[0] != 'e') // eventX
				continue;
			char name[PATH_MAX];
			snprintf(name, PATH_MAX, "%s/%s", dirname, de->d_name);
			fd = open(name, O_RDONLY);
			if (fd < 0) {
				ALOGE("could not open %s, %s", name, strerror(errno));
				continue;
			}
			memset(caps, 0, sizeof(caps));
			if (ioctl(fd, EVIOCGBIT(EV_SW, sizeof(caps)), caps) < 1) {
				ALOGE("could not get device caps for %s, %s\n", name, strerror(errno));
				continue;
			}
			if (test_bit(SW_TABLET_MODE, caps)) {
				ALOGI("open %s(%s) ok", de->d_name, name);
				return fd;
			}
			close(fd);
		}
		closedir(dir);
	}
	return -1;
}

void send_switch(int ufd, int state) {
	struct input_event nev;
	ALOGI("Tablet Mode Switch to %d\n", state);
	memset(&nev, 0, sizeof(struct input_event));
	nev.type = EV_SW;
	nev.code = SW_LID;
	nev.value = !!state;
	write(ufd, &nev, sizeof(struct input_event));
	nev.type = EV_SYN;
	nev.code = SYN_REPORT;
	nev.value = 0;
	write(ufd, &nev, sizeof(struct input_event));
}

int main(void)
{
	int ifd = openfd();
	if (ifd < 0) {
		ALOGE("could not find any tablet mode switch, exiting.");
		return -1;
	}

	sleep(10); //wait some time or otherwise EventHub might not pick up our events correctly!?

	int ufd = open("/dev/uinput", O_WRONLY | O_NDELAY);
	if (ufd >= 0) {
		struct uinput_user_dev ud;
		memset(&ud, 0, sizeof(struct uinput_user_dev));
		strcpy(ud.name, "Android Tablet Lid Switch");
		write(ufd, &ud, sizeof(struct uinput_user_dev));
		ioctl(ufd, UI_SET_EVBIT, EV_SW);
		ioctl(ufd, UI_SET_SWBIT, SW_LID);
		ioctl(ufd, UI_DEV_CREATE, 0);
	} else {
		ALOGE("could not open uinput device: %s", strerror(errno));
		return -1;
	}

	// send initial switch state
	unsigned long sw_state[NBITS(SW_TABLET_MODE+1)];
	memset(sw_state, 0, sizeof(sw_state));
	if (ioctl(ifd, EVIOCGSW(sizeof(sw_state)), sw_state) >= 0) {
		send_switch(ufd, test_bit(SW_TABLET_MODE, sw_state) ? 1 : 0);
	}

	// read events and pass them on modified
	while (1) {
		struct input_event iev;
		size_t res = read(ifd, &iev, sizeof(struct input_event));
		if (res < sizeof(struct input_event)) {
			ALOGW("insufficient input data(%d)? fd=%d", res, ifd);
			continue;
		}
		//LOGV("type=%d scancode=%d value=%d from fd=%d", iev.type, iev.code, iev.value, ifd);
		if (iev.type == EV_SW && iev.code == SW_TABLET_MODE) {
			send_switch(ufd, iev.value);
		}
	}

	return 0;
}
