package com.falsework.core.mock;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.Watch;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.kv.PutResponse;
import com.coreos.jetcd.lease.LeaseGrantResponse;
import com.coreos.jetcd.lease.LeaseKeepAliveResponse;
import com.coreos.jetcd.options.PutOption;
import com.coreos.jetcd.watch.WatchEvent;
import com.coreos.jetcd.watch.WatchResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class EtcdTest {
    private Client client;

    @Before
    public void setUp() throws Exception {
        client = Client.builder().endpoints("http://127.0.0.1:2379")
                .user(ByteSequence.fromString("root"))
                .password(ByteSequence.fromString("root"))
                .build();
    }


    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void tt01() throws ExecutionException, InterruptedException {
        PutResponse put = client.getKVClient().put(ByteSequence.fromString("test"), ByteSequence.fromString("test"),
                PutOption.newBuilder()
                        .withPrevKV()
                        .build()).get();
        Assert.assertNotNull(put);
    }

    @Test
    public void tt02() throws ExecutionException, InterruptedException {
        LeaseGrantResponse lease = client.getLeaseClient().grant(30).get();
        PutResponse put = client.getKVClient().put(ByteSequence.fromString("test"), ByteSequence.fromString("test"),
                PutOption.newBuilder()
                        .withPrevKV()
                        .withLeaseId(lease.getID())
                        .build()).get();
        Assert.assertNotNull(put);
    }

    @Test
    public void tt03() throws ExecutionException, InterruptedException {
        LeaseGrantResponse lease = client.getLeaseClient().grant(30).get();
        client.getKVClient().put(ByteSequence.fromString("test"), ByteSequence.fromString("test"),
                PutOption.newBuilder()
                        .withPrevKV()
                        .withLeaseId(lease.getID())
                        .build()).get();
        Lease.KeepAliveListener listener = client.getLeaseClient().keepAlive(lease.getID());
        LeaseKeepAliveResponse alive = listener.listen();
        Assert.assertTrue(alive.getID() > 0);
        Assert.assertTrue(alive.getTTL() > 0);
        TimeUnit.SECONDS.sleep(300);
    }

    @Test
    public void tt04() throws InterruptedException {
        Watch.Watcher watcher = client.getWatchClient().watch(ByteSequence.fromString("test"));
        for (; ; ) {
            WatchResponse listen = watcher.listen();
            for (WatchEvent watchEvent : listen.getEvents()) {
                System.out.println(watchEvent);
            }
        }
    }
}
