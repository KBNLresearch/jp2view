/**
 * Copyright (c) 2013, Koninklijke Bibliotheek - Nationale bibliotheek van Nederland
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *	* Redistributions of source code must retain the above copyright notice, this
 *	 list of conditions and the following disclaimer.
 *	* Redistributions in binary form must reproduce the above copyright notice,
 *	 this list of conditions and the following disclaimer in the documentation
 *	 and/or other materials provided with the distribution.
 *	* Neither the name of the Koninklijke Bibliotheek nor the names of its contributors
 *	 may be used to endorse or promote products derived from this software without
 *	 specific prior written permission.
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
#include <stdio.h>
#include <openjpeg.h>
#include "nl_kb_jp2_JP2Reader.h"

static void error_callback(const char *msg, void *client_data) {(void)client_data; fprintf(stdout, "[ERROR] %s\r\n", msg);}
static void warning_callback(const char *msg, void *client_data) { (void)client_data; fprintf(stdout, "[WARNING] %s\r\n", msg);}
static void info_callback(const char *msg, void *client_data) {(void)client_data; fprintf(stdout, "[INFO] %s\r\n", msg);}

struct opj_res {
	int status;
	opj_stream_t *l_stream;
	opj_codec_t *l_codec;
	opj_image_t *image;
	FILE * open_file;
};

struct opj_res opj_init(const char *fname, opj_dparameters_t *parameters) {

	struct opj_res resources;
	resources.status = 0;
	resources.image = NULL;
	FILE *fptr = fopen(fname, "rb");
	resources.open_file = fptr;
	resources.l_stream = opj_stream_create_default_file_stream(fptr,1);
	resources.l_codec = opj_create_decompress(OPJ_CODEC_JP2);
	if(!resources.l_stream) { resources.status = 1; }
	if(!opj_setup_decoder(resources.l_codec, parameters)) {
		opj_stream_destroy(resources.l_stream);
		opj_destroy_codec(resources.l_codec);
		resources.status = 2;
	}

	if(!opj_read_header(resources.l_stream, resources.l_codec, &(resources.image))) {
		opj_stream_destroy(resources.l_stream);
		opj_destroy_codec(resources.l_codec);
		opj_image_destroy(resources.image);
		resources.status = 3;
	}

/*	opj_set_info_handler(resources.l_codec, info_callback,00);
	opj_set_warning_handler(resources.l_codec, warning_callback,00);
	opj_set_error_handler(resources.l_codec, error_callback,00);*/
	return resources;
}

void opj_cleanup(struct opj_res *resources) {
	if(resources->l_stream) { opj_stream_destroy(resources->l_stream); }
	if(resources->l_codec) { opj_destroy_codec(resources->l_codec); }
	if(resources->image) { opj_image_destroy(resources->image); }
	if(resources->open_file) { fclose(resources->open_file); }
}



#define JP2_RFC3745_MAGIC "\x00\x00\x00\x0c\x6a\x50\x20\x20\x0d\x0a\x87\x0a"
#define JP2_MAGIC "\x0d\x0a\x87\x0a"
static int is_jp2(FILE *fptr) {
	unsigned char buf[12];
	unsigned int l_nb_read;

	l_nb_read = fread(buf, 1, 12, fptr);
	fseek(fptr, 0, SEEK_SET);

	int retval = memcmp(buf, JP2_RFC3745_MAGIC, 12) == 0 || memcmp(buf, JP2_MAGIC, 4) == 0;
	fclose(fptr);
	return retval;
}

#define READ_FAILURE 0
#define READ_SUCCESS 1


JNIEXPORT jintArray JNICALL Java_nl_kb_jp2_JP2Reader_getTile
	(JNIEnv *env, jclass cls, jstring fname, jint tile_index, jint reduction_factor, jobjectArray pixels) {
	const char *filename = (*env)->GetStringUTFChars(env, fname, 0);
	jclass intArrayClass = (*env)->FindClass(env, "[I");
	jintArray ary = (*env)->NewIntArray(env, 3);

	int data[3];
	int i = 0;
	for(i = 0; i < 3; ++i) { data[i] = 0; }
	data[0] = READ_FAILURE;

	FILE *fptr = fopen(filename, "rb");
	if(fptr != NULL && is_jp2(fptr)) {
		opj_dparameters_t parameters;
		opj_set_default_decoder_parameters(&parameters);
		parameters.cp_reduce = reduction_factor;
		parameters.cp_layer = 100;
		struct opj_res resources = opj_init(filename, &parameters);
		if(resources.status == 0 && opj_get_decoded_tile(resources.l_codec, resources.l_stream, resources.image, tile_index)) {
			int numpix = resources.image->comps[0].w * resources.image->comps[0].h;
			int comp;
			for(comp = 0; comp < resources.image->numcomps; ++comp) {
				jintArray data = (*env)->NewIntArray(env, numpix);
				(*env)->SetIntArrayRegion(env, data, (jsize) 0, (jsize) numpix, (jint*) resources.image->comps[comp].data);
				(*env)->SetObjectArrayElement(env, pixels, (jsize) comp, data);
			}
			data[0] = READ_SUCCESS;
			data[1] = resources.image->comps[0].w;
			data[2] = resources.image->comps[0].h;
		}
		opj_cleanup(&resources);
	}

	(*env)->SetIntArrayRegion(env, ary, 0, 3, data);
	return ary;
}



#define FIELD_LEN 9
JNIEXPORT jintArray JNICALL Java_nl_kb_jp2_JP2Reader_getJp2Specs
	(JNIEnv *env, jclass cls, jstring fname) {

	const char *filename = (*env)->GetStringUTFChars(env, fname, 0);
	jintArray ary = (*env)->NewIntArray(env, FIELD_LEN);

	int data[FIELD_LEN];
	int i = 0;
	for(i = 0; i < FIELD_LEN; ++i) { data[i] = 0; }
	data[0] = READ_FAILURE;

	FILE *fptr = fopen(filename, "rb");
	if(fptr != NULL && is_jp2(fptr)) {
		opj_dparameters_t parameters;
		opj_set_default_decoder_parameters(&parameters);
		struct opj_res resources = opj_init(filename, &parameters);

		opj_codestream_info_v2_t* info = opj_get_cstr_info(resources.l_codec);
		if(resources.status == 0) {
			data[0] = READ_SUCCESS;
			data[1] = resources.image->x1;
			data[2] = resources.image->y1;
			data[3] = info->tw;
			data[4] = info->th;
			data[5] = info->tdx;
			data[6] = info->tdy;
			data[7] = info->m_default_tile_info.tccp_info[0].numresolutions;
			data[8] = resources.image->numcomps;
		}
		opj_destroy_cstr_info(&info);
		opj_cleanup(&resources);
	} else {
		error_callback("Cannot read file:", NULL);
		error_callback(filename, NULL);
	}
	(*env)->SetIntArrayRegion(env, ary, 0, FIELD_LEN, data);
	return ary;
}
