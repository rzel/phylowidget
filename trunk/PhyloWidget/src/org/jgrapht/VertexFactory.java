/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Creator:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
 *
 * (C) Copyright 2003-2007, by Barak Naveh and Contributors.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */
/* ------------------
 * VertexFactory.java
 * ------------------
 * (C) Copyright 2003-2007, by John V. Sichi and Contributors.
 *
 * Original Author:  John V. Sichi
 * Contributor(s):   Christian Hammer
 *
 * $Id: VertexFactory.java 568 2007-09-30 00:12:18Z perfecthash $
 *
 * Changes
 * -------
 * 16-Sep-2003 : Initial revision (JVS);
 * 11-Mar-2004 : Made generic (CH);
 *
 */
package org.jgrapht;

/**
 * A vertex factory used by graph algorithms for creating new vertices.
 * Normally, vertices are constructed by user code and added to a graph
 * explicitly, but algorithms which generate new vertices require a factory.
 *
 * @author John V. Sichi
 * @since Sep 16, 2003
 */
public interface VertexFactory<V>
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Creates a new vertex.
     *
     * @return the new vertex
     */
    public V createVertex();
}

// End VertexFactory.java
