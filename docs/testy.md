# Raport testów

Wykonano 04.10.2026. Wszystkie opisane poniżej wyniki pochodzą z uruchomionych testów, a nie z planu testowania.

| Grupa | Liczba | Niepowodzenia |
|---|---:|---:|
| Backend Python | 14 | 0 |
| Domena Kotlin / JUnit | 11 | 0 |
| Android: UI i integracja REST | 11 | 0 |
| Razem | 36 | 0 |

`assembleDebug` zakończone powodzeniem. Końcowy Android Lint: 0 błędów, 1 ostrzeżenie informujące o nowszej wersji Lifecycle. Końcowe budowanie i Lint wykonano z `--offline`; zdalne sprawdzanie najnowszych wersji bibliotek nie jest częścią tego przebiegu. Nie użyto baseline ani wyłączenia błędów Lint.

## Środowisko

Android Emulator Medium_Phone, Android 10 / API 29, ARM64, 1080 × 2400, gęstość 420 dpi. SDK kompilacji 36.1, AGP 9.1.1, Gradle 9.3.1, JDK 21. Python 3.13. Backend testowany na localhost:8080; klient emulatora używa 10.0.2.2:8080.

Pliki budowania umieszczono poza katalogiem synchronizowanym przez usługę chmurową, przez opcję `-PtripsyncBuildRoot=/private/tmp/tripsync-build`. Jest to lokalna opcja uruchomienia, nie zależność projektu od tego katalogu.

## Domena: 11 testów

- Dokładne odczytanie kwot z kropką i przecinkiem; graniczna dopuszczalna kwota.
- Odrzucenie zera, liczb ujemnych, nadmiernej precyzji, NaN, notacji naukowej i przepełnienia.
- Podział 101 groszy na trzy osoby bez utraty reszty.
- Salda i przelewy dla kosztów 720 PLN i 180 PLN.
- 200 deterministycznych losowych zestawów wydatków: suma sald zero, wszystkie salda wyzerowane po przelewach, brak ujemnych przelewów i limit n−1.
- Odrzucenie niespójnych udziałów, pustych oraz powtórzonych uczestników.
- Poprawne terminy i punkty planu; odrzucenie odwróconych dat, punktu poza wyjazdem, niepoprawnych dat/godzin i nazw.

## Backend: 14 testów

Hasła przechowywane jako hash z solą; sesja może zostać unieważniona. Błędne logowanie i duplikat e-mail są odrzucane. Zaproszenie dodaje uczestnika, rotacja unieważnia poprzedni kod. Obca osoba nie widzi wyjazdu, a uczestnik nie może zmienić danych wyjazdu wymagających organizatora.

Ponowienie operacji nie tworzy duplikatu ani kolejnej wersji. Równoczesne edycje tworzą konflikt, możliwe jest ponowienie na aktualnej wersji. Usunięty punkt pozostaje tombstone. Głos można dodać, cofnąć i dodać ponownie. Nieprawidłowe wydatki, zmiana waluty po kosztach i usunięcie uczestnika z aktywnymi udziałami są odrzucane.

Usunięcie uczestnika cofa dostęp i czyści jego przypisanie do zadań. Usunięcie wyjazdu odbiera widoczność i unieważnia zaproszenie. Walidowane są daty punktów i zawężenie terminu wyjazdu. Dane i sesje zachowują się po ponownym otwarciu SQLite. Test HTTP sprawdza health, uwierzytelnione pobieranie oraz odpowiedzi 400 i 401.

## Android: 11 testów

Siedem testów UI: najważniejsze widoki, CRUD wydatku i aktualizacja salda, zachowanie nierównego podziału przy edycji, formularz po odtworzeniu aktywności, walidacja pustego formularza, trzykrotna zmiana głosu i odtworzenie danych po restarcie aktywności.

Cztery testy rzeczywistego REST używają dwóch oddzielnych repozytoriów Android i dwóch kont: kolejka lokalna, konflikt dwóch edycji i wybór lokalnej wersji, przyjęcie wersji serwera, przesłanie usunięcia, cofnięcie uprawnień z wyczyszczeniem lokalnego wyjazdu i ponowne oddanie cofniętego głosu.

Dodatkowo powtórzono sam scenariusz screenów, aby pobrać obrazy przed odinstalowaniem aplikacji przez narzędzie Gradle. Nie jest on liczony drugi raz do sumy 36.

## Dowody i ograniczenia

Raporty JUnit XML, Lint HTML/XML/TXT oraz logi budowania i backendu są dołączone w `output/reports/`. W `docs/screenshots/` znajdują się rzeczywiste zrzuty z aplikacji. Nie wykonano testów na fizycznym urządzeniu, audytu bezpieczeństwa ani testów obciążeniowych. Test odtworzenia aktywności nie jest deklarowany jako test zabicia procesu przez system. Lokalne przypomnienia nie mają gwarancji dokładnej godziny dostarczenia.

Dodatkowa weryfikacja ręczna przez ADB: `am force-stop` i ponowne uruchomienie aplikacji zachowały sesję demonstracyjną oraz wyjazd w lokalnej bazie. Podpis końcowego APK zweryfikowano narzędziem apksigner (certyfikat Android Debug).
