/*
 * DesertRPGAndroid
 *
 * This program is a port of the Python text‑based desert RPG to a single
 * Java class suitable for running in an Android IDE that supports plain
 * Java projects (for example AIDE on Android).  It preserves the core
 * mechanics of the original: the player explores a procedurally generated
 * desert, fights enemies, loots caches, meets NPCs, finds healing oases
 * and faces a boss.  The grid size is configurable via MAP_WIDTH and
 * MAP_HEIGHT constants.  Beyond the direct translation, a handful of
 * extras were added:
 *
 *  • A new character class, Snajper (Sniper), with high damage and a
 *    special ability that deals triple damage once per fight.
 *  • A new tile type, T – Pułapka (trap).  Exploring a trap deals
 *    immediate damage but may drop a small amount of XP as consolation.
 *  • A scanning mechanic.  Players start with a few scan charges and may
 *    use the command "skanuj"/"scan" to reveal the contents of adjacent
 *    tiles.  Additional scan charges can be found in chests as items.
 *  • New items such as "Nano tarcza" (boosts HP) and "Skanner"
 *    (grants extra scanning charges) were added to the loot tables.
 *
 * To play the game in an Android IDE, create a new Java console
 * application, add this file, and run the `main` method.  The game
 * operates entirely in the console using standard input and output.
 */

import java.util.*;

public class DesertRPGAndroid {
    // Map dimensions – adjust to make the world larger or smaller.
    private static final int MAP_WIDTH = 8;
    private static final int MAP_HEIGHT = 8;

    // Random generator used throughout the game.
    private static final Random RANDOM = new Random();

    /**
     * Representation of an item that can be stored and used.  Each item has
     * a name, description, effect type and numeric value.  The effect type
     * dictates what happens when the item is applied to a player: healing
     * HP, increasing attack damage, boosting dodge chance, raising max HP,
     * adding scan charges, etc.
     */
    private static class Item {
        final String name;
        final String description;
        final String effectType;
        final int value;

        Item(String name, String description, String effectType, int value) {
            this.name = name;
            this.description = description;
            this.effectType = effectType;
            this.value = value;
        }

        /**
         * Apply the item's effect to the given player.  Returns a message
         * describing the outcome.  Consumes the item from the player's
         * inventory; inventory management is handled externally.
         */
        String apply(Player player) {
            switch (effectType) {
                case "heal": {
                    int healed = Math.min(value, player.hpMax - player.hp);
                    player.hp += healed;
                    return "Twoje rany trochę się zabliźniły. +" + healed + " HP.";
                }
                case "attack": {
                    player.atkMin += value;
                    player.atkMax += value;
                    return "Czujesz przypływ mocy. Atak wzrósł o " + value + ".";
                }
                case "dodge": {
                    player.dodgeChance = Math.min(95, player.dodgeChance + value);
                    return "Twoje zmysły wyostrzyły się. Szansa uniku wzrasta o " + value + "%.";
                }
                case "hp_boost": {
                    player.hpMax += value;
                    player.hp += value;
                    return "Czujesz, jak twoje ciało się wzmocniło. +" + value + " do maksymalnego HP.";
                }
                case "scan": {
                    player.scanCharges += value;
                    return "Twój skaner migocze. Otrzymujesz " + value + " ładunek/ładunki skanu.";
                }
                default:
                    return "Nic się nie stało. To chyba tylko złom.";
            }
        }
    }

    /**
     * An adversary encountered during combat.  Each enemy has hit points,
     * attack range, dodge chance and an XP reward upon defeat.  The flavour
     * string is displayed at the start of combat to set the scene.
     */
    private static class Enemy {
        final String name;
        int hp;
        int atkMin;
        int atkMax;
        int dodgeChance;
        final int xpReward;
        final String flavour;

        Enemy(String name, int hp, int atkMin, int atkMax, int dodgeChance,
              int xpReward, String flavour) {
            this.name = name;
            this.hp = hp;
            this.atkMin = atkMin;
            this.atkMax = atkMax;
            this.dodgeChance = dodgeChance;
            this.xpReward = xpReward;
            this.flavour = flavour;
        }

        boolean isAlive() {
            return hp > 0;
        }

        int attack() {
            return RANDOM.nextInt(atkMax - atkMin + 1) + atkMin;
        }
    }

    /**
     * Representation of the player's character and statistics.  The class
     * choice determines the starting stats and inventory.  The player also
     * tracks XP and leveling, along with a limited number of scan charges
     * used to reveal adjacent tiles.
     */
    private static class Player {
        String className;
        int hpMax;
        int hp;
        int atkMin;
        int atkMax;
        int dodgeChance;
        final Map<String, Integer> inventory = new LinkedHashMap<>();
        int level = 1;
        int xp = 0;
        int xpToNext = 100;
        int scanCharges = 3; // starting scan charges

        Player(String cls) {
            String lower = cls.toLowerCase(Locale.ROOT);
            if (lower.equals("wojownik") || lower.equals("woj") || lower.equals("warrior")) {
                className = "Wojownik";
                hpMax = hp = 120;
                atkMin = 8;
                atkMax = 14;
                dodgeChance = 10;
                inventory.put("Mały medykit", 1);
            } else if (lower.equals("technik") || lower.equals("tech") || lower.equals("technician")) {
                className = "Technik";
                hpMax = hp = 90;
                atkMin = 6;
                atkMax = 10;
                dodgeChance = 20;
                inventory.put("Adrenalina", 1);
            } else if (lower.equals("snajper") || lower.equals("sniper") || lower.equals("strzelec")) {
                className = "Snajper";
                hpMax = hp = 80;
                atkMin = 12;
                atkMax = 20;
                dodgeChance = 15;
                inventory.put("Skanner", 1);
            } else {
                className = "Nomada";
                hpMax = hp = 100;
                atkMin = 7;
                atkMax = 12;
                dodgeChance = 15;
                inventory.put("Suchary", 2);
            }
        }

        boolean isAlive() {
            return hp > 0;
        }

        int attack() {
            return RANDOM.nextInt(atkMax - atkMin + 1) + atkMin;
        }

        void gainXp(int amount) {
            xp += amount;
            while (xp >= xpToNext) {
                xp -= xpToNext;
                level++;
                xpToNext = (int) (xpToNext * 1.3) + 50;
                // Increase stats on level up
                int hpIncrease = RANDOM.nextInt(8, 16); // 8–15 inclusive
                hpMax += hpIncrease;
                hp = hpMax;
                int atkIncrease = RANDOM.nextInt(1, 4); // 1–3 inclusive
                atkMin += atkIncrease;
                atkMax += atkIncrease;
                dodgeChance = Math.min(95, dodgeChance + 2);
                System.out.println("Awansujesz na poziom " + level + "! Czujesz przypływ sił: +" + hpIncrease + " HP max, +" + atkIncrease + " do obrażeń, +2% do uniku.");
            }
        }

        void addItem(Item item) {
            inventory.put(item.name, inventory.getOrDefault(item.name, 0) + 1);
        }

        /**
         * Attempt to use an item by name.  Returns a message describing the
         * effect or null if the item is not found.  Inventory counts are
         * decremented and removed when reaching zero.
         */
        String useItem(String name) {
            String found = null;
            for (String key : new ArrayList<>(inventory.keySet())) {
                if (key.equalsIgnoreCase(name)) {
                    found = key;
                    break;
                }
            }
            if (found == null) return null;
            int count = inventory.get(found);
            if (count <= 0) return null;
            // Build a transient database of item definitions.  In a
            // production app this would come from a static registry.
            Map<String, Item> itemDb = new HashMap<>();
            itemDb.put("Mały medykit", new Item("Mały medykit", "Mały pakiet medyczny przywracający nieco zdrowia.", "heal", 30));
            itemDb.put("Adrenalina", new Item("Adrenalina", "Zastrzyk adrenaliny zwiększający twój atak.", "attack", 2));
            itemDb.put("Suchary", new Item("Suchary", "Czerstwe ciastka, ale lepsze to niż nic.", "heal", 20));
            itemDb.put("Pancerz Fenrira", new Item("Pancerz Fenrira", "Kamizelka wzmacniająca twoją szansę na unik.", "dodge", 5));
            itemDb.put("Eliksir mocy", new Item("Eliksir mocy", "Tajemniczy płyn zwiększający atak.", "attack", 3));
            itemDb.put("Mega medykit", new Item("Mega medykit", "Duży pakiet medyczny przywracający dużo zdrowia.", "heal", 70));
            // Added items
            itemDb.put("Moduł bojowy", new Item("Moduł bojowy", "Zaawansowany moduł zwiększający twój atak.", "attack", 5));
            itemDb.put("Kamizelka tytanowa", new Item("Kamizelka tytanowa", "Tytanowa zbroja zwiększająca twoje maksymalne zdrowie.", "hp_boost", 50));
            itemDb.put("Implant uniku", new Item("Implant uniku", "Cybernetyczny implant zwiększający szansę uniku.", "dodge", 10));
            itemDb.put("Nano tarcza", new Item("Nano tarcza", "Nadprzewodząca osłona zwiększająca twoje maksymalne HP.", "hp_boost", 30));
            itemDb.put("Skanner", new Item("Skanner", "Przenośne urządzenie zwiększające liczbę ładunków skanu.", "scan", 2));
            Item item = itemDb.getOrDefault(found, new Item(found, "Nieznany przedmiot.", "heal", 10));
            inventory.put(found, count - 1);
            if (inventory.get(found) <= 0) inventory.remove(found);
            return item.apply(this);
        }
    }

    /**
     * Generate a map filled with different terrain/event symbols.  The
     * distribution of cell types roughly scales up from the original Python
     * implementation.  We include an extra trap (T) tile type here, which
     * damages the player when explored.
     */
    private static char[][] generateMap() {
        int total = MAP_WIDTH * MAP_HEIGHT;
        int numBattle = Math.max(5, (int) (total * 0.23)); // W – combat
        int numLoot   = Math.max(3, (int) (total * 0.15)); // L – loot
        int numNpc    = Math.max(2, (int) (total * 0.12)); // N – NPC
        int numBoss   = Math.max(1, (int) (total * 0.03)); // B – boss
        int numOasis  = Math.max(1, (int) (total * 0.07)); // O – oasis
        int numTrap   = Math.max(1, (int) (total * 0.05)); // T – trap
        int numEmpty  = total - (numBattle + numLoot + numNpc + numBoss + numOasis + numTrap);
        List<Character> cells = new ArrayList<>(total);
        for (int i = 0; i < numBattle; i++) cells.add('W');
        for (int i = 0; i < numLoot; i++) cells.add('L');
        for (int i = 0; i < numNpc; i++) cells.add('N');
        for (int i = 0; i < numBoss; i++) cells.add('B');
        for (int i = 0; i < numOasis; i++) cells.add('O');
        for (int i = 0; i < numTrap; i++) cells.add('T');
        for (int i = 0; i < numEmpty; i++) cells.add('.');
        Collections.shuffle(cells, RANDOM);
        // Ensure the starting position (centre) is safe (not a boss or trap)
        int startX = MAP_WIDTH / 2;
        int startY = MAP_HEIGHT / 2;
        int startIndex = startY * MAP_WIDTH + startX;
        char startVal = cells.get(startIndex);
        if (startVal == 'B' || startVal == 'W' || startVal == 'T') {
            for (int i = 0; i < cells.size(); i++) {
                char c = cells.get(i);
                if ((c == '.' || c == 'L' || c == 'N' || c == 'O') && i != startIndex) {
                    cells.set(i, startVal);
                    cells.set(startIndex, c);
                    break;
                }
            }
        }
        char[][] map = new char[MAP_HEIGHT][MAP_WIDTH];
        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
                map[y][x] = cells.get(y * MAP_WIDTH + x);
            }
        }
        return map;
    }

    /**
     * Generate an enemy appropriate for the tile type.  Boss fights come in
     * two flavours: the first time you encounter a boss tile, you fight a
     * Fenrir commander; subsequent boss tiles spawn a weaker guard.  This
     * behaviour is controlled externally via the bossFought flag.
     */
    private static Enemy generateEnemy(char tileType, boolean bossFought) {
        if (tileType == 'B') {
            if (bossFought) {
                return new Enemy("Fenrir Strażnik", 80, 10, 14, 15, 80,
                        "Strażnik z Fenrira macha swoim karabinem, gotów cię zakończyć.");
            }
            return new Enemy("Fenrir Dowódca", 150, 14, 20, 20, 200,
                    "Dowódca Fenrira patrzy na ciebie z pogardą. To nie będzie łatwa walka.");
        } else if (tileType == 'W') {
            List<Enemy> enemies = Arrays.asList(
                    new Enemy("Piaskowy robak", 60, 6, 12, 5, 50,
                            "Ogromny robak wynurza się z piasku, jego zęby błyszczą w słońcu."),
                    new Enemy("Fenrir Zwiadowca", 70, 8, 12, 10, 60,
                            "Zwiadowca Fenrira wyskakuje zza wydmy, strzelając seriami."),
                    new Enemy("Nomadzki łupieżca", 50, 5, 10, 20, 45,
                            "Łupieżca Nomadów zdziera maskę, szyderczo się uśmiechając."),
                    new Enemy("Harkonnen Berserker", 80, 9, 15, 5, 70,
                            "Berserker Harkonnenów krzyczy coś niezrozumiałego i rzuca się na ciebie."),
                    new Enemy("Rój nanodronów", 40, 4, 8, 25, 40,
                            "Chmura nanodronów unosi się, gotowa cię przegryźć na kawałki.")
            );
            return enemies.get(RANDOM.nextInt(enemies.size()));
        } else {
            return new Enemy("Zmutowany szakal", 55, 7, 11, 10, 50,
                    "Zmutowany szakal warczy groźnie.");
        }
    }

    /**
     * Return a random item from the basic loot table.  Additional items
     * introduced in the chest drop function are not part of this table.
     */
    private static Item generateLoot() {
        List<Item> lootTable = Arrays.asList(
                new Item("Mały medykit", "Mały pakiet medyczny przywracający nieco zdrowia.", "heal", 30),
                new Item("Adrenalina", "Zastrzyk adrenaliny zwiększający twój atak.", "attack", 2),
                new Item("Pancerz Fenrira", "Kamizelka wzmacniająca twoją szansę na unik.", "dodge", 5),
                new Item("Suchary", "Czerstwe ciastka, ale lepsze to niż nic.", "heal", 20),
                new Item("Eliksir mocy", "Tajemniczy płyn zwiększający atak.", "attack", 3),
                new Item("Mega medykit", "Duży pakiet medyczny przywracający dużo zdrowia.", "heal", 70)
        );
        return lootTable.get(RANDOM.nextInt(lootTable.size()));
    }

    // Define loot pools for post‑combat chests.  Items are categorised by
    // rarity: common, rare and epic.  Some new items grant scan charges.
    private static final List<Item> COMMON_CHEST_ITEMS = Arrays.asList(
            new Item("Mały medykit", "Mały pakiet medyczny przywracający nieco zdrowia.", "heal", 30),
            new Item("Suchary", "Czerstwe ciastka, ale lepsze to niż nic.", "heal", 20),
            new Item("Adrenalina", "Zastrzyk adrenaliny zwiększający twój atak.", "attack", 2),
            new Item("Skanner", "Przenośne urządzenie zwiększające liczbę ładunków skanu.", "scan", 1)
    );
    private static final List<Item> RARE_CHEST_ITEMS = Arrays.asList(
            new Item("Pancerz Fenrira", "Kamizelka wzmacniająca twoją szansę na unik.", "dodge", 5),
            new Item("Eliksir mocy", "Tajemniczy płyn zwiększający atak.", "attack", 3),
            new Item("Mega medykit", "Duży pakiet medyczny przywracający dużo zdrowia.", "heal", 70),
            new Item("Nano tarcza", "Nadprzewodząca osłona zwiększająca twoje maksymalne HP.", "hp_boost", 30)
    );
    private static final List<Item> EPIC_CHEST_ITEMS = Arrays.asList(
            new Item("Moduł bojowy", "Zaawansowany moduł zwiększający twój atak.", "attack", 5),
            new Item("Kamizelka tytanowa", "Tytanowa zbroja zwiększająca twoje maksymalne zdrowie.", "hp_boost", 50),
            new Item("Implant uniku", "Cybernetyczny implant zwiększający szansę uniku.", "dodge", 10),
            new Item("Skanner", "Przenośne urządzenie zwiększające liczbę ładunków skanu.", "scan", 2)
    );

    /**
     * Randomly select an item from one of the chest pools.  Common items
     * occur 60% of the time, rare items 30% and epic items 10%.
     */
    private static Map.Entry<Item, String> dropChest() {
        double roll = RANDOM.nextDouble();
        if (roll < 0.6) {
            Item item = COMMON_CHEST_ITEMS.get(RANDOM.nextInt(COMMON_CHEST_ITEMS.size()));
            return new AbstractMap.SimpleEntry<>(item, "zwykły");
        } else if (roll < 0.9) {
            Item item = RARE_CHEST_ITEMS.get(RANDOM.nextInt(RARE_CHEST_ITEMS.size()));
            return new AbstractMap.SimpleEntry<>(item, "rzadki");
        } else {
            Item item = EPIC_CHEST_ITEMS.get(RANDOM.nextInt(EPIC_CHEST_ITEMS.size()));
            return new AbstractMap.SimpleEntry<>(item, "epicki");
        }
    }

    /**
     * Handle interaction with an NPC.  Depending on the type, the player
     * might trade an item, receive a blessing or a gift, or simply hear a
     * story.  NPCs disappear after one interaction.
     */
    private static void npcInteraction(Player player, Scanner scanner) {
        List<String> npcTypes = Arrays.asList(
                "Wędrowny handlarz",
                "Nomadzka szamanka",
                "Stary cyberwłóczęga",
                "Fenrir informator"
        );
        String npc = npcTypes.get(RANDOM.nextInt(npcTypes.size()));
        System.out.println("Spotykasz kogoś: " + npc + ".");
        if (npc.equals("Wędrowny handlarz")) {
            System.out.println("\"Mam to i owo. Nie za darmo, ale za drobny przysługę mogę wymienić.\"");
            if (player.inventory.isEmpty()) {
                System.out.println("Nie masz czym handlować. Handlarz macha ręką i odchodzi.");
                return;
            }
            System.out.println("Możesz oddać jeden przedmiot ze swojego ekwipunku w zamian za losowy nowy.");
            while (true) {
                System.out.println("Co chcesz oddać? (wpisz nazwę lub 'anuluj')");
                System.out.println("Twoje przedmioty:");
                for (Map.Entry<String, Integer> entry : player.inventory.entrySet()) {
                    System.out.println(" - " + entry.getKey() + " x" + entry.getValue());
                }
                System.out.print("> ");
                String choice = scanner.nextLine().trim();
                if (choice.equalsIgnoreCase("anuluj")) {
                    System.out.println("Rezygnujesz z wymiany. Handlarz wzrusza ramionami.");
                    return;
                }
                String selected = null;
                for (String key : player.inventory.keySet()) {
                    if (key.equalsIgnoreCase(choice)) {
                        selected = key;
                        break;
                    }
                }
                if (selected == null) {
                    System.out.println("Nie masz takiego przedmiotu.");
                    continue;
                }
                // Remove one quantity and give a random new item (not necessarily distinct)
                int count = player.inventory.get(selected);
                player.inventory.put(selected, count - 1);
                if (player.inventory.get(selected) <= 0) player.inventory.remove(selected);
                Item newItem = generateLoot();
                System.out.println("Handlarz zabiera " + selected + ", w zamian otrzymujesz " + newItem.name + ".");
                player.addItem(newItem);
                return;
            }
        } else if (npc.equals("Nomadzka szamanka")) {
            System.out.println("Szamanka rzuca runami na piasek. \"Los jest kapryśny\", mówi, \"ale podaruję ci błogosławieństwo\".");
            int roll = RANDOM.nextInt(3);
            if (roll == 0) {
                int bonus = RANDOM.nextInt(10, 31);
                player.hpMax += bonus;
                player.hp += bonus;
                System.out.println("Czujesz, jak twoje ciało napełnia się energią. +" + bonus + " do maksymalnego HP.");
            } else if (roll == 1) {
                int bonus = 2;
                player.atkMin += bonus;
                player.atkMax += bonus;
                System.out.println("Szamanka dotyka twojej broni. +" + bonus + " do obrażeń.");
            } else {
                int bonus = 5;
                player.dodgeChance = Math.min(95, player.dodgeChance + bonus);
                System.out.println("Na twoje czoło trafia znak. +" + bonus + "% do uniku.");
            }
        } else if (npc.equals("Stary cyberwłóczęga")) {
            System.out.println("Zgarbiona postać rozdrapuje wtyczki ze swojego karku. \"Kiedyś byłem kimś...\"");
            if (RANDOM.nextBoolean()) {
                Item item = generateLoot();
                System.out.println("Włóczęga wręcza ci " + item.name + ", uśmiechając się, po czym odchodzi.");
                player.addItem(item);
            } else {
                System.out.println("Włóczęga opowiada ci swoją tragiczną historię. Nic z tego nie wynika, ale czujesz się dziwnie. Czarny humor aż kapie.");
            }
        } else { // Fenrir informator
            System.out.println("Odziany w czarny płaszcz informator spogląda na ciebie uważnie.");
            System.out.println("\"Widziałem coś interesującego na północ stąd...\" – mówi cicho.");
            // The mechanic of revealing a tile north could be implemented here.
        }
    }

    /**
     * Render the map to the console, showing discovered cells and the player
     * position.  Undiscovered tiles are shown as '?' to preserve mystery.
     */
    private static void printMap(char[][] gameMap, int playerX, int playerY, Set<String> discovered) {
        System.out.println("Mapa pustkowi:");
        for (int y = 0; y < MAP_HEIGHT; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < MAP_WIDTH; x++) {
                if (x == playerX && y == playerY) {
                    row.append('🧍');
                } else if (discovered.contains(x + "," + y)) {
                    row.append(gameMap[y][x]);
                } else {
                    row.append('?');
                }
                if (x < MAP_WIDTH - 1) row.append(' ');
            }
            System.out.println(row.toString());
        }
        System.out.println();
    }

    /**
     * Run a combat loop.  Returns true if the player survives, false if
     * killed.  Special abilities are reset per combat via the specialUsed flag.
     */
    private static boolean combat(Player player, Enemy enemy, Scanner scanner) {
        System.out.println(enemy.flavour);
        boolean specialUsed = false;
        boolean blocked = false;
        int dodgeBoostTurns = 0;
        while (enemy.isAlive() && player.isAlive()) {
            System.out.println();
            System.out.println(enemy.name + " – HP: " + enemy.hp);
            System.out.println("Twój HP: " + player.hp + "/" + player.hpMax);
            System.out.println("Wybierz akcję: [atakuj] [lecz] [uciekaj] [blokuj] [specjalna]");
            System.out.print("> ");
            String cmd = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            if (cmd.equals("atakuj") || cmd.equals("atak") || cmd.equals("a")) {
                int dmg = player.attack();
                // Enemy dodge
                if (RANDOM.nextInt(100) < enemy.dodgeChance) {
                    System.out.println("Przeciwnik uniknął twojego ataku!");
                } else {
                    enemy.hp -= dmg;
                    System.out.println("Zadajesz " + dmg + " obrażeń.");
                }
                if (enemy.isAlive()) {
                    int effectiveDodge = player.dodgeChance + (dodgeBoostTurns > 0 ? 20 : 0);
                    if (RANDOM.nextInt(100) < effectiveDodge) {
                        System.out.println("Uniknąłeś ataku " + enemy.name + "!");
                    } else {
                        int enemyDmg = enemy.attack();
                        if (blocked) {
                            enemyDmg /= 2;
                            blocked = false;
                            System.out.println("Twoja blokada redukuje obrażenia przeciwnika.");
                        }
                        player.hp -= enemyDmg;
                        System.out.println(enemy.name + " zadaje ci " + enemyDmg + " obrażeń.");
                    }
                    if (dodgeBoostTurns > 0) dodgeBoostTurns--;
                }
            } else if (cmd.equals("lecz") || cmd.equals("heal") || cmd.equals("l")) {
                // Use first available healing item
                String healingItem = null;
                for (String key : player.inventory.keySet()) {
                    String keyLower = key.toLowerCase(Locale.ROOT);
                    if (keyLower.equals("mały medykit") || keyLower.equals("maly medykit") || keyLower.equals("mega medykit") || keyLower.equals("suchary")) {
                        healingItem = key;
                        break;
                    }
                }
                if (healingItem == null) {
                    System.out.println("Nie masz nic, czym mógłbyś się wyleczyć!");
                } else {
                    String effect = player.useItem(healingItem);
                    System.out.println(effect);
                }
                if (enemy.isAlive()) {
                    int effectiveDodge = player.dodgeChance + (dodgeBoostTurns > 0 ? 20 : 0);
                    if (RANDOM.nextInt(100) < effectiveDodge) {
                        System.out.println("Uniknąłeś ataku " + enemy.name + "!");
                    } else {
                        int dmg = enemy.attack();
                        if (blocked) {
                            dmg /= 2;
                            blocked = false;
                            System.out.println("Twoja blokada redukuje obrażenia przeciwnika.");
                        }
                        player.hp -= dmg;
                        System.out.println(enemy.name + " korzysta z okazji i zadaje ci " + dmg + " obrażeń.");
                    }
                    if (dodgeBoostTurns > 0) dodgeBoostTurns--;
                }
            } else if (cmd.equals("uciekaj") || cmd.equals("uciek") || cmd.equals("u") || cmd.equals("run")) {
                if (RANDOM.nextDouble() < 0.5) {
                    System.out.println("Udaje ci się uciec!");
                    return true;
                } else {
                    System.out.println("Nie udało się uciec!");
                    int effectiveDodge = player.dodgeChance + (dodgeBoostTurns > 0 ? 20 : 0);
                    if (RANDOM.nextInt(100) < effectiveDodge) {
                        System.out.println("Uniknąłeś ataku " + enemy.name + "!");
                    } else {
                        int dmg = enemy.attack();
                        if (blocked) {
                            dmg /= 2;
                            blocked = false;
                            System.out.println("Twoja blokada redukuje obrażenia przeciwnika.");
                        }
                        player.hp -= dmg;
                        System.out.println(enemy.name + " rani cię za " + dmg + " obrażeń.");
                    }
                    if (dodgeBoostTurns > 0) dodgeBoostTurns--;
                }
            } else if (cmd.equals("blokuj") || cmd.equals("block") || cmd.equals("b")) {
                blocked = true;
                System.out.println("Napinasz mięśnie i szykujesz się do zablokowania nadchodzącego ataku.");
                if (enemy.isAlive()) {
                    int effectiveDodge = player.dodgeChance + (dodgeBoostTurns > 0 ? 20 : 0);
                    if (RANDOM.nextInt(100) < effectiveDodge) {
                        System.out.println("Uniknąłeś ataku " + enemy.name + "!");
                        blocked = false;
                    } else {
                        int dmg = enemy.attack();
                        dmg /= 2;
                        player.hp -= dmg;
                        blocked = false;
                        System.out.println("Twój blok zmniejsza obrażenia do " + dmg + ".");
                    }
                    if (dodgeBoostTurns > 0) dodgeBoostTurns--;
                }
            } else if (cmd.equals("specjalna") || cmd.equals("umiejętność") || cmd.equals("umiejetnosc") || cmd.equals("skill") || cmd.equals("u")) {
                if (specialUsed) {
                    System.out.println("Już użyłeś swojej umiejętności w tej walce!");
                } else {
                    specialUsed = true;
                    if (player.className.equals("Wojownik")) {
                        int dmg = player.attack() * 2;
                        enemy.hp -= dmg;
                        System.out.println("Wpadasz w furię! Potężny cios zadaje " + dmg + " obrażeń.");
                    } else if (player.className.equals("Technik")) {
                        int reduction = 3;
                        enemy.atkMin = Math.max(1, enemy.atkMin - reduction);
                        enemy.atkMax = Math.max(enemy.atkMin, enemy.atkMax - reduction);
                        System.out.println("Wstrzykujesz wirusa w system wroga. Jego obrażenia spadają.");
                    } else if (player.className.equals("Nomada")) {
                        dodgeBoostTurns = 3;
                        System.out.println("Twoje ruchy przyspieszają, przez chwilę stajesz się niemal nieuchwytny.");
                    } else if (player.className.equals("Snajper")) {
                        int dmg = player.attack() * 3;
                        enemy.hp -= dmg;
                        System.out.println("Krytyczne trafienie! Twój strzał zadaje " + dmg + " obrażeń.");
                    }
                    if (enemy.isAlive()) {
                        int effectiveDodge = player.dodgeChance + (dodgeBoostTurns > 0 ? 20 : 0);
                        if (RANDOM.nextInt(100) < effectiveDodge) {
                            System.out.println("Uniknąłeś ataku " + enemy.name + "!");
                        } else {
                            int dmg = enemy.attack();
                            if (blocked) {
                                dmg /= 2;
                                blocked = false;
                                System.out.println("Twoja blokada redukuje obrażenia przeciwnika.");
                            }
                            player.hp -= dmg;
                            System.out.println(enemy.name + " kontratakuje i zadaje ci " + dmg + " obrażeń.");
                        }
                        if (dodgeBoostTurns > 0) dodgeBoostTurns--;
                    }
                }
            } else {
                System.out.println("Nie rozumiem tej komendy.");
                continue;
            }
        }
        if (player.isAlive()) {
            System.out.println("Pokonałeś " + enemy.name + "!");
            player.gainXp(enemy.xpReward);
            Map.Entry<Item, String> chest = dropChest();
            Item item = chest.getKey();
            String rarity = chest.getValue();
            System.out.println("Po walce znajdujesz " + rarity + " przedmiot: " + item.name + " – " + item.description);
            player.addItem(item);
            return true;
        } else {
            System.out.println("Zginąłeś. Twoja przygoda tutaj dobiega końca.");
            return false;
        }
    }

    /**
     * Explore the current tile and trigger an event.  Returns a pair
     * indicating whether to continue the game and whether a boss was fought
     * during this tile.  This influences subsequent boss spawns.
     */
    private static boolean[] exploreTile(Player player, char[][] gameMap, int x, int y,
                                        Set<String> discovered, boolean bossFought,
                                        Scanner scanner) {
        char tile = gameMap[y][x];
        discovered.add(x + "," + y);
        // Generic flavour for exploration
        String[] exploreDescriptions = {
                "Idziesz przez zniszczoną karawanę, piasek tnie ci skórę. Wtedy słyszysz metaliczny dźwięk...",
                "Napotykasz ruiny starych maszyn – zwiastun dawnej technologii.",
                "Twoje buty grzęzną w piasku, gdy nagle zauważasz coś błyszczącego.",
                "Cichy szept wiatru przynosi zapach ozonu... to może nie wróżyć nic dobrego.",
                "Kiedy spoglądasz na horyzont, widzisz szczątki gigantycznego robota."
        };
        System.out.println(exploreDescriptions[RANDOM.nextInt(exploreDescriptions.length)]);
        if (tile == '.') {
            System.out.println("Nic ciekawego tu nie ma.");
            return new boolean[]{true, bossFought};
        } else if (tile == 'W') {
            Enemy enemy = generateEnemy('W', bossFought);
            boolean survived = combat(player, enemy, scanner);
            if (!survived) return new boolean[]{false, bossFought};
            gameMap[y][x] = '.';
            return new boolean[]{true, bossFought};
        } else if (tile == 'L') {
            Item item = generateLoot();
            System.out.println("Otwierasz skrzynię i znajdujesz " + item.name + ": " + item.description);
            player.addItem(item);
            gameMap[y][x] = '.';
            return new boolean[]{true, bossFought};
        } else if (tile == 'N') {
            npcInteraction(player, scanner);
            gameMap[y][x] = '.';
            return new boolean[]{true, bossFought};
        } else if (tile == 'B') {
            Enemy enemy = generateEnemy('B', bossFought);
            boolean survived = combat(player, enemy, scanner);
            if (!survived) return new boolean[]{false, bossFought};
            gameMap[y][x] = '.';
            return new boolean[]{true, true};
        } else if (tile == 'O') {
            player.hp = player.hpMax;
            System.out.println("Znajdujesz oazę! Chłodna woda gasi twoje pragnienie, a rany znikają.");
            if (RANDOM.nextDouble() < 0.3) {
                player.atkMin += 1;
                player.atkMax += 1;
                System.out.println("Wśród palm znajduje się tajemniczy eliksir. Czujesz, że twoja siła rośnie.");
            }
            gameMap[y][x] = '.';
            return new boolean[]{true, bossFought};
        } else if (tile == 'T') {
            // Trap: deal damage and perhaps give a tiny XP bonus
            int dmg = RANDOM.nextInt(10, 26); // 10–25 damage
            player.hp -= dmg;
            System.out.println("Wpadłeś w pułapkę! Tracisz " + dmg + " HP.");
            if (!player.isAlive()) {
                System.out.println("Pułapka okazała się śmiertelna.");
                return new boolean[]{false, bossFought};
            }
            // 30% chance to find a small chest as consolation
            if (RANDOM.nextDouble() < 0.3) {
                Map.Entry<Item, String> chest = dropChest();
                System.out.println("W pułapce znajdujesz " + chest.getValue() + " skrzynię!");
                System.out.println("Zawartość: " + chest.getKey().name + ".");
                player.addItem(chest.getKey());
            }
            gameMap[y][x] = '.';
            return new boolean[]{true, bossFought};
        } else {
            System.out.println("Tu jest tylko wiatr i piach.");
            return new boolean[]{true, bossFought};
        }
    }

    /**
     * The entry point for the game.  Handles class selection, map
     * initialisation and the main command loop.  Accepts input via
     * standard input and prints messages to standard output.
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Witaj na pustkowiach Fenrira. Czeka cię walka o przetrwanie.");
        System.out.println("Wybierz klasę: [Wojownik] [Technik] [Nomada] [Snajper]");
        Player player;
        while (true) {
            System.out.print("> ");
            String cls = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            if (cls.equals("wojownik") || cls.equals("woj") || cls.equals("warrior") ||
                cls.equals("technik") || cls.equals("tech") || cls.equals("technician") ||
                cls.equals("nomada") || cls.equals("nomad") ||
                cls.equals("snajper") || cls.equals("sniper") || cls.equals("strzelec")) {
                player = new Player(cls);
                break;
            }
            System.out.println("Nie rozpoznaję tej klasy. Spróbuj ponownie.");
        }
        System.out.println("Wybrałeś klasę: " + player.className + ". Przygotuj się na przygodę!");
        // Initialise map and state
        char[][] gameMap = generateMap();
        int posX = MAP_WIDTH / 2;
        int posY = MAP_HEIGHT / 2;
        Set<String> discovered = new HashSet<>();
        discovered.add(posX + "," + posY);
        boolean bossFought = false;
        while (true) {
            if (!player.isAlive()) {
                System.out.println("Nie żyjesz. Koniec gry.");
                return;
            }
            System.out.println();
            System.out.println("Co chcesz zrobić? (wpisz 'pomoc' dla listy komend)");
            System.out.print("> ");
            String cmd = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            if (cmd.startsWith("idz")) {
                String[] parts = cmd.split(" ", 2);
                if (parts.length < 2) {
                    System.out.println("Dokąd?");
                    continue;
                }
                String dir = parts[1];
                // Normalise diacritics: replace Polish accented letters with base Latin
                dir = dir.replace('ą', 'a').replace('ć', 'c').replace('ę', 'e')
                        .replace('ł', 'l').replace('ń', 'n').replace('ó', 'o')
                        .replace('ś', 's').replace('ż', 'z').replace('ź', 'z');
                dir = dir.toLowerCase(Locale.ROOT);
                Set<String> northSyn = new HashSet<>(Arrays.asList("polnoc", "pn", "n", "p", "north"));
                Set<String> southSyn = new HashSet<>(Arrays.asList("poludnie", "pd", "s", "d", "south"));
                Set<String> eastSyn  = new HashSet<>(Arrays.asList("wschod", "wsch", "e", "w", "east"));
                Set<String> westSyn  = new HashSet<>(Arrays.asList("zachod", "zach", "z", "west"));
                int dx = 0, dy = 0;
                if (northSyn.contains(dir)) dy = -1;
                else if (southSyn.contains(dir)) dy = 1;
                else if (eastSyn.contains(dir)) dx = 1;
                else if (westSyn.contains(dir)) dx = -1;
                else {
                    System.out.println("Nie rozumiem tego kierunku.");
                    continue;
                }
                int newX = posX + dx;
                int newY = posY + dy;
                if (newX >= 0 && newX < MAP_WIDTH && newY >= 0 && newY < MAP_HEIGHT) {
                    posX = newX;
                    posY = newY;
                    // Random movement description
                    String[] moveDescriptions = {
                        "Piasek chrzęści pod butami, a wiatr śpiewa niespokojne melodie.",
                        "Ktoś tu kiedyś mieszkał. Teraz tylko upiór pustyni i Ty.",
                        "Ślady jakiejś bestii przecinają twoją ścieżkę. Lepiej zachować czujność.",
                        "Z oddali słychać metaliczny zgrzyt – może to złom, może dron.",
                        "Piasek tnie ci skórę, jakby chciał wejść pod ubranie."
                    };
                    System.out.println(moveDescriptions[RANDOM.nextInt(moveDescriptions.length)]);
                    discovered.add(posX + "," + posY);
                } else {
                    System.out.println("Nie możesz iść dalej, tam jest przepaść... albo koniec mapy.");
                }
            } else if (cmd.equals("mapa")) {
                printMap(gameMap, posX, posY, discovered);
            } else if (cmd.equals("eksploruj")) {
                boolean[] result = exploreTile(player, gameMap, posX, posY, discovered, bossFought, scanner);
                if (!result[0]) return; // player died
                if (result[1]) bossFought = true;
            } else if (cmd.equals("ekwipunek")) {
                if (player.inventory.isEmpty()) {
                    System.out.println("Twój ekwipunek jest pusty.");
                } else {
                    System.out.println("Ekwipunek:");
                    for (Map.Entry<String, Integer> entry : player.inventory.entrySet()) {
                        System.out.println(" - " + entry.getKey() + " x" + entry.getValue());
                    }
                    System.out.println("Ładunki skanu: " + player.scanCharges);
                }
            } else if (cmd.startsWith("użyj") || cmd.startsWith("uzyj")) {
                String[] parts = cmd.split(" ", 2);
                if (parts.length < 2) {
                    System.out.println("Podaj nazwę przedmiotu do użycia.");
                    continue;
                }
                String itemName = parts[1];
                String res = player.useItem(itemName);
                if (res != null) {
                    System.out.println(res);
                } else {
                    System.out.println("Nie masz takiego przedmiotu.");
                }
            } else if (cmd.equals("status")) {
                System.out.println(
                        "Klasa: " + player.className + "\n" +
                        "Poziom: " + player.level + "\n" +
                        "XP: " + player.xp + "/" + player.xpToNext + "\n" +
                        "HP: " + player.hp + "/" + player.hpMax + "\n" +
                        "Atak: " + player.atkMin + "-" + player.atkMax + "\n" +
                        "Unik: " + player.dodgeChance + "%\n" +
                        "Ładunki skanu: " + player.scanCharges
                );
            } else if (cmd.equals("pomoc")) {
                System.out.println("Dostępne komendy:");
                String[] lines = {
                        "idz północ / polnoc / n / p – ruch na północ",
                        "idz południe / poludnie / s / d – ruch na południe",
                        "idz wschód / wschod / e / w – ruch na wschód",
                        "idz zachód / zachod / z – ruch na zachód",
                        "mapa – pokazuje mapę z twoją pozycją",
                        "eksploruj – przeszukaj bieżącą lokację",
                        "ekwipunek – pokaż zawartość ekwipunku i ładunków skanu",
                        "użyj [nazwa] – użyj przedmiotu z ekwipunku",
                        "status – pokazuje twoje statystyki",
                        "blokuj – zmniejsz obrażenia przy następnym ciosie",
                        "specjalna – użyj klasowej umiejętności specjalnej (raz na walkę)",
                        "skanuj – ujawnia zawartość sąsiednich pól (zużywa ładunek)",
                        "pomoc – pokazuje tę listę",
                        "wyjdz / quit / exit – zakończ grę"
                };
                for (String l : lines) System.out.println(l);
            } else if (cmd.equals("skanuj") || cmd.equals("scan") || cmd.equals("skan")) {
                if (player.scanCharges <= 0) {
                    System.out.println("Brak ładunków skanu!");
                } else {
                    player.scanCharges--;
                    System.out.println("Skanujesz otoczenie...\n");
                    int[][] dirs = {
                            {-1, -1}, {0, -1}, {1, -1},
                            {-1, 0},           {1, 0},
                            {-1, 1},  {0, 1},  {1, 1}
                    };
                    for (int[] d : dirs) {
                        int sx = posX + d[0];
                        int sy = posY + d[1];
                        if (sx >= 0 && sx < MAP_WIDTH && sy >= 0 && sy < MAP_HEIGHT) {
                            discovered.add(sx + "," + sy);
                            char c = gameMap[sy][sx];
                            String desc;
                            switch (c) {
                                case 'W': desc = "walka"; break;
                                case 'L': desc = "łup"; break;
                                case 'N': desc = "NPC"; break;
                                case 'B': desc = "boss"; break;
                                case 'O': desc = "oaza"; break;
                                case 'T': desc = "pułapka"; break;
                                default: desc = "pusto"; break;
                            }
                            System.out.println("Pole (" + sx + "," + sy + "): " + desc);
                        }
                    }
                }
            } else if (cmd.equals("wyjdz") || cmd.equals("quit") || cmd.equals("exit")) {
                System.out.println("Koniec gry. Do zobaczenia na pustkowiach.");
                return;
            } else {
                System.out.println("Nie rozumiem tej komendy.");
            }
        }
    }
}