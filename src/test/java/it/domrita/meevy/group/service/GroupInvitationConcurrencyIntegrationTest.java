
package it.domrita.meevy.group.service;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import it.domrita.meevy.common.security.OpaqueTokenGenerator;

import it.domrita.meevy.group.dto.*;
import it.domrita.meevy.group.exception.AlreadyGroupMemberException;
import it.domrita.meevy.group.exception.GroupInvitationUsedException;
import it.domrita.meevy.group.repository.GroupMembershipRepository;
import it.domrita.meevy.group.role.GroupRole;

import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import({
        GroupInvitationService.class,
        StudyGroupService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/meevy",
        "spring.flyway.url=jdbc:postgresql://localhost:5432/meevy",
        "spring.flyway.user=${DB_USERNAME}",
        "spring.flyway.password=${DB_PASSWORD}",
        "spring.flyway.enabled=true",
        "spring.flyway.clean-disabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.hikari.maximum-pool-size=5"
})
public class GroupInvitationConcurrencyIntegrationTest {

    @Autowired
    private GroupInvitationService groupInvitationService;

    @Autowired
    private StudyGroupService studyGroupService;

    @Autowired
    private GroupMembershipRepository groupMembershipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private CurrentUserIdProvider currentUserIdProvider;

    //Ogni thread deve avere il proprio utente autenticato
    private final ThreadLocal<Long> currentUser =
            new ThreadLocal<>();

    private final List<Long> temporaryUsers =
            new ArrayList<>();

    private TransactionTemplate transactionTemplate;

    private Long groupId;

    private Long ownerId;

    private Long member1Id;

    private Long member2Id;


    @BeforeEach
    void setUp(){

        transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.setIsolationLevel(
                TransactionDefinition.ISOLATION_READ_COMMITTED
        );

        //Il mock restituisce l'utente associato al thread corrente
        when(currentUserIdProvider.getCurrentUserId())
                .thenAnswer(invocation -> {

                    Long userId = currentUser.get();

                    if (userId == null) {
                        throw new IllegalStateException(
                                "Utente di test non impostato."
                        );
                    }

                    return userId;
                });

        ownerId = createUser("Owner");
        member1Id = createUser("Membro1");
        member2Id = createUser("Membro2");

        //Creiamo un gruppo utilizzando il Service reale
        StudyGroupResponse group = asUser(
                ownerId,
                () -> studyGroupService.createGroup(
                        new StudyGroupRequest(
                                "Gruppo test concorrenza",
                                "Test degli inviti"
                        )
                )
        );

        groupId = group.id();
    }


    @Test //Due utenti tentano di utilizzare contemporaneamente lo stesso invito
    void acceptInvitation_ConcurrentSameToken() throws Exception {

        GroupInvitationResponse invitation =
                createInvitation();

        String token = invitation.token();

        executeConcurrentAcceptance(
                member1Id,
                token,
                member2Id,
                token,
                GroupInvitationUsedException.class
        );

        //Solo il primo utente deve essere entrato nel gruppo
        assertTrue(
                groupMembershipRepository
                        .existsByGroup_IdAndUser_Id(
                                groupId,
                                member1Id
                        )
        );

        assertFalse(
                groupMembershipRepository
                        .existsByGroup_IdAndUser_Id(
                                groupId,
                                member2Id
                        )
        );

        //Il gruppo contiene solamente OWNER e primo MEMBER
        assertEquals(
                2L,
                groupMembershipRepository.countByGroup_Id(groupId)
        );

        //Verifichiamo chi ha effettivamente utilizzato l'invito
        assertEquals(
                member1Id,
                getInvitationUsedBy(token)
        );

        assertTrue(isInvitationUsed(token));
    }


    @Test //Lo stesso utente usa due inviti diversi dello stesso gruppo
    void acceptInvitation_ConcurrentDifferentTokens() throws Exception {

        GroupInvitationResponse invitation1 =
                createInvitation();

        GroupInvitationResponse invitation2 =
                createInvitation();

        assertNotEquals(
                invitation1.token(),
                invitation2.token()
        );

        executeConcurrentAcceptance(
                member1Id,
                invitation1.token(),
                member1Id,
                invitation2.token(),
                AlreadyGroupMemberException.class
        );

        //L'utente deve avere una sola appartenenza al gruppo
        assertTrue(
                groupMembershipRepository
                        .existsByGroup_IdAndUser_Id(
                                groupId,
                                member1Id
                        )
        );

        assertEquals(
                2L,
                groupMembershipRepository.countByGroup_Id(groupId)
        );

        //Il primo invito è stato consumato
        assertTrue(
                isInvitationUsed(invitation1.token())
        );

        assertEquals(
                member1Id,
                getInvitationUsedBy(invitation1.token())
        );

        //Il secondo invito deve essere ancora disponibile
        assertFalse(
                isInvitationUsed(invitation2.token())
        );

        assertNull(
                getInvitationUsedBy(invitation2.token())
        );

        //Un altro utente deve poter utilizzare il secondo invito
        StudyGroupResponse response = asUser(
                member2Id,
                () -> groupInvitationService.acceptInvitation(
                        new GroupInvitationAcceptRequest(
                                invitation2.token()
                        )
                )
        );

        assertEquals(GroupRole.MEMBER, response.myRole());
        assertEquals(3L, response.memberCount());

        assertEquals(
                member2Id,
                getInvitationUsedBy(invitation2.token())
        );
    }


    @Test //Il rollback annulla sia l'iscrizione sia il consumo dell'invito
    void acceptInvitation_TransactionRollback(){

        GroupInvitationResponse invitation =
                createInvitation();

        String token = invitation.token();

        asUser(member1Id, () -> {

            transactionTemplate.executeWithoutResult(status -> {

                StudyGroupResponse response =
                        groupInvitationService.acceptInvitation(
                                new GroupInvitationAcceptRequest(token)
                        );

                //L'utente è entrato nella transazione corrente
                assertEquals(2L, response.memberCount());

                assertTrue(
                        groupMembershipRepository
                                .existsByGroup_IdAndUser_Id(
                                        groupId,
                                        member1Id
                                )
                );

                //Simuliamo il rollback della transazione
                status.setRollbackOnly();
            });

            return null;
        });

        //Verifichiamo i dati dopo il rollback
        assertFalse(
                groupMembershipRepository
                        .existsByGroup_IdAndUser_Id(
                                groupId,
                                member1Id
                        )
        );

        assertEquals(
                1L,
                groupMembershipRepository.countByGroup_Id(groupId)
        );

        assertFalse(isInvitationUsed(token));
        assertNull(getInvitationUsedBy(token));

        //L'invito deve essere ancora utilizzabile
        StudyGroupResponse response = asUser(
                member2Id,
                () -> groupInvitationService.acceptInvitation(
                        new GroupInvitationAcceptRequest(token)
                )
        );

        assertEquals(GroupRole.MEMBER, response.myRole());
        assertEquals(2L, response.memberCount());

        assertEquals(member2Id, getInvitationUsedBy(token));
    }


    //METODI DI SUPPORTO


    //Esegue due accettazioni concorrenti sullo stesso gruppo
    private void executeConcurrentAcceptance(
            Long firstUserId,
            String firstToken,
            Long secondUserId,
            String secondToken,
            Class<? extends RuntimeException> expectedException
    ) throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        //Segnala che il primo utente ha accettato l'invito
        CountDownLatch firstAccepted =
                new CountDownLatch(1);

        //Impedisce il commit finché non abbiamo verificato il lock
        CountDownLatch allowCommit =
                new CountDownLatch(1);

        //Identificativi delle due connessioni PostgreSQL
        CompletableFuture<Integer> firstPid =
                new CompletableFuture<>();

        CompletableFuture<Integer> secondPid =
                new CompletableFuture<>();

        try {

            //PRIMO THREAD
            Future<StudyGroupResponse> firstFuture =
                    executor.submit(() -> asUser(
                            firstUserId,
                            () -> transactionTemplate.execute(status -> {

                                firstPid.complete(
                                        getCurrentPostgresPid()
                                );

                                StudyGroupResponse response =
                                        groupInvitationService.acceptInvitation(
                                                new GroupInvitationAcceptRequest(
                                                        firstToken
                                                )
                                        );

                                //Il Service ha terminato ma la transazione è ancora aperta
                                firstAccepted.countDown();

                                //Manteniamo il lock fino al segnale del test
                                awaitLatch(allowCommit);

                                return response;
                            })
                    ));

            //Aspettiamo che il primo thread abbia accettato l'invito
            assertTrue(
                    firstAccepted.await(15, TimeUnit.SECONDS),
                    "Il primo thread non ha completato l'accettazione."
            );

            //SECONDO THREAD
            Future<StudyGroupResponse> secondFuture =
                    executor.submit(() -> asUser(
                            secondUserId,
                            () -> transactionTemplate.execute(status -> {

                                //Evita un'attesa infinita in caso di problemi
                                jdbcTemplate.execute(
                                        "SET LOCAL lock_timeout = '15s'"
                                );

                                secondPid.complete(
                                        getCurrentPostgresPid()
                                );

                                return groupInvitationService.acceptInvitation(
                                        new GroupInvitationAcceptRequest(
                                                secondToken
                                        )
                                );
                            })
                    ));

            Integer pidA = firstPid.get(
                    5, TimeUnit.SECONDS
            );

            Integer pidB = secondPid.get(
                    5, TimeUnit.SECONDS
            );

            //Verifichiamo che siano due connessioni differenti
            assertNotEquals(pidA, pidB);

            //Controlliamo direttamente che PostgreSQL stia bloccando B
            waitForPostgresLock(pidA, pidB);

            assertFalse(
                    secondFuture.isDone(),
                    "Il secondo thread non dovrebbe avere terminato."
            );

            //Adesso il primo thread può eseguire il commit
            allowCommit.countDown();

            StudyGroupResponse firstResponse =
                    firstFuture.get(10, TimeUnit.SECONDS);

            assertEquals(groupId, firstResponse.id());
            assertEquals(GroupRole.MEMBER, firstResponse.myRole());
            assertEquals(2L, firstResponse.memberCount());

            //Il secondo thread deve fallire con l'eccezione prevista
            ExecutionException exception = assertThrows(
                    ExecutionException.class,
                    () -> secondFuture.get(
                            10,
                            TimeUnit.SECONDS
                    )
            );

            assertInstanceOf(
                    expectedException,
                    exception.getCause()
            );

        } finally {

            //Rilasciamo sempre il primo thread anche se un test fallisce
            allowCommit.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            25, TimeUnit.SECONDS
                    ),
                    "I thread del test non sono terminati."
            );
        }
    }


    //Verifica che la seconda transazione aspetti il lock sul gruppo
    private void waitForPostgresLock(
            Integer blockingPid,
            Integer waitingPid
    ) throws InterruptedException {

        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(10);

        while (System.nanoTime() < deadline) {

            Boolean blocked = jdbcTemplate.queryForObject(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM pg_stat_activity
                        WHERE pid = ?
                          AND ? = ANY(pg_blocking_pids(pid))
                          AND lower(query) LIKE '%study_group%'
                          AND lower(query) ~
                              'for[[:space:]]+(no[[:space:]]+key[[:space:]]+)?update'
                    )
                    """,
                    Boolean.class,
                    waitingPid,
                    blockingPid
            );

            if (Boolean.TRUE.equals(blocked)) {
                return;
            }

            //Ripetiamo il controllo senza mantenere occupata la CPU
            Thread.sleep(25);
        }

        fail(
                "PostgreSQL non ha rilevato il secondo thread " +
                        "in attesa del lock sul gruppo."
        );
    }


    //Recupera l'id della connessione PostgreSQL corrente
    private Integer getCurrentPostgresPid(){

        return jdbcTemplate.queryForObject(
                "SELECT pg_backend_pid()",
                Integer.class
        );
    }


    //Aspetta il segnale del test con un tempo massimo
    private void awaitLatch(CountDownLatch latch){

        try {

            if (!latch.await(40, TimeUnit.SECONDS)) {

                throw new IllegalStateException(
                        "Tempo massimo di attesa superato."
                );
            }

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Thread interrotto durante il test.",
                    ex
            );
        }
    }


    //Associa un utente al thread che esegue l'operazione
    private <T> T asUser(
            Long userId,
            Supplier<T> operation
    ){

        currentUser.set(userId);

        try {
            return operation.get();
        } finally {
            currentUser.remove();
        }
    }


    //Crea un utente temporaneo con email univoca
    private Long createUser(String name){

        User user = userRepository.saveAndFlush(
                new User(
                        "group-test-" + UUID.randomUUID()
                                + "@example.invalid",
                        "test-password-hash",
                        name,
                        "Test"
                )
        );

        temporaryUsers.add(user.getId());

        return user.getId();
    }


    //Crea un invito utilizzando il vero Service
    private GroupInvitationResponse createInvitation(){

        return asUser(
                ownerId,
                () -> groupInvitationService.createInvitation(groupId)
        );
    }


    //Controlla direttamente sul database chi ha utilizzato l'invito
    private Long getInvitationUsedBy(String token){

        return jdbcTemplate.queryForObject(
                """
                SELECT used_by_user_id
                FROM group_invitation
                WHERE token_hash = ?
                """,
                Long.class,
                OpaqueTokenGenerator.hash(token)
        );
    }


    //Verifica se l'invito è stato consumato
    private boolean isInvitationUsed(String token){

        Boolean used = jdbcTemplate.queryForObject(
                """
                SELECT used_at IS NOT NULL
                FROM group_invitation
                WHERE token_hash = ?
                """,
                Boolean.class,
                OpaqueTokenGenerator.hash(token)
        );

        return Boolean.TRUE.equals(used);
    }


    //Elimina esclusivamente i dati creati dal test
    @AfterEach
    void cleanup(){

        if (groupId != null) {

            //Le membership e gli inviti vengono eliminati tramite CASCADE
            jdbcTemplate.update(
                    "DELETE FROM study_group WHERE id = ?",
                    groupId
            );
        }

        for (Long userId : temporaryUsers) {

            jdbcTemplate.update(
                    "DELETE FROM \"user\" WHERE id = ?",
                    userId
            );
        }

        currentUser.remove();
    }
}
