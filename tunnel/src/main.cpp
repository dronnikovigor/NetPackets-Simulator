#include <arpa/inet.h>
#include <netinet/in.h>
#include <cstdio>
#include <string>

#include "packet.h"
#include "tunnel.h"

int main()
{

	Tunnel tunnel;

	std::string tunInterface = "tun1";
	bool success = tunnel.init(tunInterface);
	if (success)
	{
		printf("Interface opened: %s\n", tunInterface.c_str());

		do
		{
			Packet *packet = tunnel.readPacket(1000);
			if (packet)
			{
				printf("    Received packet:\n");
				printf("        packet total length: %d\n",
						packet->get_packet_length());
				printf("        packet protocol: %s\n",
						protocol_to_string(packet->get_protocol()).c_str());
				printf("        src ip: %s\n",
						inet_ntoa(packet->get_ip_header()->ip_src));
				printf("        dst ip: %s\n",
						inet_ntoa(packet->get_ip_header()->ip_dst));

				const udp_header_t *udp_header = packet->get_udp_header();
				if (udp_header)
				{
					printf("    Udp header info:\n");
					printf("        src port: %d\n",
							ntohs(udp_header->src_port));
					printf("        dst port: %d\n",
							ntohs(udp_header->dst_port));
					printf("        datagram length: %d\n",
							ntohs(udp_header->len));
					printf("        data:\n==START==\n%s==FINISH==\n\n", packet->get_data_painter());

				}

				delete packet;
			}
		} while (1);
	}

	// int flag = IFF_TAP;
	// int fd = tun_alloc("tun1");

	// printf("fd = %d\n", fd);

	// char buffer[BUFFER_SIZE];
	// int nread;

	// printf("press s to start reading file descriptor...\n");
	// // {} while (getchar() != 's');

	// do
	// {
	//     nread = cread(fd, buffer, BUFFER_SIZE);
	//     printf("readed %d bytes\n", nread);
	// } while (1);

	// close(fd);

	printf("FINISH!");
	return 0;
}
