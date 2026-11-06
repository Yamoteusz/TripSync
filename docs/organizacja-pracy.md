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

Jeżeli poprawka wymaga zmiany repozytorium albo testu, dodaj je w tym samym commicie. Repozytorium origin jest skonfigurowane jako https://github.com/Yamoteusz/TripSync. Zatwierdzone zmiany wysyłaj przez Push origin w GitHub Desktop. Wykonawcy: Brajan Sieroń, Damian Panońko, Patryk Dawczak.

## Zawartość paczki

Paczka `output/TripSync_do_oddania.zip` zawiera tylko `TripSync_Dokumentacja.pdf` i `TripSync.apk`. PDF obejmuje wszystkie wymagane opisy, diagramy, screeny, wyniki testów, wykonawców i link do repozytorium. Źródła, Gradle Wrapper, backend i szczegółowe raporty pozostają w repozytorium Git. Paczkę z istniejącego PDF i APK odtwarza `python3 scripts/package_project.py`; po udanym budowaniu publikuje ją również GitHub Actions jako artefakt TripSync-do-oddania.
