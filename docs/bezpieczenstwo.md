# Bezpieczeństwo i obsługa błędów

## Android

Sesja jest szyfrowana AES-GCM. Klucz powstaje w Android Keystore; zaszyfrowane dane i IV znajdują się w prywatnych SharedPreferences. Hasło logowania nie jest zapisywane przez rememberSaveable. Baza Room znajduje się w prywatnym katalogu aplikacji, bez dodatkowego szyfrowania SQLCipher. Backup aplikacji i transfer danych są wyłączone w konfiguracji.

W release obowiązuje HTTPS. Debug dopuszcza HTTP tylko dla lokalnych adresów testowych. Zmiana serwera jest możliwa bez aktywnej sesji, aby nie wysłać tokenu do innego API. Wylogowanie czyści bazę, kolejkę, konflikty i sesję lokalną; niewysłane zmiany zostaną utracone.

## Backend

Hasła: PBKDF2-HMAC-SHA256, losowa sól, 260 000 iteracji i porównanie stałoczasowe. Token sesji zawiera 48 losowych bajtów przed kodowaniem; backend przechowuje jego hash SHA-256. Sesja wygasa po 7 dniach. Limiter rejestracji/logowania: 30 prób na adres IP w 5 minut, w pamięci pojedynczego procesu.

Każda operacja sprawdza sesję, dostęp do wyjazdu, rolę, payload i wersję. Organizator zarządza wyjazdem, budżetem, zaproszeniami i uczestnikami. Użytkownik głosuje za siebie i edytuje wyłącznie własne spłaty. Parametry SQL są przekazywane jako parametry, nie przez sklejanie danych użytkownika z zapytaniem.

## Hosting i dane

Hasło MySQL znajduje się wyłącznie w Environment albo `.env` serwera. `.gitignore` wyklucza sekrety, bazy, kopie i klucze podpisu. HTTPS aplikacji zabezpiecza kanał Android → API; nie zapewnia automatycznie TLS kanału API → MySQL. Ustawienie `TRIPSYNC_MYSQL_SSL_CA` włącza weryfikację TLS bazy, jeśli hosting udostępnia odpowiedni certyfikat.

Nagłówki adresu klienta są akceptowane tylko od jawnie zaufanych proxy. Kopie bazy muszą być przechowywane poza tym samym serwerem; `backend/backup.py` obsługuje SQLite, a MySQL wymaga eksportu narzędziem hostingu lub `mysqldump`.

## Mapa i zewnętrzne API

Leaflet jest lokalnym zasobem. WebView nie ma mostka JavaScript do Androida, blokuje dostęp do plików/treści, mieszany HTTP i niepożądaną nawigację. Nazwy markerów są kodowane przez Gson oraz wstawiane jako textContent. CSP ogranicza źródła. Android nie pobiera GPS telefonu; współrzędne miejsca są przekazywane dostawcy mapy i prognozy. Klient Open-Meteo nie otrzymuje tokenu TripSync.

## Obsługa błędów

Walidacja daje opis pola lub reguły przed zapisem. Błędy HTTP są mapowane z komunikatu serwera, a brak sieci pozostawia lokalne dane i kolejkę. Konflikt nie jest nadpisywany automatycznie. Mapa pokazuje błąd i ponowienie; pogoda zachowuje cache z datą. Backend zwraca ujednolicone JSON `message`, bez ujawniania sekretów konfiguracji.

## Ograniczenia

To podstawowe zabezpieczenia projektu dydaktycznego, bez przeprowadzonego audytu bezpieczeństwa. Nie ma odzyskiwania hasła, weryfikacji e-mail, samodzielnego usuwania konta ani monitoringu produkcyjnego. Urządzenie offline może nadal wyświetlać kopię danych po odebraniu dostępu na serwerze, aż do udanej synchronizacji. Pełna lista: `ograniczenia.md`.
