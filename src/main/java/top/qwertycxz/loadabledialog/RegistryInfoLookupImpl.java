package top.qwertycxz.loadabledialog;

import static com.mojang.serialization.JsonOps.INSTANCE;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static net.minecraft.core.RegistrationInfo.BUILT_IN;
import static net.minecraft.core.registries.Registries.DIALOG;
import static net.minecraft.core.registries.Registries.tagsDirPath;
import static net.minecraft.resources.RegistryOps.RegistryInfo.fromRegistryLookup;
import static net.minecraft.server.dialog.Dialog.DIRECT_CODEC;
import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;
import static net.minecraft.tags.TagLoader.ElementLookup.fromWritableRegistry;

import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.core.RegistryAccess.ImmutableRegistryAccess;
import net.minecraft.core.RegistryAccess.RegistryEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.RegistryOps.RegistryInfo;
import net.minecraft.resources.RegistryOps.RegistryInfoLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import top.qwertycxz.loadabledialog.mixin.NamedInvoker;

@NullMarked
public record RegistryInfoLookupImpl<T>(Map<? extends ResourceKey<? extends Registry<T>>, ? extends RegistryInfo<T>> info) implements RegistryInfoLookup {
	private static final Lifecycle LIFECYCLE = BUILT_IN.lifecycle();
	private static final JsonOps OPERATIONS = requireNonNull(INSTANCE);

	@Override
	@SuppressWarnings("unchecked")
	public <U> Optional<RegistryInfo<U>> lookup(ResourceKey<? extends Registry<? extends U>> key) {
		return requireNonNull(ofNullable((RegistryInfo<U>)info.get(key)));
	}

	@SuppressWarnings("unchecked")
	public static Frozen loadDialog(Frozen layer, Provider access, ResourceManager manager) {
		ConcurrentMap<@NonNull ResourceKey<? extends Registry<?>>, @NonNull RegistryInfo<?>> info = concat(layer.listRegistries(), access.listRegistries())
			.collect(toConcurrentMap(RegistryLookup::key, lookup -> fromRegistryLookup(requireNonNull(lookup))));
		var registry = new MappedRegistry<>(DIALOG, LIFECYCLE);
		var getter = registry.createRegistrationLookup();
		info.put(DIALOG, new RegistryInfo<>(registry, getter, LIFECYCLE));

		var dialogs = new ConcurrentHashMap<Identifier, Dialog>();
		scanDirectory(manager, DIALOG, RegistryOps.create(OPERATIONS, new RegistryInfoLookupImpl<>(info)), DIRECT_CODEC, dialogs);
		for (var dialog : dialogs.entrySet()) {
			registry.register(ResourceKey.create(DIALOG, requireNonNull(dialog.getKey())), requireNonNull(dialog.getValue()), BUILT_IN);
		}
		var loader = new TagLoader<>(fromWritableRegistry(registry), tagsDirPath(DIALOG));
		for (var holder : loader.build(loader.load(manager)).entrySet()) {
			((NamedInvoker<Dialog>)getter.getOrThrow(TagKey.create(DIALOG, requireNonNull(holder.getKey())))).invokeBind(requireNonNull(holder.getValue()));
		}
		return new ImmutableRegistryAccess(requireNonNull(concat(layer.registries().filter(entry -> entry.key() != DIALOG), of((@Nullable RegistryEntry<Dialog>)new RegistryEntry<>(DIALOG, registry))))).freeze();
	}
}
