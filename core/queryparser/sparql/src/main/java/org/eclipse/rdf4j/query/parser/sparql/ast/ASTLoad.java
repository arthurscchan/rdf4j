/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
/* Generated By:JJTree: Do not edit this line. ASTLoad.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.eclipse.rdf4j.query.parser.sparql.ast;

public class ASTLoad extends ASTUpdate {

	private boolean silent;

	public ASTLoad(int id) {
		super(id);
	}

	public ASTLoad(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	/** Accept the visitor. **/
	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
		return visitor.visit(this, data);
	}

	public void setSilent(boolean silent) {
		this.silent = silent;
	}

	public boolean isSilent() {
		return this.silent;
	}
}
/* JavaCC - OriginalChecksum=b83ece3152041c4178153a0f76debe55 (do not edit this line) */
