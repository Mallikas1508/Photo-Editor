#include <jni.h>

JNIEXPORT void JNICALL
Java_com_mallika_photoeditor_MainActivity_blackAndWhite(JNIEnv *env, jclass clazz, jintArray pixels_, jint width, jint height) {

    jint *pixels = (*env)-> GetIntArrayElements(env, pixels_, NULL);

    unsigned char *colors = (char *) pixels;
    //char *colors = (char *) pixels;  //For b/w

    int pixelCount = width * height * 4;
    int i=0;
    while (i<pixelCount) {
        unsigned char average = (colors[i] + colors[i + 1] + colors[i + 2]) / 3;
        //colors[i] = average;    //blue
        //colors[i+1] = average;  //green
        if (colors[i + 2] < 250)
        {
            colors[i + 2] += 3;//average;  //red
        }
        i+=4;
    }

    (*env)-> ReleaseIntArrayElements(env, pixels_, pixels, 0);
}