package top.qwertycxz.loadabledialog;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps.RegistryInfo;
import net.minecraft.resources.RegistryOps.RegistryInfoLookup;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record RegistryInfoLookupImpl(Map<? extends ResourceKey<? extends Registry<?>>, ? extends RegistryInfo<?>> info) implements RegistryInfoLookup {
	@Override
	@SuppressWarnings("unchecked")
	public <T> Optional<RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
		return requireNonNull(ofNullable((RegistryInfo<T>)info.get(key)));
	}
}
