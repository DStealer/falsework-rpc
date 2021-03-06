/*
 * This file is generated by jOOQ.
 */
package com.falsework.census.generated.jooq;


import com.falsework.census.generated.jooq.tables.MetricsViews;
import com.falsework.census.generated.jooq.tables.records.MetricsViewsRecord;

import javax.annotation.Generated;

import org.jooq.UniqueKey;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables of 
 * the <code>statistics</code> schema.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.7"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // IDENTITY definitions
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<MetricsViewsRecord> KEY_METRICS_VIEWS_PRIMARY = UniqueKeys0.KEY_METRICS_VIEWS_PRIMARY;

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class UniqueKeys0 {
        public static final UniqueKey<MetricsViewsRecord> KEY_METRICS_VIEWS_PRIMARY = Internal.createUniqueKey(MetricsViews.METRICS_VIEWS, "KEY_metrics_views_PRIMARY", MetricsViews.METRICS_VIEWS.METRIC_ID);
    }
}
