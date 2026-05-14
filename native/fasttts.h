#pragma once
#include <jni.h>
#include <windows.h>
#include <string>
#include <vector>

// Forward declarations for JNI methods
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jbyteArray JNICALL Java_fasttts_WindowsTTSBackend_synthesizeNative(JNIEnv* env, jobject obj, jstring text, jstring voiceId, jfloat rate, jfloat pitch, jfloat volume);

JNIEXPORT jobject JNICALL Java_fasttts_WindowsTTSBackend_getVoicesNative(JNIEnv* env, jobject obj);

#ifdef __cplusplus
}
#endif
