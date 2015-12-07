/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.process.traversal.step.map;

import org.apache.tinkerpop.gremlin.process.computer.KeyValue;
import org.apache.tinkerpop.gremlin.process.computer.MapReduce;
import org.apache.tinkerpop.gremlin.process.computer.traversal.TraversalVertexProgram;
import org.apache.tinkerpop.gremlin.process.computer.util.StaticMapReduce;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.step.MapReducer;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.ReducingBarrierStep;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.TraverserRequirement;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.util.TraverserSet;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.function.ConstantSupplier;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;

import static org.apache.tinkerpop.gremlin.process.traversal.NumberHelper.max;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class MaxGlobalStep<S extends Number> extends ReducingBarrierStep<S, S> implements MapReducer {

    public MaxGlobalStep(final Traversal.Admin traversal) {
        super(traversal);
        this.setSeedSupplier(new ConstantSupplier<>(null));
        this.setBiFunction(MaxGlobalBiFunction.<S>instance());
    }

    @Override
    public MapReduce<MapReduce.NullObject, Number, MapReduce.NullObject, Number, Number> getMapReduce() {
        return MaxGlobalMapReduce.instance();
    }

    /////

    private static class MaxGlobalBiFunction<S extends Number> implements BiFunction<S, Traverser<S>, S>, Serializable {

        private static final MaxGlobalBiFunction INSTANCE = new MaxGlobalBiFunction();

        private MaxGlobalBiFunction() {

        }

        @Override
        public S apply(final S mutatingSeed, final Traverser<S> traverser) {
            final S value = traverser.get();
            return mutatingSeed != null ? (S) max(mutatingSeed, traverser.get()) : value;
        }

        public static <S extends Number> MaxGlobalBiFunction<S> instance() {
            return INSTANCE;
        }
    }

    ///////////

    private static class MaxGlobalMapReduce extends StaticMapReduce<MapReduce.NullObject, Number, MapReduce.NullObject, Number, Number> {

        private static final MaxGlobalMapReduce INSTANCE = new MaxGlobalMapReduce();

        private MaxGlobalMapReduce() {

        }

        @Override
        public boolean doStage(final MapReduce.Stage stage) {
            return true;
        }

        @Override
        public void map(final Vertex vertex, final MapEmitter<NullObject, Number> emitter) {
            vertex.<TraverserSet<Number>>property(TraversalVertexProgram.HALTED_TRAVERSERS).ifPresent(traverserSet -> traverserSet.forEach(traverser -> emitter.emit(traverser.get())));
        }

        @Override
        public void combine(final NullObject key, final Iterator<Number> values, final ReduceEmitter<NullObject, Number> emitter) {
            this.reduce(key, values, emitter);
        }

        @Override
        public void reduce(final NullObject key, final Iterator<Number> values, final ReduceEmitter<NullObject, Number> emitter) {
            if (values.hasNext()) {
                Number max = null;
                while (values.hasNext()) {
                    final Number value = values.next();
                    max = max != null ? max(value, max) : value;
                }
                emitter.emit(max);
            }
        }

        @Override
        public String getMemoryKey() {
            return REDUCING;
        }

        @Override
        public Number generateFinalResult(final Iterator<KeyValue<NullObject, Number>> keyValues) {
            return keyValues.hasNext() ? keyValues.next().getValue() : Double.NaN;

        }

        public static MaxGlobalMapReduce instance() {
            return INSTANCE;
        }
    }
}