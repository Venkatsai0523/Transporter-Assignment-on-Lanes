package com.transporter.assignment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "lane_quotes",
        uniqueConstraints = @UniqueConstraint(name = "uk_lane_quote_lane_transporter",
                columnNames = {"lane_id", "transporter_id"}),
        indexes = {
                @Index(name = "idx_lane_quote_lane_id", columnList = "lane_id"),
                @Index(name = "idx_lane_quote_transporter_id", columnList = "transporter_id")
        })
public class LaneQuote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lane_id", nullable = false)
    private Lane lane;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporter_id", nullable = false)
    private Transporter transporter;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal quote;

    protected LaneQuote() {
    }

    public LaneQuote(Lane lane, Transporter transporter, BigDecimal quote) {
        this.lane = lane;
        this.transporter = transporter;
        this.quote = quote;
        lane.getQuotes().add(this);
        transporter.getQuotes().add(this);
    }

    public Long getId() {
        return id;
    }

    public Lane getLane() {
        return lane;
    }

    public Transporter getTransporter() {
        return transporter;
    }

    public BigDecimal getQuote() {
        return quote;
    }
}
