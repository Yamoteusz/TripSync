# GitHub Desktop i aktualizacje zespołu

Repozytorium jest skonfigurowane jako [Yamoteusz/TripSync](https://github.com/Yamoteusz/TripSync), gałąź main. Zachowujemy istniejącą historię; nie trzeba ponownie publikować repozytorium pod inną nazwą.

## Lokalny katalog

W GitHub Desktop wybierz File → Add Local Repository i katalog zawierający README, app, backend i `.git`. Nie dodawaj osobno `app` ani rozpakowanego ZIP-a. Gdy nowe commity już istnieją lokalnie, kliknij **Push origin**. Gdy dopiero zmieniłeś pliki, najpierw wykonaj commit z opisem rzeczywistej zmiany.

## Praca trzech osób

Przed pracą Fetch origin i Pull origin. Przy równoległym rozwijaniu używajcie osobnych gałęzi i pull requestów. Przykład nazwy: `codex/plan-kalendarz`. Włączcie dostęp pozostałych osób oraz prowadzącego, jeśli repozytorium jest prywatne. Strukturę kodu opisuje `organizacja-pracy.md`, a wykonawców wymienia `AUTHORS.md`.

Commit zapisuje stan lokalnie, push publikuje go na GitHubie, a aktualizacja Wispbyte pobiera zdalny kod.

## GitHub Actions

Workflow `.github/workflows/checks.yml` wykonuje testy backendu, kontrolę składni Python, debug APK, testy jednostkowe Android oraz lint. Publikuje artefakty **TripSync-debug**, **TripSync-do-oddania** i **android-reports**. Paczka do oddania łączy wersjonowany PDF z APK zbudowanym w tym przebiegu; wewnętrzny ZIP ma dokładnie dwa pliki. Nie uruchamia testów emulatora i nie wdraża automatycznie backendu. Wyniki workflow i pliki do pobrania są dostępne w zakładce Actions na GitHubie po zakończeniu przebiegu.

## Co nie trafia do historii

Bazy i kopie, `.env`, hasła, tokeny, klucze podpisu, local.properties, cache, pliki budowania i APK. `gradle/gradle-daemon-jvm.properties` pozostaje lokalny. Przykłady env są szablonami bez hasła; nie są automatycznie odczytywane. PDF, screeny i raporty dokumentacyjne są wersjonowane.

## Aktualizacja hostingu

Po Push origin uruchom lub zrestartuj Wispbyte zgodnie z [wispbyte.md](wispbyte.md). Zachowaj domyślny startup uruchamiający main.py oraz konfigurację Environment. Publikacja plików app nie instaluje nowego Androida na telefonie: aplikację aktualizuje się przez Run albo APK.
