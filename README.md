# ToolMods

**Server-side tool modifications plugin for Paper 1.21.11**

Players can buy permanent modifications for their tools, weapons, and armor via a GUI shop. Mods are applied to the held item and stored in the item's PersistentDataContainer (PDC). Each mod can be toggled on/off per player via a management GUI. Mods persist through smithing table upgrades, enchanting, anvil repairs, and Market trading.

---

## Requirements

- **Paper 1.21.11** (or compatible fork)
- **Java 21+**
- **Economy plugin**: MultiBank (preferred) or Vault

## Optional Dependencies

| Plugin | Purpose |
|--------|---------|
| **MultiBank** | Primary economy provider (async, long cents) |
| **Vault** | Fallback economy provider |
| **PlayerSettings** | Per-player language selection |
| **LandClaim** | Area-effect mod protection checks |
| **AntiXRAY** | Notified when area-effect mods break blocks to prevent fake ores |

---

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/mods` | Opens the mod shop GUI | — |
| `/mods manage` | Opens the mod manager for your held item | — |
| `/mods reload` | Reloads config and language files | `toolmods.admin` |

Alias: `/toolmods`

---

## Mod Catalog

### Pickaxe Mods

| Mod | Default Price | Prerequisite | Description |
|-----|--------------|--------------|-------------|
| **Area Mine 3×3** | $15,000 | — | Mines a 3×3 plane based on the face you hit. Sneaking disables. |
| **Area Mine 5×5** | $50,000 | Area Mine 3×3 | Mines a 5×5 plane. Overrides 3×3 when both present. |
| **Auto Smelt** | $8,000 | — | Automatically smelts drops (raw iron → ingot, cobble → stone, etc.). Disabled by Silk Touch. |
| **Telepathy** | $12,000 | — | Drops go directly to inventory. XP given directly. Overflow drops on ground. |
| **Vein Miner** | $1,600 | — | Mines all connected ore blocks (BFS). Activates while sneaking on ore blocks only. Max 64 blocks. |

### Axe Mods

| Mod | Default Price | Description |
|-----|--------------|-------------|
| **Tree Feller** | $800 | Chops entire tree (sneaking). Includes leaf blower + auto replant. Validates tree by checking for leaves. Max 128 blocks. |

### Shovel Mods

| Mod | Default Price | Prerequisite | Description |
|-----|--------------|--------------|-------------|
| **Excavator 3×3** | $12,000 | — | Digs a 3×3 area of shovel blocks. Sneaking disables. |
| **Excavator 5×5** | $40,000 | Excavator 3×3 | Digs a 5×5 area. Overrides 3×3. |
| **Path Maker** | $5,000 | — | Right-click converts a 3-wide strip to dirt path. |

### Hoe Mods

| Mod | Default Price | Prerequisite | Description |
|-----|--------------|--------------|-------------|
| **Harvester 3×3** | $10,000 | — | Harvests mature crops in a 3×3 area. Auto-replants. |
| **Harvester 5×5** | $35,000 | Harvester 3×3 | Harvests in a 5×5 area. |
| **Tiller** | $4,000 | — | Right-click tills a 3×3 area to farmland. |

### Sword Mods

| Mod | Default Price | Description |
|-----|--------------|-------------|
| **Lifesteal** | $30,000 | Heals 15% of damage dealt to hostile mobs/players. Capped at 4 HP/sec. |
| **Thunderstrike** | $30,000 | After 7 consecutive hits on the same target, summons lightning (+4 HP damage). 10s cooldown. |
| **Decapitator** | $15,000 | Players: 100% head drop. Mobs with vanilla heads: 5% chance. |
| **Evoker Fangs** | $25,000 | After 5 consecutive hits on the same target, spawns a line of evoker fangs (magic damage). 8s cooldown. |

### Spear Mods

| Mod | Default Price | Description |
|-----|--------------|-------------|
| **Cripple** | $6,000 | Jab: Slowness I for 3s. Charge: Slowness II for 4s. |
| **Bleed** | $8,000 | Hits cause bleeding — 1♥ every 2s for 6 seconds. Does not stack. |
| **Dash Impact** | $10,000 | Charge hits deal 40% AoE damage to nearby entities. |
| **Spear Sweep** | $12,000 | Crouch + jab hits all entities in a 180° arc. 60% damage to extra targets. |
| **Momentum** | $15,000 | Charge damage scales with distance traveled. +10% per block (max +100%). |
| **Evoker Fangs (Spear)** | $25,000 | Charge hits spawn a line of evoker fangs (magic damage). 8s cooldown. |
| **Riptide** | $20,000 | Right-click to launch forward like Riptide — no water needed. Costs 1 water bucket per use (free in rain). 8s cooldown. Grants fall damage immunity after launch. Skips if trident has vanilla Riptide. |
| **Loyalty+** | $15,000 | Trident returns twice as fast and damages enemies on the return path (4 HP). Requires Loyalty enchantment. Each entity hit once per return. |
| **Chain Lightning** | $25,000 | On hit (melee or thrown), lightning strikes the target and chains to up to 3 nearby enemies. Each chain deals 30% less damage. 6s cooldown. |
| **Anchor** | $18,000 | On hit (melee or thrown), grounds the target for 2s — Slowness V, no jumping, cancels elytra. Action bar countdown for anchored players. 10s cooldown. |
| **Poseidon's Call** | $30,000 | Throw trident into water to create a whirlpool pulling enemies toward impact point for 3s. 6-block radius. Only works in water. 12s cooldown. |

### Shield Mods

| Mod | Default Price | Description |
|-----|--------------|-------------|
| **Magnetic Shield** | $8,000 | While blocking, attracts and destroys nearby arrows within 2 blocks. |
| **Shield Bash** | $10,000 | Sprint while blocking to bash enemies. Deals 3♥ damage + knockback. Counterable by axe or spear. 5s cooldown. |
| **Phalanx** | $12,000 | While blocking, allies behind you in a 120° cone (3 blocks) gain Resistance I. |
| **Reflective Shield** | $18,000 | While blocking, reflect 25% of blocked damage to attacker. Works on melee and arrows. |

### Mace Mods

| Mod | Default Price | Description |
|-----|--------------|-------------|
| **Tremor** | $20,000 | Smash attacks apply Slowness II + Nausea to all nearby entities for 3 seconds. |
| **Seismic Slam** | $35,000 | Smash AoE expanded to 5 blocks. Deals 50% smash damage to all entities in radius. Does not break blocks. |
| **Graviton Pulse** | $40,000 | After smash, pulls all entities within 6 blocks toward impact point for 1.5 seconds. 15s cooldown. |
| **Meteor Strike** | $50,000 | Fall 15+ blocks before smash to set enemies on fire for 5 seconds. Fire trail during descent. |

### Bow / Crossbow Mods

| Mod | Default Price | Description |
|-----|--------------|-------------|
| **Homing Arrow** | $25,000 | Arrows gently curve toward nearest hostile within 3 blocks. Disabled with Multishot. |
| **Explosive Arrow** | $18,000 | Arrows explode on impact (radius 2, 5♥ center damage, entity only). 2s cooldown. |

### Elytra Mods

| Mod | Default Price | Description |
|-----|--------------|-------------|
| **Firework Efficiency** | $8,000 | Firework rockets used during elytra flight last 50% longer. Passive. |
| **Safe Landing** | $12,000 | 75% fall damage reduction while wearing elytra. Auto Slow Falling when descending fast near ground. 30s cooldown on auto-save. |
| **Aerodynamic** | $15,000 | 30% less speed decay while gliding. Passive — always active while gliding. |
| **Boost** | $20,000 | Crouch while gliding for a speed boost (like a free firework). White trail particles. 8s cooldown. |
| **Phantom Cloak** | $25,000 | After 3 seconds of continuous gliding, become invisible. Hides nametag. Ends on land, damage, or attack. |
| **Sonic Boom** | $35,000 | At 30+ blocks/sec, entities within 3 blocks of flight path take 2♥ damage + sideways knockback. Does not activate in PvP-protected claims. |

### Helmet Mods

| Mod | Default Price | Description |
|-----|--------------|-------------|
| **Night Owl** | $30,000 | Permanent Night Vision while helmet is equipped. No potion particles. |
| **Aqua Lung** | $25,000 | Permanent Water Breathing while helmet is equipped. No potion particles. |
| **Hunter's Sight** | $20,000 | Hostile mobs within 15 blocks are highlighted with a red particle marker. Does not highlight players. |

### Chestplate Mods

| Mod | Default Price | Description |
|-----|--------------|-------------|
| **Vitality** | $30,000 | +2 extra hearts (4 HP) while chestplate is equipped. Health capped on removal. |
| **Last Stand** | $30,000 | Below 3 hearts: gain Resistance II + Strength I for 5 seconds. 60s cooldown. |
| **Thorns Aura** | $18,000 | Melee attackers always take 1♥ damage back. Does not stack with Thorns enchant. |

### Leggings Mods

| Mod | Default Price | Description |
|-----|--------------|-------------|
| **Swift** | $20,000 | Permanent Speed I while leggings are equipped. Stacks with beacons/potions. |
| **Dodger** | $35,000 | 20% chance to completely avoid melee or projectile attacks. 1s internal cooldown. |

### Boots Mods

| Mod | Default Price | Description |
|-----|--------------|-------------|
| **Featherweight** | $10,000 | 50% fall damage reduction. Stacks with Feather Falling. |
| **Double Jump** | $55,000 | One extra jump while in the air. 5s cooldown. Cloud puff particles. |
| **Frost Walker Plus** | $20,000 | Enhanced Frost Walker: 4-block radius, ice lasts 10s. Speed I while on ice. |

### Universal Mods (any tool/weapon/armor)

| Mod | Default Price | Description |
|-----|--------------|-------------|
| **Soulbound** | $100,000 | Item is preserved on death and returned on respawn. Persists across server restarts. |
| **Experienced** | $6,000 | +50% XP from mining and killing with this tool. |
| **Unbreakable** | $500,000 | Zero durability loss — ever. |

---

## How Mods Work

### Storage (PDC)

Each mod is stored as a `NamespacedKey` on the item:

```java
NamespacedKey key = new NamespacedKey(plugin, "mod_area_mine_3x3");
meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
```

Toggle state uses a `_disabled` suffix key. If `mod_area_mine_3x3_disabled` exists, the mod is inactive but still on the item.

### Lore Display

Mods appear in a dedicated section below existing lore:

```
[existing lore]
§8──────────────
§6⚙ Mods:
§7• Area Mine 3×3
§7• Auto Smelt
§7• ✘ Telepathy (disabled)
```

Lore is purely visual — PDC is the source of truth and is rebuilt automatically.

### Mod Interactions

- **Silk Touch + Auto Smelt**: Silk Touch takes priority, Auto Smelt is skipped
- **Fortune + Auto Smelt**: Fortune applies to drop count, then Auto Smelt converts material type
- **Vein Miner + Area Mine**: Mutually exclusive. Vein Miner on ores (sneaking), Area Mine on non-ores (not sneaking)
- **5×5 + 3×3**: Higher tier always wins when both are present
- **Unbreaking + Unbreakable**: Unbreakable cancels all durability damage; Unbreaking is respected when Unbreakable is absent
- **Thorns Aura + Thorns**: Uses whichever value is higher, does not stack

### Cooldown System

Mods with cooldowns display remaining time on the player's action bar:
```
⏳ Cooldown | Explosive Arrow ends in 1.3s
```
If multiple cooldowns are active, the one expiring soonest is shown.

### Multi-Tick Processing

Area-effect mods (Area Mine, Excavator, Vein Miner, Tree Feller) process ~10 blocks per tick to prevent lag spikes. Operations cancel automatically if the tool breaks mid-process.

---

## GUI System

### Main Menu (`/mods`)

6-row chest with category buttons:

```
Row 0:  ····  [Player Head]  ····
Row 1:  [Pickaxe] · [Axe] · [Shovel] · [Hoe] · [Sword]
Row 2:  [Spear] · [Shield] · [Mace] · [Bow] · [Universal]
Row 3:  [Helmet] · [Chestplate] · [Leggings] · [Boots] · [Elytra]
Row 4:  ···· [My Mods] ····
Row 5:  ···· [Close] ····
```

### Category Menu

Shows all mods in a category. Each mod item displays:
- Name and description
- Price
- Status: **Click to buy** / **✔ Owned** / **✘ Requires [prerequisite]**

Click to purchase — opens a **confirmation GUI** (1-row) before withdrawing money. Green wool to confirm, red wool to cancel.

### My Mods (`/mods manage`)

Shows all mods on the currently held item:
- **Green wool** = enabled (click to disable)
- **Red wool** = disabled (click to enable)

---

## Configuration

### config.yml

```yaml
language: en_US
debug: false

economy:
  mode: auto              # auto, multibank, or vault
  priority: [MultiBank, Vault]
  currency-symbol: "$"
  currency-format: "#,##0.00"

mods:
  mod-sell-bonus-multiplier: 0.10   # 10% of mod cost added to sell value

  pickaxe:
    area-mine-3x3: { enabled: true, price: 15000 }
    vein-miner: { enabled: true, price: 1600, max-blocks: 64 }
    # ... all mods have enabled + price

  sword:
    lifesteal: { heal-percent: 0.15, max-heal-per-second: 4.0 }
    thunderstrike: { hits-required: 7, cooldown-seconds: 10 }
    evoker-fangs: { hits-required: 5, cooldown-seconds: 8 }

  spear:
    cripple: { jab-slowness-level: 0, charge-slowness-level: 1 }
    bleed: { damage-per-tick: 2.0, tick-interval-seconds: 2, duration-seconds: 6 }
    momentum: { percent-per-block: 0.10, max-bonus-percent: 1.0 }
    riptide: { launch-power: 2.5, cooldown-seconds: 8, free-in-rain: true, fall-immunity-seconds: 5 }
    loyalty-plus: { return-speed-multiplier: 2.0, return-damage: 4.0, return-hit-radius: 1.5 }
    chain-lightning: { max-chains: 3, chain-radius: 5.0, chain-start-multiplier: 0.70, chain-reduction-per-jump: 0.70, cooldown-seconds: 6 }
    anchor: { duration-seconds: 2, slowness-level: 4, cooldown-seconds: 10 }
    poseidons-call: { pull-radius: 6.0, pull-duration-seconds: 3, pull-strength: 0.3, cooldown-seconds: 12 }

  shield:
    shield-bash: { damage: 6.0, cooldown-seconds: 5 }
    reflective-shield: { reflect-percent: 0.25 }

  mace:
    tremor: { slowness-level: 1, slowness-duration-seconds: 3 }
    seismic-slam: { radius: 5.0, aoe-damage-percent: 0.50 }
    graviton-pulse: { radius: 6.0, pull-duration-seconds: 1.5, cooldown-seconds: 15 }
    meteor-strike: { min-fall-blocks: 15, fire-duration-seconds: 5 }

  armor:
    helmet:
      night-owl: { enabled: true, price: 30000 }
      aqua-lung: { enabled: true, price: 25000 }
      hunters-sight: { radius: 15, update-interval-ticks: 40 }
    chestplate:
      vitality: { extra-health: 4.0 }
      last-stand: { health-threshold: 6.0, duration-seconds: 5, cooldown-seconds: 60 }
      thorns-aura: { reflect-damage: 2.0 }
    leggings:
      swift: { enabled: true, price: 20000 }
      dodger: { dodge-chance: 0.20, internal-cooldown-seconds: 1 }
    boots:
      featherweight: { reduction-percent: 0.50 }
      double-jump: { cooldown-seconds: 5, jump-velocity-multiplier: 0.80 }
      frost-walker-plus: { radius: 4, ice-duration-seconds: 10 }

  elytra:
    firework-efficiency: { enabled: true, price: 8000, efficiency-multiplier: 1.5 }
    safe-landing: { enabled: true, price: 12000, damage-reduction: 0.75, auto-slow-fall-cooldown-seconds: 30 }
    aerodynamic: { enabled: true, price: 15000, drag-reduction: 0.30 }
    boost: { enabled: true, price: 20000, speed-multiplier: 1.5, cooldown-seconds: 8 }
    phantom-cloak: { enabled: true, price: 25000, activation-delay-seconds: 3 }
    sonic-boom: { enabled: true, price: 35000, speed-threshold: 1.5, damage: 4.0, radius: 3.0 }

  universal:
    experienced: { xp-multiplier: 1.5 }
```

### Language Files

Located in `plugins/ToolMods/lang/`:
- `en_US.yml` — English (default)
- `pt_PT.yml` — Portuguese
- `fr_FR.yml` — French

All messages use **MiniMessage** formatting. Per-player language is supported via the PlayerSettings plugin.

---

## Integration

### Economy (MultiBank + Vault)

Dual-provider system with configurable priority. MultiBank is preferred (async API, long cents). Vault is the fallback (sync, double-based). The plugin auto-detects which is available.

### LandClaim

All area-effect mods check LandClaim protection for each block. Protected blocks are silently skipped. If LandClaim is not installed, all blocks are allowed.

### Market

Items with mods retain their PDC data through Market buy/sell. Mod lore is visible in listings. The Server Shop sell value can include a mod bonus: `base price + Σ(mod price × mod-sell-bonus-multiplier)`.

### PlayerSettings

Per-player language codes are queried from PlayerSettings on every message. Falls back to the global `language:` setting in config.yml.

---

## Project Structure

```
src/main/java/pt/henrique/toolmods/
├── ToolMods.java                        # Main plugin class
├── command/
│   └── ModsCommand.java                # /mods command (Brigadier)
├── config/
│   ├── ConfigManager.java              # config.yml reader
│   └── LangManager.java               # Per-player language system
├── economy/
│   ├── EconomyProvider.java            # Provider interface
│   ├── EconomyManager.java            # Dual-provider manager
│   ├── MultiBankProvider.java          # MultiBank direct imports
│   └── VaultProvider.java              # Vault via reflection
├── gui/
│   ├── MainMenuGui.java                # Category selection (15 categories)
│   ├── CategoryGui.java                # Mod shop per category
│   ├── ConfirmationGui.java            # Purchase confirmation (1-row)
│   ├── MyModsGui.java                  # Toggle mods on held item
│   └── GuiListener.java                # Click handler
├── hook/
│   ├── AntiXrayHook.java               # Optional AntiXRAY integration
│   ├── LandClaimHook.java              # Optional LandClaim integration
│   └── PlayerSettingsHook.java         # Optional PlayerSettings integration
├── listener/
│   ├── AreaMineListener.java           # Area Mine 3×3 / 5×5
│   ├── AutoSmeltListener.java          # Auto Smelt utility
│   ├── BlockBreakHelper.java           # Shared: durability, area calc, drops
│   ├── DecapitatorListener.java        # Decapitator head drops
│   ├── ExcavatorListener.java          # Excavator 3×3 / 5×5
│   ├── ExperiencedListener.java        # XP multiplier
│   ├── ExplosiveArrowListener.java     # Explosive Arrow
│   ├── HarvesterListener.java          # Harvester 3×3 / 5×5
│   ├── HomingArrowListener.java        # Homing Arrow tracking
│   ├── LifestealListener.java          # Lifesteal heal on hit
│   ├── PathMakerListener.java          # Path Maker 3-wide
│   ├── SoulboundListener.java          # Death item preservation
│   ├── TelepathyListener.java          # Inventory insertion utility
│   ├── ThunderstrikeListener.java      # Thunderstrike combo + lightning
│   ├── TillerListener.java             # Tiller 3×3
│   ├── TreeFellerListener.java         # Tree Feller + leaf blower + replant
│   ├── UnbreakableListener.java        # Cancel durability damage
│   ├── VeinMinerListener.java          # Vein Miner BFS
│   ├── EvokerFangsListener.java        # Evoker Fangs (Sword)
│   ├── CrippleListener.java            # Cripple (Spear)
│   ├── BleedListener.java              # Bleed (Spear)
│   ├── DashImpactListener.java         # Dash Impact (Spear)
│   ├── SpearSweepListener.java         # Spear Sweep
│   ├── MomentumListener.java           # Momentum (Spear)
│   ├── SpearEvokerFangsListener.java   # Evoker Fangs (Spear)
│   ├── RiptideListener.java            # Riptide (Trident)
│   ├── LoyaltyPlusListener.java        # Loyalty+ (Trident)
│   ├── ChainLightningListener.java     # Chain Lightning (Trident)
│   ├── AnchorListener.java             # Anchor (Trident)
│   ├── PoseidonsCallListener.java      # Poseidon's Call (Trident)
│   ├── MagneticShieldListener.java     # Magnetic Shield
│   ├── ShieldBashListener.java         # Shield Bash
│   ├── PhalanxListener.java            # Phalanx
│   ├── ReflectiveShieldListener.java   # Reflective Shield
│   ├── TremorListener.java             # Tremor (Mace)
│   ├── SeismicSlamListener.java        # Seismic Slam (Mace)
│   ├── GravitonPulseListener.java      # Graviton Pulse (Mace)
│   ├── MeteorStrikeListener.java       # Meteor Strike (Mace)
│   ├── NightOwlListener.java           # Night Owl (Helmet)
│   ├── AquaLungListener.java           # Aqua Lung (Helmet)
│   ├── HuntersSightListener.java       # Hunter's Sight (Helmet)
│   ├── VitalityListener.java           # Vitality (Chestplate)
│   ├── LastStandListener.java          # Last Stand (Chestplate)
│   ├── ThornsAuraListener.java         # Thorns Aura (Chestplate)
│   ├── SwiftListener.java              # Swift (Leggings)
│   ├── DodgerListener.java             # Dodger (Leggings)
│   ├── FeatherweightListener.java      # Featherweight (Boots)
│   ├── DoubleJumpListener.java         # Double Jump (Boots)
│   └── FrostWalkerPlusListener.java    # Frost Walker Plus (Boots)
│   ├── FireworkEfficiencyListener.java # Firework Efficiency (Elytra)
│   ├── SafeLandingListener.java        # Safe Landing (Elytra)
│   ├── AerodynamicListener.java        # Aerodynamic (Elytra)
│   ├── BoostListener.java              # Boost (Elytra)
│   ├── PhantomCloakListener.java       # Phantom Cloak (Elytra)
│   └── SonicBoomListener.java          # Sonic Boom (Elytra)
├── mod/
│   ├── ModType.java                    # Enum of all mods (50+)
│   ├── ModUtils.java                   # PDC read/write + lore management
│   └── ToolCategory.java              # Tool type matching (15 categories)
└── util/
    ├── CooldownManager.java            # Cooldown tracking + action bar display
    ├── ItemBuilder.java                # Fluent GUI item builder
    ├── SoundUtil.java                  # Sound utility
    └── SpearChargeTracker.java         # Spear jab vs charge detection
```

---

## Building

```bash
./gradlew build
```

Output: `build/libs/ToolMods-1.0.0.jar`

## License

All rights reserved. © Henrique
