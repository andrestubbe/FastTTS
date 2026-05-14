#pragma once
#include <jni.h>
#include <string>

std::wstring jstringToWString(JNIEnv* env, jstring jstr);
#include <windows.h>
#include <string>
#include <vector>

// Forward declarations for JNI methods
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jbyteArray JNICALL Java_fasttts_WindowsTTSBackend_synthesizeNative(JNIEnv* env, jobject obj, jstring text, jstring voiceId, jfloat rate, jfloat pitch, jfloat volume);

JNIEXPORT jobject JNICALL Java_fasttts_WindowsTTSBackend_getVoicesNative(JNIEnv* env, jobject obj);

// --- Kokoro ONNX Bridge ---
JNIEXPORT jlong JNICALL Java_fasttts_KokoroBackend_initNative(JNIEnv* env, jobject obj, jstring modelPath);
JNIEXPORT jbyteArray JNICALL Java_fasttts_KokoroBackend_synthesizeNative(JNIEnv* env, jobject obj, jlong handle, jstring text, jstring voiceId);
JNIEXPORT void JNICALL Java_fasttts_KokoroBackend_streamNative(JNIEnv* env, jobject obj, jlong handle, jstring text, jstring voiceId, jobject chunkConsumer);
JNIEXPORT void JNICALL Java_fasttts_KokoroBackend_releaseNative(JNIEnv* env, jobject obj, jlong handle);

#ifdef __cplusplus
}
#endif
