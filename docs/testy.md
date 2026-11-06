# Testowanie i wyniki wersji 1.2.1

Data wykonania: **06.11.2026**. Android: **1.2.1**, versionCode **4**. Backend: **API 1.2.0**.

## Wyniki końcowe

| Grupa | Liczba testów | Niepowodzenia |
|---|---:|---:|
| Backend Python | 45 | 0 |
| Jednostkowe Android / Kotlin | 23 | 0 |
| Android: UI, integracja REST i cache pogody | 31 | 0 |
| Razem, różne testy | 99 | 0 |

Dodatkowo powtórzono pięć testów formularzy oraz wybór daty i godziny na ekranie 320 dp ze skalą czcionki 1.3. Powtórzenia nie zwiększają liczby 99. Budowanie debug APK i APK testowego zakończyło się powodzeniem. Lint uruchomiony offline zgłosił 0 błędów i 1 ostrzeżenie GradleDependency o dostępnej aktualizacji Lifecycle. Wynik offline nie jest pełnym sprawdzeniem aktualności wszystkich bibliotek.

Pierwszy przebieg urządzeniowy ujawnił dwie niezgodności asercji z zachowaniem klawiatury i przewijania. Po dopasowaniu przebiegu testów powtórzono pięć testów formularzy oraz cały zestaw 31 testów; oba końcowe przebiegi zakończyły się bez błędów. Log pierwszego przebiegu pozostaje w raportach.

## Środowisko i materiały wynikowe

Emulator Android 10 / API 29, ARM64, 1080 x 2400 px, 420 dpi; test kompaktowy: 840 x 1470 px, skala czcionki 1.3. Użyto osobnego tymczasowego AVD. Kompilacja: SDK 36.1, AGP 9.1.1, Gradle 9.3.1, JDK 21. Backend testowy: Python 3.13.5 i izolowana lokalna SQLite; bez kont na publicznym serwerze.

- [Wyniki JSON](test-results-1.2.1.json).
- [Raporty](../output/reports/1.2.1/): logi backendu, budowania, przebiegu urządzeniowego i kompaktowego; JUnit XML i Lint HTML/XML/TXT.
- [Zrzuty interfejsu](screenshots-1.2.1/): 18 obrazów z wersji 1.2.1.
- Raporty i screeny wersji 1.2 oraz 1.1 są archiwalne.

## Testy jednostkowe Android

Odczyt i formatowanie kwot, precyzja, granice i przepełnienie; podział groszy, udziały, salda i przelewy, w tym 200 deterministycznych zestawów rozliczeń. Spłaty częściowe, pełne i nadpłaty; budżety, współrzędne oraz daty i godziny w różnych strefach i podczas zmiany czasu. Nowe przypadki sprawdzają zerowy limit kategorii, odrzucanie ujemnych i zbyt dużych kwot, nadmiernej precyzji oraz zachowanie wymogu dodatniego wydatku.

## Testy backendu

Konta, hashowanie, sesje, uprawnienia, zaproszenia, idempotencja, konflikty, tombstone i głosy; koszty, kategorie, budżety, spłaty i historia. Kopia SQLite, proxy, WSGI, kontrakt HTTP, parser Environment i adapter MySQL.

Osiem nowych testów konwersji obejmuje pojedynczą transakcję i usunięcie głosów, wyścig dwóch użytkowników, błędny termin punktu, rollback po awarii bazy, idempotentne ponowienie, nowszą wersję pomysłu, pomysł utworzony wyłącznie offline oraz brak uprawnień i usunięte źródło. Równoczesna konwersja pozostawia dokładnie jeden punkt planu.

Testy adaptera MySQL korzystają ze sterownika zastępczego. Nie wykonywano regresji na rzeczywistej bazie hostingu Wispbyte.

## Testy urządzeniowe

CRUD wydatków, salda, formularze, głosowanie, budżet, spłaty, miejsca, historia, kalendarz i zegar, zachowanie szkicu po odtworzeniu aktywności oraz stan filtrów. Sprawdzono ekranowe potwierdzenie odrzucenia zmian w budżecie, miejscu i spłacie, anulowanie i zatwierdzenie usuwania budżetu oraz zapis limitu 0. Testy cofania uwzględniają zamknięcie klawiatury przed systemowym Wstecz.

Integracja REST używa dwóch niezależnych repozytoriów i kont: kolejka, synchronizacja, konflikt z wyborem obu wersji, usunięcie i cofnięcie dostępu. Nowe przypadki obejmują konwersję przez dwie osoby, ponowienie wobec nowszego pomysłu, dalszą edycję konwersji offline, usunięcie oczekującej konwersji, anulowanie przypomnienia przy przyjęciu wersji serwera i natychmiastowe przekazanie przypomnienia w demo. Cache pogody sprawdzono po błędzie sieci.

## Powtórzenie testów

```sh
python3 -m unittest discover -s backend -v
./gradlew :app:testDebugUnitTest :app:lintDebug
```

Na potrzeby testów urządzeniowych uruchom dedykowany emulator i lokalny backend:

```sh
python3 backend/server.py --db /tmp/tripsync-test.sqlite
./gradlew :app:connectedDebugAndroidTest
```

Testy wylogowują aplikację i odtwarzają demo, dlatego wymagają oddzielnego urządzenia testowego. Integracja używa adresu http://10.0.2.2:8080/. Przy kilku urządzeniach ustaw ANDROID_SERIAL. Każdy test tworzy odrębne konta; przy częstych pełnych powtórzeniach należy zrestartować izolowany backend testowy, aby limit logowań nie zakłócał przebiegu.

## Granice weryfikacji

Nie wykonano pełnej macierzy nowszych wersji Androida, testów fizycznego telefonu i tabletu, audytu TalkBack ani testów obciążenia i bezpieczeństwa. Test odtworzenia aktywności nie jest testem utraty procesu. WorkManager podlega ograniczeniom systemu; sprawdzenie przekazania przypomnienia nie gwarantuje dokładnej godziny jego dostarczenia.
