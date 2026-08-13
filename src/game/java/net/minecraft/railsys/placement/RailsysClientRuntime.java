package net.minecraft.railsys.placement;

import net.minecraft.client.Minecraft;

/**
 * RailsysClientRuntime — Phase 1-R10 normal client runtime ownership.
 *
 * CP-R10-03 owns the SAFE normal-world client initialization on the CLIENT
 * thread. It is called by Minecraft.runTick BEFORE any validation hook. It
 * only performs idempotent, normal-world R10 initialization — at minimum
 * loading the embedded prototype ModelPack exactly once via
 * RailAssetRegistry.ensurePrototypePackLoaded(). It has no world-name gate and
 * never mutates placement state; proof drivers (MarkerCantClientHook /
 * MarkerPlaceClientHook) remain validation-only and keep their own explicit
 * world gates.
 */
public final class RailsysClientRuntime {

	private RailsysClientRuntime() {
	}

	public static void onClientTick(Minecraft mc) {
		if (mc == null) {
			return;
		}
		net.minecraft.railsys.render.RailAssetRegistry.ensurePrototypePackLoaded();
		// Phase 1-R15: ModelPack asset registry (fallback + imported assets).
		net.minecraft.railsys.render.RailsysModelPackClient.ensureInitialized();
		// Phase 1-R15: deferred asset selector open (Shift+Right-click / command).
		net.minecraft.railsys.placement.RailsysAssetSelector.onClientTick(mc);
	}
}
