# Pénzügyi menedzser alkalmazás telefonra

#### Tartalomjegyzék:

- [Szoftverkövetelmények](#szoftverkövetelmények)
- [A szoftver célja](#a-szoftver-célja)
- [A szoftver funkciói](#a-szoftver-funkciói)
- [Szükséges szoftverkomponensek](#szükséges-szoftverkomponensek)
- [A szoftver működésének műszaki feltételei](#a-szoftver-működésének-műszaki-feltételei)
- [Készítők](#készítők)
- [Thanks](#thanks)
- [Copyright and license](#copyright-and-license)
- [Felhasznált segítségek, vagy inspirációk](#felhasznált-segítségek-vagy-inspirációk)


## Szoftverkövetelmények

##### Funkcionális követelmények

- A szoftver legyen képes egy felhasználót regisztrálni, adatait lekérni az adatbázisból, és azokat megjeleníteni
- Legyen képes az adatokkal különböző műveleteket (összeadás, kivonás, időszakos megjelenítés) végrehajtani
- Az adatokat képes legyen a megfelelő helyre, a megfelelő regisztrált fiókhoz csatolni

#### Nem funkcionális követelmények

- A rendszer képes legyen 1000 egyidejű felhasználót kiszolgálni.
- Az adatok 256 bites titkosítással legyenek védve.
- A válaszidő nem haladhatja meg az 1 másodpercet egy adott művelet esetén.

#### Végfelhasználói követelmények

- A felhasználó képes legyen jelszót visszaállítani e-mail segítségével.
- A rendszer intuitív felhasználói felületet biztosítson érintőképernyőn.

## A szoftver célja

Egy hétköznapi személy pénzének a könnyű, átlátható, és praktikus kezelése. Ezt többféle módon lehet elérni, mely az emberek kényelmét támogatja, hisz sok funkciót tartalmaz magában.

## A szoftver funkciói

#### Főoldal:
1.  Egyenleg összegének a megjelenítése
2. Egyenleghez való hozzáadás
3. Egyenlegből való levonás
4. Többi funkciót tartalmazó ablak elérése egy legördülő menüvel

#### Elemzés

1. Elsődlegesen a kiadások grafikonos megjelenítése
2. Kategorizálja akár a bevételt, de első soron a kiadásokat
3. Ezeket mind kimutatja a szoftverben található módon, nap, hét, hónap, év, vagy akár egy saját magunk által megadott időszakban

#### Rendszeres kifizetések

1. Ez azt a célt szolgálja, hogy a például havi előfizetésekkel mutatja számunkra az egyenlegünk, ezzel is azt elősegítve, hogy mennyi pénzzel számoljunk igazán
2. Rendszeres kifizetés hozzáadása (összeg, név, és kategória megadása)
3. Rendszeres kifizetés eltávolítása, szerkesztése

#### Adósságok

1. Adósságok (név, összeg megadásával) hozzáadása, ez oly módon segít, mint a rendszeres kifizetéseknél, ha van, akkor levonja automatikusan az egyenlegből, ugyanazzal a céllal, mint az előbb említett fülnél
2. Adósságok eltávolítása, szerkesztése

#### Rendszeres bevételek

1. Ha van ilyen, akkor be tudod álítani, hogy hányadikán legkésőbb, de a program automatikusan hozzáadja ezt az összeget az egyenleghez
2. Ezeket a felhasználó képes is lesz eltávolítani, vagy változtatni

#### Általános beállítások

1. Betűméret
2. Nyelv
3. Felhasználói profil beállítások
   - Profil adatok
   - Jelszó változtatás
   - Profil törlése
5. Személyre szabás

#### Kijelentkezés

1. Kijelentkezteti a felhasználót a fiókjából

## Szükséges szoftverkomponensek

1. Fejlesztői környezet és eszközök:\
Android Studio: Az Android hivatalos fejlesztői környezete, amely Kotlin támogatással rendelkezik.\
Kotlin Plugin: Az Android Studio alapértelmezetten támogatja a Kotlin nyelvet, de ha manuálisan kellene hozzáadni, ezt is be kell állítani.\
Gradle: Az Android projektek építési automatizációjához és függőségkezeléséhez szükséges eszköz.
2. Android SDK (Software Development Kit):\
Az Android SDK biztosítja az eszközöket és API-kat az Android alkalmazás fejlesztéséhez.
Tartalmazza az emulátorokat, futási időt, eszközkezelőket, valamint a különböző Android API szinteket (API level-eket).
3. Függőségek és könyvtárak:\
Jetpack komponensek: Ezek megkönnyítik az Android alkalmazások fejlesztését, és ajánlott őket használni.
Navigation: Navigáció kezelés.
ViewModel: Állapotkezelés.
LiveData: Megfigyelhető adatok kezelése.
Room: Lokális adatbázis-kezelő.
Kotlin Coroutines: Könnyű és hatékony párhuzamos futtatás kezelése.
Retrofit vagy OkHttp: Hálózati kommunikációhoz.
Glide vagy Coil: Képek betöltéséhez és gyorsítótárazásához.
4. Android Manifest fájl:\
Az alkalmazás konfigurációs fájlja, amely tartalmazza az alkalmazás metaadatait, engedélyeket és a különböző komponenseket (Activity, Service, BroadcastReceiver).
5. UI komponensek:\
XML Layout fájlok: A felhasználói felület meghatározására szolgálnak.
ViewBinding vagy DataBinding: Megkönnyíti a nézetek és az adatok összekapcsolását.
6. Tesztelési eszközök:\
Beépített szimulált Android eszköz
7. Build-folyamat eszközök:\
Firebase App Distribution: Az alkalmazás terjesztéséhez és béta teszteléséhez.
8. Elemző és monitorozó eszközök:\
Firebase Analytics: Felhasználói tevékenység elemzése.
9. Verziókezelő rendszer:\
Git: Verziókövetéshez és csapatmunkához (GitHub)


## A szoftver működésének műszaki feltételei

- Modern mobiltelefon
- Internetelérés
- nem tom hany MB, meg kiderul pontosan, majd szepen updatelve lesz


## Készítők

- <https://github.com/orosssz>

- <https://github.com/plebentee>

- <https://github.com/cigranyxd>

## Köszönet


## Licencek


## Felhasznált segítségek, vagy inspirációk

- A dokumentációhoz használt minta: [https://github.com/Ismaestro/markdown-template/blob/master/README.md?plain=1](https://github.com/Ismaestro/markdown-template/blob/master/README.md?plain=1)
- A ChatGPT által írt kód tettszésünkre, és megfelelésünkre való átírása
- Más hasonló alkalmazások inspirációnak való felhasználása GitHubon
