# Biblioteki i technologie

Poniższa tabela przedstawia technologie i wersje deklarowane w konfiguracji projektu. Wersje zależności przechodnich są rozwiązywane przez Gradle.

| Technologia | Deklaracja / wersja | Zastosowanie |
|---|---|---|
| Kotlin | Wbudowany Kotlin AGP; plugin Compose 2.2.10 | Android, modele i domena |
| Android Gradle Plugin | 9.1.1 | Budowanie aplikacji |
| Gradle Wrapper | 9.3.1 | Powtarzalne uruchamianie Gradle |
| Android SDK | compile 36.1, target 36, min 26 | Kompilacja i zgodność Android 8.0+ |
| Java / JDK | Kod Java 17; sprawdzono JDK 21 | Narzędzia budowania |
| Compose BOM | 2024.01.00 | Deklarowana platforma zależności Compose |
| Compose / Material 3 | Wersje zarządzane grafem Gradle | Ekrany, motywy i kontrolki |
| Activity Compose | 1.13.0 | Aktywność, stan i obsługa Wstecz |
| Lifecycle | 2.9.0 | ViewModel i zbieranie stanu zgodnie z cyklem życia |
| Navigation Compose | 2.7.7 | Nawigacja wieloekranowa |
| Room | 2.8.4 | SQLite lokalne, DAO, transakcje i Flow |
| KSP | 2.2.10-2.0.2 | Generowanie implementacji Room |
| DataStore | 1.1.7 | Motyw i adres API |
| WorkManager | 2.9.0 | Okresowa synchronizacja i przypomnienia |
| Coroutines | 1.10.2 | Asynchroniczność, Flow i współbieżność |
| Retrofit / converter-gson | 2.9.0 | Klienci REST i DTO |
| OkHttp | 4.12.0 | Transport HTTP |
| Gson | 2.11.0 | Kodowanie payloadów i DTO |
| Android Keystore / AES-GCM | API Android | Szyfrowana sesja lokalna |
| Python | 3.10+; lokalnie testowano 3.13 | Backend REST i testy |
| SQLite | Biblioteka standardowa Python | Lokalny backend i testy |
| PyMySQL | 1.2.3, wariant rsa | Adapter bazy MySQL na hostingu |
| Leaflet | 1.9.4 | Biblioteka map dołączona lokalnie w assets |
| OpenStreetMap | Kafelki HTTPS | Obraz mapy i atrybucja |
| Open-Meteo | Geocoding / forecast API | Miejscowości i 7-dniowa pogoda |
| JUnit | 4.13.2 | Testy jednostkowe Kotlin |
| AndroidX Test | runner 1.7.0, ext-junit 1.3.0 | Testy urządzeniowe |
| Espresso | 3.7.0 | Test kompaktowego systemowego kalendarza |
| Compose UI Test | Graf Compose | Testy formularzy i nawigacji |
| ReportLab | Narzędzie generatora dokumentacji | PDF, bez zależności aplikacji od Pythona |

## Licencje i źródła

Leaflet ma licencję BSD-2-Clause dołączoną w `app/src/main/assets/leaflet/LICENSE`. Mapa wyświetla atrybucję OpenStreetMap. Warunki usług Open-Meteo i kafelków trzeba respektować także w projekcie bezpłatnym; nie pobieramy całych obszarów map offline. Przed komercyjną rozbudową trzeba ponownie sprawdzić warunki dostawców.

- [Compose i BOM](https://developer.android.com/develop/ui/compose/bom)
- [MVVM / architektura Android](https://developer.android.com/topic/architecture)
- [Room](https://developer.android.com/training/data-storage/room)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
- [Retrofit](https://square.github.io/retrofit/)
- [Leaflet](https://leafletjs.com/)
- [Open-Meteo](https://open-meteo.com/en/docs)
- [Polityka kafelków OSM](https://operations.osmfoundation.org/policies/tiles/)
