package se.l4.silo.engine;

import java.util.function.Consumer;

public interface TransactionValueProvider
{
	void provideTransactionValues(
		Consumer<? super TransactionValue<?>> consumer
	);
}
