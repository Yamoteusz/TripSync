# Wispbyte + MySQL: aktualna konfiguracja

Backend TripSync jest skonfigurowany do pracy pod domeną `https://tripsync.wisp.uno/`, na porcie API 9853, z Pythonem 3.11 i bazą MySQL. Konfiguracja hostingu obejmuje 1 GiB RAM i 10 GiB dysku.

## Startup: zachowaj domyślną komendę

Nie trzeba zmieniać komendy startowej Wispbyte. Jej ostatni krok uruchamia `/usr/local/bin/python /home/container/main.py`. Root repozytorium zawiera `main.py`, `requirements.txt` i `backend/`. Katalog `/home/container` musi być checkoutem repozytorium, bez dodatkowego nadrzędnego folderu.

W ustawieniach startupu panelu:

| Pole / zmienna panelu | Wartość |
|---|---|
| Python | 3.11 lub zgodne 3.10+ |
| REQUIREMENTS_FILE | requirements.txt |
| PY_FILE, jeśli udostępnione | main.py |
| AUTO_UPDATE, jeśli udostępnione | 1 dla git pull przy starcie |
| Port aplikacji | Przydzielony przez panel, obecnie 9853 |

`requirements.txt` wskazuje zależności MySQL z `backend/requirements-mysql.txt`. Panel instaluje je przed uruchomieniem. Ustawienie REQUIREMENTS_FILE musi być dostępne dla komendy panelu; wpis tylko do pliku czytanego później przez aplikację nie gwarantuje instalacji pakietów przed startem.

## Environment albo plik .env

W **Configuration → Environment** można podać zmienne. W pokazanym panelu edytor wskazuje `.env`. Każdy wpis pliku ma format `NAZWA=wartość`; sama nazwa bez `=` jest błędem. Wzór znajduje się w `backend/wispbyte.env.example`, bez hasła.

| Zmienna | Znaczenie |
|---|---|
| TRIPSYNC_DATABASE_ENGINE | mysql |
| TRIPSYNC_MYSQL_HOST | Host bazy z panelu |
| TRIPSYNC_MYSQL_PORT | 3306 lub port bazy z panelu |
| TRIPSYNC_MYSQL_DATABASE | Pełna nazwa bazy z panelu |
| TRIPSYNC_MYSQL_USER | Pełna nazwa użytkownika z panelu, dokładnie z właściwym prefiksem |
| TRIPSYNC_MYSQL_PASSWORD | Hasło bazy, wyłącznie na serwerze |
| PORT | 9853 lub przydzielony port API |
| TRIPSYNC_MYSQL_SSL_CA | Opcjonalna ścieżka CA, gdy hosting zapewnia TLS MySQL |
| TRIPSYNC_TRUSTED_PROXIES | Opcjonalne, potwierdzone adresy IP proxy, rozdzielone przecinkami |

Plik `/home/container/.env` znajduje się obok `main.py`. Parser nie usuwa cudzysłowów, spacji ani znaków `$`, `#`, `=` z wartości: są dosłowne. Nie dopisuj komentarza po haśle. Każda wartość zajmuje jeden wiersz. Wpisy nieobsługiwane przez aplikację, np. poprawny `REQUIREMENTS_FILE=requirements.txt`, są pomijane przez parser aplikacji; nadal mogą służyć panelowi.

Rzeczywiste zmienne środowiskowe mają pierwszeństwo nad plikiem, także jeśli błędną wartość wpisano wcześniej. Pełna konfiguracja MySQL w środowisku pozwala pominąć plik. Hasła nie wpisuj do publicznego szablonu, README, komendy startupu ani GitHuba. `.env` jest ignorowany przez Git.

## Baza i sieć

API tworzy sześć tabel w już istniejącej bazie: users, sessions, records, members, invites, operations. Konto potrzebuje CREATE oraz SELECT/INSERT/UPDATE/DELETE. Port 3306 bazy i port 9853 API to dwa odrębne połączenia. HTTPS domeny nie zabezpiecza automatycznie MySQL; certyfikat CA bazy jest osobną konfiguracją.

Nie ma automatycznej migracji SQLite do MySQL. Nie uruchamiaj kilku procesów backendu na tej samej bazie w ramach tego projektu; kontrola współbieżności zakłada pojedynczy proces. Ogranicz zdalny dostęp do bazy według możliwości hostingu. `remote: %` dopuszcza wszystkie hosty przy poprawnym uwierzytelnieniu i nie zastępuje hasła ani kontroli sieci.

## Uruchomienie i sprawdzenie

1. W GitHub Desktop wyślij sprawdzone commity przez Push origin.
2. Sprawdź połączenie Wispbyte z repozytorium i gałęzią main. Dostęp do repozytorium prywatnego ustaw w panelu.
3. Zapisz konfigurację startupu i Environment; sprawdź pełną nazwę użytkownika bazy.
4. Uruchom serwer. Oczekiwany log: `TripSync API na porcie 9853`.
5. Sprawdź `https://tripsync.wisp.uno/health`; aktualne API raportuje `version: 1.2.0`, Android ma 1.2.1.
6. Utwórz konto prezentacyjne i wyjazd, odczytaj na drugim koncie po zaproszeniu, zrestartuj backend i sprawdź trwałość.

Health potwierdza działający proces. Zapis, odczyt i restart weryfikują dodatkowo pracę bazy. Zwracanie 404 dla `/` nie oznacza błędu: backend jest API, nie stroną internetową. Nie wyłączaj sprawdzania certyfikatów, aby korzystać z adresu IP zamiast domeny.

## Aktualizacje, kopie i błędy

Domyślny startup robi git pull, instaluje requirements i uruchamia main.py. Kroki w pokazanej komendzie są rozdzielone średnikami: błąd pull/pip nie musi zatrzymać dalszego startu, dlatego sprawdzaj cały log. Aktualizacja kodu nie powinna zastępować `.env` ani kasować bazy.

MySQL kopiuj przez eksport panelu lub odpowiednio skonfigurowany mysqldump; `backend/backup.py` jest wyłącznie dla SQLite. Kopie przechowuj poza serwerem. Przy błędzie MySQL sprawdź kolejno host, port, pełny login, nazwę bazy, hasło i dopuszczenie połączenia z serwera API. Nie publikuj zrzutu z widocznym hasłem.

## Aktualizacja do Android 1.2.1 i API 1.2.0

Najpierw wyślij zmiany przez GitHub Desktop i zrestartuj Wispbyte, aby pobrał backend API 1.2.0. Sprawdź `/health`, następnie zainstaluj nowy APK u uczestników. Atomowe przenoszenie pomysłów używa nowej operacji `convert` i wymaga aktualnego backendu. Domyślna komenda startupu, zmienne Environment i schemat MySQL pozostają bez zmian; aktualizacja nie wymaga czyszczenia bazy.
