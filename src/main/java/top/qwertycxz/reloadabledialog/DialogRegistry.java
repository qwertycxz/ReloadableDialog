package top.qwertycxz.reloadabledialog;

import static com.mojang.logging.LogUtils.getLogger;
import static com.mojang.serialization.JsonOps.INSTANCE;
import static java.util.Collections.newSetFromMap;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static net.minecraft.core.RegistrationInfo.BUILT_IN;
import static net.minecraft.core.registries.Registries.DIALOG;
import static net.minecraft.core.registries.Registries.tagsDirPath;
import static net.minecraft.resources.FileToIdConverter.registry;
import static net.minecraft.server.dialog.Dialog.DIRECT_CODEC;
import static net.minecraft.tags.TagLoader.ElementLookup.fromWritableRegistry;
import static net.minecraft.util.StrictJsonParser.parse;

import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.core.RegistryAccess.ImmutableRegistryAccess;
import net.minecraft.core.RegistryAccess.RegistryEntry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import top.qwertycxz.reloadabledialog.mixin.NamedInvoker;

/// The dialog registry. While loading, lookups create unbound holders on demand so that dialogs can reference each other, including forward and circular references. Once loading finishes, it behaves exactly like a normal frozen registry.
@NullMarked
public class DialogRegistry extends MappedRegistry<Dialog> {
	/// A set of players whose client is not up-to-date with the server, so we should not send them dialogs.
	public static final Set<ServerPlayer> STALE_PLAYERS = requireNonNull(newSetFromMap(new WeakHashMap<>()));
	/// The converter between dialog JSON file paths and dialog registry keys.
	private static final FileToIdConverter CONVERTER = requireNonNull(registry(DIALOG));
	/// The lifecycle of the dialog registry, which is always stable since it only contains built-in entries.
	private static final Lifecycle LIFECYCLE = requireNonNull(BUILT_IN.lifecycle());
	/// The logger for dialog loading errors.
	private static final Logger LOGGER = getLogger();
	/// The operations used for loading dialog JSONs, which is the same as the one used for loading built-in registries.
	private static final JsonOps OPERATIONS = requireNonNull(INSTANCE);
	/// The directory path of dialog tags.
	private static final String PATH = requireNonNull(tagsDirPath(DIALOG));

	/// Loads dialogs from the resource manager and returns a frozen registry access containing the dialog registry.
	///
	/// @param layer layer `n`, the frozen registry access layer where the dialog registry will be added
	/// @param access layers `0 ~ n-1`, the provider of registry lookups as the base for dialog loading
	/// @param manager the resource manager to load dialog JSONs from
	/// @return modified layer `n`, a frozen registry access containing the dialog registry
	@SuppressWarnings("unchecked")
	public static Frozen loadDialog(Frozen layer, Provider access, ResourceManager manager) {
		var instance = new DialogRegistry();
		var operations = RegistryOps.create(OPERATIONS, Provider.create(requireNonNull(of(requireNonNull(layer.listRegistries().filter(lookup -> lookup.key() != DIALOG)), access.listRegistries(), requireNonNull(of(instance))).flatMap(identity()))));

		for (var resource : CONVERTER.listMatchingResources(manager).entrySet()) {
			var i = requireNonNull(resource.getKey());
			var key = ResourceKey.create(DIALOG, CONVERTER.fileToId(i));
			try (var reader = resource.getValue().openAsReader()) {
				instance.register(key, requireNonNull(DIRECT_CODEC.parse(operations, parse(reader)).getOrThrow(JsonParseException::new)), BUILT_IN);
			}
			catch (IOException | IllegalArgumentException | JsonParseException e) {
				LOGGER.error("Couldn't parse data file '{}' from '{}'", key, i, e);
			}
		}

		var loader = new TagLoader<>(fromWritableRegistry(instance), PATH);
		for (var holders : loader.build(loader.load(manager)).entrySet()) {
			((NamedInvoker<Dialog>)instance.getter.getOrThrow(TagKey.create(DIALOG, requireNonNull(holders.getKey())))).invokeBind(requireNonNull(holders.getValue()));
		}

		instance.loaded = true;
		return new ImmutableRegistryAccess(requireNonNull(concat(layer.registries().filter(entry -> entry.key() != DIALOG), of((@Nullable RegistryEntry<Dialog>)new RegistryEntry<>(DIALOG, instance))))).freeze();
	}

	/// The lazy registration lookup, which creates unbound holders on demand so that references can be resolved later at registration time.
	private final HolderGetter<Dialog> getter = createRegistrationLookup();
	/// Whether loading has finished. Only before loading finishes do lookups create unbound holders; afterwards lookups behave like a normal frozen registry.
	private boolean loaded;

	/// Creates a new empty dialog registry.
	private DialogRegistry() {
		super(DIALOG, LIFECYCLE);
	}

	/// Looks up a dialog by key. During loading, an unbound holder is created if the dialog is not registered yet.
	///
	/// @param key the key of the dialog
	/// @return a reference to the dialog, which may be unbound during loading
	@Override
	public Optional<Reference<Dialog>> get(ResourceKey<Dialog> key) {
		if (loaded) return super.get(key);
		return getter.get(key);
	}

	/// Looks up a dialog tag by key. During loading, an unbound named holder set is created if the tag is not bound yet.
	///
	/// @param key the key of the tag
	/// @return a named holder set of the tag, which may be unbound during loading
	@Override
	public Optional<Named<Dialog>> get(TagKey<Dialog> key) {
		if (loaded) return super.get(key);
		return getter.get(key);
	}

	/// Looks up a dialog by key. During loading, an unbound holder is created if the dialog is not registered yet.
	///
	/// @param key the key of the dialog
	/// @return a reference to the dialog, which may be unbound during loading
	@Override
	public Reference<Dialog> getOrThrow(ResourceKey<Dialog> key) {
		if (loaded) return super.getOrThrow(key);
		return getter.getOrThrow(key);
	}

	/// Looks up a dialog tag by key. During loading, an unbound named holder set is created if the tag is not bound yet.
	///
	/// @param key the key of the tag
	/// @return a named holder set of the tag, which may be unbound during loading
	@Override
	public Named<Dialog> getOrThrow(TagKey<Dialog> key) {
		if (loaded) return super.getOrThrow(key);
		return getter.getOrThrow(key);
	}
}
