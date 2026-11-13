
package it.domrita.meevy.group.model;

import it.domrita.meevy.user.model.User;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class GroupInvitationTest {

    private final Instant createdAt =
            Instant.parse("2026-11-12T10:00:00Z");

    private final Instant expiresAt =
            createdAt.plusSeconds(86400);


    @Test //Creazione corretta dell'invito
    void createInvitation_Test(){

        StudyGroup group = new StudyGroup(
                "Basi di Dati",
                "Preparazione esame"
        );

        User user = new User(
                "utente@example.com",
                "passwordHash",
                "Domenico",
                "Test"
        );

        GroupInvitation invitation = new GroupInvitation(
                group,
                user,
                "hashDiProva",
                createdAt,
                expiresAt
        );

        assertNull(invitation.getId());
        assertSame(group, invitation.getGroup());
        assertSame(user, invitation.getCreatedBy());
        assertEquals("hashDiProva", invitation.getTokenHash());
        assertEquals(createdAt, invitation.getCreatedAt());
        assertEquals(expiresAt, invitation.getExpiresAt());

        assertFalse(invitation.isUsed());
        assertNull(invitation.getUsedAt());
        assertNull(invitation.getUsedBy());
    }


    @Test //Invito non ancora scaduto
    void isExpired_ValidInvitation(){

        GroupInvitation invitation = createInvitation();

        assertFalse(invitation.isExpired(createdAt));
        assertFalse(invitation.isExpired(expiresAt.minusSeconds(1)));
    }


    @Test //Invito scaduto
    void isExpired_ExpiredInvitation(){

        GroupInvitation invitation = createInvitation();

        assertTrue(invitation.isExpired(expiresAt.plusSeconds(1)));
    }


    @Test //L'invito scade esattamente alla data di scadenza
    void isExpired_ExactExpiration(){

        GroupInvitation invitation = createInvitation();

        assertTrue(invitation.isExpired(expiresAt));
    }


    @Test //Utilizzo corretto dell'invito
    void markAsUsed_Test(){

        GroupInvitation invitation = createInvitation();

        User member = new User(
                "membro@example.com",
                "passwordHash",
                "Rita",
                "Test"
        );

        Instant now = createdAt.plusSeconds(60);

        invitation.markAsUsed(member, now);

        assertTrue(invitation.isUsed());
        assertSame(member, invitation.getUsedBy());
        assertEquals(now, invitation.getUsedAt());

        assertEquals(createdAt, invitation.getCreatedAt());
        assertEquals(expiresAt, invitation.getExpiresAt());
    }


    @Test //Tentativo di utilizzare due volte lo stesso invito
    void markAsUsed_AlreadyUsed(){

        GroupInvitation invitation = createInvitation();

        User member1 = new User(
                "membro1@example.com",
                "passwordHash",
                "Rita",
                "Test"
        );

        User member2 = new User(
                "membro2@example.com",
                "passwordHash",
                "Mario",
                "Test"
        );

        Instant firstUsage = createdAt.plusSeconds(60);

        invitation.markAsUsed(member1, firstUsage);

        assertThrows(
                IllegalStateException.class,
                () -> invitation.markAsUsed(
                        member2,
                        firstUsage.plusSeconds(10)
                )
        );

        //Il primo utilizzo non deve essere sovrascritto
        assertSame(member1, invitation.getUsedBy());
        assertEquals(firstUsage, invitation.getUsedAt());
    }


    @Test //Tentativo di utilizzare un invito scaduto
    void markAsUsed_ExpiredInvitation(){

        GroupInvitation invitation = createInvitation();

        User member = new User(
                "membro@example.com",
                "passwordHash",
                "Rita",
                "Test"
        );

        assertThrows(
                IllegalStateException.class,
                () -> invitation.markAsUsed(member, expiresAt)
        );

        assertFalse(invitation.isUsed());
        assertNull(invitation.getUsedAt());
        assertNull(invitation.getUsedBy());
    }


    @Test //Tentativo di utilizzare l'invito prima della creazione
    void markAsUsed_BeforeCreation(){

        GroupInvitation invitation = createInvitation();

        User member = new User(
                "membro@example.com",
                "passwordHash",
                "Rita",
                "Test"
        );

        assertThrows(
                IllegalStateException.class,
                () -> invitation.markAsUsed(
                        member,
                        createdAt.minusSeconds(1)
                )
        );

        assertFalse(invitation.isUsed());
        assertNull(invitation.getUsedBy());
    }


    //METODO DI SUPPORTO

    private GroupInvitation createInvitation(){

        StudyGroup group = new StudyGroup(
                "Basi di Dati",
                null
        );

        User owner = new User(
                "owner@example.com",
                "passwordHash",
                "Domenico",
                "Test"
        );

        return new GroupInvitation(
                group,
                owner,
                "hashDiProva",
                createdAt,
                expiresAt
        );
    }
}
