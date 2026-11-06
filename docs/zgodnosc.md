# Powiązanie wymagań z implementacją

Wersja Android: 1.2.1. Backend API: 1.2.0. Wykonawcy: Brajan Sieroń, Damian Panońko, Patryk Dawczak.

## Wymagania techniczne

| Wymaganie | Rozwiązanie |
|---|---|
| Język i środowisko | Kotlin, Android Studio, Gradle Wrapper 9.3.1 |
| Android SDK | compile 36.1, target 36, min 26 |
| Architektura | MVVM: Compose, TripViewModel, Repository, warstwa domain i data |
| Interfejs | Jetpack Compose, Material Design 3, motyw jasny i ciemny |
| Baza danych | Room/SQLite na urządzeniu, SQLite lub MySQL na backendzie |
| Sieć | Retrofit, OkHttp, REST API Python, integracje Open-Meteo i OpenStreetMap |
| Asynchroniczność | Coroutines, Flow, StateFlow i zadania WorkManager |
| Nawigacja i stan | Navigation Compose, ViewModel i rememberSaveable |
| Ustawienia | DataStore: motyw i adres serwera |
| Obsługa błędów | Walidacja formularzy, komunikaty, kolejka i konflikty wersji |
| Ochrona danych | Android Keystore, AES-GCM, HTTPS, role i walidacja serwera |
| Testy | 23 jednostkowe Android, 31 urządzeniowych i 45 backendu |
| Wersjonowanie | Git, repozytorium Yamoteusz/TripSync |
| Dokumentacja | PDF, dokumenty Markdown, diagramy, screeny, instrukcje i raporty |

## Wymagania funkcjonalne i niefunkcjonalne

CRUD obejmuje wyjazdy, punkty planu, zadania, propozycje, koszty, budżety, spłaty i miejsca. Formularze walidują wymagane pola, daty, godziny, kwoty i współrzędne. Aplikacja ma ponad cztery odrębne widoki, trwały zapis Room oraz obsługę błędów sieci i serwera.

UI korzysta z Material Design 3, etykiet kontrolek, motywów i przewijania. Sprawdzono odtworzenie aktywności oraz wybór dat i godzin na ekranie 320 dp ze skalą czcionki 1.3. Kod jest podzielony na ui, domain i data. Operacje bazy i sieci wykonywane są asynchronicznie.

Zakres testów i ograniczenia weryfikacji opisuje [testy.md](testy.md). Bieżące ograniczenia funkcjonalne zawiera [ograniczenia.md](ograniczenia.md). Konfiguracja projektu używa SDK 36.1; aktualizacja SDK stanowi oddzielną zmianę techniczną.

## Materiały projektu

Pełną listę wymaganych dokumentów, repozytorium i APK zawiera [oddanie.md](oddanie.md).
