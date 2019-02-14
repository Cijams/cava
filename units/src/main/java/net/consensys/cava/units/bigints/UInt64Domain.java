/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.units.bigints;

import com.google.common.collect.DiscreteDomain;

/**
 * A {@link DiscreteDomain} over {@link UInt64}.
 */
public final class UInt64Domain extends DiscreteDomain<UInt64> {

  @Override
  public UInt64 next(UInt64 value) {
    return value.add(1);
  }

  @Override
  public UInt64 previous(UInt64 value) {
    return value.subtract(1);
  }

  @Override
  public long distance(UInt64 start, UInt64 end) {
    boolean negativeDistance = start.compareTo(end) < 0;
    UInt64 distance = negativeDistance ? end.subtract(start) : start.subtract(end);
    if (!distance.fitsLong()) {
      return negativeDistance ? Long.MIN_VALUE : Long.MAX_VALUE;
    }
    long distanceLong = distance.toLong();
    return negativeDistance ? -distanceLong : distanceLong;
  }

  @Override
  public UInt64 minValue() {
    return UInt64.MIN_VALUE;
  }

  @Override
  public UInt64 maxValue() {
    return UInt64.MAX_VALUE;
  }
}
