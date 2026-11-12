package net.minestom.server.entity;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class AutomaticPlayerVisibility {

  private AutomaticPlayerVisibility() {
    throw new AssertionError();
  }

  public static void withViewerLock(Player viewer, Runnable action) {
    synchronized (viewer.viewEngine) {
      action.run();
    }
  }

  public static boolean targetAllows(Player target, Player viewer) {
    return target.isAutoViewable() && target.viewEngine.viewableOption.predicate(viewer);
  }

  public static void show(Player target, Player viewer) {
    if (!viewer.autoViewEntities()
        || !target.isActive()
        || target.isRemoved()
        || target.getInstance() != viewer.getInstance()
        || !targetAllows(target, viewer)
        || target.isViewer(viewer)) {
      return;
    }

    requireNonNull(target.viewEngine.viewableOption.addition, "addition").accept(viewer);

    Entity vehicle = target.getVehicle();
    if (vehicle != null
        && vehicle.isViewer(viewer)
        && target.isViewer(viewer)) {
      viewer.sendPacket(vehicle.getPassengersPacket());
    }
  }

  public static void hide(Player target, Player viewer) {
    if (!target.isViewer(viewer)) {
      return;
    }

    boolean[] manual = {false};

    target.viewEngine.forManuals(player -> {
      if (player == viewer) {
        manual[0] = true;
      }
    });

    if (manual[0]) {
      return;
    }

    requireNonNull(target.viewEngine.viewableOption.removal, "removal").accept(viewer);
  }
}
