# Testowanie i wyniki wersji 1.2

## Wykonane sprawdzenia

Wyniki zachowane z 05.10.2026. To raport z wykonanych przebiegów, nie plan przyszłych testów.

| Grupa | Liczba | Niepowodzenia |
|---|---:|---:|
| Backend Python | 37 | 0 |
| Jednostkowe Android / Kotlin | 21 | 0 |
| Android: UI, integracja REST i cache pogody | 20 | 0 |
| Razem, unikalne testy | 78 | 0 |

Dodatkowo powtórzono wybór daty i godziny na ekranie 320 dp ze skalą czcionki 1.3 oraz pokaz głównych widoków ze sprawdzeniem kontrastu systemowych pasków w ciemnym motywie. Powtórzenia nie zwiększają liczby 78. Budowanie zakończyło się powodzeniem. Lint: 0 błędów i 16 ostrzeżeń wyłącznie o dostępnych aktualizacjach Gradle oraz zależności.

Po poprawce katalogu kompilacji na macOS ponownie wykonano świeże budowanie i 21 testów jednostkowych, a następnie powtórne budowanie bez dodatkowej konfiguracji ścieżki. Oba przebiegi zakończyły się powodzeniem; nowe pliki nie miały kopii ` 2.class` / ` 3.class`.

## Środowisko i dowody

API 29 / Android 10, ARM64, 1080 × 2400 px, 420 dpi; wariant kompaktowy 840 × 1470 px i font scale 1.3. Kompilacja: SDK 36.1, AGP 9.1.1, Gradle 9.3.1, JDK 21. Backend testów: Python 3.13 i osobna lokalna SQLite. Testy integracji nie tworzyły kont na publicznym serwerze.

- Wyniki maszynowe: `docs/test-results-1.2.json`.
- JUnit XML, logi budowania i Lint HTML/XML/TXT: `output/reports/ui-1.2/`.
- Zrzuty działającego interfejsu: `docs/screenshots-1.2/`.
- Poprzednie wyniki w `docs/test-results.json`, `docs/screenshots/` oraz raportach poza `ui-1.2/` są archiwalne.

## Zakres testów jednostkowych

Odczyt kwot, przecinek/kropka, precyzja, granice i przepełnienie; dokładny podział groszy, walidacja udziałów, salda i przelewy. Testy obejmują 200 deterministycznych losowych zestawów rozliczeń, spłaty częściowe, całkowite i nadpłatę. Sprawdzono budżety, współrzędne, daty i godziny. Testy dat weryfikują polskie formatowanie oraz zachowanie dnia przy zmianach strefy i przejściu czasu letniego.

## Zakres backendu

Konta, hashowanie, sesje, logowanie i błędne dane; uprawnienia, zaproszenia, idempotencja, konflikty, tombstone i głosowanie; wydatki, kategorie, budżety, spłaty i historia. Dodatkowo kopia SQLite, kontrola proxy, WSGI, kontrakt HTTP, konfiguracja `.env` i adapter MySQL. Testy adaptera MySQL używają sterownika zastępczego; nie są pełnym automatycznym testem usługi MySQL Wispbyte.

## Zakres urządzeniowy

CRUD wydatku i salda, walidacja formularza, głosowanie, budżet, spłata, miejsca, historia i główne ekrany. Odtwarzanie aktywności zachowuje formularz i dane. Regresje 1.2 obejmują wybór daty/godziny, ochronę przed odrzuceniem formularza, filtr kosztów po przełączeniu zakładki oraz zachowanie udziałów przy dołączeniu nowej osoby.

Integracja REST używa dwóch niezależnych repozytoriów i kont: zapis, kolejka, synchronizacja, konflikt z wyborem obu wersji, usunięcie i cofnięcie dostępu. Test cache pogody sprawdza zachowanie zapisanej prognozy po błędzie sieci.

## Jak powtórzyć testy

```sh
python3 -m unittest discover -s backend -v
./gradlew :app:testDebugUnitTest :app:lintDebug
```

Do testów urządzeniowych uruchom dedykowany emulator oraz osobny serwer na porcie 8080:

```sh
python3 backend/server.py --db /tmp/tripsync-test.sqlite
```

W drugim terminalu:

```sh
./gradlew :app:connectedDebugAndroidTest
```

Testy UI wylogowują aplikację i tworzą demo, więc nie uruchamiaj ich na urządzeniu z ważnymi danymi użytkownika. Integracja ustawia adres `http://10.0.2.2:8080/`. Przy kilku emulatorach ogranicz wybór do testowego urządzenia zmienną `ANDROID_SERIAL`; nie testuj przypadkowo aktywnego telefonu.

Gradle może usunąć APK po testach. Do zachowania screenów można osobno zainstalować oba APK i uruchomić runner:

```sh
adb -s EMULATOR shell am instrument -w -e class 'pl.tripsync.TripSyncUiTest#mainScreensAndRealScreenshots' pl.tripsync.test/androidx.test.runner.AndroidJUnitRunner
adb -s EMULATOR pull /sdcard/Android/data/pl.tripsync/files/screenshots/. /tmp/tripsync-screenshots/
```

## Granice weryfikacji

Nie wykonano pełnych testów na nowszych Androidach, tablecie i fizycznym telefonie, audytu TalkBack, bezpieczeństwa ani testów obciążenia. Test `scenario.recreate()` nie jest testem zabicia procesu przez system. Starszy raport opisuje dodatkowe ręczne force-stop i odczyt demo; nie rozciągamy tego wyniku na wszystkie scenariusze wersji 1.2. Wyniki nie dowodzą braku każdego możliwego błędu.
