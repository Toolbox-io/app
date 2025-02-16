// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("ultimatesecurity");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("ultimatesecurity")
//      }
//    }

#include <jni.h>
#include <__fwd/string.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL Java_ru_morozovit_ultimatesecurity_Test_error(JNIEnv* env, jobject o) {
    std::wstring retval {L"Hello World!"};
    return env->NewString(
            (const jchar*)retval.c_str(),
            (jsize)retval.length()
    );
}