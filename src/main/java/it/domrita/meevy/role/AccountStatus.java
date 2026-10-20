package it.domrita.meevy.role;

/*
 * Stato globale dell'account.
 *
 * È distinto dalle eventuali espulsioni da un singolo gruppo:
 * SUSPENDED e BANNED riguardano l'intera piattaforma.
 */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    BANNED
}