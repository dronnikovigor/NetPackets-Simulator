#include "checksum.h"

#include <netinet/ip.h>
#include <netinet/tcp.h>
#include <netinet/udp.h>

uint16_t checksum::udp_tcp_checksum(const void *buff, size_t len,
		in_addr_t src_addr, in_addr_t dest_addr, uint16_t protocol)
{
	const uint16_t *buf = (uint16_t*) buff;
	uint16_t *ip_src = (uint16_t*) &src_addr, *ip_dst = (uint16_t*) &dest_addr;
	uint32_t sum;
	size_t length = len;

	// Calculate the sum
	sum = 0;
	while (len > 1)
	{
		sum += *buf++;
		len -= 2;
	}

	if (len & 1)
		// Add the padding if the packet lenght is odd
		sum += *((uint8_t*) buf);

	// Add the pseudo-header
	sum += *(ip_src++);
	sum += *ip_src;

	sum += *(ip_dst++);
	sum += *ip_dst;

	sum += htons(protocol);
	sum += htons(length);

	// Add the carries
	while (sum >> 16)
		sum = (sum & 0xFFFF) + (sum >> 16);

	// Return the one's complement of sum
	return (~sum);
}

uint16_t checksum::ip_checksum(const void *buff, size_t len)
{
	const uint16_t *buf = (uint16_t*) buff;
	uint32_t sum;

	// Calculate the sum
	sum = 0;
	while (len > 1)
	{
		sum += *buf++;
		if (sum & 0x800)
			sum = (sum & 0xFFFF) + (sum >> 16);
		len -= 2;
	}

	if (len & 1)
		// Add the padding if the packet lenght is odd
		sum += *((uint8_t*) buf);

	// Add the carries
	while (sum >> 16)
		sum = (sum & 0xFFFF) + (sum >> 16);

	// Return the one's complement of sum
	return ((uint16_t) (~sum));
}

void checksum::calculate_checksums(const char *buf)
{
	//set pointer to beginning of ip header
	iphdr *ip_header = (iphdr*) (buf + 4);
	//in bytes
	size_t total_packet_len = ntohs(ip_header->tot_len);
	//in bytes
	size_t ip_header_len = ip_header->ihl * 4;

	ip_header->check = 0;
	ip_header->check = ip_checksum(ip_header, ip_header->ihl * 4);

	switch (ip_header->protocol)
	{
	case IPPROTO_UDP:
	{
		udphdr *udp_header = (udphdr*) (((char*) ip_header) + ip_header_len);
		udp_header->check = 0;
		udp_header->check = udp_tcp_checksum(udp_header, ntohs(udp_header->len),
				ip_header->saddr, ip_header->daddr, IPPROTO_UDP);
		break;
	}
	case IPPROTO_TCP:
	{
		tcphdr *tcp_header = (tcphdr*) (((char*) ip_header) + ip_header_len);
		tcp_header->check = 0;
		tcp_header->check = udp_tcp_checksum(tcp_header,
				total_packet_len - ip_header_len, ip_header->saddr,
				ip_header->daddr, IPPROTO_TCP);
		break;
	}
	default:
		break;
		//ignore
	};

}
