package ch.xavier.tradingbot.realtime;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import net.jacobpeterson.abstracts.websocket.exception.WebsocketException;
import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.websocket.marketdata.listener.MarketDataListener;
import net.jacobpeterson.alpaca.websocket.marketdata.listener.MarketDataListenerAdapter;
import net.jacobpeterson.alpaca.websocket.marketdata.message.MarketDataMessageType;
import net.jacobpeterson.domain.alpaca.marketdata.realtime.MarketDataMessage;
import net.jacobpeterson.domain.alpaca.marketdata.realtime.bar.BarMessage;

import java.util.HashMap;
import java.util.Map;

public class RealtimeQuotesImporter extends AbstractBehavior<WatchSymbolMessage> {

    private final Map<String, MarketDataListener> realtimeQuotesImporters = new HashMap<>();
    private static AlpacaAPI api;


    public static Behavior<WatchSymbolMessage> create(AlpacaAPI alpacaAPI) {
        api = alpacaAPI;
        return Behaviors.setup(RealtimeQuotesImporter::new);
    }

    private RealtimeQuotesImporter(ActorContext<WatchSymbolMessage> context) {
        super(context);
    }

    @Override
    public Receive<WatchSymbolMessage> createReceive() {
        return newReceiveBuilder()
                .onMessage(WatchSymbolMessage.class, this::watchSymbol)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<WatchSymbolMessage> watchSymbol(WatchSymbolMessage message) {
        getContext().getLog().info("Now watching symbol:{}", message.symbol());

        MarketDataListener listenerTSLA = new MarketDataListenerAdapter(message.symbol(), MarketDataMessageType.BAR){
            @Override
            public void onStreamUpdate(MarketDataMessageType streamMessageType, MarketDataMessage streamMessage) {
                if (streamMessageType == MarketDataMessageType.BAR) {
                    BarMessage barMessage = (BarMessage) streamMessage;
                    getContext().getSystem().log().info("Bar received for symbol:{}: Open={} High={} Low={} Close={} Timestamp={}",
                            message.symbol(),
                            barMessage.getOpen(),
                            barMessage.getHigh(),
                            barMessage.getLow(),
                            barMessage.getClose(),
                            barMessage.getTimestamp());
                } else {
                    getContext().getSystem().log().error("Unknowm message received when watching symbol:{}, here it is:{}", message.symbol(),
                            streamMessage.toString());
                }
            }
        };
        realtimeQuotesImporters.put(message.symbol(), listenerTSLA);

        try {
            api.addMarketDataStreamListener(listenerTSLA);
        } catch (WebsocketException e) {
            getContext().getSystem().log().error("Error when adding listener for symbol:{}", message.symbol());
            e.printStackTrace();
        }

        return this;
    }

    private Behavior<WatchSymbolMessage> onPostStop() {
        getContext().getSystem().log().info("Stopping quotesImporter, {} listeners to stop", realtimeQuotesImporters.size());

        realtimeQuotesImporters.forEach((symbol, listener) -> {
            try {
                api.removeMarketDataStreamListener(listener);
            } catch (WebsocketException e) {
                getContext().getSystem().log().error("Error when closing realtime listener for symbol:{}", symbol);
                e.printStackTrace();
            }
        });
        return this;
    }
}