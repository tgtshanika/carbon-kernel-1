package org.wso2.carbon.caching.impl.clustering;

import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.caching.impl.CacheImpl;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.io.Serializable;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;

/**
 * This is the cluster-wide local cache invalidation message that is sent
 * to all the other nodes in a cluster. This invalidates its own cache.
 *
 * This is based on Axis2 clustering.
 *
 */
public class ClusterCacheInvalidationRequest extends ClusteringMessage {

    private static final transient Log log = LogFactory.getLog(ClusterCacheInvalidationRequest.class);
    private static final long serialVersionUID = 94L;

    private CacheInfo cacheInfo;
    private String tenantDomain;
    private int tenantId;

    public ClusterCacheInvalidationRequest(CacheInfo cacheInfo, String tenantDomain, int tenantId) {
        this.cacheInfo = cacheInfo;
        this.tenantDomain = tenantDomain;
        this.tenantId = tenantId;
    }

    public ClusterCacheInvalidationRequest() {
    }

    @Override
    public void execute(ConfigurationContext configurationContext) throws ClusteringFault {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Received [" + this + "] ");
            }
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantId(tenantId);
            carbonContext.setTenantDomain(tenantDomain);

            CacheManager cacheManager = Caching.getCacheManagerFactory().getCacheManager(cacheInfo.cacheManagerName);
            Cache<Object, Object> cache = cacheManager.getCache(cacheInfo.cacheName);
            if (cache instanceof CacheImpl) {
                ((CacheImpl) cache).removeLocal(cacheInfo.cacheKey);
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

    }

    @Override
    public String toString() {
        return "ClusterCacheInvalidationRequest{" +
                "tenantId=" + tenantId +
                ", tenantDomain='" + tenantDomain + '\'' +
                ", messageId=" + getUuid() +
                ", cacheManager=" + cacheInfo.cacheManagerName +
                ", cache=" + cacheInfo.cacheName +
                ", cacheKey=" +cacheInfo.cacheKey +
                '}';
    }

    @Override
    public ClusteringCommand getResponse() {
        return null;
    }

    public static class CacheInfo implements Serializable {
        private String cacheManagerName;
        private String cacheName;
        private Object cacheKey;

        public CacheInfo(String cacheManagerName, String cacheName, Object cacheKey) {
            this.cacheManagerName = cacheManagerName;
            this.cacheName = cacheName;
            this.cacheKey = cacheKey;
        }
    }

}
