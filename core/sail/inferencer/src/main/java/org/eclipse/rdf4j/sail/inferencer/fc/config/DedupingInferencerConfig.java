/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.fc.config;

import org.eclipse.rdf4j.sail.config.AbstractDelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailImplConfig;

public class DedupingInferencerConfig extends AbstractDelegatingSailImplConfig {

	public DedupingInferencerConfig() {
		super(DedupingInferencerFactory.SAIL_TYPE);
	}

	public DedupingInferencerConfig(SailImplConfig delegate) {
		super(DedupingInferencerFactory.SAIL_TYPE, delegate);
	}
}
