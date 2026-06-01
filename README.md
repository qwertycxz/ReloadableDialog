# Reloadable Dialog

Minecraft 1.21.6 added a new feature called [dialog](https://minecraft.wiki/w/Dialog), which allows players to interact through a dialog interface. However, the dialog data is loaded only once when the server starts, and any changes to the dialog data require a server restart to take effect.

This mod fix that.

## What does it do and doesn't do?

This server-side mod allows you to reload dialog data without restarting the server. You can simply edit the dialog JSON files and run the command `/reload` to apply the changes immediately.

However, the reloaded dialog data cannot be sent to players who are already connected to the server. They will need to reconnect to see the updated dialog data. This is what a server-side mod can do without modifying the client.

## Download

TBD

## Requirement

* Minecraft ⩾ 1.21.6
* Fabric or NeoForge

## Usage

Very simple, just add the mod to your server and restart the server. After that, you can edit the dialog JSON files and run the command `/reload` to apply the changes.

## Contributor

[@qwertycxz](https://github.com/qwertycxz)

## How could I contribute?

[Issue](https://github.com/qwertycxz/MetadataWildcard4fabric-permissions-api/issues/new) and [Pull-requests](https://github.com/qwertycxz/MetadataWildcard4fabric-permissions-api/compare) are both welcomed.

## License

[Apache 2.0](LICENSE) © qwertycxz
