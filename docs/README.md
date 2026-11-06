# Dokumentacja TripSync 1.2.1

Stan dokumentacji: 06.11.2026. Wersja aplikacji: 1.2.1. Wykonawcy: **Brajan Sieroń, Damian Panońko, Patryk Dawczak**.

[Pakiet do oddania](oddanie.md) zawiera wykaz wszystkich wymaganych materiałów. Wyniki testów aplikacji pochodzą z przebiegów z 06.11.2026.

| Dokument | Zawartość |
|---|---|
| [Materiały do oddania](oddanie.md) | Wykaz dokumentów, wykonawców, repozytorium i APK |
| [Cel i wymagania](wymagania.md) | Zakres, role, wymagania funkcjonalne i niefunkcjonalne |
| [Funkcje i screeny](funkcje.md) | Instrukcja korzystania, scenariusze użytkownika i galeria |
| [Architektura](architektura.md) | MVVM, przepływ danych, offline, kolejka, konflikty i WorkManager |
| [Bazy danych](baza-danych.md) | Fizyczne schematy Room oraz SQLite/MySQL i logiczne payloady |
| [Przypadki użycia](przypadki-uzycia.md) | Aktorzy, uprawnienia, diagram UML i scenariusze |
| [API](api.md) | Endpointy, kontrakt JSON, wersje rekordów i błędy |
| [Technologie](technologie.md) | Biblioteki, zadania i źródła dokumentacji |
| [Instalacja i uruchomienie](uruchomienie.md) | Android Studio, Mac, APK, lokalne API i emulator |
| [Wispbyte i MySQL](wispbyte.md) | Environment, `.env`, domyślny startup i diagnostyka |
| [Testy i wyniki](testy.md) | 99 testów, odtworzenie przebiegów, raporty i ograniczenia |
| [Bezpieczeństwo](bezpieczenstwo.md) | Kontrola dostępu, sesje, TLS, walidacja i granice ochrony |
| [Zgodność z wytycznymi](zgodnosc.md) | Powiązanie wymagań oraz kryteriów oceny z dowodami |
| [Prezentacja i obrona](prezentacja.md) | Scenariusz pokazu i pytania techniczne |
| [Struktura projektu](organizacja-pracy.md) | Podział folderów i pliki projektu |
| [GitHub Desktop](github-desktop.md) | Commity, push, gałęzie i workflow |
| [Ograniczenia](ograniczenia.md) | Granice bieżącej wersji aplikacji |
| [Poprawki 1.2.1](zmiany-1.2.1.md) | Atomowa konwersja, formularze, przypomnienia i limity |
| [Zmiany UI 1.2](ui-1.2.md) | Kalendarze, motywy, logo i poprawione błędy |

**Dokument do oddania:** [PDF](../output/pdf/TripSync_dokumentacja_do_oddania.pdf). Generator: `scripts/create_submission.py`. Paczka `output/TripSync_do_oddania.zip` zawiera tylko PDF i APK. **Autorzy:** [AUTHORS.md](../AUTHORS.md). **Repozytorium:** [GitHub](https://github.com/Yamoteusz/TripSync). Rozszerzony dokument techniczny można odtworzyć przez `scripts/create_documentation.py`.

Architektura i bazy mają edytowalne źródła Mermaid w dokumentach. Wszystkie diagramy mają samodzielne pliki SVG w [diagrams/](diagrams/). [UML przypadków użycia](diagrams/przypadki-uzycia.svg) zawiera aktorów, granicę systemu, asocjacje i generalizację roli organizatora.

Aktualne wyniki: [test-results-1.2.1.json](test-results-1.2.1.json), raporty [output/reports/1.2.1/](../output/reports/1.2.1/). `test-results.json`, `test-results-1.2.json`, starsze raporty oraz `screenshots/` są materiałem archiwalnym poprzednich przebiegów. Obecny interfejs pokazuje `screenshots-1.2.1/`; starszy screen pogody ma osobno opisane pochodzenie.

## Odtworzenie diagramów i PDF

Generator korzysta z ReportLab i Pillow. Z katalogu głównego repozytorium:

```sh
python3 -m venv .venv
.venv/bin/python -m pip install -r scripts/requirements-docs.txt
.venv/bin/python scripts/create_submission.py
```

Na Windows użyj `.venv\Scripts\python.exe` zamiast `.venv/bin/python`. Potrzebna jest czcionka Arial (typowo obecna na macOS) albo DejaVu Sans w systemowej lokalizacji Linuksa wskazanej w generatorze. Dla innego systemu ustaw dostępne ścieżki fontów. Generator korzysta z istniejących czterech SVG, bieżących wymagań, wyników testów i ośmiu screenów wersji 1.2.1. Zapisuje PDF i pakuje go z gotowym APK. Diagramy odtwarza `python3 scripts/create_diagrams.py` bez dodatkowych bibliotek.

Po zmianach przejrzyj wygenerowane strony. Generator nie uruchamia testów aplikacji ani nie aktualizuje ich wyników. Samo ponowne spakowanie gotowego PDF i APK: `python3 scripts/package_project.py`.
