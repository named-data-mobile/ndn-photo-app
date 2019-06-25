cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE ON)

set(SQLITE_VERSION 3.28.0)
set(SQLITE_LIBS ${ANDROID_NDK}/packages/sqlite/${SQLITE_VERSION}/libs/${ANDROID_ABI})

set(OPENSSL_VERSION 1.1.1-pre8)
set(OPENSSL_LIBS ${ANDROID_NDK}/packages/openssl/${OPENSSL_VERSION}/libs/${ANDROID_ABI})

set(BOOST_VERSION 1.70.0)
set(BOOST_DIR ${ANDROID_NDK}/packages/boost/${BOOST_VERSION})

set(NDN_CXX_VERSION 0.6.6)
set(NDN_CXX_LIBS ${ANDROID_NDK}/packages/ndn_cxx/${NDN_CXX_VERSION}/libs/${ANDROID_ABI}/llvm)
set(NDN_CXX_INCLUDE ${ANDROID_NDK}/packages/ndn_cxx/${NDN_CXX_VERSION}/include)

set(PSYNC_VERSION 0.1.0)
set(PSYNC_LIBS ${ANDROID_NDK}/packages/psync/${PSYNC_VERSION}/libs/${ANDROID_ABI}/llvm)
set(PSYNC_INCLUDE ${ANDROID_NDK}/packages/psync/${PSYNC_VERSION}/include)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++14 -frtti -fexceptions -DBOOST_LOG_DYN_LINK")

# Add ndn-cxx
add_library(libndn-cxx SHARED IMPORTED)
set_target_properties(libndn-cxx PROPERTIES IMPORTED_LOCATION ${NDN_CXX_LIBS}/libndn-cxx.so)

# Add PSync
add_library(libPSync SHARED IMPORTED)
set_target_properties(libPSync PROPERTIES IMPORTED_LOCATION ${PSYNC_LIBS}/libPSync.so)

# Add boost
set( Boost_INCLUDE_DIR ${BOOST_DIR}/include )
set( Boost_LIBRARY_DIR ${BOOST_DIR}/libs/${ANDROID_ABI}/llvm )

find_package( Boost ${BOOST_VERSION} COMPONENTS system atomic regex filesystem date_time iostreams
        program_options chrono thread log log_setup REQUIRED)

# Add opnessl
add_library(libssl SHARED IMPORTED)
set_target_properties(libssl PROPERTIES IMPORTED_LOCATION ${OPENSSL_LIBS}/libssl.so)
add_library(libcrypto SHARED IMPORTED)
set_target_properties(libcrypto PROPERTIES IMPORTED_LOCATION ${OPENSSL_LIBS}/libcrypto.so)

# Add sqlite
add_library(libsql SHARED IMPORTED)
set_target_properties(libsql PROPERTIES IMPORTED_LOCATION ${SQLITE_LIBS}/libsqlite3.so)

# Add JNI
add_library(psync-jni SHARED psync-wrapper.cpp)
target_include_directories(psync-jni PUBLIC ${NDN_CXX_INCLUDE} ${Boost_INCLUDE_DIR} ${PSYNC_INCLUDE})

target_link_libraries(psync-jni libndn-cxx ${Boost_LIBRARIES} libsql libcrypto libssl android log libPSync)

#-----------------------------------------------------------------------------------------------
# Copy library files to src/main/lib so that gradle can pick them up to package in the apk
# as set by the setting sourceSets.main.jniLibs.srcDirs = ['src/main/libs']

set(LIBS_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../libs/${ANDROID_ABI})

file(COPY ${OPENSSL_LIBS}/libcrypto.so DESTINATION ${LIBS_DIR})
file(COPY ${OPENSSL_LIBS}/libssl.so DESTINATION ${LIBS_DIR})

file(COPY ${SQLITE_LIBS}/libsqlite3.so DESTINATION ${LIBS_DIR})

set( Boost_COMPONENT_LIST system atomic regex filesystem date_time iostreams
        program_options chrono thread stacktrace_basic log log_setup)

foreach( file_i ${Boost_COMPONENT_LIST})
    file(COPY ${Boost_LIBRARY_DIR}/libboost_${file_i}.so DESTINATION ${LIBS_DIR})
endforeach( file_i )

file(COPY ${NDN_CXX_LIBS}/libndn-cxx.so DESTINATION ${LIBS_DIR})
file(COPY ${PSYNC_LIBS}/libPSync.so DESTINATION ${LIBS_DIR})
