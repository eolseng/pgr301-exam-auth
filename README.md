## Oppsett for pushing til Travis-CI
For at Travis-CI skal kunne bygge og publisere et Docker Image til Google Container Registry trengs følgende:
1. Lag en `Service Account` for Travis-CI
    * Gi kontoen rollen `Storage Admin` - dette lar Travis pushe images til Container Registry
    * Eksporter en .json-nøkkel for kontoen
    * Navngi nøkkelen `gcp_keyfile.json` og plasser den i prosjektet rot-mappe
2. Påse at prosjektet har `Container Registry` aktivert
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

## Simulert aktivitet
Programmet simulerer aktivitet for å gi data til logging og metrics. Denne aktiviteten kan deaktiveres ved å kommentere ut innholdet i eller slette filen `no/eolseng/pgr301examauth/SimulateActivity.kt`.