#ifndef UT2_POOL_H__
#define UT2_POOL_H__

#include <arpa/inet.h>

#include "ut2_types2.h"
#include "server_args.h"

#include "stats.h"

struct ut2_Pool_s;
struct ut2_Cluster_s;
struct ut2_CipherCtx_s;
struct ut2_Request_s;
struct ut2_PipeSet_s;
struct ut2_Pipe_s;
struct ut2_Mon_s;

typedef ut2_Pool_s      *ut2_Pool;
typedef ut2_Cluster_s   *ut2_Cluster;
typedef ut2_CipherCtx_s *ut2_CipherCtx;
typedef ut2_Request_s   *ut2_Request;
typedef ut2_PipeSet_s   *ut2_PipeSet;
typedef ut2_Pipe_s      *ut2_Pipe;
typedef ut2_Mon_s       *ut2_Mon;

enum ut2_Z {
    ut2_Z_None    = 0,
    ut2_Z_Deflate = 1,
};

enum ut2_StreamFlag {
    ut2_Stream_Closed    = 1,
    ut2_Stream_Cancelled = 2,
};

typedef int ut2_Int;
typedef unsigned int  ut2_UInt;
typedef unsigned char ut2_Bool;
typedef unsigned char ut2_UByte;

typedef int ut2_MonKey;

struct ut2_Request_Status {
    char     err[32];
    ut2_UInt bytes_avail;
    ut2_UInt conn_delay;
    ut2_Bool closed;
};

struct ut2_Pool_BindingInfo {
    char err_msg[64];
    sockaddr_in udp_addr;
    ut2_Bool bound;
};

struct ut2_StreamArgs {
    char handler[32];
    ut2_CipherCtx cipher_ctx;
    ut2_Z inp_z, out_z;
    ut2_UByte priority;
};

UT2_EXPORT ut2_Pool ut2_Pool_new          (const ut2_ServerArgs *args, ut2_Pool_BindingInfo *bind_info);
UT2_EXPORT void     ut2_Pool_free         (ut2_Pool);
UT2_EXPORT void     ut2_Pool_stats_read   (ut2_Pool, ut2_AgentStats *stats);
UT2_EXPORT void     ut2_Pool_stats_refresh(ut2_Pool);

UT2_EXPORT ut2_Cluster ut2_Cluster_new      (ut2_Pool);
UT2_EXPORT void        ut2_Cluster_free     (ut2_Cluster);
UT2_EXPORT void        ut2_Cluster_update   (ut2_Cluster, const ut2_Addr *, ut2_UInt amount);
UT2_EXPORT void        ut2_Cluster_put_host (ut2_Cluster, const ut2_Addr *);
UT2_EXPORT ut2_Pool    ut2_Cluster_get_pool (ut2_Cluster);
/**
 * Returns cluster attributes - multiline C-string.
 * Might return null when no OPT-CLUSTER-STATUS is received.
 */
UT2_EXPORT const char *ut2_Cluster_get_props(ut2_Cluster);

UT2_EXPORT ut2_CipherCtx ut2_CipherCtx_new                 (ut2_Pool);
UT2_EXPORT void          ut2_CipherCtx_free                (ut2_CipherCtx);
UT2_EXPORT void          ut2_CipherCtx_set_verification_key(ut2_CipherCtx, const char *);
UT2_EXPORT void          ut2_CipherCtx_assign_cluster      (ut2_CipherCtx, ut2_Cluster);
UT2_EXPORT bool          ut2_CipherCtx_load                (ut2_CipherCtx, const void *, ut2_UInt);
UT2_EXPORT const void   *ut2_CipherCtx_store               (ut2_CipherCtx, ut2_UInt *);

UT2_EXPORT ut2_Request ut2_Request_new       (ut2_Cluster, const ut2_StreamArgs *);
UT2_EXPORT void        ut2_Request_free      (ut2_Request);
UT2_EXPORT void        ut2_Request_set_mon   (ut2_Request, ut2_Mon, ut2_MonKey);
UT2_EXPORT void        ut2_Request_write     (ut2_Request, const void *ptr, ut2_UInt len);
UT2_EXPORT void        ut2_Request_execute   (ut2_Request);
UT2_EXPORT void        ut2_Request_get_status(ut2_Request, ut2_Request_Status *);
UT2_EXPORT ut2_UInt    ut2_Request_read      (ut2_Request, void *ptr, ut2_UInt len);

UT2_EXPORT ut2_PipeSet ut2_PipeSet_new(ut2_Cluster, const ut2_StreamArgs *);
UT2_EXPORT void        ut2_PipeSet_free(ut2_PipeSet);
UT2_EXPORT void        ut2_PipeSet_set_mon(ut2_PipeSet, ut2_Mon, ut2_MonKey);
UT2_EXPORT ut2_Pipe    ut2_PipeSet_accept(ut2_PipeSet);

UT2_EXPORT void        ut2_Pipe_free     (ut2_Pipe);
UT2_EXPORT void        ut2_Pipe_set_mon  (ut2_Pipe, ut2_Mon, ut2_MonKey);
UT2_EXPORT void        ut2_Pipe_push     (ut2_Pipe, const void *ptr, ut2_UInt  len);
UT2_EXPORT const char *ut2_Pipe_pull     (ut2_Pipe, ut2_UInt *len);
UT2_EXPORT ut2_Bool    ut2_Pipe_is_closed(ut2_Pipe);
UT2_EXPORT const char *ut2_Pipe_get_error(ut2_Pipe);

UT2_EXPORT ut2_Mon  ut2_Mon_new   (ut2_Pool);
UT2_EXPORT void     ut2_Mon_free  (ut2_Mon);
UT2_EXPORT void     ut2_Mon_wake  (ut2_Mon);
UT2_EXPORT ut2_UInt ut2_Mon_select(ut2_Mon, ut2_MonKey *arr, ut2_UInt amount, ut2_Int timeout_ms);

#endif