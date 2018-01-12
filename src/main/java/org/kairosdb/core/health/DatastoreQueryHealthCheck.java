package org.kairosdb.core.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import org.kairosdb.core.Main;
import org.kairosdb.core.datastore.DataPointGroup;
import org.kairosdb.core.datastore.DatastoreQuery;
import org.kairosdb.core.datastore.KairosDatastore;
import org.kairosdb.core.datastore.QueryMetric;
import org.kairosdb.core.exception.DatastoreException;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class DatastoreQueryHealthCheck extends HealthCheck implements HealthStatus {
    static final String NAME = "Datastore-Query";
    private final KairosDatastore datastore;
    private org.slf4j.Logger logger = LoggerFactory.getLogger(Main.class);

    @Inject
    public DatastoreQueryHealthCheck(KairosDatastore datastore) {
        this.datastore = checkNotNull(datastore);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected Result check() throws Exception {
        DatastoreQuery query = null;
        try {
            logger.debug("Checking datastore health...");
            query = datastore.createQuery(
                    new QueryMetric(System.currentTimeMillis() - (10 * 60 * 1000),
                            0, "kairosdb.jvm.thread_count"));
            List<DataPointGroup> results = query.execute();
            logger.debug(" ... got results (size:" + results.size() + ")");
            return Result.healthy();
        } catch (DatastoreException e) {
            logger.warn("Got exception : " + e.getMessage(), e);
            return Result.unhealthy(e);
        } finally {
            if (query!=null) {
                try {
                    query.close();
                } catch (Exception ex) {
                    logger.error("Caught exception while closing query", ex);
                }
            }
        }
    }
}
