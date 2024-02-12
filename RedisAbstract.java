import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;


public abstract class RedisAbstract {
    private final RoundRobinConnectionPool<String, String> roundRobinConnectionPool;
    private final List<StatefulRedisPubSubConnection<String, String>> pubSubConnections;
    protected RedisClient lettuceRedisClient;

    public RedisAbstract(RedisClient lettuceRedisClient, int poolSize) {
        this.lettuceRedisClient = lettuceRedisClient;
        this.roundRobinConnectionPool = new RoundRobinConnectionPool<>(lettuceRedisClient::connect, poolSize);
        pubSubConnections = new CopyOnWriteArrayList<>();
    }

    public <T> CompletionStage<T> getConnectionAsync(Function<RedisAsyncCommands<String, String>, CompletionStage<T>> redisCallBack) {
        return redisCallBack.apply(roundRobinConnectionPool.get().async());
    }

    public <T> CompletionStage<T> getConnectionPipeline(Function<RedisAsyncCommands<String, String>, CompletionStage<T>> redisCallBack) {
        StatefulRedisConnection<String, String> connection = roundRobinConnectionPool.get();
        connection.setAutoFlushCommands(false);
        CompletionStage<T> completionStage = redisCallBack.apply(connection.async());
        connection.flushCommands();
        connection.setAutoFlushCommands(true);
        return completionStage;
    }

    public StatefulRedisPubSubConnection<String, String> getPubSubConnection() {
        StatefulRedisPubSubConnection<String, String> pubSubConnection = lettuceRedisClient.connectPubSub();
        pubSubConnections.add(pubSubConnection);
        return pubSubConnection;
    }

    public void close() {
        pubSubConnections.forEach(StatefulRedisPubSubConnection::close);
        Bukkit.getLogger().info("Closing pubsub connection");
        lettuceRedisClient.shutdown();
        Bukkit.getLogger().info("Lettuce shutdown connection");
    }


}
