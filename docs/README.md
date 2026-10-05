# Dokumentacja TripSync 1.2.0

Stan dokumentacji: 05.10.2026. Materiały opisują aktualny kod i zachowane wyniki wykonanych testów. Jest to wersja robocza do dalszego rozwijania na zajęciach; brak nazwisk autorów i zadań końcowych jest jawnie zaznaczony.

| Dokument | Zawartość |
|---|---|
| [Cel i wymagania](wymagania.md) | Zakres, role, wymagania funkcjonalne i niefunkcjonalne |
| [Funkcje i screeny](funkcje.md) | Instrukcja korzystania, scenariusze użytkownika i galeria |
| [Architektura](architektura.md) | MVVM, przepływ danych, offline, kolejka, konflikty i WorkManager |
| [Bazy danych](baza-danych.md) | Fizyczne schematy Room oraz SQLite/MySQL i logiczne payloady |
| [Przypadki użycia](przypadki-uzycia.md) | Aktorzy, uprawnienia, diagram UML i scenariusze |
| [API](api.md) | Endpointy, kontrakt JSON, wersje rekordów i błędy |
| [Technologie](technologie.md) | Biblioteki, zadania i źródła dokumentacji |
| [Instalacja i uruchomienie](uruchomienie.md) | Android Studio, Mac, APK, lokalne API i emulator |
| [Wispbyte i MySQL](wispbyte.md) | Environment, `.env`, domyślny startup i diagnostyka |
| [Testy i wyniki](testy.md) | 78 testów, odtworzenie przebiegów, raporty i ograniczenia |
| [Bezpieczeństwo](bezpieczenstwo.md) | Kontrola dostępu, sesje, TLS, walidacja i granice ochrony |
| [Zgodność z wytycznymi](zgodnosc.md) | Powiązanie wymagań oraz kryteriów oceny z dowodami |
| [Prezentacja i obrona](prezentacja.md) | Scenariusz pokazu i pytania techniczne |
| [Organizacja pracy](organizacja-pracy.md) | Podział folderów i proponowane obszary dla trzech osób |
| [GitHub Desktop](github-desktop.md) | Commity, push, gałęzie i workflow |
| [Ograniczenia i dalsze prace](ograniczenia.md) | Zrealizowany zakres oraz rzeczy niezaimplementowane |
| [Zmiany UI 1.2](ui-1.2.md) | Kalendarze, motywy, logo i poprawione błędy |

**Dokument zbiorczy:** [TripSync_dokumentacja_projektu.pdf](../output/pdf/TripSync_dokumentacja_projektu.pdf). Generator: `scripts/create_documentation.py`. **Autorzy:** [AUTHORS.md](../AUTHORS.md). **Repozytorium:** [GitHub](https://github.com/Yamoteusz/TripSync).

Architektura i bazy mają edytowalne źródła Mermaid w dokumentach. Wszystkie diagramy mają samodzielne pliki SVG w [diagrams/](diagrams/). [UML przypadków użycia](diagrams/przypadki-uzycia.svg) zawiera aktorów, granicę systemu, asocjacje i generalizację roli organizatora.

Aktualne wyniki: [test-results-1.2.json](test-results-1.2.json), raporty [output/reports/ui-1.2/](../output/reports/ui-1.2/). `test-results.json`, starsze raporty oraz `screenshots/` są materiałem archiwalnym poprzednich przebiegów. Obecny interfejs pokazuje `screenshots-1.2/`; starszy screen pogody ma osobno opisane pochodzenie.

## Odtworzenie diagramów i PDF

Generator korzysta z ReportLab i Pillow. Z katalogu głównego repozytorium:

```sh
python3 -m venv .venv
.venv/bin/python -m pip install -r scripts/requirements-docs.txt
.venv/bin/python scripts/create_documentation.py
```

Na Windows użyj `.venv\Scripts\python.exe` zamiast `.venv/bin/python`. Potrzebna jest czcionka Arial (typowo obecna na macOS) albo DejaVu Sans w systemowej lokalizacji Linuksa wskazanej w skrypcie. Dla innego systemu ustaw dostępne ścieżki fontów w generatorze. Skrypt odtwarza cztery SVG, czyta dokumenty Markdown i `AUTHORS.md`, następnie zapisuje PDF. Do samych diagramów wystarcza `python3 scripts/create_diagrams.py` bez dodatkowych bibliotek.

Po zmianach przejrzyj wygenerowane strony. Generator nie uruchamia testów aplikacji i nie aktualizuje samodzielnie ich wyników. Zestaw docelowy pakujesz dopiero po zapisaniu zmian w Git.
