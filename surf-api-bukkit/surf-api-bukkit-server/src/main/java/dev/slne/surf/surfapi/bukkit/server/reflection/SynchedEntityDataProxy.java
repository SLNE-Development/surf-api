package dev.slne.surf.surfapi.bukkit.server.reflection;

import java.util.List;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;

@Proxies(SynchedEntityData.class)
public interface SynchedEntityDataProxy {

  @MethodName("packAll")
  List<DataValue<?>> packAll(SynchedEntityData entityData);
}
