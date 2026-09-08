# NYGUS LIVE SIMPLE

Prosty projekt Android: interfejs jest w `app/src/main/assets/index.html`, a właściwy dashboard w `dashboard.html`.

## Jak działa
1. Wklejasz link strony HTML albo zostawiasz puste pole, aby użyć wbudowanego dashboardu.
2. Wybierasz audio: system/strona, mikrofon lub mix.
3. W ustawieniach wklejasz pełny endpoint RTMP/RTMPS razem z kluczem streamu.
4. Naciskasz START LIVE i akceptujesz systemowe okno przechwytywania ekranu.
5. Aplikacja przechodzi na wybraną stronę i wysyła ekran + audio do endpointu.

## Ważne
- Android zawsze wymaga zgody użytkownika na MediaProjection.
- Przechwytywanie audio systemowego działa od Androida 10 i może być blokowane przez aplikację odtwarzającą dźwięk.
- Bez RTMP/RTMPS i klucza streamu TikToka aplikacja nie może sama rozpocząć bezpośredniej transmisji na TikTok.
- Projekt ma minSdk 29 (Android 10).

## Budowa APK
Otwórz folder w Android Studio, poczekaj na synchronizację Gradle, a potem wybierz Build > Build APK(s).
