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

JNIEXPORT void JNICALL Java_fasttts_WindowsTTSBackend_streamNative(JNIEnv* env, jobject obj, jstring text, jstring voiceId, jfloat rate, jfloat pitch, jfloat volume, jobject chunkConsumer);

JNIEXPORT jobject JNICALL Java_fasttts_WindowsTTSBackend_getVoicesNative(JNIEnv* env, jobject obj);

JNIEXPORT jbyteArray JNICALL Java_fasttts_WindowsTTSBackend_synthesizeNative(JNIEnv* env, jobject obj, jstring text, jstring voiceId, jfloat rate, jfloat pitch, jfloat volume) {
    std::wstring wtext = jstringToWString(env, text);
    ISpVoice* pVoice = NULL;
    ISpStream* pStream = NULL;
    IStream* pBaseStream = NULL;
    HRESULT hr = CoInitialize(NULL);
    hr = CoCreateInstance(CLSID_SpVoice, NULL, CLSCTX_ALL, IID_ISpVoice, (void**)&pVoice);
    if (FAILED(hr)) return nullptr;

    hr = CreateStreamOnHGlobal(NULL, TRUE, &pBaseStream);
    WAVEFORMATEX wfx = { WAVE_FORMAT_PCM, 1, 44100, 88200, 2, 16, 0 };

    hr = CoCreateInstance(CLSID_SpStream, NULL, CLSCTX_ALL, IID_ISpStream, (void**)&pStream);
    hr = pStream->SetBaseStream(pBaseStream, SPDFID_WaveFormatEx, &wfx);
    hr = pVoice->SetOutput(pStream, TRUE);
    pVoice->SetRate((long)((rate - 1.0f) * 10.0f));
    pVoice->Speak(wtext.c_str(), SPF_DEFAULT, NULL);

    STATSTG stat;
    pBaseStream->Stat(&stat, STATFLAG_NONAME);
    ULONG size = (ULONG)stat.cbSize.QuadPart;
    jbyteArray result = env->NewByteArray(size);
    HGLOBAL hGlobal;
    GetHGlobalFromStream(pBaseStream, &hGlobal);
    void* pData = GlobalLock(hGlobal);
    env->SetByteArrayRegion(result, 0, size, (jbyte*)pData);
    GlobalUnlock(hGlobal);

    pStream->Release(); pBaseStream->Release(); pVoice->Release();
    CoUninitialize();
    return result;
}

JNIEXPORT void JNICALL Java_fasttts_WindowsTTSBackend_streamNative(JNIEnv* env, jobject obj, jstring text, jstring voiceId, jfloat rate, jfloat pitch, jfloat volume, jobject chunkConsumer) {
    jbyteArray full = Java_fasttts_WindowsTTSBackend_synthesizeNative(env, obj, text, voiceId, rate, pitch, volume);
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



JNIEXPORT jobject JNICALL Java_fasttts_WindowsTTSBackend_getVoicesNative(JNIEnv* env, jobject obj) {
    jclass listClass = env->FindClass("java/util/ArrayList");
    jmethodID listInit = env->GetMethodID(listClass, "<init>", "()V");
    jmethodID listAdd = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");
    jobject list = env->NewObject(listClass, listInit);

    jclass voiceClass = env->FindClass("fasttts/FastTTSVoice");
    jmethodID voiceInit = env->GetMethodID(voiceClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

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
                    
                    jobject voiceObj = env->NewObject(voiceClass, voiceInit, id, name, lang, gender);
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


