/*
 * Copyright 2019 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"; you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.function.udaf.sum;

import io.confluent.ksql.GenericKey;
import io.confluent.ksql.execution.function.TableAggregationFunction;
import io.confluent.ksql.function.BaseAggregateFunction;
import io.confluent.ksql.function.KsqlFunctionException;
import io.confluent.ksql.function.ParameterInfo;
import io.confluent.ksql.function.types.ParamTypes;
import io.confluent.ksql.schema.ksql.types.SqlDecimal;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collections;
import java.util.function.Function;
import org.apache.kafka.streams.kstream.Merger;

public class DecimalSumKudaf
    extends BaseAggregateFunction<BigDecimal, BigDecimal, BigDecimal>
    implements TableAggregationFunction<BigDecimal, BigDecimal, BigDecimal> {

  private final MathContext context;
  private final BigDecimal maxValue;
  private final int precision;
  private final int scale;
  private final int digits;

  DecimalSumKudaf(
      final String functionName,
      final int argIndexInValue,
      final SqlDecimal outputSchema
  ) {
    super(
        functionName,
        argIndexInValue,
        () -> BigDecimal.ZERO,
        outputSchema,
        outputSchema,
        Collections.singletonList(new ParameterInfo("val", ParamTypes.DECIMAL, "", false)),
        "Computes the sum of decimal values for a key, resulting in a decimal with the same "
            + "precision and scale.");
    context = new MathContext(outputSchema.getPrecision());
    precision = outputSchema.getPrecision();
    scale = outputSchema.getScale();
    digits = outputSchema.getPrecision() - outputSchema.getScale();
    maxValue = BigDecimal.valueOf(Math.pow(10, digits));
  }

  @Override
  public BigDecimal aggregate(final BigDecimal currentValue, final BigDecimal aggregateValue) {
    if (currentValue == null) {
      return aggregateValue;
    }

    final BigDecimal value = aggregateValue.add(currentValue, context);

    if (maxValue.compareTo(value.abs()) < 1) {
      throw new KsqlFunctionException(
          String.format("Numeric field overflow: A field with precision %d and scale %d "
                  + "must round to an absolute value less than 10^%d. Got %s",
              precision,
              scale,
              digits,
              value.toPlainString()));
    }

    return value;
  }

  @Override
  public Merger<GenericKey, BigDecimal> getMerger() {
    return (key, agg1, agg2) -> agg1.add(agg2, context);
  }

  @Override
  public Function<BigDecimal, BigDecimal> getResultMapper() {
    return Function.identity();
  }

  @Override
  public BigDecimal undo(final BigDecimal valueToUndo, final BigDecimal aggregateValue) {
    if (valueToUndo == null) {
      return aggregateValue;
    }
    return aggregateValue.subtract(valueToUndo, context);
  }
}
