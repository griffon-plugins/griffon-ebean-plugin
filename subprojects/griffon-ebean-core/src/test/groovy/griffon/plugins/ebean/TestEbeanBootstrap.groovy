/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package griffon.plugins.ebean

import com.avaje.ebean.EbeanServer

import javax.annotation.Nonnull

class TestEbeanBootstrap implements EbeanBootstrap {
    boolean initWitness
    boolean destroyWitness

    @Override
    void init(@Nonnull String ebeanServerName, @Nonnull EbeanServer ebeanServer) {
        initWitness = true
    }

    @Override
    void destroy(@Nonnull String ebeanServerName, @Nonnull EbeanServer ebeanServer) {
        destroyWitness = true
    }
}
