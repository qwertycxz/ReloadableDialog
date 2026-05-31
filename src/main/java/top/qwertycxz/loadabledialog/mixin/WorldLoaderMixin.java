package top.qwertycxz.loadabledialog.mixin;

import static com.mojang.serialization.JsonOps.INSTANCE;
import static java.util.Objects.requireNonNull;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess.ImmutableRegistryAccess;
import net.minecraft.core.RegistryAccess.RegistryEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.RegistryOps.RegistryInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldLoader.DataLoadOutput;
import net.minecraft.server.WorldLoader.WorldDataSupplier;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.qwertycxz.loadabledialog.RegistryInfoLookupImpl;

@Mixin(WorldLoader.class)
@NullMarked
public abstract class WorldLoaderMixin {
	@Unique
	private static final Lifecycle LIFECYCLE = BUILT_IN.lifecycle();
	@Unique
	private static final JsonOps OPERATIONS = requireNonNull(INSTANCE);

	@ModifyVariable(argsOnly = true, at = @At("HEAD"), method = "load")
	@SuppressWarnings("unchecked")
	private static <T> WorldDataSupplier<T> loadDialog(WorldDataSupplier<T> supplier) {
		return context -> {
			var data = supplier.get(context);
			var dimensions = data.finalDimensions();
			ConcurrentMap<@NonNull ResourceKey<? extends Registry<?>>, @NonNull RegistryInfo<?>> info = concat(context.datapackWorldgen().listRegistries(), dimensions.listRegistries())
				.collect(toConcurrentMap(RegistryLookup::key, lookup -> fromRegistryLookup(requireNonNull(lookup))));
			var registry = new MappedRegistry<>(DIALOG, LIFECYCLE);
			var getter = registry.createRegistrationLookup();
			info.put(DIALOG, new RegistryInfo<>(registry, getter, LIFECYCLE));

			var dialogs = new ConcurrentHashMap<Identifier, Dialog>();
			var manager = context.resources();

			scanDirectory(manager, DIALOG, RegistryOps.create(OPERATIONS, new RegistryInfoLookupImpl(info)), DIRECT_CODEC, dialogs);
			for (var dialog : dialogs.entrySet()) {
				registry.register(ResourceKey.create(DIALOG, requireNonNull(dialog.getKey())), requireNonNull(dialog.getValue()), BUILT_IN);
			}
			var loader = new TagLoader<>(fromWritableRegistry(registry), tagsDirPath(DIALOG));
			for (var holder : loader.build(loader.load(manager)).entrySet()) {
				((NamedInvoker<Dialog>)getter.getOrThrow(TagKey.create(DIALOG, requireNonNull(holder.getKey())))).invokeBind(holder.getValue());
			}
			return new DataLoadOutput<>(data.cookie(), new ImmutableRegistryAccess(requireNonNull(concat(dimensions.registries(), of((@Nullable RegistryEntry<Dialog>)new RegistryEntry<>(DIALOG, registry))))).freeze());
		};
	}
}
