# Emotecraft Fix Animate Texture

A client-side compatibility mod that prevents Emotecraft animations from glitching when used with player animation resource packs, such as **Fresh Animations: Player Extension**.

I originally made this mod for a server with friends. Its purpose is to temporarily suppress conflicting EMF player animations while an Emotecraft emote is playing, allowing the emote animation to display correctly.

## Supported Versions

| Mod Loader | Minecraft Version | Branch |
|---|---:|---|
| Forge | 1.20.1 | `main` |
| Fabric | 1.21.11 | `fabric-1.21.11` |

The Forge and Fabric versions are maintained in separate branches to avoid mixing the configuration and source code of both mod loaders.

## Features

- Prevents compatible player animation resource packs from overriding Emotecraft emotes.
- Temporarily suppresses conflicting EMF animations while an emote is active.
- Restores normal player animations after the emote finishes.
- Client-side only.
- Supports the local player and other rendered players when possible.
- Does not permanently disable the animation resource pack.

## Author

- **Catman0221** — Original mod author and Forge 1.20.1 version

## Contributors

- [atleugim](https://github.com/atleugim) — Fabric 1.21.11 port
