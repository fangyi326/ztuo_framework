package cn.ztuo.bitrade.processor;


import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class CoinProcessorFactory {
    private HashMap<String, CoinProcessor> processorMap;
    private List<String> exchangeZones = new ArrayList<>();
    public CoinProcessorFactory() {
        processorMap = new HashMap<>();
    }

    public void addProcessor(String symbol, CoinProcessor processor) {
        log.info("CoinProcessorFactory addProcessor = {}",symbol);
        processorMap.put(symbol, processor);
        if(!exchangeZones.contains(processor.getBaseCoin())){
            exchangeZones.add(processor.getBaseCoin());
        }
    }

    public CoinProcessor getProcessorByCoin(String coin){
        for(String base:exchangeZones){
            String symbol = coin + "/" +base;
            if(processorMap.containsKey(symbol)){
                return processorMap.get(symbol);
            }
        }
        return null;
    }

    public CoinProcessor getProcessor(String symbol) {
        return processorMap.get(symbol);
    }

    public HashMap<String, CoinProcessor> getProcessorMap() {
        return processorMap;
    }
}
