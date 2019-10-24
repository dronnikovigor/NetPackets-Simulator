#ifndef UT2_STATS_H__
#define UT2_STATS_H__

struct ut2_AgentStats {
    struct {
        int opened;
        int closed;
    } peers;
    struct {
        struct {
            int overall;
            int data;
            int dat0;
            int ctrl;
            int halt;
            int ping;
            int opt;
        } recv, sent;
        int received_via;
        int delivered_via;
    } udp;
    struct {
        struct {
            int data;
            int ctrl;
            int ping;
            int halt;
            int opt;
        } recv, sent;
        int received_via;
        int delivered_via;
    } tcp;

    /* customs */
    int udp_retransmit;
};

#endif