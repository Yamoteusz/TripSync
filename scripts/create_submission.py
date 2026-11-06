from pathlib import Path
import json
import re

from PIL import Image as PILImage
from reportlab.lib.pagesizes import A4
from reportlab.platypus import Image, PageBreak, Spacer, Table, TableStyle

from create_documentation import Documentation, Logo, SvgDiagram, WIDTH, frame, paragraph, table
from package_project import PDF, package_submission

ROOT = Path(__file__).resolve().parent.parent
RESULTS = json.loads((ROOT / 'docs/test-results-1.2.1.json').read_text())


def requirements(section):
    text = (ROOT / 'docs/wymagania.md').read_text().split('## ' + section + '\n', 1)[1].split('\n## ', 1)[0]
    return [[cell.strip() for cell in line.strip().strip('|').split('|')]
            for line in text.splitlines() if line.startswith('|') and not re.match(r'^\|[-:| ]+\|$', line)]


def heading(title):
    return [PageBreak(), paragraph(title, 'h1')]


def gallery(number, entries):
    items = heading(f'10. Najważniejsze widoki ({number}/2)')
    items.append(paragraph('TripSync 1.2.1, emulator Android 10, 06.11.2026. Dane demonstracyjne.', 'small'))
    cells = []
    for filename, caption in entries:
        path = ROOT / 'docs/screenshots-1.2.1' / filename
        with PILImage.open(path) as picture:
            width, height = picture.size
        cells.append([Image(str(path), width=width * 265 / height, height=265), paragraph(caption, 'caption')])
    grid = Table([[cells[0], cells[1]], [cells[2], cells[3]]], colWidths=[WIDTH / 2] * 2)
    grid.setStyle(TableStyle([
        ('VALIGN', (0, 0), (-1, -1), 'TOP'), ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
        ('TOPPADDING', (0, 0), (-1, -1), 8), ('BOTTOMPADDING', (0, 0), (-1, -1), 12),
        ('LEFTPADDING', (0, 0), (-1, -1), 14), ('RIGHTPADDING', (0, 0), (-1, -1), 14),
    ]))
    items.append(grid)
    return items


def main():
    if RESULTS['version'] != '1.2.1' or RESULTS['unique_tests_passed'] != 99:
        raise RuntimeError('Uaktualnij dokumentację do bieżących wyników testów.')
    story = [Logo(), Spacer(1, 20), paragraph('Dokumentacja projektu Android', 'h1')]
    story += [paragraph('**Wykonawcy:** Brajan Sieroń, Damian Panońko, Patryk Dawczak.'),
              paragraph('**Wersja aplikacji:** 1.2.1 (kod 4) | **Backend:** API 1.2.0 | **Data:** 06.11.2026.', 'small'),
              paragraph('1. Cel i zakres aplikacji', 'h2'),
              paragraph('TripSync wspiera organizację wyjazdów niewielkich grup znajomych. Łączy wspólny harmonogram, zadania i pakowanie, propozycje z głosowaniem oraz koszty i rozliczenia. Celem jest zebranie informacji o podróży w jednym miejscu i umożliwienie pracy także podczas utraty połączenia.'),
              paragraph('Zakres obejmuje konta i role, zaproszenia, CRUD wyjazdów i ich elementów, budżety, kategorie wydatków, zapis spłat, historię zmian, mapę miejsc, wyszukiwanie miejscowości i prognozę na 7 dni. Zmiany zapisują się lokalnie i synchronizują z serwerem; konflikty można rozstrzygnąć w aplikacji.'),
              paragraph('Gość korzysta z demo lub zakłada konto. Uczestnik współtworzy dane wyjazdu. Organizator zarządza dodatkowo wyjazdem, zaproszeniami, uczestnikami i budżetem. Każdy wyjazd ma jedną walutę (PLN, EUR lub USD). Spłaty są deklaracjami użytkowników. Kafelki mapy i nowa prognoza wymagają internetu.'),
              paragraph('Materiały i kod źródłowy', 'h2'),
              paragraph('**Repozytorium Git:** [github.com/Yamoteusz/TripSync](https://github.com/Yamoteusz/TripSync)'),
              paragraph('**Aplikacja w paczce:** `TripSync.apk`, Android 8.0 lub nowszy, podpis debug do prezentacji. Ten dokument zawiera wymagane opisy, diagramy, wyniki testów i zrzuty ekranu.'),
              paragraph('Spis treści', 'h2'),
              paragraph('1. Cel i zakres · 2. Wymagania funkcjonalne · 3. Wymagania niefunkcjonalne · 4. Architektura · 5. Przypadki użycia · 6. Bazy danych · 7. Technologie · 8. Instalacja i uruchomienie · 9. Testy · 10. Zrzuty ekranu.', 'small')]

    functional = requirements('Wymagania funkcjonalne')
    story += heading('2. Wymagania funkcjonalne')
    story += [paragraph('CRUD oznacza dodawanie, odczytywanie, edycję i usuwanie danych. Poniższe kryteria określają oczekiwane zachowanie aplikacji.', 'small'), table(functional, widths=[35, 137, 327])]
    story += heading('3. Wymagania niefunkcjonalne')
    story += [table(requirements('Wymagania niefunkcjonalne'), widths=[35, 137, 327]), Spacer(1, 14),
              paragraph('Trwałość i poprawność danych', 'h2'),
              paragraph('Room przechowuje dane po ponownym uruchomieniu. Zapis danych i kolejki synchronizacji jest transakcyjny. Walidacja obejmuje m.in. termin wyjazdu, daty punktów planu, godziny, współrzędne i udziały w wydatku. Kwoty są liczbami całkowitymi najmniejszych jednostek waluty; podział kosztów zachowuje sumę co do grosza.'),
              paragraph('Kontrola dostępu', 'h2'),
              paragraph('Backend weryfikuje sesję, członkostwo i rolę przy operacjach. Hasła są hashowane PBKDF2-SHA256 z indywidualną solą (260 000 iteracji); tokeny sesji są przechowywane na serwerze jako skróty. Lokalną sesję chroni AES-GCM i Android Keystore. Kopie zapasowe aplikacji są wyłączone. Lokalna baza Room nie jest szyfrowana; dostęp ogranicza piaskownica systemu Android.')]
    story += heading('4. Diagram architektury aplikacji')
    story += [SvgDiagram(ROOT / 'docs/diagrams/architektura.svg'), Spacer(1, 12),
              paragraph('Aplikacja korzysta z **MVVM**. Compose wyświetla stan z TripViewModel i przekazuje akcje użytkownika. ViewModel udostępnia StateFlow, a Repository koordynuje warstwę danych. Modele, walidacja i obliczenia rozliczeń znajdują się w domenie. Kod jest rozdzielony na foldery ui, domain i data oraz osobny backend.'),
              paragraph('**Room jest lokalnym źródłem danych.** Zmiana rekordu oraz wpis do operations powstają w jednej transakcji. Synchronizacja przez Retrofit wysyła operacje z identyfikatorem i wersją bazową. Serwer sprawdza uprawnienia, zapobiega podwójnemu wykonaniu i zwraca konflikty. WorkManager uruchamia synchronizację okresową przy dostępnej sieci; użytkownik może też wywołać ją ręcznie.'),
              paragraph('Przeniesienie propozycji do harmonogramu jest jedną operacją serwera: zmienia typ rekordu, usuwa głosy i zapisuje historię w transakcji. TravelRepository obsługuje Open-Meteo i cache. Mapę renderuje Leaflet w WebView, a ustawienia przechowuje DataStore.')]
    story += heading('5. Diagram przypadków użycia')
    story += [SvgDiagram(ROOT / 'docs/diagrams/przypadki-uzycia.svg'), Spacer(1, 8),
              paragraph('Organizator dziedziczy możliwości uczestnika. Gość uruchamia demo, rejestruje się lub loguje. Android realizuje pracę w tle i powiadomienia. Uprawnienia do danych grupy są sprawdzane na serwerze.', 'small'),
              paragraph('**Przykładowy przebieg:** organizator tworzy wyjazd i udostępnia kod. Uczestnik dołącza, proponuje aktywność i oddaje głos. Propozycja trafia do planu; grupa zapisuje wydatki, sprawdza salda i odnotowuje spłatę. Przy braku sieci zmiany czekają w kolejce.', 'small')]
    story += heading('6. Baza danych na urządzeniu')
    story += [SvgDiagram(ROOT / 'docs/diagrams/baza-lokalna.svg'), Spacer(1, 15),
              table([['Tabela', 'Rola w aplikacji'],
                     ['records', 'Lokalne rekordy wyjazdu i jego elementów. Pole kind określa typ, json zawiera dane modelu, version wersję serwera, pending i deleted stan synchronizacji.'],
                     ['operations', 'Trwała kolejka zmian: identyfikator operacji, rekord, typ działania, payload, baseVersion i czas utworzenia. Jeden oczekujący wpis na rekord.'],
                     ['conflicts', 'Wersja serwera i przyczyna konfliktu lub odrzucenia. Użytkownik wybiera zachowanie swojej wersji albo danych serwera.']]), Spacer(1, 12),
              paragraph('Relacje i formaty', 'h2'),
              paragraph('Diagram pokazuje fizyczne tabele Room/SQLite. Powiązania przerywane są logiczne; schemat nie deklaruje kluczy obcych. records ma indeksy tripId i kind, a operations unikalny recordId. Usunięcia rekordów synchronizowanych wykorzystują znaczniki deleted.'),
              paragraph('W payloadach znajdują się typowane modele: wyjazd, punkt planu, propozycja, głos, zadanie, wydatek, budżet, spłata, miejsce i wpis historii. Identyfikatory łączą element z wyjazdem oraz użytkownikiem. Daty przechowywane są jako ISO, godziny jako czas 24-godzinny, a kwoty jako liczby całkowite najmniejszych jednostek waluty. Wersja schematu Room: 1.')]
    story += heading('6. Baza danych backendu')
    story += [SvgDiagram(ROOT / 'docs/diagrams/baza-backendu.svg'), Spacer(1, 10),
              table([['Tabela', 'Przechowywane dane'],
                     ['users', 'Konta: identyfikator, nazwa, unikalny e-mail, sól i skrót hasła.'],
                     ['sessions', 'Skrót tokenu, identyfikator użytkownika i termin wygaśnięcia.'],
                     ['records', 'Współdzielone rekordy, wyjazd, rodzaj, JSON, wersja i usunięcie.'],
                     ['members', 'Uczestnictwo i rola; klucz złożony trip_id + user_id.'],
                     ['invites', 'Kod zaproszenia i unikalny identyfikator wyjazdu.'],
                     ['operations', 'Wynik operacji i skrót żądania; ponowienie zwraca ten sam wynik.']]), Spacer(1, 8),
              paragraph('Backend obsługuje SQLite lokalnie i MySQL na hostingu (InnoDB, utf8mb4). Linie ciągłe oznaczają klucze obce, przerywane powiązania logiczne. Członkostwo, poprawność payloadów i wersje rekordów kontroluje warstwa API.', 'small')]
    story += heading('7. Biblioteki i technologie')
    story += [paragraph('Wersje wynikają z deklaracji projektu. Zależności przechodnie rozwiązuje Gradle.', 'small'),
              table([['Technologia / wersja', 'Zastosowanie'],
                     ['Kotlin; plugin Compose 2.2.10', 'Logika i deklaratywne ekrany. Kotlin wbudowany w AGP.'],
                     ['AGP 9.1.1; Gradle 9.3.1', 'Gradle Wrapper; Java 17, sprawdzono JDK 21.'],
                     ['Android SDK 36.1', 'compile 36.1, target 36, minimum 26 (Android 8.0).'],
                     ['Jetpack Compose / Material 3', 'Motywy, formularze i pickery; BOM 2024.01.00.'],
                     ['Navigation 2.7.7; Lifecycle 2.9.0', 'Nawigacja, ViewModel i cykl życia.'],
                     ['Room 2.8.4; KSP 2.2.10-2.0.2', 'SQLite, DAO, transakcje, schemat i obserwacja danych.'],
                     ['Coroutines 1.10.2 / Flow', 'Operacje asynchroniczne i aktualizacje stanu.'],
                     ['Retrofit 2.9.0; OkHttp 4.12.0; Gson 2.11.0', 'REST, transport HTTP i serializacja JSON.'],
                     ['DataStore 1.1.7; WorkManager 2.9.0', 'Ustawienia, synchronizacja i przypomnienia.'],
                     ['Android Keystore / AES-GCM', 'Ochrona lokalnej sesji.'],
                     ['Python 3.10+; SQLite', 'Backend REST, lokalna baza i testy.'],
                     ['PyMySQL[rsa] 1.2.3', 'Połączenie backendu z MySQL na hostingu.'],
                     ['Leaflet 1.9.4 / OpenStreetMap', 'Mapa w WebView, kafelki HTTPS i atrybucja OSM.'],
                     ['Open-Meteo', 'Geokodowanie miejscowości i prognoza pogody.'],
                     ['JUnit 4.13.2; AndroidX Test', 'Testy Kotlin; runner 1.7.0, ext-junit 1.3.0.'],
                     ['Compose UI Test; Espresso 3.7.0', 'Testy interfejsu i systemowych kontrolek daty.']]), Spacer(1, 9),
              paragraph('Leaflet jest dołączony lokalnie z licencją BSD-2-Clause. Mapa pokazuje źródło OpenStreetMap. Aplikacja korzysta z publicznych usług geokodowania i prognozy Open-Meteo. Nie wymaga płatnej subskrypcji.', 'small')]
    story += heading('8. Instalacja i uruchomienie')
    story += [paragraph('Instalacja załączonego APK', 'h2'),
              paragraph('1. Rozpakuj ZIP i przenieś `TripSync.apk` na telefon z Androidem 8.0+ albo przeciągnij plik do uruchomionego emulatora. Na telefonie zezwól użytej aplikacji na instalowanie APK. Pakiet pl.tripsync ma wersję 1.2.1 i jest podpisany kluczem debug.'),
              paragraph('2. Otwórz TripSync i wybierz **Wypróbuj bez konta**. Przykładowy wyjazd trzech osób pozwala pokazać edycję planu, zadania, głosy, koszty i budżet bez uruchamiania backendu.'),
              paragraph('3. Do współpracy ustaw adres API ikoną ustawień przed logowaniem. Wszyscy używają tego samego serwera i własnych kont; dołączają kodem lub linkiem. Adres domyślny: `https://tripsync.wisp.uno/`. Wymagany backend API 1.2.0. Dostępność publicznego hostingu nie była częścią testów tej paczki.'),
              paragraph('Uruchomienie ze źródeł', 'h2'),
              paragraph('Pobierz [repozytorium Git](https://github.com/Yamoteusz/TripSync) i otwórz cały katalog w Android Studio. Wymagane: JDK 17 lub 21, SDK 36.1, Build Tools 36.0.0. Zsynchronizuj Gradle, wybierz telefon lub emulator API 26+ i kliknij Run. Budowanie z terminala w katalogu projektu:'),
              paragraph('./gradlew -PtripsyncBuildRoot=/tmp/tripsync-build :app:assembleDebug', 'code'),
              paragraph('Wynik: `/tmp/tripsync-build/app/outputs/apk/debug/app-debug.apk`. Na Windows użyj gradlew.bat i własnego katalogu wynikowego.', 'small'),
              paragraph('Własny backend do demonstracji współpracy', 'h2'),
              paragraph('Lokalny backend SQLite wymaga Pythona 3.10+; nie wymaga dodatkowych pakietów. W drugim terminalu, w katalogu repozytorium, uruchom:'),
              paragraph('python3 backend/server.py --db /tmp/tripsync-local.sqlite', 'code'),
              paragraph('W emulatorze ustaw `http://10.0.2.2:8080/`. Dla telefonu po USB uruchom `adb reverse tcp:8080 tcp:8080` i ustaw `http://127.0.0.1:8080/`. Lokalne HTTP jest dozwolone w wariancie debug; serwer zdalny wymaga HTTPS.'),
              paragraph('Jeśli używany jest obecny Wispbyte, przed nowym APK opublikuj kod API 1.2.0 przez GitHub Desktop (commit i Push origin), a następnie zrestartuj serwer pobierający zmiany. Dane MySQL przechowuj w Environment lub .env na serwerze. Szczegóły konfiguracji są w repozytorium: docs/wispbyte.md.', 'small')]
    story += heading('9. Przeprowadzone testy i wyniki')
    story += [paragraph('Weryfikacja z **06.11.2026**: Android 1.2.1, backend API 1.2.0. Poniżej końcowe wyniki wykonanych przebiegów, bez doliczania powtórzeń.'),
              table([['Grupa', 'Testy zaliczone', 'Niepowodzenia'],
                     ['Backend Python', str(RESULTS['backend']['tests']), '0'],
                     ['Jednostkowe Android / Kotlin', str(RESULTS['android_unit']['tests']), '0'],
                     ['Android: interfejs i integracja', str(RESULTS['android_device']['tests']), '0'],
                     ['Łącznie', str(RESULTS['unique_tests_passed']), '0']], widths=[285, 107, 107]), Spacer(1, 10),
              paragraph('**Budowanie APK:** zakończone powodzeniem. **Lint offline:** 0 błędów i 1 ostrzeżenie GradleDependency dotyczące aktualizacji Lifecycle.'),
              paragraph('Zakres testów', 'h2'),
              paragraph('**Kotlin:** kwoty, precyzja i przepełnienia, podział kosztów, salda, spłaty, budżety, walidacja współrzędnych oraz dat i godzin. Zerowy limit kategorii jest poprawny; wydatek pozostaje dodatni.'),
              paragraph('**Backend:** konta, sesje, role, zaproszenia, walidacja, synchronizacja, idempotencja, konflikty, historia, usunięcia i konwersja propozycji. Sprawdzono wyścig dwóch użytkowników, rollback po błędzie i ponowienie operacji bez duplikatu punktu planu.'),
              paragraph('**Urządzenie:** CRUD, nawigacja, formularze, budżet i spłaty, głosy, mapa miejsc, historia, kalendarz i zegar, odtworzenie aktywności, dwa konta z REST, konflikty, działanie offline i cache pogody. Sprawdzono ochronę niezapisanych formularzy, potwierdzenie usuwania budżetu i obsługę przypomnienia przy konwersji.'),
              paragraph('Środowisko i granice weryfikacji', 'h2'),
              paragraph('Android 10 / API 29, emulator ARM64, 1080 x 2400 px, 420 dpi. Dodatkowa próba daty i godziny: szerokość 320 dp i skala czcionki 1.3. SDK 36.1, JDK 21, Python 3.13.5; izolowane lokalne API i SQLite. Adapter MySQL testowano ze sterownikiem zastępczym. Nie badano rzeczywistego MySQL hostingu, nowszych Androidów, fizycznych urządzeń, TalkBack ani obciążenia. Przypomnienia podlegają ograniczeniom systemu.'),
              paragraph('Powtórzenie i dowody', 'h2'),
              paragraph('python3 -m unittest discover -s backend -v', 'code'),
              paragraph('./gradlew :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest', 'code'),
              paragraph('Testy urządzeniowe wymagają osobnego emulatora oraz lokalnego API pod http://10.0.2.2:8080/. Logi i raporty w repozytorium: [output/reports/1.2.1](https://github.com/Yamoteusz/TripSync/tree/main/output/reports/1.2.1); [zestawienie wyników](https://github.com/Yamoteusz/TripSync/blob/main/docs/test-results-1.2.1.json).', 'small')]
    story += gallery(1, [('02-podsumowanie.png', 'Podsumowanie wyjazdu'), ('03-harmonogram.png', 'Harmonogram'), ('04-zadania.png', 'Zadania i pakowanie'), ('05-glosowanie.png', 'Propozycje i głosowanie')])
    story += gallery(2, [('06-rozliczenia.png', 'Wydatki i rozliczenia'), ('08-budzet.png', 'Budżet i kategorie'), ('10-mapa.png', 'Mapa miejsc'), ('11-historia.png', 'Historia zmian')])
    PDF.parent.mkdir(parents=True, exist_ok=True)
    document = Documentation(str(PDF), pagesize=A4, rightMargin=48, leftMargin=48, topMargin=50, bottomMargin=57,
                             title='TripSync - dokumentacja projektu Android', author='Brajan Sieroń, Damian Panońko, Patryk Dawczak',
                             subject='Cel, wymagania, architektura, bazy danych, testy i uruchomienie wersji 1.2.1')
    document.build(story, onFirstPage=frame, onLaterPages=frame)
    print(PDF)
    print(package_submission())


if __name__ == '__main__':
    main()
