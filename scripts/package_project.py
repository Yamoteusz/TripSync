from pathlib import Path
import hashlib
import zipfile

ROOT = Path(__file__).resolve().parent.parent
PDF = ROOT / 'output/pdf/TripSync_dokumentacja_do_oddania.pdf'
APK = ROOT / 'output/apk/TripSync-debug.apk'
PACKAGE = ROOT / 'output/TripSync_do_oddania.zip'


def package_submission():
    artifacts = [(PDF, 'TripSync_Dokumentacja.pdf'), (APK, 'TripSync.apk')]
    for source, _ in artifacts:
        if not source.is_file():
            raise RuntimeError(f'Brak pliku: {source}. Najpierw wygeneruj PDF i zbuduj APK.')
    if not PDF.read_bytes().startswith(b'%PDF-'):
        raise RuntimeError('Nieprawidłowy dokument PDF.')
    with zipfile.ZipFile(APK) as application:
        if application.testzip() or not {'AndroidManifest.xml', 'classes.dex'}.issubset(application.namelist()):
            raise RuntimeError('APK jest uszkodzony lub niekompletny.')
    PACKAGE.parent.mkdir(parents=True, exist_ok=True)
    temporary = PACKAGE.with_suffix('.zip.tmp')
    try:
        with zipfile.ZipFile(temporary, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
            for source, name in artifacts:
                archive.write(source, name)
        with zipfile.ZipFile(temporary) as archive:
            if archive.testzip() or archive.namelist() != [name for _, name in artifacts]:
                raise RuntimeError('Nieprawidłowa zawartość paczki.')
            for source, name in artifacts:
                if hashlib.sha256(source.read_bytes()).digest() != hashlib.sha256(archive.read(name)).digest():
                    raise RuntimeError(f'Nieprawidłowa kopia pliku: {name}')
        temporary.replace(PACKAGE)
    finally:
        temporary.unlink(missing_ok=True)
    return PACKAGE


if __name__ == '__main__':
    print(package_submission())
