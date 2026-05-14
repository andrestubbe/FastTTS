#include "fasttts.h"
#include <windows.h>
#include <sapi.h>
#include <vector>

/**
 * Native implementation of Windows TTS using SAPI (Raw COM, no ATL).
 */

// Helper to convert jstring to std::wstring
std::wstring jstringToWString(JNIEnv* env, jstring jstr) {
    if (!jstr) return L"";
    const jchar* chars = env->GetStringChars(jstr, nullptr);
    std::wstring wstr((const wchar_t*)chars, env->GetStringLength(jstr));
    env->ReleaseStringChars(jstr, chars);
    return wstr;
}

JNIEXPORT void JNICALL Java_fasttts_backends_windows_WindowsTTSBackend_streamNative(JNIEnv* env, jobject obj, jstring text, jstring voiceId, jfloat rate, jfloat pitch, jfloat volume, jobject chunkConsumer);

JNIEXPORT jobject JNICALL Java_fasttts_backends_windows_WindowsTTSBackend_getVoicesNative(JNIEnv* env, jobject obj);

JNIEXPORT jbyteArray JNICALL Java_fasttts_backends_windows_WindowsTTSBackend_synthesizeNative(JNIEnv* env, jobject obj, jstring text, jstring voiceId, jfloat rate, jfloat pitch, jfloat volume) {
    std::wstring wtext = jstringToWString(env, text);
    ISpVoice* pVoice = NULL;
    ISpStream* pStream = NULL;
    IStream* pBaseStream = NULL;
    HRESULT hr = CoInitialize(NULL);
    
    hr = CoCreateInstance(CLSID_SpVoice, NULL, CLSCTX_ALL, IID_ISpVoice, (void**)&pVoice);
    if (FAILED(hr)) return nullptr;

    hr = CreateStreamOnHGlobal(NULL, TRUE, &pBaseStream);
    if (FAILED(hr)) { pVoice->Release(); return nullptr; }

    WAVEFORMATEX wfx = { WAVE_FORMAT_PCM, 1, 44100, 88200, 2, 16, 0 };
    
    hr = CoCreateInstance(CLSID_SpStream, NULL, CLSCTX_ALL, IID_ISpStream, (void**)&pStream);
    if (SUCCEEDED(hr)) {
        hr = pStream->SetBaseStream(pBaseStream, SPDFID_WaveFormatEx, &wfx);
        hr = pVoice->SetOutput(pStream, TRUE);
        
        pVoice->SetRate((long)((rate - 1.0f) * 10.0f));
        pVoice->SetVolume((unsigned short)(volume * 100.0f));
        
        hr = pVoice->Speak(wtext.c_str(), SPF_DEFAULT, NULL);
        if (SUCCEEDED(hr)) {
            pVoice->WaitUntilDone(INFINITE);

            STATSTG stat;
            pBaseStream->Stat(&stat, STATFLAG_NONAME);
            ULONG pcmSize = (ULONG)stat.cbSize.QuadPart;
            ULONG wavSize = pcmSize + 44;
            
            if (pcmSize > 0) {
                jbyteArray result = env->NewByteArray(wavSize);
                
                // Write WAV Header
                unsigned char header[44];
                memcpy(header, "RIFF", 4);
                unsigned int totalSize = wavSize - 8;
                memcpy(header + 4, &totalSize, 4);
                memcpy(header + 8, "WAVE", 4);
                memcpy(header + 12, "fmt ", 4);
                unsigned int fmtLen = 16;
                memcpy(header + 16, &fmtLen, 4);
                unsigned short format = 1; // PCM
                memcpy(header + 20, &format, 2);
                unsigned short channels = 1;
                memcpy(header + 22, &channels, 2);
                unsigned int sampleRate = 44100;
                memcpy(header + 24, &sampleRate, 4);
                unsigned int byteRate = 44100 * 2;
                memcpy(header + 28, &byteRate, 4);
                unsigned short blockAlign = 2;
                memcpy(header + 32, &blockAlign, 2);
                unsigned short bits = 16;
                memcpy(header + 34, &bits, 2);
                memcpy(header + 36, "data", 4);
                memcpy(header + 40, &pcmSize, 4);

                env->SetByteArrayRegion(result, 0, 44, (jbyte*)header);

                HGLOBAL hGlobal;
                if (SUCCEEDED(GetHGlobalFromStream(pBaseStream, &hGlobal))) {
                    void* pData = GlobalLock(hGlobal);
                    if (pData) {
                        env->SetByteArrayRegion(result, 44, pcmSize, (jbyte*)pData);
                        GlobalUnlock(hGlobal);
                        pStream->Release(); pBaseStream->Release(); pVoice->Release();
                        CoUninitialize();
                        return result;
                    }
                }
            }
        }
    }

    if (pStream) pStream->Release();
    if (pBaseStream) pBaseStream->Release();
    if (pVoice) pVoice->Release();
    CoUninitialize();
    return nullptr;
}

JNIEXPORT void JNICALL Java_fasttts_backends_windows_WindowsTTSBackend_streamNative(JNIEnv* env, jobject obj, jstring text, jstring voiceId, jfloat rate, jfloat pitch, jfloat volume, jobject chunkConsumer) {
    jbyteArray full = Java_fasttts_backends_windows_WindowsTTSBackend_synthesizeNative(env, obj, text, voiceId, rate, pitch, volume);
    if (!full) return;

    jclass consumerClass = env->GetObjectClass(chunkConsumer);
    jmethodID acceptMethod = env->GetMethodID(consumerClass, "accept", "(Ljava/lang/Object;)V");
    
    jsize len = env->GetArrayLength(full);
    jsize chunkSize = 4096;
    
    jbyte* body = env->GetByteArrayElements(full, NULL);
    for (jsize i = 0; i < len; i += chunkSize) {
        jsize currentSize = (i + chunkSize > len) ? len - i : chunkSize;
        jbyteArray chunk = env->NewByteArray(currentSize);
        env->SetByteArrayRegion(chunk, 0, currentSize, body + i);
        env->CallVoidMethod(chunkConsumer, acceptMethod, chunk);
        env->DeleteLocalRef(chunk);
    }
    env->ReleaseByteArrayElements(full, body, JNI_ABORT);
    env->DeleteLocalRef(full);
}

JNIEXPORT jobject JNICALL Java_fasttts_backends_windows_WindowsTTSBackend_getVoicesNative(JNIEnv* env, jobject obj) {
    jclass listClass = env->FindClass("java/util/ArrayList");
    jmethodID listInit = env->GetMethodID(listClass, "<init>", "()V");
    jmethodID listAdd = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");
    jobject list = env->NewObject(listClass, listInit);

    jclass voiceClass = env->FindClass("fasttts/core/FastTTSVoice");
    jmethodID voiceInit = env->GetMethodID(voiceClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

    ISpObjectTokenCategory* pCategory = NULL;
    IEnumSpObjectTokens* pEnum = NULL;
    
    HRESULT hr = CoInitialize(NULL);
    hr = CoCreateInstance(CLSID_SpObjectTokenCategory, NULL, CLSCTX_ALL, IID_ISpObjectTokenCategory, (void**)&pCategory);
    
    if (SUCCEEDED(hr)) {
        hr = pCategory->SetId(SPCAT_VOICES, FALSE);
        if (SUCCEEDED(hr)) {
            hr = pCategory->EnumTokens(NULL, NULL, &pEnum);
            if (SUCCEEDED(hr)) {
                ISpObjectToken* pToken = NULL;
                while (pEnum->Next(1, &pToken, NULL) == S_OK) {
                    LPWSTR pDescription = NULL;
                    LPWSTR pId = NULL;
                    
                    pToken->GetStringValue(NULL, &pDescription);
                    pToken->GetId(&pId);
                    
                    jstring id = env->NewString((const jchar*)pId, (jsize)wcslen(pId));
                    jstring name = env->NewString((const jchar*)pDescription, (jsize)wcslen(pDescription));
                    jstring lang = env->NewStringUTF("unknown");
                    jstring gender = env->NewStringUTF("unknown");
                    jstring backendId = env->NewStringUTF("windows");
                    
                    jobject voiceObj = env->NewObject(voiceClass, voiceInit, id, name, lang, gender, backendId);
                    env->CallBooleanMethod(list, listAdd, voiceObj);
                    
                    if (pDescription) CoTaskMemFree(pDescription);
                    if (pId) CoTaskMemFree(pId);
                    pToken->Release();
                }
                pEnum->Release();
            }
        }
        pCategory->Release();
    }

    CoUninitialize();
    return list;
}


