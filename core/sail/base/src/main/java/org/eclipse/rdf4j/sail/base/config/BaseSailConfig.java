/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base.config;

import static org.eclipse.rdf4j.model.util.Values.literal;

import java.util.Optional;

import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Configurations;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.vocabulary.Config;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.sail.config.AbstractSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

public abstract class BaseSailConfig extends AbstractSailImplConfig {

	private String evalStratFactoryClassName;

	private QueryEvaluationMode defaultQueryEvaluationMode;

	protected BaseSailConfig(String type) {
		super(type);
	}

	public String getEvaluationStrategyFactoryClassName() {
		return evalStratFactoryClassName;
	}

	public void setEvaluationStrategyFactoryClassName(String className) {
		this.evalStratFactoryClassName = className;
	}

	public EvaluationStrategyFactory getEvaluationStrategyFactory() throws SailConfigException {
		if (evalStratFactoryClassName == null) {
			return null;
		}

		try {
			var factory = (EvaluationStrategyFactory) Thread.currentThread()
					.getContextClassLoader()
					.loadClass(evalStratFactoryClassName)
					.newInstance();
			return factory;
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new SailConfigException(e);
		}
	}

	@Override
	public Resource export(Model graph) {
		Resource implNode = super.export(graph);

		if (evalStratFactoryClassName != null) {
			graph.add(implNode, Config.Sail.evaluationStrategyFactory,
					literal(evalStratFactoryClassName));
		}
		getDefaultQueryEvaluationMode().ifPresent(mode -> {
			graph.add(implNode, Config.Sail.defaultQueryEvaluationMode,
					literal(mode.getValue()));
		});

		return implNode;
	}

	@Override
	public void parse(Model graph, Resource implNode) throws SailConfigException {
		super.parse(graph, implNode);

		try {
			Configurations.getLiteralValue(graph, implNode, Config.Sail.defaultQueryEvaluationMode,
					BaseSailSchema.DEFAULT_QUERY_EVALUATION_MODE)
					.ifPresent(qem -> setDefaultQueryEvaluationMode(
							QueryEvaluationMode.valueOf(qem.stringValue())));

			Configurations.getLiteralValue(graph, implNode, Config.Sail.evaluationStrategyFactory,
					BaseSailSchema.EVALUATION_STRATEGY_FACTORY)
					.ifPresent(factoryClassName -> {
						setEvaluationStrategyFactoryClassName(factoryClassName.stringValue());
					});
		} catch (IllegalArgumentException | ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}

	/**
	 * @return the defaultQueryEvaluationMode
	 */
	public Optional<QueryEvaluationMode> getDefaultQueryEvaluationMode() {
		return Optional.ofNullable(defaultQueryEvaluationMode);
	}

	/**
	 * @param defaultQueryEvaluationMode the defaultQueryEvaluationMode to set
	 */
	public void setDefaultQueryEvaluationMode(QueryEvaluationMode defaultQueryEvaluationMode) {
		this.defaultQueryEvaluationMode = defaultQueryEvaluationMode;
	}
}
