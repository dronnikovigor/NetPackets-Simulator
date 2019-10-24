#ifndef SERVER_ARGS_H__
#define SERVER_ARGS_H__

#include <netinet/in.h>

#define UT2_EXPORT extern "C" __attribute__((visibility("default")))

struct ut2_ServerArgs {
    struct Udp {
        struct Send {
            int socket_buffer_size;
            unsigned int concurrency;
            // microseconds
            unsigned int fetch_rate; 
            // microseconds
            unsigned int flush_rate; 
        } sender;
        struct Receive {
            int socket_buffer_size;
            unsigned int concurrency;
            int timeout_ms;
        } recver;
        sockaddr_in bind_addr;
        bool enabled;
    } udp;
    struct Tcp {
        struct EPoll {
            unsigned int concurrency;
        } epoll;
        struct Binding {
            sockaddr_in bind_addr;
            bool enabled;
        } binding;
        struct Opt {
            bool client_mode;
        } opt;
        bool enabled;
    } tcp;
    struct Agent {
        unsigned int concurrency;
        unsigned int max_packets_queued;
    } agent;
};

UT2_EXPORT void ut2_ServerArgs_set_defaults(ut2_ServerArgs *args);

#endif