#ifndef CHECKSUM_H_
#define CHECKSUM_H_

#include <bits/stdint-uintn.h>
#include <netinet/in.h>
#include <stddef.h>

namespace checksum
{

uint16_t udp_tcp_checksum(const void *buff, size_t len, in_addr_t src_addr,
		in_addr_t dest_addr, uint16_t protocol);

uint16_t ip_checksum(const void *buff, size_t len);

void calculate_checksums(const char *buf);

}

#endif /* CHECKSUM_H_ */
