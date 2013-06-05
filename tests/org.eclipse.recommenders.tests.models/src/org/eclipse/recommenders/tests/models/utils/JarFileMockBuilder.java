/**
 * Copyright (c) 2010 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Johannes Lerch - initial API and implementation.
 *    Andreas Sewe - enhancements to mock's fidelity
 */
package org.eclipse.recommenders.tests.models.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.recommenders.utils.Tuple;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class JarFileMockBuilder {

    private final List<Tuple<String, InputStream>> entries = new LinkedList<Tuple<String, InputStream>>();

    public JarFile build() {
        final JarFile jarFile = Mockito.mock(JarFile.class);
        Mockito.when(jarFile.entries()).thenAnswer(new Answer<Enumeration<JarEntry>>() {

            @Override
            public Enumeration<JarEntry> answer(InvocationOnMock invocation) throws Throwable {
                return new EntryEnumeration(jarFile);
            }
        });
        return jarFile;
    }

    public void addEntry(final String filename, final InputStream inputStream) {
        entries.add(Tuple.newTuple(filename, inputStream));
    }

    private class EntryEnumeration implements Enumeration<JarEntry> {

        private final Iterator<Tuple<String, InputStream>> iterator;
        private final JarFile jarFile;

        private EntryEnumeration(final JarFile jarFile) {
            this.jarFile = jarFile;
            iterator = entries.iterator();
        }

        @Override
        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        @Override
        public JarEntry nextElement() {
            final Tuple<String, InputStream> tuple = iterator.next();
            final JarEntry jarEntry = Mockito.mock(JarEntry.class);
            Mockito.when(jarEntry.getName()).thenReturn(tuple.getFirst());
            try {
                Mockito.when(jarFile.getInputStream(jarEntry)).thenReturn(tuple.getSecond());
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
            return jarEntry;
        }

    }
}
