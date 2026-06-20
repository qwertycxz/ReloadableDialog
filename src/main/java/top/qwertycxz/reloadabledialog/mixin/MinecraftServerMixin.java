package top.qwertycxz.reloadabledialog.mixin;

import static java.util.concurrent.CompletableFuture.failedFuture;
import static net.minecraft.server.RegistryLayer.DIMENSIONS;
import static net.minecraft.server.packs.PackType.SERVER_DATA;
import static top.qwertycxz.reloadabledialog.RegistryInfoLookupImpl.STALE_PLAYERS;
import static top.qwertycxz.reloadabledialog.RegistryInfoLookupImpl.loadDialog;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.players.PlayerList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/// Mixin for dialog reloading
@Mixin(MinecraftServer.class)
@NullMarked
public abstract class MinecraftServerMixin {
	/// The player list, used for marking players as stale when reloading dialogs.
	@Shadow
	@SuppressWarnings("null")
	private PlayerList playerList;
	/// The layered registry access, used for replacing the dialog registry when reloading dialogs.
	@Final
	@Mutable
	@Shadow
	@SuppressWarnings("null")
	private LayeredRegistryAccess<RegistryLayer> registries;

	/// Wraps the callback of `reloadResources` to load dialogs from the resource manager and replace the dialog registry in the layered registry access.
	///
	/// Once failed, the registries will be reverted to the old one.
	///
	/// @param function the original callback that reloads resources
	/// @return the wrapped callback that loads dialogs and then calls the original callback
	@ModifyArg(at = @At(target = "thenCompose", value = "INVOKE"), method = "reloadResources")
	private <T extends @NonNull List<PackResources>, U extends AutoCloseable> Function<T, CompletableFuture<U>> reloadDialog(Function<T, CompletableFuture<U>> function) {
		return resources -> {
			var old = registries;
			var manager = new MultiPackResourceManager(SERVER_DATA, resources);
			try {
				registries = old.replaceFrom(DIMENSIONS, loadDialog(old.getLayer(DIMENSIONS), old.getAccessForLoading(DIMENSIONS), manager));
				return function.apply(resources).whenComplete((result, e) -> {
					if (e == null) {
						STALE_PLAYERS.addAll(playerList.getPlayers());
					}
					else {
						registries = old;
					}
				});
			}
			catch (Throwable e) {
				registries = old;
				manager.close();
				return failedFuture(e);
			}
		};
	}
}
