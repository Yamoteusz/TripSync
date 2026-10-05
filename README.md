<p align="center">
  <img src="docs/brand/tripsync.svg" width="420" alt="TripSync" />
</p>

<p align="center"><strong>Wspólny wyjazd. Jeden plan. Czytelne rozliczenia.</strong></p>
<p align="center">Android · Kotlin · Jetpack Compose · MVVM · Room · REST API</p>

# TripSync

TripSync to aplikacja Android do organizacji podróży w grupie. Łączy harmonogram, zadania, pakowanie, głosowanie, wspólne wydatki, budżety, spłaty oraz mapę miejsc i prognozę pogody. Dane są zapisywane lokalnie, a współpraca między kontami odbywa się przez własny backend REST.

**Wersja Android: 1.2.0** · **Android 8.0+** · **Projekt akademicki dla trzech osób** · **Bez płatności i subskrypcji**

[Dokumentacja](docs/README.md) · [Instrukcja uruchomienia](docs/uruchomienie.md) · [PDF projektu](output/pdf/TripSync_dokumentacja_projektu.pdf) · [Wyniki testów](docs/testy.md) · [Znane ograniczenia](docs/ograniczenia.md)

## Zobacz aplikację

<p>
  <img src="docs/screenshots-1.2/02-podsumowanie.png" width="220" alt="Podsumowanie wyjazdu w TripSync" />
  <img src="docs/screenshots-1.2/06-rozliczenia.png" width="220" alt="Wspólne koszty i kategorie wydatków" />
  <img src="docs/screenshots-1.2/14-kalendarz.png" width="220" alt="Wybór daty w kalendarzu" />
</p>

Zrzuty pochodzą z działającej aplikacji na emulatorze. Pokazują dane demonstracyjne oraz izolowane środowisko testowe. [Pełna galeria](docs/funkcje.md#zrzuty-ekranu).

## Co można zrobić

| Obszar | Działające funkcje |
|---|---|
| Wyjazdy i grupa | CRUD wyjazdów, konta, role organizatora i uczestnika, zaproszenia kodem lub linkiem |
| Plan | Punkty z datą, godziną i miejscem, kolejność, wykonanie i lokalne przypomnienia |
| Zadania | Zadania oraz pakowanie, przypisanie osoby, status wykonania |
| Pomysły | Propozycje, własne głosy, cofanie głosu i przeniesienie propozycji do planu |
| Koszty | Podział równy lub własne udziały, sześć kategorii, wyszukiwanie i sortowanie |
| Budżet i spłaty | Limit całkowity, limity kategorii, salda, propozycje przelewów i zapis własnych spłat |
| Miejsca i pogoda | Wspólna mapa, CRUD miejsc, wyszukiwanie miejscowości, prognoza na 7 dni i cache |
| Współpraca | Zapis offline, kolejka operacji, synchronizacja, konflikty edycji i historia zmian |
| Interfejs | Jasny i ciemny motyw, polskie daty, kalendarze, zegar 24-godzinny i ochrona formularza przy cofnięciu |

Spłata jest zapisem deklaracji użytkownika. Aplikacja nie realizuje przelewów bankowych.

## Szybki start

1. Otwórz **cały katalog repozytorium** w Android Studio, zsynchronizuj Gradle i kliknij **Run**.
2. W aplikacji wybierz **Wypróbuj bez konta**, aby zobaczyć edytowalny wyjazd trzech osób bez uruchamiania backendu.
3. Do współpracy utwórz własne konto. Nowa instalacja korzysta z `https://tripsync.wisp.uno/`. Każdy uczestnik powinien używać tego samego serwera.

Pełna konfiguracja SDK, uruchomienie na Macu, emulatorze i telefonie: [uruchomienie.md](docs/uruchomienie.md).

APK debug można pobrać z artefaktu **TripSync-debug** udanego przebiegu [GitHub Actions](https://github.com/Yamoteusz/TripSync/actions). Dostępny lokalnie APK jest w `output/apk/TripSync-debug.apk`; pliki APK są wyłączone z historii Git. Workflow tworzy artefakt dopiero po udanym zbudowaniu aplikacji.

## Budowanie i testy

Wymagania obecnego projektu: **JDK 17 lub 21**, Android SDK **36.1**, Build Tools **36.0.0**. W lokalnej weryfikacji użyto JDK 21. SDK 36.1 jest faktyczną konfiguracją wersji 1.2; aktualizacja do SDK 37 pozostaje zadaniem przed oddaniem, jeśli prowadzący wymaga najnowszego stabilnego SDK.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
python3 -m unittest discover -s backend -v
```

Na macOS pliki kompilacji powstają automatycznie poza repozytorium w `~/.gradle/tripsync-builds/<identyfikator-katalogu>/app/`, aby synchronizacja Dokumentów nie tworzyła kopii plików `.class`. Na pozostałych systemach pozostaje `app/build/`. Możesz jawnie wskazać katalog:

```sh
./gradlew -PtripsyncBuildRoot=/tmp/tripsync-build :app:assembleDebug
```

APK powstanie wtedy w `/tmp/tripsync-build/app/outputs/apk/debug/app-debug.apk`. `sh scripts/build.sh` wykonuje budowanie, lint, testy jednostkowe i backendu oraz kopiuje APK do `output/apk/`.

Ostatnia udokumentowana weryfikacja z **05.10.2026**:

| Grupa | Wynik |
|---|---:|
| Testy jednostkowe Android | 21 / 21 |
| Testy urządzeniowe: UI i integracja | 20 / 20 |
| Testy backendu | 37 / 37 |
| Razem, bez powtórnych przebiegów | **78 / 78** |
| Lint | 0 błędów, 16 ostrzeżeń o nowszych wersjach narzędzi i bibliotek |

Testy urządzeniowe wykonano na API 29; wybór daty i godziny dodatkowo sprawdzono przy szerokości 320 dp oraz skali czcionki 1.3. Instrukcja uruchomienia i granice weryfikacji: [testy.md](docs/testy.md). Liczby opisują zapisany przebieg, a nie automatyczną gwarancję każdego przyszłego commita.

## Architektura

```mermaid
flowchart LR
    UI[Compose i Navigation] --> VM[TripViewModel / StateFlow]
    VM --> REPO[Repository]
    REPO --> DB[(Room / SQLite)]
    DB -->|Flow| VM
    REPO --> API[Retrofit / REST]
    API --> SERVER[Backend Python]
    SERVER --> MYSQL[(MySQL lub SQLite)]
    WORK[WorkManager] --> REPO
    VM --> TRAVEL[TravelRepository]
    TRAVEL --> METEO[Open-Meteo i cache]
```

Room jest lokalnym źródłem danych. Zapis rekordu i operacji synchronizacji odbywa się w transakcji. API weryfikuje sesję, uprawnienia i wersję rekordu; równoczesne zmiany mogą wymagać rozstrzygnięcia konfliktu. [Szczegóły MVVM i synchronizacji](docs/architektura.md).

## Struktura repozytorium

```text
app/
  src/main/java/pl/tripsync/
    ui/             ekrany, formularze, motyw i ViewModel
    domain/         modele, walidacja, daty i obliczenia pieniędzy
    data/           Room, API, sesja, ustawienia i synchronizacja
  src/test/         testy jednostkowe Kotlin
  src/androidTest/  testy UI i integracji
  schemas/          eksport schematu Room
backend/            API Python, adapter MySQL, hosting i testy
main.py             punkt startowy dla domyślnej komendy Wispbyte
requirements.txt    zależności wariantu MySQL
.github/workflows/  kontrola projektu i artefakty APK
scripts/            budowanie, generator PDF i pakowanie
docs/              dokumentacja, diagramy i zrzuty ekranu
output/             PDF, raporty i lokalne paczki prezentacyjne
```

## Backend i hosting

Lokalne API ze SQLite nie wymaga dodatkowych pakietów Python:

```sh
python3 backend/server.py --db /tmp/tripsync-local.sqlite
```

Emulator łączy się z nim pod adresem `http://10.0.2.2:8080/`; ustaw go przed zalogowaniem. HTTP jest dopuszczone tylko dla lokalnych adresów w wariancie debug.

Aktualny hosting projektu to **Wispbyte + MySQL**. Zachowujemy domyślny startup panelu uruchamiający `main.py`; hasło i pozostałe ustawienia wpisuje się na serwerze w Environment lub `.env`. [Jedna instrukcja Wispbyte](docs/wispbyte.md) opisuje dokładne zmienne, aktualizacje i diagnostykę.

## Dokumentacja do zajęć

[Indeks dokumentacji](docs/README.md) obejmuje cel i zakres, wymagania, diagramy architektury i baz, przypadki użycia, technologie, instrukcję instalacji, testy, screeny i scenariusz prezentacji. [Zgodność z wytycznymi](docs/zgodnosc.md) wskazuje zarówno realizację wymagań, jak i pozostające zadania.

Dane trzech autorów i rzeczywisty podział wkładu należy uzupełnić w [AUTHORS.md](AUTHORS.md). Dokumentacja nie przypisuje nikomu niepotwierdzonego autorstwa.

## Ograniczenia

Jedna waluta na wyjazd (PLN, EUR lub USD), maksymalnie 100 uczestników, okresowa lub ręczna synchronizacja. Kafelki mapy wymagają internetu. Prognoza dotyczy najbliższych dni; wyszukiwanie obejmuje miejscowości, a nie pełny katalog atrakcji. Brak czatu, płatności, załączników, odzyskiwania hasła oraz usuwania konta przez użytkownika. [Pełna lista i zadania przed oddaniem](docs/ograniczenia.md).

## GitHub i materiały

Repozytorium: [Yamoteusz/TripSync](https://github.com/Yamoteusz/TripSync). Do publikacji lokalnych commitów w GitHub Desktop użyj **Push origin**. Jeśli repozytorium jest prywatne, przyznaj dostęp prowadzącemu. [Praca zespołu i aktualizacje](docs/github-desktop.md).

Po zatwierdzeniu dokumentacji `python3 scripts/package_project.py` tworzy ZIP, Git bundle i sumy SHA-256 w `output/`. Skrypt nie publikuje kodu ani nie wdraża backendu. Historia z paczki może być odtworzona przez `git clone TripSync-history.bundle TripSync`.

## Biblioteki i licencje

Wersje deklarowane i zastosowanie opisano w [technologie.md](docs/technologie.md). Leaflet ma dołączoną licencję BSD-2-Clause; mapa pokazuje atrybucję OpenStreetMap, a pogoda korzysta z Open-Meteo. Repozytorium nie ma obecnie odrębnej licencji na własny kod; publiczne udostępnienie repozytorium samo w sobie jej nie ustanawia.
