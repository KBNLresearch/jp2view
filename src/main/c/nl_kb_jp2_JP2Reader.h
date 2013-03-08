/**
 * Copyright (c) 2013, Koninklijke Bibliotheek - Nationale bibliotheek van Nederland
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Koninklijke Bibliotheek nor the names of its contributors
 *     may be used to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

#include <jni.h>

#ifndef _Included_nl_kb_jp2_JP2Reader
#define _Included_nl_kb_jp2_JP2Reader
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     nl_kb_jp2_JP2Reader
 * Method:    getJp2Specs
 * Signature: (Ljava/lang/String;)[I
 */
JNIEXPORT jintArray JNICALL Java_nl_kb_jp2_JP2Reader_getJp2Specs
  (JNIEnv *, jclass, jstring);

/*
 * Class:     nl_kb_jp2_JP2Reader
 * Method:    getJp2Specs
 * Signature: (Ljava/lang/String;)[I
 */
JNIEXPORT jintArray JNICALL JNICALL Java_nl_kb_jp2_JP2Reader_getTile
  (JNIEnv *, jclass, jstring, jint, jint, jobjectArray);


#ifdef __cplusplus
}
#endif
#endif
