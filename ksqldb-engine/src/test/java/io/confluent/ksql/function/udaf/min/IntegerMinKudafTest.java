/*
 * Copyright 2018 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.function.udaf.min;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import io.confluent.ksql.GenericKey;
import io.confluent.ksql.function.AggregateFunctionInitArguments;
import io.confluent.ksql.function.KsqlAggregateFunction;
import io.confluent.ksql.schema.ksql.SqlArgument;
import io.confluent.ksql.schema.ksql.types.SqlTypes;
import java.util.Collections;
import org.apache.kafka.streams.kstream.Merger;
import org.junit.Test;

public class IntegerMinKudafTest {

  @Test
  public void shouldFindCorrectMin() {
    final MinKudaf<Integer> integerMinKudaf = getIntegerMinKudaf();
    final int[] values = new int[]{3, 5, 8, 2, 3, 4, 5};
    int currentMin = Integer.MAX_VALUE;
    for (final int i: values) {
      currentMin = integerMinKudaf.aggregate(i, currentMin);
    }
    assertThat(2, equalTo(currentMin));
  }

  @Test
  public void shouldHandleNull() {
    final MinKudaf<Integer> integerMinKudaf = getIntegerMinKudaf();
    final int[] values = new int[]{3, 5, 8, 2, 3, 4, 5};
    Integer currentMin = null;

    // aggregate null before any aggregation
    currentMin = integerMinKudaf.aggregate(null, currentMin);
    assertThat(null, equalTo(currentMin));

    // now send each value to aggregation and verify
    for (final int i: values) {
      currentMin = integerMinKudaf.aggregate(i, currentMin);
    }
    assertThat(2, equalTo(currentMin));

    // null should not impact result
    currentMin = integerMinKudaf.aggregate(null, currentMin);
    assertThat(2, equalTo(currentMin));
  }

  @Test
  public void shouldFindCorrectMinForMerge() {
    final MinKudaf<Integer> integerMinKudaf = getIntegerMinKudaf();
    final Merger<GenericKey, Integer> merger = integerMinKudaf.getMerger();
    final Integer mergeResult1 = merger.apply(null, 10, 12);
    assertThat(mergeResult1, equalTo(10));
    final Integer mergeResult2 = merger.apply(null, 10, -12);
    assertThat(mergeResult2, equalTo(-12));
    final Integer mergeResult3 = merger.apply(null, -10, 0);
    assertThat(mergeResult3, equalTo(-10));
  }

  @SuppressWarnings("unchecked")
  private MinKudaf<Integer> getIntegerMinKudaf() {
    final KsqlAggregateFunction<Integer, Integer, Integer> aggregateFunction =
        (KsqlAggregateFunction<Integer, Integer, Integer>) new MinAggFunctionFactory()
        .createAggregateFunction(Collections.singletonList(SqlArgument.of(SqlTypes.INTEGER)),
            AggregateFunctionInitArguments.EMPTY_ARGS);
    assertThat(aggregateFunction, instanceOf(MinKudaf.class));
    return  (MinKudaf<Integer>) aggregateFunction;
  }
}