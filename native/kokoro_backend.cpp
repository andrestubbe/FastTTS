#include "fasttts.h"
#include <string>
#include <vector>

/**
 * Native Kokoro ONNX Bridge.
 * 
 * NOTE: This requires onnxruntime.dll and headers.
 * For now, we provide the JNI plumbing.
 */

struct KokoroSession {
    std::wstring modelPath;
    // Ort::Session* session; // Placeholder for ONNX Runtime session
};

JNIEXPORT jlong JNICALL Java_fasttts_KokoroBackend_initNative(JNIEnv* env, jobject obj, jstring modelPath) {
    std::wstring path = jstringToWString(env, modelPath);
    KokoroSession* session = new KokoroSession();
    session->modelPath = path;
    
    // TODO: Initialize ONNX Runtime here
    // Ort::Env env(ORT_LOGGING_LEVEL_WARNING, "Kokoro");
    // session->session = new Ort::Session(env, path.c_str(), Ort::SessionOptions{});
    
    return (jlong)session;
}

JNIEXPORT jbyteArray JNICALL Java_fasttts_KokoroBackend_synthesizeNative(JNIEnv* env, jobject obj, jlong handle, jstring text, jstring voiceId) {
    KokoroSession* session = (KokoroSession*)handle;
    std::wstring wtext = jstringToWString(env, text);
    
    // TODO: Run inference
    // 1. Tokenize text
    // 2. Prepare inputs (text tokens, voice style)
    // 3. Run session
    // 4. Extract output float array and convert to PCM bytes
    
    // Return empty array for now to ensure compilation
    return env->NewByteArray(0);
}

JNIEXPORT void JNICALL Java_fasttts_KokoroBackend_streamNative(JNIEnv* env, jobject obj, jlong handle, jstring text, jstring voiceId, jobject chunkConsumer) {
    // Similar to synthesize but call chunkConsumer.accept() for each synthesized sentence or chunk
}

JNIEXPORT void JNICALL Java_fasttts_KokoroBackend_releaseNative(JNIEnv* env, jobject obj, jlong handle) {
    KokoroSession* session = (KokoroSession*)handle;
    if (session) {
        // delete session->session;
        delete session;
    }
}
