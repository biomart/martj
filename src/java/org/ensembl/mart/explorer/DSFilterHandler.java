/*
    Copyright (C) 2003 EBI, GRL

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */
 
package org.ensembl.mart.explorer;

import java.sql.Connection;

/**
 * <p>This system is designed to facilitate the inclusion of Domain Specific Filters
 * into a Mart Query.  Domain Specific Filters use logic outside of the &quot;Normal&quot;
 * Mart filters, which involve SQL where-clause statements involving Mart Main/dimension tables.</p>
 * <br>
 * <p>The DSFilterHandler system is designed to &quot;translate&quot; Domain
 * Specific Filters that are supported by the system into &quot;Normal&quot;
 * filters on a Query.  All Mart queries are designed to return attributes
 * related to the focus object within a particular dataset defined within the Mart.
 * Domain Specific Filters must ultimately filter on this focus object, eg, through
 * primary-key mapping, or some other system supported by the Mart.  To accomplish this,
 * a DSFilterHandler implementing object must resolve the Domain Specific aspects that 
 * it is designed to deal with into the addition of Filter implementing objects to
 * the Query.</p>
 * <br>
 * <p>The EnsMart Mart-Explorer implementation includes the following DSFilterHandler implementing
 *    objects:</p>
 * <ul>
 *   <li><p>Marker: Resolves Filters for Genes/Snps located between pairs of known chromosomal markers
 *          into chromosomal start and end coordinate filters.</p>
 *          
 *   <li><p>Band: Resolves Filters for Genes/Snps located between pairs of known chromosomal bands
 *          into chromosomal start and end coordinate filters.</p>
 *          
 *   <li><p>Encode: Resolves Filters for Genes/Snps in locations defined in the <a href="http://www.genome.gov/Pages/Research/ENCODE/">NCBI ENCODE PROJECT</a>
 *          into a chromosome filter, and chromosomal start and end coordinate filters.</p>
 *          
 *   <li><p>Qtl: Resolves Filters for Genes/Snps within regions with known Quantitative Trait Loci
 *          into a chromosome filter, and chromosomal start and end coordinate filters.</p>
 *          
 *   <li><p>Expression: Resolves Filters for Genes based on expression data from microarray datasets provided by the Mart
 *          using the EVOC controlled Expression Vocabulary.  These filters are resolved into a transcript_id IDListFilter.</p>
 *          
 *   </ul>
 *     
 * @author <a href="mailto:dlondon@ebi.ac.uk">Darin London</a>
 * @author <a href="mailto:craig@ebi.ac.uk">Craig Melsopp</a>
 * @see DSFilterHandlerFactory
 * @see DSMarkerFilterHandler
 * @see DSBandFilterHandler
 * @see DSEncodeQtlFilterHandler
 * @see DSExpressionFilterHandler
 */
public interface DSFilterHandler {
	
	/**
	 * Method that Resolves the DomainSpecificFilter into one or more Normal Filters on a Query.  
	 * All information that a specific DSFilterHandler implementing object needs to function should be 
	 * provided in the String parameter. The ModifyFilter method should then be able to process this String 
	 * using an implementation specific method. The User Interface is responsible for constructing the parameter 
	 * to meet the needs of the specific DomainSpecificFilter that it is adding to the Query.
	 * The method should take a Query object as an argument, construct
	 * a copy of that Query using its copy constructor, add all Filter objects to the new Query depending
	 * on the results of its implementation dependent logic, and return the new Query.  
	 * The SQL Connection object is provided for DSFilterHandler implementing objects needing to make SQL 
	 * requests on the mart database to get necessary information.
	 * 
	 * @param conn - A java.sql.Connection object connected to a Mart Database.
	 * @param parameter - A string containing all information that a specific DSFilterHandler implementing object needs to function.
	 * @param query - A Query object with all filters/attributes defined by the UI, including the DomainSpecificFilter objects.
	 * @return Query q - A copy of the input Query object, modified with new Filters which map to the Domain Specific Filter functionality.
	 * @throws InvalidQueryException - The method should chain all underlying checked Exceptions (SQLExceptions, IOExceptions, etc.) in an InvalidQueryException
	 */
	public Query ModifyQuery(Connection conn, String parameter, Query query) throws InvalidQueryException;
}
