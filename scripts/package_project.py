from pathlib import Path
import hashlib
import subprocess
import zipfile

root = Path(__file__).resolve().parent.parent
output = root / 'output'
apk = output / 'apk/TripSync-debug.apk'
pdf = output / 'pdf/TripSync_dokumentacja_projektu.pdf'
for artifact in [apk, pdf, root / 'docs/test-results.json']:
    if not artifact.is_file():
        raise RuntimeError(f'Brak wymaganego pliku: {artifact}')
bundle = output / 'TripSync-history.bundle'
subprocess.run(['git', 'bundle', 'create', str(bundle), 'main', 'HEAD'], cwd=root, check=True)
subprocess.run(['git', 'bundle', 'verify', str(bundle)], cwd=root, check=True)
tracked = subprocess.check_output(['git', 'ls-files', '-z'], cwd=root).decode().split('\0')
package = output / 'TripSync_projekt.zip'
with zipfile.ZipFile(package, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    for name in tracked:
        if name and (root / name).is_file():
            archive.write(root / name, 'TripSync/' + name)
    archive.write(apk, 'TripSync/output/apk/TripSync-debug.apk')
    archive.write(bundle, 'TripSync/TripSync-history.bundle')
checksums = output / 'checksums.sha256'
checksums.write_text(''.join(hashlib.sha256(path.read_bytes()).hexdigest() + '  ' + path.relative_to(output).as_posix() + '\n' for path in [apk, pdf, bundle, package]))
print(package)
print(checksums)
