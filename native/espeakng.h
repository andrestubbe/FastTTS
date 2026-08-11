#pragma once
#include <jni.h>
#include <string>

// Forward declarations for eSpeak-NG JNI methods
#ifdef __cplusplus
extern "C" {
#endif

// Initialize eSpeak-NG library
JNIEXPORT jlong JNICALL Java_fasttts_backends_piper_PiperONNXBackend_espeakInitNative(JNIEnv* env, jobject obj);

// Convert text to IPA phonemes using eSpeak-NG
JNIEXPORT jstring JNICALL Java_fasttts_backends_piper_PiperONNXBackend_textToPhonemesNative(JNIEnv* env, jobject obj, jlong handle, jstring text, jstring voice);

// Cleanup eSpeak-NG library
JNIEXPORT void JNICALL Java_fasttts_backends_piper_PiperONNXBackend_espeakCleanupNative(JNIEnv* env, jobject obj, jlong handle);

#ifdef __cplusplus
}
#endif
