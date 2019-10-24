#ifndef CONFIGURATION_H__
#define CONFIGURATION_H__

#include "rapidjson/document.h"
#include <string>
#include <ut2-native/ut2_types2.h>
#include <vector>

class Configuration
{
public:
    explicit Configuration(rapidjson::Document &document);

    Configuration(Configuration &) = default;
    Configuration(Configuration &&) = default;
    Configuration &operator=(Configuration &) = default;
    Configuration &operator=(Configuration &&) = default;

    const bool tcp_mode;
    const bool udp_mode;
    const std::string client_ip;
    std::vector<ut2_Addr> servers;
};

Configuration parseConfiguration(std::string configurationPath);

#endif