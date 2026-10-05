from pathlib import Path
from xml.etree import ElementTree
from html import escape
import json
import re
import runpy
from PIL import Image as PILImage
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Image, Flowable

ROOT = Path(__file__).resolve().parent.parent
OUTPUT = ROOT / 'output/pdf/TripSync_dokumentacja_projektu.pdf'
WIDTH = 499
NAVY = colors.HexColor('#122E38')
TEAL = colors.HexColor('#006B62')
MINT = colors.HexColor('#C4E8C9')
GRAY = colors.HexColor('#54645E')
PALE = colors.HexColor('#F2F6F0')
DATE = '05.10.2026'
REPOSITORY = 'https://github.com/Yamoteusz/TripSync'

for name, candidates in [('TS', ['/System/Library/Fonts/Supplemental/Arial.ttf', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf']), ('TSB', ['/System/Library/Fonts/Supplemental/Arial Bold.ttf', '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'])]:
    font = next((path for path in candidates if Path(path).exists()), None)
    if not font:
        raise RuntimeError('Zainstaluj Arial lub DejaVu Sans, aby zachować polskie znaki.')
    pdfmetrics.registerFont(TTFont(name, font))
pdfmetrics.registerFontFamily('TS', normal='TS', bold='TSB')

styles = {
    'body': ParagraphStyle('body', fontName='TS', fontSize=9.5, leading=13.2, textColor=GRAY, spaceAfter=7),
    'small': ParagraphStyle('small', fontName='TS', fontSize=8.4, leading=11.5, textColor=GRAY, spaceAfter=6),
    'h1': ParagraphStyle('h1', fontName='TSB', fontSize=22, leading=27, textColor=NAVY, spaceAfter=15, keepWithNext=True),
    'h2': ParagraphStyle('h2', fontName='TSB', fontSize=12.5, leading=17, textColor=NAVY, spaceBefore=10, spaceAfter=6, keepWithNext=True),
    'h3': ParagraphStyle('h3', fontName='TSB', fontSize=10.5, leading=15, textColor=NAVY, spaceBefore=8, spaceAfter=6, keepWithNext=True),
    'cell': ParagraphStyle('cell', fontName='TS', fontSize=8.2, leading=11, textColor=GRAY, splitLongWords=True),
    'head': ParagraphStyle('head', fontName='TSB', fontSize=8.5, leading=11.5, textColor=colors.white),
    'code': ParagraphStyle('code', fontName='TS', fontSize=8.1, leading=11.5, textColor=TEAL, backColor=PALE, borderPadding=7, spaceBefore=5, spaceAfter=12, splitLongWords=True),
    'caption': ParagraphStyle('caption', fontName='TSB', fontSize=9, leading=13, textColor=NAVY, alignment=1, spaceBefore=5),
    'bullet': ParagraphStyle('bullet', fontName='TS', fontSize=9.5, leading=13.2, textColor=GRAY, leftIndent=14, firstLineIndent=-11, spaceAfter=5),
}


def markup(text, source=None):
    tokens = []

    def protect(value):
        tokens.append(value)
        return f'ZZTOKEN{len(tokens) - 1}ZZ'

    def link(match):
        label, target = match.groups()
        if not target.startswith(('https://', 'http://', '#')) and source:
            path = (source.parent / target.split('#')[0]).resolve()
            target = REPOSITORY + '/blob/main/' + path.relative_to(ROOT).as_posix()
        return protect(f'<a href="{escape(target, quote=True)}" color="#006B62">{escape(label)}</a>')

    text = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', link, text)
    text = re.sub(r'`([^`]+)`', lambda m: protect(f'<b>{escape(m.group(1))}</b>'), text)
    text = escape(text)
    text = re.sub(r'\*\*(.+?)\*\*', r'<b>\1</b>', text)
    for i, token in enumerate(tokens):
        text = text.replace(f'ZZTOKEN{i}ZZ', token)
    return text


def paragraph(text, kind='body', source=None):
    return Paragraph(markup(text, source), styles[kind])


def table(rows, source=None, widths=None):
    columns = len(rows[0])
    if widths is None:
        if columns == 2:
            widths = [155, WIDTH - 155]
        elif columns == 3 and rows[0][0] == 'ID':
            widths = [39, 195, WIDTH - 234]
        elif columns == 3:
            widths = [126, 165, WIDTH - 291]
        elif columns == 4:
            widths = [172, 58, 133, WIDTH - 363]
        else:
            widths = [WIDTH / columns] * columns
    cells = [[Paragraph(markup(cell, source), styles['head' if i == 0 else 'cell']) for cell in row] for i, row in enumerate(rows)]
    result = Table(cells, colWidths=widths, repeatRows=1, hAlign='LEFT')
    result.setStyle(TableStyle([
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('LEFTPADDING', (0, 0), (-1, -1), 7), ('RIGHTPADDING', (0, 0), (-1, -1), 7),
        ('TOPPADDING', (0, 0), (-1, -1), 5), ('BOTTOMPADDING', (0, 0), (-1, -1), 5),
        ('BACKGROUND', (0, 0), (-1, 0), NAVY),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, PALE]),
        ('LINEBELOW', (0, 0), (-1, -1), .35, colors.HexColor('#DAE4DC')),
    ]))
    return result


class SvgDiagram(Flowable):
    def __init__(self, path, width=WIDTH):
        super().__init__()
        self.svg = ElementTree.parse(path).getroot()
        self.source_width = float(self.svg.attrib['width'])
        self.source_height = float(self.svg.attrib['height'])
        self.width = width
        self.height = self.source_height * width / self.source_width

    def draw(self):
        canvas = self.canv
        canvas.saveState()
        canvas.scale(self.width / self.source_width, self.width / self.source_width)
        height = self.source_height
        for element in self.svg:
            tag = element.tag.split('}')[-1]
            attr = element.attrib
            fill, stroke = attr.get('fill', 'none'), attr.get('stroke', 'none')
            canvas.setFillColor(colors.HexColor(fill) if fill != 'none' else colors.white)
            canvas.setStrokeColor(colors.HexColor(stroke) if stroke != 'none' else colors.white)
            canvas.setLineWidth(float(attr.get('stroke-width', 1)))
            canvas.setDash([float(value) for value in attr.get('stroke-dasharray', '').split()])
            filled, stroked = int(fill != 'none'), int(stroke != 'none')
            number = lambda key, default=0: float(attr.get(key, default))
            if tag == 'rect':
                canvas.roundRect(number('x'), height - number('y') - number('height'), number('width'), number('height'), number('rx'), fill=filled, stroke=stroked)
            elif tag == 'line':
                canvas.line(number('x1'), height - number('y1'), number('x2'), height - number('y2'))
            elif tag == 'circle':
                canvas.circle(number('cx'), height - number('cy'), number('r'), fill=filled, stroke=stroked)
            elif tag == 'ellipse':
                x, y, rx, ry = number('cx'), height - number('cy'), number('rx'), number('ry')
                canvas.ellipse(x - rx, y - ry, x + rx, y + ry, fill=filled, stroke=stroked)
            elif tag == 'polygon':
                points = [tuple(map(float, pair.split(','))) for pair in attr['points'].split()]
                path = canvas.beginPath()
                path.moveTo(points[0][0], height - points[0][1])
                for x, y in points[1:]:
                    path.lineTo(x, height - y)
                path.close()
                canvas.drawPath(path, fill=filled, stroke=stroked)
            elif tag == 'text':
                canvas.setFont('TSB' if attr.get('font-weight') == '700' else 'TS', number('font-size', 16))
                text = ''.join(element.itertext())
                draw = {'middle': canvas.drawCentredString, 'end': canvas.drawRightString}.get(attr.get('text-anchor'), canvas.drawString)
                draw(number('x'), height - number('y'), text)
        canvas.restoreState()


class Logo(Flowable):
    def __init__(self):
        super().__init__()
        self.width, self.height = WIDTH, 112

    def draw(self):
        canvas = self.canv
        canvas.setFillColor(NAVY)
        canvas.roundRect(0, 0, WIDTH, 112, 18, fill=1, stroke=0)
        canvas.setStrokeColor(MINT)
        canvas.setFillColor(MINT)
        canvas.setLineWidth(3.5)
        points = [(30, 43), (51, 77), (74, 39), (100, 74)]
        path = canvas.beginPath()
        path.moveTo(*points[0])
        for point in points[1:]:
            path.lineTo(*point)
        canvas.drawPath(path)
        for x, y in points[:3]:
            canvas.circle(x, y, 4, stroke=0, fill=1)
        canvas.line(88, 74, 100, 74)
        canvas.line(100, 74, 100, 62)
        canvas.setFillColor(colors.white)
        canvas.setFont('TSB', 33)
        canvas.drawString(132, 55, 'TripSync')
        canvas.setFont('TS', 11)
        canvas.drawString(134, 31, 'Wspólny wyjazd. Jeden plan.')


class Documentation(SimpleDocTemplate):
    def afterFlowable(self, flowable):
        if isinstance(flowable, Paragraph) and flowable.style.name == 'h1':
            title = flowable.getPlainText()
            key = getattr(flowable, 'bookmark', 'page-' + str(self.page))
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(title, key, 0, False)


def frame(canvas, document):
    canvas.saveState()
    canvas.setStrokeColor(colors.HexColor('#DAE4DC'))
    canvas.line(48, 43, 547, 43)
    canvas.setFillColor(GRAY)
    canvas.setFont('TS', 8)
    canvas.drawString(48, 29, 'TripSync 1.2.0 | Dokumentacja projektu Android | ' + DATE)
    canvas.drawRightString(547, 29, str(document.page))
    canvas.setFillColor(TEAL)
    canvas.setFont('TSB', 8)
    canvas.drawString(48, 813, 'TRIPSYNC / PROJEKT ZESPOŁOWY')
    canvas.restoreState()


def markdown_section(filename, diagrams=()):
    source = ROOT / filename
    lines = source.read_text().splitlines()
    items = []
    diagram_iter = iter(diagrams)
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1
            continue
        if line.startswith('# '):
            items.append(paragraph(line[2:], 'h1', source))
        elif line.startswith('### '):
            items.append(paragraph(line[4:], 'h3', source))
        elif line.startswith('## '):
            if filename == 'docs/funkcje.md' and 'Zrzuty ekranu' in line:
                break
            items.append(paragraph(line[3:], 'h2', source))
        elif line.startswith('```'):
            kind = line[3:]
            code = []
            i += 1
            while i < len(lines) and not lines[i].strip().startswith('```'):
                code.append(lines[i])
                i += 1
            if kind == 'mermaid':
                diagram = next(diagram_iter, None)
                if diagram:
                    items.extend([SvgDiagram(ROOT / 'docs/diagrams' / diagram), Spacer(1, 9)])
            else:
                items.append(Paragraph('<br/>'.join(escape(value).replace(' ', '&#160;') for value in code), styles['code']))
        elif line.startswith('!['):
            target = re.search(r'\]\(([^)]+)\)', line).group(1)
            path = source.parent / target
            if path.suffix == '.svg':
                items.extend([SvgDiagram(path), Spacer(1, 9)])
        elif line.startswith('|'):
            rows = []
            while i < len(lines) and lines[i].strip().startswith('|'):
                row = [cell.strip() for cell in lines[i].strip().strip('|').split('|')]
                if not all(re.fullmatch(r':?-+:?', cell.replace(' ', '')) for cell in row):
                    rows.append(row)
                i += 1
            if items and isinstance(items[-1], Paragraph) and items[-1].style.name == 'h2':
                items[-1].style = ParagraphStyle('h2table', parent=styles['h2'], keepWithNext=False)
            items.extend([table(rows, source), Spacer(1, 10)])
            continue
        elif line.startswith('- '):
            items.append(paragraph('• ' + line[2:], 'bullet', source))
        elif re.match(r'^\d+\. ', line):
            items.append(paragraph(line, 'bullet', source))
        else:
            text = [line]
            while i + 1 < len(lines) and lines[i + 1].strip() and not re.match(r'^(#|\||```|[-] |\d+\. |!\[)', lines[i + 1].strip()):
                i += 1
                text.append(lines[i].strip())
            items.append(paragraph(' '.join(text), source=source))
        i += 1
    if filename in ('docs/funkcje.md', 'docs/api.md'):
        for item in items:
            if isinstance(item, Paragraph) and item.style.name in ('body', 'bullet'):
                item.style = ParagraphStyle(item.style.name + '-compact', parent=item.style, fontSize=9.3, leading=12.6, spaceAfter=6)
            elif isinstance(item, Paragraph) and item.style.name == 'h2':
                item.style = ParagraphStyle('h2compact', parent=item.style, leading=16, spaceBefore=8, spaceAfter=5)
    return items


def screenshots():
    batches = [
        [('01-wyjazdy.png', 'Wyjazdy'), ('02-podsumowanie.png', 'Podsumowanie'), ('03-harmonogram.png', 'Harmonogram'), ('04-zadania.png', 'Zadania i pakowanie')],
        [('05-glosowanie.png', 'Głosowanie'), ('06-rozliczenia.png', 'Koszty i rozliczenia'), ('07-formularz.png', 'Formularz wydatku'), ('08-budzet.png', 'Budżet i kategorie')],
        [('09-splata.png', 'Spłata'), ('10-mapa.png', 'Mapa miejsc'), ('11-historia.png', 'Historia zmian'), ('16-ciemny.png', 'Motyw ciemny')],
        [('14-kalendarz.png', 'Wybór daty'), ('15-zegar.png', 'Wybór godziny'), ('17-kalendarz-kompakt.png', 'Kalendarz: mały ekran'), ('18-godzina-kompakt.png', 'Zegar: mały ekran')],
    ]
    items = []
    for number, batch in enumerate(batches, 1):
        items.extend([PageBreak(), paragraph(f'Widoki aplikacji / {number}', 'h1')])
        if number == 1:
            items.append(paragraph('Rzeczywiste zrzuty z testów wersji 1.2.0, 05.10.2026. Dane pochodzą z demo i izolowanego środowiska testowego.', 'small'))
        rows = []
        for offset in (0, 2):
            row = []
            for filename, caption in batch[offset:offset + 2]:
                path = ROOT / 'docs/screenshots-1.2' / filename
                width, height = PILImage.open(path).size
                target_height = 263
                target_width = target_height * width / height
                image = Image(str(path), width=target_width, height=target_height)
                row.append([image, paragraph(caption, 'caption')])
            rows.append(row)
        grid = Table(rows, colWidths=[WIDTH / 2, WIDTH / 2], hAlign='CENTER')
        grid.setStyle(TableStyle([('VALIGN', (0, 0), (-1, -1), 'TOP'), ('ALIGN', (0, 0), (-1, -1), 'CENTER'), ('TOPPADDING', (0, 0), (-1, -1), 4), ('BOTTOMPADDING', (0, 0), (-1, -1), 15)]))
        items.append(grid)
    return items


def main():
    runpy.run_path(str(ROOT / 'scripts/create_diagrams.py'), run_name='__main__')
    results = json.loads((ROOT / 'docs/test-results-1.2.json').read_text())
    sections = [
        ('docs/wymagania.md', ()), ('docs/funkcje.md', ()),
        ('docs/architektura.md', ('architektura.svg',)),
        ('docs/przypadki-uzycia.md', ()),
        ('docs/baza-danych.md', ('baza-lokalna.svg', 'baza-backendu.svg')),
        ('docs/technologie.md', ()), ('docs/api.md', ()),
        ('docs/uruchomienie.md', ()), ('docs/wispbyte.md', ()),
        ('docs/bezpieczenstwo.md', ()), ('docs/testy.md', ()),
        ('docs/zgodnosc.md', ()), ('docs/organizacja-pracy.md', ()),
        ('docs/github-desktop.md', ()), ('docs/prezentacja.md', ()),
        ('docs/ograniczenia.md', ()),
    ]
    story = [Spacer(1, 24), Logo(), Spacer(1, 23), paragraph('Dokumentacja projektu', 'h1'), paragraph('Aplikacja Android do wspólnego planowania i rozliczania wyjazdów.', 'h2'), paragraph('Wersja Android 1.2.0 / kod wersji 3. Backend API 1.1.0. Stan dokumentacji: ' + DATE + '.'), paragraph('Projekt dla zespołu trzyosobowego. W obecnym zakresie wszystkie funkcje są bezpłatne; aplikacja nie realizuje płatności ani subskrypcji.'), table([['Obszar', 'Stan projektu'], ['Platforma', 'Kotlin, Compose / Material 3, MVVM, Android 8.0+'], ['Dane i integracje', 'Room, REST, MySQL lub SQLite, Open-Meteo i OpenStreetMap'], ['Testy z 05.10.2026', f"{results['unique_tests_passed']} różnych testów zaliczonych; lint: 0 błędów, 16 ostrzeżeń"], ['Repozytorium', REPOSITORY]]), Spacer(1, 10), paragraph('Autorzy i zakres oddania', 'h2')]
    author_lines = (ROOT / 'AUTHORS.md').read_text().splitlines()
    author_rows = [[cell.strip() for cell in line.strip('|').split('|')] for line in author_lines if line.startswith('|')]
    author_rows = [row for row in author_rows if not all(re.fullmatch(r'-+', cell) for cell in row)]
    story.append(table(author_rows, widths=[40, 180, WIDTH - 220]))
    story.extend([Spacer(1, 10), paragraph('Dane autorów wymagają uzupełnienia przed oddaniem. Dokument opisuje rzeczywisty zakres oraz ograniczenia; nie stanowi deklaracji uzyskania konkretnej oceny.', 'small'), paragraph('APK debug: output/apk/TripSync-debug.apk. Instrukcje i dokumenty źródłowe znajdują się w repozytorium. SDK 36.1 / target 36: aktualizacja do stabilnego API 37 pozostaje do wykonania.', 'small'), PageBreak(), paragraph('Spis dokumentacji', 'h1')])
    for index, (filename, _) in enumerate(sections, 1):
        title = (ROOT / filename).read_text().splitlines()[0].removeprefix('# ')
        story.append(paragraph(f'{index:02d}. [{title}](#section-{index})'))
    story.extend([paragraph('17. Najważniejsze widoki aplikacji: cztery strony rzeczywistych zrzutów ekranu.'), Spacer(1, 14), paragraph('Jak korzystać z dokumentacji', 'h2'), paragraph('PDF zawiera treść dokumentów źródłowych Markdown. Nawigacja po rozdziałach jest dostępna w zakładkach PDF. Spis treści prowadzi do rozdziałów PDF. Pozostałe odnośniki otwierają pliki w repozytorium; dostęp zależy od jego widoczności i uprawnień.'), paragraph('README na GitHubie stanowi skrócony opis i punkt wejścia. Dokumenty w docs/ opisują wymagania, implementację, instalację, wyniki testów oraz scenariusz obrony. AUTHORS.md służy do uzupełnienia autorów i ich faktycznego wkładu.'), paragraph('Wyniki testów pochodzą z zapisanych raportów wersji 1.2.0. Starsze dokumenty i zrzuty wersji 1.1 pozostają materiałami archiwalnymi; nie są dowodem aktualnego wyglądu aplikacji.', 'small')])
    for index, (filename, diagrams) in enumerate(sections, 1):
        story.append(PageBreak())
        section = markdown_section(filename, diagrams)
        section[0].bookmark = f'section-{index}'
        story.extend(section)
    story.extend(screenshots())
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    document = Documentation(str(OUTPUT), pagesize=A4, leftMargin=48, rightMargin=48, topMargin=57, bottomMargin=57, title='TripSync 1.2.0 - dokumentacja projektu Android', author='Zespół TripSync - autorzy do uzupełnienia', subject='Wymagania, architektura, baza danych, instalacja, testy i prezentacja')
    document.build(story, onFirstPage=frame, onLaterPages=frame)
    print(OUTPUT)


if __name__ == '__main__':
    main()
