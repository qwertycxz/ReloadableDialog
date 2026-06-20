package top.qwertycxz.reloadabledialog.mixin;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static net.minecraft.core.Holder.Kind.REFERENCE;
import static net.minecraft.core.Holder.direct;
import static net.minecraft.network.chat.Component.literal;
import static net.minecraft.server.dialog.DialogAction.CLOSE;
import static net.minecraft.server.dialog.NoticeDialog.DEFAULT_ACTION;
import static net.minecraft.server.dialog.body.PlainMessage.DEFAULT_WIDTH;
import static top.qwertycxz.reloadabledialog.RegistryInfoLookupImpl.STALE_PLAYERS;

import net.minecraft.core.Holder;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.NoticeDialog;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/// Mixin to replace the dialog sent to players whose client is not up-to-date with the server, so they can see a notice dialog instead of a missing dialog error when loading dialogs.
@Mixin(ServerPlayer.class)
@NullMarked
public abstract class ServerPlayerMixin {
	/// A holder of a notice dialog that is shown to players whose client is not up-to-date with the server..
	@Unique
	private static final Holder<@NonNull Dialog> STALE_DIALOG = direct(
		new NoticeDialog(
			new CommonDialogData(
				literal("Reconnect Required"), requireNonNull(empty()), true, true, CLOSE,
				requireNonNull(asList((@Nullable PlainMessage)new PlainMessage(literal("Dialogs have been reloaded. Please reconnect to the server to use this dialog."), DEFAULT_WIDTH))), requireNonNull(asList())
			), DEFAULT_ACTION
		)
	);

	/// Replaces the dialog sent to players.
	///
	/// @param dialog the original dialog to be sent to the player
	/// @return the dialog to be sent to the player, which is the original dialog if the player is up-to-date, or a notice dialog if the player is not up-to-date
	@ModifyArg(method = "openDialog", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/common/ClientboundShowDialogPacket;<init>"))
	private Holder<@NonNull Dialog> staleDialog(Holder<@NonNull Dialog> dialog) {
		if (dialog.kind() == REFERENCE && STALE_PLAYERS.contains((Object)this)) return STALE_DIALOG;
		return dialog;
	}
}
