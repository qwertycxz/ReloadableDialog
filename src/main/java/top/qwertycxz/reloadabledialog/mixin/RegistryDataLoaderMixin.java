package top.qwertycxz.reloadabledialog.mixin;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static net.minecraft.core.registries.Registries.DIALOG;
import static net.minecraft.core.registries.Registries.WORLD_PRESET;

import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryDataLoader.RegistryData;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/// Mixin to remove the dialog registry from the vanilla registry data loader, since we want to load dialogs from our own data pack instead of the built-in one.
///
/// The registry list is built inline in the class initializer, and its field is named differently across supported Minecraft versions. Filtering the varargs passed to `List.of` avoids depending on that field name.
@Mixin(RegistryDataLoader.class)
@NullMarked
public abstract class RegistryDataLoaderMixin {
	/// Removes the dialog registry from the registry list that also contains world presets.
	///
	/// `WORLD_PRESET` identifies the world/worldgen registry list across supported versions. Other registry lists, including the synchronized registry list, do not contain it and are returned unchanged.
	///
	/// @param elements the registry data passed to `List.of` by the class initializer
	/// @return the original elements, or the world/worldgen registry data with dialogs removed
	@ModifyArg(at = @At(target = "of([Ljava/lang/Object;)Ljava/util/List;", value = "INVOKE"), method = "<clinit>")
	private static Object[] filterRegistries(Object[] elements) {
		if (stream(elements).anyMatch(element -> element instanceof RegistryData<?> data && data.key() == WORLD_PRESET)) return requireNonNull(stream(elements).filter(element -> element instanceof RegistryData<?> data && data.key() != DIALOG).toArray());
		return elements;
	}
}
