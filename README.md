# Skautu inventoriaus valdymas Android

Android programele, parasyta su Jetpack Compose, Hilt, Retrofit, Room ir WorkManager. Ji jungiasi prie atskiro backend API, todel pries paleidziant programele backend serveris turi buti pasiekiamas is emuliatoriaus arba fizinio irenginio.

## Apie sistema

Skautu inventoriaus valdymo sistema skirta tuntams ir ju padaliniams administruoti inventoriu telefone. Programele leidzia naudotojams dirbti su kasdieniais inventoriaus, rezervaciju, pirkimu, nariu ir renginiu procesais vienoje vietoje.

Pagrindines funkcijos:

- prisijungimas, registracija, registracija pagal kvietima ir tunto pasirinkimas
- pagrindinis ekranas su santrauka, greitais veiksmais ir vartotojo uzduotimis
- inventoriaus perziura, filtravimas, kurimas, redagavimas, QR skenavimas, CSV importas / eksportas, rinkiniai ir inventorizacijos patikros
- bendro tunto sandelio, vieneto inventoriaus ir asmeninio inventoriaus atskyrimas pagal `custodianId`
- daikto kilmes rodymas pagal `origin`, iskaitant is bendro sandelio perduota inventoriu
- rezervacijos, isdavimas, grazinimas ir tvirtinimo eiga
- pirkimo / papildymo prasymai ir bendro inventoriaus paemimo prasymai
- nariu, kvietimu, pareigu, rangu ir organizaciniu vienetu perziura bei valdymas pagal teises
- lokaciju medis ir daiktu priskyrimas lokacijoms
- renginiai, poreikiai, inventoriaus planas, pirkimai, pastovykles, inventoriaus judejimai ir suderinimas
- kalendorius, profilis, sinchronizavimo busena ir offline/cache palaikymas
- superadmin prisijungimas ir tuntams skirti administravimo ekranai

## Reikalavimai

- Android Studio
- Android SDK
- Paleistas backend serveris
- JDK / Gradle aplinka, kuria naudoja Android Studio

Projektas naudoja `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.

## Greitas startas

1. Paleiskite backend serveri.
2. Atidarykite `skautu-inventoriaus-valdymas-android` Android Studio.
3. Isitikinkite, kad turite lokalu `local.properties` faila su teisingu `sdk.dir`.
4. Nustatykite backend adresa per `api.baseUrl`, `API_BASE_URL` arba Gradle property.
5. Pasirinkite emuliatoriu arba fizini irengini.
6. Paleiskite programele iprastu Android Studio budu.

## Programeles struktura

Svarbiausios projekto dalys:

- `app/src/main/java/lt/skautai/android/MainActivity.kt` - startinis activity, sesijos patikra ir deep link apdorojimas
- `app/src/main/java/lt/skautai/android/SkautuInventoriusApp.kt` - Hilt application klase
- `ui/common/AppNavGraph.kt` - pagrindinis nav graph
- `ui/common/MainScaffold.kt` - autentifikuotos programeles shell, drawer, bottom nav, sync indikatoriai
- `ui/auth/` - login, register ir register invite ekranai
- `ui/home/` - pagrindinis santraukos ekranas
- `ui/inventory/` - inventorius, detalus vaizdas, add/edit, QR, rinkiniai, inventorizacija
- `ui/reservations/` - rezervaciju sarasas, kurimas, detales, isdavimas/grazinimas
- `ui/requests/` - pirkimo / papildymo ir bendro inventoriaus prasymai
- `ui/events/` - renginiai, poreikiai, pirkimai, planas, pastovykles, judejimai ir suderinimas
- `ui/members/`, `ui/units/`, `ui/locations/` - nariai, organizaciniai vienetai ir lokacijos
- `ui/tasks/`, `ui/calendar/`, `ui/profile/`, `ui/tuntas/`, `ui/superadmin/` - papildomi pagrindiniai moduliai
- `data/remote/` - Retrofit API aprasai ir DTO
- `data/repository/` - repozitorijos, kurios apjungia API, lokalu cache ir Result grazinima
- `data/local/` - Room entity, DAO, mappers ir lokalus cache
- `data/sync/` - pending operacijos ir WorkManager sinchronizavimas
- `di/` - Hilt moduliai: network, repository, database ir sync
- `util/TokenManager.kt` - DataStore JWT, aktyvaus `tuntasId` ir aktyvaus vieneto kontekstas

Navigacija aprasyta `NavRoutes` sealed klaseje. ViewModel'iai dazniausiai expose'ina `StateFlow` su UI busena, o ekranai renka busena per lifecycle-aware Compose kolektorius.

## Backend URL konfiguracija

`local.properties` yra lokalus Android Studio failas:

- jis nera skirtas commitinti
- jame laikomas `sdk.dir`
- jame galima laikyti lokalia `api.baseUrl`, `api.host` ir `api.certPin` reiksmes

Pavyzdys:

```properties
sdk.dir=C\:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk
api.baseUrl=http://10.0.2.2:8080/
api.host=
api.certPin=
```

Programa nustato `API_BASE_URL` tokia tvarka:

1. Gradle property `API_BASE_URL`
2. Environment variable `API_BASE_URL`
3. `local.properties` reiksme `api.baseUrl`
4. numatytoji reiksme `http://10.0.2.2:8080/`

Papildomai palaikomi:

- `API_HOST` / `api.host`
- `API_CERT_PIN` / `api.certPin`

Dazniausiai paprasciausia backend adresa nustatyti per `local.properties`.

## Paleidimas su Android emuliatoriumi

Jei backend veikia tame paciame kompiuteryje, emuliatoriui rekomenduojama:

```properties
api.baseUrl=http://10.0.2.2:8080/
```

`10.0.2.2` yra specialus adresas, leidziantis emuliatoriui pasiekti host kompiuteri.

## Paleidimas su fiziniu telefonu

Jei naudojate fizini irengini, `api.baseUrl` turi rodyti i kompiuterio IP tame paciame tinkle, pvz.:

```properties
api.baseUrl=http://192.168.1.50:8080/
```

Tokiu atveju:

- backend turi buti pasiekiamas is lokalaus tinklo
- negalima naudoti `localhost` ar `127.0.0.1`, nes jie rodys i pati telefona
- debug build'e cleartext HTTP leidziamas, release build'e `usesCleartextTraffic` yra `false`

## Auth, tuntas ir API header'iai

`TokenManager` saugo JWT, aktyvu `tuntasId` ir aktyvu organizacini vieneta DataStore. Repozitorijos paima token'a ir perduoda ji kaip Bearer autentikacija. Dauguma tuntu scoped uzklausu taip pat siuncia `X-Tuntas-Id`, nes backend leidimai priklauso nuo aktyvaus tunto ir vartotojo role/scope.

## Offline cache ir sync

Aplikacija turi lokalu Room cache pagrindiniams resursams ir pending operaciju mechanizma. Kai veiksmai atliekami be stabilaus rysio arba turi buti pakartoti, jie saugomi kaip pending operacijos ir veliau sinchronizuojami per WorkManager.

Svarbios dalys:

- `data/local/` - lokalios lenteles, DAO ir mapperiai
- `data/sync/PendingOperationRepository.kt` - pending operaciju vykdymas
- `data/sync/PendingSyncScheduler.kt` - WorkManager sinchronizavimo planavimas
- `ui/common/PendingSyncViewModel.kt` ir `NavRoutes.SyncStatus` - sync busenos UI

## Aktualus DTO / API terminai

- `ItemDto` naudoja `custodianId`, `custodianName` ir `origin`; senu `ownerType` / `ownerId` lauku nebenaudojama.
- `BendrasRequestDto`, `CreateBendrasRequestDto`, `ReservationDto` ir `CreateReservationRequestDto` naudoja `requestingUnitId`.
- `OrganizationalUnitDto` turi `subtype`.
- Inventoriaus saraso marsrutai filtruoja pagal `custodianId` ir `sharedOnly`, ne pagal sena `ownerType`.
- Backend bendra tunto saugykla atpazistama pagal `custodianId == null`.

## Naudingos Gradle uzduotys

Android build ir paleidimas iprastai atliekami per Android Studio. Jei reikia patikrinti is CLI:

```powershell
.\gradlew.bat assembleDebug --console=plain
.\gradlew.bat testDebugUnitTest --console=plain
.\gradlew.bat compileDebugKotlin --console=plain
```

## Superadmin ekranas

Superadmin prisijungimo ekranas atidaromas per deep link:

```text
skautai://superadmin
```

Kad jis veiktu, backend puseje pirmiausia turi buti sukurtas superadmin vartotojas.

Patogiausias budas atidaryti ekrana development metu:

```bash
adb shell am start -a android.intent.action.VIEW -d "skautai://superadmin" lt.skautai.android
```

Atsidarius siam ekranui galima prisijungti su superadmin paskyra ir naudoti administravimo funkcijas.
