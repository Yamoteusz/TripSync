# Publikacja TripSync przez GitHub Desktop

Projekt ma już lokalne repozytorium Git i historię zmian. Publikuj istniejący katalog, zachowując podział na `app`, `backend`, `docs`, `deploy` i `scripts`.

## Pierwsza publikacja

1. W GitHub Desktop zaloguj się na swoje konto.
2. Wybierz **File → Add Local Repository…** i wskaż katalog `Android`, zawierający ten README oraz katalog `.git`. Nie wybieraj samego `app` ani rozpakowanego ZIP-a.
3. Kliknij **Add Repository**. Sprawdź, że aktywna gałąź to `main`.
4. Kliknij **Publish repository**, wpisz nazwę `TripSync` i pozostaw **Keep this code private**.
5. Kliknij **Publish Repository**. Link do opublikowanego repozytorium możesz dodać do dokumentacji projektu.
6. Zaproś dwie pozostałe osoby w ustawieniach repozytorium na GitHubie. Wispbyte połącz z tym repozytorium i gałęzią `main`.

Nie skonfigurowano jeszcze zdalnego repozytorium ani publikacji. Logowanie do GitHub Desktop i wybór konta wykonuje właściciel.

## Kolejne aktualizacje

Przed rozpoczęciem pracy wybierz **Fetch origin**, a następnie **Pull origin**, jeśli są zmiany zespołu. Po wykonaniu i sprawdzeniu konkretnej funkcji zaznacz jej pliki w zakładce **Changes**, wpisz opis w **Summary**, kliknij **Commit to main**, a następnie **Push origin**. Commit zapisuje zmiany lokalnie; push wysyła je na GitHub. Nie twórz fikcyjnych commitów przez wysyłanie gotowych folderów w odstępach czasu.

Przy równoległej pracy twórzcie osobne gałęzie i pull requesty. Przed połączeniem sprawdzajcie wyniki **Actions → Project checks**. Workflow buduje debug APK, uruchamia testy jednostkowe Android, lint i testy backendu. APK można pobrać z artefaktów zakończonego przebiegu. Testy emulatora pozostają osobnym krokiem opisanym w README. Workflow został przygotowany lokalnie; pierwszy rzeczywisty przebieg nastąpi po publikacji.

## Pliki lokalne

`.gitignore` wyklucza bazę i kopie, `.env`, pliki podpisu aplikacji, lokalną ścieżkę SDK, cache, APK oraz pliki budowania. Plik `gradle/gradle-daemon-jvm.properties` wygenerowany przez Android Studio pozostaje lokalny; wymagania JDK opisano w README.

Przykłady `*.env.example` są szablonami z pustym hasłem. Pliki `*.env.example` nie są odczytywane automatycznie; skopiuj wzór do `.env` na serwerze. Hasło MySQL wpisz jako `TRIPSYNC_MYSQL_PASSWORD` w zmiennych Wispbyte. Nie umieszczaj go w komendzie startowej ani w GitHubie.

## Wispbyte z MySQL

Po podłączeniu repozytorium do serwera ustaw Python 3.10+ i komendę:

```sh
python3 -m pip install -r requirements.txt && python3 main.py
```

Zmienne: `TRIPSYNC_DATABASE_ENGINE=mysql`, dane połączenia i hasło z panelu bazy, oraz `PORT` z przydziału serwera aplikacji. Port bazy `3306` nie jest portem API. Domenę HTTPS przypisz do API zgodnie z [instrukcją Wispbyte](wispbyte.md).

Aktualizacja kodu z repozytorium nie zawiera danych MySQL i nie kasuje istniejących tabel. Przed wdrożeniem zachowaj kopię bazy; po pierwszym wdrożeniu sprawdź rejestrację, zapis wyjazdu oraz odczyt po restarcie. Pierwsze wdrożenia uruchamiaj ręcznie z panelu po sprawdzeniu zmian.

Instrukcje GitHub Desktop: [dodanie lokalnego repozytorium](https://docs.github.com/en/desktop/adding-and-cloning-repositories/adding-a-repository-from-your-local-computer-to-github-desktop), [publikacja istniejącego projektu](https://docs.github.com/en/desktop/adding-and-cloning-repositories/adding-an-existing-project-to-github-using-github-desktop).
