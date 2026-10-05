# TripSync na płatnym Wispbyte

Konfiguracja dla udostępnionego planu: 1120 MB RAM, 10 GB dysku, środowisko Python i własna komenda startowa. Zużycie przy większym ruchu trzeba zmierzyć; parametry wystarczają do rozpoczęcia testów grupowych lekkiego backendu SQLite.

1. W panelu wybierz Python 3.10 lub nowszy.
2. Wgraj `backend/server.py` i `backend/start_hosted.py` do katalogu głównego serwera. Backend nie potrzebuje instalacji pakietów pip.
3. Ustaw komendę startową `python3 start_hosted.py`.
4. Ustaw `PORT` na przydzielony port aplikacji. Skrypt odczytuje najpierw `PORT`, następnie `SERVER_PORT`, a przy braku obu używa `9853`. Port należy potwierdzić w przydziałach panelu; adres panelu sam w sobie nie potwierdza portu aplikacji.
5. Opcjonalnie ustaw `TRIPSYNC_DB=data/tripsync.sqlite`. Przy aktualizacji wymieniaj pliki Python i zachowuj cały katalog `data`. Potwierdź z hostingiem, że dysk przetrwa restart; reinstalacja serwera może usunąć dane.
6. Jeśli panel udostępnia domenę, przypisz dostępną `tripsync.wisp.uno` do portu aplikacji i włącz HTTPS obsługiwane przez hosting. Sam wpis domeny nie potwierdza działania TLS ani routingu.
7. Sprawdź `https://tripsync.wisp.uno/health`. Oczekiwana odpowiedź: JSON ze statusem `ok`. Następnie wpisz `https://tripsync.wisp.uno/` jako adres API w ustawieniach aplikacji i utwórz prawdziwe konto.
8. Uruchom test: utwórz wyjazd, zrestartuj backend, zaloguj się ponownie i sprawdź zachowanie wyjazdu. Powtórz test po aktualizacji plików serwera.

Adres `https://78.154.103.2:9853` nie został zweryfikowany jako działający endpoint API. Certyfikat domeny może nie obejmować adresu IP. Nie wyłączaj walidacji certyfikatów w aplikacji.

Domyślnie backend nie ufa nagłówkom przekazanego adresu klienta. `TRIPSYNC_TRUSTED_PROXIES` ustawiaj tylko na potwierdzone adresy IP proxy hostingu, rozdzielone przecinkami. Bez tej konfiguracji wspólny adres proxy może powodować wspólny limit logowania dla użytkowników.

Do kopii bazy wgraj również `backend/backup.py` i korzystaj z instrukcji w `docs/hosting.md`. Przechowuj kopie poza tym samym serwerem. Bazy, kopii, tokenów i haseł nie dodawaj do Git.

Obecny etap obejmuje darmową wersję aplikacji. Premium organizatora dla całej grupy jest planem późniejszego etapu, bez płatności w obecnym wydaniu.


## Baza MySQL Wispbyte

Wgraj także `backend/mysql_database.py` i `backend/requirements-mysql.txt`. Przy uruchamianiu z repozytorium zachowaj strukturę `backend/` i ustaw komendę:

```sh
python3 -m pip install -r requirements.txt && python3 main.py
```

W panelu ustaw zmienne z `backend/wispbyte.env.example`. `TRIPSYNC_MYSQL_PASSWORD` uzupełnij wyłącznie w panelu, zapisując wcześniej hasło w menedżerze haseł. Plik `.env` w głównym katalogu repozytorium jest odczytywany przez `start_hosted.py` i `main.py`. Port API `PORT` jest niezależny od portu MySQL `3306`.

Backend tworzy sześć tabel w wybranej bazie. Konto potrzebuje uprawnień do tworzenia tabel i operacji SELECT/INSERT/UPDATE/DELETE. Domyślna lokalna wersja SQLite pozostaje dostępna. Istniejące dane SQLite nie są automatycznie przenoszone do MySQL; przed przełączeniem zachowaj kopię i wykonaj osobną migrację, jeśli masz już prawdziwe dane.

Jeżeli hosting udostępnia certyfikat CA bazy, ustaw `TRIPSYNC_MYSQL_SSL_CA` na jego ścieżkę. W tym trybie sprawdzany jest certyfikat i tożsamość serwera. HTTPS domeny aplikacji nie zapewnia szyfrowania osobnego połączenia z bazą; obsługę TLS MySQL należy potwierdzić u hostingu.

`remote: %` oznacza dopuszczenie połączeń z dowolnego hosta przy poprawnym uwierzytelnieniu. Jeżeli panel pozwala, ogranicz dostęp do adresu wyjściowego serwera aplikacji, potwierdzonego przez hosting. Nie jest to adres telefonu użytkownika.

Po konfiguracji wykonaj rejestrację i zapis wyjazdu, następnie restart i ponowny odczyt. `/health` potwierdza dostępność procesu API, a operacja zapisu i odczytu potwierdza działanie bazy. Nie uruchamiaj jednocześnie kilku procesów backendu: obecna kontrola synchronizacji opiera się na blokadzie jednego procesu.

Adapter zweryfikowano testami jednostkowymi; rzeczywiste połączenie z MySQL Wispbyte wymaga hasła ustawionego w panelu i nie zostało jeszcze wykonane.


## Domyślna komenda startowa panelu Wispbyte

Jeśli panel uruchamia `/usr/local/bin/python /home/container/main.py`, zachowaj tę komendę. Repozytorium zawiera teraz `main.py` i `requirements.txt` w katalogu głównym. Ustaw `REQUIREMENTS_FILE=requirements.txt`. W katalogu `/home/container` powinny znajdować się `main.py`, `requirements.txt` i katalog `backend`, bez dodatkowego nadrzędnego folderu `TripSync`.

Startup panelu pobiera aktualizacje przez `git pull`, instaluje zależności z `REQUIREMENTS_FILE`, a następnie uruchamia backend. Aby aktualizacje działały, katalog główny serwera musi być checkoutem repozytorium z `.git` i dostępem do prywatnego repozytorium. Token dostępu ustaw przez konfigurację hostingu, nie w kodzie lub komendzie publikowanej w Git.

Ustaw także `TRIPSYNC_DATABASE_ENGINE=mysql` i wszystkie zmienne MySQL ze wzoru `backend/wispbyte.env.example`; sam `REQUIREMENTS_FILE` nie wybiera bazy. Hasło wpisz w panelu jako `TRIPSYNC_MYSQL_PASSWORD`. Zachowaj port API przydzielony przez hosting. Po restarcie sprawdź log konsoli i endpoint `/health`, następnie zapis i odczyt wyjazdu.

W przedstawionej komendzie kroki są rozdzielone średnikami, więc nieudany `git pull` lub instalacja pakietów nie zatrzyma automatycznie kolejnego kroku. Po każdym wdrożeniu sprawdź log, czy pobranie kodu i instalacja zakończyły się powodzeniem.


## Gdzie wpisać hasło, gdy panel nie ma własnych zmiennych

Domyślna komenda startowa nadal obowiązuje. W panelu plików serwera utwórz `/home/container/.env`, obok `main.py`. Skopiuj zawartość `backend/wispbyte.env.example` i wpisz hasło po `TRIPSYNC_MYSQL_PASSWORD=`. Wartości są dosłowne: nie dodawaj cudzysłowów, spacji po znaku `=` ani komentarza za hasłem. Symbole `#`, `$`, `=` i cudzysłowy będą częścią hasła. Hasło musi zajmować jeden wiersz. Ustaw zmienną panelu `REQUIREMENTS_FILE` na `requirements.txt`, żeby domyślna komenda zainstalowała zależności.

Plik `.env` nie jest publikowany w Git. Utwórz go bezpośrednio na serwerze, nie edytuj publicznego wzoru hasłem. Jeśli plik powstanie na Macu, `.gitignore` również go wykluczy. Zmienne ustawione w panelu mają pierwszeństwo nad plikiem; usuń błędne istniejące wartości w panelu przed użyciem tego wariantu. Jeśli menedżer plików ukrywa pliki z kropką, włącz ich widoczność. Jeżeli panel udostępnia uprawnienia plików, ogranicz `.env` do odczytu i zapisu właściciela (`600`).

Po zapisaniu pliku i pobraniu aktualizacji z GitHuba uruchom ponownie serwer. Skrypt czyta `.env` niezależnie od bieżącego katalogu roboczego. Nie wypisuje konfiguracji ani hasła do logów.
