# Materiały do oddania - TripSync

**Wykonawcy:** Brajan Sieroń, Damian Panońko, Patryk Dawczak.

**Wersja aplikacji:** 1.2.1, kod wersji 4.

**Data dokumentacji:** 06.11.2026.

## Wykaz wymaganych materiałów

| Wymagany element | Materiał |
|---|---|
| Opis celu i zakresu aplikacji | [Cel, zakres i wymagania](wymagania.md) oraz rozdział PDF |
| Wymagania funkcjonalne i niefunkcjonalne | Specyfikacja F01-F26 i N01-N09 w [wymagania.md](wymagania.md) |
| Diagram architektury aplikacji | [Opis MVVM](architektura.md), [diagram SVG](diagrams/architektura.svg) i PDF |
| Diagram przypadków użycia | [UML i scenariusze](przypadki-uzycia.md), [diagram SVG](diagrams/przypadki-uzycia.svg) i PDF |
| Diagram bazy danych | [Model danych](baza-danych.md), [Room](diagrams/baza-lokalna.svg), [SQLite/MySQL](diagrams/baza-backendu.svg) i PDF |
| Zrzuty najważniejszych widoków | Osiem najważniejszych widoków w PDF; szersza galeria dostępna w repozytorium |
| Biblioteki i technologie | [Technologie, wersje i zastosowanie](technologie.md) |
| Instrukcja instalacji i uruchomienia | [Android Studio, APK i backend lokalny](uruchomienie.md); [Wispbyte i MySQL](wispbyte.md) |
| Testy i ich wyniki | Opis i wyniki 99 testów w PDF; raporty źródłowe dostępne w repozytorium |
| Link do repozytorium Git | [github.com/Yamoteusz/TripSync](https://github.com/Yamoteusz/TripSync) |
| Działająca aplikacja | APK debug wersji 1.2.1 w paczce do oddania |

## Dokument PDF i paczka

Dokument do oddania: [PDF](../output/pdf/TripSync_dokumentacja_do_oddania.pdf).

Paczka `output/TripSync_do_oddania.zip` zawiera dokładnie dwa pliki, bez podfolderów:

- `TripSync_Dokumentacja.pdf` - cel, zakres, wymagania, diagramy architektury, UML i obu baz, osiem screenów, technologie, instrukcja instalacji, wyniki testów, wykonawcy i link do Git.
- `TripSync.apk` - aplikacja 1.2.1 na Android 8.0 lub nowszy.

Wszystkie wymagane materiały opisowe są w jednym PDF. Kod aplikacji i backendu oraz dodatkowe raporty są dostępne w repozytorium wskazanym w dokumencie. Generowanie PDF i paczki: `python3 scripts/create_submission.py` (zależności z `scripts/requirements-docs.txt`). Ponowne spakowanie istniejących plików: `python3 scripts/package_project.py`.

## Uruchomienie prezentacji

1. Rozpakuj paczkę i zainstaluj `TripSync.apk` na telefonie lub emulatorze Android 8.0+.
2. Wybierz **Wypróbuj bez konta**, aby uruchomić lokalny przykład.
3. Do współpracy wielu osób użyj odrębnych kont na tym samym API. Adres domyślny: `https://tripsync.wisp.uno/`.
4. Pełny scenariusz pokazu znajduje się w [prezentacja.md](prezentacja.md).

APK jest podpisany kluczem debug i służy do instalacji testowej. Wyniki obejmują 99 testów wykonanych 06.11.2026. Przed aktualizacją APK należy wdrożyć backend API 1.2.0 zgodnie z instrukcją Wispbyte.
