/*
 * Copyright (c) 2014-2019,  The University of Memphis
 *
 * This file is part of PSync.
 * See AUTHORS.md for complete list of PSync authors and contributors.
 *
 * PSync is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * PSync is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * PSync, e.g., in COPYING.md file.  If not, see <http://www.gnu.org/licenses/>.
 **/

#include "net_named_data_jni_psync_PSync.h"
#include "net_named_data_jni_psync_PSync_FullProducer.h"
#include "net_named_data_jni_psync_PSync_Consumer.h"
#include "net_named_data_jni_psync_PSync_PartialProducer.h"

#include <ndn-cxx/name.hpp>
#include <ndn-cxx/face.hpp>
#include <ndn-cxx/interest.hpp>

#include <PSync/full-producer.hpp>
#include <PSync/partial-producer.hpp>
#include <PSync/consumer.hpp>

#include <thread>
#include <iostream>

#include <android/log.h>
#include <android/native_activity.h>

#define  LOG_TAG    "testjni"
#define  ALOG(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

static std::unique_ptr<ndn::Face> g_facePtr;
static std::unique_ptr<std::thread> g_thread;

static JavaVM* g_jvm;
static jobject g_fullProducerObject = nullptr;
static jobject g_consumerObject = nullptr;
static jclass g_arrayList;
static jclass g_missingDataInfoClass;
static jmethodID g_addToArrayList;
static jmethodID g_arrayListConstructor;
static jmethodID g_onFullProducerSyncUpdate;
static jmethodID g_onHelloDataUpdate;
static jmethodID g_onConsumerSyncUpdate;
static jmethodID g_mdiConstructor;

class FullProducerWrapper {
public:
  std::unique_ptr<psync::FullProducer> fullProducer;
};

JNIEXPORT void JNICALL Java_net_named_1data_jni_psync_PSync_initialize
        (JNIEnv *env, jobject thisObject, jstring homePath)
{
  if (!g_facePtr) {
    ALOG("%s", "Initializing face");
    ::setenv("HOME", env->GetStringUTFChars(homePath, 0), true);
    g_facePtr = std::make_unique<ndn::Face>("127.0.0.1");
  }

  if (!g_thread) {
    g_thread = std::make_unique<std::thread>([] {
        ALOG("%s", "Starting process events thread");
        try {
          // If keepThread is not passed as true, then PSync does not seem to set the interest filter in NFD
          g_facePtr->processEvents(ndn::time::milliseconds::zero(), true);
        }
        catch (const std::exception &e) {
          ALOG("%s", e.what());
        }
    });
  }

  // Save reference to global JVM
  env->GetJavaVM(&g_jvm);

  g_arrayList = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/ArrayList")));
  g_addToArrayList = env->GetMethodID(g_arrayList, "add", "(Ljava/lang/Object;)Z");
  g_arrayListConstructor = env->GetMethodID(g_arrayList, "<init>", "(I)V");

  jclass fullProducerClass = env->FindClass("net/named_data/jni/psync/PSync$FullProducer");
  if (fullProducerClass == 0) {
    ALOG("%s", "Full Producer class not found");
    return;
  }
  g_onFullProducerSyncUpdate = env->GetMethodID(fullProducerClass, "onSyncUpdate", "(Ljava/util/ArrayList;)V");

  g_missingDataInfoClass = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("net/named_data/jni/psync/MissingDataInfo")));

  if (g_missingDataInfoClass == 0) {
    ALOG("%s", "MissingDataInfo class not found");
    return;
  }

  g_mdiConstructor = env->GetMethodID(g_missingDataInfoClass, "<init>", "(Ljava/lang/String;JJ)V");

  jclass consumerClass = env->FindClass("net/named_data/jni/psync/PSync$Consumer");
  if (consumerClass == 0) {
    ALOG("%s", "Consumer class not found");
    return;
  }
  g_onHelloDataUpdate = env->GetMethodID(consumerClass, "onHelloData", "(Ljava/util/ArrayList;)V");
  g_onConsumerSyncUpdate = env->GetMethodID(consumerClass, "onSyncData", "(Ljava/util/ArrayList;)V");
}

JNIEXPORT void JNICALL Java_net_named_1data_jni_psync_PSync_stop
  (JNIEnv *, jobject)
{
  g_facePtr->shutdown();
}

void
processFullProducerSyncUpdate(const std::vector<psync::MissingDataInfo>& updates)
{
  JNIEnv *env;
  jint res = g_jvm->AttachCurrentThread(&env, nullptr);

  if (JNI_OK != res) {
    ALOG("%s", "Failed to AttachCurrentThread");
    return;
  }

  jobject result = env->NewObject(g_arrayList, g_arrayListConstructor, updates.size());

  for (const auto& update : updates) {
    jstring jstr = env->NewStringUTF(update.prefix.toUri().c_str());

    jobject mdiObj = env->NewObject(g_missingDataInfoClass, g_mdiConstructor,
                                    jstr, update.lowSeq, update.highSeq);

    env->CallBooleanMethod(result, g_addToArrayList, mdiObj);

    env->DeleteLocalRef(jstr);
    env->DeleteLocalRef(mdiObj);
  }

  env->CallVoidMethod(g_fullProducerObject, g_onFullProducerSyncUpdate, result);
  env->DeleteLocalRef(result);
  g_jvm->DetachCurrentThread();
}

JNIEXPORT
jobject
JNICALL Java_net_named_1data_jni_psync_PSync_00024FullProducer_startFullProducer(
  JNIEnv *env, jobject thisObject, jint ibfSize, jstring syncPrefix,
  jstring userPrefix, jlong syncInterestLifetimeMillis, jlong syncReplyFreshnessMillis)
{
  if (g_fullProducerObject == nullptr) {
    g_fullProducerObject = env->NewGlobalRef(thisObject);
  }
  FullProducerWrapper* fullProducerWrapper = new FullProducerWrapper();
  ndn::Name syncPrefixName(env->GetStringUTFChars(syncPrefix, nullptr));
  ndn::Name userPrefixName(env->GetStringUTFChars(userPrefix, nullptr));

  ALOG("%s", "Initializing PSync Full Producer");
  fullProducerWrapper->fullProducer = std::make_unique<psync::FullProducer>((size_t) ibfSize,
                  *g_facePtr,
                  syncPrefixName,
                  userPrefixName,
                  [](const std::vector<psync::MissingDataInfo> &updates) {
                      processFullProducerSyncUpdate(updates);
                  },
                  ndn::time::milliseconds(syncInterestLifetimeMillis),
                  ndn::time::milliseconds(syncReplyFreshnessMillis));

  return env->NewDirectByteBuffer(fullProducerWrapper, 0);
}

JNIEXPORT void JNICALL Java_net_named_1data_jni_psync_PSync_00024FullProducer_stop
  (JNIEnv *env, jobject obj, jobject handle)
{
  FullProducerWrapper* fullProducerWrapper = (FullProducerWrapper*) env->GetDirectBufferAddress(handle);
  delete fullProducerWrapper;
}

JNIEXPORT jboolean JNICALL Java_net_named_1data_jni_psync_PSync_00024FullProducer_addUserNode
  (JNIEnv *env, jobject obj, jobject handle, jstring prefix)
{
  FullProducerWrapper* fullProducerWrapper = (FullProducerWrapper*) env->GetDirectBufferAddress(handle);
  return fullProducerWrapper->fullProducer->addUserNode(ndn::Name(env->GetStringUTFChars(prefix, nullptr)));
}

JNIEXPORT void JNICALL Java_net_named_1data_jni_psync_PSync_00024FullProducer_removeUserNode
  (JNIEnv *env, jobject obj, jobject handle, jstring prefix)
{
  FullProducerWrapper* fullProducerWrapper = (FullProducerWrapper*) env->GetDirectBufferAddress(handle);
  fullProducerWrapper->fullProducer->removeUserNode(ndn::Name(env->GetStringUTFChars(prefix, nullptr)));
}

JNIEXPORT jlong JNICALL Java_net_named_1data_jni_psync_PSync_00024FullProducer_getSeqNo
  (JNIEnv *env, jobject obj, jobject handle, jstring prefix)
{
  FullProducerWrapper* fullProducerWrapper = (FullProducerWrapper*) env->GetDirectBufferAddress(handle);
  return fullProducerWrapper->fullProducer->getSeqNo(ndn::Name(env->GetStringUTFChars(prefix, nullptr))).value();
}

JNIEXPORT void JNICALL Java_net_named_1data_jni_psync_PSync_00024FullProducer_publishName
  (JNIEnv *env, jobject obj, jobject handle, jstring prefix)
{
  FullProducerWrapper* fullProducerWrapper = (FullProducerWrapper*) env->GetDirectBufferAddress(handle);
  fullProducerWrapper->fullProducer->publishName(ndn::Name(env->GetStringUTFChars(prefix, nullptr)));
}

// ---------------------------------PartialProducer-start-------------------------------------------

class PartialProducerWrapper {
public:
    std::unique_ptr<psync::PartialProducer> partialProducer;
};

JNIEXPORT jobject JNICALL Java_net_named_1data_jni_psync_PSync_00024PartialProducer_startPartialProducer
  (JNIEnv *env, jobject thisObject, jint ibfSize, jstring syncPrefix, jstring userPrefix, jlong helloReplyFreshness, jlong syncReplyFreshness)
{
  PartialProducerWrapper *partialProducerWrapper = new PartialProducerWrapper();

  ndn::Name syncPrefixName(env->GetStringUTFChars(syncPrefix, nullptr));
  ndn::Name userPrefixName(env->GetStringUTFChars(userPrefix, nullptr));

  partialProducerWrapper->partialProducer = std::make_unique<psync::PartialProducer>((size_t) ibfSize,
                                              *g_facePtr,
                                              syncPrefixName,
                                              userPrefixName,
                                              ndn::time::milliseconds(helloReplyFreshness),
                                              ndn::time::milliseconds(syncReplyFreshness));

  return env->NewDirectByteBuffer(partialProducerWrapper, 0);
}

JNIEXPORT void JNICALL Java_net_named_1data_jni_psync_PSync_00024PartialProducer_stop
  (JNIEnv *env, jobject, jobject handle)
{
  PartialProducerWrapper* partialProducerWrapper = (PartialProducerWrapper*) env->GetDirectBufferAddress(handle);
  delete partialProducerWrapper;
}

JNIEXPORT jboolean JNICALL Java_net_named_1data_jni_psync_PSync_00024PartialProducer_addUserNode
  (JNIEnv *env, jobject, jobject handle, jstring prefix)
{
  PartialProducerWrapper* partialProducerWrapper = (PartialProducerWrapper*) env->GetDirectBufferAddress(handle);
  return partialProducerWrapper->partialProducer->addUserNode(ndn::Name(env->GetStringUTFChars(prefix, nullptr)));
}

JNIEXPORT void JNICALL Java_net_named_1data_jni_psync_PSync_00024PartialProducer_removeUserNode
  (JNIEnv *env, jobject, jobject handle, jstring prefix)
{
  PartialProducerWrapper* partialProducerWrapper = (PartialProducerWrapper*) env->GetDirectBufferAddress(handle);
  partialProducerWrapper->partialProducer->removeUserNode(ndn::Name(env->GetStringUTFChars(prefix, nullptr)));
}

JNIEXPORT jlong JNICALL Java_net_named_1data_jni_psync_PSync_00024PartialProducer_getSeqNo
  (JNIEnv *env, jobject, jobject handle, jstring prefix)
{
  PartialProducerWrapper* partialProducerWrapper = (PartialProducerWrapper*) env->GetDirectBufferAddress(handle);
  return partialProducerWrapper->partialProducer->getSeqNo(ndn::Name(env->GetStringUTFChars(prefix, nullptr))).value();
}

JNIEXPORT void JNICALL Java_net_named_1data_jni_psync_PSync_00024PartialProducer_publishName
  (JNIEnv *env, jobject, jobject handle, jstring prefix)
{
  PartialProducerWrapper* partialProducerWrapper = (PartialProducerWrapper*) env->GetDirectBufferAddress(handle);
  partialProducerWrapper->partialProducer->publishName(ndn::Name(env->GetStringUTFChars(prefix, nullptr)));
}

// ---------------------------------Consumer-start-------------------------------------------
class ConsumerWrapper {
public:
    std::unique_ptr<psync::Consumer> consumer;
};

void
processHelloDataUpdate(const std::vector<ndn::Name>& names, jobject consumerObject)
{
  JNIEnv *env;
  jint res = g_jvm->AttachCurrentThread(&env, nullptr);

  if (JNI_OK != res) {
    ALOG("%s", "Failed to AttachCurrentThread");
    return;
  }

  jobject result = env->NewObject(g_arrayList, g_arrayListConstructor, names.size());

  for (const auto& name : names) {
    jstring jstr = env->NewStringUTF(name.toUri().c_str());

    env->CallBooleanMethod(result, g_addToArrayList, jstr);

    env->DeleteLocalRef(jstr);
  }

  env->CallVoidMethod(consumerObject, g_onHelloDataUpdate, result);
  env->DeleteLocalRef(result);
  g_jvm->DetachCurrentThread();
}

void
processConsumerSyncUpdate(const std::vector<psync::MissingDataInfo>& updates, jobject consumerObject)
{
  JNIEnv *env;
  jint res = g_jvm->AttachCurrentThread(&env, nullptr);

  if (JNI_OK != res) {
    ALOG("%s", "Failed to AttachCurrentThread");
    return;
  }

  jobject result = env->NewObject(g_arrayList, g_arrayListConstructor, updates.size());

  for (const auto& update : updates) {
    jstring jstr = env->NewStringUTF(update.prefix.toUri().c_str());

    jobject mdiObj = env->NewObject(g_missingDataInfoClass, g_mdiConstructor,
                                    jstr, update.lowSeq, update.highSeq);

    env->CallBooleanMethod(result, g_addToArrayList, mdiObj);

    env->DeleteLocalRef(jstr);
    env->DeleteLocalRef(mdiObj);
  }

  env->CallVoidMethod(g_consumerObject, g_onConsumerSyncUpdate, result);
  env->DeleteLocalRef(result);
  g_jvm->DetachCurrentThread();
}

JNIEXPORT jobject JNICALL Java_net_named_1data_jni_psync_PSync_00024Consumer_initializeConsumer
  (JNIEnv *env, jobject thisObject, jstring syncPrefix, jint count, jdouble falsePositive,
    jlong helloInterestLifetimeMillis, jlong syncInterestLifetimeMillis)
{

  jobject consumerObject = env->NewGlobalRef(thisObject);

  g_consumerObject = env->NewGlobalRef(thisObject);
  ConsumerWrapper* consumerWrapper = new ConsumerWrapper();
  ndn::Name syncPrefixName(env->GetStringUTFChars(syncPrefix, nullptr));
  consumerWrapper->consumer = std::make_unique<psync::Consumer>(syncPrefixName, *g_facePtr,
    [consumerObject](const std::vector<ndn::Name>& updates) {
      processHelloDataUpdate(updates, consumerObject);
    },
    [consumerObject](const std::vector<psync::MissingDataInfo>& updates) {
      processConsumerSyncUpdate(updates, consumerObject);
    },
    count,
    falsePositive,
    ndn::time::milliseconds(helloInterestLifetimeMillis),
    ndn::time::milliseconds(syncInterestLifetimeMillis));

  return env->NewDirectByteBuffer(consumerWrapper, 0);
}

JNIEXPORT void JNICALL Java_net_named_1data_jni_psync_PSync_00024Consumer_sendHelloInterest
  (JNIEnv *env, jobject, jobject handle)
{
  ConsumerWrapper* consumerWrapper = (ConsumerWrapper*) env->GetDirectBufferAddress(handle);
  consumerWrapper->consumer->sendHelloInterest();
}

JNIEXPORT void JNICALL Java_net_named_1data_jni_psync_PSync_00024Consumer_sendSyncInterest
  (JNIEnv *env, jobject, jobject handle)
{
  ConsumerWrapper* consumerWrapper = (ConsumerWrapper*) env->GetDirectBufferAddress(handle);
  consumerWrapper->consumer->sendSyncInterest();
}

JNIEXPORT jboolean JNICALL Java_net_named_1data_jni_psync_PSync_00024Consumer_addSubscription
  (JNIEnv *env, jobject, jobject handle, jstring prefix)
{
  ConsumerWrapper* consumerWrapper = (ConsumerWrapper*) env->GetDirectBufferAddress(handle);
  ALOG("%s", "Trying to add subscription for consumer");
  return consumerWrapper->consumer->addSubscription(ndn::Name(env->GetStringUTFChars(prefix, nullptr)));
}

JNIEXPORT jobject JNICALL Java_net_named_1data_jni_psync_PSync_00024Consumer_getSubscriptionList
  (JNIEnv *env, jobject, jobject handle)
{
  ConsumerWrapper* consumerWrapper = (ConsumerWrapper*) env->GetDirectBufferAddress(handle);
  const std::set<ndn::Name>& subscriptions = consumerWrapper->consumer->getSubscriptionList();

  jobject result = env->NewObject(g_arrayList, g_arrayListConstructor, subscriptions.size());
  for (const auto& sub : subscriptions) {
    jstring jstr = env->NewStringUTF(sub.toUri().c_str());

    env->CallBooleanMethod(result, g_addToArrayList, jstr);

    env->DeleteLocalRef(jstr);
  }
  return result;
}

JNIEXPORT jboolean JNICALL Java_net_named_1data_jni_psync_PSync_00024Consumer_isSubscribed
  (JNIEnv *env, jobject, jobject handle, jstring prefix)
{
  ConsumerWrapper* consumerWrapper = (ConsumerWrapper*) env->GetDirectBufferAddress(handle);
  return consumerWrapper->consumer->isSubscribed(ndn::Name(env->GetStringUTFChars(prefix, nullptr)));
}

JNIEXPORT jlong JNICALL Java_net_named_1data_jni_psync_PSync_00024Consumer_getSeqNo
  (JNIEnv *env, jobject, jobject handle, jstring prefix)
{
  ConsumerWrapper* consumerWrapper = (ConsumerWrapper*) env->GetDirectBufferAddress(handle);
  return consumerWrapper->consumer->getSeqNo(ndn::Name(env->GetStringUTFChars(prefix, nullptr))).value();
}
