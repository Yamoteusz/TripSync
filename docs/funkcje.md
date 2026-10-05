# Funkcje i korzystanie z aplikacji

## Początek i konta

Gość wybiera rejestrację, logowanie albo demo. Demo zawiera wyjazd trzech osób; można go edytować offline. Rejestracja na publicznym lub własnym API tworzy odrębne konto. E-mail jest identyfikatorem, bez wysyłania potwierdzenia. Adres API zmienia się przed logowaniem. Wylogowanie czyści lokalną bazę i niewysłane zmiany; kolejne demo odtwarza przykład.

## Wyjazdy i plan

Lista umożliwia wyszukiwanie po nazwie i miejscu oraz filtrowanie aktywnych i zakończonych podróży. Organizator ustala daty, miejsce docelowe, opis i walutę. Podsumowanie pokazuje grupę, koszty, zadania i najbliższy przyszły niewykonany punkt według daty i godziny.

Plan jest pogrupowany według dni. Formularz używa kalendarza ograniczonego do terminu wyjazdu i zegara 24-godzinnego. Na kompaktowym ekranie albo przy większej czcionce pojawia się systemowy kalendarz oraz specjalizowany formularz godziny. Punkt może zawierać zapisane miejsce, opis i przypomnienie. Główny edytor pyta o odrzucenie niezapisanych zmian przy cofnięciu.

## Zadania i pomysły

Dwie listy obejmują zadania oraz pakowanie. Element można przypisać osobie i oznaczyć jako wykonany. Pomysły mają tytuł, miejsce i opis. Każdy uczestnik oddaje i cofa własny głos. Przeniesienie propozycji do harmonogramu wymaga potwierdzenia danych punktu w formularzu.

## Wydatki, budżet i spłaty

Wydatek ma kwotę, datę, płatnika i mapę udziałów osób. Podział równy rozdziela pozostałe grosze deterministycznie; podział własny wymaga sumy udziałów równej kosztowi. Kwoty są całkowite w groszach/centach. Nowy wydatek otrzymuje dzisiejszą datę.

Sześć kategorii to transport, noclegi, jedzenie, atrakcje, zakupy i pozostałe. Koszty można wyszukiwać po nazwie, kategorii i płatniku, filtrować i sortować. Filtry i przewinięcie zakładek wracają w bieżącej sesji.

Organizator ustala budżet i limity kategorii. Suma limitów nie może przekroczyć całości. Saldo osoby wynosi: zapłacone wydatki minus jej udziały, plus wysłane spłaty, minus odebrane spłaty. Spłatę zapisuje jej nadawca i tylko on może ją zmieniać lub usunąć. Data nie może pochodzić z przyszłości. To deklaracja, nie transakcja bankowa; nadpłata może odwrócić należność.

## Miejsca, mapa i pogoda

Miejsce można dodać ręcznie współrzędnymi lub wybrać miejscowość z wyszukiwarki Open-Meteo. Lista jest współdzielona; na mapie pojawiają się znaczniki i atrybucja OpenStreetMap. Mapa pokazuje ładowanie, błąd i ponowienie. Przejście do zewnętrznych map nie wymaga od aplikacji uprawnienia GPS.

Prognoza obejmuje najbliższe 7 dni dla miejsca: minimum i maksimum temperatury, opis i prawdopodobieństwo opadów. Ostatni wynik jest przechowywany lokalnie i oznaczony czasem pobrania. Nie jest to prognoza na dowolnie odległą datę wyjazdu.

## Współpraca i błędy

Organizator udostępnia 12-znakowy kod albo link `tripsync://join?code=KOD`. Pozostali logują się na tym samym API. Zapis offline pozostawia operację w kolejce. Ręczna synchronizacja lub WorkManager wysyła zmiany i pobiera dostępne wyjazdy. Konflikt wersji wymaga wyboru danych lokalnych albo serwera. Historia jest dopisywana przez backend po zaakceptowanej zmianie i pokazuje do 200 najnowszych wpisów.

Usunięcie uczestnika z aktywnymi kosztami/spłatami wymaga wcześniejszego uporządkowania tych danych. Kod zaproszenia można unieważnić przez rotację. Nie ma funkcji zmiany właściciela wyjazdu.

## Zrzuty ekranu

| Widok | Obraz |
|---|---|
| Lista wyjazdów | [01-wyjazdy](screenshots-1.2/01-wyjazdy.png) |
| Podsumowanie | [02-podsumowanie](screenshots-1.2/02-podsumowanie.png) |
| Plan | [03-harmonogram](screenshots-1.2/03-harmonogram.png) |
| Zadania | [04-zadania](screenshots-1.2/04-zadania.png) |
| Pomysły | [05-glosowanie](screenshots-1.2/05-glosowanie.png) |
| Koszty | [06-rozliczenia](screenshots-1.2/06-rozliczenia.png) |
| Formularz wydatku | [07-formularz](screenshots-1.2/07-formularz.png) |
| Budżet | [08-budzet](screenshots-1.2/08-budzet.png) |
| Spłata | [09-splata](screenshots-1.2/09-splata.png) |
| Mapa | [10-mapa](screenshots-1.2/10-mapa.png) |
| Historia | [11-historia](screenshots-1.2/11-historia.png) |
| Kalendarz i zegar | [14-kalendarz](screenshots-1.2/14-kalendarz.png), [15-zegar](screenshots-1.2/15-zegar.png) |
| Motyw ciemny | [16-ciemny](screenshots-1.2/16-ciemny.png) |
| Ekran kompaktowy | [17-kalendarz](screenshots-1.2/17-kalendarz-kompakt.png), [18-godzina](screenshots-1.2/18-godzina-kompakt.png) |

Powyższe obrazy wykonano w wersji 1.2 na API 29. [Screen pogody](screenshots/12-pogoda.png) pochodzi z wcześniejszej weryfikacji 1.1 z 04.10.2026 i nie przedstawia odświeżonego UI 1.2. Podział ten zapobiega mieszaniu dowodów z różnych wydań.
