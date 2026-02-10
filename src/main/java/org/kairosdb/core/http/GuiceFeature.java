/*
 * Copyright 2016 KairosDB Authors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.kairosdb.core.http;

import com.google.inject.Injector;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

/**
 * Jersey Feature that enables Guice integration.
 * This allows Jersey to use Guice-managed instances for JAX-RS resources.
 */
public class GuiceFeature implements Feature
{
    private final Injector injector;

    public GuiceFeature(Injector injector)
    {
        this.injector = injector;
    }

    @Override
    public boolean configure(FeatureContext context)
    {
        return true;
    }

    public Injector getInjector()
    {
        return injector;
    }
}
