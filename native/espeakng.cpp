#include "espeakng.h"
#include <espeak-ng/espeak_ng.h>
#include <string>
#include <vector>
#include <memory>

// Handle structure for eSpeak-NG context
struct EspeakContext {
    int samplerate;
    bool initialized;
    
    EspeakContext() : samplerate(22050), initialized(false) {}
};

JNIEXPORT jlong JNICALL Java_fasttts_backends_piper_PiperONNXBackend_espeakInitNative(JNIEnv* env, jobject obj) {
    EspeakContext* ctx = new EspeakContext();
    
    // Initialize eSpeak-NG
    int result = espeak_Initialize(AUDIO_OUTPUT_SYNCHRONOUS, 0, NULL, 0);
    if (result < 0) {
        delete ctx;
        return 0;
    }
    
    ctx->samplerate = result;
    ctx->initialized = true;
    
    // Set eSpeak to output IPA phonemes
    espeak_SetParameter(espeakPHONEMES, 1, 0);
    
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT jstring JNICALL Java_fasttts_backends_piper_PiperONNXBackend_textToPhonemesNative(JNIEnv* env, jobject obj, jlong handle, jstring text, jstring voice) {
    EspeakContext* ctx = reinterpret_cast<EspeakContext*>(handle);
    if (!ctx || !ctx->initialized) {
        return nullptr;
    }
    
    // Convert jstring to regular string
    const char* textChars = env->GetStringUTFChars(text, nullptr);
    const char* voiceChars = voice ? env->GetStringUTFChars(voice, nullptr) : nullptr;
    
    std::string textStr(textChars);
    std::string voiceStr(voiceChars ? voiceChars : "");
    
    // Set voice if specified
    if (!voiceStr.empty()) {
        espeak_SetVoiceByName(voiceStr.c_str());
    }
    
    // Set phoneme mode to IPA
    espeak_SetParameter(espeakPHONEMES, 1, 0); // 1 = IPA phonemes
    
    // Synthesize phonemes (using callback to capture output)
    std::string phonemeOutput;
    
    // Set up callback to capture phoneme output
    espeak_SetSynthCallback([](short* wav, int numsamples, espeak_EVENT* events) {
        // We're only interested in phoneme events, not audio
        return 0;
    });
    
    // Actually, for IPA output we need a different approach
    // Use espeak_TextToPhonemes with espeakCHARS_UTF8 and espeakPHONEMES_IPA
    const char* phonemes = espeak_TextToPhonemes(
        (const char*)textStr.c_str(), 
        espeakCHARS_UTF8, 
        espeakPHONEMES_IPA
    );
    
    // Clean up JNI strings
    env->ReleaseStringUTFChars(text, textChars);
    if (voiceChars) {
        env->ReleaseStringUTFChars(voice, voiceChars);
    }
    
    if (phonemes) {
        return env->NewStringUTF(phonemes);
    }
    
    return nullptr;
}

JNIEXPORT void JNICALL Java_fasttts_backends_piper_PiperONNXBackend_espeakCleanupNative(JNIEnv* env, jobject obj, jlong handle) {
    EspeakContext* ctx = reinterpret_cast<EspeakContext*>(handle);
    if (ctx) {
        if (ctx->initialized) {
            espeak_Terminate();
        }
        delete ctx;
    }
}
