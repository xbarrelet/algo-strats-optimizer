package ch.xavier.backtester.strategy.analyzer;

import ch.xavier.backtester.quote.typed.QuoteType;
import ch.xavier.backtester.result.MongoResultsRepository;
import ch.xavier.backtester.strategy.Strategies;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Slf4j
class StrategiesAnalyzerTest {

    @Autowired
    private MongoResultsRepository resultsRepository;

    @Autowired
    private StrategiesAnalyzer analyzer;

    private static final String TEST_COLLECTION_NAME = "GlobalExtremaStrategy - DAILY";
    private static final Strategies TEST_STRATEGY = Strategies.GlobalExtremaStrategy;
    private static final QuoteType TEST_QUOTE_TYPE = QuoteType.DAILY;



    @Test
    public void run_alltestStrategies_onFB() {
        // GIVEN
        resultsRepository.dropCollection(TEST_COLLECTION_NAME).block();

        // WHEN
        analyzer.analyzeStrategyOnSymbolsWithQuotes(Flux.just("FB"), TEST_QUOTE_TYPE, TEST_STRATEGY)
                .blockLast(); //60 variations * 82 symbols

        // THEN
//        assertEquals(4920, resultsRepository.countResultsInCollection(TEST_COLLECTION_NAME).block());
        assertEquals(60, resultsRepository.countSpecificResultsForSymbolInCollection("FB", TEST_COLLECTION_NAME).block());
    }
}