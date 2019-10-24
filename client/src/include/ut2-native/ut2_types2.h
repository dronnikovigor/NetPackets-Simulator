#ifndef UT2_TYPES2_H__
#define UT2_TYPES2_H__

#include <arpa/inet.h>

typedef in_addr_t ut2_host;
typedef in_addr_t ut2_Host;
typedef in_port_t ut2_port;
typedef in_port_t ut2_Port;

struct ut2_Addr {
    ut2_Host host;     // network byte order (big-endian)
    ut2_Port port_udp; // network byte order (big-endian)
    ut2_Port port_tcp; // network byte order (big-endian)
};

typedef ut2_Addr ut2_addr;

#endif