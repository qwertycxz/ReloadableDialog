package top.qwertycxz.loadabledialog.mixin;

import static java.util.concurrent.CompletableFuture.failedFuture;
import static net.minecraft.server.RegistryLayer.DIMENSIONS;
import static net.minecraft.server.packs.PackType.SERVER_DATA;
import static top.qwertycxz.loadabledialog.RegistryInfoLookupImpl.loadDialog;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MinecraftServer.class)
@NullMarked
public abstract class MinecraftServerMixin {
	@Final
	@Mutable
	@Shadow
	@SuppressWarnings("null")
	private LayeredRegistryAccess<RegistryLayer> registries;

	@ModifyArg(at = @At(target = "thenCompose", value = "INVOKE"), method = "reloadResources")
	private <T extends @NonNull List<PackResources>, U extends AutoCloseable> Function<T, CompletableFuture<U>> reloadDialog(Function<T, CompletableFuture<U>> function) {
		return resources -> {
			var old = registries;
			var manager = new MultiPackResourceManager(SERVER_DATA, resources);
			try {
				registries = old.replaceFrom(DIMENSIONS, loadDialog(old.getLayer(DIMENSIONS), old.getAccessForLoading(DIMENSIONS), manager));
				return function.apply(resources).whenComplete((ok, e) -> {
					if (e != null) {
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
