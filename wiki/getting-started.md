[← Back to Wiki Hub](README.md) | [Main Documentation](../README.md)

# Getting Started with Magic Storage

Magic Storage centralizes storage, item sorting, and multi-station crafting into a single connected network.

---

## 1. Minimal Network Setup

A functioning network requires three blocks placed adjacent to each other:

1. **Storage Heart:** The central processing unit and station container.
2. **Storage Unit:** The physical storage container that holds items.
3. **Storage Access** or **Crafting Access:** The terminal used to interact with stored items.

```
+------------------+    +------------------+    +------------------+
|   Storage Unit   | <-> |  Storage Heart   | <-> | Crafting Access  |
|  (Holds Items)   |    |  (Master Brain)  |    | (Grid & Terminal)|
+------------------+    +------------------+    +------------------+
```

---

## 2. Network Layout Rules

* **Adjacency:** Every block in the network must touch at least one other network block on one of its 6 faces.
* **Maximum Distance:** Network blocks must be within **32 blocks** of the Storage Heart.
* **Single Heart Rule:** A network must contain exactly one Storage Heart. Connecting two active Storage Hearts together will deactivate the network until separated.
* **Chunk Loading:** The Storage Heart automatically keeps its own chunk and connected storage unit chunks loaded through `ForgeChunkManager`.

---

## 3. Terminal Interface & Sorting

Opening a Storage Access or Crafting Access terminal presents the network inventory grid:

| Control | Action | Function |
| :--- | :--- | :--- |
| **Search Bar** | Type text | Filters inventory by item display name, registry name, tooltip text, or mod ID. |
| **Sort Mode** | Click button | Cycles sorting by **Quantity** (default), **Item Name** (alphabetical), or **Mod ID**. |
| **Sort Order** | Click $\uparrow$ / $\downarrow$ | Toggles between Ascending and Descending order. |
| **Keep Text** | Click button | Preserves search bar query when closing and reopening the GUI. |
| **JEI Sync** | Click button | Synchronizes search query with the JEI search bar. |
| **Clear (`X`)** | Click button | Clears current search query. |

---

## 4. Keybindings & Shortcuts

| Shortcut | Context | Behavior |
| :--- | :--- | :--- |
| **`Left Alt`** | In-Game (No GUI) | Opens the first linked portable remote found in the player inventory. |
| **`Q`** | Hovering item in terminal | Drops **1 item** from the stack into the world. |
| **`Ctrl + Q`** | Hovering item in terminal | Drops the **entire stack** into the world. |
| **`Shift + Left-Click`** | Item in Terminal / Inventory | Transfers items between player inventory and network storage. |
| **`Shift + Right-Click`** | Storage Unit (Empty Hand) | Displays unit capacity and fullness in chat with an audio cue. |
| **`Shift + Right-Click`** | Storage Unit (Holding Upgrade) | Applies tier upgrade in-place without dropping stored items. |
| **`R` / `U`** | Hovering item in terminal | Displays JEI Recipes (`R`) or Uses (`U`). |

---

[← Back to Wiki Hub](README.md) | [Main Documentation](../README.md)
