package com.ieltsplatform.common.base;

/**
 * ====================================================================================================
 * ARCHITECTURAL DESIGN & KNOWLEDGE BASE: BASE ENTITY (BaseEntity)
 * ====================================================================================================
 *
 * ----------------------------------------------------------------------------------------------------
 * 1. WHY DO WE NEED THIS BASE CLASS? (PROBLEM -> SOLUTION)
 * ----------------------------------------------------------------------------------------------------
 * Problem 1: Code Duplication Across 15+ Entities
 *   - Without a common base class, every entity (User, ReadingPassage, TestAttempt, EssaySubmission,
 *     Flashcard, DictationItem, etc.) would redundantly define primary key (@Id UUID id) and timestamp
 *     fields (createdAt, updatedAt).
 *   - Solution: @MappedSuperclass allows all child entities to inherit these metadata fields automatically.
 *
 * Problem 2: Concurrent Update Data Corruption ("Lost Update Problem")
 *   - Scenario: User A and User B open the same record simultaneously. User A edits and saves at 10:00 AM.
 *     User B edits and saves at 10:01 AM without realizing User A updated it. User B's save silently
 *     overwrites User A's changes.
 *   - Solution: Optimistic Locking ("Soft Lock") via a @Version column. Hibernate verifies that the
 *     version in DB matches the version in memory before updating:
 *     UPDATE table SET ..., version = version + 1 WHERE id = ? AND version = current_version;
 *     If the row version changed, Hibernate throws an OptimisticLockException.
 *
 * Problem 3: Stale & Inconsistent Timestamps
 *   - Developers frequently forget to manually set `updatedAt = Instant.now()` in service code.
 *   - Solution: Spring Data JPA's @EntityListeners(AuditingEntityListener.class) automatically handles
 *     @CreatedDate and @LastModifiedDate injection during INSERT and UPDATE operations.
 *
 * Problem 4: Broken Java Collection Equality (Set/Map) with Hibernate Proxies
 *   - Lazy loading in JPA uses proxy classes. Default Object.equals() compares memory references,
 *     which breaks when comparing a lazy-loaded proxy to a real entity inside a HashSet/HashMap.
 *   - Solution: Override equals() and hashCode() strictly derived from the business primary key (`id`).
 *
 * ----------------------------------------------------------------------------------------------------
 * 2. UNDERSTANDING java.time.Instant
 * ----------------------------------------------------------------------------------------------------
 * What is Instant?
 *   - Represents an absolute, single point on the global timeline, measured in nanoseconds from
 *     the Unix epoch (1970-01-01T00:00:00Z).
 *
 * Why Instant instead of Date or LocalDateTime?
 *   - Always UTC: Instant has no timezone ambiguity. LocalDateTime lacks timezone context (making it
 *     dangerous for global apps), while java.util.Date is legacy, mutable, and notoriously buggy.
 *   - Database Mapping: Maps directly to PostgreSQL `TIMESTAMPTZ` (TIMESTAMP WITH TIME ZONE).
 *
 * ----------------------------------------------------------------------------------------------------
 * 3. LOCKING STRATEGIES: OPTIMISTIC LOCK (SOFT LOCK) VS PESSIMISTIC LOCK (HARD LOCK)
 * ----------------------------------------------------------------------------------------------------
 * Optimistic Locking (Soft Lock - Handled by @Version here):
 *   - Non-blocking: No database row locks are acquired during reads or editing.
 *   - Conflict Detection: Verifies version number at the moment of UPDATE. Throws OptimisticLockException
 *     if another transaction modified the row first.
 *   - Best for: High-throughput web applications, profile updates, flashcards, essay submissions.
 *
 * Pessimistic Locking (Hard Lock - Handled via @Lock(LockModeType.PESSIMISTIC_WRITE) in Repositories):
 *   - DB-Level Blocking: Physically executes `SELECT ... FOR UPDATE` in SQL to lock the row.
 *   - Other transactions attempting to access the row must wait until the locking transaction commits/rolls back.
 *   - Best for: High-contention financial transactions, Stripe webhook updates, inventory deductions.
 *
 * ====================================================================================================
 * IMPLEMENTATION SKELETON & TODO TASKS
 * ====================================================================================================
 */

// TODO: Import jakarta.persistence.* annotations (@MappedSuperclass, @EntityListeners, @Id, @GeneratedValue, @GenerationType, @Column, @Version)
// TODO: Import org.springframework.data.annotation.* annotations (@CreatedDate, @LastModifiedDate)
// TODO: Import org.springframework.data.jpa.domain.support.AuditingEntityListener
// TODO: Import lombok.Getter and lombok.Setter
// TODO: Import java.time.Instant
// TODO: Import java.util.UUID and java.util.Objects

// TODO: Add @Getter annotation (Lombok)
// TODO: Add @Setter annotation (Lombok)
// TODO: Add @MappedSuperclass annotation (JPA)
// TODO: Add @EntityListeners(AuditingEntityListener.class) annotation (Spring Data JPA)
public abstract class BaseEntity {

    // ================================================================================================
    // FIELD 1: PRIMARY KEY IDENTIFIER (UUID)
    // ================================================================================================
    // TODO: Annotate with @Id
    // TODO: Annotate with @GeneratedValue(strategy = GenerationType.UUID)
    // TODO: Annotate with @Column(name = "id", updatable = false, nullable = false)
    // TODO: Declare field: private UUID id;


    // ================================================================================================
    // FIELD 2: CREATION TIMESTAMP (UTC INSTANT)
    // ================================================================================================
    // TODO: Annotate with @CreatedDate
    // TODO: Annotate with @Column(name = "created_at", nullable = false, updatable = false)
    // TODO: Declare field: private Instant createdAt;


    // ================================================================================================
    // FIELD 3: LAST MODIFICATION TIMESTAMP (UTC INSTANT)
    // ================================================================================================
    // TODO: Annotate with @LastModifiedDate
    // TODO: Annotate with @Column(name = "updated_at", nullable = false)
    // TODO: Declare field: private Instant updatedAt;


    // ================================================================================================
    // FIELD 4: OPTIMISTIC LOCKING / SOFT LOCK VERSION COUNTER
    // ================================================================================================
    // TODO: Annotate with @Version
    // TODO: Annotate with @Column(name = "version", nullable = false)
    // TODO: Declare field: private Long version;


    // ================================================================================================
    // METHODS: EQUALS & HASHCODE CONTRACT (ID-BASED EQUALITY)
    // ================================================================================================

    // TODO: Override equals(Object o) method:
    //       1. Check reference equality: if (this == o) return true;
    //       2. Check null & class compatibility: if (o == null || getClass() != o.getClass()) return false;
    //       3. Cast o to BaseEntity.
    //       4. Compare primary keys: return id != null && Objects.equals(id, that.id);

    // TODO: Override hashCode() method:
    //       1. Return id != null ? Objects.hash(id) : getClass().hashCode();
}
