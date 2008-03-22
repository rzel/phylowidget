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
package org.phylowidget.ui;

import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import org.andrewberman.ui.AbstractUIObject;
import org.andrewberman.ui.EventManager;
import org.andrewberman.ui.FocusManager;
import org.andrewberman.ui.Point;
import org.andrewberman.ui.UIRectangle;
import org.andrewberman.ui.UIUtils;
import org.andrewberman.ui.ifaces.UIObject;
import org.andrewberman.ui.tools.Tool;
import org.andrewberman.ui.tween.Tween;
import org.andrewberman.ui.tween.TweenListener;
import org.andrewberman.ui.tween.TweenQuad;
import org.phylowidget.PhyloWidget;
import org.phylowidget.render.NodeRange;
import org.phylowidget.render.RenderStyleSet;
import org.phylowidget.render.TreeRenderer;
import org.phylowidget.tree.RootedTree;
import org.phylowidget.tree.TreeManager;

import processing.core.PApplet;

public class NodeTraverser extends AbstractUIObject implements TweenListener
{
	public static final int LEFT = 0, RIGHT = 1, UP = 2, DOWN = 3;
	private NodeRange curNodeRange;
	public Tween glowTween;

	boolean isGlowing;
	Point mousePt = new Point();

	ArrayList nearNodes = new ArrayList(10);

	private PApplet p;

	Point pt = new Point();

	UIRectangle rect = new UIRectangle();

	Point tempPt = new Point();

	public NodeTraverser(PApplet p)
	{
		UIUtils.loadUISinglets(p);
		this.p = p;
		glowTween = new Tween(this, TweenQuad.tween, Tween.INOUT, 1f, .75f, 30);
		EventManager.instance.add(this);
	}

	private boolean containsPoint(NodeRange r, Point pt)
	{
		tempPt.setLocation(getX(r), getY(r));
		float radius = r.render.getTextSize() / 2f;
		float distance = (float) pt.distance(tempPt);
		// System.out.println("rad:" + radius + " dist:" + distance);
		if (distance < radius)
		{
			return true;
		}

		Rectangle rc = new Rectangle();
		rc.setFrameFromDiagonal(r.loX, r.loY, r.hiX, r.hiY);
		return rc.contains(pt);
	}

	public synchronized void draw()
	{
		/*
		 * Update the glowing circle's radius.
		 */
		if (isGlowing)
		{
			glowTween.update();
			float glowRadius = PhyloWidget.trees.getRenderer().getTextSize();
			glowRadius *= glowTween.getPosition();

			p.noFill();
			p.strokeWeight(glowRadius / 10f);
			int color = RenderStyleSet.defaultStyle().hoverColor.getRGB();
			p.stroke(color);
			NodeRange r = getCurRange();
			float cX = getX(r);
			float cY = getY(r);
			p.ellipse(cX, cY, glowRadius, glowRadius);
		}
	}

	public void focusEvent(FocusEvent e)
	{

	}

	public NodeRange getCurRange()
	{
		if (curNodeRange == null)
		{
			/*
			 * If we haven't set a "focused" NodeRange yet, set it to the root
			 * node.
			 */
			TreeRenderer render = PhyloWidget.trees.getRenderer();
			RootedTree t = render.getTree();
			PhyloNode n = (PhyloNode) t.getRoot();
			curNodeRange = rangeForNode(render, n);
		}
		return curNodeRange;
	}

	public PhyloNode getCurrentNode()
	{
		NodeRange nr = getCurRange();
		return nr.node;
	}

	private NodeRange getNearestNode(float x, float y)
	{
		getWithinRange(x, y, 40);
		pt.setLocation(x, y);
		float nearestDist = Float.MAX_VALUE;
		NodeRange temp = null;
		for (int i = 0; i < nearNodes.size(); i++)
		{
			NodeRange r = (NodeRange) nearNodes.get(i);
			PhyloNode n = r.node;
			switch (r.type)
			{
				case (NodeRange.NODE):
					float dist = (float) pt.distance(getX(r), getY(r));

					if (dist < nearestDist)
					{
						temp = r;
						nearestDist = dist;
					}
					break;
			}
		}
		return temp;
	}

	private void getWithinRange(float x, float y, float radius)
	{
		float ratio = TreeManager.camera.getZ();
		float rad = radius * ratio;

		pt.setLocation(x, y);
		rect.x = (float) (pt.getX() - rad);
		rect.y = (float) (pt.getY() - rad);
		rect.width = rad * 2;
		rect.height = rad * 2;
		UIUtils.screenToModel(pt);
		UIUtils.screenToModel(rect);
		nearNodes.clear();
		PhyloWidget.trees.nodesInRange(nearNodes, rect);
	}

	float getX(NodeRange r)
	{
		return r.render.getX(r.node);
	}

	float getY(NodeRange r)
	{
		return r.render.getY(r.node);
	}

	public void keyEvent(KeyEvent e)
	{
		if (e.getID() != KeyEvent.KEY_PRESSED)
			return;
		if (PhyloWidget.ui.context.isOpen())
			return;
		if (FocusManager.instance.getFocusedObject() != null)
			return;
		int code = e.getKeyCode();
		// System.out.println(e);
		switch (code)
		{
			case (KeyEvent.VK_LEFT):
				navigate(LEFT);
				break;
			case (KeyEvent.VK_RIGHT):
				navigate(RIGHT);
				break;
			case (KeyEvent.VK_UP):
				navigate(UP);
				break;
			case (KeyEvent.VK_DOWN):
				navigate(DOWN);
				break;
			case (KeyEvent.VK_ENTER):
				// System.out.println("Open!");
				if (getCurRange() != null)
					openContextMenu();
				break;
		}
	}

	public void mouseEvent(MouseEvent e, Point screen, Point model)
	{
		mousePt.setLocation(screen);
		pt.setLocation(screen);
		if (EventManager.instance.getToolManager() == null)
			return;
		Tool t = EventManager.instance.getToolManager().getCurrentTool();
		if (t == null)
			return;
		if (PhyloWidget.ui.context == null || PhyloWidget.ui.context.isOpen())
			return;
		if (getCurRange() == null)
			return;
		if (FocusManager.instance.getFocusedObject() != null)
			return;
		PhyloWidget.trees.getRenderer().setMouseLocation(pt);
		setCurRange(getNearestNode((float) pt.getX(), (float) pt.getY()));
		boolean containsPoint = containsPoint(getCurRange(), pt);
		switch (e.getID())
		{
			case (MouseEvent.MOUSE_MOVED):
			case (MouseEvent.MOUSE_DRAGGED):
				PhyloTree tree = (PhyloTree) PhyloWidget.trees.getTree();
				if (containsPoint && t.respondToOtherEvents())
				{
					UIUtils.setCursor(this, p, Cursor.HAND_CURSOR);
					tree.setHoveredNode(getCurRange().node);
				} else
				{
					UIUtils.releaseCursor(this, p);
					tree.setHoveredNode(null);
				}
				break;
			case (MouseEvent.MOUSE_PRESSED):
				if (containsPoint && t.respondToOtherEvents())
				{
					openContextMenu();
					isGlowing = false;
				}
				break;
		}
		if (!t.respondToOtherEvents())
		{
			PhyloTree tree = (PhyloTree) PhyloWidget.trees.getTree();
			tree.setHoveredNode(null);
			setCurRange(null);
		}
	}

	private void navigate(int dir)
	{
		/*
		 * Ok, our strategy for navigation will be as follows:
		 * 
		 * 1. Get all nodes within a reasonable range. 2. Score the closest
		 * nodes, adding points for closeness but deducting points for being
		 * off-axis.
		 */
		Point base = new Point();
		NodeRange cur = getCurRange();

		/*
		 * The LEFT or RIGHT directions should always go IN or OUT of the tree,
		 * while the up and down will use the score-based search.
		 */
		if (dir == LEFT || dir == RIGHT)
		{
			RootedTree t = cur.render.getTree();
			PhyloNode curNode = null;
			if (dir == LEFT)
			{
				if (t.getParentOf(cur.node) != null)
				{
					curNode = (PhyloNode) t.getParentOf(cur.node);
					setCurRange(rangeForNode(cur.render, curNode));
					return;
				}
			} else if (dir == RIGHT)
			{
				if (!t.isLeaf(cur.node))
				{
					List kids = t.getChildrenOf(cur.node);
					curNode = (PhyloNode) kids.get(kids.size() - 1);
					setCurRange(rangeForNode(cur.render, curNode));
					return;
				}
			}

		}

		switch (dir)
		{
			case (LEFT):
				base.setLocation(-1, .1);
				break;
			case (RIGHT):
				base.setLocation(1, -.1);
				break;
			case (UP):
				base.setLocation(0, -1);
				break;
			case (DOWN):
				base.setLocation(0, 1);
				break;
		}

		pt.setLocation(cur.node.getX(), cur.node.getY());
		getWithinRange(cur.node.getX(), cur.node.getY(), 200);
		Point pt2 = new Point();
		float maxScore = -Float.MAX_VALUE;
		NodeRange maxRange = null;
		for (int i = 0; i < nearNodes.size(); i++)
		{
			NodeRange r = (NodeRange) nearNodes.get(i);
			if (r.type == NodeRange.LABEL)
				continue;
			if (r.node == cur.node)
				continue;
			pt2.setLocation(r.node.getX(), r.node.getY());
			float score = score(pt, pt2, base);
			if (score > maxScore)
			{
				maxScore = score;
				maxRange = r;
			}
		}
		if (maxRange == null)
		{
			System.out.println("Nothing near found");
		} else
		{
			setCurRange(maxRange);
		}
	}

	void openContextMenu()
	{
		PhyloWidget.ui.context.open(getCurRange());
	}

	private NodeRange rangeForNode(TreeRenderer tr, PhyloNode n)
	{
		return (NodeRange) tr.rangeForNode(n);
	}

	private void resetGlow()
	{
		glowTween.rewind();
		glowTween.start();
	}

	float score(Point me, Point him, Point dirV)
	{
		him.translate((float) -me.getX(), (float) -me.getY());
		float len = him.length();
		float dot = dirV.dotProd(him);
		if (dot / len < 0.1)
			return -Float.MAX_VALUE;
		return dot / len - len / 30;
	}

	public void setCurRange(NodeRange r)
	{
		if (r != null && r != curNodeRange)
		{
			Tool t = EventManager.instance.getToolManager().getCurrentTool();
			if (t.respondToOtherEvents())
			{
				RootedTree tree = r.render.getTree();
				PhyloWidget.ui.updateNodeInfo(tree, r.node);
			}
		}
		/*
		 * Logic for the glow resetting and whatnot.
		 */
		if (r == null)
		{
			isGlowing = false;
		} else
		{
			if (isGlowing == false)
				resetGlow();
			if (r != curNodeRange)
			{
				curNodeRange = r;
				resetGlow();
			}
			isGlowing = true;
		}
	}

	public void tweenEvent(Tween source, int eventType)
	{
		if (eventType == Tween.FINISHED)
			source.yoyo();
	}

}