package conexp.frontend.ruleview;

import conexp.core.ImplicationCalcStrategy;
import conexp.core.calculationstrategies.LinCbOImplicationCalculator;


public class LinCbOImplicationCalculatorFactory implements ImplicationCalcStrategyFactory {
    public ImplicationCalcStrategy makeImplicationCalcStrategy() {
        return new LinCbOImplicationCalculator();
    }

    private LinCbOImplicationCalculatorFactory() {
    }

    private static final ImplicationCalcStrategyFactory gInstance = new LinCbOImplicationCalculatorFactory();

    public static ImplicationCalcStrategyFactory getInstance() {
        return gInstance;
    }
}
