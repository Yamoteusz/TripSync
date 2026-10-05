# Foldery i aktualizacje projektu

Projekt ma jedną aplikację Android i odrębny backend. Podział folderów umożliwia osobną pracę nad funkcjami; wszystkie katalogi razem tworzą działający projekt. Nie należy wgrywać samych ekranów bez wspólnych zależności.

| Folder | Zakres |
|---|---|
| `backend/` | API, konta, kontrola dostępu, synchronizacja i testy Python |
| `app/src/main/java/pl/tripsync/domain/` | Modele, walidacja i algorytm rozliczeń |
| `app/src/main/java/pl/tripsync/data/` | Room, kolejka, konflikty, Retrofit, DataStore i Keystore |
| `app/src/main/java/pl/tripsync/ui/screens/auth/` | Logowanie i rejestracja |
| `app/src/main/java/pl/tripsync/ui/screens/trips/` | Lista wyjazdów |
| `app/src/main/java/pl/tripsync/ui/screens/trip/` | Podsumowanie i połączenie zakładek |
| `app/src/main/java/pl/tripsync/ui/screens/plan/` | Harmonogram |
| `app/src/main/java/pl/tripsync/ui/screens/tasks/` | Zadania i pakowanie |
| `app/src/main/java/pl/tripsync/ui/screens/ideas/` | Propozycje i głosowanie |
| `app/src/main/java/pl/tripsync/ui/screens/money/` | Koszty, kategorie, salda i CRUD spłat |
| `app/src/main/java/pl/tripsync/ui/screens/budget/` | Budżet i limity kategorii |
| `app/src/main/java/pl/tripsync/ui/screens/activity/` | Historia zmian |
| `app/src/main/java/pl/tripsync/ui/screens/places/` | Mapa, miejsca, wyszukiwanie i pogoda |
| `deploy/` | Hosting WSGI oraz alternatywnie Docker/Caddy |
| `app/src/main/java/pl/tripsync/ui/screens/editor/` | Formularze dodawania i edycji |
| `app/src/main/java/pl/tripsync/ui/screens/members/` | Uczestnicy i zaproszenia |
| `app/src/main/java/pl/tripsync/ui/screens/conflicts/` | Rozstrzyganie konfliktów |
| `app/src/main/java/pl/tripsync/ui/screens/settings/` | Ustawienia i sesja |
| `app/src/test/` i `app/src/androidTest/` | Testy domeny, interfejsu i synchronizacji |
| `docs/` | Dokumentacja, diagramy i screeny |
| `scripts/` | Powtarzalne budowanie materiałów końcowych |

## Zasada aktualizacji w Git

Commit powinien opisywać rzeczywistą zmianę. Nowe funkcje, poprawki i testy dodawaj w miarę ich realizacji. Zmiany w powiązanych folderach można zatwierdzać razem, aby każdy etap zachował spójne zależności.

Przykład kolejnej rzeczywistej poprawki harmonogramu:

```sh
git switch -c codex/plan-poprawka
```

Po wprowadzeniu i sprawdzeniu zmiany:

```sh
git add app/src/main/java/pl/tripsync/ui/screens/plan
git commit -m "fix(plan): popraw wyświetlanie kolejności punktów"
```

Jeżeli poprawka wymaga zmiany repozytorium albo testu, dodaj je w tym samym commicie. Repozytorium origin jest skonfigurowane jako https://github.com/Yamoteusz/TripSync. Zatwierdzone zmiany wysyłaj przez Push origin w GitHub Desktop. Faktyczny wkład trzech osób uzupełnij w AUTHORS.md.

## Zawartość paczki

Paczka do oddania obejmuje źródła, Gradle Wrapper, backend, dokumentację, screeny, APK i raporty. Nie zawiera prywatnych sesji, baz testowych, lokalnych ścieżek SDK ani cache narzędzi. Git bundle pozwala odtworzyć lokalną historię również bez zdalnego serwera.

## Zakres dla trzech osób po rozbudowie

| Osoba | Główne moduły | Weryfikacja |
|---|---|---|
| 1 | Konta, uprawnienia, API, WSGI/hosting, historia i kopie bazy | Testy backendu, idempotencja, trwałość i instrukcja wdrożenia |
| 2 | Plan, zadania, pomysły, mapa wspólnych miejsc, Open-Meteo i cache prognozy | Formularze, błędy API, UI i dokumentacja map/pogody |
| 3 | Wydatki i kategorie, budżety, spłaty, algorytm sald, Room/synchronizacja | Testy domeny i integracja między kontami |

To propozycja odpowiedzialności do dalszej pracy i obrony. Każda osoba powinna rozumieć swój zakres oraz wspólną architekturę.
