package it.domrita.meevy.auth.repository;

 /*
         * Attualmente non abbiamo implementato JWT, quindi
 * utilizzeremo un'implementazione temporanea che restituisce
        * l'ID di un utente di prova.
        *
        * In futuro creeremo un'implementazione che recupererà
        * l'ID dell'utente dal token JWT verificato da Spring Security.
        *
        * In questo modo potremo cambiare il sistema di identificazione
 * senza modificare la logica del SubjectService.
        */
public interface CurrentUserIdProvider {
     /*
      * Restituisce l'ID dell'utente corrente.
      * Il modo in cui viene recuperato dipende
      * dall'implementazione utilizzata.
      */
    Long getCurrentUserId();
}
