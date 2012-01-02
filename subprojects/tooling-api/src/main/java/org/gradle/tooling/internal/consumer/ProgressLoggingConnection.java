/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.tooling.internal.consumer;

import org.gradle.listener.ListenerManager;
import org.gradle.logging.ProgressLogger;
import org.gradle.logging.internal.ProgressCompleteEvent;
import org.gradle.logging.internal.ProgressEvent;
import org.gradle.logging.internal.ProgressListener;
import org.gradle.logging.internal.ProgressStartEvent;
import org.gradle.tooling.internal.consumer.connection.ConsumerConnection;
import org.gradle.tooling.internal.protocol.BuildOperationParametersVersion1;
import org.gradle.tooling.internal.protocol.BuildParametersVersion1;
import org.gradle.tooling.internal.protocol.ConnectionMetaDataVersion1;
import org.gradle.tooling.internal.protocol.ProgressListenerVersion1;

/**
 * Provides some high-level progress information.
 */
public class ProgressLoggingConnection implements ConsumerConnection {
    private final ConsumerConnection connection;
    private final LoggingProvider loggingProvider;

    public ProgressLoggingConnection(ConsumerConnection connection, LoggingProvider loggingProvider) {
        this.connection = connection;
        this.loggingProvider = loggingProvider;
    }

    public void stop() {
        connection.stop();
    }

    public ConnectionMetaDataVersion1 getMetaData() {
        return connection.getMetaData();
    }

    public void executeBuild(final BuildParametersVersion1 buildParameters, final BuildOperationParametersVersion1 operationParameters) {
        run("Execute build", operationParameters, new BuildAction<Void>() {
            public Void run(ConsumerConnection connection) {
                connection.executeBuild(buildParameters, operationParameters);
                return null;
            }
        });
    }

    public <T> T getModel(final Class<T> type, final BuildOperationParametersVersion1 operationParameters) {
        return run("Load projects", operationParameters, new BuildAction<T>() {
            public T run(ConsumerConnection connection) {
                return connection.getModel(type, operationParameters);
            }
        });
    }

    private <T> T run(String description, BuildOperationParametersVersion1 parameters, BuildAction<T> action) {
        ProgressListenerAdapter listener = new ProgressListenerAdapter(parameters.getProgressListener());
        ListenerManager listenerManager = loggingProvider.getListenerManager();
        listenerManager.addListener(listener);
        try {
            ProgressLogger progressLogger = loggingProvider.getProgressLoggerFactory().newOperation(ProgressLoggingConnection.class);
            progressLogger.setDescription(description);
            progressLogger.started();
            try {
                return action.run(connection);
            } finally {
                progressLogger.completed();
            }
        } finally {
            listenerManager.removeListener(listener);
        }
    }

    private interface BuildAction<T> {
        T run(ConsumerConnection connection);
    }

    private static class ProgressListenerAdapter implements ProgressListener {
        private final ProgressListenerVersion1 progressListener;

        public ProgressListenerAdapter(ProgressListenerVersion1 progressListener) {
            this.progressListener = progressListener;
        }

        public void started(ProgressStartEvent event) {
            progressListener.onOperationStart(event.getDescription());
        }

        public void progress(ProgressEvent event) {
        }

        public void completed(ProgressCompleteEvent event) {
            progressListener.onOperationEnd();
        }
    }
}
