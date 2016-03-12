/*
 * Copyright 2016 Santanu Sinha <santanu.sinha@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.dropwizard.sharding.utils;

import org.hibernate.*;
import org.hibernate.context.internal.ManagedSessionContext;

/**
 * A transaction handler utility class
 */
public class TransactionHandler {


    // Context variables
    private Session session;
    private final SessionFactory sessionFactory;
    private boolean readOnly;

    public TransactionHandler(SessionFactory sessionFactory, boolean readOnly) {
        this.sessionFactory = sessionFactory;
        this.readOnly = readOnly;
    }

    public void beforeStart() {

        session = sessionFactory.openSession();
        try {
            configureSession();
            ManagedSessionContext.bind(session);
            beginTransaction();
        } catch (Throwable th) {
            session.close();
            session = null;
            ManagedSessionContext.unbind(sessionFactory);
            throw th;
        }
    }

    public void afterEnd() {
        if (session == null) {
            return;
        }

        try {
            commitTransaction();
        } catch (Exception e) {
            rollbackTransaction();
            throw e;
        } finally {
            session.close();
            session = null;
            ManagedSessionContext.unbind(sessionFactory);
        }

    }

    public void onError() {
        if (session == null) {
            return;
        }

        try {
            rollbackTransaction();
        } finally {
            session.close();
            session = null;
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    private void configureSession() {
        session.setDefaultReadOnly(readOnly);
        session.setCacheMode(CacheMode.NORMAL);
        session.setFlushMode(FlushMode.AUTO);
    }

    private void beginTransaction() {
        session.beginTransaction();
    }

    private void rollbackTransaction() {
        final Transaction txn = session.getTransaction();
        if (txn != null && txn.isActive()) {
            txn.rollback();
        }
    }

    private void commitTransaction() {
        final Transaction txn = session.getTransaction();
        if (txn != null && txn.isActive()) {
            txn.commit();
        }
    }
}
