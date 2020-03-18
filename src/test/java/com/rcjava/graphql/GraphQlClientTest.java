package com.rcjava.graphql;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import com.apollographql.apollo.exception.ApolloException;
//import com.example.graphql.client.GetTransactionsQuery;
import com.apollographql.apollo.internal.ApolloLogger;
import com.apollographql.apollo.internal.subscription.RealSubscriptionManager;
import com.apollographql.apollo.subscription.*;
import com.rcjava.client.graphql.TransactionQuery;
import com.rcjava.client.graphql.TransactionSubSubscription;
import com.rcjava.client.graphql.type.TransactionWhereUniqueInput;
import com.rcjava.protos.Peer;
import okhttp3.*;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static java.lang.Thread.sleep;

/**
 * @author zyf
 */
public class GraphQlClientTest {

    private Subscription subscription = TransactionSubSubscription.builder().build();

    private ApolloSubscriptionCall.Callback subCallBack;

    @Test
    void testGetTransaction() throws Exception {
        ApolloClient client = ApolloClient.builder()
                .serverUrl("http://localhost:4466")
                .okHttpClient(new OkHttpClient.Builder().build()).build();

        System.out.println(TransactionQuery.builder().where(TransactionWhereUniqueInput.builder().txid("7a6e689d-beda-4bc5-bde6-86668872894f").build()).build().queryDocument());

        client.query(TransactionQuery.builder().where(TransactionWhereUniqueInput.builder().txid("7a6e689d-beda-4bc5-bde6-86668872894f").build()).build()).enqueue(
                new ApolloCall.Callback<Optional<TransactionQuery.Data>>() {
                    @Override
                    public void onResponse(@NotNull Response<Optional<TransactionQuery.Data>> response) {
                        System.out.println(response);
                        System.out.println(response.data().get().getTransaction().get().getChaincodeID().getChaincodeName());
                        System.out.println(response.dependentKeys());
                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        System.out.println(e.getMessage());
                    }
                }
        );

        sleep(10000);

    }

    @Test
    @SuppressWarnings("unchecked")
    void testSubTransaction() throws InterruptedException {

        final boolean[] onTerminated = {false};

        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

        ApolloClient client = ApolloClient.builder()
                .serverUrl("http://localhost:4466")
                .subscriptionTransportFactory(new WebSocketSubscriptionTransport.Factory("ws://localhost:4466/", okHttpClient))
                .okHttpClient(okHttpClient)
                .build();

        client.addOnSubscriptionManagerStateChangeListener(new SubscriptionManagerOnStateChangeListener(client));

        System.out.println(client.getSubscriptionManager().getState());

        ApolloSubscriptionCall subscriptionCall = client.subscribe(subscription);

        subCallBack = new ApolloSubscriptionCall.Callback<Optional<TransactionSubSubscription.Data>>() {
            @Override
            public void onResponse(@NotNull Response<Optional<TransactionSubSubscription.Data>> response) {
                System.out.println(response.data().get());
                System.out.println(response.data().get().getTransaction().get().getNode());
                System.out.println(response.data().get().getTransaction().get().getNode().get().getTxid());
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println(e.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("completed");
            }

            @Override
            public void onTerminated() {
                onTerminated[0] = true;
                System.out.println("terminated");

            }

            @Override
            public void onConnected() {
                System.err.println("connected");
            }
        };

        subscriptionCall.execute(subCallBack);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(()-> {
            if (onTerminated[0]) {
                try {
                    client.getSubscriptionManager().unsubscribe(subscription);
                    sleep(15000);
                    subscriptionCall.clone().execute(subCallBack);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                onTerminated[0] = false;
            }
        },10, 30, TimeUnit.SECONDS);


        System.out.println(client.getSubscriptionManager().getState());

        sleep(3600000);
    }

    @Test
    void testSyncGetTransaction() {
        ApolloClient client = ApolloClient.builder()
                .serverUrl("http://localhost:4466")
                .okHttpClient(new OkHttpClient.Builder().build()).build();

        toCompletableFuture(
                client.query(TransactionQuery.builder()
                        .where(TransactionWhereUniqueInput.builder()
                                .txid("7a6e689d-beda-4bc5-bde6-86668872894f")
                                .build())
                        .build())).join();

    }


    public <T> CompletableFuture<Response<T>> toCompletableFuture(ApolloCall<T> apolloCall) {

        CompletableFuture<Response<T>> completableFuture = new CompletableFuture<>();

        completableFuture.whenComplete((tResponse, throwable) -> {
            if (completableFuture.isCancelled()) {
                completableFuture.cancel(true);
            }
            System.out.println(((Optional<TransactionQuery.Data>) tResponse.data()).get().getTransaction().get().getChaincodeID().getChaincodeName());
        });

        apolloCall.enqueue(new ApolloCall.Callback<T>() {
            @Override
            public void onResponse(@NotNull Response<T> response) {
                completableFuture.complete(response);
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                completableFuture.completeExceptionally(e);
            }
        });

        return completableFuture;
    }


    @SuppressWarnings("unchecked")
    private class SubscriptionManagerOnStateChangeListener implements OnSubscriptionManagerStateChangeListener {

        private final ApolloClient client;

        public SubscriptionManagerOnStateChangeListener(ApolloClient client) {
            this.client = client;
        }

        @Override
        public void onStateChange(SubscriptionManagerState fromState, SubscriptionManagerState toState) {
            synchronized (this) {

                System.err.println(fromState.name() + ":" + toState.name());

//                if (fromState == SubscriptionManagerState.ACTIVE && toState == SubscriptionManagerState.DISCONNECTED) {
//                    // 1、先unsubscribe，不执行disconnect，容易资源泄露
////                     client.getSubscriptionManager().unsubscribe(subscription);
//                    // 2、unsubscribe所有，停掉所有connectiion
//                    client.disableSubscriptions();
//                }
//                if (fromState == SubscriptionManagerState.STOPPING && toState ==  SubscriptionManagerState.STOPPED) {
//                    // re-enable subscription
//                    client.enableSubscriptions();
//                }
//                if (fromState == SubscriptionManagerState.STOPPED && toState ==  SubscriptionManagerState.DISCONNECTED) {
//                    // 然后执行subCallback即可
//                    client.subscribe(subscription).execute(subCallBack);
//                }
            }
        }


    }

}

