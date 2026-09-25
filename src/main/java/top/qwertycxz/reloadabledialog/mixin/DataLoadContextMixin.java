package top.qwertycxz.reloadabledialog.mixin;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.server.WorldLoader.DataLoadContext;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.WorldDataConfiguration;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.qwertycxz.reloadabledialog.ProviderAccessor;

/// Mixin to capture the provider of registry lookups passed to the `DataLoadContext` constructor, whose parameter types are the same across versions even though the record accessor name is not.
@Mixin(DataLoadContext.class)
@NullMarked
public abstract class DataLoadContextMixin implements ProviderAccessor {
	/// The captured provider of registry lookups, which is used as the base for dialog loading.
	@SuppressWarnings("null")
	@Unique
	private Provider provider;

	@Override
	@Unique
	public Provider getProvider() {
		return provider;
	}

	/// Captures the provider of registry lookups passed to the constructor.
	///
	/// @param manager the resource manager, which is not used
	/// @param configuration the world data configuration, which is not used
	/// @param provider the provider of registry lookups to capture
	/// @param frozen the frozen registry access, which is not used
	/// @param ci the callback info, which is not used
	@Inject(at = @At("RETURN"), method = "<init>")
	private void setProvider(ResourceManager manager, WorldDataConfiguration configuration, Provider provider, Frozen frozen, CallbackInfo ci) {
		this.provider = provider;
	}
}
