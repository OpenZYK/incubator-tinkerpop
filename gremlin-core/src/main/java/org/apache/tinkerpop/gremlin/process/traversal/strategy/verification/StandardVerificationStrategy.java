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
package org.apache.tinkerpop.gremlin.process.traversal.strategy.verification;

import org.apache.tinkerpop.gremlin.process.computer.traversal.step.VertexComputing;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.step.Barrier;
import org.apache.tinkerpop.gremlin.process.traversal.step.branch.RepeatStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.ProfileMarkerStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.ReducingBarrierStep;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.AbstractTraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class StandardVerificationStrategy extends AbstractTraversalStrategy<TraversalStrategy.VerificationStrategy> implements TraversalStrategy.VerificationStrategy {

    private static final StandardVerificationStrategy INSTANCE = new StandardVerificationStrategy();

    private StandardVerificationStrategy() {
    }

    @Override
    public void apply(final Traversal.Admin<?, ?> traversal) {
        if (!traversal.getStrategies().toList().contains(ComputerVerificationStrategy.instance())) {
            if (!TraversalHelper.getStepsOfAssignableClass(VertexComputing.class, traversal).isEmpty())
                throw new VerificationException("VertexComputing steps must be executing with a GraphComputer: " + TraversalHelper.getStepsOfAssignableClass(VertexComputing.class, traversal), traversal);
        }

        traversal.getSteps().forEach(step -> {
            if (step instanceof ReducingBarrierStep && step.getTraversal().getParent() instanceof RepeatStep && step.getTraversal().getParent().getGlobalChildren().get(0).getSteps().contains(step))
                throw new VerificationException("The parent of a reducing barrier can not be repeat()-step: " + step, traversal);
        });

        if (TraversalHelper.hasStepOfClass(ProfileMarkerStep.class, traversal) && traversal.asAdmin().getEndStep() instanceof ProfileMarkerStep) {
            throw new VerificationException("When specified, the .profile()-Step must be the last step.", traversal);
        }

        if (TraversalHelper.getStepsOfClass(ProfileMarkerStep.class, traversal).size() > 1) {
            throw new VerificationException("The .profile()-Step cannot be specified multiple times.", traversal);
        }
    }

    public static StandardVerificationStrategy instance() {
        return INSTANCE;
    }
}
