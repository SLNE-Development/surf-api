package dev.slne.surf.surfapi.core.api.util;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.google.common.base.Preconditions.*;

@ApiStatus.NonExtendable
public interface ItemStackFactory {

    static ItemStack of(@NotNull ItemType material, int amount, @NotNull Consumer<NBTCompound> nbtConsumer) {
        checkNotNull(material, "Material may not be null");
        checkNotNull(nbtConsumer, "NBT consumer may not be null");

        final NBTCompound nbt = new NBTCompound();
        nbtConsumer.accept(nbt);

        return ItemStack.builder().type(material).amount(amount).nbt(nbt).build();
    }

    static ItemStack of(@NotNull ItemType material, int amount) {
        return of(material, amount, __ -> {
        });
    }

    static ItemStack of(@NotNull ItemType material) {
        return of(material, 1);
    }
}
