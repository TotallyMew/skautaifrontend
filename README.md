# Skautų inventoriaus valdymas Android

Android programėlė, parašyta su Jetpack Compose, Hilt ir Retrofit. Programėlė jungiasi prie atskiro backend API, todėl prieš paleidžiant backend serveris turi būti pasiekiamas.

## Apie sistemą

Skautų inventoriaus valdymo sistema skirta tuntų ir jų padalinių inventoriui administruoti telefone. Programėlė leidžia naudotojams dirbti su kasdieniais inventoriaus procesais vienoje vietoje.

Pagrindinės funkcijos:

- inventoriaus peržiūra, kūrimas ir redagavimas pagal naudotojo teises
- daiktų rezervacijos ir grąžinimo eiga
- bendro inventoriaus prašymai
- narių ir organizacinių vienetų peržiūra bei valdymas
- renginių, jų poreikių ir susijusio inventoriaus valdymas
- superadmin prisijungimas ir administravimo ekranai

Programėlė yra pagrindinis vartotojo UI sluoksnis, kuris komunikuoja su backend API, rodo duomenis ir įgyvendina mobilią navigaciją bei ekrano logiką.

## Reikalavimai

- Android Studio
- Android SDK
- Paleistas backend serveris

## Greitas startas

1. Atidarykite projektą Android Studio.
2. Įsitikinkite, kad turite lokalų `local.properties` failą su teisingu `sdk.dir`.
3. Nustatykite backend adresą per `api.baseUrl`, `API_BASE_URL` arba Gradle property.
4. Paleiskite backend serverį.
5. Paleiskite programėlę emuliatoriuje arba fiziniame įrenginyje.

## Programėlės struktūra

Svarbiausios projekto dalys:

- `ui/` - ekranai, navigacija ir Compose UI logika
- `data/remote/` - Retrofit API aprašai ir DTO
- `data/repository/` - repozitorijos darbui su backend
- `di/` - Hilt priklausomybių sukonfigūravimas
- `util/` - bendri pagalbiniai komponentai, įskaitant token valdymą

Programėlė naudoja JWT autentikaciją ir aktyvaus tento kontekstą, kuris saugomas lokaliai ir siunčiamas backend'ui kartu su užklausomis.

## `local.properties`

`local.properties` yra lokalus Android Studio failas:

- jis nėra skirtas commitinti
- jame laikomas jūsų `sdk.dir`
- jame galima laikyti lokalią `api.baseUrl` reikšmę

Pavyzdys:

```properties
sdk.dir=C\:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk
api.baseUrl=http://10.0.2.2:8080/
```

## Kaip nustatomas backend URL

Programėlė nustato `API_BASE_URL` tokia tvarka:

1. Gradle property `API_BASE_URL`
2. Environment variable `API_BASE_URL`
3. `local.properties` reikšmė `api.baseUrl`
4. numatytoji reikšmė `http://10.0.2.2:8080/`

Papildomai palaikomi:

- `API_HOST`
- `API_CERT_PIN`

Dažniausiai paprasčiausia backend adresą nustatyti per `local.properties`.

## Paleidimas su Android emuliatoriumi

Jei backend veikia jūsų kompiuteryje, emuliatoriui rekomenduojama:

```properties
api.baseUrl=http://10.0.2.2:8080/
```

`10.0.2.2` yra specialus adresas, leidžiantis emuliatoriui pasiekti host kompiuterį.

## Paleidimas su fiziniu telefonu

Jei naudojate fizinį įrenginį, `api.baseUrl` turi rodyti į jūsų kompiuterio IP tame pačiame tinkle, pvz.:

```properties
api.baseUrl=http://192.168.1.50:8080/
```

Tokiu atveju:

- backend turi būti pasiekiamas iš lokalaus tinklo
- negalima naudoti `localhost` ar `127.0.0.1`, nes jie rodys į patį telefoną

## Pilnas paleidimo scenarijus

1. Paleiskite backend serverį.
2. Android Studio atidarykite šį projektą.
3. Patikrinkite `sdk.dir` savo `local.properties` faile.
4. Nustatykite `api.baseUrl`.
5. Pasirinkite emuliatorių arba įrenginį.
6. Paleiskite programėlę įprastu Android Studio būdu.

## Superadmin ekranas

Superadmin prisijungimo ekranas atidaromas per deep link:

```text
skautai://superadmin
```

Kad jis veiktų, backend pusėje pirmiausia turi būti sukurtas superadmin vartotojas.

Patogiausias būdas atidaryti ekraną development metu:

```bash
adb shell am start -a android.intent.action.VIEW -d "skautai://superadmin" lt.skautai.android
```

Atsidarius šiam ekranui galima prisijungti su superadmin paskyra ir naudoti administravimo funkcijas.
