from pathlib import Path
import json
import os
import re
from xml.etree import ElementTree
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.enums import TA_CENTER
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Image, Flowable, KeepTogether

ROOT = Path(__file__).resolve().parent.parent
for name, candidates in [('TS', ['/System/Library/Fonts/Supplemental/Arial.ttf', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf']), ('TSB', ['/System/Library/Fonts/Supplemental/Arial Bold.ttf', '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'])]:
    font = next((p for p in candidates if Path(p).exists()), None)
    if not font:
        raise RuntimeError('Zainstaluj Arial lub DejaVu Sans, aby zachować polskie znaki.')
    pdfmetrics.registerFont(TTFont(name, font))
pdfmetrics.registerFontFamily('TS', normal='TS', bold='TSB')
NAVY = colors.HexColor('#142C46')
TEAL = colors.HexColor('#007B78')
MINT = colors.HexColor('#57D9CC')
GRAY = colors.HexColor('#465769')
PALE = colors.HexColor('#EDF5F5')
styles = {
    'body': ParagraphStyle('body', fontName='TS', fontSize=10, leading=14, textColor=GRAY, spaceAfter=8),
    'small': ParagraphStyle('small', fontName='TS', fontSize=8.7, leading=12, textColor=GRAY, spaceAfter=5),
    'h1': ParagraphStyle('h1', fontName='TSB', fontSize=22, leading=27, textColor=NAVY, spaceAfter=16),
    'h2': ParagraphStyle('h2', fontName='TSB', fontSize=12, leading=17, textColor=NAVY, spaceBefore=10, spaceAfter=8),
    'cell': ParagraphStyle('cell', fontName='TS', fontSize=8.8, leading=12, textColor=GRAY),
    'cellhead': ParagraphStyle('cellhead', fontName='TSB', fontSize=9, leading=12, textColor=colors.white),
    'caption': ParagraphStyle('caption', fontName='TSB', fontSize=9, leading=13, textColor=NAVY, alignment=TA_CENTER),
    'code': ParagraphStyle('code', fontName='TS', fontSize=9, leading=13, textColor=TEAL, backColor=PALE, borderPadding=8, spaceAfter=12)
}


def p(text, style='body'):
    return Paragraph(text, styles[style])


def table(rows, widths, header=True):
    content = [[p(str(cell), 'cellhead' if header and i == 0 else 'cell') for cell in row] for i, row in enumerate(rows)]
    t = Table(content, colWidths=widths, hAlign='LEFT')
    t.setStyle(TableStyle([
        ('VALIGN', (0, 0), (-1, -1), 'TOP'), ('LEFTPADDING', (0, 0), (-1, -1), 9),
        ('RIGHTPADDING', (0, 0), (-1, -1), 9), ('TOPPADDING', (0, 0), (-1, -1), 7),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 7), ('LINEBELOW', (0, 0), (-1, -1), .4, colors.HexColor('#DAE4EA')),
        ('BACKGROUND', (0, 0), (-1, 0), NAVY if header else PALE),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, colors.HexColor('#F5F8FA')])
    ]))
    return t


class Diagram(Flowable):
    def __init__(self, kind, height):
        Flowable.__init__(self)
        self.width = 499
        self.height = height
        self.kind = kind

    def draw(self):
        c = self.canv

        def box(x, y, w, h, title, lines=()):
            c.setFillColor(PALE)
            c.setStrokeColor(colors.HexColor('#B6D0D3'))
            c.roundRect(x, y, w, h, 9, stroke=1, fill=1)
            c.setFillColor(NAVY)
            c.setFont('TSB', 10)
            c.drawCentredString(x+w/2, y+h-19, title)
            c.setFont('TS', 8.5)
            for i, line in enumerate(lines):
                c.drawString(x+11, y+h-37-i*13, line)

        def arrow(x1, y1, x2, y2, dotted=False):
            import math
            c.setStrokeColor(TEAL)
            c.setFillColor(TEAL)
            c.setLineWidth(1.2)
            c.setDash(3, 3) if dotted else c.setDash()
            c.line(x1, y1, x2, y2)
            c.setDash()
            angle = math.atan2(y2-y1, x2-x1)
            path = c.beginPath()
            path.moveTo(x2, y2)
            path.lineTo(x2-7*math.cos(angle-.4), y2-7*math.sin(angle-.4))
            path.lineTo(x2-7*math.cos(angle+.4), y2-7*math.sin(angle+.4))
            path.close()
            c.drawPath(path, fill=1, stroke=0)

        if self.kind == 'architecture':
            box(130, 320, 240, 52, 'Jetpack Compose', ['Ekrany, formularze, nawigacja'])
            box(130, 241, 240, 52, 'TripViewModel / StateFlow', ['Stan i obsługa działań użytkownika'])
            box(130, 154, 240, 60, 'Repository / Mutex', ['Transakcje, kolejka, konflikty'])
            box(0, 57, 150, 65, 'Room / Flow', ['records, operations, conflicts', 'Lokalne źródło danych'])
            box(180, 57, 140, 65, 'Retrofit / OkHttp', ['REST + token sesji', 'Serwer Python / SQLite'])
            box(350, 57, 149, 65, 'Keystore / DataStore', ['Szyfrowana sesja', 'Adres API, motyw'])
            arrow(250, 320, 250, 293); arrow(250, 241, 250, 214)
            arrow(180, 154, 75, 122); arrow(250, 154, 250, 122); arrow(325, 154, 425, 122)
            box(0, 253, 100, 56, 'Domena', ['Money', 'Validation / modele'])
            arrow(130, 267, 100, 280)
            box(0, 157, 100, 60, 'TravelRepository', ['Open-Meteo / cache', 'Osobny Retrofit'])
            arrow(130, 241, 100, 215)
            box(392, 253, 107, 56, 'Mapa miejsc', ['Leaflet / kafelki OSM'])
            arrow(370, 346, 445, 309)
            box(392, 163, 107, 65, 'WorkManager', ['SyncWorker', 'ReminderWorker'])
            arrow(392, 188, 370, 188)
            c.setFont('TS', 8)
            c.setFillColor(GRAY)
            c.drawCentredString(250, 20, 'UI odczytuje Room. Sieć aktualizuje lokalną bazę; nie blokuje ekranu.')
        elif self.kind == 'usecases':
            c.setStrokeColor(colors.HexColor('#B6D0D3'))
            c.roundRect(140, 12, 356, 449, 8, fill=0, stroke=1)
            c.setFillColor(NAVY); c.setFont('TSB', 11); c.drawCentredString(318, 440, 'TripSync')
            def actor(x, y, name):
                c.setStrokeColor(NAVY); c.setLineWidth(1.3)
                c.circle(x, y+24, 8, stroke=1, fill=0); c.line(x, y+16, x, y-9)
                c.line(x-14, y+6, x+14, y+6); c.line(x, y-9, x-12, y-27); c.line(x, y-9, x+12, y-27)
                c.setFont('TSB', 9); c.setFillColor(NAVY); c.drawCentredString(x, y-43, name)
            actors=[(45,390,'Gość'),(45,250,'Uczestnik'),(45,108,'Organizator')]
            for a in actors: actor(*a)
            cases=[('Konto / demo',399),('Dołączenie do wyjazdu',350),('Plan / zadania / mapa / pogoda',301),('Propozycje / własny głos',252),('Koszty / budżety / spłaty',203),('Synchronizacja / konflikty / historia',154),('Zarządzanie wyjazdem / ekipą',105),('Przypomnienia lokalne',56)]
            for title,y in cases:
                c.setFillColor(PALE); c.setStrokeColor(TEAL); c.ellipse(171,y-18,474,y+18,fill=1,stroke=1)
                c.setFillColor(NAVY);c.setFont('TS',9);c.drawCentredString(322,y-3,title)
            c.setStrokeColor(colors.HexColor('#9BB2C0'));c.setLineWidth(.8)
            c.line(60,390,171,399)
            for yy in [350,301,252,203,154]: c.line(60,250,171,yy)
            c.line(60,108,171,105)
            actor(88,47,'Android')
            c.setStrokeColor(colors.HexColor('#9BB2C0')); c.line(103,47,171,56)
            c.setFont('TS',8);c.setFillColor(GRAY);c.drawString(165,0,'Organizator ma również przypadki użycia uczestnika.')
        elif self.kind == 'database':
            box(0,256,222,95,'users',['id PK / email UNIQUE','name / salt / password hash'])
            box(276,256,222,95,'sessions',['token_hash PK','user_id FK → users.id / expires'])
            box(0,128,222,96,'members',['(trip_id, user_id) PK','user_id FK → users.id / role'])
            box(276,128,222,96,'records',['id PK / trip_id / kind','payload JSON / version / deleted'])
            box(0,0,222,96,'operations',['(user_id, id) PK','digest / result JSON'])
            box(276,0,222,96,'invites',['code PK / trip_id UNIQUE','Rotacja unieważnia stary kod'])
            arrow(222,302,276,302)
            arrow(110,256,110,224)
            arrow(276,176,222,176,True)
            arrow(389,128,389,96,True)
            c.setStrokeColor(TEAL); c.setDash(3,3); c.line(0,302,-9,302); c.line(-9,302,-9,48); c.setDash(); arrow(-9,48,0,48,True)
        elif self.kind == 'local':
            box(0,16,150,123,'records',['id PK / tripId / kind','json / version','deleted / pending'])
            box(174,16,150,123,'operations',['id PK / recordId UNIQUE','tripId / kind / json','baseVersion / deleted','createdAt'])
            box(348,16,150,123,'conflicts',['recordId PK','serverJson / message','invalid'])
            arrow(150,80,174,80,True)
            c.setStrokeColor(TEAL);c.setDash(3,3);c.line(75,16,75,4);c.line(75,4,423,4);c.setDash();arrow(423,4,423,16,True)


def frame(c, doc):
    c.saveState()
    c.setStrokeColor(colors.HexColor('#DCE5EC'));c.line(48,43,547,43)
    c.setFillColor(GRAY);c.setFont('TS',8);c.drawString(48,29,'TripSync | Dokumentacja projektu Android | 04.10.2026')
    c.drawRightString(547,29,str(doc.page))
    c.setFillColor(TEAL);c.setFont('TSB',9);c.drawString(48,813,'TRIPSYNC / PROJEKT ZESPOŁOWY')
    c.restoreState()


story=[]
add=story.extend
add([Spacer(1,25),p('TripSync','h1'),p('Wersja 1.1: plan, budżet, mapa i pogoda - także dane offline.','h2'),Spacer(1,18)])
class Logo(Flowable):
    def __init__(self):
        Flowable.__init__(self)
        self.width=499
        self.height=104
    def draw(self):
        c=self.canv;c.setFillColor(NAVY);c.roundRect(0,0,499,104,16,fill=1,stroke=0)
        c.setStrokeColor(MINT);c.setFillColor(MINT);c.setLineWidth(3)
        points=[(30,40),(50,70),(71,34),(94,62)]
        path=c.beginPath();path.moveTo(*points[0])
        for point in points[1:]:path.lineTo(*point)
        c.drawPath(path)
        for x,y in points[:3]:c.circle(x,y,4,stroke=0,fill=1)
        c.line(83,62,94,62);c.line(94,62,94,51)
        c.setFillColor(colors.white);c.setFont('TSB',30);c.drawString(125,45,'TripSync')
add([Logo(),Spacer(1,25),p('Cel i zakres','h2'),p('Aplikacja dla niewielkich grup znajomych organizujących wspólne wyjazdy. Łączy harmonogram, zadania, pakowanie, pomysły, budżety, kategorie wydatków, spłaty, historię zmian oraz mapę i prognozę pogody. Dane są dostępne bez internetu, a po odzyskaniu połączenia synchronizowane z własnym backendem REST.'),p('Zakres dla trzech osób rozszerza podstawowe wymagania o współpracę wielu kont, role i kontrolę dostępu, wykrywanie konfliktów edycji, kolejkę operacji, dokładne rozliczenia i integracje Open-Meteo/OpenStreetMap.'),table([['Platforma','Dostarczany rezultat'],['Android 8.0+ / Kotlin','Kod źródłowy, APK debug, backend SQLite/Python'],['MVVM / Jetpack Compose / Material 3','Ekrany, testy, diagramy, instrukcja i screeny'],['Działanie lokalne i online','Demo bez konta; współdzielenie po uruchomieniu API']],[220,279]),p('Scenariusz użytkownika','h2'),p('Organizator tworzy wyjazd i udostępnia kod zaproszenia. Grupa ustala plan, głosuje na pomysły i dzieli zadania. Każdy zapisuje poniesione koszty, także offline. Po synchronizacji wszyscy widzą aktualne dane, a ekran kosztów pokazuje salda i propozycje przelewów.'),p('Granice projektu','h2'),p('Jedna waluta na wyjazd: PLN, EUR albo USD. Bez czatu, rezerwacji, realizowania płatności i pełnych map offline. Synchronizacja jest okresowa lub ręczna; aplikacja nie deklaruje aktualizacji w czasie rzeczywistym.','small'),PageBreak()])
add([p('Wymagania','h1'),p('Funkcjonalne','h2')])
functional=[('F01','Konta: rejestracja, logowanie, szyfrowana sesja i wylogowanie.'),('F02','CRUD wyjazdów: nazwa, miejsce, daty, opis i waluta; rola organizatora.'),('F03','Uczestnicy: dołączenie kodem/linkiem, rotacja kodu, usuwanie dostępu.'),('F04','Harmonogram: CRUD, godzina, miejsce, kolejność i wykonanie.'),('F05','Propozycje: CRUD, jeden własny głos, cofnięcie i ponowne oddanie głosu.'),('F06','Przeniesienie propozycji do harmonogramu przez formularz.'),('F07','Zadania i pakowanie: CRUD, przypisanie osoby, oznaczanie wykonania.'),('F08','Wydatki: CRUD, płatnik, data, uczestnicy i dwa sposoby podziału kosztu.'),('F09','Salda i propozycje przelewów z dokładnością grosza/centa.'),('F10','Trwałość danych i lokalny CRUD bez internetu.'),('F11','Synchronizacja, kolejka operacji, ponowienia i konflikty wersji.'),('F12','Lokalne przypomnienia o punktach planu i zadaniach.'),('F13','Wieloekranowa nawigacja, zachowanie formularza, ustawienia motywu.'),('F14','Walidacja i czytelne komunikaty błędów sieci, bazy oraz serwera.'),('F15','Edytowalne demo bez konta i systemowe udostępnianie planu.')]
t=table([['ID','Wymaganie']]+functional,[47,452]);t.setStyle(TableStyle([('TOPPADDING',(0,1),(-1,-1),4),('BOTTOMPADDING',(0,1),(-1,-1),4)]));story.append(t)
add([p('Niefunkcjonalne','h2'),p('<b>N01-N03:</b> Material Design 3, responsywne widoki, modularny kod i asynchroniczne operacje Room/Retrofit poza głównym wątkiem. <b>N04:</b> odporność na odtworzenie aktywności przez ViewModel, rememberSaveable i Room. <b>N05-N06:</b> kontrola dostępu, Keystore, HTTPS, dostępność i etykiety kontrolek. <b>N07-N09:</b> testy domeny, UI i integracji, możliwość rozbudowy, historia Git i komplet materiałów do uruchomienia.','small'),PageBreak()])
extended=[('F16','Budżet całego wyjazdu i limity kategorii; CRUD przez organizatora.'),('F17','Sześć kategorii wydatków, zestawienie i filtr kosztów.'),('F18','CRUD własnych deklaracji spłat oraz przeliczenie pozostałych sald.'),('F19','Historia zmian z autorem i czasem serwera; wyłącznie do odczytu.'),('F20','CRUD współdzielonych miejsc, mapa i otwarcie aplikacji map.'),('F21','Wyszukiwanie miejscowości przez API i zapis na wspólnej mapie.'),('F22','Prognoza na 7 dni: temperatury, opis pogody i szansa opadów.'),('F23','Ostatnia prognoza zapisana lokalnie, daty i komunikaty błędów.')]
add([p('Rozszerzenia wersji 1.1','h1'),table([['ID','Wymaganie']]+extended,[47,452]),p('Budżety i spłaty','h2'),p('Organizator ustala limit całkowity oraz limity sześciu kategorii. Suma limitów kategorii nie może przekraczać budżetu. Ekran pokazuje wykorzystanie i przekroczenia. Zapisane spłaty zwiększają saldo nadawcy i zmniejszają saldo odbiorcy, dzięki czemu proponowane rozliczenie uwzględnia oddane pieniądze. Nadpłata tworzy należność w przeciwnym kierunku.'),p('Historia i miejsca','h2'),p('Historia powstaje na serwerze w transakcji zmiany; ponowienia i konflikty nie tworzą duplikatów. Klient nie może wysyłać wpisów historii. Pokazujemy 200 najnowszych wpisów, bez cofania zmian. Nazwy zapisanych miejsc można wybrać w harmonogramie i pomysłach.'),p('Pogoda i granice integracji','h2'),p('Open-Meteo zwraca prognozę najbliższych siedmiu dni w strefie miejsca. Wyszukiwanie obejmuje miejscowości; dokładne punkty można dodać współrzędnymi. Mapa wymaga internetu do kafelków, a stara prognoza jest oznaczona datą i komunikatem. Spłaty są deklaracjami użytkowników, bez wykonywania lub bankowego potwierdzania przelewów.','small'),PageBreak()])
add([p('Architektura aplikacji','h1'),p('MVVM z wydzieloną domeną','h2'),Diagram('architecture',385),p('Przepływ i synchronizacja','h2'),p('1. Formularz waliduje dane, a repozytorium zapisuje rekord i operację w jednej transakcji Room.<br/>2. Ekrany otrzymują dane z lokalnej bazy przez Flow i StateFlow.<br/>3. SyncWorker wysyła operacje, a serwer sprawdza sesję, rolę, dane i bazową wersję.<br/>4. Identyfikator operacji zapobiega podwójnemu zapisowi po ponowieniu.<br/>5. Różnica wersji tworzy konflikt. Użytkownik wybiera lokalną albo zdalną wersję.<br/>6. Snapshot uzupełnia Room, zachowując niewysłane lokalne zmiany. Utrata dostępu usuwa lokalny wyjazd po udanej synchronizacji.'),p('Mapa: WebView + lokalny Leaflet + kafelki OSM. Pogoda: TripViewModel → TravelRepository → osobny Retrofit Open-Meteo; prywatny cache prognozy. Nowe typy rekordów nie zmieniają schematu Room.', 'small'),p('Usunięcia są przesyłane jako tombstone. Mutex porządkuje zapisy i synchronizację. Dane z aktywnym konfliktem czekają na rozstrzygnięcie; nie są automatycznie nadpisywane.','small'),PageBreak()])
add([p('Przypadki użycia','h1'),Diagram('usecases',474),p('Role i uprawnienia','h2'),p('<b>Gość:</b> konto albo lokalne demo. <b>Uczestnik:</b> wspólny plan, zadania, koszty i propozycje; głos wyłącznie za siebie. <b>Organizator:</b> dodatkowo tworzenie, edycja i usuwanie wyjazdu oraz zaproszenia, zarządzanie uczestnikami i budżetem.'),p('<b>System Android:</b> WorkManager uruchamia synchronizację i lokalne przypomnienia. Organizator nie może usunąć siebie ani uczestnika mającego aktywne wydatki; wcześniej należy zmienić koszty i usunąć aktywne spłaty tej osoby.','small'),PageBreak()])
add([p('Diagramy baz danych','h1'),p('Backend: fizyczne tabele SQLite','h2'),Diagram('database',351),p('Strzałki ciągłe: fizyczne powiązania FK do users. Linie przerywane: powiązania logiczne obsługiwane w API. Rekordy podrzędne wskazują wyjazd przez trip_id; payload JSON zawiera pola danego typu.','small'),p('Urządzenie: Room','h2'),Diagram('local',146),p('Relacje lokalne są logiczne, bez fizycznych FK. Unikalny indeks operations.recordId zapewnia jedną oczekującą operację na rekord. Indeksy records.tripId i records.kind wspierają odczyty. Spójność zapisów zapewniają transakcje repozytorium. Schemat Room jest dołączony w app/schemas.','small'),PageBreak()])
add([p('Technologie i uruchomienie','h1'),table([['Technologia','Zastosowanie'],['Kotlin / AGP 9.1.1 / SDK 36.1','Android; Gradle Wrapper 9.3.1, JDK 17 lub 21'],['Compose BOM 2024.01.00 / Material 3','Interfejs, motyw, kontrolki i dostępność'],['Navigation Compose 2.7.7 / Lifecycle 2.9.0','Nawigacja i ViewModel; zależności rozwiązywane przez Gradle'],['Room 2.8.4 / KSP 2.2.10-2.0.2','Lokalna baza i generowany DAO'],['Coroutines 1.10.2 / Flow','Asynchroniczny odczyt i reaktywny stan'],['Retrofit 2.9.0 / OkHttp 4.12.0 / Gson 2.11.0','REST oraz kodowanie JSON'],['DataStore 1.1.7 / WorkManager 2.9.0','Ustawienia, synchronizacja i przypomnienia'],['Keystore / AES-GCM','Szyfrowane przechowywanie sesji'],['Python 3.10+ / SQLite / unittest','Backend bez dodatkowych pakietów'],['JUnit 4 / Compose UI Test / AndroidJUnitRunner','Testy jednostkowe i urządzeniowe'],['Leaflet 1.9.4 / OpenStreetMap','Lokalna biblioteka JS i kafelki mapy'],['Open-Meteo / TravelRepository','Geokodowanie, prognoza i cache bez klucza API']],[211,288]),p('Instalacja i start','h2'),p('Najprościej: zainstaluj <b>output/apk/TripSync-debug.apk</b> i wybierz <b>Wypróbuj bez konta</b>. Do współpracy uruchom serwer, ustaw adres API przed zalogowaniem i utwórz oddzielne konta.'),p('python3 backend/server.py<br/>./gradlew :app:assembleDebug<br/>./gradlew :app:testDebugUnitTest','code'),p('Emulator debug: http://10.0.2.2:8080/. Telefon przez USB: adb reverse tcp:8080 tcp:8080 i adres http://127.0.0.1:8080/. Pozostałe urządzenia wymagają API HTTPS z zaufanym certyfikatem. Otwórz katalog w Android Studio i ustaw własne sdk.dir w local.properties. Pełne komendy, uruchomienie TLS i testy są w README.md.','small'),PageBreak()])

screens=ROOT/'docs/screenshots'
def shot(name,caption,width=135):
    f=screens/name
    if not f.exists():
        raise RuntimeError('Brak rzeczywistego zrzutu: '+str(f))
    image=Image(str(f),width=width,height=width*2400/1080)
    return [image,Spacer(1,5),p(caption,'caption')]
add([p('Najważniejsze widoki / 1','h1')])
photos=Table([[shot('01-wyjazdy.png','Lista wyjazdów i status danych'),shot('02-podsumowanie.png','Podsumowanie wyjazdu')],[shot('03-harmonogram.png','Harmonogram i wykonanie'),shot('04-zadania.png','Zadania oraz pakowanie')]],colWidths=[249.5,249.5])
photos.setStyle(TableStyle([('VALIGN',(0,0),(-1,-1),'TOP'),('ALIGN',(0,0),(-1,-1),'CENTER'),('TOPPADDING',(0,0),(-1,-1),0),('BOTTOMPADDING',(0,0),(-1,-1),12)]));story.append(photos);story.append(PageBreak())
add([p('Najważniejsze widoki / 2','h1')])
photos=Table([[shot('05-glosowanie.png','Propozycje i głosowanie',151),shot('06-rozliczenia.png','Wydatki, budżet i kategorie',151),shot('07-formularz.png','Formularz wydatku',151)]],colWidths=[166.3]*3)
photos.setStyle(TableStyle([('VALIGN',(0,0),(-1,-1),'TOP'),('ALIGN',(0,0),(-1,-1),'CENTER'),('LEFTPADDING',(0,0),(-1,-1),2),('RIGHTPADDING',(0,0),(-1,-1),2)]));story.append(photos)
add([Spacer(1,20),p('Źródło zrzutów','h2'),p('Obrazy pochodzą z działającej aplikacji na emulatorze Android, a nie z makiet. Test UI przechodzi przez główne zakładki i zapisuje rzeczywiste screeny. Dane przykładowe: trzy osoby, nocleg 720 PLN i bilety 180 PLN.'),p('Interpretacja rozliczeń','h2'),p('Przy równym podziale kosztów Anna ma saldo +420 PLN, Bartek -120 PLN, a Celina -300 PLN. Propozycja rozliczenia: Bartek oddaje Annie 120 PLN, Celina oddaje Annie 300 PLN. Suma sald wynosi zero.'),p('Nawigacja','h2'),p('Poza pokazanymi widokami dostępne są logowanie/rejestracja, uczestnicy z zaproszeniami, formularze pozostałych danych, ustawienia oraz rozstrzyganie konfliktów.'),PageBreak()])
add([p('Najważniejsze widoki / 3','h1')])
photos=Table([[shot('08-budzet.png','Limity całego budżetu i kategorii'),shot('09-splata.png','Formularz własnej spłaty')],[shot('10-mapa.png','Mapa wspólnych miejsc'),shot('11-historia.png','Autorzy i czas zmian')]],colWidths=[249.5,249.5])
photos.setStyle(TableStyle([('VALIGN',(0,0),(-1,-1),'TOP'),('ALIGN',(0,0),(-1,-1),'CENTER'),('TOPPADDING',(0,0),(-1,-1),0),('BOTTOMPADDING',(0,0),(-1,-1),12)]));story.append(photos);story.append(PageBreak())
add([p('Pogoda i darmowy hosting','h1')])
weather=Table([[shot('12-pogoda.png','Rzeczywista prognoza Open-Meteo',145),[p('PythonAnywhere Beginner Free','h2'),p('Backend ma gotowy adapter WSGI. Trzy pliki Python, trwała SQLite poza kodem, bez dodatkowych pakietów. HTTPS i domenę aplikacji zapewnia hosting.','small'),p('1. Załóż darmowe konto.<br/>2. Wgraj server.py, wsgi.py i backup.py do ~/TripSync/backend.<br/>3. Web → Manual configuration → Python 3.10+.<br/>4. Wklej szablon deploy/pythonanywhere_wsgi.py.example do WSGI.<br/>5. Reload i sprawdź /health.<br/>6. Wpisz adres HTTPS w TripSync przed rejestracją.','small'),p('Baza: ~/tripsync-data.sqlite. Aktualizacja: kopia bazy, nowe pliki kodu, Reload. Pilnuj terminu odnowienia aplikacji w panelu. Pełne komendy i odtwarzanie kopii: docs/hosting.md.','small'),p('Hosting nie został publicznie uruchomiony. Adapter WSGI przetestowano lokalnie. Konto i adres należą do użytkownika.','small')]]],colWidths=[180,319])
weather.setStyle(TableStyle([('VALIGN',(0,0),(-1,-1),'TOP'),('ALIGN',(0,0),(0,0),'CENTER')]));story.append(weather)
add([p('Źródła i licencje','h2'),p('<link href="https://www.pythonanywhere.com/pricing/">PythonAnywhere: darmowy plan, jedna aplikacja i 512 MB plików</link> · <link href="https://blog.pythonanywhere.com/221/">zmiany planu od 2026 r.</link>. Darmowy Render nie zachowuje lokalnej SQLite po restartach; wybór hostingu uwzględnia trwałość danych.','small'),p('<link href="https://open-meteo.com/en/docs">Open-Meteo: bezpłatne API niekomercyjne, dane CC BY 4.0</link>. <link href="https://operations.osmfoundation.org/policies/tiles/">OpenStreetMap: polityka kafelków i atrybucja</link>. Leaflet: licencja BSD-2-Clause w assets. Mapa nie pobiera całych obszarów offline i nie wymaga lokalizacji GPS telefonu.','small'),PageBreak()])
results=json.loads((ROOT/'docs/test-results.json').read_text())
add([p('Testy i rezultaty','h1'),p('Weryfikacja wykonana 04.10.2026','h2'),table([['Obszar','Wynik'],['Backend Python',f"{results['backend']} testów / 0 niepowodzeń"],['Domena Kotlin / JUnit',f"{results['unit']} testów / 0 niepowodzeń"],['Android: interfejs i REST',f"{results['device']} testów / 0 niepowodzeń"],['Budowanie APK','assembleDebug: zakończone powodzeniem'],['Android Lint',f"{results['lint_errors']} błędów / {results['lint_warnings']} ostrzeżenie"]],[250,249]),p('Zakres sprawdzeń','h2'),p('Domena: dokładne kwoty, reszty groszowe, błędne udziały, walidacja dat i godzin oraz 200 losowych scenariuszy rozliczeń. Backend: hashowanie, sesje, role, zaproszenia, idempotencja, konflikty, tombstone, głosowanie, wydatki, budżety, spłaty, historię, kopie bazy, proxy, WSGI i trwałość danych.'),p('Android: CRUD wydatku i zmiana salda, błędy formularza, zachowanie formularza po odtworzeniu aktywności, ponowne głosowanie, odtworzenie danych i główne widoki, limity budżetu, formularz spłaty, miejsce oraz historię, wysokość mapy i zachowanie cache pogody po błędzie sieci. Integracja REST: dwa oddzielne konta/repozytoria, lokalna kolejka, równoczesna edycja, wybór obu sposobów rozwiązania konfliktu, usunięcia i cofnięcie dostępu, budżet, miejsce, spłatę i autorów historii.'),p('Środowisko i dowody','h2'),p('Emulator Medium_Phone, Android 10 / API 29, ARM64, 1080 × 2400. Kompilacja: SDK 36.1, JDK 21 i Gradle 9.3.1. Testy backendu wykonano na Pythonie 3.13. Raporty XML, raport Lint i logi znajdują się w output/reports.','small'),p('Ograniczenia weryfikacji','h2'),p('Nie wykonano testów na fizycznym telefonie, audytu bezpieczeństwa ani testów obciążeniowych. Interwały powiadomień i pracy w tle podlegają ograniczeniom Androida. Końcowy Lint uruchomiono offline: 1 ostrzeżenie wersji Lifecycle. JavaScript jest wymagany przez lokalny Leaflet; WebView nie ma mostka do Androida. Sprawdzenia dostępności nowszych wersji przez sieć nie należą do tego przebiegu.','small'),p('Bezpieczeństwo i zakres odpowiedzialności','h2'),p('Sesja AES-GCM/Keystore; prywatna baza lokalna bez SQLCipher; wyłączone backup i transfer danych. Serwer: PBKDF2-SHA256, losowe tokeny ważne 7 dni, kontrola dostępu i limiter logowania. Backend jest dydaktyczny i wymaga własnego hostingu HTTPS do pracy poza lokalnym środowiskiem.','small'),PageBreak()])
add([p('Organizacja i oddanie','h1'),p('Podział folderów','h2'),table([['Folder','Odpowiedzialność'],['backend/','API, konta, uprawnienia, synchronizacja i testy'],['app/.../domain/','Modele, walidacja i rozliczenia'],['app/.../data/','Room, API, sesja, ustawienia i kolejka'],['app/.../ui/screens/','Oddzielne foldery funkcji: auth, trips, trip, plan, tasks, ideas, money, editor, members, conflicts, settings, budget, activity, places'],['app/src/test/ oraz androidTest/','Testy Kotlin, Compose i rzeczywistego REST'],['docs/ oraz scripts/','Dokumentacja, Mermaid, screeny i odtwarzalne budowanie'],['output/','APK, PDF, raporty oraz paczka projektu']],[235,264]),p('Proponowany podział odpowiedzialności','h2'),p('<b>Osoba 1:</b> backend, konta, historia i hosting. <b>Osoba 2:</b> interfejs, plan, zadania, mapa i pogoda. <b>Osoba 3:</b> koszty, kategorie, budżety, spłaty oraz synchronizacja. Każdy obszar obejmuje testy i dokumentację. Podział umożliwia niezależne rozwijanie modułów i wspólną integrację.'),p('Repozytorium Git','h2'),p('Projekt ma lokalne repozytorium i rzeczywistą historię etapów pracy. Nie wskazano zdalnego repozytorium, dlatego dokumentacja nie zawiera fikcyjnego publicznego linku. Po publikacji należy dopisać adres repozytorium. Dołączony Git bundle zachowuje historię.'),p('Materiały do przekazania','h2'),p('Źródła Android i backendu, Gradle Wrapper, APK debug, dokumentacja PDF/Markdown, diagramy, schemat Room, screeny i raporty testów. Paczka pomija lokalne ścieżki SDK, cache, tokeny i bazy testowe.'),p('Rozbudowa','h2'),p('Możliwe kolejne kroki: większe testy urządzeniowe, paginacja synchronizacji, testy hostingu publicznego, odzyskiwanie hasła i weryfikacja adresu e-mail. Funkcje te nie są przedstawiane jako już zaimplementowane.','small')])
output=ROOT/'output/pdf/TripSync_dokumentacja_projektu.pdf';output.parent.mkdir(parents=True,exist_ok=True)
doc=SimpleDocTemplate(str(output),pagesize=A4,leftMargin=48,rightMargin=48,topMargin=63,bottomMargin=57,title='TripSync - dokumentacja projektu Android',author='Zespół projektowy TripSync')
doc.build(story,onFirstPage=frame,onLaterPages=frame)
print(output)
