/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfjson;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;
import org.eclipse.rdf4j.rio.helpers.JSONSettings;
import org.eclipse.rdf4j.rio.helpers.RDFJSONParserSettings;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * {@link RDFParser} implementation for the RDF/JSON format
 *
 * @author Peter Ansell
 */
public class RDFJSONParser extends AbstractRDFParser {

	private final RDFFormat actualFormat;

	/**
	 * Creates a parser using {@link RDFFormat#RDFJSON} to identify the parser.
	 */
	public RDFJSONParser() {
		this(RDFFormat.RDFJSON);
	}

	/**
	 * Creates a parser using the given RDFFormat to self identify.
	 *
	 * @param actualFormat
	 */
	public RDFJSONParser(final RDFFormat actualFormat) {
		this.actualFormat = actualFormat;
	}

	@Override
	public RDFFormat getRDFFormat() {
		return this.actualFormat;
	}

	@Override
	public void parse(final InputStream inputStream, final String baseUri)
			throws IOException, RDFParseException, RDFHandlerException {
		JsonParser jp = null;

		clear();

		try {
			if (this.rdfHandler != null) {
				this.rdfHandler.startRDF();
			}

			jp = configureNewJsonFactory().createParser(inputStream);
			rdfJsonToHandlerInternal(this.rdfHandler, this.valueFactory, jp);
		} catch (final IOException e) {
			if (jp != null) {
				reportFatalError("Found IOException during parsing", e, jp.getCurrentLocation());
			} else {
				reportFatalError(e);
			}
		} finally {
			clear();
			if (jp != null) {
				try {
					jp.close();
				} catch (final IOException e) {
					reportFatalError("Found exception while closing JSON parser", e, jp.getCurrentLocation());
				}
			}
		}
		if (this.rdfHandler != null) {
			this.rdfHandler.endRDF();
		}
	}

	/**
	 * Creates a literal, using the current value, language, and datatype, and additionally using the given
	 * {@link JsonLocation} to provide information about the line and column numbers in the event of a warning, error or
	 * exception being generated by the creation of the literal.
	 *
	 * @param label           the literal's lexical label
	 * @param language        the literal's language tag. Can be null.
	 * @param datatype        the literal's datatype. Can be null.
	 * @param currentLocation the current JsonLocation. May not be null.
	 * @return the created {@link Literal} object.
	 * @throws RDFParseException
	 */
	protected Literal createLiteral(String label, String language, IRI datatype, JsonLocation currentLocation)
			throws RDFParseException {
		return createLiteral(label, language, datatype, currentLocation.getLineNr(), currentLocation.getColumnNr());
	}

	protected void reportError(String msg, Exception e, JsonLocation location, RioSetting<Boolean> setting)
			throws RDFParseException {
		reportError(msg, e, location.getLineNr(), location.getColumnNr(), setting);
	}

	protected void reportError(String msg, JsonLocation location, RioSetting<Boolean> setting)
			throws RDFParseException {
		reportError(msg, location.getLineNr(), location.getColumnNr(), setting);
	}

	protected void reportFatalError(String msg, Exception e, JsonLocation location) throws RDFParseException {
		reportFatalError(msg, e, location.getLineNr(), location.getColumnNr());
	}

	protected void reportFatalError(String msg, JsonLocation location) throws RDFParseException {
		reportFatalError(msg, location.getLineNr(), location.getColumnNr());
	}

	@Override
	public void parse(final Reader reader, final String baseUri)
			throws IOException, RDFParseException, RDFHandlerException {
		JsonParser jp = null;

		clear();

		try {
			if (this.rdfHandler != null) {
				this.rdfHandler.startRDF();
			}

			jp = configureNewJsonFactory().createParser(reader);
			rdfJsonToHandlerInternal(rdfHandler, valueFactory, jp);
		} catch (final IOException e) {
			if (jp != null) {
				reportFatalError("Found IOException during parsing", e, jp.getCurrentLocation());
			} else {
				reportFatalError(e);
			}
		} finally {
			clear();
			if (jp != null) {
				try {
					jp.close();
				} catch (final IOException e) {
					reportFatalError("Found exception while closing JSON parser", e, jp.getCurrentLocation());
				}
			}
		}
		if (rdfHandler != null) {
			rdfHandler.endRDF();
		}
	}

	private void rdfJsonToHandlerInternal(final RDFHandler handler, final ValueFactory vf, final JsonParser jp)
			throws IOException, JsonParseException, RDFParseException, RDFHandlerException {
		if (jp.nextToken() != JsonToken.START_OBJECT) {
			reportFatalError("Expected RDF/JSON document to start with an Object", jp.getCurrentLocation());
		}

		while (jp.nextToken() != JsonToken.END_OBJECT) {
			final String subjStr = jp.getCurrentName();
			Resource subject;

			subject = subjStr.startsWith("_:") ? createNode(subjStr.substring(2)) : vf.createIRI(subjStr);
			if (jp.nextToken() != JsonToken.START_OBJECT) {
				reportFatalError("Expected subject value to start with an Object", jp.getCurrentLocation());
			}

			boolean foundPredicate = false;
			while (jp.nextToken() != JsonToken.END_OBJECT) {
				final String predStr = jp.getCurrentName();

				final IRI predicate = vf.createIRI(predStr);
				foundPredicate = true;

				if (jp.nextToken() != JsonToken.START_ARRAY) {
					reportFatalError("Expected predicate value to start with an array", jp.getCurrentLocation());
				}

				boolean foundObject = false;

				while (jp.nextToken() != JsonToken.END_ARRAY) {
					if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
						reportFatalError("Expected object value to start with an Object: subject=<" + subjStr
								+ "> predicate=<" + predStr + ">", jp.getCurrentLocation());
					}

					String nextValue = null;
					String nextType = null;
					String nextDatatype = null;
					String nextLanguage = null;
					final Set<String> nextContexts = new HashSet<>(2);

					while (jp.nextToken() != JsonToken.END_OBJECT) {
						final String fieldName = jp.getCurrentName();
						if (RDFJSONUtility.VALUE.equals(fieldName)) {
							if (nextValue != null) {
								reportError(
										"Multiple values found for a single object: subject=" + subjStr + " predicate="
												+ predStr,
										jp.getCurrentLocation(), RDFJSONParserSettings.FAIL_ON_MULTIPLE_OBJECT_VALUES);
							}

							jp.nextToken();

							nextValue = jp.getText();
						} else if (RDFJSONUtility.TYPE.equals(fieldName)) {
							if (nextType != null) {
								reportError(
										"Multiple types found for a single object: subject=" + subjStr + " predicate="
												+ predStr,
										jp.getCurrentLocation(), RDFJSONParserSettings.FAIL_ON_MULTIPLE_OBJECT_TYPES);
							}

							jp.nextToken();

							nextType = jp.getText();
						} else if (RDFJSONUtility.LANG.equals(fieldName)) {
							if (nextLanguage != null) {
								reportError(
										"Multiple languages found for a single object: subject=" + subjStr
												+ " predicate=" + predStr,
										jp.getCurrentLocation(),
										RDFJSONParserSettings.FAIL_ON_MULTIPLE_OBJECT_LANGUAGES);
							}

							jp.nextToken();

							nextLanguage = jp.getText();
						} else if (RDFJSONUtility.DATATYPE.equals(fieldName)) {
							if (nextDatatype != null) {
								reportError(
										"Multiple datatypes found for a single object: subject=" + subjStr
												+ " predicate=" + predStr,
										jp.getCurrentLocation(),
										RDFJSONParserSettings.FAIL_ON_MULTIPLE_OBJECT_DATATYPES);
							}

							jp.nextToken();

							nextDatatype = jp.getText();
						} else if (RDFJSONUtility.GRAPHS.equals(fieldName)) {
							if (jp.nextToken() != JsonToken.START_ARRAY) {
								reportError("Expected graphs to start with an array", jp.getCurrentLocation(),
										RDFJSONParserSettings.SUPPORT_GRAPHS_EXTENSION);
							}

							while (jp.nextToken() != JsonToken.END_ARRAY) {
								final String nextGraph = jp.getText();
								nextContexts.add(nextGraph);
							}
						} else {
							reportError(
									"Unrecognised JSON field name for object: subject=" + subjStr + " predicate="
											+ predStr + " fieldname=" + fieldName,
									jp.getCurrentLocation(), RDFJSONParserSettings.FAIL_ON_UNKNOWN_PROPERTY);
						}
					}

					Value object = null;

					if (nextType == null) {
						reportFatalError("No type for object: subject=" + subjStr + " predicate=" + predStr,
								jp.getCurrentLocation());
					}

					if (nextValue == null) {
						reportFatalError("No value for object: subject=" + subjStr + " predicate=" + predStr,
								jp.getCurrentLocation());
					}

					if (RDFJSONUtility.LITERAL.equals(nextType)) {
						if (nextLanguage != null) {
							object = this.createLiteral(nextValue, nextLanguage, null, jp.getCurrentLocation());
						} else if (nextDatatype != null) {
							object = this.createLiteral(nextValue, null, this.createURI(nextDatatype),
									jp.getCurrentLocation());
						} else {
							object = this.createLiteral(nextValue, null, null, jp.getCurrentLocation());
						}
					} else if (RDFJSONUtility.BNODE.equals(nextType)) {
						if (nextLanguage != null) {
							reportFatalError("Language was attached to a blank node object: subject=" + subjStr
									+ " predicate=" + predStr, jp.getCurrentLocation());
						}
						if (nextDatatype != null) {
							reportFatalError("Datatype was attached to a blank node object: subject=" + subjStr
									+ " predicate=" + predStr, jp.getCurrentLocation());
						}
						object = createNode(nextValue.substring(2));
					} else if (RDFJSONUtility.URI.equals(nextType)) {
						if (nextLanguage != null) {
							reportFatalError("Language was attached to a uri object: subject=" + subjStr + " predicate="
									+ predStr, jp.getCurrentLocation());
						}
						if (nextDatatype != null) {
							reportFatalError("Datatype was attached to a uri object: subject=" + subjStr + " predicate="
									+ predStr, jp.getCurrentLocation());
						}
						object = vf.createIRI(nextValue);
					}
					foundObject = true;

					if (!nextContexts.isEmpty()) {
						for (final String nextContext : nextContexts) {
							final Resource context;

							if (nextContext.equals(RDFJSONUtility.NULL)) {
								context = null;
							} else if (nextContext.startsWith("_:")) {
								context = createNode(nextContext.substring(2));
							} else {
								context = vf.createIRI(nextContext);
							}
							Statement st = vf.createStatement(subject, predicate, object, context);
							if (handler != null) {
								handler.handleStatement(st);
							}
						}
					} else {
						Statement st = vf.createStatement(subject, predicate, object);
						if (handler != null) {
							handler.handleStatement(st);
						}
					}

				}
				if (!foundObject) {
					reportFatalError("No object for predicate: subject=" + subjStr + " predicate=" + predStr,
							jp.getCurrentLocation());
				}
			}

			if (!foundPredicate) {
				reportFatalError("No predicate for object: subject=" + subjStr, jp.getCurrentLocation());
			}
		}
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		Collection<RioSetting<?>> result = new HashSet<>(super.getSupportedSettings());

		result.add(RDFJSONParserSettings.FAIL_ON_MULTIPLE_OBJECT_DATATYPES);
		result.add(RDFJSONParserSettings.FAIL_ON_MULTIPLE_OBJECT_LANGUAGES);
		result.add(RDFJSONParserSettings.FAIL_ON_MULTIPLE_OBJECT_TYPES);
		result.add(RDFJSONParserSettings.FAIL_ON_MULTIPLE_OBJECT_VALUES);
		result.add(RDFJSONParserSettings.FAIL_ON_UNKNOWN_PROPERTY);
		result.add(RDFJSONParserSettings.SUPPORT_GRAPHS_EXTENSION);

		result.add(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER);
		result.add(JSONSettings.ALLOW_COMMENTS);
		result.add(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS);
		result.add(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS);
		result.add(JSONSettings.ALLOW_SINGLE_QUOTES);
		result.add(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS);
		result.add(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES);
		result.add(JSONSettings.ALLOW_YAML_COMMENTS);
		result.add(JSONSettings.ALLOW_TRAILING_COMMA);
		result.add(JSONSettings.INCLUDE_SOURCE_IN_LOCATION);
		result.add(JSONSettings.STRICT_DUPLICATE_DETECTION);

		return result;
	}

	/**
	 * Get an instance of JsonFactory configured using the settings from {@link #getParserConfig()}.
	 *
	 * @return A newly configured JsonFactory based on the currently enabled settings
	 */
	private JsonFactory configureNewJsonFactory() {
		final JsonFactory nextJsonFactory = new JsonFactory();
		// Disable features that may work for most JSON where the field names are
		// in limited supply,
		// but does not work for RDF/JSON where a wide range of URIs are used for
		// subjects and predicates
		nextJsonFactory.disable(JsonFactory.Feature.INTERN_FIELD_NAMES);
		nextJsonFactory.disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES);
		nextJsonFactory.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

		if (getParserConfig().isSet(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,
					getParserConfig().get(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_COMMENTS)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_COMMENTS,
					getParserConfig().get(JSONSettings.ALLOW_COMMENTS));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS,
					getParserConfig().get(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS,
					getParserConfig().get(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_SINGLE_QUOTES)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES,
					getParserConfig().get(JSONSettings.ALLOW_SINGLE_QUOTES));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS,
					getParserConfig().get(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES,
					getParserConfig().get(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_YAML_COMMENTS)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS,
					getParserConfig().get(JSONSettings.ALLOW_YAML_COMMENTS));
		}
		if (getParserConfig().isSet(JSONSettings.ALLOW_TRAILING_COMMA)) {
			nextJsonFactory.configure(JsonParser.Feature.ALLOW_TRAILING_COMMA,
					getParserConfig().get(JSONSettings.ALLOW_TRAILING_COMMA));
		}
		if (getParserConfig().isSet(JSONSettings.INCLUDE_SOURCE_IN_LOCATION)) {
			nextJsonFactory.configure(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION,
					getParserConfig().get(JSONSettings.INCLUDE_SOURCE_IN_LOCATION));
		}
		if (getParserConfig().isSet(JSONSettings.STRICT_DUPLICATE_DETECTION)) {
			nextJsonFactory.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION,
					getParserConfig().get(JSONSettings.STRICT_DUPLICATE_DETECTION));
		}
		return nextJsonFactory;
	}
}
