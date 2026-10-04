# TripSync 1.1 — prawdziwe konta i darmowy hosting

## Polecany wariant: PythonAnywhere, plan Beginner Free

Backend ma adapter WSGI (`backend/wsgi.py`) do hostingu PythonAnywhere. SQLite zapisuje prawdziwe konta, wyjazdy i zmiany na dysku konta. Nie potrzebujesz domeny ani płatnego klucza API. Android łączy się przez HTTPS. Dane demonstracyjne nie są wysyłane na serwer.

Sprawdzone 04.10.2026: darmowy plan obejmuje jedną aplikację internetową i 512 MB prywatnych plików. Ma ograniczenia wydajności, dostępu serwera do zewnętrznych stron oraz wsparcie społeczności. Nowe darmowe konta nie mają MySQL ani zadań cyklicznych. Pilnuj daty wygaśnięcia aplikacji w zakładce Web i odnawiaj ją w panelu — od 2026 r. nieużywane aplikacje wygasają po miesiącu. To wariant do projektu studenckiego i małej grupy.

Źródła: [cennik](https://www.pythonanywhere.com/pricing/), [zmiany darmowego planu](https://blog.pythonanywhere.com/221/), [konfiguracja aplikacji](https://help.pythonanywhere.com/pages/Flask/).

Pogoda i geokodowanie są pobierane przez telefon bezpośrednio z Open-Meteo, a kafelki map z OpenStreetMap. Ograniczenia połączeń wychodzących darmowego backendu nie blokują tych funkcji.

## Instalacja krok po kroku

1. Utwórz darmowe konto na `https://www.pythonanywhere.com/` (Beginner). Zanotuj nazwę użytkownika.
2. W **Consoles → Bash** utwórz katalog: `mkdir -p ~/TripSync/backend`.
3. W **Files** przejdź do `/home/TWOJ_LOGIN/TripSync/backend` i wgraj `backend/server.py`, `backend/wsgi.py` i `backend/backup.py` z projektu. Wystarczą te trzy niewielkie pliki; nie trzeba wgrywać APK ani całej paczki. Jeśli masz repozytorium, możesz zamiast tego sklonować je do `~/TripSync` przez HTTPS.
4. W **Web → Add a new web app** wybierz bezpłatną domenę z panelu, **Manual configuration**, następnie Python 3.10 lub nowszy. Nie uruchamiaj `server.py` w konsoli jako usługi.
5. W zakładce Web otwórz plik konfiguracji **WSGI**. Zastąp jego treść zawartością `deploy/pythonanywhere_wsgi.py.example`. Szablon sam używa katalogu domowego konta. Katalog projektu musi nazywać się `TripSync`.
6. Kliknij **Reload** w zakładce Web. Baza powstanie w `/home/TWOJ_LOGIN/tripsync-data.sqlite`, poza katalogiem kodu. Przy aktualizacji nie usuwaj tego pliku ani plików `-wal`/`-shm` działającej bazy.
7. Otwórz w przeglądarce `https://TWOJ_LOGIN.pythonanywhere.com/health`. Poprawny wynik: `{"status":"ok","version":"1.1.0"}`. Jeśli korzystasz z regionu EU, użyj dokładnie domeny pokazanej w panelu.
8. Zainstaluj najnowszy APK 1.1.0. W TripSync wyloguj się z demo, otwórz **Ustawienia** i wpisz `https://TWOJ_LOGIN.pythonanywhere.com/` jako adres API. Zapisz adres przed zalogowaniem. Każda osoba musi wpisać ten sam adres.
9. Zarejestruj trzy oddzielne konta. E-mail służy jako identyfikator; projekt nie wysyła e-maili potwierdzających. Organizator tworzy własny wyjazd i synchronizuje go, potem udostępnia kod w **Uczestnicy i zaproszenia**. Pozostałe osoby wybierają **Dołącz kodem**.
10. Dodaj rzeczywisty wydatek na jednym urządzeniu, zsynchronizuj oba i sprawdź dane na drugim. Następnie wyłącz internet, zmień zadanie, włącz internet i sprawdź, czy kolejka została wysłana.

Nie publikujemy hasła do hostingu w repozytorium. Uczestnicy używają wyłącznie kont aplikacji, nie konta PythonAnywhere.

## Kopia bazy i aktualizacje

Przed aktualizacją uruchom w konsoli Bash hostingu:

```sh
mkdir -p ~/tripsync-backups
python3 ~/TripSync/backend/backup.py ~/tripsync-data.sqlite ~/tripsync-backups/przed-aktualizacja.sqlite
```

Kopia musi mieć nową nazwę. Narzędzie robi spójną kopię przez API SQLite (uwzględnia WAL) i sprawdza jej integralność. Pobierz plik z Files na własny komputer; zawiera prywatne dane i sesje, więc nie dodawaj go do Git. Darmowy plan nie ma automatycznego harmonogramu kopii — wykonuj je ręcznie. Jeśli istnieje już plik kopii, użyj nowej nazwy z datą.

Aktualizacja: zrób kopię, wgraj nowe pliki `server.py` i `wsgi.py` pod te same ścieżki i kliknij **Reload**. Bazy nie podmieniaj. Zainstaluj nowy APK jako aktualizację (`adb install -r ...` lub normalny instalator), zachowując ten sam podpis debug.

Odtwarzanie: w panelu wyłącz aplikację (**Disable**, jeśli dostępne; w razie potrzeby tymczasowo zastąp WSGI prostą odpowiedzią 503 i wykonaj Reload), upewnij się, że żaden proces nie używa SQLite, przenieś obecną bazę i jej pliki WAL/SHM do katalogu awaryjnego, skopiuj spójną kopię pod nazwę `tripsync-data.sqlite`, przywróć WSGI i Reload. Następnie sprawdź `/health` oraz logowanie i dane. Nie nadpisuj aktywnej bazy.

## Gdy będziesz mieć własny VPS

Alternatywna konfiguracja jest w `deploy/compose.yaml` i `deploy/Caddyfile`: trwała baza w wolumenie, API bez publicznego portu, Caddy z certyfikatem HTTPS. Potrzebne są Docker Compose, domena wskazująca serwer oraz dostępne porty 80/443. Skopiuj `deploy/.env.example` do `deploy/.env`, wpisz domenę i e-mail, potem `docker compose --project-directory deploy up -d --build`. Nie używaj `down -v`, bo usuwa wolumeny. Sieć `172.30.42.0/24` musi być wolna. Ten wariant nie był uruchamiany w Dockerze w środowisku projektu (Docker nie jest zainstalowany).

[Caddy — automatyczne HTTPS](https://caddyserver.com/docs/automatic-https).

## Dlaczego nie SQLite na darmowym Render

Darmowy Render usuwa zmiany lokalnego dysku po restarcie/uśpieniu/wdrożeniu; darmowy Postgres wygasa po 30 dniach. Dla tego backendu wariant PythonAnywhere jest prostszy i zachowuje plik bazy. [Dokumentacja Render](https://render.com/docs/free).

## Stan weryfikacji

Adapter WSGI, REST i ponowne otwarcie bazy są testowane lokalnie. Nie utworzono konta ani publicznej instancji PythonAnywhere; logowanie do panelu i wybór domeny wykonuje właściciel konta. Instrukcja nie oznacza, że serwer został już opublikowany.
