#include "configuration.h"
#include "rapidjson/filereadstream.h"

Configuration::Configuration(rapidjson::Document &document) :
tcp_mode(document["tcp_mode"].GetBool()),
udp_mode(document["udp_mode"].GetBool()),
client_ip(document["client_ip"].GetString())
{
    const rapidjson::Value &servers_array = document["servers"];
    assert(servers_array.IsArray());
    servers.reserve(servers_array.Size());
    for (rapidjson::SizeType i = 0; i < servers_array.Size(); i++) // Uses SizeType instead of size_t
    {
        const auto server_params = servers_array[i].GetObject();

        ut2_Addr server;
        inet_pton(AF_INET, server_params["ip"].GetString(), &server.host);
        if (tcp_mode)
            server.port_tcp = htons(server_params["port"].GetInt());
        else
            server.port_udp = htons(server_params["port"].GetInt());
        servers.push_back(server);
    }
};

Configuration parseConfiguration(std::string configurationPath)
{
    FILE *fp = fopen(configurationPath.c_str(), "r");
    //TODO change buffer size
    char readBuffer[65536];
    rapidjson::FileReadStream is(fp, readBuffer, sizeof(readBuffer));
    rapidjson::Document d;
    d.ParseStream(is);
    return Configuration(d);
}