# Cel, zakres i wymagania

## Cel projektu

TripSync porządkuje organizację podróży niewielkich grup znajomych: plan, zadania, pomysły i wspólne koszty. Aplikacja umożliwia wspólne ustalanie harmonogramu, dzielenie obowiązków i rozliczanie wydatków, również przy okresowym braku dostępu do sieci. Wersja aplikacji: Android 1.2.1; backend API 1.2.0.

## Role i odbiorcy

Gość może utworzyć konto, zalogować się lub skorzystać z demo. Uczestnik współpracuje przy planie, zadaniach, pomysłach, miejscach i kosztach. Organizator ma dodatkowe uprawnienia do zarządzania wyjazdem, zaproszeniami, uczestnikami i budżetem. System Android wykonuje zadania WorkManager i lokalne powiadomienia. Uprawnienia opisuje macierz w `przypadki-uzycia.md`.

## Wymagania funkcjonalne

| ID | Wymaganie | Kryterium odbioru |
|---|---|---|
| F01 | Rejestracja, logowanie, wylogowanie | Walidacja, sesja lokalna i unieważnienie tokenu przy wylogowaniu online |
| F02 | CRUD wyjazdów | Nazwa, cel, daty, waluta i opis; edycja/usunięcie przez organizatora |
| F03 | Uczestnicy i zaproszenia | Kod 12 znaków lub link; rotacja kodu i kontrola dostępu |
| F04 | CRUD harmonogramu | Data w terminie wyjazdu, godzina, miejsce, kolejność i wykonanie |
| F05 | Propozycje i głosy | CRUD propozycji, jeden własny głos na propozycję, cofnięcie i ponowne oddanie |
| F06 | Propozycja do planu | Jedna wersjonowana operacja zmienia pomysł w punkt, usuwa głosy i nie tworzy duplikatów |
| F07 | Zadania i pakowanie | CRUD, kategoria, osoba, wykonanie i opcjonalne przypomnienie |
| F08 | Wydatki | CRUD, płatnik, data, uczestnicy, podział równy lub kwoty własne |
| F09 | Rozliczenia | Dokładność groszy, suma sald zero i propozycje przelewów |
| F10 | Offline i trwałość | Lokalny zapis bez sieci, ponowny odczyt po uruchomieniu |
| F11 | Synchronizacja | Kolejka, ponowienia bez duplikatów, wersje oraz wybór wersji przy konflikcie |
| F12 | Powiadomienia lokalne | Przypomnienie punktu/zadania, anulowanie po wykonaniu/usunięciu |
| F13 | Stan i ustawienia | Zachowanie formularza po odtworzeniu aktywności, motyw i adres API w DataStore |
| F14 | Obsługa błędów | Walidacja, komunikaty sieci/serwera i stan odrzuconych operacji |
| F15 | Demo i udostępnianie | Edytowalny przykład offline oraz udostępnienie tekstowego planu |
| F16 | Budżet | CRUD limitu i kategorii, limit kategorii 0 i potwierdzanie usunięcia przez organizatora |
| F17 | Kategorie i lista kosztów | Sześć kategorii, zestawienie, filtr, wyszukiwanie i sortowanie |
| F18 | Spłaty | CRUD własnych spłat; salda uwzględniają spłatę częściową i nadpłatę |
| F19 | Historia zmian | Autorytatywne wpisy serwera z autorem i czasem, odczyt bez edycji klienta |
| F20 | Miejsca | CRUD współrzędnych, mapa wspólna i otwarcie w zewnętrznych mapach |
| F21 | Wyszukiwanie | Geokodowanie miejscowości i zapis wyniku jako miejsce |
| F22 | Prognoza | 7 dni, temperatury, opis oraz szansa opadów |
| F23 | Cache pogody | Ostatni wynik i daty dostępne po błędzie sieci |
| F24 | Daty i godziny | Kalendarz i zegar 24-godzinny; w bazie ISO, prezentacja po polsku |
| F25 | Ochrona formularza | Potwierdzenie odrzucenia zmian w edytorze głównym, budżecie, spłacie i miejscu |
| F26 | Widoki wyjazdów i planu | Wyszukiwanie, aktywne/zakończone wyjazdy, plan według dni |

## Wymagania niefunkcjonalne

| ID | Wymaganie | Realizacja i granice |
|---|---|---|
| N01 | Material 3 i responsywność | Compose, motywy, przewijanie, maksymalna szerokość i kompaktowe pickery; szeroka weryfikacja tabletów pozostaje do wykonania |
| N02 | Architektura i modularność | MVVM, foldery ui/domain/data i odrębny backend; jedna aplikacja Gradle, nie pełna Clean Architecture |
| N03 | Asynchroniczność | Coroutines, Flow, suspend Room/Retrofit i I/O poza głównym wątkiem |
| N04 | Zmiana konfiguracji | ViewModel i rememberSaveable; test odtworzenia aktywności, bez pełnej macierzy wszystkich konfiguracji |
| N05 | Ochrona danych | Keystore, HTTPS, kontrola dostępu i walidacja; baza Room bez SQLCipher |
| N06 | Dostępność | Etykiety pól i ikon funkcyjnych, kontrast i większa czcionka; audyt TalkBack pozostaje do wykonania |
| N07 | Testy | Domena Kotlin, backend, Compose UI i integracja REST |
| N08 | Rozbudowa | Typowane modele, wersjonowane operacje, repozytorium i eksport schematu Room |
| N09 | Git i dokumentacja | Historia, README, diagramy, testy, screeny, PDF i APK |

Powiązanie wymagań z implementacją: [zgodnosc.md](zgodnosc.md). Ograniczenia: [ograniczenia.md](ograniczenia.md). Wykonawcy: [AUTHORS.md](../AUTHORS.md).
