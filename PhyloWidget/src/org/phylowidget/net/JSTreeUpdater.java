/**************************************************************************
 * Copyright (c) 2007, 2008 Gregory Jordan
 * 
 * This file is part of PhyloWidget.
 * 
 * PhyloWidget is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * PhyloWidget is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with PhyloWidget.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.phylowidget.net;

import org.andrewberman.ui.unsorted.DelayedAction;
import org.andrewberman.ui.unsorted.JSCaller;
import org.phylowidget.PhyloWidget;
import org.phylowidget.tree.RootedTree;
import org.phylowidget.tree.TreeIO;

public class JSTreeUpdater extends DelayedAction
{
	
	JSCaller caller = new JSCaller(PhyloWidget.p);
	RootedTree tree;
	String jsCall;

	public void triggerUpdate(RootedTree t)
	{
		tree = t;
		trigger(200);
		jsCall = "updateTree";
	}

	public void run()
	{
		String s = TreeIO.createNewickString(tree,false);
		String cmd = jsCall;
		try {
			Object o = caller.getMember("PhyloWidget");
			caller.callWithObject(o, jsCall, s);
		} catch (Exception e)
		{
//			e.printStackTrace();
		}
	}

}