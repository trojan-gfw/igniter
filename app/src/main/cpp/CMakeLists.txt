cmake_minimum_required(VERSION 3.6)
project(igniter-bins)
set(CMAKE_CXX_STANDARD 11)
add_executable(libtrojan.so
    trojan/src/authenticator.cpp
    trojan/src/clientsession.cpp
    trojan/src/config.cpp
    trojan/src/forwardsession.cpp
    trojan/src/log.cpp
    trojan/src/main.cpp
    trojan/src/serversession.cpp
    trojan/src/service.cpp
    trojan/src/session.cpp
    trojan/src/socks5address.cpp
    trojan/src/ssldefaults.cpp
    trojan/src/sslsession.cpp
    trojan/src/trojanrequest.cpp
    trojan/src/udppacket.cpp
    trojan/src/version.cpp)
find_package(Threads REQUIRED)
target_link_libraries(libtrojan.so ${CMAKE_THREAD_LIBS_INIT})
include_directories(${CMAKE_SOURCE_DIR}/libs/include)
target_link_libraries(libtrojan.so
    ${CMAKE_SOURCE_DIR}/libs/lib/libboost_program_options.a
    ${CMAKE_SOURCE_DIR}/libs/lib/libboost_system.a
    ${CMAKE_SOURCE_DIR}/libs/lib/libssl.a
    ${CMAKE_SOURCE_DIR}/libs/lib/libcrypto.a)
