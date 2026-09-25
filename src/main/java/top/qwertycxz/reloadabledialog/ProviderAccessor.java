package top.qwertycxz.reloadabledialog;

import net.minecraft.core.HolderLookup.Provider;
import org.jspecify.annotations.NullMarked;

/// Accessor for the provider of registry lookups held by `DataLoadContext`, which cannot be accessed by the record accessor directly since its name differs across versions.
@NullMarked
public interface ProviderAccessor {
	/// Gets the provider of registry lookups used as the base for loading.
	///
	/// @return the provider of registry lookups
	Provider getProvider();
}
