from pathlib import Path
from html import escape

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'docs/diagrams'
OUT.mkdir(parents=True, exist_ok=True)
NAVY, TEAL, PALE, GRAY = '#122E38', '#006B62', '#F2F6F0', '#54645E'

class Svg:
    def __init__(self, width, height, title):
        self.width, self.height = width, height
        self.parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" role="img" aria-label="{escape(title)}"><title>{escape(title)}</title><rect x="0" y="0" width="{width}" height="{height}" fill="#FFFFFF"/>']
    def text(self, x, y, text, size=16, color=NAVY, bold=False, anchor='start'):
        self.parts.append(f'<text x="{x}" y="{y}" font-family="Arial, sans-serif" font-size="{size}" fill="{color}" font-weight="{700 if bold else 400}" text-anchor="{anchor}">{escape(text)}</text>')
    def line(self, x1, y1, x2, y2, dashed=False, arrow=False):
        self.parts.append(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{TEAL}" stroke-width="2"'+(' stroke-dasharray="7 5"' if dashed else '')+'/>')
        if arrow:
            import math
            angle = math.atan2(y2-y1, x2-x1)
            points = [(x2,y2),(x2-10*math.cos(angle-.45),y2-10*math.sin(angle-.45)),(x2-10*math.cos(angle+.45),y2-10*math.sin(angle+.45))]
            self.parts.append(f'<polygon points="{" ".join(f"{x},{y}" for x,y in points)}" fill="{TEAL}"/>')
    def box(self, x, y, w, h, title, rows=()):
        self.parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="{PALE}" stroke="#CEDBD3" stroke-width="2"/>')
        self.text(x+w/2, y+29, title, 19, bold=True, anchor='middle')
        for i,row in enumerate(rows):
            self.text(x+18, y+56+i*22, row, 16, GRAY)
    def write(self, name):
        (OUT/name).write_text('\n'.join(self.parts)+ '\n</svg>\n', encoding='utf-8')

s=Svg(1000,790,'Architektura MVVM TripSync')
for a in [(500,110,500,155),(500,235,500,280),(360,360,180,420),(500,360,500,420),(640,360,820,420),(500,500,500,550),(500,630,500,680),(290,315,320,315),(680,195,730,195),(840,235,840,285),(680,75,740,75)]:
    s.line(*a,arrow=True)
s.line(40,470,20,470,dashed=True);s.line(20,470,20,195,dashed=True);s.line(20,195,320,195,dashed=True,arrow=True)
s.text(40,180,'Flow',16,GRAY)
s.line(300,620,310,620);s.line(310,620,310,220);s.line(310,220,320,220,arrow=True)
s.line(165,360,165,420,arrow=True)
s.box(320,40,360,70,'Jetpack Compose / Navigation', ['Ekrany, formularze i motywy'])
s.box(320,155,360,80,'TripViewModel / StateFlow',['Stan UI i działania użytkownika'])
s.box(320,280,360,80,'Repository / Mutex',['Transakcje, kolejka i konflikty'])
s.box(40,420,260,100,'Room / SQLite',['records, operations, conflicts','Lokalne źródło danych / Flow'])
s.box(360,420,280,80,'Retrofit / TripApi',['REST, DTO i token sesji'])
s.box(700,420,260,100,'Keystore / DataStore',['Szyfrowana sesja','Motyw oraz adres API'])
s.box(360,550,280,80,'Backend Python',['Store / Handler / role'])
s.box(360,680,280,80,'MySQL lub SQLite',['Trwała baza backendu'])
s.box(40,275,250,85,'WorkManager',['SyncWorker / ReminderWorker'])
s.box(730,155,240,80,'TravelRepository',['Osobny Retrofit / cache'])
s.box(730,285,240,70,'Open-Meteo',['Miejscowości i prognoza'])
s.box(740,40,230,70,'Mapa miejsc',['WebView / Leaflet / OSM'])
s.box(40,580,260,90,'Domena',['Money / Validation / Dates','Typowane modele Kotlin'])
s.text(40,775,'Dane ekranów pochodzą z Room; synchronizacja aktualizuje lokalną bazę.',16,GRAY)
s.write('architektura.svg')

s=Svg(1000,760,'Schemat fizycznej bazy backendu SQLite / MySQL')
s.line(470,150,530,150,arrow=True)
s.line(250,235,250,275,arrow=True)
s.line(530,365,470,365,dashed=True,arrow=True)
s.line(750,455,750,500,dashed=True,arrow=True)
s.line(45,150,20,150,dashed=True);s.line(20,150,20,580,dashed=True);s.line(20,580,45,580,dashed=True,arrow=True)
s.box(45,45,425,190,'users',['id PK · email UNIQUE','name','salt · password (hash)'])
s.box(530,45,425,190,'sessions',['token_hash PK','user_id FK → users.id','expires (Unix, sekundy)'])
s.box(45,275,425,180,'members',['trip_id + user_id PK','user_id FK → users.id','role'])
s.box(530,275,425,180,'records',['id PK · trip_id · kind','payload JSON','version · deleted · indeks trip_id'])
s.box(45,500,425,180,'operations',['user_id + id PK','digest · result JSON','Wyniki przyjętych operacji'])
s.box(530,500,425,180,'invites',['code PK','trip_id UNIQUE','Jeden aktualny kod wyjazdu'])
s.text(45,714,'Linia ciągła: fizyczna relacja FK. Linia przerywana: relacja logiczna kontrolowana w kodzie.',16,GRAY)
s.text(45,739,'MySQL: InnoDB / utf8mb4. SQLite: FK włączone / WAL. Typy szczegółowe: baza-danych.md.',16,GRAY)
s.write('baza-backendu.svg')

s=Svg(1000,390,'Lokalny schemat Room, wersja 1')
s.line(320,175,345,175,dashed=True,arrow=True)
s.line(175,310,175,343,dashed=True);s.line(175,343,825,343,dashed=True);s.line(825,343,825,310,dashed=True,arrow=True)
s.box(30,35,290,275,'records',['id PK','tripId · kind','json · version','deleted · pending','Indeksy: tripId, kind'])
s.box(345,35,310,275,'operations',['id PK','recordId UNIQUE','tripId · kind · json','baseVersion · deleted','createdAt','Kolejka oczekujących zapisów'])
s.box(680,35,290,275,'conflicts',['recordId PK','serverJson','message','invalid'])
s.text(30,375,'Powiązania logiczne, bez fizycznych FK. Zapis rekordu i operacji w jednej transakcji Room.',16,GRAY)
s.write('baza-lokalna.svg')

s=Svg(1000,1040,'Diagram UML przypadków użycia TripSync')
s.parts.append('<rect x="250" y="25" width="555" height="990" rx="12" fill="#FFFFFF" stroke="#CEDBD3" stroke-width="2"/>')
s.text(528,58,'TripSync',23,bold=True,anchor='middle')
for y in [100,175]: s.line(130,145,280,y)
for y in [250,325,400,475,550,625,700]: s.line(130,440,280,y)
for y in [775,850,925]: s.line(130,800,280,y)
s.line(880,925,775,975);s.line(880,925,775,625)
s.line(100,743,100,505)
s.parts.append(f'<polygon points="100,487 87,507 113,507" fill="#FFFFFF" stroke="{TEAL}" stroke-width="2"/>')
for y,title in [(100,'Rejestracja i logowanie'),(175,'Demo lokalne'),(250,'Tworzenie wyjazdu / dołączenie'),(325,'Plan, zadania i pakowanie'),(400,'Propozycje i własne głosy'),(475,'Wydatki, salda i własne spłaty'),(550,'Miejsca, mapa i prognoza'),(625,'Synchronizacja i konflikty'),(700,'Odczyt historii zmian'),(775,'Edycja/usunięcie wyjazdu'),(850,'Zarządzanie budżetem'),(925,'Zaproszenia i uczestnicy'),(975,'Lokalne przypomnienia')]:
    ry=20 if y==975 else 25
    s.parts.append(f'<ellipse cx="528" cy="{y}" rx="248" ry="{ry}" fill="{PALE}" stroke="{TEAL}" stroke-width="2"/>')
    s.text(528,y+6,title,18,anchor='middle')
def actor(x,y,title):
    s.parts.append(f'<circle cx="{x}" cy="{y-34}" r="12" fill="#FFFFFF" stroke="{NAVY}" stroke-width="2"/>')
    for coords in [(x,y-22,x,y+16),(x-23,y-5,x+23,y-5),(x,y+16,x-20,y+43),(x,y+16,x+20,y+43)]: s.line(*coords)
    s.text(x,y+67,title,19,bold=True,anchor='middle')
actor(100,135,'Gość');actor(100,415,'Uczestnik');actor(100,790,'Organizator');actor(905,920,'Android')
s.text(35,620,'Dziedziczenie',16,GRAY);s.text(35,643,'uprawnień',16,GRAY)
s.write('przypadki-uzycia.svg')
print(OUT)
