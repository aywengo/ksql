/*
 * Copyright 2022 Confluent Inc.
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

package io.confluent.ksql.security;

import java.security.Principal;
import java.util.Optional;

/**
 * Interface to extract auth token information to ksqlDB
 */
public interface KsqlAuthTokenProvider {

  /**
   * Extract the lifetime of a token from the Principal.
   *
   * @param principal The {@link Principal} that's carrying the auth token.
   * @return An {@Optional} containing the expiration time of the token in ms if there is one
   */
  Optional<Long> getLifetimeMs(Principal principal);
}
