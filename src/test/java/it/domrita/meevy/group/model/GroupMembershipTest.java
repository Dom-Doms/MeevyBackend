
package it.domrita.meevy.group.model;

import it.domrita.meevy.group.role.GroupRole;
import it.domrita.meevy.user.model.User;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class GroupMembershipTest {

    @Test //Creazione dell'appartenenza del proprietario
    void createMembership_OwnerTest(){

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

        GroupMembership membership = new GroupMembership(
                group,
                user,
                GroupRole.OWNER
        );

        assertNull(membership.getId());
        assertSame(group, membership.getGroup());
        assertSame(user, membership.getUser());
        assertEquals(GroupRole.OWNER, membership.getRole());
        assertNull(membership.getJoinedAt());
    }


    @Test //Creazione dell'appartenenza di un membro
    void createMembership_MemberTest(){

        StudyGroup group = new StudyGroup(
                "Basi di Dati",
                "Preparazione esame"
        );

        User user = new User(
                "utente@example.com",
                "passwordHash",
                "Rita",
                "Test"
        );

        GroupMembership membership = new GroupMembership(
                group,
                user,
                GroupRole.MEMBER
        );

        assertSame(group, membership.getGroup());
        assertSame(user, membership.getUser());
        assertEquals(GroupRole.MEMBER, membership.getRole());
    }


    @Test //Promozione di un membro a moderatore
    void changeRole_PromoteMember(){

        StudyGroup group = new StudyGroup(
                "Basi di Dati",
                null
        );

        User user = new User(
                "utente@example.com",
                "passwordHash",
                "Rita",
                "Test"
        );

        GroupMembership membership = new GroupMembership(
                group,
                user,
                GroupRole.MEMBER
        );

        membership.changeRole(GroupRole.MODERATOR);

        assertEquals(GroupRole.MODERATOR, membership.getRole());
        assertSame(group, membership.getGroup());
        assertSame(user, membership.getUser());
    }


    @Test //Declassamento di un moderatore
    void changeRole_DemoteModerator(){

        StudyGroup group = new StudyGroup(
                "Basi di Dati",
                null
        );

        User user = new User(
                "utente@example.com",
                "passwordHash",
                "Rita",
                "Test"
        );

        GroupMembership membership = new GroupMembership(
                group,
                user,
                GroupRole.MODERATOR
        );

        membership.changeRole(GroupRole.MEMBER);

        assertEquals(GroupRole.MEMBER, membership.getRole());
    }


    @Test //Assegnazione del ruolo OWNER
    void changeRole_NewOwner(){

        StudyGroup group = new StudyGroup(
                "Basi di Dati",
                null
        );

        User user = new User(
                "utente@example.com",
                "passwordHash",
                "Rita",
                "Test"
        );

        GroupMembership membership = new GroupMembership(
                group,
                user,
                GroupRole.MEMBER
        );

        membership.changeRole(GroupRole.OWNER);

        assertEquals(GroupRole.OWNER, membership.getRole());
    }


    @Test //Impostazione della data di ingresso
    void onCreate_Test(){

        StudyGroup group = new StudyGroup(
                "Basi di Dati",
                null
        );

        User user = new User(
                "utente@example.com",
                "passwordHash",
                "Rita",
                "Test"
        );

        GroupMembership membership = new GroupMembership(
                group,
                user,
                GroupRole.MEMBER
        );

        Instant before = Instant.now();

        ReflectionTestUtils.invokeMethod(
                membership, "onCreate"
        );

        Instant after = Instant.now();

        assertNotNull(membership.getJoinedAt());

        assertFalse(membership.getJoinedAt().isBefore(before));
        assertFalse(membership.getJoinedAt().isAfter(after));
    }


    @Test //Il cambio del ruolo non modifica la data di ingresso
    void changeRole_JoinedAtUnchanged(){

        StudyGroup group = new StudyGroup(
                "Basi di Dati",
                null
        );

        User user = new User(
                "utente@example.com",
                "passwordHash",
                "Rita",
                "Test"
        );

        GroupMembership membership = new GroupMembership(
                group,
                user,
                GroupRole.MEMBER
        );

        Instant joinedAt = Instant.parse("2026-11-12T10:00:00Z");

        ReflectionTestUtils.setField(
                membership, "joinedAt", joinedAt
        );

        membership.changeRole(GroupRole.MODERATOR);

        assertEquals(GroupRole.MODERATOR, membership.getRole());
        assertEquals(joinedAt, membership.getJoinedAt());
    }
}
