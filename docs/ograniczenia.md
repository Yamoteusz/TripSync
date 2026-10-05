# Ograniczenia i dalsze prace

Stan: Android 1.2.0, backend API 1.1.0. Poniższe elementy nie są przedstawiane jako już ukończone funkcje.

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

## Zadania przed oddaniem

1. Uzupełnić nazwiska, numery albumów i rzeczywisty wkład w `AUTHORS.md` oraz ponownie wygenerować PDF.
2. Zaktualizować SDK 36.1 do 37, jeśli prowadzący wymaga najnowszego stabilnego wydania; przetestować zachowanie po aktualizacji. Obecna dokumentacja celowo podaje używane SDK.
3. Sprawdzić aplikację na nowszym Androidzie, fizycznym telefonie, tablecie, przy obrocie i z TalkBack. Test odtworzenia aktywności nie jest dowodem dla każdego scenariusza zmiany konfiguracji.
4. Zweryfikować dostęp prowadzącego do repozytorium i aktualny APK, a także powtórzyć scenariusz dwóch kont na wybranym serwerze.
5. Przejrzeć jakość kodu: granice odpowiedzialności ViewModel/repozytorium, powtarzalne formularze, formatowanie i logikę domenową. Podział na foldery nie zastępuje przeglądu architektury.

## Kolejne funkcje bez płatności

Szablony i kopiowanie wyjazdów, zaproszenia QR, eksport kalendarza, komentarze przy punktach planu i powiadomienia grupowe. To propozycje następnych wersji, bez obietnicy wykonania w 1.2.0.
