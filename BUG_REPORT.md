# Bug Report & Analysis — Slimefun5

## Daftar Isi
1. [Item Output Ilang (Overflow)](#1-item-output-ilang-overflow)
2. [Dupe Item saat Block Break + Re-place](#2-dupe-item-saat-block-break--re-place)
3. [Instant Processing (Integer Division Zero Ticks)](#3-instant-processing-integer-division-zero-ticks)
4. [Nama Item Berubah Vanilla Saat Dipindah](#4-nama-item-berubah-vanilla-saat-dipindah)
5. [Slimefun4 Compatibility Shim](#5-slimefun4-compatibility-shim)
6. [Item Merge / Stacking](#6-item-merge--stacking)

---

## 1. Item Output Ilang (Overflow)

### Severity
**CRITICAL**

### Deskripsi
Saat mesin menyelesaikan crafting (progress bar 100%), hasil output di-push ke output slot. Tapi selama progress bar berjalan (bisa 30+ detik), pemain bisa mengisi output slot dengan item lain. Saat `pushItem` dipanggil, item yang tidak muat akan **ilang/hilang** tanpa jejak.

### Penyebab
**File**: `core/src/main/java/me/mrCookieSlime/Slimefun/Objects/SlimefunItem/abstractItems/AContainer.java`
**Baris 367-369**:
```java
for (ItemStack output : currentOperation.getResults()) {
    inv.pushItem(output.clone(), getOutputSlots());
}
```
- `pushItem` (di `DirtyChestMenu.java:114-153`) mengembalikan `ItemStack` sisa jika tidak muat, atau `null` jika muat semua
- Tapi return value ini **tidak pernah dicek** oleh `tick()`
- `findNextRecipe` (line 435) hanya cek `InvUtils.fitAll` **sekali di awal craft** sebelum progress bar dimulai. Selama crafting, tidak ada validasi ulang.

### Skenario
1. Player memasukkan bahan ke mesin
2. Mesin mulai craft, progress bar berjalan
3. Player mengisi kedua output slot (slot 24 & 25) dengan item lain
4. Mesin selesai → `pushItem` dipanggil → output tidak muat → **item ilang**

### Fix
Di `AContainer.tick()`, sebelum `pushItem`:
1. Validasi ulang `InvUtils.fitAll()` untuk output item
2. Jika tidak muat: **jangan push** — biarkan operasi tetap "selesai" tapi output ditahan (skip `endOperation` atau ulang progress bar)
3. Jika `pushItem` return tidak null (ada sisa): drop sisa ke lokasi block

---

## 2. Dupe Item saat Block Break + Re-place

### Severity
**CRITICAL**

### Deskripsi
Ketika mesin di-break (dihancurkan) dengan item di dalamnya, item di-drop ke tanah. Tapi jika mesin di-place ulang di lokasi yang **sama**, item muncul lagi — alias **dupe**.

### Penyebab
**File**: `core/src/main/java/me/mrCookieSlime/Slimefun/Objects/SlimefunItem/abstractItems/AContainer.java`
**Baris 71-87** (`onBlockBreak`):
```java
protected BlockBreakHandler onBlockBreak() {
    return new SimpleBlockBreakHandler() {
        @Override
        public void onBlockBreak(Block b) {
            BlockMenu inv = BlockStorage.getInventory(b);
            if (inv != null) {
                inv.dropItems(b.getLocation(), getInputSlots());
                inv.dropItems(b.getLocation(), getOutputSlots());
            }
            processor.endOperation(b);
        }
    };
}
```

**File**: `core/src/main/java/io/github/thebusybiscuit/slimefun5/implementation/listeners/BlockListener.java`
**Baris 224-242** (`callBlockHandler`):
```java
if (sfItem != null && !sfItem.useVanillaBlockBreaking()) {
    sfItem.callItemHandler(BlockBreakHandler.class, handler -> handler.onPlayerBreak(e, item, drops));
    // ... close inventory ...
    BlockStorage.clearBlockInfo(e.getBlock());  // DEFERRED to next ticker tick
}
```

Flow saat block break:
1. `onPlayerBreak` → `dropItems` → item di-drop, slot dikosongkan, `markDirty()` (ada perubahan belum disimpan)
2. `BlockStorage.clearBlockInfo()` → `queueDelete(l, true)` — **DEFERRED** ke ticker tick berikutnya
3. File `.sfi` di disk **masih berisi state LENGKAP** (sebelum break)
4. Jika server/plugin crash atau ada race condition sebelum ticker memproses queue, file `.sfi` tidak pernah dihapus
5. Saat block di-place ulang di lokasi sama → `setBlockInfo` membaca `.sfi` yang masih ada → item lama muncul kembali = **dupe**

**KASUS TAMBAHAN**: Jika perubahan kamu sebelumnya **tidak** memanggil `dropItems` sama sekali (misalnya hanya `clearBlockInfo`), maka BlockMenu di RAM tidak pernah dikosongkan, file `.sfi` tidak pernah diupdate, dan saat re-place file lama langsung di-load = dupe.

### Fix
- Tambahkan `inv.delete(b.getLocation())` **setelah** `dropItems` dan **sebelum** `processor.endOperation(b)` untuk langsung menghapus file `.sfi`
- Atau: pastikan urutan tidak diubah dari implementasi original

---

## 3. Instant Processing (Integer Division Zero Ticks)

### Severity
**HIGH**

### Deskripsi
Beberapa mesin tingkat tinggi (tier 3) memiliki kecepatan (speed) tinggi (10), dan recipe base time-nya kecil (3-4 detik). Integer division Java menyebabkan hasil = 0 ticks, sehingga **mesin memproses item secara instan setiap tick** tanpa delay.

### Penyebab (3 lokasi)

#### 3a. ElectricGoldPan_3
**File**: `core/src/main/java/io/github/thebusybiscuit/slimefun5/implementation/items/electric/machines/ElectricGoldPan.java`
**Baris 102 & 105**:
```java
recipe = new MachineRecipe(3 / getSpeed(), ...);  // 3/10 = 0
recipe = new MachineRecipe(4 / getSpeed(), ...);  // 4/10 = 0
```
Speed tier 3 = 10. Base 3 detik → `3/10 = 0` detik = 0 ticks.

#### 3b. ElectricDustWasher_3
**File**: `core/src/main/java/io/github/thebusybiscuit/slimefun5/implementation/items/electric/machines/ElectricDustWasher.java`
**Baris 58, 65, 67**:
```java
recipe = new MachineRecipe(4 / getSpeed(), ...);  // 4/10 = 0
```
Speed tier 3 = 10. Base 4 detik → `4/10 = 0` detik = 0 ticks.

#### 3c. ElectricOreGrinder_3
**File**: (via PostSetup) `core/src/main/java/io/github/thebusybiscuit/slimefun5/implementation/setup/PostSetup.java`
**Baris 202**:
```java
container.registerRecipe(4, input, output);
```
Di `AContainer.registerRecipe()` (baris 310):
```java
recipe.setTicks(recipe.getTicks() / getSpeed());
```
Speed tier 3 = 10. Base 4 detik = 8 ticks → `8/10 = 0` ticks.

### Fix
- ElectricGoldPan & ElectricDustWasher: ganti `4 / getSpeed()` → `Math.max(1, 4 / getSpeed())` di semua tempat
- ElectricOreGrinder (PostSetup): ganti `registerRecipe(4, ...)` → `registerRecipe(Math.max(1, 4 * 10 / speed), ...)` atau pastikan base time cukup besar

### Daftar Machine Terkena
| Machine | Tier | Speed | Base (s) | Ticks | Hasil |
|---|---|---|---|---|---|
| ElectricGoldPan_3 | 3 | 10 | 3 | 6/10=0 | **INSTANT** |
| ElectricGoldPan_3 (nether) | 3 | 10 | 4 | 8/10=0 | **INSTANT** |
| ElectricDustWasher_3 | 3 | 10 | 4 | 8/10=0 | **INSTANT** |
| ElectricOreGrinder_3 | 3 | 10 | 4 | 8/10=0 | **INSTANT** |

---

## 4. Nama Item Berubah Vanilla Saat Dipindah

### Severity
**MEDIUM**

### Deskripsi
Dua item Slimefun yang identik (misalnya dua `BLISTERING_INGOT` (33%)) tidak bisa di-merge di inventory. Saat dipindah posisi, salah satu item namanya berubah menjadi nama material vanilla ("Gold Ingot"). Perlu di-click kanan (di-ingame) untuk mengembalikan nama yang benar.

### Penyebab
**File**: `core/src/main/java/io/github/thebusybiscuit/slimefun5/core/services/localization/ItemTranslationService.java`
**Baris 477-537** (`renderForPacket`):
```java
RenderedDisplay renderForPacket(String id, String languageId, FallbackMode fallback, boolean includeDescription) {
    // ...
    String name;
    if (translation != null && translation.name != null) {
        name = ... // translated name
    } else if (fallback == FallbackMode.ID) {
        name = id;  // "BLISTERING_INGOT"
    } else {
        ItemTranslation en = lookup("en", id);
        if (en != null && en.name != null) {
            name = ... // English name
        } else {
            ItemMeta englishNameMeta = english != null ? english.getItemMeta() : item.getItem().getItemMeta();
            name = (englishNameMeta != null && englishNameMeta.hasDisplayName()) 
                   ? englishNameMeta.getDisplayName() 
                   : id;
        }
    }
```

**Masalah**:
1. **`englishBaseline` cache miss**: `englishBaseline` di-populate sekali di `canonicalizeToId()` dengan `item.getItem().clone()`. Tapi item hasil craft adalah **instance baru** (clone dari `SlimefunItemStack`). Saat `renderForPacket` dipanggil untuk instance baru ini, `englishBaseline.get(id)` bisa return null jika canonicalize belum selesai atau item dari addon.
2. **Fallback ke ItemMeta yang sudah terbake**: Jika `englishBaseline.get(id)` return null, fallback ke `item.getItem().getItemMeta()`. Tapi `getItem()` untuk `SlimefunItemStack` mengembalikan `delegate.clone()` — ItemMeta dari clone ini mungkin kehilangan display name karena di-reset oleh `bakeTranslatedDisplay`.
3. **Race condition**: `renderForPacket` dipanggil dari Netty thread (async), sementara `canonicalizeToId()` berjalan di main thread saat boot. Ada window dimana packet terkirim sebelum canonicalize selesai.
4. **Lore composition null**: Di `LoreComposer.compose()`, jika `fallbackBase` kosong atau null, lore tidak ter-render dengan benar.

**Mengapa tidak bisa merge**: Karena `ItemStack#isSimilar()` untuk Slimefun items membandingkan PDC (Persistent Data Container) — dua clone dari item yang sama seharusnya bisa merge. Tapi jika satu item display name-nya berbeda (satu "Blistering Ingot", satu "Gold Ingot"), `isSimilar` bisa return false karena ItemMeta berbeda.

### Fix
1. Pastikan `renderForPacket` selalu punya fallback ke `item.getItemName()` (getItemName dari SlimefunItem, yang merupakan nama display dari SlimefunItemStack)
2. Di `canonicalizeToId()`, tunggu semua addon selesai register sebelum capture `englishBaseline`
3. Safety net: di `renderForPacket`, jika semua fallback gagal, gunakan `item.getItem().getItemMeta().getDisplayName()` (yang sudah terbake)
4. Pastikan `LoreComposer.compose()` tidak mereturn lore yang corrupt

---

## 5. Slimefun4 Compatibility Shim

### Severity
**CRITICAL**

### Status
**BERBAHAYA — shim menyebabkan identifikasi item kacau**

### Detail
**File**: `core/src/main/java/io/github/thebusybiscuit/slimefun4/api/items/SlimefunItem.java`
(60 baris — shim tipis)

Fork memindahkan package dari `io.github.thebusybiscuit.slimefun4` ke `io.github.thebusybiscuit.slimefun5`. Shim ini menyediakan class `SlimefunItem` di package lama yang mendelegasikan ke implementasi baru.

### Masalah Utama: Shim Membuat Instance Baru Setiap Kali

```java
@Nullable
private static SlimefunItem wrap(@Nullable io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem item) {
    return item == null ? null : new SlimefunItem(item);  // <-- INSTANCE BARU setiap panggilan
}
```

Setiap `getByItem()` atau `getById()` membuat **object baru**. Akibatnya:

| Masalah | Dampak |
|---|---|
| **`equals()` pakai Object identity** | Dua `getByItem(stack1)` dan `getByItem(stack2)` untuk item yang SAMA akan return `false` dengan `equals()` |
| **`hashCode()` pakai Object identity** | Satu item bisa masuk HashSet berkali-kali, HashMap lookup gagal |
| **Garbage collection overload** | Setiap lookup bikin new object — tidak scalable |

### Contoh Skenario Berbahaya (ExtraStorage / UniItem)

Plugin yang menggunakan `SlimefunItem.getByItem()` untuk identifikasi item:

```java
SlimefunItem sfItem = SlimefunItem.getByItem(someStack);
String id = sfItem.getId();  // ✅ OK, delegate benar

// TAPI jika plugin simpan reference dan bandingkan:
List<SlimefunItem> cache = new ArrayList<>();
cache.add(SlimefunItem.getByItem(stack1));

// Cek apakah item sudah ada di cache:
SlimefunItem found = SlimefunItem.getByItem(stack2);
if (cache.contains(found)) {    // ❌ SELALU false! instance berbeda
    // Tidak pernah true!
}

// Atau sebagai key di Map:
Map<SlimefunItem, Data> map = new HashMap<>();
map.put(SlimefunItem.getByItem(stack), someData);
Data d = map.get(SlimefunItem.getByItem(sameStack)); // ❌ null! key berbeda
```

### Dampak Konkret yang Kamu Alami

> "Mungkin kayaknya tipe nya ke ubah jadi gold biasa, makanya jadi 1 padahal mau tuker yang 33% di dalam itu 2 dengan 66% 2 karena carbonado abis"

Plugin eksternal (seperti sistem penyimpanan atau cargo) yang menggunakan `SlimefunItem.getByItem()` untuk mengidentifikasi **item apa yang ada di slot** tidak bisa membedakan `BLISTERING_INGOT` (33%), `BLISTERING_INGOT_2` (66%), dan `BLISTERING_INGOT_3` (100%) dengan benar karena:
- Shim membuat wrapper baru tiap `getByItem()`
- Jika plugin bandingkan hasil `getByItem()` secara reference (`==`), selalu false
- Jika plugin bandingkan `getId()`, OK. Tapi jika plugin simpan di Set/Map → gagal
- **Hasilnya: item ditransfer ke tier yang salah atau dianggap sama semua**

### Fix yang Dibutuhkan

Shim perlu mengimplementasikan `equals()` dan `hashCode()` yang mendelegasikan ke `delegate`:

```java
@Override
public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof SlimefunItem other)) return false;
    return delegate.equals(other.delegate);
}

@Override
public int hashCode() {
    return delegate.hashCode();
}
```

Atau metode yang lebih robust: **ganti shim menjadi caching wrapper** — reuse instance yang sama untuk delegate yang sama:

```java
private static final Map<io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem, SlimefunItem> cache = new WeakHashMap<>();

@Nullable
private static SlimefunItem wrap(@Nullable io.github.thebusybiscuit.slimefun5.api.items.SlimefunItem item) {
    if (item == null) return null;
    return cache.computeIfAbsent(item, SlimefunItem::new);
}
```

### Method yang di-shim saat ini
| Method | Status |
|---|---|
| `getByItem(ItemStack)` | ✅ syntax OK, ⚠️ tapi bikin instance baru |
| `getById(String)` | ✅ syntax OK, ⚠️ tapi bikin instance baru |
| `getId()` | ✅ |
| `getItem()` | ✅ |
| `isItem(ItemStack)` | ✅ |
| `equals(Object)` | ❌ **TIDAK di-shim** — pakai Object identity |
| `hashCode()` | ❌ **TIDAK di-shim** — pakai Object identity |

---

## 6. Item Merge / Stacking

### Severity
**INFORMATIONAL** (tidak ada bug, konfirmasi aman)

### Detail
Setelah investigasi menyeluruh, **item berbeda ID tidak akan merge di `findNextRecipe`**:

`SlimefunUtils.isItemSimilar()` (baris 368-371):
```java
SlimefunItem sf_sfitem = SlimefunItem.getByItem(sfitem);
SlimefunItem sf_item = SlimefunItem.getByItem(item);
if (sf_sfitem != null && sf_item != null) {
    if (!sf_sfitem.getId().equals(sf_item.getId())) {
        return false;  // <-- BERBEDA ID = FALSE
    }
}
```

- `BLISTERING_INGOT` (33%) ID = `"BLISTERING_INGOT"`
- `BLISTERING_INGOT_2` (66%) ID = `"BLISTERING_INGOT_2"`
- `BLISTERING_INGOT_3` (100%) ID = `"BLISTERING_INGOT_3"`

Ketiganya disimpan di PDC (Persistent Data Container) item. `isItemSimilar` membaca PDC dan membandingkan ID. ID berbeda → return false. **Tidak ada kemungkinan salah match.**

### Tapi kenapa user mengalami merge?

Kemungkinan penyebab merge yang user alami disebabkan oleh **Bug #1 (output overflow)**: item hasil craft ilang/tertukar, atau **Bug #2 (dupe)**: item lama muncul lagi setelah block break, atau **Bug #4 (nama vanilla)**: item namanya berubah sehingga secara visual terlihat "merge" karena namanya jadi sama.

---

## Ringkasan Fix Prioritas

| Priority | Bug | File | Fix |
|---|---|---|---|
| **P0** | Output ilang (overflow) | `AContainer.java:368` | Validasi `InvUtils.fitAll` sebelum push; handle return value `pushItem` |
| **P0** | Dupe block break | `AContainer.java:71-87` | Tambah `inv.delete(location)` setelah `dropItems` |
| **P1** | Instant speed 10 | `ElectricGoldPan.java`, `ElectricDustWasher.java`, `PostSetup.java` | `Math.max(1, value / speed)` atau base time cukup besar |
| **P2** | Nama vanilla berubah | `ItemTranslationService.java:477-537` | Fallback safety net ke displayName yang sudah terbake |
| **P0** | Shim slimefun4 — instance baru tiap `getByItem()`, tidak ada `equals`/`hashCode`, plugin eksternal gagal identifikasi tier | `io/github/thebusybiscuit/slimefun4/api/items/SlimefunItem.java` | Cache + implementasi `equals()` & `hashCode()` delegate |
