# Ograniczenia aplikacji

Wersja Android 1.2.1, backend API 1.2.0.

## Granice bieżącej aplikacji

- Jedna waluta na wyjazd: PLN, EUR albo USD. Nie ma przeliczania kursów.
- Maksymalnie 100 uczestników. Rozliczenia zachowują dokładność grosza/centa. Algorytm tworzy co najwyżej n-1 przelewów dla n osób z niezerowym saldem, ale nie gwarantuje globalnie najmniejszej liczby przelewów.
- Spłata jest deklaracją nadawcy; aplikacja nie potwierdza jej w banku i nie wykonuje płatności.
- Synchronizacja pobiera pełny snapshot, jest ręczna lub okresowa przez WorkManager, bez aktualizacji w czasie rzeczywistym. Usunięte uprawnienia są egzekwowane na serwerze natychmiast; urządzenie offline zachowuje wcześniej pobraną kopię do udanej synchronizacji.
- Okresowa synchronizacja i lokalne przypomnienia podlegają ograniczeniom Androida. Nie są alarmami dokładnymi.
- Wylogowanie czyści lokalną bazę i niewysłane zmiany. Demo nie jest eksportowane do prawdziwego konta.
- Mapa potrzebuje internetu do kafelków. Brak map offline i GPS telefonu. Wyszukiwanie obejmuje miejscowości; inne punkty można podać współrzędnymi.
- Prognoza obejmuje najbliższe 7 dni, nie dowolny termin przyszłego wyjazdu. Cache jest oznaczany datą.
- Historia pokazuje 200 najnowszych wpisów; brak paginacji i cofania zmian.
- Backend działa jako jeden proces z serializowanymi zapisami; nie zweryfikowano wieloprocesowego wdrożenia ani obciążenia.
- Brak odzyskiwania hasła, weryfikacji e-mail i samodzielnego usuwania konta. Brak czatu, załączników, skanowania paragonów, QR, szablonów oraz eksportu PDF/ICS z poziomu aplikacji. Udostępnianie tekstowego planu już działa.
- Nie ma płatności, premium, donate ani reklam. Zakres pozostaje projektem na zajęcia Android.
