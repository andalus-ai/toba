package com.dremioplatform.ai.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only log of every AI_SUMMARIZE/AI_CLASSIFY call (plan Section 5.5, "AI
 * functions: exposes SQL-callable UDFs ... implemented as a Dremio UDF that proxies
 * to this service"). Exists for two reasons: cost/usage tracking (LLM calls cost
 * money per token, and a runaway query calling an AI function per-row could get
 * expensive fast — this table is the data a future rate-limiter/budget-alert would
 * key off), and because Governance's audit trail should be able to answer "did an
 * AI function ever see this sensitive column's data" — this table plus a future
 * catalog.events-style publish gives it that.
 */
@Entity
@Table(name = "ai_function_invocation")
public class AiFunctionInvocation extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String functionName;

    @Column(nullable = false, columnDefinition = "text")
    public String inputText;

    @Column(columnDefinition = "text")
    public String outputText;

    @Column(nullable = false)
    public String model;

    public Integer promptTokens;
    public Integer completionTokens;

    public String principalType;
    public String principalId;

    /** Dataset/column path the input text was pulled from, if the caller supplied
     *  one — optional, since AI functions can also be called on ad-hoc literal text. */
    public String sourceDatasetPath;

    @Column(nullable = false)
    public Instant invokedAt = Instant.now();

    public Long latencyMillis;

    public boolean failed = false;
    public String errorMessage;
}
