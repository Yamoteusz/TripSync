# API REST - kontrakt backendu 1.1.0

Android 1.2.0 zachowuje kontrakt API 1.1.0. Endpoint `/health` raportuje wersję backendu, a nie interfejsu.

JSON UTF-8; wszystkie endpointy poza health i uwierzytelnianiem wymagają `Authorization: Bearer TOKEN`. Odpowiedź błędu HTTP zawiera `message`. Limit treści żądania: 256 KiB. Żądania nie mają interfejsu CORS dla przeglądarki; klientem jest aplikacja Android.

| Metoda | Ścieżka | Dane / wynik |
|---|---|---|
| GET | `/health` | `status`, `version` |
| POST | `/auth/register` | `name`, `email`, `password` → `token`, `user` |
| POST | `/auth/login` | `email`, `password` → `token`, `user` |
| POST | `/auth/logout` | Unieważnia bieżącą sesję |
| GET | `/sync` | `tripIds`, `records` dostępnych wyjazdów |
| POST | `/operations` | `operations` → `results`; od 1 do 100 operacji |
| GET | `/trips/{id}/invite` | Kod zaproszenia organizatora |
| POST | `/trips/{id}/invite` | Nowy kod; poprzedni traci ważność |
| POST | `/join` | `code` → `tripId` |

## Operacja

```json
{
  "id": "uuid-operacji",
  "recordId": "uuid-rekordu",
  "tripId": "uuid-wyjazdu",
  "kind": "event",
  "payload": {
    "title": "Spacer", "date": "2026-10-11", "time": "09:00",
    "place": "Zakopane", "description": "", "rank": 0,
    "done": false, "reminder": false
  },
  "baseVersion": 0,
  "deleted": false
}
```

Wyniki: `applied` z aktualnym rekordem, `conflict` z wersją serwera, `forbidden` lub `invalid` z komunikatem. Wynik operacji jest niezależny od pozostałych elementów batcha; nie jest to transakcja całej paczki. Rekord ma pola `id`, `tripId`, `kind`, `payload`, `version`, `deleted`.

Rodzaje rekordów: trip, member, event, task, proposal, vote, expense, budget, settlement, place oraz generowane przez serwer activity. Wyjazd ma `id=tripId`. Uczestnik ma `id=tripId:userId`. Głos ma `id=proposalId:userId`. Wydatki używają całkowitych kwot w groszach/centach i mapy `shares` od identyfikatora użytkownika do jego udziału.

`member` powstaje wyłącznie po utworzeniu wyjazdu lub dołączeniu przez zaproszenie. API operacji dopuszcza tylko usunięcie uczestnika przez organizatora. Nie ma endpointu zmiany właściciela.

## Payloady obsługiwane przez Android 1.2

- `expense`: dodatkowe opcjonalne `category`: transport, accommodation, food, activities, shopping, other. Starszy rekord bez pola traktowany jest jako other.
- `budget`: stały `recordId=budget:tripId`, `limit` i mapa `categoryLimits` (kwoty całkowite w groszach/centach). Jeden budżet wyjazdu; zapis/usunięcie/przywrócenie tylko przez organizatora. Suma limitów kategorii ≤ limit całkowity.
- `settlement`: `from`, `to`, `amount`, `date`, `note`. Nadawca musi być zalogowanym użytkownikiem i nie może zmienić właściciela istniejącej spłaty. Tylko nadawca edytuje i usuwa. Data nie może być z przyszłości. To deklaracja, bez potwierdzenia bankowego.
- `place`: `title`, `latitude`, `longitude`, `description`. Zakresy współrzędnych [-90,90] i [-180,180].
- `activity`: `actorId`, `actorName`, `action`, `recordKind`, `title`, `at` (czas serwera w ms). Powstaje wyłącznie na serwerze, w transakcji zapisu. Ponowienia i konflikty nie tworzą powtórnych wpisów. Nie przyjmuje operacji klienta i nie zawiera sekretów zaproszeń.

Waluta jest blokowana po dodaniu aktywnych wydatków, budżetu lub spłat. Usunięcie uczestnika z aktywnymi spłatami wymaga ich wcześniejszego usunięcia przez nadawcę.

## Zewnętrzne publiczne API

Android używa oddzielnych klientów Retrofit bez tokenu TripSync:

- `https://geocoding-api.open-meteo.com/v1/search?name=...&count=8&language=pl` — miejscowości.
- `https://api.open-meteo.com/v1/forecast?latitude=...&longitude=...&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto&forecast_days=7` — prognoza.
- `https://tile.openstreetmap.org/{z}/{x}/{y}.png` — kafelki, standardowy cache WebView, identyfikujący User-Agent TripSync/<wersja aplikacji>, widoczna atrybucja, brak masowego pobierania.

Źródła: [Open-Meteo](https://open-meteo.com/en/docs), [geokodowanie](https://open-meteo.com/en/docs/geocoding-api), [polityka kafelków](https://operations.osmfoundation.org/policies/tiles/). Darmowe Open-Meteo jest przeznaczone do niekomercyjnego użycia i ma limity oraz brak SLA. Współrzędne wybranego miejsca są przekazywane tym dostawcom po użyciu mapy/prognozy; lokalizacja telefonu nie jest pobierana.

## Odpowiedzi HTTP

| Status | Znaczenie |
|---|---|
| 200 | Sukces, także batch z wynikami poszczególnych operacji |
| 400 | Błędny JSON lub dane żądania |
| 401 | Brak tokenu, błędna lub wygasła sesja |
| 403 | Brak wymaganych uprawnień |
| 404 | Nieznany endpoint lub zasób |
| 429 | Limit prób rejestracji/logowania |
| 503 | Chwilowa niedostępność bazy lub I/O |
| 500 | Inny błąd serwera, bez ujawniania szczegółów |

Status wyniku operacji `conflict` nie jest osobnym HTTP 409: batch odpowiada 200, a klient odczytuje `results[].status`. Autoryzacja całego żądania jest sprawdzana przed obsługą paczki.

Przykład publicznego endpointu bez sekretów:

```sh
curl https://tripsync.wisp.uno/health
```

Dokładny model rekordu i schemat bazy opisuje `baza-danych.md`.
