# Cel, zakres i wymagania

## Cel

TripSync porządkuje wspólne wyjazdy w jednym miejscu i zapewnia dostęp do danych podczas braku sieci. Odbiorcami są niewielkie grupy znajomych. Projekt sprawdza projektowanie, implementację, testowanie i dokumentowanie aplikacji Android, w rozszerzonym zakresie dla trzech osób.

## Wymagania funkcjonalne

| ID | Wymaganie | Kryterium odbioru |
|---|---|---|
| F01 | Rejestracja, logowanie i wylogowanie | Walidacja adresu i hasła, szyfrowana lokalna sesja, wyczyszczenie danych po wylogowaniu |
| F02 | CRUD wyjazdów | Nazwa, miejsce, daty, waluta i opis; edycja/usunięcie tylko przez organizatora |
| F03 | Uczestnicy i zaproszenia | Dołączenie przez 12-znakowy kod lub link; unieważnienie poprzedniego kodu |
| F04 | CRUD harmonogramu | Data w terminie wyjazdu, godzina, miejsce, opis, kolejność i status wykonania |
| F05 | Propozycje i głosowanie | Dodawanie/edycja/usuwanie propozycji, jeden głos użytkownika na propozycję, cofnięcie głosu |
| F06 | Przeniesienie propozycji do planu | Powstaje punkt planu, propozycja znika z listy po potwierdzeniu formularza |
| F07 | Zadania i pakowanie | CRUD, dwie kategorie, osoba odpowiedzialna, status wykonania |
| F08 | CRUD wydatków | Kwota, data, płatnik, wybrani uczestnicy; podział równy lub własne kwoty |
| F09 | Salda i rozliczenia | Dokładność najmniejszej jednostki waluty, suma sald zero, propozycje przelewów |
| F10 | Offline i trwałość | CRUD lokalnie bez sieci; dane odtwarzane po ponownym uruchomieniu |
| F11 | Synchronizacja i konflikty | Kolejka, ponowienia bez duplikatów, wykrywanie wersji, wybór lokalnej/zdalnej wersji |
| F12 | Powiadomienia lokalne | Włączanie dla punktu/zadania, anulowanie po wykonaniu/usunięciu, zgoda systemowa |
| F13 | Nawigacja i ustawienia | Stan formularza po odtworzeniu aktywności, jasny/ciemny motyw zapisany w DataStore |
| F14 | Obsługa błędów | Komunikaty walidacji, sieci i serwera; status oczekujących/odrzuconych zmian |
| F15 | Prezentacja i udostępnianie | Edytowalny przykład bez konta, eksport planu przez systemowe udostępnianie |

## Wymagania niefunkcjonalne

| ID | Wymaganie | Realizacja |
|---|---|---|
| N01 | Material Design 3 i responsywność | Compose, przewijanie, ograniczenie maksymalnej szerokości, elastyczne wiersze |
| N02 | Modularność | UI / domain / data, ViewModel i repozytorium, odrębny backend |
| N03 | Asynchroniczność | Coroutines, Flow, suspend Room/Retrofit, WorkManager; brak sieci na głównym wątku |
| N04 | Odporność na konfigurację | ViewModel, rememberSaveable, nawigacja Compose, trwały Room |
| N05 | Ochrona danych | Keystore, HTTPS, prywatny katalog, brak backupu, autoryzacja serwera |
| N06 | Dostępność | Material components, opisy przycisków ikonowych, etykiety pól, kontrast i skalowanie tekstu |
| N07 | Testowalność | Testy domeny, testy backendu, testy Compose i rzeczywistej synchronizacji |
| N08 | Możliwość rozbudowy | Typowane payloady, repozytorium, wersjonowany schemat bazy |
| N09 | Wersjonowanie i dokumentacja | Git, instrukcja startu, diagramy, screeny, raport testów i APK |

## Role

Gość loguje się lub uruchamia demo. Uczestnik dołącza i współpracuje przy planie, zadaniach, propozycjach i kosztach. Organizator dodatkowo zarządza wyjazdem i uczestnikami. System Android wykonuje synchronizację w tle i przypomnienia.

## Zakres trzyosobowy

| Obszar | Odpowiedzialność zespołowa |
|---|---|
| Konta, backend, wyjazdy, uprawnienia | Obszar osoby 1 |
| Harmonogram, zadania, głosowanie, interfejs | Obszar osoby 2 |
| Wydatki, rozliczenia, offline i synchronizacja | Obszar osoby 3 |

Każdy obszar obejmuje własne testy i dokumentację. Jest to proponowany podział odpowiedzialności, nie oświadczenie o autorstwie konkretnych osób.
