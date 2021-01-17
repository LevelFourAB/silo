package se.l4.silo.engine.internal.tx;

import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.silo.Transactions;
import se.l4.silo.engine.TransactionValue;
import se.l4.silo.engine.TransactionValueProvider;

/**
 * Transaction support as seen internally in the storage engine.
 */
public interface TransactionSupport
	extends Transactions
{
	/**
	 * Register a value that should be provided in instances of
	 * {@link WriteableTransactionExchange}.
	 *
	 * @param value
	 */
	void registerValue(TransactionValue<?> value);

	/**
	 * Execute a function that should have access to an instance of
	 * {@link WriteableTransactionExchange}.
	 *
	 * @param <V>
	 * @param func
	 * @return
	 */
	<V> Mono<V> withExchange(
		Function<WriteableTransactionExchange, V> func,
		TransactionValueProvider... valuesToCapture
	);

	/**
	 * Execute a function that should have access to an instance of
	 * {@link WriteableTransactionExchange}.
	 *
	 * @param <V>
	 * @param func
	 * @return
	 */
	<V> Mono<V> monoWithExchange(
		Function<WriteableTransactionExchange, Mono<V>> func,
		TransactionValueProvider... valuesToCapture
	);

	/**
	 * Execute a function that should have access to an instance of
	 * {@link WriteableTransactionExchange}.
	 *
	 * @param <V>
	 * @param func
	 * @return
	 */
	<V> Flux<V> fluxWithExchange(
		Function<WriteableTransactionExchange, Flux<V>> func,
		TransactionValueProvider... valuesToCapture
	);
}
