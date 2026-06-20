package top.qwertycxz.reloadabledialog.mixin;

import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldLoader.DataLoadOutput;
import net.minecraft.server.WorldLoader.WorldDataSupplier;
import static top.qwertycxz.reloadabledialog.RegistryInfoLookupImpl.loadDialog;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/// We removed the dialog loading code from the main world loading code and put it in a separate supplier, so we need to modify the world loading code to use our supplier instead of the original one.
@Mixin(WorldLoader.class)
@NullMarked
public abstract class WorldLoaderMixin {
	/// Modifies the world loading code to use our dialog loading code.
	///
	/// @param supplier the original world data supplier, which is used for loading world data
	/// @return a new world data supplier that loads dialogs using our dialog loading code after loading world data using the original supplier
	@ModifyVariable(argsOnly = true, at = @At("HEAD"), method = "load")
	private static <T> WorldDataSupplier<T> initialDialog(WorldDataSupplier<T> supplier) {
		return context -> {
			var output = supplier.get(context);
			return new DataLoadOutput<>(output.cookie(), loadDialog(output.finalDimensions(), context.datapackWorldgen(), context.resources()));
		};
	}
}
