# DesertRPG Android

**DesertRPG Android** to tekstowa gra RPG osadzona na pustyni, napisana w Javie i przystosowana do uruchamiania w Android IDE (np. AIDE) lub w konsoli na PC.  
Gracz eksploruje proceduralnie generowaną mapę pustkowi, walczy z przeciwnikami, zdobywa łupy, spotyka NPC, korzysta z oaz, omija pułapki i stawia czoła bossowi Fenrira.

## Funkcje
- **4 klasy postaci**: Wojownik, Technik, Nomada, Snajper (każda z unikalną umiejętnością specjalną).
- **Proceduralnie generowana mapa** (wielkość konfigurowalna).
- **Walka turowa** z systemem uniku, blokowania, przedmiotów i umiejętności.
- **NPC** z wymianą przedmiotów, błogosławieństwami i fabularnymi wstawkami.
- **Pułapki (T)** z obrażeniami i szansą na nagrodę.
- **System ekwipunku** z przedmiotami leczącymi, wzmacniającymi i dodającymi ładunki skanu.
- **Skaner** ujawniający sąsiednie pola na mapie.

## Wymagania
- Java 8+
- Środowisko umożliwiające uruchamianie aplikacji konsolowych:
  - **Android**: AIDE, Termux + `javac`
  - **PC**: dowolny IDE (IntelliJ, Eclipse, NetBeans) lub terminal z Javą

## Uruchomienie
1. Sklonuj repozytorium lub pobierz plik `DesertRPGAndroid.java`.
2. Otwórz plik w środowisku programistycznym.
3. Uruchom metodę `main()` w klasie `DesertRPGAndroid`.

## Sterowanie
Podczas gry dostępne są komendy (wpisywane w konsoli), np.:
- `idz północ` / `idz polnoc` / `n` – ruch na północ  
- `mapa` – wyświetlenie mapy  
- `eksploruj` – przeszukanie lokacji  
- `ekwipunek` – zawartość plecaka  
- `użyj [nazwa]` – użycie przedmiotu  
- `status` – statystyki postaci  
- `skanuj` – użycie skanera  
- `specjalna` – użycie umiejętności specjalnej (raz na walkę)  
- `wyjdz` – zakończenie gry
