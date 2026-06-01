package top.qwertycxz.loadabledialog.mixin;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet.Named;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/// Invoker for bind tags
@Mixin(Named.class)
@NullMarked
public interface NamedInvoker<T> {
	/// Invokes the `bind` method of `Named`, which is used for binding holders to a named holder set.
	///
	/// @param holders the holders to bind
	@Invoker("bind")
	void invokeBind(List<Holder<T>> holders);
}
