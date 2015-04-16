package com.viewds.test.vertx.cluster;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.Router;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.ext.apex.handler.CookieHandler;
import io.vertx.ext.apex.handler.SessionHandler;
import io.vertx.ext.apex.sstore.ClusteredSessionStore;
import io.vertx.ext.apex.sstore.SessionStore;

public class Example
        implements Runnable
{
    private static final Logger logger =
            LoggerFactory.getLogger(Example.class);
    private Vertx vertx;
    
    public static void main(String[] args)
    {
        new Example(args).run();
    }

    public Example(String[] args)
    {
    }

    @Override
    public void run()
    {
        VertxOptions options;
        
        options = new VertxOptions();
        Vertx.clusteredVertx(options, (AsyncResult<Vertx> event) -> {
            if (event.succeeded()) {
                vertx = event.result();
                startHttpServer();
            }
            else {
                logger.error("failed to obtain vertx object", event.cause());
            }
        });
    }

    private void startHttpServer()
    {
        SessionStore store;
        Router router;

        store = ClusteredSessionStore.create(vertx);
        
        router = Router.router(vertx);
        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(store));
        router.route().handler(this::doSomething);

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8080, (AsyncResult<HttpServer> event) -> {
                    if (event.succeeded()) {
                        logger.info("HTTP server started successfully");
                    }
                    else {
                        logger.error("HTTP server failed to start",
                                event.cause());
                    }
                });
    }

    private void doSomething(RoutingContext context)
    {
        Integer count;
        
        if ((count = context.session().get("count")) == null) {
            count = 0;
        }
        count += 1;
        context.session().put("count", count);
        context.response()
                .setStatusCode(200)
                .putHeader("Content-type", "text/plain")
                .end("count = " + count + "\n");
    }
}
