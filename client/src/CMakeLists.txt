cmake_minimum_required(VERSION 2.8)

project (ut2-shaper)

# needed headers
include_directories("./include")

# target_link_libraries(ut2 "../lib/libut2.so")
link_directories(${PROJECT_SOURCE_DIR}/../lib)

set(CMAKE_CXX_FLAGS "-std=c++11 -Wall -Wextra -O0 -g -DLOGGING")

add_executable(client
    client.cpp
    configuration.cpp
    )

target_link_libraries(client ut2)