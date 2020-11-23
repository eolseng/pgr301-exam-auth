> Eksamen | [PGR301 - DevOps i skyen](https://old.kristiania.no/emnebeskrivelse-2-2/?kode=PGR301&arstall=2020&terminkode=H%C3%98ST) | [Oppgave](./docs/PGR301_exam_2020.pdf) | [App](https://github.com/eolseng/pgr301-exam-auth) | [Infrastucture](https://github.com/eolseng/pgr301-exam-infrastructure) | Kandidatnummer: 10004
# DevOps - Applikasjon [![Build Status](https://travis-ci.com/eolseng/pgr301-exam-auth.svg?branch=master)](https://travis-ci.com/eolseng/pgr301-exam-auth) <img src="https://app.statuscake.com/button/index.php?Track=5750635&Days=1&Design=1" height="20" />

Dette repositoriet er infrastruktur-biten av eksamenen min i faget **PGR301 - DevOps i skyen**.
Prosjektet viser prinsipper innen for DevOps med CI/CD via **Travis-CI** med automatisert testing, bygging av **Docker Images** og pushing av disse til et **Docker Registry**.

Applikasjonen var originalt basert på en SpringBoot Auth-applikasjon jeg ønsket å bruke til oppgaven, men så meg nødt til å utvide den grunnet oppgavens omfang.
På bakgrunn av både eksamensperiode og covid-19 har jeg savnet sosialt samvær med venner, og for å få samlet metrikk og logg dedikerte jeg dermed utvidelsen til øl.

* Bygging og pushing av Docker Images til registry gjøres kun på `master`-branchen. Tester kjører på alle.
* I `./docs`-mappen har jeg vedlagt en `Postman Collection` med HTTP-operasjoner mot tjenesten når den kjører lokalt. Dette kan importeres i Postman ved å klikke på "Import" knappen øverst i venstre hjørne.
* 'LogBack' logger kun til konsoll ved kjøring lokalt, men er konfigurert til å logge mot `Logz.io` i produksjonsmiljøet.

## Kjøre programmet lokalt
Applikasjonen bruker `PostreSQL` for database, `InfluxDB` for metrics og `Grafana` for visualisering av metrics.

### Lokalt med simulert aktivitet
Ved start på denne måten kjøres det simuleringer av aktivitet i applikasjonen som produserer metrikk som kan observeres i Grafana.
* Kjør kommandoen `sudo bash ./scripts/start_local`, evt. `DOCKER_BUILDKIT=1 COMPOSE_DOCKER_CLI_BUILD=1 docker-compose -f docker-compose.yml up --build`, for å starte programmet med alle tilknyttede tjenester.
* Det benyttes eksperimentelle funksjoner i Docker for å cache `.m2/`-mappen ved bygging, slik at man raskt får bygd imaget ved små endringer. Dette krever Docker vesjon 18.09 eller høyere.
* Kan kjøres uten eksperimentelle funksjoner ved å endre `auth-app.build.dockerfile` fra `experimental.Dockerfile` til bare `Dockerfile`.
* Kan skru av simulert aktivitet ved å fjerne "simulation" under `auth-app.environment.SPRING_PROFILES_ACTIVE` i docker-compose filen.

### Utvikling / dev
* Kjør kommandoen `sudo bash ./scripts/start_dev_services`, evt. `docker-compose -f docker-compose-dev.yml up` for å starte alle nødvendige tjenester.
* Start så `src/test/kotlin/no/eolseng/pgr301examauth/LocalApplicationRunner.kt` for å starte programmet med `dev` profil som skrur på loggføring mot InfluxDB og kobler seg på PostgreSQL.

## Oppsett for pushing til Travis-CI
For at Travis-CI skal kunne bygge og publisere et Docker Image til Google Container Registry trengs følgende:
1. Lag en `Service Account` for Travis-CI
    * Gi kontoen rollen `Storage Admin` - dette lar Travis pushe images til Container Registry
    * Eksporter en .json-nøkkel for kontoen
    * Navngi nøkkelen `gcp_keyfile.json` og plasser den i prosjektet rot-mappe
2. Påse at GCP prosjektet har `Container Registry` aktivert
3. Krypter `gcp_keyfile.json` og variabler for GCP Prosjekt ID og Container Registry sitt hostname med følgende kommandoer:
    ```
   travis encypt-file --pro gcp_keyfile.json --add
   travis encrypt --pro GCP_PROJECT_ID="[GCP Prosjektets ID]" --add
   travis encrypt --pro GCP_REGISTRY_HOSTNAME="[GCP Container Registry sitt hostnavn]" --add
    ```
   NB.: Krever pålogget bruker på Travis-CI sin CLI
   
   `GCP Container Registry` har følgende mulige hostnavn: `gcr.io`, `us.gcr.io`, `eu.gcr.io`, eller `asia.gcr.io`
   
4. Prosjektet er nå konfigurert slik at et push til `master` branchen vil bygge et nytt Docker Image, tagge dette som `latest` og `GIT_COMMIT_ID` og pushe dette til Google Cloud Registry. 

## Metrics
Progammet bruker `Micrometer` til å samle metrikk som lagres i `InfluxDB`. Disse kan enkelt visualiseres i `Grafana` ved å importere innholdet i `grafana/dashboard.json`.

![Grafana Dashboard](./docs/Beercentage%20Grafana%20Dashboard.png)
### Oppsett av Grafana
1. Naviger til `http://localhost:3000`
2. Logg inn med `Username: admin | Password: admin`
3. Trykk på "Add your first data source" og velg "InfluxDB"
4. Sett URL til `http://influxdb:8086`
5. Sett "Database" til `metrics`, "User" til `user` og "Password" til `user`
6. Trykk "Save & Test"
7. Naviger til `http://localhost:3000/dashboard/import`
8. Trykk på "Upload JSON file"
9. Naviger til og velg `grafana_dashboard.json` som ligger i prosjektets `./docs` og trykk "Import"
10. Du ser nå et ferdigkonfigurert dashboard for applikasjonen som fylles med data ved bruk.

### Counter
`Counter` metrikk blir brukt til å samle statistikk på antall "sipper" som er blitt forsøkt og utfallet av disse.

De forskjellige `result`ene er:
* `NONEXSISTENT_MUG` - glasset som bruker forsøker å drikke fra eksisterer ikke
* `NOT_OWNER` - glasset som bruker forsøker å drikke fra tilhører noen andre
* `EMPTY_MUG` - glasset som bruker forsøker å drikke fra er tomt
* `SUCCESS` - bruker fikk tatt seg en slurk øl

Counteren opprettes i `no/eolseng/pgr301examauth/beer/SipController.kt` under `SipService` sin `sipBeer()` metode.
Data lagres i tabellen `beer_sips_count` og tagges med `result` for å måle utfallet av forsøket.

### Distribution Summary
`DistributionSummary` brukes til å måle størrelsen av påfyllinger i glass.

Dette tillater målinger som:
* Gjennomsnittlig størrelse av påfyll
* Total mengde påfyll over en gitt periode
* Antall påfyllninger i en gitt periode

DistributionSummaryet opprettes i `no/eolseng/pgr301examauth/beer/TapController.kt` under `TapService` sin `tapBeer()` metode.
Data lagres i tabellen `beer.taps.volume`.

### Gauge
`Gauge` brukes til å regne ut _**Beercentage**_ - prosentandelen av keg-kapasiteten som er fylt. Dersom denne blir for lav er det på tide å selge øl til kundene.

Gaugen opprettes i `no/eolseng/pgr301examauth/beer/KegController.kt`. Usikker på om dette er den korrekte måten å sette det opp på, men den gir dataen jeg ønsker.
Data lagres i tabellen `beer.kegs.avg`.

### Timer
`Timer` brukes til å loggføre hvor mye tid som brukes på å fylle opp 'kegs'.

Timeren opprettes i `no/eolseng/pgr301examauth/beer/KegController.kt` under `KegService` sin `fillKeg()` metode.
Data lagres i tabellen `beer.kegs.avg`.

### LongTaskTimer
`LongTaskTimer` brukes til å se hvor lang tid påfylling av alle kegs tar. Programmet har en egen @Scheduled aktivitet som fyller opp alle kegs.

LongTaskTimeren opprettes i `no/eolseng/pgr301examauth/beer/BreweryScheduling.kt` under `BreweryScheduling` sin `refillAllKegs()` metode.
Data lagres i tabellen `beer.kegs.fill.all`

## Bugs
* [Grunnet en feil i SpringBoot](https://github.com/spring-projects/spring-boot/issues/24192) skrives det mange loggmeldinger av typen `Skipping unloadable jar file: file:/workspace/XXXjar` ved oppstart av Docker containeren. Dette har ingen påvirkning på selve programmet.
* Ved oppstart vil Gauge-måleren motta en null-verdi som produserer en feilmelding når det ikke eksisterer noen kegs.
