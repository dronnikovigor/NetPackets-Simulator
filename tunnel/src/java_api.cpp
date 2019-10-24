#include "java_api.h"

#include <assert.h>
#include <bits/stdint-uintn.h>
#include <jni_md.h>
#include <stddef.h>
#include <cstdio>
#include <string>

#include "tunnel.h"

class Env
{
public:
	JNIEnv &base;
public:
	Env(JNIEnv &env) :
			base(env)
	{
	}
	Env(JNIEnv *env) :
			Env(*env)
	{
	}

	jobject get_object(jobject obj, const char *field, const char *className)
	{
		return base.GetObjectField(obj,
				base.GetFieldID(base.GetObjectClass(obj), field, className));
	}

	jfieldID find_field(jobject obj, const char *field, const char *type) const
	{
		return base.GetFieldID(base.GetObjectClass(obj), field, type);
	}

	int get_int(jobject obj, const char *field) const
	{
		return base.GetIntField(obj, find_field(obj, field, "I"));
	}

	jlong get_long(jobject obj, const char *field) const
	{
		return base.GetLongField(obj, find_field(obj, field, "J"));
	}

	void set_long(jobject obj, const char *field, jlong value) const
	{
		base.SetLongField(obj, find_field(obj, field, "J"), value);
	}

	int get_int(jobject obj, jfieldID fld)
	{
		return base.GetIntField(obj, fld);
	}

	bool get_bool(jobject obj, const char *field)
	{
		return base.GetBooleanField(obj, find_field(obj, field, "Z"));
	}

	jobject alloc_obj(jclass clz)
	{
		return base.AllocObject(clz);
	}

	// Has to be closed!
	jclass open_class(const char *path)
	{
		jclass clz = base.FindClass(path);
		assert(clz && path);
		clz = (jclass) base.NewGlobalRef(clz);
		assert(clz && path);
		return clz;
	}

	void close_class(jclass clz)
	{
		base.DeleteGlobalRef(clz);
	}

	jfieldID get_class_field(const char *class_path, const char *field,
			const char *type)
	{
		jclass clz = base.FindClass(class_path);
		assert(clz);
		jfieldID fld = base.GetFieldID(clz, field, type);
		assert(fld);
		return fld;
	}
};

struct
{

	jfieldID fld_ref;

	void init(JNIEnv &jni_env)
	{
		jclass clz = jni_env.FindClass("one/transport/ut2/testing/tunnel/jni/JNIObject");
		assert(clz && "class JNIObject");
		fld_ref = jni_env.GetFieldID(clz, "objectRef", "J");
		assert(fld_ref && "field JNIObject.objectRef");
	}
} j_ref;

class JEnv: public Env
{
public:
	JEnv(JNIEnv *env) :
			Env(env)
	{
	}

	void set_ref(jobject jobj, void *ptr) const
	{
		base.SetLongField(jobj, j_ref.fld_ref, (jlong) ptr);
	}

	template<typename T>
	void get_ref(jobject jobj, T *&ptr) const
	{
		if (!jobj)
			ptr = 0;
		else
			ptr = (T*) base.GetLongField(jobj, j_ref.fld_ref);
	}

	template<typename T>
	jintArray toJInts(const T &obj)
	{
		static_assert(!(sizeof(obj) % sizeof(jint)), "");
		constexpr uint32_t len = sizeof(obj) / sizeof(jint);
		jintArray arr = base.NewIntArray(len);
		base.SetIntArrayRegion(arr, 0, len, (jint*) &obj);
		return arr;
	}

//	JClass Class(const char *name)
//	{
//		jclass clz = base.FindClass(name);
//		assert(clz);
//		return JClass
//		{ *this, clz };
//	}
//
//	JObject Object(jobject obj)
//	{
//		assert(obj);
//		return JObject
//		{ *this, obj };
//	}

//	jbyteArray toJBytes(const_pvoid ptr, uint32_t len)
//	{
//		jbyteArray out = base.NewByteArray(len);
//		base.SetByteArrayRegion(out, 0, len,
//				reinterpret_cast<const jbyte*>(ptr));
//		return out;
//	}
};

std::string jstring2string(JNIEnv *jni_env, jstring j_string)
{
	//converting java string to cpp string
	const char *utf_chars = jni_env->GetStringUTFChars(j_string, NULL);
	size_t length = (size_t) jni_env->GetStringLength(j_string);
	std::string str = std::string(utf_chars, length);
	jni_env->ReleaseStringUTFChars(j_string, utf_chars);
	return str;
}

#ifdef __cplusplus
extern "C"
{
#endif

JNIEXPORT jboolean JNICALL Java_one_transport_ut2_testing_tunnel_jni_Tunnel_init(
		JNIEnv *jni_env, jobject objTunnel, jstring j_interface_name)
{
	JEnv env(jni_env);

	Tunnel *tunnel = new Tunnel();

	//initializing tunnel
	std::string interface_name = jstring2string(jni_env, j_interface_name);

	bool succ = tunnel->init(interface_name);
	if (succ)
	{
		//saving reference
		env.set_ref(objTunnel, tunnel);
	}
	else
	{
		//clear resources
		delete tunnel;
	}

	return succ;
}

JNIEXPORT void JNICALL Java_one_transport_ut2_testing_tunnel_jni_Tunnel_stop(JNIEnv *jni_env,
		jobject objTunnel)
{
	JEnv env(jni_env);

	Tunnel *tunnel;
	env.get_ref(objTunnel, tunnel);
	assert(tunnel);

	delete tunnel;
	env.set_ref(objTunnel, 0);
}

JNIEXPORT jbyteArray JNICALL Java_one_transport_ut2_testing_tunnel_jni_Tunnel_readPacket(
		JNIEnv *jni_env, jobject objTunnel, jint timeout)
{
	JEnv env(jni_env);

	Tunnel *tunnel;
	env.get_ref(objTunnel, tunnel);
	assert(tunnel);

	char *packet_bytes = 0;
	int len = tunnel->readPacket(timeout, packet_bytes);

	if (len > 0)
	{
		jbyteArray jbytes = jni_env->NewByteArray(len);
		env.base.SetByteArrayRegion(jbytes, 0, len, (jbyte*) packet_bytes);

		return jbytes;
	}

	return 0;
}

JNIEXPORT void JNICALL Java_one_transport_ut2_testing_tunnel_jni_Tunnel_writePacket(
		JNIEnv *jni_env, jobject objTunnel, jbyteArray jbytes)
{
	JEnv env(jni_env);

	Tunnel *tunnel;
	env.get_ref(objTunnel, tunnel);
	assert(tunnel);

	assert(jbytes);
	uint32_t len = env.base.GetArrayLength(jbytes);

	signed char buf[len];
	env.base.GetByteArrayRegion(jbytes, 0, len, buf);

	tunnel->writePacket((char *) buf, len);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void*)
{
	JNIEnv *env;
	if (vm->GetEnv((void**) &env, JNI_VERSION_1_8) != JNI_OK)
	{
		return -1;
	}
	JEnv jenv(env);

	j_ref.init(jenv.base);

	return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void*)
{
	JNIEnv *env;
	if (vm->GetEnv((void**) &env, JNI_VERSION_1_8) != JNI_OK)
	{
		return;
	}
	JEnv jenv(env);
}

#ifdef __cplusplus
}
#endif
;
