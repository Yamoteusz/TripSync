# Architektura TripSync

## MVVM i przepływ danych

```mermaid
flowchart TD
    UI[Jetpack Compose / Navigation] --> VM[TripViewModel / StateFlow]
    VM --> DOMAIN[Walidacja / Money / modele domenowe]
    VM --> REPO[Repository / Mutex]
    REPO --> ROOM[Room: records, operations, conflicts]
    ROOM --> FLOW[Flow lokalnych danych]
    FLOW --> VM
    REPO --> API[Retrofit / OkHttp]
    API --> SERVER[Python REST / kontrola dostępu]
    SERVER --> SQLITE[SQLite backendu]
    REPO --> VAULT[SessionVault / Android Keystore]
    REPO --> PREFS[DataStore: API i motyw]
    WORK[WorkManager / SyncWorker] --> REPO
    REMIND[ReminderWorker] --> ROOM
    REMIND --> NOTIF[Powiadomienia Android]
```

Źródłem danych dla ekranów jest Room. Każdy zapis danych i powiązanej operacji odbywa się w jednej transakcji. ViewModel zbiera reaktywne strumienie i publikuje stan UI. Mutex serializuje zapisy, uwierzytelnianie, wylogowanie i synchronizację. CoroutineWorker uruchamia synchronizację w tle po spełnieniu warunku dostępności sieci.

Repozytorium używa DTO na granicy sieci; rekordy przechowują typowany logicznie payload JSON. Jedna tabela rekordów pozwala zastosować jednolity protokół wersjonowania dla wszystkich modułów. Walidacja spójności relacji odbywa się w repozytorium/UI i na backendzie; indeksy i klucze główne są fizyczne, relacje do pól JSON są logiczne.

## Synchronizacja

1. Zapis lokalny aktualizuje rekord i operację w transakcji. Kolejna edycja tego samego rekordu zastępuje oczekującą operację nowym identyfikatorem, zachowując bazową wersję.
2. Klient wysyła operacje w kolejności zależności: wyjazdy, propozycje, pozostałe dane, głosy. Maksymalnie 100 operacji na żądanie.
3. Serwer sprawdza sesję, dostęp do wyjazdu, rolę, dane i bazową wersję rekordu.
4. Ponowienie już przyjętej operacji z tym samym identyfikatorem i treścią zwraca poprzedni wynik bez kolejnego zapisu.
5. Różnica wersji tworzy konflikt, zamiast nadpisywać dane. Operacje konfliktowe nie są wysyłane automatycznie do czasu decyzji użytkownika.
6. Klient pobiera pełny snapshot dostępnych wyjazdów. Oczekujące lokalne edycje nie są nadpisywane snapshotem.
7. Utrata dostępu powoduje usunięcie wyjazdu, jego kolejki, konfliktów i przypomnień z urządzenia po udanej synchronizacji.
8. Usunięcia są reprezentowane tombstone. Wyjazd usunięty przez organizatora przestaje być dostępny wszystkim uczestnikom. Głosy mogą być ponownie dodawane po cofnięciu.

Przy utracie odpowiedzi serwera i kolejnej edycji klient może otrzymać konflikt również dla własnej wcześniejszej zmiany. Mechanizm zachowuje dane i wymaga świadomego rozstrzygnięcia.

## Bezpieczeństwo

Hasła backendu: PBKDF2-HMAC-SHA256, losowa sól i 260 000 iteracji. Tokeny sesji: 48 losowych bajtów przed kodowaniem URL-safe, w bazie wyłącznie SHA-256 tokenu, ważność 7 dni. Porównanie hashy hasła jest stałoczasowe. Limiter logowania/rejestracji: 30 prób na adres IP w 5 minut, w pamięci procesu.

Autoryzacja jest egzekwowana na serwerze. Tylko organizator edytuje i usuwa wyjazd, generuje zaproszenia i usuwa uczestników. Każdy uczestnik zarządza planem, zadaniami, propozycjami i kosztami; głosować może wyłącznie za siebie. Usunięcie uczestnika mającego aktywne wydatki wymaga wcześniejszej zmiany/usunięcia tych wydatków. Przypisania jego zadań i głosy są czyszczone przez serwer.

Aplikacja nie przekazuje tokenów po zmianie adresu API: adres można zmienić tylko bez zalogowanej sesji. Cleartext HTTP jest ograniczone do lokalnych hostów debug. Wydanie release wymaga HTTPS.

## Przypadki użycia

```mermaid
flowchart LR
    G[Gość] --> LOGIN((Rejestracja / logowanie))
    G --> DEMO((Demonstracja offline))
    U[Uczestnik] --> JOIN((Dołączenie kodem / linkiem))
    U --> PLAN((CRUD planu))
    U --> TASK((Zadania i pakowanie))
    U --> IDEA((Propozycje / własny głos))
    U --> COST((Koszty i rozliczenia))
    U --> SYNC((Synchronizacja / konflikty))
    U --> SHARE((Udostępnienie planu))
    U --> SETTINGS((Motyw / wylogowanie))
    O[Organizator] --> CREATE((Tworzenie / edycja / usunięcie wyjazdu))
    O --> INVITE((Zaproszenia / usunięcie uczestnika))
    O --> PLAN
    O --> TASK
    O --> IDEA
    O --> COST
    O --> SYNC
    OS[System Android] --> REMIND((Przypomnienia lokalne))
    OS --> SYNC
```

## Fizyczna baza backendu

```mermaid
erDiagram
    USERS ||--o{ SESSIONS : ma
    USERS ||--o{ MEMBERS : uczestniczy
    USERS ||--o{ OPERATIONS : wysyla_logicznie
    RECORDS ||--o{ MEMBERS : wyjazd_logicznie
    RECORDS ||--o| INVITES : zaproszenie_logicznie
    USERS {
        text id PK
        text name
        text email UK
        text salt
        text password
    }
    SESSIONS {
        text token_hash PK
        text user_id FK
        integer expires
    }
    MEMBERS {
        text trip_id PK
        text user_id PK,FK
        text role
    }
    RECORDS {
        text id PK
        text trip_id
        text kind
        text payload
        integer version
        integer deleted
    }
    INVITES {
        text code PK
        text trip_id UK
    }
    OPERATIONS {
        text user_id PK
        text id PK
        text digest
        text result
    }
```

Fizyczne FK dotyczą sessions.user_id i members.user_id. Powiązania z records i operations.user_id są logiczne, kontrolowane przez kod API. Tabela records przechowuje wyjazdy oraz rekordy podrzędne, wskazywane przez trip_id. Payload zawiera właściwe dane danego typu.

## Lokalna baza Room

```mermaid
erDiagram
    RECORDS ||--o| OPERATIONS : kolejka_logiczna
    RECORDS ||--o| CONFLICTS : konflikt_logiczny
    RECORDS {
        string id PK
        string tripId
        string kind
        string json
        int version
        boolean deleted
        boolean pending
    }
    OPERATIONS {
        string id PK
        string recordId UK
        string tripId
        string kind
        string json
        int baseVersion
        boolean deleted
        long createdAt
    }
    CONFLICTS {
        string recordId PK
        string serverJson
        string message
        boolean invalid
    }
```

Room ma indeksy records.tripId, records.kind i unikalny indeks operations.recordId. Nie deklaruje fizycznych FK pomiędzy tymi trzema tabelami; spójność jest utrzymywana przez transakcje repozytorium. Schemat wyeksportowany przez Room znajduje się w app/schemas.
