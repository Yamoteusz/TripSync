# Zmiany w wersji 1.2.1

Wykonawcy: Brajan Sieroń, Damian Panońko, Patryk Dawczak. Data wydania: 06.11.2026. Android: 1.2.1 (versionCode 4), backend: API 1.2.0.

- Atomowe przenoszenie pomysłu w punkt planu: jeden identyfikator, jedna operacja serwera, kontrola wersji, usunięcie głosów i historia w transakcji.
- Ochrona niezapisanych danych w formularzach budżetu, spłaty i miejsca przy cofnięciu strzałką i przyciskiem systemowym.
- Natychmiastowe planowanie przypomnienia po konwersji, także lokalnie i w demo; anulowanie przy odrzuceniu lokalnej konwersji.
- Potwierdzenie usuwania budżetu, z zachowaniem zapisanych wydatków.
- Możliwość zapisania zerowego limitu kategorii przy zachowaniu dodatnich kwot wydatków i spłat.

Zakres i wyniki testów opisuje [testy.md](testy.md). Instalacja wymaga aktualizacji backendu przed nowym APK; szczegóły zawiera [wispbyte.md](wispbyte.md).
