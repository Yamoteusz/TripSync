# Zgodność z wytycznymi zajęć Android

Ocena dotyczy kodu Android 1.2.0, backendu API 1.1.0 i zapisanych wyników testów. Nie jest przyznaniem punktów ani gwarancją oceny prowadzącego.

## Wymagania techniczne

| Wymaganie | Status | Dowód / uwaga |
|---|---|---|
| Java lub Kotlin | Spełnione | Kotlin w app/src/main/java |
| Android Studio i aktualne stabilne SDK | Częściowo | Android Studio działa; SDK 36.1. Aktualizacja do 37 pozostaje do wykonania |
| MVVM lub Clean Architecture | Spełnione w wariancie MVVM | TripViewModel, Repository, domain, data, ui |
| Compose lub XML / Material 3 | Spełnione | Compose i Material 3 |
| DBMS | Spełnione | Room/SQLite oraz adapter backendu MySQL |
| REST / Retrofit | Spełnione | TripApi, TravelRepository, API Python |
| Coroutines / Flow | Spełnione | ViewModel, DAO Flow i suspend operacje |
| Stan i nawigacja | Spełnione | StateFlow, rememberSaveable i Navigation Compose |
| Ustawienia | Spełnione | DataStore: motyw i adres API |
| Obsługa błędów | Zaimplementowane | Walidacja, komunikaty, kolejka i konflikty; bez wyczerpującego testu awarii każdej bazy |
| Jednostkowe i UI | Spełnione | 21 jednostkowych i 20 urządzeniowych w zapisanym przebiegu |
| Podstawowe bezpieczeństwo | Zaimplementowane | Keystore, HTTPS, backendowe role i walidacja; bez audytu |
| Git | Spełnione lokalnie | Historia i origin; dostęp prowadzącego do repozytorium do potwierdzenia |
| Dokumentacja | Przygotowana wersja robocza | README, Markdown, PDF, diagramy; dane autorów do uzupełnienia |

## Funkcjonalne i niefunkcjonalne

CRUD, walidacja, ponad cztery ekrany, nawigacja, trwałość i obsługa błędów są zaimplementowane. Rozszerzenia obejmują wiele kont, współpracę offline, konflikty, rozliczenia, budżety, historię, mapy i pogodę.

Responsywność sprawdzono na telefonicznym emulatorze oraz dla pickerów na małym ekranie ze skalą czcionki 1.3. ViewModel i rememberSaveable wspierają odtworzenie aktywności; istnieje test tego scenariusza. Opisy kontrolek i motywy wspierają dostępność, ale pełne sprawdzenie TalkBack, tabletu i najnowszego Androida pozostaje do wykonania. Czytelność i modularność kodu należy ocenić w przeglądzie; nie utożsamiamy samych folderów z idealną architekturą.

## Kryteria oceny

| Kryterium | Maksimum | Materiał do oceny |
|---|---:|---|
| Funkcje | 25 | CRUD, plan, koszty, współpraca, mapa i pogoda |
| Architektura i kod | 15 | MVVM, domena Money/Validation, Repository, transakcje i synchronizacja |
| UI i UX | 10 | Material 3, formularze, pickery, dwa motywy i błędy |
| Baza i dane | 10 | Room, MySQL/SQLite, schematy, indeksy, trwałość i kolejka |
| API / urządzenie | 10 | REST, Open-Meteo, mapa, powiadomienia i WorkManager |
| Błędy i bezpieczeństwo | 10 | Role backendu, Keystore, walidacja, konflikty i komunikaty |
| Testy | 10 | 21 jednostkowych Android, 20 urządzeniowych oraz 37 backendu |
| Dokumentacja | 5 | Cel, wymagania, diagramy, instrukcje, screeny, wyniki, repo i APK |
| Prezentacja i obrona | 5 | Samodzielny pokaz i wyjaśnienie rozwiązań przez studentów |
| Razem | 100 | Punkty przyznaje prowadzący po weryfikacji |

## Pakiet wymagany od studentów

Cel i zakres: `wymagania.md`; specyfikacja: ten sam dokument. Diagram architektury: `architektura.md` i SVG. UML przypadków użycia: `przypadki-uzycia.md` i SVG. Bazy: `baza-danych.md`, schemat Room i backend. Widoki: `funkcje.md` oraz `screenshots-1.2/`. Biblioteki: `technologie.md`. Instalacja: `uruchomienie.md`. Testy: `testy.md`, JSON i raporty. Repozytorium: https://github.com/Yamoteusz/TripSync. APK: lokalna paczka lub artefakt udanego workflow. Całość podsumowuje PDF.

Nazwiska i rzeczywisty wkład należy uzupełnić w `AUTHORS.md`, a formę indywidualnego oddania przez trzy osoby uzgodnić z prowadzącym. Zakres nie wymaga płatności ani publikacji w Google Play.

Źródło planowanej aktualizacji SDK: [Android 17 / API 37](https://developer.android.com/about/versions/17/setup-sdk). Obecna dokumentacja nie udaje, że aktualizacja została wykonana.
