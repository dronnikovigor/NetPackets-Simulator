cmake_minimum_required(VERSION 2.8)

project (ut2-shaper)

include_directories($ENV{JAVA_HOME}/include
                    $ENV{JAVA_HOME}/include/linux)

set (CMAKE_CXX_FLAGS "-std=c++11 -Wall -Wextra -g")
# disallow unresolved symbols 
set (CMAKE_SHARED_LINKER_FLAGS "-Wl,--no-undefined")

add_library(tunnel SHARED
	tunnel.cpp
    java_api.cpp
    checksum.cpp
    )