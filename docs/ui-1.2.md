# TripSync 1.2 — interfejs i formularze

Wydanie rozwija darmową wersję aplikacji. Motyw wykorzystuje zieleń, jasne tło, wyraźną typografię, jednolite karty, ilustrację podróży oraz odświeżony znak i ikonę adaptacyjną z warstwą monochromatyczną. Dostępny jest również motyw ciemny; logo oraz ikony pasków systemowych zmieniają kontrast zgodnie z wybranym motywem.

## Wprowadzone zmiany

- Kalendarze Material 3 dla początku i końca wyjazdu, punktów planu, wydatków, przypomnień zadań i spłat. Data końca nie poprzedza początku; wybór daty punktu ogranicza się do terminu wyjazdu, a spłaty do dzisiaj lub wcześniej.
- Zegar 24-godzinny z opcją wprowadzania cyfr. Na niskim ekranie automatycznie pojawia się specjalizowany formularz godziny. Kalendarz można przewijać w ograniczonej wysokości okna. Na ekranie poniżej 360 dp, niskim ekranie albo przy większej czcionce używany jest systemowy kalendarz Androida, aby wszystkie dni pozostały dostępne.
- Daty na ekranach, w historii kosztów i w udostępnianym planie mają polskie nazwy miesięcy. W bazie i API pozostaje format ISO. Wybrany dzień jest przeliczany przez UTC, więc zmiana strefy urządzenia nie przesuwa daty kalendarzowej.
- Wyjazdy można wyszukiwać po nazwie i miejscu oraz filtrować na aktywne i zakończone. Plan jest pogrupowany według dni.
- Koszty można wyszukiwać po tytule, kategorii i płatniku, filtrować według kategorii oraz sortować po dacie, kwocie i nazwie. Filtry i pozycja przewinięcia wracają przy przełączaniu zakładek w bieżącej sesji aplikacji.
- Podsumowanie wskazuje najbliższy przyszły, niewykonany punkt według daty i godziny. Wskaźniki kategorii kosztów poprawiają czytelność budżetu.
- Zmieniony formularz edytora wymaga potwierdzenia przed wyjściem przyciskiem Wstecz. Formularz pokazuje komunikat walidacji przy przycisku zapisu, a klawiatura nie zasłania końca przewijanej strony.
- Mapa pokazuje ładowanie, komunikat błędu kafelków i przycisk ponowienia. Pozostają dostępne zapisane miejsca. Wyszukiwanie miejscowości nie uruchamia kolejnego zapytania podczas trwającej operacji.
- Domyślny serwer nowych instalacji to `https://tripsync.wisp.uno/`. Wcześniej zapisany adres zostaje zachowany i można go zmienić po wylogowaniu. Testy sieciowe jawnie używają lokalnego serwera testowego.

## Poprawione błędy

Nowy wydatek otrzymuje dzisiejszą datę zamiast przyszłej daty początku wyjazdu. Nierówne udziały kosztów są przechowywane w formularzu pod identyfikatorami użytkowników: dołączenie nowej osoby nie przesuwa kwot między istniejącymi uczestnikami. Wyszukiwanie wydatków nie znika po przełączeniu zakładki. Kontrolka edycji kosztu ma obszar dotyku 48 dp. Hasło logowania nie jest zapisywane w stanie formularza odtwarzanym po zniszczeniu aktywności. Wersja w ustawieniach pochodzi z konfiguracji buildu.

## Zgodność danych

Pakiet Android pozostaje `pl.tripsync`, versionCode to 3, a versionName to 1.2.0. Schemat Room i format API nie wymagają migracji dla tego wydania. Instaluj aktualizację podpisaną tym samym kluczem; nie odinstalowuj aplikacji ani nie kasuj danych, jeśli chcesz zachować lokalne wyjazdy. Backend nie wymaga zmiany komendy startowej.

## Weryfikacja

Dokładne wyniki i zakres testów znajdują się w `test-results-1.2.json`; raporty w `output/reports/ui-1.2/`. Zrzuty przedstawiają działający emulator API 29 oraz dane demonstracyjne i izolowane środowisko testowe, w `screenshots-1.2/`. Dokumentacja zbiorcza PDF została uaktualniona do Android 1.2.0. Starsze screeny i raporty są archiwalne; ten plik opisuje zakres zmian UI.
