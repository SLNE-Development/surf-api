package dev.slne.surf.api.paper.server.nms.v1_21_11.reflection;

import io.papermc.paper.adventure.ChatProcessor;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.network.chat.LastSeenMessagesValidator;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.FutureChain;
import org.jspecify.annotations.NullMarked;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.BitSet;

@NullMarked
public final class NmsReflections {
    private NmsReflections() {
        throw new UnsupportedOperationException();
    }

    private static final VarHandle serverGamePacketListenerImpl$chatMessageChain;
    private static final VarHandle serverGamePacketListenerImpl$nextChatIndex;
    private static final VarHandle serverGamePacketListenerImpl$messageSignatureCache;
    private static final VarHandle serverGamePacketListenerImpl$lastSeenMessages;
    private static final VarHandle chatProcessor$PAPER_RAW;

    private static final MethodHandle filterMask$mask;
    private static final MethodHandle filterMask$constructorBitSet;

    public static FutureChain getChatMessageChain(ServerGamePacketListenerImpl instance) {
        return (FutureChain) serverGamePacketListenerImpl$chatMessageChain.get(instance);
    }

    public static int increaseAndGetNextChatIndex(ServerGamePacketListenerImpl instance) {
        return (int) serverGamePacketListenerImpl$nextChatIndex.getAndAdd(instance, 1) + 1;
    }

    public static MessageSignatureCache getMessageSignatureCache(ServerGamePacketListenerImpl instance) {
        return (MessageSignatureCache) serverGamePacketListenerImpl$messageSignatureCache.get(instance);
    }

    public static LastSeenMessagesValidator getLastSeenMessages(ServerGamePacketListenerImpl instance) {
        return (LastSeenMessagesValidator) serverGamePacketListenerImpl$lastSeenMessages.get(instance);
    }

    public static FilterMask createFilterMask(BitSet bitSet) throws Throwable {
        return (FilterMask) filterMask$constructorBitSet.invokeExact(bitSet);
    }

    public static BitSet getMask(FilterMask filterMask) throws Throwable {
        return (BitSet) filterMask$mask.invokeExact(filterMask);
    }

    @SuppressWarnings("unchecked")
    public static ResourceKey<ChatType> getPaperRaw() {
        return (ResourceKey<ChatType>) chatProcessor$PAPER_RAW.get();
    }

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            MethodHandles.Lookup privateLookupInServerGamePacketListener = MethodHandles.privateLookupIn(ServerGamePacketListenerImpl.class, lookup);
            MethodHandles.Lookup privateLookupInFilterMask = MethodHandles.privateLookupIn(FilterMask.class, lookup);
            MethodHandles.Lookup privateLookupInChatProcessor = MethodHandles.privateLookupIn(ChatProcessor.class, lookup);

            serverGamePacketListenerImpl$chatMessageChain = privateLookupInServerGamePacketListener.findVarHandle(
                    ServerGamePacketListenerImpl.class,
                    "chatMessageChain",
                    FutureChain.class
            );

            serverGamePacketListenerImpl$nextChatIndex = privateLookupInServerGamePacketListener.findVarHandle(
                    ServerGamePacketListenerImpl.class,
                    "nextChatIndex",
                    int.class
            );

            serverGamePacketListenerImpl$messageSignatureCache = privateLookupInServerGamePacketListener.findVarHandle(
                    ServerGamePacketListenerImpl.class,
                    "messageSignatureCache",
                    MessageSignatureCache.class
            );

            serverGamePacketListenerImpl$lastSeenMessages = privateLookupInServerGamePacketListener.findVarHandle(
                    ServerGamePacketListenerImpl.class,
                    "lastSeenMessages",
                    LastSeenMessagesValidator.class
            );

            filterMask$mask = privateLookupInFilterMask.findVirtual(
                    FilterMask.class,
                    "mask",
                    MethodType.methodType(BitSet.class)
            );

            filterMask$constructorBitSet = privateLookupInFilterMask.findConstructor(
                    FilterMask.class,
                    MethodType.methodType(void.class, BitSet.class)
            );

            chatProcessor$PAPER_RAW = privateLookupInChatProcessor.findStaticVarHandle(
                    ChatProcessor.class,
                    "PAPER_RAW",
                    ResourceKey.class
            );
        } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
