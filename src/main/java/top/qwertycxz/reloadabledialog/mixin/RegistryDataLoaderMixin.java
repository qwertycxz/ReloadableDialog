package top.qwertycxz.reloadabledialog.mixin;

import static java.util.Objects.requireNonNull;
import static net.minecraft.core.registries.Registries.DIALOG;

import java.util.List;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryDataLoader.RegistryData;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// Mixin to remove the dialog registry from the vanilla registry data loader, since we want to load dialogs from our own data pack instead of the built-in one.
@Mixin(RegistryDataLoader.class)
@NullMarked
public abstract class RegistryDataLoaderMixin {
	/// Where the dialog registry used to be
	@Final
	@Mutable
	@Shadow
	@SuppressWarnings("null")
	private static List<RegistryData<?>> WORLDGEN_REGISTRIES;

	/// Remove the dialog registry from the list of registries to load, since we will load it ourselves in the mod initializer.
	///
	/// @param ci the callback info, which is not used
	@Inject(at = @At("RETURN"), method = "<clinit>")
	private static void filterRegistries(CallbackInfo ci) {
		WORLDGEN_REGISTRIES = requireNonNull(WORLDGEN_REGISTRIES.stream().filter(data -> data.key() != DIALOG).toList());
	}
}
