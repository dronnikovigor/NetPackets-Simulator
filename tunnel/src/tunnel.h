#ifndef TUNNEL_H__
#define TUNNEL_H__

#include <poll.h>
#include <string>

class Tunnel
{
private:
	/* buffer for reading from tun/tap interface, must be >= 1500 */
	static constexpr int buffer_size = 2000;
	char buffer[buffer_size];
	pollfd poll_fd;

	/**
	 * Tries to open a TUN interface by its name.
	 * String dev should be the name of the device with a format string (e.g. "tun%d")
	 * and should has size IFNAMSIZ or lower.
	 */
	int tun_alloc(std::string &dev);

	/**
	 * Make an attempt to read a packet from file descriptor of TUN interface.
	 */
	int cread(int fd, char *buf, int n);

	/**
	 * Make an attempt to write a packet to file descriptor of TUN interface.
	 */
	int cwrite(int fd, const char *buf, int n);

public:
	Tunnel();

	~Tunnel();

	bool init(std::string &interface_name);

	void stop();

	bool is_active();

	int readPacket(int timeout, char *&packet_ref);

	int writePacket(char *packet_data, int length);
};

#endif
