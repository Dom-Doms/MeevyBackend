# Come eseguire i test

1. Apri il terminale di IntelliJ nella cartella `MeevyBackend`.
2. Assicurati che PostgreSQL sia avviato. Per il test completo, le variabili del database e la chiave JWT devono essere disponibili **anche a Maven** (quelle della Run Configuration dell'app non passano automaticamente al terminale).
```powershell
   $env:DB_USERNAME = "incollalo qui"
   $env:DB_PASSWORD = "incollalo qui"
   $env:MEEVY_JWT_SECRET_BASE64 = "incollalo qui"
   ```
3. Esegui:

   ```powershell
   mvn clean verify
   ```

4. Se compare `BUILD SUCCESS` e i test hanno `Failures: 0, Errors: 0`, apri il report JaCoCo:

   ```powershell
   Start-Process ".\target\site\jacoco\index.html"
   ```

5. Nella riga **Total**, per calcolare la copertura delle linee usa:

   `Line Coverage = (Lines - Missed Lines) / Lines × 100`

**Nota:** per provare solo i test unitari senza avviare il test completo di Spring:

```powershell
mvn "-Dtest=AuthServiceTest,UserServiceTest,TokenServiceTest" clean verify
```

Il coverage di questo secondo comando riguarda soltanto i test selezionati.
