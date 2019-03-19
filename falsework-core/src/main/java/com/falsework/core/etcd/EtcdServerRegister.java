/**
 * 服务信息注册
 */

package com.falsework.core.etcd;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.kv.DeleteResponse;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.lease.LeaseGrantResponse;
import com.coreos.jetcd.lease.LeaseKeepAliveResponse;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.falsework.core.common.PPrints;
import com.falsework.core.generated.etcd.ServiceDefinition;
import com.falsework.core.generated.etcd.ServiceInformation;
import com.falsework.core.server.ServerRegister;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class EtcdServerRegister implements ServerRegister {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdServerRegister.class);
    private final Client client;
    private ServiceDefinition service;
    private ServiceInformation information;
    private Lease.KeepAliveListener listener;

    public EtcdServerRegister(Client client) {
        Preconditions.checkNotNull(client, "jetcd client");
        this.client = client;
    }


    public EtcdServerRegister withServiceDefinition(ServiceDefinition definition) {
        Preconditions.checkNotNull(definition, "service definition");
        this.service = definition;
        return this;
    }

    public EtcdServerRegister withInformationDefinition(ServiceInformation information) {
        Preconditions.checkNotNull(information, "service information");
        this.information = information;
        return this;
    }

    /**
     * 注册服务信息
     *
     * @throws Exception
     */
    @Override
    public synchronized void register() throws Exception {
        Preconditions.checkNotNull(this.service, "service definition");
        Preconditions.checkNotNull(this.information, "service information");
        ByteSequence serverDef = ByteSequence.fromByteString(this.service.toByteString());
        for (int i = 0; i < 10; i++) {
            if (this.listener != null) {
                LOGGER.warn("Service  has registered,no thing to do");
                break;
            } else {
                try {
                    LOGGER.info("Service:{} will register", PPrints.toString(this.service));
                    LeaseGrantResponse grantResponse = this.client.getLeaseClient().grant(30).get();
                    this.listener = this.client.getLeaseClient().keepAlive(grantResponse.getID());
                    LeaseKeepAliveResponse listen = this.listener.listen();
                    this.client.getKVClient().put(serverDef, ByteSequence.fromByteString(this.information.toByteString()),
                            PutOption.newBuilder().withLeaseId(listen.getID()).build()).get();
                    GetResponse confirmResponse = this.client.getKVClient().get(serverDef, GetOption.newBuilder()
                            .withCountOnly(true).build()).get();
                    if (confirmResponse.getCount() == 0) {
                        LOGGER.warn("register error,try later...");
                        this.listener.close();
                        this.listener = null;
                    } else {
                        break;
                    }
                } catch (Exception e) {
                    LOGGER.warn("Register service error, try later", e);
                    TimeUnit.SECONDS.sleep(5);
                }
            }
        }

        GetResponse confirmResponse = this.client.getKVClient().get(serverDef, GetOption.DEFAULT).get();
        if (confirmResponse.getCount() > 0) {
            LOGGER.info("Service:{} register success", PPrints.toString(this.service));
        } else {
            throw new RuntimeException("Sorry,register server failed,start failed");
        }
    }

    /**
     * 取消注册
     */
    @Override
    public synchronized void unregister() throws Exception {
        Preconditions.checkNotNull(this.service, "service definition");
        Preconditions.checkNotNull(this.information, "service information");
        LOGGER.info("service:\n{} will unregister", PPrints.toString(this.service));
        if (this.listener != null) {
            this.listener.close();
        }
        DeleteResponse deleteResponse = this.client.getKVClient().delete(
                ByteSequence.fromByteString(this.service.toByteString())).get();
        if (deleteResponse.getDeleted() > 0) {
            LOGGER.info("service unregister completely");
        } else {
            LOGGER.warn("service unregister unsuccessful");
        }
    }
}
