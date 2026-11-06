# Instalacja i uruchomienie

## Android Studio na Macu i pozostałych systemach

1. Sklonuj [repozytorium](https://github.com/Yamoteusz/TripSync) przez GitHub Desktop lub Git. Otwórz cały katalog z `settings.gradle.kts`, nie sam folder `app`.
2. W Android Studio zainstaluj platformę Android SDK 36.1 i Build Tools 36.0.0.
3. Ustaw Gradle JDK 17 lub 21; lokalnie zweryfikowano JDK 21 z Android Studio. Używaj dołączonego Gradle Wrapper 9.3.1.
4. Android Studio utworzy lokalny `local.properties` z `sdk.dir`. Nie wysyłaj go do Git.
5. Wybierz **File → Sync Project with Gradle Files**. Przy pierwszym pobraniu zależności potrzebny jest internet.
6. Wybierz emulator lub telefon z Androidem 8.0+ i konfigurację `app`, następnie **Run**.

Emulator używany w testach: Android 10 / API 29, ARM64. Minimalny API aplikacji to 26; nie należy go zmieniać tylko po to, aby uruchomić aplikację na nowszym emulatorze.

## Gradle i pliki wynikowe

```sh
./gradlew :app:assembleDebug
```

Na macOS pliki powstają w `~/.gradle/tripsync-builds/<identyfikator-katalogu>/app/`. Przeniesienie projektu do innego katalogu nada osobny identyfikator. Na innych systemach jest to `app/build/`.

Dla dokładnie znanej lokalizacji:

```sh
./gradlew -PtripsyncBuildRoot=/tmp/tripsync-build :app:assembleDebug
```

APK: `/tmp/tripsync-build/app/outputs/apk/debug/app-debug.apk`. W przypadku konfliktów synchronizacji Dokumentów nie edytuj plików `.class`; są generowane. W tym projekcie lokalizacja poza Dokumentami zapobiega ich użyciu w kolejnych kompilacjach. Nie zmieniaj źródeł na podstawie dekompilowanych klas.

Pełny skrypt weryfikacji i kopiowania APK:

```sh
sh scripts/build.sh
```

Skrypt używa katalogu tymczasowego lub zmiennej `TRIPSYNC_BUILD_ROOT` i zapisuje końcowy APK w `output/apk/TripSync-debug.apk`.

## Instalacja APK na telefonie

Zainstaluj APK z lokalnej paczki lub artefaktu udanego GitHub Actions. Przy instalacji ręcznej Android może wymagać jednorazowej zgody dla źródła pliku. Przy urządzeniu USB:

```sh
adb install -r output/apk/TripSync-debug.apk
```

W przypadku kilku urządzeń wybierz jedno: `adb -s IDENTYFIKATOR install -r ...`. Aktualizacja zachowuje dane, jeśli pakiet i podpis są zgodne. APK debug z innego komputera może mieć inny klucz: nie odinstalowuj aplikacji z ważnymi niewysłanymi danymi wyłącznie po to, aby obejść błąd podpisu.

## Demo i prawdziwe konta

**Wypróbuj bez konta** tworzy lokalny przykład. Demo nie wysyła danych do serwera. Wylogowanie czyści lokalne dane i niewysłane zmiany; ponowne wejście do demo odtwarza przykład.

Nowa instalacja używa `https://tripsync.wisp.uno/`. Dotychczas zapisany adres jest zachowany. Adres można zmienić w ustawieniach po wylogowaniu, przed utworzeniem konta. Każda osoba korzysta z własnego konta na tym samym serwerze; nie używa hasła bazy MySQL.

## Własny backend lokalny

Python 3.10+. Wariant SQLite korzysta z biblioteki standardowej:

```sh
python3 backend/server.py --db /tmp/tripsync-local.sqlite
```

Domyślny port to 8080. W osobnym terminalu:

```sh
curl http://127.0.0.1:8080/health
```

Odpowiedź bieżącego backendu: `{"status":"ok","version":"1.2.0"}`. Numer wersji API nie jest numerem wersji interfejsu Android 1.2.1; API 1.2.0 dodaje atomową operację convert wymaganą przez aplikację 1.2.1.

Ustaw w emulatorze debug `http://10.0.2.2:8080/`. Telefon przez USB może użyć:

```sh
adb reverse tcp:8080 tcp:8080
```

Wtedy adres aplikacji to `http://127.0.0.1:8080/`. Lokalny HTTP jest dopuszczony tylko w debug dla `10.0.2.2`, `127.0.0.1` i `localhost`. Udostępnienie innym urządzeniom wymaga HTTPS z zaufanym certyfikatem; instrukcja hostingu: [wispbyte.md](wispbyte.md).

## Diagnoza najczęstszych problemów

| Objaw | Sprawdzenie |
|---|---|
| Nie ma konta po wejściu do demo | Demo jest odrębne i lokalne; użyj rejestracji na wybranym API |
| Brak synchronizacji | Adres API, internet, stan kolejki oraz komunikat sesji/konfliktu |
| `defined multiple times` i nazwy ` 2.class` | Zsynchronizuj aktualny Gradle; generowane pliki na Macu powinny być poza Dokumentami |
| Nie działa MySQL | Sprawdź użytkownika bazy, host, hasło i dostęp; używaj panelu Wispbyte, nie ustawień Androida |
| `.env` zgłasza numer wiersza | Każdy wpis ma formę `NAZWA=wartość`; samej nazwy bez `=` parser nie zaakceptuje |
| Mapa lub pogoda nie ładuje się | Dostęp do zewnętrznych HTTPS; zapisane miejsca i cache pogody mogą pozostać dostępne |
| Brak powiadomienia | Zgoda od Androida 13, włączone przypomnienie oraz ograniczenia pracy w tle |
