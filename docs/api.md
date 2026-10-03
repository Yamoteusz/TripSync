# API REST

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

Rodzaje rekordów: trip, member, event, task, proposal, vote, expense. Wyjazd ma `id=tripId`. Uczestnik ma `id=tripId:userId`. Głos ma `id=proposalId:userId`. Wydatki używają całkowitych kwot w groszach/centach i mapy `shares` od identyfikatora użytkownika do jego udziału.

`member` powstaje wyłącznie po utworzeniu wyjazdu lub dołączeniu przez zaproszenie. API operacji dopuszcza tylko usunięcie uczestnika przez organizatora. Nie ma endpointu zmiany właściciela.
