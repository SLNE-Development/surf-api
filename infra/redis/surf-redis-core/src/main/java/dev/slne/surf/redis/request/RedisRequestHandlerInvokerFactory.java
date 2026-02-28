package dev.slne.surf.redis.request;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

@NullMarked
final class RedisRequestHandlerInvokerFactory {

    private static final byte[] TEMPLATE_CLASS_BYTES;
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static {
        try (final InputStream is = RedisRequestHandlerInvokerTemplate.class.getResourceAsStream(RedisRequestHandlerInvokerTemplate.class.getSimpleName() + ".class")) {
            Objects.requireNonNull(is, "RedisRequestHandlerInvokerTemplate.class not found");
            TEMPLATE_CLASS_BYTES = is.readAllBytes();
        } catch (IOException e) {
            throw new AssertionError("Failed to load RedisRequestHandlerInvokerTemplate.class", e);
        }
    }

    private RedisRequestHandlerInvokerFactory() {
        throw new UnsupportedOperationException("RedisRequestHandlerInvokerFactor is a utility class and cannot be instantiated");
    }

    public static RedisRequestHandlerInvoker create(final Object handler, final Method method, final Class<? extends RedisRequest> requestEventClass) {
        try {
            final Class<?> handlerClass = handler.getClass();
            final MethodHandles.Lookup privateLookupIn = MethodHandles.privateLookupIn(handlerClass, LOOKUP);
            final List<Object> classData = List.of(handler, requestEventClass, method, privateLookupIn);

            final MethodHandles.Lookup hiddenClassLookup = LOOKUP.defineHiddenClassWithClassData(TEMPLATE_CLASS_BYTES, classData, true);

            return hiddenClassLookup.lookupClass()
                    .asSubclass(RedisRequestHandlerInvoker.class)
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to create RedisRequestHandlerInvoker for " + method, e);
        }
    }

    record ClassData(Method method, MethodHandle methodHandle, Class<? extends RedisRequest> requestEventClass) {
    }

    static ClassData classData(final MethodHandles.Lookup lookup) {
        try {
            final Object handler = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, Object.class, 0);
            final Class<?> requestEventClass = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, Class.class, 1);
            final Method method = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, Method.class, 2);
            final MethodHandles.Lookup privateLookupIn = MethodHandles.classDataAt(lookup, ConstantDescs.DEFAULT_NAME, MethodHandles.Lookup.class, 3);

            final MethodHandle methodHandle = privateLookupIn.unreflect(method)
                    .bindTo(handler)
                    .asType(MethodType.methodType(void.class, RequestContext.class));

            return new ClassData(method, methodHandle, requestEventClass.asSubclass(RedisRequest.class));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to retrieve class data for RedisRequestHandlerInvoker", e);
        }
    }
}
