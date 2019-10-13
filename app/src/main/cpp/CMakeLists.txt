cmake_minimum_required(VERSION 3.6)
project(igniter)
add_definitions(-DENABLE_ANDROID_LOG)
find_library( # Sets the name of the path variable.
          androidLogLib

          # Specifies the name of the NDK library that
          # you want CMake to locate.
          log)
add_library(trojan
    trojan/src/core/authenticator.cpp
    trojan/src/core/config.cpp
    trojan/src/core/log.cpp
    trojan/src/core/service.cpp
    trojan/src/core/version.cpp
    trojan/src/proto/socks5address.cpp
    trojan/src/proto/trojanrequest.cpp
    trojan/src/proto/udppacket.cpp
    trojan/src/session/clientsession.cpp
    trojan/src/session/forwardsession.cpp
    trojan/src/session/natsession.cpp
    trojan/src/session/serversession.cpp
    trojan/src/session/session.cpp
    trojan/src/session/udpforwardsession.cpp
    trojan/src/ssl/ssldefaults.cpp
    trojan/src/ssl/sslsession.cpp)
target_include_directories(trojan PRIVATE
    ${CMAKE_SOURCE_DIR}/libs/include
    ${CMAKE_SOURCE_DIR}/libs/include/${ANDROID_ABI}
    ${CMAKE_SOURCE_DIR}/trojan/src)
target_link_libraries(trojan
    ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libboost_program_options.a
    ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libboost_system.a
    ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libssl.a
    ${CMAKE_SOURCE_DIR}/libs/lib/${ANDROID_ABI}/libcrypto.a
    ${androidLogLib})
add_library(jni-helper SHARED jni-helper.cpp)
target_include_directories(jni-helper PRIVATE
    ${CMAKE_SOURCE_DIR}/trojan/src
    ${CMAKE_SOURCE_DIR}/libn2t/src
    ${CMAKE_SOURCE_DIR}/libs/include
    ${CMAKE_SOURCE_DIR}/libs/include/${ANDROID_ABI})
target_link_libraries(jni-helper trojan)
