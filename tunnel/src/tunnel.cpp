#include "tunnel.h"

#include <bits/stdint-uintn.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fcntl.h>
#include <linux/if.h>
#include <linux/if_tun.h>
#include <sys/ioctl.h>
#include <unistd.h>

#include "checksum.h"

typedef uint32_t addr_t;

/**
 * Tries to open a TUN interface by its name.
 * String dev should be the name of the device with a format string (e.g. "tun%d")
 * and should has size IFNAMSIZ or lower.
 */
int Tunnel::tun_alloc(std::string &dev)
{
	if (dev.size() > IFNAMSIZ)
	{
		return -1;
	}

	ifreq ifr;
	int fd, err;

	if ((fd = open("/dev/net/tun", O_RDWR)) < 0)
	{
		perror("Opening /dev/net/tun");
		return fd;
	}

	memset(&ifr, 0, sizeof(ifr));

	/* Flags:   IFF_TUN   - TUN device (no Ethernet headers)
	 *       IFF_TAP   - TAP device
	 *
	 *       IFF_NO_PI - Do not provide packet information
	 */
	ifr.ifr_flags = IFF_TUN;
	strncpy(ifr.ifr_name, dev.c_str(), IFNAMSIZ);

	if ((err = ioctl(fd, TUNSETIFF, (void *)&ifr)) < 0)
	{
		close(fd);
		return err;
	}

	dev = ifr.ifr_name;

	return fd;
}

/**
 * Make an attempt to read a packet from file descriptor of TUN interface.
 */
int Tunnel::cread(int fd, char *buf, int n)
{
	int nread;

	if ((nread = read(fd, buf, n)) < 0)
	{
		perror("Reading data");
		exit(1);
	}
	return nread;
}

/**
 * Make an attempt to write a packet to file descriptor of TUN interface.
 */
int Tunnel::cwrite(int fd, const char *buf, int n)
{
	int nwrite;

	if ((nwrite = write(fd, buf, n)) < 0)
	{
		perror("Writing data");
		exit(1);
	}
	return nwrite;
}

Tunnel::Tunnel()
{
	poll_fd.fd = -1;
	poll_fd.events = 0;
	poll_fd.revents = 0;
}

Tunnel::~Tunnel()
{
	//check if we need to close file descriptor
	if (poll_fd.fd >= 0)
	{
		close(poll_fd.fd);
	}
}

bool Tunnel::init(std::string &interface_name)
{
	int fd = tun_alloc(interface_name);
	//todo chec case when fd == 0
	if (fd >= 0)
	{
		//pollfd init
		poll_fd.fd = fd;
		poll_fd.events = POLLIN;
		poll_fd.revents = 0;
		return true;
	}
	return false;
}

void Tunnel::stop()
{
	if (poll_fd.fd > 0)
	{
		close(poll_fd.fd);
		poll_fd.fd = -1;
	}
}

bool Tunnel::is_active()
{
	return poll_fd.fd >= 0;
}

int Tunnel::readPacket(int timeout, char *&packet_ref)
{
	if (poll_fd.fd > 0)
	{
		poll(&poll_fd, 1, timeout);

		if (poll_fd.revents & POLLIN)
		{
			int readed = cread(poll_fd.fd, buffer, buffer_size);

			//clear returned flags
			poll_fd.revents = 0;

			packet_ref = buffer;
			return readed;
		}
	}

	return 0;
}

int Tunnel::writePacket(char *packet_data, int length)
{
	if (poll_fd.fd > 0)
	{
		checksum::calculate_checksums(packet_data);
		int nwrite = cwrite(poll_fd.fd, packet_data, length);

		return nwrite;
	}
	return 0;
}
