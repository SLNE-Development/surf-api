package dev.slne.surf.redis.util;

import org.jspecify.annotations.NullMarked;

import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.List;

@NullMarked
public final class RedisHiddenInvokerUtil {

    private RedisHiddenInvokerUtil() {
        throw new UnsupportedOperationException();
    }

    public static <I> I createInvoker(MethodHandles.Lookup lookup, byte[] templateBytes, Class<I> invokerInterface, Object target, Method method, Class<?> payloadClass) throws ReflectiveOperationException {
        final MethodHandles.Lookup privateLookupIn = MethodHandles.privateLookupIn(target.getClass(), lookup);
        final List<Object> classData = List.of(target, payloadClass, method, privateLookupIn);
        final MethodHandles.Lookup hiddenClassLookup = lookup.defineHiddenClassWithClassData(templateBytes, classData, true);

        return hiddenClassLookup.lookupClass()
                .asSubclass(invokerInterface)
                .getDeclaredConstructor()
                .newInstance();
    }

    public record ClassData<P>(Method method, MethodHandle methodHandle, Class<? extends P> payloadClass) {
    }

    public static <P> ClassData<P> loadClassData(MethodHandles.Lookup lookup, MethodType methodType, Class<P> payloadSuperType) throws ReflectiveOperationException {
        final Object target = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, Object.class, 0);
        final Class<?> payload = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, Class.class, 1);
        final Method method = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, Method.class, 2);
        final MethodHandles.Lookup privateLookupIn = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, MethodHandles.Lookup.class, 3);

        final MethodHandle handle = privateLookupIn.unreflect(method).bindTo(target).asType(methodType);
        return new ClassData<P>(method, handle, payload.asSubclass(payloadSuperType));
    }
}
