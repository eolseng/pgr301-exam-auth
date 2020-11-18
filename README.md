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
