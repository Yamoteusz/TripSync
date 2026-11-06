# Prezentacja i obrona projektu

## Scenariusz pokazu, około 10-12 minut

| Etap | Co pokazać | Co wyjaśnić |
|---|---|---|
| 1. Cel, 1 minuta | Lista wyjazdów i podsumowanie | Problem wspólnego planu i kosztów; zakres dla trzech osób |
| 2. Plan i formularze, 2 minuty | Nowy punkt, kalendarz, zegar, zadanie i propozycja | CRUD, walidacja oraz zachowanie niezapisanego formularza |
| 3. Finanse, 2 minuty | Wydatek, własne udziały, budżet, saldo i spłata | Kwoty całkowite w groszach, reszta przy podziale, spłata jako deklaracja |
| 4. Offline i współpraca, 3 minuty | Dwa konta, zapis offline, kolejka, synchronizacja i konflikt | Room jako źródło danych, wersjonowanie oraz idempotencja |
| 5. API i urządzenie, 1 minuta | Mapa, pogoda, cache lub lokalne przypomnienie | Retrofit, zewnętrzne API, obsługa błędów i ograniczenia Androida |
| 6. Dowody, 1-3 minuty | Diagramy, testy, Git i APK | Architektura, rzeczywisty wkład i granice weryfikacji |

Demo wystarcza do pokazania lokalnego CRUD. Współdzielenie, autorzy historii i konflikty wymagają dwóch rzeczywistych kont na tym samym API. Używaj kont prezentacyjnych, nie danych prywatnych. Sprawdź internet i dostępność backendu przed zajęciami; miej APK oraz lokalny wariant serwera przygotowane wcześniej.

## Przykład do rozliczeń

Anna płaci 720 PLN za nocleg, Bartek 180 PLN za bilety; oba koszty dotyczą Anny, Bartka i Celiny w równych udziałach. Każda osoba ma koszt 300 PLN. Salda: Anna +420 PLN, Bartek -120 PLN, Celina -300 PLN. Propozycje przelewów: Bartek → Anna 120 PLN i Celina → Anna 300 PLN. Po zapisaniu spłaty Bartka pozostaje przelew Celiny. Pokaż, że suma sald nadal wynosi zero.

## Pytania, na które każdy powinien umieć odpowiedzieć

- Gdzie znajduje się stan aplikacji i czym różni się ViewModel od repozytorium?
- Dlaczego ekran czyta Room zamiast bezpośrednio wynik Retrofit?
- Co dzieje się z zapisem bez internetu i co oznacza `baseVersion`?
- Jak wykrywa się konflikt i dlaczego ponowienie operacji nie tworzy duplikatu?
- Dlaczego pieniądze są `Long` w groszach, a nie `Double`?
- Które relacje bazy są FK, a które są zapisane w JSON i kontrolowane przez kod?
- Dlaczego sprawdzenie roli wyłącznie w UI nie byłoby wystarczające?
- Jak jest przechowywany token i jaka jest różnica między HTTPS API a TLS MySQL?
- Które testy są jednostkowe, a które wymagają emulatora oraz backendu?
- Co zostało zweryfikowane, a czego nie obejmuje obecny raport?

## Wykonawcy

Brajan Sieroń, Damian Panońko, Patryk Dawczak. Materiały prezentacyjne obejmują dokumentację PDF, APK i repozytorium [Yamoteusz/TripSync](https://github.com/Yamoteusz/TripSync).
