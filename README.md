# TripSync

Aplikacja Android do organizacji wyjazdów grupowych: harmonogram, głosowanie, zadania, pakowanie, wydatki i rozliczenia. Działa offline i synchronizuje dane przez własne API REST. Projekt dla zespołu trzyosobowego.

## Szybki start

1. Zainstaluj `output/apk/TripSync-debug.apk` na Androidzie 8.0 lub nowszym.
2. Wybierz **Wypróbuj bez konta**. Przykładowy wyjazd jest w pełni edytowalny i nie wymaga serwera.
3. Aby współpracować z innymi użytkownikami, uruchom backend, ustaw adres API przed zalogowaniem i utwórz konto.

Tryb demonstracyjny nie udaje synchronizacji z innymi urządzeniami. Zmiany w demo są przechowywane wyłącznie lokalnie. Ponowne wejście do demo po wylogowaniu odtwarza zestaw przykładowy.

## Budowanie w Android Studio

Otwórz katalog projektu w Android Studio. Zainstaluj Android SDK 36.1 i Build Tools 36.0.0. Projekt korzysta z Gradle Wrapper 9.3.1, Android Gradle Plugin 9.1.1, wbudowanego Kotlina i kompilatora Compose 2.2.10. JDK 17 lub 21; w tym środowisku użyto JDK 21 z Android Studio.

Lokalny plik `local.properties` powinien wskazywać własny Android SDK. Nie jest wersjonowany.

```properties
sdk.dir=/sciezka/do/Android/sdk
```

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Jeżeli katalog projektu jest synchronizowany przez usługę chmurową, pliki budowania można umieścić poza nim: `./gradlew -PtripsyncBuildRoot=/tmp/tripsync-build :app:assembleDebug`. Wtedy APK powstaje w `/tmp/tripsync-build/app/outputs/apk/debug/`.

APK wynikowy: `app/build/outputs/apk/debug/app-debug.apk`. Jest podpisany kluczem debug i służy do prezentacji. Wersja release wymaga własnego podpisu. Przy pierwszym budowaniu potrzebne jest połączenie z repozytoriami Google Maven i Maven Central.

## Uruchomienie backendu

Python 3.10 lub nowszy, wyłącznie biblioteka standardowa, bez instalowania pakietów.

```sh
python3 backend/server.py
```

Baza SQLite powstanie w `backend/tripsync.sqlite`. Endpoint kontrolny: `http://127.0.0.1:8080/health`.

W emulatorze Android ustaw w aplikacji: `http://10.0.2.2:8080/`. Ten adres jest domyślny w wersji debug. Współdzielenie można sprawdzić na dwóch emulatorach z dwoma oddzielnymi kontami.

Na telefonie podłączonym przez USB można użyć lokalnego tunelu:

```sh
adb reverse tcp:8080 tcp:8080
```

Wtedy ustaw `http://127.0.0.1:8080/`. Dla urządzeń poza lokalnym tunelem użyj HTTPS z certyfikatem zaufanym przez Android:

```sh
python3 backend/server.py --host 0.0.0.0 --port 8443 --cert /sciezka/fullchain.pem --key /sciezka/privkey.pem
```

Alternatywnie uruchom API za reverse proxy HTTPS. Publiczne udostępnienie i konfiguracja domeny nie są wykonywane automatycznie. Wersja release blokuje cały nieszyfrowany HTTP. W debug HTTP jest dopuszczone jedynie dla `10.0.2.2`, `127.0.0.1` i `localhost`.

## Wspólny wyjazd

1. Anna rejestruje konto i tworzy wyjazd.
2. Po synchronizacji otwiera **Uczestnicy i zaproszenia → Pokaż kod zaproszenia**.
3. Bartek na drugim urządzeniu rejestruje własne konto i używa **Dołącz kodem**.
4. Obie osoby dodają elementy i wybierają ikonę synchronizacji.
5. Po odłączeniu sieci zmiany pozostają w lokalnej kolejce; po połączeniu WorkManager ponawia synchronizację. Można również użyć przycisku synchronizacji.
6. Przy równoczesnej edycji tego samego rekordu pojawia się ekran konfliktów. Każda osoba wybiera swoją wersję albo wersję serwera.

Kod zaproszenia ma 12 znaków. Organizator może go unieważnić przez wygenerowanie nowego. Link `tripsync://join?code=KOD` otwiera aplikację; po zalogowaniu kod trafia do formularza dołączenia. Dodawanie uczestników i uwierzytelnianie wymagają sieci.

## Testowanie

```sh
python3 -m unittest discover -s backend -v
./gradlew :app:testDebugUnitTest
```

Testy urządzeniowe obejmują interfejs oraz rzeczywistą komunikację dwóch niezależnych repozytoriów Android z API. Przed ich uruchomieniem włącz backend na porcie 8080, uruchom emulator i pozostaw domyślny adres API:

```sh
./gradlew :app:connectedDebugAndroidTest
```

Testy używają losowych kont w bazie serwera. Zalecana oddzielna baza testowa:

```sh
python3 backend/server.py --db tmp/test-server.sqlite
```

Zrzuty ekranów z testu `mainScreensAndRealScreenshots` trafiają do `Android/data/pl.tripsync/files/screenshots` na urządzeniu. Materiały końcowe i wyniki wykonanych testów znajdują się w `docs/` i `output/`. Gradle może usunąć aplikację po testach; aby zachować screeny, zainstaluj oba APK i uruchom sam test screenów przez `adb shell am instrument -w -e class 'pl.tripsync.TripSyncUiTest#mainScreensAndRealScreenshots' pl.tripsync.test/androidx.test.runner.AndroidJUnitRunner`, a potem pobierz katalog przez `adb pull`.

## Struktura

| Katalog | Zawartość |
|---|---|
| `app/src/main/java/pl/tripsync/ui` | Ekrany Compose, formularze, motyw i ViewModel |
| `app/src/main/java/pl/tripsync/domain` | Modele, walidacja, dokładne obliczenia pieniężne |
| `app/src/main/java/pl/tripsync/data` | Room, API Retrofit, repozytorium, sesja i ustawienia |
| `backend` | API REST, baza SQLite i testy backendu |
| `docs` | Dokumentacja funkcjonalna, diagramy i zrzuty ekranu |
| `output` | APK, PDF i paczka do przekazania |

## Założenia i ograniczenia

Jedna waluta na wyjazd (PLN, EUR lub USD), bez przeliczania kursów. Maksymalnie 100 uczestników. Rozliczenia są propozycją przelewów; aplikacja nie realizuje płatności. Algorytm zachowuje dokładność groszy i ogranicza liczbę przelewów do co najwyżej n−1 dla n osób z niezerowym saldem; nie gwarantuje globalnego minimum.

Synchronizacja jest oparta na pełnym pobraniu dostępnych wyjazdów, a nie na transmisji w czasie rzeczywistym. Synchronizacja okresowa ma interwał co najmniej 15 minut i zależy od ograniczeń systemu. Usunięcie uprawnień jest skuteczne na serwerze natychmiast, a lokalne dane zostają usunięte przy następnej udanej synchronizacji. Telefon całkowicie offline zachowuje wcześniej pobraną kopię.

Powiadomienia są lokalne, wymagają zgody od Androida 13 i mogą być opóźnione przez system. Nie używają alarmów dokładnych. Wylogowanie usuwa lokalną bazę i niewysłane zmiany. Dane aplikacji znajdują się w prywatnym katalogu Android, sesja jest szyfrowana AES-GCM kluczem Keystore, a kopie zapasowe aplikacji są wyłączone. Sama baza Room nie jest dodatkowo szyfrowana SQLCipher.

Backend jest rozwiązaniem dydaktycznym: pojedyncza instancja procesu, SQLite i serializowane operacje zapisu. Nie zawiera odzyskiwania hasła, weryfikacji e-mail ani infrastruktury monitoringu produkcyjnego. Nie ma czatu, rezerwacji, map offline ani automatycznego wykrywania atrakcji.

## Dokumentacja bibliotek

- [Android Gradle Plugin](https://developer.android.com/build/releases/gradle-plugin)
- [Jetpack Compose i BOM](https://developer.android.com/develop/ui/compose/bom)
- [Room](https://developer.android.com/training/data-storage/room)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
- [Retrofit](https://square.github.io/retrofit/)

## Repozytorium

Projekt jest wersjonowany lokalnie przez Git. Nie skonfigurowano jeszcze zdalnego adresu repozytorium. Po wskazaniu repozytorium należy dodać jego URL do dokumentacji i opublikować historię zmian.

## Odtworzenie historii i materiałów

Paczka ZIP zawiera foldery projektu i plik `TripSync-history.bundle`. Historię można odtworzyć w oddzielnym katalogu:

```sh
git clone TripSync-history.bundle TripSync-z-historia
```

APK jest załącznikiem prezentacyjnym; nie jest dodawane do historii Git. Po zmianach można odbudować je przez `scripts/build.sh`. Dokumentację PDF można odtworzyć przez `python3 scripts/create_documentation.py` po zainstalowaniu pakietu `reportlab`; wymagane są dołączone screeny, wyniki JSON oraz czcionka Arial albo DejaVu Sans. Gotowa dokumentacja nie wymaga tych narzędzi do odczytu.

Po zatwierdzeniu zmian polecenie `python3 scripts/package_project.py` tworzy ZIP, Git bundle i sumy SHA-256. Nie publikuje projektu w zdalnym repozytorium.
