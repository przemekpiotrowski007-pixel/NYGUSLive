# NYGUS LIVE — automatyczny APK na GitHubie

## Najprościej

1. Załóż nowe repozytorium na GitHubie.
2. Rozpakuj tę paczkę i wrzuć **całą zawartość** do repozytorium, razem z folderem `.github`.
3. Wejdź w zakładkę **Actions**.
4. Otwórz workflow **Build NYGUS APK**.
5. Kliknij **Run workflow**.
6. Po zakończeniu otwórz wykonany workflow i na dole w **Artifacts** pobierz **NYGUSLive-APK**.
7. W ZIP-ie z artifactem będzie plik `NYGUSLive.apk`.

Workflow uruchomi się też automatycznie po każdym pushu na branch `main` lub `master`.

## Co robi aplikacja

- otwiera wbudowany dashboard albo podany link HTML,
- prosi Androida o zgodę na przechwytywanie ekranu,
- pozwala wybrać audio telefonu, mikrofon albo mix,
- wysyła stream na podany endpoint `rtmp://` lub `rtmps://`.

Do transmisji na TikTok potrzebny jest endpoint/klucz streamu udostępniony dla danego konta TikTok.
